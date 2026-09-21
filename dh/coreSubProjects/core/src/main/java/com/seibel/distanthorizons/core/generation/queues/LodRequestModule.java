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

package com.seibel.distanthorizons.core.generation.queues;

import com.seibel.distanthorizons.core.file.fullDatafile.GeneratedFullDataSourceProvider;
import com.seibel.distanthorizons.core.level.*;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.logging.f3.F3Screen;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos2D;
import com.seibel.distanthorizons.core.util.ThreadUtil;
import com.seibel.distanthorizons.core.world.DhApiWorldProxy;
import com.seibel.distanthorizons.core.logging.DhLogger;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Handles both single-player/server-side world gen and client side LOD requests.
 * 
 * @see AbstractLodRequestState
 * @see IFullDataSourceRetrievalQueue
 */
public class LodRequestModule implements Closeable
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	
	private final IDhLevel level;
	private final GeneratedFullDataSourceProvider.IOnWorldGenCompleteListener onWorldGenCompleteListener;
	private final ThreadPoolExecutor tickerThread;
	
	private final GeneratedFullDataSourceProvider dataSourceProvider;
	private final Supplier<? extends AbstractLodRequestState> worldGenStateSupplier;
	
	private final AtomicReference<AbstractLodRequestState> lodRequestStateRef = new AtomicReference<>();
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	public LodRequestModule(
			IDhLevel level,
			GeneratedFullDataSourceProvider.IOnWorldGenCompleteListener onWorldGenCompleteListener,
			GeneratedFullDataSourceProvider dataSourceProvider,
			Supplier<? extends AbstractLodRequestState> worldGenStateSupplier
		)
	{
		this.level = level;
		this.onWorldGenCompleteListener = onWorldGenCompleteListener;
		this.dataSourceProvider = dataSourceProvider;
		this.worldGenStateSupplier = worldGenStateSupplier;
		
		String levelId = this.level.getLevelWrapper().getDhIdentifier();
		this.tickerThread = ThreadUtil.makeSingleDaemonThreadPool("Request Module Ticker ["+levelId+"]");
		this.tickerThread.execute(this::tickLoop);
	}
	
	//endregion
	
	
	
	//=========//
	// ticking //
	//=========//
	//region
	
	private void tickLoop()
	{
		try
		{
			// Initial wait is to prevent an issue
			// where this starts before the child object's constructor finishes,
			// causing null pointers on final non-null references.
			// The try-catch in the while loop should also handle this
			// but this way we shouldn't have error logs.
			Thread.sleep(500);
			
			// run until the threadpool is shut down
			while (!Thread.interrupted())
			{
				try
				{
					Thread.sleep(20);
					
					this.tick();
				}
				catch (InterruptedException e) { throw e; }
				catch (Exception e)
				{
					LOGGER.error("Unexpected error in [" + LodRequestModule.class.getSimpleName() + "] tick loop, error: [" + e.getMessage() + "].", e);
				}
			}
		}
		catch (InterruptedException ignore) { }
	}
	private void tick()
	{
		boolean shouldDoWorldGen = this.onWorldGenCompleteListener.shouldDoWorldGen();
		
		// if the world is read only don't generate anything
		shouldDoWorldGen &= !DhApiWorldProxy.INSTANCE.tryGetReadOnly();
		
		
		
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
	
	//endregion
	
	
	
	//===================//
	// world gen control //
	//===================//
	//region
	
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
		// synchronized shutdown necessary to make sure the tasks are all handled correctly
		worldGenState.closeAsync(true).join();
		dataFileHandler.removeWorldGenCompleteListener(this.onWorldGenCompleteListener);
	}
	
	//endregion
	
	
	
	//=======================//
	// base method overrides //
	//=======================//
	//region
	
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
				// synchronized shutdown necessary to make sure the tasks are all handled correctly
				worldGenState.closeAsync(true).join();
			}
		}
	}
	
	//endregion
	
	
	
	//=========//
	// getters //
	//=========//
	//region
	
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
	
	//endregion
	
	
	
}
