package com.seibel.distanthorizons.core.api.internal.chunkUpdating;

import com.seibel.distanthorizons.core.api.internal.SharedApi;
import com.seibel.distanthorizons.core.pos.DhChunkPos;

import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

public class ChunkPosQueue
{
	private final PriorityBlockingQueue<DhChunkPos> closestQueue;
	private final PriorityBlockingQueue<DhChunkPos> furthestQueue;
	private final ConcurrentHashMap<DhChunkPos, ChunkUpdateData> updateDataByChunkPos;
	
	private DhChunkPos center;
	
	
	
	//=============//
	// constructor //
	//=============//
	
	public ChunkPosQueue()
	{
		this.closestQueue = new PriorityBlockingQueue<>(500, Comparator.comparingDouble(pos -> pos.squaredDistance(this.center)));
		this.furthestQueue = new PriorityBlockingQueue<>(500, Comparator.comparingDouble(pos -> ((DhChunkPos)pos).squaredDistance(this.center)).reversed());
		this.updateDataByChunkPos = new ConcurrentHashMap<>();
		// defaulting to 0,0 is fine since it'll be updated once we start adding items 
		this.center = new DhChunkPos(0, 0);
	}
	
	
	
	//==============//
	// list methods //
	//==============//
	
	public boolean contains(DhChunkPos pos) { return this.updateDataByChunkPos.containsKey(pos); }
	
	public void clear()
	{
		this.updateDataByChunkPos.clear();
		this.closestQueue.clear();
		this.furthestQueue.clear();
	}
	
	public void addItem(DhChunkPos pos, ChunkUpdateData updateData)
	{
		if (this.updateDataByChunkPos.containsKey(pos))
		{
			// Chunk is already present in queue, no need to insert
			return;
		}
		this.updateDataByChunkPos.put(pos, updateData);
		this.closestQueue.add(pos);
		this.furthestQueue.add(pos);
	}
	
	public int getQueuedCount() { return this.updateDataByChunkPos.size(); }
	
	public boolean isEmpty() { return this.updateDataByChunkPos.isEmpty(); }
	
	
	
	//==================//
	// position methods //
	//==================//
	
	public void setCenter(DhChunkPos newCenter)
	{
		// if the rebuild time takes too long 
		// (in James' testing a queue of 500 items only took around 0.1 milliseconds)
		// this equation could be changed to only update after moving 2 or 4 chunks from the center
		if (newCenter.equals(this.center))
		{
			return;
		}
		
		this.center = newCenter;
		
		// rebuild the priority queues to match the new center
		this.closestQueue.clear();
		this.furthestQueue.clear();
		for (DhChunkPos pos : this.updateDataByChunkPos.keySet())
		{
			this.closestQueue.add(pos);
			this.furthestQueue.add(pos);
		}
	}
	
	public ChunkUpdateData popClosest()
	{
		if (this.closestQueue.isEmpty())
		{
			return null;
		}
		
		DhChunkPos closest = this.closestQueue.poll();
		if (closest == null)
		{
			return null;
		}
		
		this.furthestQueue.remove(closest);
		return this.updateDataByChunkPos.remove(closest);
	}
	public ChunkUpdateData popFurthest()
	{
		if (this.furthestQueue.isEmpty())
		{
			return null;
		}
		
		DhChunkPos furthest = this.furthestQueue.poll();
		if (furthest == null)
		{
			return null;
		}
		
		this.closestQueue.remove(furthest);
		return this.updateDataByChunkPos.remove(furthest);
	}
}
		
