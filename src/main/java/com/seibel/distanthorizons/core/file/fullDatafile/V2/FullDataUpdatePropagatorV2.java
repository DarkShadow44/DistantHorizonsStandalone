package com.seibel.distanthorizons.core.file.fullDatafile.V2;

import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dataObjects.fullData.sources.FullDataSourceV2;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos;
import com.seibel.distanthorizons.core.render.renderer.AbstractDebugWireframeRenderer;
import com.seibel.distanthorizons.core.render.renderer.IDebugRenderable;
import com.seibel.distanthorizons.core.util.ThreadUtil;
import com.seibel.distanthorizons.core.util.threading.PriorityTaskPicker;
import com.seibel.distanthorizons.core.util.threading.ThreadPoolUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class FullDataUpdatePropagatorV2 implements IDebugRenderable, AutoCloseable
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	private static final IMinecraftClientWrapper MC_CLIENT = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	
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
	
	/**
	 * It'd be better if we could be told when changes are available,
	 * then run the update thread, but having a constantly running background
	 * thread is simpler to deal with and gets the job done.
	 */
	public final ThreadPoolExecutor updateQueueProcessor;
	
	private final AtomicBoolean isShutdownRef = new AtomicBoolean(false);
	private final String levelId;
	
	
	private final FullDataSourceProviderV2 provider;
	private final FullDataUpdaterV2 dataUpdater;
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	public FullDataUpdatePropagatorV2(FullDataSourceProviderV2 provider, FullDataUpdaterV2 dataUpdater, String levelId)
	{
		this.provider = provider;
		this.dataUpdater = dataUpdater;
		this.levelId = levelId;
		
		// update propagation doesn't need to be run on the server since only the highest detail level is needed
		this.updateQueueProcessor = ThreadUtil.makeSingleThreadPool("Update Propagate Queue [" + this.levelId + "]");
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
				if (executor == null || executor.isTerminated())
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
				
				if (Config.Common.LodBuilding.Experimental.upsampleLowerDetailLodsToFillHoles.get())
				{
					this.runChildUpdates(executor, targetBlockPos);
				}
				
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
	/** will always apply updates */
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
						//LOGGER.info("updating parent: "+parentUpdatePos);
						
						// Locking the parent before the children should prevent deadlocks.
						// TryLock is used instead of lock so this thread can handle a different update.
						if (parentWriteLock.tryLock())
						{
							parentLocked = true;
							this.dataUpdater.lockedPosSet.add(parentUpdatePos);
							
							try (FullDataSourceV2 parentDataSource = this.provider.get(parentUpdatePos))
							{
								// will return null if the file handler is shutting down
								if (parentDataSource != null)
								{
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
									
									
									if (DhSectionPos.getDetailLevel(parentUpdatePos) < FullDataSourceProviderV2.ROOT_SECTION_DETAIL_LEVEL)
									{
										parentDataSource.applyToParent = true;
									}
									
									this.dataUpdater.updateDataSource(parentDataSource);
								}
							}
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
	/** stops if it finds any LOD data */
	private void runChildUpdates(PriorityTaskPicker.Executor executor, DhBlockPos targetBlockPos)
	{
		int maxUpdateTaskCount = getMaxPropagateTaskCount();
		
		// queue child updates
		if (executor.getQueueSize() < maxUpdateTaskCount
			&& this.updatingPosSet.size() < maxUpdateTaskCount)
		{
			// get the positions that need to be applied to their children
			LongArrayList childUpdatePosList = this.provider.repo.getChildPositionsToUpdate(targetBlockPos.getX(), targetBlockPos.getZ(), maxUpdateTaskCount);
			
			// queue the updates
			for (long parentUpdatePos : childUpdatePosList)
			{
				// stop if there are already a bunch of updates queued
				if (this.updatingPosSet.size() > maxUpdateTaskCount
					|| executor.getQueueSize() > maxUpdateTaskCount)
				{
					break;
				}
				
				// skip already updating positions
				if (!this.updatingPosSet.add(parentUpdatePos))
				{
					continue;
				}
				
				try
				{
					executor.execute(() ->
					{
						ReentrantLock parentReadLock = this.dataUpdater.updateLockProvider.getLock(parentUpdatePos);
						boolean parentLocked = false;
						try
						{
							//LOGGER.info("updating parent: "+parentUpdatePos);
							
							// Locking the parent before the children should prevent deadlocks.
							// TryLock is used instead of lock so this thread can handle a different update.
							if (parentReadLock.tryLock())
							{
								parentLocked = true;
								this.dataUpdater.lockedPosSet.add(parentUpdatePos);
								
								try (FullDataSourceV2 parentDataSource = this.provider.get(parentUpdatePos))
								{
									// will return null if the file handler is shutting down
									if (parentDataSource != null)
									{
										// apply parent to each child
										for (int i = 0; i < 4; i++)
										{
											long childPos = DhSectionPos.getChildByIndex(parentUpdatePos, i);
											
											ReentrantLock childWriteLock = this.dataUpdater.updateLockProvider.getLock(childPos);
											try
											{
												childWriteLock.lock();
												this.dataUpdater.lockedPosSet.add(childPos);
												
												try (FullDataSourceV2 childDataSource = this.provider.get(childPos))
												{
													// will return null if the file handler is shutting down
													if (childDataSource != null)
													{
														childDataSource.updateFromDataSource(parentDataSource);
														
														// don't propagate child updates past the bottom of the tree
														if (DhSectionPos.getDetailLevel(childPos) != DhSectionPos.SECTION_BLOCK_DETAIL_LEVEL)
														{
															childDataSource.applyToChildren = true;
														}
														
														this.dataUpdater.updateDataSource(childDataSource);
													}
												}
											}
											catch (Exception e)
											{
												LOGGER.error("Unexpected in child update propagation for parent pos: ["+DhSectionPos.toString(parentUpdatePos)+"], child pos: [" + DhSectionPos.toString(parentUpdatePos) + "], Error: [" + e.getMessage() + "].", e);
											}
											finally
											{
												this.provider.repo.setApplyToChild(parentUpdatePos, false);
												
												childWriteLock.unlock();
												this.dataUpdater.lockedPosSet.remove(childPos);
											}
										}
									}
								}
							}
						}
						finally
						{
							if (parentLocked)
							{
								parentReadLock.unlock();
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
