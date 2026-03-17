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

package com.seibel.distanthorizons.api.methods.events.abstractEvents;

import com.seibel.distanthorizons.api.interfaces.block.IDhApiBiomeWrapper;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBlockStateWrapper;
import com.seibel.distanthorizons.api.interfaces.factories.IDhApiWrapperFactory;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.methods.events.interfaces.IDhApiEvent;
import com.seibel.distanthorizons.api.methods.events.interfaces.IDhApiEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;

/**
 * Used to override which blocks may be stored in a given chunk.
 * This can be used for X-ray prevention or to replace problematic mod blocks
 * that don't fit into the {@link IDhApiBlockStateWrapper} format DH requires
 * (IE modded blocks that use NBT data
 * to determine their model and/or texture). <br/><br/>
 *
 * This event is fired for each block or biome change when DH is processing a chunk.
 * A change happens when DH finds a different block or biome while walking through a chunk.
 * For example with the block sequence:<br/>
 * <code> stone -> stone -> air -> stone </code> <br/>
 * This event would be fired for the first, third, and forth blocks in the sequence
 * (IE the first stone, first air, and last stone respectively). <br/> <br/>
 * 
 * The order DH will process blocks is undefined so a specific ordering shouldn't be relied upon for your logic to function. <br/> <br/>
 * 
 * <b>Threading note:</b> this event may be called concurrently across multiple threads. <br/>
 * <b>Performance note:</b> this event will be called very frequently, avoid expensive lookups or other slow operations if possible. <br/>
 * 
 * @see DhApiLevelLoadEvent
 * @see IDhApiWrapperFactory
 *
 * @author James Seibel
 * @version 2025-09-29
 * @since API 4.1.0
 */
public abstract class DhApiChunkProcessingEvent implements IDhApiEvent<DhApiChunkProcessingEvent.EventParam>
{
	public abstract void blockOrBiomeChangedDuringChunkProcessing(DhApiEventParam<EventParam> event);
	
	
	//=========================//
	// internal DH API methods //
	//=========================//
	
	@Override
	public final void fireEvent(DhApiEventParam<EventParam> event) { this.blockOrBiomeChangedDuringChunkProcessing(event); }
	
	
	//==================//
	// parameter object //
	//==================//
	
	public static class EventParam implements IDhApiEventParam
	{
		/** The saved level. */
		public final IDhApiLevelWrapper levelWrapper;
		
		/** the processed chunk's X pos in chunk coordinates */
		public final int chunkX;
		/** the processed chunk's Z pos in chunk coordinates */
		public final int chunkZ;
		
		
		public int relativeBlockPosX;
		public int blockPosY;
		public int relativeBlockPosZ;
		
		public IDhApiBlockStateWrapper currentBlock;
		public IDhApiBiomeWrapper currentBiome;
		
		private IDhApiBlockStateWrapper newBlock;
		private IDhApiBiomeWrapper newBiome;
		
		
		
		//=============//
		// constructor //
		//=============//
		
		public EventParam(IDhApiLevelWrapper newLevelWrapper, int chunkX, int chunkZ)
		{
			this.levelWrapper = newLevelWrapper;
			this.chunkX = chunkX;
			this.chunkZ = chunkZ;
		}
		
		/** 
		 * Internal method use by Distant Horizons
		 * to set up this event.
		 */
		public void updateForPosition(
				int relativeBlockPosX, int blockPosY, int relativeBlockPosZ,
				IDhApiBlockStateWrapper currentBlock,
				IDhApiBiomeWrapper currentBiome)
		{
			this.relativeBlockPosX = relativeBlockPosX;
			this.blockPosY = blockPosY;
			this.relativeBlockPosZ = relativeBlockPosZ;
			
			this.newBlock = null;
			this.newBiome = null;
			
			this.currentBlock = currentBlock;
			this.currentBiome = currentBiome;
		}
		
		
		
		//=================//
		// getters/setters //
		//=================//
		
		/**
		 * Sets the {@link IDhApiBlockStateWrapper} that should be used at this event's current position in the chunk.
		 * If you don't want to modify the block at this event's current position, 
		 * either don't call this method or pass in null. <br>
		 * Passing in null will remove the override, meaning the original block will be used. <br><br>
		 *
		 * A {@link IDhApiWrapperFactory} should be used to get the {@link IDhApiBlockStateWrapper} that's returned. 
		 * Attempting to create your own {@link IDhApiBlockStateWrapper} will cause a {@link ClassCastException}. <br/> <br/>
		 *
		 * If multiple API users are listening to this event the override may already have been set.
		 * With that in mind it is recommended to check if an override has already been set via
		 * {@link EventParam#getBlockOverride()} to handle that occurrence. <br>
		 * Note that the order of API events firing is undefined so a specific order shouldn't be relied upon. <br><br>
		 * 
		 * @see IDhApiWrapperFactory
		 */
		public void setBlockOverride(IDhApiBlockStateWrapper block) { this.newBlock = block; }
		/** 
		 * Returns the currently overriding block for this position.
		 * This will be null if no other API event has set the override.
		 */
		public IDhApiBlockStateWrapper getBlockOverride() { return this.newBlock; }
		
		
		
		/**
		 * Sets the {@link IDhApiBiomeWrapper} that should be used at this event's current position in the chunk.
		 * If you don't want to modify the biome at this event's current position, 
		 * either don't call this method or pass in null. <br>
		 * Passing in null will remove the override, meaning the original biome will be used. <br><br>
		 *
		 * A {@link IDhApiWrapperFactory} should be used to get the {@link IDhApiBiomeWrapper} that's returned. 
		 * Attempting to create your own {@link IDhApiBiomeWrapper} will cause a {@link ClassCastException}. <br/> <br/>
		 *
		 * If multiple API users are listening to this event the override may already have been set.
		 * With that in mind it is recommended to check if an override has already been set via
		 * {@link EventParam#getBiomeOverride()} ()} to handle that occurrence. <br>
		 * Note that the order of API events firing is undefined so a specific order shouldn't be relied upon. <br><br>
		 *
		 * @see IDhApiWrapperFactory
		 */
		public void setBiomeOverride(IDhApiBiomeWrapper biome) { this.newBiome = biome; }
		/**
		 * Returns the currently overriding biome for this position.
		 * This will be null if no other API event has set the override.
		 */
		public IDhApiBiomeWrapper getBiomeOverride() { return this.newBiome; }
		
		
		
		/** 
		 * Returns the same instance of this event.
		 * Copying this event isn't recommended due to 
		 * how often it would be called per chunk, creating
		 * unnecessary garbage collector pressure.
		 */
		@Override
		public EventParam copy() { return this; }
		
		@Override 
		public boolean getCopyBeforeFire() { return false; }
		
	}
	
}