package com.seibel.distanthorizons.core.util.delayedSaveCache;

import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.util.KeyedLockContainer;
import com.seibel.distanthorizons.core.util.ThreadUtil;
import com.seibel.distanthorizons.core.logging.DhLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Used to batch together multiple updates that all
 * affect the same position.
 * 
 * @see AbstractSaveObjContainer
 */
public abstract class AbstractDelayedSaveCache<TSaveObj, TSaveContainer extends AbstractSaveObjContainer<TSaveObj>> implements AutoCloseable
{
	private static final DhLogger LOGGER = new DhLoggerBuilder()
		.name(AbstractDelayedSaveCache.class.getSimpleName())
		.build();
	
	/** 
	 * a cache won't automatically clean itself unless we trigger it's clean method
	 * if not done then we'd only see the cache invalidate when new inserts happen,
	 * which causes weird behavior when placing/breaking blocks.
	 */
	private static final ThreadPoolExecutor BACKGROUND_CLEAN_UP_THREAD = ThreadUtil.makeSingleDaemonThreadPool("delayed save cache cleaner");
	private static final Set<WeakReference<AbstractDelayedSaveCache<?,?>>> SAVE_CACHE_SET = Collections.newSetFromMap(new ConcurrentHashMap<>());
	/** how long between clean up checks */
	private static final int CLEANUP_CHECK_TIME_IN_MS = 1_000;
	
	
	
	protected final ConcurrentHashMap<Long, TSaveContainer> saveContainerByPosition = new ConcurrentHashMap<>();
	
	/* don't let two threads load the same position at the same time */
	protected final KeyedLockContainer<Long> saveLockContainer = new KeyedLockContainer<>();
	
	/** how long a {@link TSaveContainer} should have lived before being eligible for automatic flushing. */
	protected final int saveDelayInMs;
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	static
	{
		BACKGROUND_CLEAN_UP_THREAD.execute(() -> runCleanupLoop());
	}
	
	public AbstractDelayedSaveCache(int saveDelayInMs)
	{
		// we can't clean items faster than the cleanup timer fires
		if (saveDelayInMs < CLEANUP_CHECK_TIME_IN_MS)
		{
			LOGGER.warn("The save delay ["+saveDelayInMs+"] shouldn't be less than the cleanup check timer interval ["+CLEANUP_CHECK_TIME_IN_MS+"].");
		}
		this.saveDelayInMs = saveDelayInMs;
		
		
		SAVE_CACHE_SET.add(new WeakReference<>(this));
	}
	
	//endregion
	
	
	
	//==================//
	// abstract methods //
	//==================//
	//region
	
	protected abstract TSaveContainer createEmptySaveObjContainer(long inputPos);
	
	/** when this method is called the {@link TSaveContainer} should no longer be in the memory cache */
	protected abstract void handleDataSourceRemoval(@NotNull TSaveContainer saveContainer);
	
	//endregion
	
	
	
	//==============//
	// update queue //
	//==============//
	//region
	
	/**
	 * Writing into memory is done synchronously so inputDataSource can 
	 * be closed after this method finishes.
	 * 
	 * @param inputObj whether or not this can be null will depend on the specific implementation of this class.
	 * 
	 * @return the new (or pre-existing) {@link TSaveContainer} so child objects can modify it if needed
	 */
	public TSaveContainer writeToMemoryAndQueueSave(long inputPos, @Nullable TSaveObj inputObj)
	{
		ReentrantLock lockForPos = this.saveLockContainer.getLockForPos(inputPos);
		try
		{
			lockForPos.lock();
			
			TSaveContainer container = this.saveContainerByPosition.get(inputPos);
			if (container == null)
			{
				// no data currently in the memory cache for this position
				container = this.createEmptySaveObjContainer(inputPos);
				TSaveContainer oldContainer = this.saveContainerByPosition.put(inputPos, container);
				if (oldContainer != null)
				{
					// shouldn't happen, but just in case
					this.handleDataSourceRemoval(oldContainer);
				}
			}
			
			// write the new data into memory
			container.update(inputObj);
			// keep track of when the last time we saved something was
			container.updateLastWrittenTimestamp();
			
			return container;
		}
		finally
		{
			lockForPos.unlock();
		}
	}
	
	//endregion
	
	
	
	//==============//
	// List methods //
	//==============//
	//region
	
	public int getUnsavedCount() { return this.saveContainerByPosition.size(); }
	
	//endregion
	
	
	
	//===============//
	// cleanup/flush //
	//===============//
	//region
	
	public void flush() { this.cleanUp(true); }
	/** Removes everything from the memory cache and fires the {@link AbstractDelayedSaveCache#handleDataSourceRemoval} for each. */
	public void cleanUp(boolean flushAll)
	{
		Enumeration<Long> keyIterator = this.saveContainerByPosition.keys();
		
		while (keyIterator.hasMoreElements())
		{
			Long pos = keyIterator.nextElement();
			ReentrantLock posLock = this.saveLockContainer.getLockForPos(pos);
			try
			{
				posLock.lock();
				
				TSaveContainer savedContainer = this.saveContainerByPosition.get(pos);
				if (savedContainer != null)
				{
					if (flushAll
						|| savedContainer.hasTimedOut(this.saveDelayInMs))
					{
						this.handleDataSourceRemoval(savedContainer);
						this.saveContainerByPosition.remove(pos);
					}
				}
			}
			finally
			{
				posLock.unlock();
			}
		}
	}
	
	private static void runCleanupLoop()
	{
		while (true)
		{
			try
			{
				try
				{
					Thread.sleep(CLEANUP_CHECK_TIME_IN_MS);
				}
				catch (InterruptedException ignore) { }
				
				SAVE_CACHE_SET.forEach((cacheRef) ->
				{
					AbstractDelayedSaveCache<?,?> cache = cacheRef.get();
					if (cache == null)
					{
						// shouldn't be necessary, but if we forget to manually close a cache, this will prevent leaking
						SAVE_CACHE_SET.remove(cacheRef);
					}
					else
					{
						cache.cleanUp(false);
					}
				});
			}
			catch (Exception e)
			{
				LOGGER.error("Unexpected error in cleanup thread: [" + e.getMessage() + "].", e);
			}
		}
	}
	
	//endregion
	
	
	
	//================//
	// base overrides //
	//================//
	//region
	
	@Override
	public void close()
	{
		// not the fastest way to handle removing,
		// but we shouldn't have more than 20 or so at once
		// so this should be just fine
		SAVE_CACHE_SET.removeIf((cacheRef) -> 
		{
			AbstractDelayedSaveCache<?,?> cache = cacheRef.get();
			return cache != null && cache.equals(this);
		});
	}
	
	//endregion
	
	
	
}
