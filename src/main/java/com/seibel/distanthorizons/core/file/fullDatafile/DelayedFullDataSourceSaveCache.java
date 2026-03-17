package com.seibel.distanthorizons.core.file.fullDatafile;

import com.seibel.distanthorizons.core.dataObjects.fullData.sources.FullDataSourceV2;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.util.KeyedLockContainer;
import com.seibel.distanthorizons.core.util.ThreadUtil;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Used to batch together multiple data source updates that all
 * affect the same position.
 */
public class DelayedFullDataSourceSaveCache implements AutoCloseable
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	/** 
	 * a cache won't automatically clean itself unless we trigger it's clean method
	 * if not done then we'd only see the cache invalidate when new inserts happen,
	 * which causes weird behavior when placing/breaking blocks.
	 */
	private static final ThreadPoolExecutor BACKGROUND_CLEAN_UP_THREAD = ThreadUtil.makeSingleDaemonThreadPool("delayed save cache cleaner");
	private static final Set<WeakReference<DelayedFullDataSourceSaveCache>> SAVE_CACHE_SET = Collections.newSetFromMap(new ConcurrentHashMap<>());
	/** how long between clean up checks */
	private static final int CLEANUP_CHECK_TIME_IN_MS = 1_000;
	
	
	
	private final ConcurrentHashMap<Long, DataSourceSavedTimePair> dataSourceByPosition = new ConcurrentHashMap<Long, DataSourceSavedTimePair>();
	
	/* don't let two threads load the same position at the same time */
	protected final KeyedLockContainer<Long> saveLockContainer = new KeyedLockContainer<>();
	
	private final ISaveDataSourceFunc onSaveTimeoutAsyncFunc;
	private final int saveDelayInMs;
	
	
	
	//=============//
	// constructor //
	//=============//
	
	static
	{
		BACKGROUND_CLEAN_UP_THREAD.execute(() -> runCleanupLoop());
	}
	
	public DelayedFullDataSourceSaveCache(@NotNull ISaveDataSourceFunc onSaveTimeoutAsyncFunc, int saveDelayInMs)
	{
		this.onSaveTimeoutAsyncFunc = onSaveTimeoutAsyncFunc;
		
		// we can't clean items faster than the cleanup timer fires
		if (saveDelayInMs < CLEANUP_CHECK_TIME_IN_MS)
		{
			LOGGER.warn("The save delay ["+saveDelayInMs+"] shouldn't be less than the cleanup check timer interval ["+CLEANUP_CHECK_TIME_IN_MS+"].");
		}
		this.saveDelayInMs = saveDelayInMs;
		
		
		SAVE_CACHE_SET.add(new WeakReference<>(this));
	}
	
	
	
	//==============//
	// update queue //
	//==============//
	
	/**
	 * Writing into memory is done synchronously so inputDataSource can 
	 * be closed after this method finishes.
	 */
	public void writeDataSourceToMemoryAndQueueSave(@NotNull FullDataSourceV2 inputDataSource)
	{
		long inputPos = inputDataSource.getPos();
		
		ReentrantLock lockForPos = this.saveLockContainer.getLockForPos(inputPos);
		try
		{
			lockForPos.lock();
			
			FullDataSourceV2 memoryDataSource;
			
			DataSourceSavedTimePair pair = this.dataSourceByPosition.getOrDefault(inputPos, null);
			if (pair == null)
			{
				// no data currently in the memory cache for this position
				memoryDataSource = FullDataSourceV2.createEmpty(inputPos);
				pair = new DataSourceSavedTimePair(memoryDataSource);
				this.dataSourceByPosition.put(inputPos, pair);
			}
			else
			{
				memoryDataSource = pair.dataSource;
			}
			
			// write the new data into memory
			memoryDataSource.update(inputDataSource);
			// keep track of when the last time we saved something was
			pair.updateLastWrittenTimestamp();
		}
		finally
		{
			lockForPos.unlock();
		}
	}
	
	/** when this method is called the datasource should no longer be in the memory cache */
	public void handleDataSourceRemoval(@NotNull FullDataSourceV2 removedDataSource)
	{
		this.onSaveTimeoutAsyncFunc.saveAsync(removedDataSource)
			.handle((voidObj, throwable) ->
			{
				try
				{
					// if this close method is fired multiple times
					// monoliths can appear due to concurrent writing to the
					// backend arrays
					removedDataSource.close();
				}
				catch (Exception e)
				{
					LOGGER.error("Unable to close datasource ["+ DhSectionPos.toString(removedDataSource.getPos()) +"], error: ["+e.getMessage()+"].", e);
				}
				
				return null;
			});
	}
	
	
	
	//==============//
	// List methods //
	//==============//
	
	public int getUnsavedCount() { return this.dataSourceByPosition.size(); }
	
	public void flush() { this.cleanUp(true); }
	/** Removes everything from the memory cache and fires the {@link DelayedFullDataSourceSaveCache#onSaveTimeoutAsyncFunc} for each. */
	public void cleanUp(boolean flushAll)
	{
		Enumeration<Long> keyIterator = this.dataSourceByPosition.keys();
		while (keyIterator.hasMoreElements())
		{
			Long pos = keyIterator.nextElement();
			ReentrantLock posLock = this.saveLockContainer.getLockForPos(pos);
			try
			{
				posLock.lock();
				
				DataSourceSavedTimePair savedPair = this.dataSourceByPosition.getOrDefault(pos, null);
				if (savedPair != null)
				{
					if (flushAll
						|| savedPair.dataSourceHasTimedOut(this.saveDelayInMs))
					{
						this.dataSourceByPosition.remove(pos);
						this.handleDataSourceRemoval(savedPair.dataSource);
					}
				}
			}
			finally
			{
				posLock.unlock();
			}
		}
	}
	
	
	
	//================//
	// static cleanup //
	//================//
	
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
					DelayedFullDataSourceSaveCache cache = cacheRef.get();
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
	
	
	
	//================//
	// base overrides //
	//================//
	
	@Override
	public void close()
	{
		// not the fastest way to handle removing,
		// but we shouldn't have more than 20 or so at once
		// so this should be just fine
		SAVE_CACHE_SET.removeIf((cacheRef) -> 
		{
			DelayedFullDataSourceSaveCache cache = cacheRef.get();
			return cache != null && cache.equals(this);
		});
	}
	
	
	
	//================//
	// helper classes //
	//================//
	
	@FunctionalInterface
	public interface ISaveDataSourceFunc
	{
		/** called after the timeout expires */
		CompletableFuture<Void> saveAsync(FullDataSourceV2 inputDataSource);
	}
	
	/** 
	 * used to keep track of when data sources
	 * were written to so we can flush them once
	 * enough time has passed.
	 */
	private static class DataSourceSavedTimePair
	{
		@NotNull
		public final FullDataSourceV2 dataSource;
		/** the last unix millisecond time this data source was written to */
		public long lastWrittenDateTimeMs;
		
		
		public DataSourceSavedTimePair(@NotNull FullDataSourceV2 dataSource)
		{
			this.dataSource = dataSource;
			this.lastWrittenDateTimeMs = System.currentTimeMillis();
		}
		
		
		public void updateLastWrittenTimestamp()
		{ this.lastWrittenDateTimeMs = System.currentTimeMillis(); }
		
		public boolean dataSourceHasTimedOut(long msTillTimeout)
		{
			long currentTime = System.currentTimeMillis();
			long timeSinceUpdate = currentTime - this.lastWrittenDateTimeMs;
			return (timeSinceUpdate > msTillTimeout);
		}
	}
	
	
}
