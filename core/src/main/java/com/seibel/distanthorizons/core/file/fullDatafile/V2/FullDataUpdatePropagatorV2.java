package com.seibel.distanthorizons.core.file.fullDatafile.V2;

import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiWorldGenerationStep;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dataObjects.fullData.sources.FullDataSourceV2;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.file.fullDatafile.GeneratedFullDataSourceProvider;
import com.seibel.distanthorizons.core.generation.queues.IFullDataSourceRetrievalQueue;
import com.seibel.distanthorizons.core.generation.tasks.DataSourceRetrievalResult;
import com.seibel.distanthorizons.core.level.IDhClientLevel;
import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos;
import com.seibel.distanthorizons.core.render.renderer.AbstractDebugWireframeRenderer;
import com.seibel.distanthorizons.core.render.renderer.IDebugRenderable;
import com.seibel.distanthorizons.core.util.ExceptionUtil;
import com.seibel.distanthorizons.core.util.ThreadUtil;
import com.seibel.distanthorizons.core.util.threading.PriorityTaskPicker;
import com.seibel.distanthorizons.core.util.threading.ThreadPoolUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftSharedWrapper;
import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class FullDataUpdatePropagatorV2 implements IDebugRenderable, AutoCloseable
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	private static final IMinecraftClientWrapper MC_CLIENT = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	private static final IMinecraftSharedWrapper MC_SHARED = SingletonInjector.INSTANCE.get(IMinecraftSharedWrapper.class);
	
	/** indicates how long the update queue thread should wait between queuing ticks */
	protected static final int PROPAGATE_QUEUE_THREAD_DELAY_IN_MS = 250;
	
	public static final int NUMBER_OF_PARENT_UPDATE_TASKS_PER_THREAD = 10;
	
	/** how many parent update tasks can be in the queue at once */
	public static int getMaxPropagateTaskCount() { return NUMBER_OF_PARENT_UPDATE_TASKS_PER_THREAD * Config.Common.MultiThreading.numberOfThreads.get(); }
	
	
	
	/**
	 * Tracks which positions are currently being updated
	 * to prevent duplicate concurrent updates.
	 */
	private final Set<Long> updatingPosSet = ConcurrentHashMap.newKeySet();
	private final Set<Long> generatingPosSet = ConcurrentHashMap.newKeySet();
	
	/**
	 * It'd be better if we could be told when changes are available,
	 * then run the update thread, but having a constantly running background
	 * thread is simpler to deal with and gets the job done.
	 */
	public final ThreadPoolExecutor updateQueueProcessor;
	
	private final IDhLevel dhLevel;
	
	
	private final FullDataSourceProviderV2 provider;
	private final FullDataUpdaterV2 dataUpdater;
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	public FullDataUpdatePropagatorV2(FullDataSourceProviderV2 provider, FullDataUpdaterV2 dataUpdater, IDhLevel dhLevel)
	{
		this.provider = provider;
		this.dataUpdater = dataUpdater;
		
		this.dhLevel = dhLevel;
		
		// update propagation doesn't need to be run on the server since only the highest detail level is needed
		this.updateQueueProcessor = ThreadUtil.makeSingleThreadPool("Update Propagate Queue [" + dhLevel.getLevelWrapper().getDhIdentifier() + "]");
		this.updateQueueProcessor.execute(this::runUpdateQueue);
	}
	
	//endregion
	
	
	
	//================//
	// parent updates //
	//================//
	//region
	
	private void runUpdateQueue()
	{
		while (!Thread.interrupted())
		{
			try
			{
				Thread.sleep(PROPAGATE_QUEUE_THREAD_DELAY_IN_MS);
				
				PriorityTaskPicker.Executor executor = ThreadPoolUtil.getUpdatePropagatorExecutor();
				if (executor == null 
					|| executor.isTerminated())
				{
					continue;
				}
				
				// update positions closest to the player (if not on a server)
				// to make world gen appear faster
				DhBlockPos targetBlockPos = DhBlockPos.ZERO;
				if (MC_CLIENT != null 
					&& MC_CLIENT.playerExists())
				{
					targetBlockPos = MC_CLIENT.getPlayerBlockPos();
				}
				
				this.runParentUpdates(executor, targetBlockPos);
				
				this.runChildUpdates(executor, targetBlockPos);
				
				this.queueRegeneration(executor, targetBlockPos);
			}
			catch (InterruptedException ignored)
			{
				Thread.currentThread().interrupt();
			}
			catch (Exception e)
			{
				LOGGER.error("Unexpected error in the parent update queue thread. Error: " + e.getMessage(), e);
			}
		}
	}
	
	private void runParentUpdates(PriorityTaskPicker.Executor executor, DhBlockPos targetBlockPos)
	{
		int maxUpdateTaskCount = getMaxPropagateTaskCount();
		
		// queue parent updates
		if (executor.getQueueSize() > maxUpdateTaskCount
			|| this.updatingPosSet.size() > maxUpdateTaskCount)
		{
			return;
		}
		
		
		// get the positions that need to be applied to their parents
		LongArrayList parentUpdatePosList = this.provider.repo.getPositionsToUpdate(targetBlockPos.getX(), targetBlockPos.getZ(), maxUpdateTaskCount);
		
		// combine updates together based on their parent
		HashMap<Long, HashSet<Long>> updatePosByParentPos = new HashMap<>();
		for (Long pos : parentUpdatePosList)
		{
			updatePosByParentPos.compute(DhSectionPos.getParentPos(pos), (parentPos, updatePosSet) ->
			{
				if (updatePosSet == null)
				{
					updatePosSet = new HashSet<>();
				}
				updatePosSet.add(pos);
				return updatePosSet;
			});
		}
		
		// queue the updates
		for (Long parentUpdatePos : updatePosByParentPos.keySet())
		{
			// stop if there are already a bunch of updates queued
			if (this.updatingPosSet.size() > maxUpdateTaskCount
				|| executor.getQueueSize() > maxUpdateTaskCount)
			{
				break;
			}
			
			// skip any already-queued positions
			if (!this.updatingPosSet.add(parentUpdatePos))
			{
				continue;
			}
			
			try
			{
				executor.execute(() ->
				{
					ReentrantLock parentWriteLock = this.dataUpdater.updateLockProvider.getLock(parentUpdatePos);
					boolean parentLocked = false;
					try
					{
						// Locking the parent before the children should prevent deadlocks.
						// TryLock is used instead of lock so this thread can handle a different update.
						if (!parentWriteLock.tryLock())
						{
							return;
						}
						
						parentLocked = true;
						this.dataUpdater.lockedPosSet.add(parentUpdatePos);
						
						try (FullDataSourceV2 parentDataSource = this.provider.get(parentUpdatePos))
						{
							// will return null if the file handler is shutting down
							if (parentDataSource == null)
							{
								return;
							}
							
							// apply each child pos to the parent
							for (Long childPos : updatePosByParentPos.get(parentUpdatePos))
							{
								ReentrantLock childReadLock = this.dataUpdater.updateLockProvider.getLock(childPos);
								try
								{
									childReadLock.lock();
									this.dataUpdater.lockedPosSet.add(childPos);
									
									try (FullDataSourceV2 childDataSource = this.provider.get(childPos))
									{
										// can return null when the file handler is being shut down
										if (childDataSource != null)
										{
											parentDataSource.updateFromDataSource(childDataSource);
										}
									}
								}
								catch (Exception e)
								{
									LOGGER.error("Unexpected in parent update propagation for parent pos: ["+DhSectionPos.toString(parentUpdatePos)+"], child pos: [" + DhSectionPos.toString(parentUpdatePos) + "], Error: [" + e.getMessage() + "].", e);
								}
								finally
								{
									this.provider.repo.setApplyToParent(childPos, false);
									
									childReadLock.unlock();
									this.dataUpdater.lockedPosSet.remove(childPos);
								}
							}
							
							// don't modify other update propagator flags
							{
								parentDataSource.applyToChildren = null;
								parentDataSource.regenerate = null;
							}
							
							this.dataUpdater.updateDataSource(parentDataSource);
						}
					}
					finally
					{
						if (parentLocked)
						{
							parentWriteLock.unlock();
							this.dataUpdater.lockedPosSet.remove(parentUpdatePos);
						}
						
						this.updatingPosSet.remove(parentUpdatePos);
					}
				});
			}
			catch (RejectedExecutionException ignore)
			{ /* the executor was shut down, it should be back up shortly and able to accept new jobs */ }
			catch (Exception e)
			{
				this.updatingPosSet.remove(parentUpdatePos);
				throw e;
			}
		}
	}
	
	private void runChildUpdates(PriorityTaskPicker.Executor executor, DhBlockPos targetBlockPos)
	{
		int maxUpdateTaskCount = getMaxPropagateTaskCount();
		
		// queue child updates
		if (executor.getQueueSize() < maxUpdateTaskCount
			&& this.updatingPosSet.size() < maxUpdateTaskCount)
		{
			// get the positions that need to be applied to their children
			LongArrayList updatePosList = this.provider.repo.getChildPositionsToUpdate(targetBlockPos.getX(), targetBlockPos.getZ(), maxUpdateTaskCount);
			
			// queue the updates
			for (long updatePos : updatePosList)
			{
				// stop if there are already a bunch of updates queued
				if (this.updatingPosSet.size() > maxUpdateTaskCount
					|| executor.getQueueSize() > maxUpdateTaskCount)
				{
					break;
				}
				
				// skip already updating positions
				if (!this.updatingPosSet.add(updatePos))
				{
					continue;
				}
				
				
				try
				{
					executor.execute(() ->
					{
						// Don't downsample extremely large LODs
						// since they'll take a long time and take up a lot of disk space
						// that may not be needed.
						// If they are needed at a future time, they can be generated.
						if (DhSectionPos.getDetailLevel(updatePos) >= DhSectionPos.SECTION_MINIMUM_DETAIL_LEVEL + 6) // LOD 1 datapoint 64 blocks wide, 4096 total blocks wide
						{
							this.provider.repo.setApplyToChild(updatePos, false);
							this.updatingPosSet.remove(updatePos);
							return;
						}
						
						ReentrantLock parentReadLock = this.dataUpdater.updateLockProvider.getLock(updatePos);
						boolean parentLocked = false;
						try
						{
							// Locking the parent before the children should prevent deadlocks.
							// TryLock is used instead of lock so this thread can handle a different update.
							if (!parentReadLock.tryLock())
							{
								return;
							}
							
							
							parentLocked = true;
							this.dataUpdater.lockedPosSet.add(updatePos);
							
							try (FullDataSourceV2 parentDataSource = this.provider.get(updatePos))
							{
								// will return null if the file handler is shutting down
								if (parentDataSource == null)
								{
									return;
								}
								
								
								
								// apply parent to each child
								for (int i = 0; i < 4; i++)
								{
									long childPos = DhSectionPos.getChildByIndex(updatePos, i);
									
									ReentrantLock childWriteLock = this.dataUpdater.updateLockProvider.getLock(childPos);
									try
									{
										childWriteLock.lock();
										this.dataUpdater.lockedPosSet.add(childPos);
										
										try (FullDataSourceV2 childDataSource = this.provider.get(childPos))
										{
											// will return null if the file handler is shutting down
											if (childDataSource == null)
											{
												continue;
											}
											
											childDataSource.updateFromDataSource(parentDataSource);
											
											// don't modify other update propagator flags
											{
												childDataSource.applyToParent = null;
												childDataSource.regenerate = null;
											}
											
											this.dataUpdater.updateDataSource(childDataSource);
											
											if (DhSectionPos.getDetailLevel(childPos) == DhSectionPos.SECTION_BLOCK_DETAIL_LEVEL)
											{
												this.provider.repo.setRegenerate(childPos, true);
											}
											
										}
									}
									catch (Exception e)
									{
										LOGGER.error("Unexpected in child update propagation for parent pos: ["+DhSectionPos.toString(updatePos)+"], child pos: [" + DhSectionPos.toString(updatePos) + "], Error: [" + e.getMessage() + "].", e);
									}
									finally
									{
										this.provider.repo.setApplyToChild(updatePos, false);
										
										childWriteLock.unlock();
										this.dataUpdater.lockedPosSet.remove(childPos);
									}
								}
							}
						}
						finally
						{
							if (parentLocked)
							{
								parentReadLock.unlock();
								this.dataUpdater.lockedPosSet.remove(updatePos);
							}
							
							this.updatingPosSet.remove(updatePos);
						}
					});
				}
				catch (RejectedExecutionException ignore)
				{ /* the executor was shut down, it should be back up shortly and able to accept new jobs */ }
				catch (Exception e)
				{
					this.updatingPosSet.remove(updatePos);
					throw e;
				}
			}
		}
	}
	
	private void queueRegeneration(PriorityTaskPicker.Executor executor, DhBlockPos targetBlockPos)
	{
		boolean canQueueRegen = false;
		if (MC_SHARED.isDedicatedServer())
		{
			// dedicated servers can always generate chunks
			canQueueRegen = true;
		}
		else if (this.dhLevel instanceof IDhClientLevel)
		{
			// singleplayer should only generate in levels that are actively being rendered
			canQueueRegen = ((IDhClientLevel)this.dhLevel).isRendering();
		}
		
		if (!canQueueRegen)
		{
			return;
		}
		
		
		
		int maxUpdateTaskCount = getMaxPropagateTaskCount();
		
		// queue child updates
		if (executor.getQueueSize() < maxUpdateTaskCount
			&& this.updatingPosSet.size() < maxUpdateTaskCount
			&& this.generatingPosSet.size() < maxUpdateTaskCount)
		{
			// get the positions that need to be regenerated
			LongArrayList updatePosList = this.provider.repo.getChildPositionsToRegen(targetBlockPos.getX(), targetBlockPos.getZ(), maxUpdateTaskCount);
			
			// queue the updates
			for (long updatePos : updatePosList)
			{
				this.tryQueueWorldGenTask(updatePos);
			}
		}
	}
	private void tryQueueWorldGenTask(long updatePos)
	{
		if (!(this.provider instanceof GeneratedFullDataSourceProvider))
		{
			return;
		}
		
		if (!this.provider.canQueueRetrievalNow())
		{
			return;
		}
		
		if (this.generatingPosSet.contains(updatePos))
		{
			return;
		}
		
		
		// TODO common method needed
		int maxQueueCount = GeneratedFullDataSourceProvider.MAX_WORLD_GEN_REQUESTS_PER_THREAD * Config.Common.MultiThreading.numberOfThreads.get();
		maxQueueCount /= 2;
		
		GeneratedFullDataSourceProvider genProvider = ((GeneratedFullDataSourceProvider)this.provider);
		IFullDataSourceRetrievalQueue queue = genProvider.worldGenQueueRef.get();
		if (queue == null
			|| queue.getQueuedChunkCount() > maxQueueCount)
		{
			return;
		}
		
		
		
		// just generate highest detail
		LongArrayList posToGen = genProvider.getPositionsToRetrieve(updatePos, (byte)DhSectionPos.SECTION_BLOCK_DETAIL_LEVEL, EDhApiWorldGenerationStep.FEATURES);
		if (posToGen == null)
		{
			return;
		}
		
		if (posToGen.size() == 0)
		{
			// position is already generated
			this.provider.repo.setRegenerate(updatePos, false);
			return;
		}
		
		
		
		if (!this.generatingPosSet.add(updatePos))
		{
			return;
		}
		
		CompletableFuture<DataSourceRetrievalResult>[] futureArray = new CompletableFuture[posToGen.size()];
		for (int i = 0; i < posToGen.size(); i++)
		{
			long genPos = posToGen.getLong(i);
			futureArray[i] = genProvider.queuePositionForRetrieval(genPos); // this can cause the same positions to be queued multiple times, we handle that sanely down stream although it's a bit strange that it happens at all
		}
		
		CompletableFuture.allOf(futureArray)
			.handle((voidObj, throwable) -> 
			{
				this.generatingPosSet.remove(updatePos);
				
				if (throwable != null 
					&& !ExceptionUtil.isShutdownException(throwable))
				{
					LOGGER.error("Unexpected error on Update gen future: ["+throwable.getMessage()+"].", throwable);
				}
				
				
				boolean finishedGenerating = true;
				for (int i = 0; i < futureArray.length; i++)
				{
					try
					{
						DataSourceRetrievalResult result = futureArray[i].getNow(null);
						if (result == null
							|| result.dataSource == null)
						{
							finishedGenerating = false;
							break;
						}
					}
					catch (Exception e)
					{
						// completed exceptionally
						finishedGenerating = false;
						break;
					}
				}
				
				if (finishedGenerating)
				{
					this.provider.repo.setRegenerate(updatePos, false);
				}
				
				return null;
			});
	}
	
	//endregion
	
	
	
	//===========//
	// overrides //
	//===========//
	//region
	
	@Override
	public void debugRender(AbstractDebugWireframeRenderer renderer)
	{
		this.updatingPosSet
				.forEach((pos) -> { renderer.renderBox(new AbstractDebugWireframeRenderer.Box(pos, -32f, 80f, 0.20f, Color.MAGENTA)); });
		
		this.generatingPosSet
				.forEach((pos) -> { renderer.renderBox(new AbstractDebugWireframeRenderer.Box(pos, -32f, 80f, 0.20f, Color.CYAN)); });
	}
	
	@Override
	public void close()
	{
		if (this.updateQueueProcessor != null)
		{
			this.updateQueueProcessor.shutdownNow();
		}
	}
	
	//endregion
	
	
	
}
