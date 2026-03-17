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

package com.seibel.distanthorizons.core.api.internal;

import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiWorldLoadEvent;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiWorldUnloadEvent;
import com.seibel.distanthorizons.core.Initializer;
import com.seibel.distanthorizons.core.api.internal.chunkUpdating.ChunkUpdateData;
import com.seibel.distanthorizons.core.api.internal.chunkUpdating.ChunkUpdateQueueManager;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.generation.DhLightingEngine;
import com.seibel.distanthorizons.core.level.DhClientLevel;
import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.logging.f3.F3Screen;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos2D;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.render.renderer.DebugRenderer;
import com.seibel.distanthorizons.core.sql.repo.AbstractDhRepo;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.util.objects.Pair;
import com.seibel.distanthorizons.core.util.threading.PriorityTaskPicker;
import com.seibel.distanthorizons.core.util.threading.ThreadPoolUtil;
import com.seibel.distanthorizons.core.world.*;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftSharedWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.distanthorizons.coreapi.DependencyInjection.ApiEventInjector;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.*;

/** Contains code and variables used by both {@link ClientApi} and {@link ServerApi} */
public class SharedApi
{
	public static final SharedApi INSTANCE = new SharedApi();
	
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	/** will be null on the server-side */
	@Nullable
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class);
	/** will be null on the server-side */
	@Nullable
	private static final IMinecraftClientWrapper MC_CLIENT = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	private static final IMinecraftSharedWrapper MC_SHARED = SingletonInjector.INSTANCE.get(IMinecraftSharedWrapper.class);
	
	public static final ChunkUpdateQueueManager CHUNK_UPDATE_QUEUE_MANAGER = new ChunkUpdateQueueManager();
	/** 
	 * how many chunks can be queued for updating per thread + player (in multiplayer), 
	 * used to prevent updates from infinitely pilling up if the user flies around extremely fast 
	 */
	public static final int MAX_UPDATING_CHUNK_COUNT_PER_THREAD_AND_PLAYER = 1_000;
	
	/** how many milliseconds must pass before an overloaded message can be sent in chat or the log */
	public static final int MIN_MS_BETWEEN_OVERLOADED_LOG_MESSAGE = 30_000;
	
	
	@Nullable
	private static AbstractDhWorld currentWorld;
	private static int lastWorldGenTickDelta = 0;
	
	
	
	//=============//
	// constructor //
	//=============//
	
	private SharedApi() { }
	public static void init() { Initializer.init(); }
	
	
	
	//===============//
	// world methods //
	//===============//
	
	public static EWorldEnvironment getEnvironment() { return (currentWorld == null) ? null : currentWorld.environment; }
	
	public static void setDhWorld(AbstractDhWorld newWorld)
	{
		AbstractDhWorld oldWorld = currentWorld;
		if (oldWorld != null)
		{
			oldWorld.close();
		}
		currentWorld = newWorld;
		
		// starting and stopping the DataRenderTransformer is necessary to prevent attempting to
		// access the MC level at inappropriate times, which can cause exceptions
		if (currentWorld != null)
		{
			ThreadPoolUtil.setupThreadPools();
			
			ApiEventInjector.INSTANCE.fireAllEvents(DhApiWorldLoadEvent.class, new DhApiWorldLoadEvent.EventParam());
		}
		else
		{
			ThreadPoolUtil.shutdownThreadPools();
			DebugRenderer.clearRenderables();
			
			if (MC_RENDER != null)
			{
				MC_RENDER.clearTargetFrameBuffer();
			}
			
			// shouldn't be necessary, but if we missed closing one of the connections this should make sure they're all closed
			AbstractDhRepo.closeAllConnections();
			// needs to be closed on world shutdown to clear out un-processed chunks
			CHUNK_UPDATE_QUEUE_MANAGER.clear();
			
			// recommend that the garbage collector cleans up any objects from the old world and thread pools
			System.gc();
			
			ApiEventInjector.INSTANCE.fireAllEvents(DhApiWorldUnloadEvent.class, new DhApiWorldUnloadEvent.EventParam());
			
			// fired after the unload event so API users can't change the read-only for any new worlds
			DhApiWorldProxy.INSTANCE.setReadOnly(false, false);
		}
	}
	
	public static void worldGenTick(Runnable worldGenRunnable)
	{
		lastWorldGenTickDelta--;
		if (lastWorldGenTickDelta <= 0)
		{
			worldGenRunnable.run();
			lastWorldGenTickDelta = 20;
		}
	}
	
	@Nullable
	public static AbstractDhWorld getAbstractDhWorld() { return currentWorld; }
	/** returns null if the {@link SharedApi#currentWorld} isn't a {@link DhClientServerWorld} */
	public static DhClientServerWorld getDhClientServerWorld() { return (currentWorld instanceof DhClientServerWorld) ? (DhClientServerWorld) currentWorld : null; }
	/** returns null if the {@link SharedApi#currentWorld} isn't a {@link DhClientWorld} or {@link DhClientServerWorld} */
	public static IDhClientWorld getIDhClientWorld() { return (currentWorld instanceof IDhClientWorld) ? (IDhClientWorld) currentWorld : null; }
	/** returns null if the {@link SharedApi#currentWorld} isn't a {@link DhServerWorld} or {@link DhClientServerWorld} */
	public static IDhServerWorld getIDhServerWorld() { return (currentWorld instanceof IDhServerWorld) ? (IDhServerWorld) currentWorld : null; }
	
	
	
	//==============//
	// chunk update //
	//==============//
	
	/** 
	 * Used to prevent getting a full chunk from MC if it isn't necessary. <br>
	 * This is important since asking MC for a chunk is slow and may block the render thread.
	 */
	public static boolean isChunkAtBlockPosAlreadyUpdating(int blockPosX, int blockPosZ)
	{ return CHUNK_UPDATE_QUEUE_MANAGER.contains(new DhChunkPos(new DhBlockPos2D(blockPosX, blockPosZ))); }
	
	public static boolean isChunkAtChunkPosAlreadyUpdating(int chunkPosX, int chunkPosZ)
	{ return CHUNK_UPDATE_QUEUE_MANAGER.contains(new DhChunkPos(chunkPosX, chunkPosZ)); }
	
	/** 
	 * This is often fired when unloading a level.
	 * This is done to prevent overloading the system when
	 * rapidly changing dimensions.
	 * (IE prevent DH from infinitely allocating memory 
	 */
	public void clearQueuedChunkUpdates() { CHUNK_UPDATE_QUEUE_MANAGER.clear(); }
	
	public int getQueuedChunkUpdateCount() { return CHUNK_UPDATE_QUEUE_MANAGER.getQueuedCount(); }
	
	
	
	/** handles both block place and break events */
	public void chunkBlockChangedEvent(IChunkWrapper chunk, ILevelWrapper level) { this.applyChunkUpdate(chunk, level, true, false); }
	public void chunkLoadEvent(IChunkWrapper chunk, ILevelWrapper level) { this.applyChunkUpdate(chunk, level, true, true); }
	
	//public void applyChunkUpdate(IChunkWrapper chunkWrapper, ILevelWrapper level, boolean canGetNeighboringChunks) { this.applyChunkUpdate(chunkWrapper, level, canGetNeighboringChunks, false); }
	public void applyChunkUpdate(IChunkWrapper chunkWrapper, ILevelWrapper level, boolean canGetNeighboringChunks, boolean newlyLoaded)
	{
		//========================//
		// world and level checks //
		//========================//
		
		if (chunkWrapper == null)
		{
			// shouldn't happen, but just in case
			return;
		}
		
		AbstractDhWorld dhWorld = SharedApi.getAbstractDhWorld();
		if (dhWorld == null)
		{
			if (level instanceof IClientLevelWrapper)
			{
				// If the client world isn't loaded yet, keep track of which chunks were loaded so we can use them later.
				// This may happen if the client world and client level load events happen out of order
				IClientLevelWrapper clientLevel = (IClientLevelWrapper) level;
				ClientApi.INSTANCE.waitingChunkByClientLevelAndPos.replace(new Pair<>(clientLevel, chunkWrapper.getChunkPos()), chunkWrapper);
			}
			
			return;
		}
		
		// ignore updates if the world is read-only
		if (DhApiWorldProxy.INSTANCE.getReadOnly())
		{
			return;
		}
		
		
		// only continue if the level is loaded
		IDhLevel dhLevel = dhWorld.getLevel(level);
		if (dhLevel == null)
		{
			if (level instanceof IClientLevelWrapper)
			{
				// the client level isn't loaded yet
				IClientLevelWrapper clientLevel = (IClientLevelWrapper) level;
				ClientApi.INSTANCE.waitingChunkByClientLevelAndPos.replace(new Pair<>(clientLevel, chunkWrapper.getChunkPos()), chunkWrapper);
			}
			
			return;
		}
		
		if (dhLevel instanceof DhClientLevel)
		{
			if (!((DhClientLevel) dhLevel).shouldProcessChunkUpdate(chunkWrapper.getChunkPos()))
			{
				return;
			}
		}
		
		// shoudln't normally happen, but just in case
		if (CHUNK_UPDATE_QUEUE_MANAGER.contains(chunkWrapper.getChunkPos()))
		{
			// TODO this will prevent some LODs from updating across dimensions if multiple levels are loaded
			return;
		}
		
		
		
		//===============================//
		// update the necessary chunk(s) //
		//===============================//
		
		if (!canGetNeighboringChunks)
		{
			// only update the center chunk
			queueChunkUpdate(chunkWrapper, null, dhLevel, false);
			return;
		}
		
		
		ArrayList<IChunkWrapper> neighboringChunkList = getNeighborChunkListForChunk(chunkWrapper, dhLevel);
		
		if (newlyLoaded)
		{
			// this means this chunkWrapper is a newly loaded chunk 
			// which may be missing some neighboring chunk data
			// because it is bordering the render distance
			// thus, only the chunks neighboring this chunkWrapper will get updated
			// because those are more likely to have their full neighboring chunk data
			//TODO this does not prevent those neighboring chunks from updating
			// this newly loaded chunk that were just skipped
			// leading to occasional lighting issues
			for (IChunkWrapper neighboringChunk : neighboringChunkList)
			{
				if (neighboringChunk == chunkWrapper)
				{
					continue;
				}
				
				this.applyChunkUpdate(neighboringChunk, level, true, false);
			}
		}
		else
		{
			// if not all neighboring chunk data is available, do not try to update
			if (neighboringChunkList.size() < 9)
			{
				return;
			}
			
			// update the center with any existing neighbour chunks. 
			// this is done so lighting changes are propagated correctly
			queueChunkUpdate(chunkWrapper, neighboringChunkList, dhLevel, true);
		}
	}
	private static ArrayList<IChunkWrapper> getNeighborChunkListForChunk(IChunkWrapper chunkWrapper, IDhLevel dhLevel)
	{
		// get the neighboring chunk list
		ArrayList<IChunkWrapper> neighborChunkList = new ArrayList<>(9);
		for (int xOffset = -1; xOffset <= 1; xOffset++)
		{
			for (int zOffset = -1; zOffset <= 1; zOffset++)
			{
				if (xOffset == 0 && zOffset == 0)
				{
					// center chunk
					neighborChunkList.add(chunkWrapper);
				}
				else
				{
					// neighboring chunk 
					DhChunkPos neighborPos = new DhChunkPos(chunkWrapper.getChunkPos().getX() + xOffset, chunkWrapper.getChunkPos().getZ() + zOffset);
					IChunkWrapper neighborChunk = dhLevel.getLevelWrapper().tryGetChunk(neighborPos);
					if (neighborChunk != null)
					{
						neighborChunkList.add(neighborChunk);
					}
				}
			}
		}
		return neighborChunkList;
	}
	
	private static void queueChunkUpdate(IChunkWrapper chunkWrapper, @Nullable ArrayList<IChunkWrapper> neighborChunkList, IDhLevel dhLevel, boolean canGetNeighboringChunks)
	{
				
		// return if the chunk is already queued
		if (CHUNK_UPDATE_QUEUE_MANAGER.contains(chunkWrapper.getChunkPos()))
		{
			return;
		}
			
		
		// add chunk update data to preUpdate queue
		ChunkUpdateData updateData = new ChunkUpdateData(chunkWrapper, neighborChunkList, dhLevel, canGetNeighboringChunks);
		CHUNK_UPDATE_QUEUE_MANAGER.addItemToPreUpdateQueue(chunkWrapper.getChunkPos(), updateData);
		
		
		// queue updates up to the number of CPU cores allocated for the job
		// (this prevents doing extra work queuing tasks that may not be necessary)
		// and makes sure the chunks closest to the player are updated first
		PriorityTaskPicker.Executor executor = ThreadPoolUtil.getChunkToLodBuilderExecutor();
		if (executor != null && executor.getQueueSize() < executor.getPoolSize())
		{
			try
			{
				executor.execute(SharedApi::processQueue);
			}
			catch (RejectedExecutionException ignore)
			{
				// the executor was shut down, it should be back up shortly and able to accept new jobs
			}
		}
	}
	
	private static void processQueue()
	{
		// update the center & max size of the queue manager
		int maxUpdateSizeMultiplier;
		if (MC_CLIENT != null && MC_CLIENT.playerExists())
		{
			// Local worlds & multiplayer
			CHUNK_UPDATE_QUEUE_MANAGER.setCenter(MC_CLIENT.getPlayerChunkPos());
			maxUpdateSizeMultiplier = MC_CLIENT.clientConnectedToDedicatedServer() ? 1 : MC_SHARED.getPlayerCount();
		}
		else
		{
			// Dedicated servers
			// Also includes spawn chunks since they're likely to be intentionally utilized with updates
			maxUpdateSizeMultiplier = 1 + MC_SHARED.getPlayerCount();
		}
		
		CHUNK_UPDATE_QUEUE_MANAGER.maxSize = MAX_UPDATING_CHUNK_COUNT_PER_THREAD_AND_PLAYER
				* Config.Common.MultiThreading.numberOfThreads.get()
				* maxUpdateSizeMultiplier;
		
		
		
		//===============================//
		// update the necessary chunk(s) //
		//===============================//
		
		// process preUpdate queue
		processQueuedChunkPreUpdate();
		
		// process update queue
		processQueuedChunkUpdate();
		
		// queue the next position if there are still positions to process
		AbstractExecutorService executor = ThreadPoolUtil.getChunkToLodBuilderExecutor();
		if (executor != null && !CHUNK_UPDATE_QUEUE_MANAGER.isEmpty())
		{
			try
			{
				executor.execute(SharedApi::processQueue);
			}
			catch (RejectedExecutionException ignore)
			{
				// the executor was shut down, it should be back up shortly and able to accept new jobs
			}
		}
		
	}
	
	private static void processQueuedChunkPreUpdate()
	{
		ChunkUpdateData preUpdateData = CHUNK_UPDATE_QUEUE_MANAGER.preUpdateQueue.popClosest();
		if (preUpdateData == null)
		{
			return;
		}
		
		IDhLevel dhLevel = preUpdateData.dhLevel;
		IChunkWrapper chunkWrapper = preUpdateData.chunkWrapper;
		boolean canGetNeighboringChunks = preUpdateData.canGetNeighboringChunks;
		ArrayList<IChunkWrapper> neighborChunkList = preUpdateData.neighborChunkList;
		
		try
		{
			// check if this chunk has been converted into an LOD already
			boolean checkChunkHash = !Config.Common.LodBuilding.disableUnchangedChunkCheck.get();
			if (checkChunkHash)
			{
				int oldChunkHash = dhLevel.getChunkHash(chunkWrapper.getChunkPos()); // shouldn't happen on the render thread since it may take a few moments to run
				int newChunkHash = chunkWrapper.getBlockBiomeHashCode();
				
				boolean hasNewChunkHash = (oldChunkHash != newChunkHash);
				if (!hasNewChunkHash)
				{
					// do not update the chunk if the hash is the same
					return;
				}
				
				// if this chunk will update and can get neighbors
				// then queue neighboring chunks to update as well
				// neighboring chunk will get added directly to the update queue
				// so they won't queue further chunk updates
				if (neighborChunkList != null 
					&& !neighborChunkList.isEmpty())
				{
					for (IChunkWrapper adjacentChunk : neighborChunkList)
					{
						// pulling a new chunkWrapper is necessary to prevent concurrent modification on the existing chunkWrappers
						IChunkWrapper newCenterChunk = dhLevel.getLevelWrapper().tryGetChunk(adjacentChunk.getChunkPos());
						if (newCenterChunk != null)
						{
							ChunkUpdateData newUpdateData;
							if (canGetNeighboringChunks)
							{
								newUpdateData = new ChunkUpdateData(newCenterChunk, getNeighborChunkListForChunk(newCenterChunk, dhLevel), dhLevel, true);
							}
							else
							{
								newUpdateData = new ChunkUpdateData(newCenterChunk, null, dhLevel, false);
							}
							
							CHUNK_UPDATE_QUEUE_MANAGER.addItemToUpdateQueue(newCenterChunk.getChunkPos(), newUpdateData);
						}
					}
				}
			}
			
			CHUNK_UPDATE_QUEUE_MANAGER.addItemToUpdateQueue(chunkWrapper.getChunkPos(), preUpdateData);
		}
		catch (Exception e)
		{
			LOGGER.error("Unexpected error when pre-updating chunk at pos: [" + chunkWrapper.getChunkPos() + "]", e);
		}
	}
	
	private static void processQueuedChunkUpdate()
	{
		//LOGGER.trace(chunkWrapper.getChunkPos() + " " + executor.getActiveCount() + " / " + executor.getQueue().size() + " - " + executor.getCompletedTaskCount());
		
		ChunkUpdateData updateData = CHUNK_UPDATE_QUEUE_MANAGER.updateQueue.popClosest();
		if (updateData == null)
		{
			return;
		}
		
		IChunkWrapper chunkWrapper = updateData.chunkWrapper;
		IDhLevel dhLevel = updateData.dhLevel;
		// having a list of the nearby chunks is needed for lighting and beacon generation
		@Nullable ArrayList<IChunkWrapper> nearbyChunkList = updateData.neighborChunkList; 
		
		// a non-null list is needed for the lighting engine
		if (nearbyChunkList == null)
		{
			nearbyChunkList = new ArrayList<IChunkWrapper>();
			nearbyChunkList.add(chunkWrapper);
		}
		
		try
		{
			// sky lighting is populated later at the data source level
			DhLightingEngine.INSTANCE.bakeChunkBlockLighting(chunkWrapper, nearbyChunkList, dhLevel.hasSkyLight() ? LodUtil.MAX_MC_LIGHT : LodUtil.MIN_MC_LIGHT);
			
			dhLevel.updateBeaconBeamsForChunk(chunkWrapper, nearbyChunkList);
			
			int newChunkHash = chunkWrapper.getBlockBiomeHashCode();
			dhLevel.updateChunkAsync(chunkWrapper, newChunkHash);
		}
		catch (Exception e)
		{
			LOGGER.error("Unexpected error when updating chunk at pos: [" + chunkWrapper.getChunkPos() + "]", e);
		}
	}
	
	
	
	//=========//
	// F3 Menu //
	//=========//
	
	public String getDebugMenuString()
	{
		String preUpdatingCountStr = F3Screen.NUMBER_FORMAT.format(CHUNK_UPDATE_QUEUE_MANAGER.preUpdateQueue.getQueuedCount());
		String updatingCountStr = F3Screen.NUMBER_FORMAT.format(CHUNK_UPDATE_QUEUE_MANAGER.updateQueue.getQueuedCount());
		String queuedCountStr = F3Screen.NUMBER_FORMAT.format(CHUNK_UPDATE_QUEUE_MANAGER.getQueuedCount());
		
		String maxUpdateCountStr = F3Screen.NUMBER_FORMAT.format(CHUNK_UPDATE_QUEUE_MANAGER.maxSize);
		
		return "Queued chunk updates: "+"( "+preUpdatingCountStr+" + "+updatingCountStr+" )  [ "+queuedCountStr+" / "+maxUpdateCountStr+" ]";
	}
	
	
	
}
