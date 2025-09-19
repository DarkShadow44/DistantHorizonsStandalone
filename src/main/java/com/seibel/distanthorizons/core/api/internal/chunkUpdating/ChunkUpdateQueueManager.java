package com.seibel.distanthorizons.core.api.internal.chunkUpdating;

import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.api.internal.SharedApi;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.world.EWorldEnvironment;
import org.apache.logging.log4j.Logger;

public class ChunkUpdateQueueManager
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	
	public final ChunkPosQueue updateQueue;
	public final ChunkPosQueue preUpdateQueue;
	
	public int maxSize = 500;
	
	private static long lastOverloadedLogMessageMsTime = 0;
	
	
	
	//=============//
	// constructor //
	//=============//
	
	public ChunkUpdateQueueManager()
	{
		this.updateQueue = new ChunkPosQueue();
		this.preUpdateQueue = new ChunkPosQueue();
	}
	
	
	
	//==================//
	// list/set methods //
	//==================//
	
	public boolean contains(DhChunkPos pos) { return this.updateQueue.contains(pos) || this.preUpdateQueue.contains(pos); }
	
	public void clear()
	{
		this.updateQueue.clear();
		this.preUpdateQueue.clear();
	}
	public int getQueuedCount() { return this.updateQueue.getQueuedCount() + this.preUpdateQueue.getQueuedCount(); }
	public boolean isEmpty()
	{
		return this.updateQueue.isEmpty()
				&& this.preUpdateQueue.isEmpty();
	}
	
	/**
	 * Adds an item to the pre-update queue of chunks that might need to be updated.
	 * If there are no more slots, replaces the item furthest from the center in the update queue.
	 */
	public void addItemToPreUpdateQueue(DhChunkPos pos, ChunkUpdateData updateData)
	{
		int remainingSlots = this.maxSize - this.getQueuedCount();
		
		// If no slots are left, get one by removing the item furthest from the center
		if (remainingSlots <= 0)
		{
			if (!this.updateQueue.isEmpty())
			{
				this.updateQueue.popFurthest();
			}
			else
			{
				this.preUpdateQueue.popFurthest();
			}
		}
		this.preUpdateQueue.addItem(pos, updateData);
		
		remainingSlots = this.maxSize - this.getQueuedCount();
		if (remainingSlots <= 0)
		{
			this.sendOverloadMessage();
		}
	}
	
	public void addItemToUpdateQueue(DhChunkPos pos, ChunkUpdateData updateData)
	{
		int remainingSlots = this.maxSize - this.getQueuedCount();
		
		// If no slots are left, get one by removing the item furthest from the center
		if (remainingSlots <= 0)
		{
			this.updateQueue.popFurthest();
		}
		
		this.updateQueue.addItem(pos,updateData);
		
		remainingSlots = this.maxSize - this.getQueuedCount();
		if (remainingSlots <= 0)
		{
			this.sendOverloadMessage();
		}
	}
	
	private void sendOverloadMessage()
	{
		// limit how often an overloaded message can be sent
		long msBetweenLastLog = System.currentTimeMillis() - lastOverloadedLogMessageMsTime;
		if (msBetweenLastLog >= SharedApi.MIN_MS_BETWEEN_OVERLOADED_LOG_MESSAGE)
		{
			lastOverloadedLogMessageMsTime = System.currentTimeMillis();
			
			String message = "\u00A76" + "Distant Horizons overloaded, too many chunks queued for LOD processing. " + "\u00A7r" +
					"\nThis may result in holes in your LODs. " +
					"\nFix: move through the world slower, decrease your vanilla render distance, slow down your world pre-generator (IE Chunky), or increase the Distant Horizons' CPU thread counts. " +
					"\nMax queue count [" + SharedApi.CHUNK_UPDATE_QUEUE_MANAGER.maxSize + "] ([" + SharedApi.MAX_UPDATING_CHUNK_COUNT_PER_THREAD_AND_PLAYER + "] per thread+players).";
			
			boolean showWarningInChat = Config.Common.Logging.Warning.showUpdateQueueOverloadedChatWarning.get();
			if (showWarningInChat)
			{
				ClientApi.INSTANCE.showChatMessageNextFrame(message);
			}
			
			// Don't log warnings in singleplayer or in hosted LAN since it usually isn't a problem (and if it is it's easy to notice).
			// Servers should always log since being overloaded is harder to notice. 
			EWorldEnvironment environment = SharedApi.getEnvironment();
			if (showWarningInChat || environment == EWorldEnvironment.SERVER_ONLY)
			{
				LOGGER.warn(message);
			}
		}
	}
	
	
	
	//==================//
	// position methods //
	//==================//
	
	public void setCenter(DhChunkPos newCenter)
	{
		this.updateQueue.setCenter(newCenter);
		this.preUpdateQueue.setCenter(newCenter);
	}
	
	
}
