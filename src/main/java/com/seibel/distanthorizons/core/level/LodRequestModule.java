/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020 James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.distanthorizons.core.level;

import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiDistantGeneratorProgressDisplayLocation;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.file.fullDatafile.GeneratedFullDataSourceProvider;
import com.seibel.distanthorizons.core.generation.IFullDataSourceRetrievalQueue;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.logging.f3.F3Screen;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos2D;
import com.seibel.distanthorizons.core.util.FormatUtil;
import com.seibel.distanthorizons.core.util.ThreadUtil;
import com.seibel.distanthorizons.core.util.objects.RollingAverage;
import com.seibel.distanthorizons.core.util.threading.PriorityTaskPicker;
import com.seibel.distanthorizons.core.util.threading.ThreadPoolUtil;
import com.seibel.distanthorizons.core.world.DhApiWorldProxy;
import com.seibel.distanthorizons.core.logging.DhLogger;

import java.io.Closeable;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Handles both single-player/server-side world gen and client side LOD requests.
 */
public class LodRequestModule implements Closeable
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	private final GeneratedFullDataSourceProvider.IOnWorldGenCompleteListener onWorldGenCompleteListener;
	private final ThreadPoolExecutor tickerThread;
	
	private final GeneratedFullDataSourceProvider dataSourceProvider;
	private final Supplier<? extends AbstractLodRequestState> worldGenStateSupplier;
	
	private final AtomicReference<AbstractLodRequestState> lodRequestStateRef = new AtomicReference<>();
	
	
	
	//=============//
	// constructor //
	//=============//
	
	public LodRequestModule(
			IDhLevel level,
			GeneratedFullDataSourceProvider.IOnWorldGenCompleteListener onWorldGenCompleteListener,
			GeneratedFullDataSourceProvider dataSourceProvider,
			Supplier<? extends AbstractLodRequestState> worldGenStateSupplier
		)
	{
		this.onWorldGenCompleteListener = onWorldGenCompleteListener;
		this.dataSourceProvider = dataSourceProvider;
		this.worldGenStateSupplier = worldGenStateSupplier;
		
		String levelId = level.getLevelWrapper().getDhIdentifier();
		this.tickerThread = ThreadUtil.makeSingleDaemonThreadPool("Request Module Ticker ["+levelId+"]");
		this.tickerThread.execute(this::tickLoop);
	}
	
	
	
	//=========//
	// ticking //
	//=========//
	
	private void tickLoop()
	{
		try
		{
			while (!Thread.interrupted())
			{
				Thread.sleep(20);
				this.tick();
			}
		}
		catch (InterruptedException ignore) { }
	}
	private void tick()
	{
		boolean shouldDoWorldGen = this.onWorldGenCompleteListener.shouldDoWorldGen();
		// if the world is read only don't generate anything
		shouldDoWorldGen &= !DhApiWorldProxy.INSTANCE.getReadOnly();
		
		boolean isWorldGenRunning = this.isWorldGenRunning();
		if (shouldDoWorldGen && !isWorldGenRunning)
		{
			// start world gen
			this.startWorldGen(this.dataSourceProvider, this.worldGenStateSupplier.get());
		}
		else if (!shouldDoWorldGen && isWorldGenRunning)
		{
			// stop world gen
			this.stopWorldGen(this.dataSourceProvider);
		}
		
		if (this.isWorldGenRunning())
		{
			AbstractLodRequestState lodRequestState = this.lodRequestStateRef.get();
			if (lodRequestState != null)
			{
				DhBlockPos2D targetPosForGeneration = this.onWorldGenCompleteListener.getTargetPosForGeneration();
				if (targetPosForGeneration != null)
				{
					lodRequestState.startRequestQueueAndSetTargetPos(targetPosForGeneration);
				}
			}
		}
	}
	
	
	
	//===================//
	// world gen control //
	//===================//
	
	public void startWorldGen(GeneratedFullDataSourceProvider dataFileHandler, AbstractLodRequestState newWgs)
	{
		// create the new world generator
		if (!this.lodRequestStateRef.compareAndSet(null, newWgs))
		{
			LOGGER.warn("Failed to start world gen due to concurrency");
			newWgs.closeAsync(false);
		}
		
		dataFileHandler.addWorldGenCompleteListener(this.onWorldGenCompleteListener);
		dataFileHandler.setWorldGenerationQueue(newWgs.retrievalQueue);
	}
	
	public void stopWorldGen(GeneratedFullDataSourceProvider dataFileHandler)
	{
		AbstractLodRequestState worldGenState = this.lodRequestStateRef.get();
		if (worldGenState == null)
		{
			LOGGER.warn("Attempted to stop world gen when it was not running");
			return;
		}
		
		// shut down the world generator
		while (!this.lodRequestStateRef.compareAndSet(worldGenState, null))
		{
			worldGenState = this.lodRequestStateRef.get();
			if (worldGenState == null)
			{
				return;
			}
		}
		dataFileHandler.clearRetrievalQueue();
		worldGenState.closeAsync(true).join(); //TODO: Make it async.
		dataFileHandler.removeWorldGenCompleteListener(this.onWorldGenCompleteListener);
	}
	
	
	
	//=======================//
	// base method overrides //
	//=======================//
	
	@Override
	public void close()
	{
		this.tickerThread.shutdownNow();
		
		// shutdown the world-gen
		AbstractLodRequestState worldGenState = this.lodRequestStateRef.get();
		if (worldGenState != null)
		{
			while (!this.lodRequestStateRef.compareAndSet(worldGenState, null))
			{
				worldGenState = this.lodRequestStateRef.get();
				if (worldGenState == null)
				{
					break;
				}
			}
			
			if (worldGenState != null)
			{
				worldGenState.closeAsync(true).join(); //TODO: Make it async.
			}
		}
	}
	
	
	
	//=========//
	// getters //
	//=========//
	
	public boolean isWorldGenRunning() { return this.lodRequestStateRef.get() != null; }
	
	/** mutates a list so it can be added to an existing {@link IDhLevel}'s debug list  */
	public void addDebugMenuStringsToList(List<String> messageList)
	{
		AbstractLodRequestState worldGenState = this.lodRequestStateRef.get();
		if (worldGenState == null)
		{
			return;
		}
		
		
		// estimated tasks
		String waitingCountStr = F3Screen.NUMBER_FORMAT.format(worldGenState.retrievalQueue.getWaitingTaskCount());
		String inProgressCountStr = F3Screen.NUMBER_FORMAT.format(worldGenState.retrievalQueue.getInProgressTaskCount());
		String totalCountEstimateStr = F3Screen.NUMBER_FORMAT.format(worldGenState.retrievalQueue.getRetrievalEstimatedRemainingChunkCount());
		String message = "World Gen/Import Tasks: "+waitingCountStr+"/"+totalCountEstimateStr+" (in progress "+inProgressCountStr+")";
		
		// estimated chunks/sec
		double chunksPerSec = worldGenState.getEstimatedChunksPerSecond();
		if (chunksPerSec > -1)
		{
			message += ", " + F3Screen.NUMBER_FORMAT.format(chunksPerSec) + " chunks/sec";
		}
		
		messageList.add(message);
		
		worldGenState.retrievalQueue.addDebugMenuStringsToList(messageList);
	}
	
	
	
	//================//
	// helper classes //
	//================//
	
	/** Handles the {@link IFullDataSourceRetrievalQueue} and any other necessary world gen information. */
	public static abstract class AbstractLodRequestState
	{
		/** static so we only send the disable message once per session */
		private static long firstProgressMessageSentMs = 0;
		
		public IFullDataSourceRetrievalQueue retrievalQueue;
		
		private static final ThreadPoolExecutor PROGRESS_UPDATER_THREAD = ThreadUtil.makeSingleDaemonThreadPool("World Gen Progress Updater");
		private boolean progressUpdateThreadRunning = false;
		
		
		
		/** @param targetPosForRequest the position that world generation should be centered around */
		public void startRequestQueueAndSetTargetPos(DhBlockPos2D targetPosForRequest) 
		{ 
			this.retrievalQueue.startAndSetTargetPos(targetPosForRequest);
			this.startProgressUpdateThread();
		}
		private void startProgressUpdateThread()
		{
			// only start the thread once
			if (!this.progressUpdateThreadRunning)
			{
				this.progressUpdateThreadRunning = true;
				
				PROGRESS_UPDATER_THREAD.execute(() -> 
				{
					while (this.progressUpdateThreadRunning)
					{
						try
						{
							this.sendRetrievalProgress();
							
							// sleep so we only see an update once in a while
							int sleepTimeInSec = Config.Common.WorldGenerator.generationProgressDisplayIntervalInSeconds.get();
							Thread.sleep(sleepTimeInSec * 1_000L);
						}
						catch (Exception e)
						{
							LOGGER.error("Unexpected issue displaying chunk retrieval progress [" + e.getMessage() + "].", e);
						}
					}
				});
			}
		}
		private void sendRetrievalProgress()
		{
			// format the remaining chunks
			int remainingChunkCount = this.retrievalQueue.getRetrievalEstimatedRemainingChunkCount();
			remainingChunkCount += this.retrievalQueue.getQueuedChunkCount();
			String remainingChunkCountStr = F3Screen.NUMBER_FORMAT.format(remainingChunkCount);
			
			String message = "DH is generating chunks. " + remainingChunkCountStr + " left.";
			
			// show a message about how to disable progress logging if requested
			int msToShowDisableInstructions = Config.Common.WorldGenerator.generationProgressDisableMessageDisplayTimeInSeconds.get() * 1_000;
			if (msToShowDisableInstructions > 0)
			{
				long timeSinceFirstMessageInMs = (System.currentTimeMillis() - firstProgressMessageSentMs);
				// always show this message for the first tick
				if (firstProgressMessageSentMs == 0
						// show this message if there is still time
						|| timeSinceFirstMessageInMs < msToShowDisableInstructions)
				{
					// append to the current message
					message += " This message can be hidden in the DH config.";
				}
			}
			
			// add the remaining time estimate if available
			double chunksPerSec = this.getEstimatedChunksPerSecond();
			if (chunksPerSec > 0)
			{
				long estimatedRemainingTime = (long) (remainingChunkCount / chunksPerSec);
				message += " ETA: " + FormatUtil.formatEta(Duration.ofSeconds(estimatedRemainingTime));
				
				if (Config.Common.WorldGenerator.generationProgressIncludeChunksPerSecond.get())
				{
					message += " at " + F3Screen.NUMBER_FORMAT.format(chunksPerSec) + " chunks/sec";
				}
			}
			
			// only log if there are chunks needing to be generated
			if (remainingChunkCount != 0)
			{
				// determine where to log
				EDhApiDistantGeneratorProgressDisplayLocation displayLocation = Config.Common.WorldGenerator.showGenerationProgress.get();
				if (displayLocation == EDhApiDistantGeneratorProgressDisplayLocation.OVERLAY)
				{
					ClientApi.INSTANCE.showOverlayMessageNextFrame(message);
				}
				else if (displayLocation == EDhApiDistantGeneratorProgressDisplayLocation.CHAT)
				{
					ClientApi.INSTANCE.showChatMessageNextFrame(message);
				}
				else if (displayLocation == EDhApiDistantGeneratorProgressDisplayLocation.LOG)
				{
					LOGGER.info(message);
				}
				
				
				// mark when the first message was sent
				if (firstProgressMessageSentMs == 0)
				{
					firstProgressMessageSentMs = System.currentTimeMillis();
				}
			}
		}
		
		/** @return -1 if this method isn't supported or available */
		public double getEstimatedChunksPerSecond()
		{
			RollingAverage avg = this.retrievalQueue.getRollingAverageChunkGenTimeInMs();
			if (avg == null)
			{
				return -1;
			}
			
			
			PriorityTaskPicker.Executor executor = ThreadPoolUtil.getWorldGenExecutor();
			int threadCount = 1;
			if (executor != null)
			{
				threadCount = executor.getPoolSize();
			}
			
			// convert chunk generation time in milliseconds to chunks per second
			double chunksPerSecond = (1 / avg.getAverage()) * 1_000;
			// estimate the number of chunks that can be processed per second by all threads
			// Note: this is probably higher than the actual number, we might want to drop this by 1 or 2 to give a more realistic estimate
			chunksPerSecond = threadCount * chunksPerSecond;
			
			return chunksPerSecond;
		}
		
		
		CompletableFuture<Void> closeAsync(boolean doInterrupt)
		{
			// this should stop the updater thread
			this.progressUpdateThreadRunning = false;
			
			return this.retrievalQueue.startClosingAsync(true, doInterrupt)
					.exceptionally(e ->
							{
								LOGGER.error("Error during first stage of generation queue shutdown, Error: ["+e.getMessage()+"].", e);
								return null;
							}
					).thenRun(this.retrievalQueue::close)
					.exceptionally(e ->
					{
						LOGGER.error("Error during second stage of generation queue shutdown, Error: ["+e.getMessage()+"].", e);
						return null;
					});
		}
		
		
	}
	
	
	
}
