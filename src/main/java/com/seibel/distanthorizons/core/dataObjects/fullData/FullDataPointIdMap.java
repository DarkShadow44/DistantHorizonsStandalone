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

package com.seibel.distanthorizons.core.dataObjects.fullData;

import com.seibel.distanthorizons.core.dataObjects.BlockBiomeWrapperPair;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.util.objects.DataCorruptedException;
import com.seibel.distanthorizons.core.util.objects.dataStreams.DhDataInputStream;
import com.seibel.distanthorizons.core.util.objects.dataStreams.DhDataOutputStream;
import com.seibel.distanthorizons.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.distanthorizons.core.logging.DhLogger;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WARNING: This is not THREAD-SAFE! <br><br>
 * 
 * Used to map a numerical IDs to a Biome/BlockState pair. <br><br>
 * 
 * TODO the serializing of this map might be really big
 *  since it stringifies every block and biome name, which is quite bulky.
 *  It might be worth while to have a biome and block ID that then both get mapped
 *  to the data point ID to reduce file size.
 *  And/or it would be good to dynamically remove IDs that aren't currently in use.
 * 
 * @author Leetom
 */
public class FullDataPointIdMap
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	/**
	 * Should only be enabled when debugging.
	 * Has the system check if any duplicate Entries were read/written
	 * when (de)serializing.
	 */
	private static final boolean RUN_SERIALIZATION_DUPLICATE_VALIDATION = false;
	/** Distant Horizons - Block State Wrapper */
	public static final String BLOCK_STATE_SEPARATOR_STRING = "_DH-BSW_";
	
	
	/** should only be used for debugging */
	private long pos;
	
	/** The index should be the same as the BlockBiomeWrapperPair's ID */
	private final ArrayList<BlockBiomeWrapperPair> blockBiomePairList = new ArrayList<>();
	private final ConcurrentHashMap<BlockBiomeWrapperPair, Integer> idMap = new ConcurrentHashMap<>();
	
	private int cachedHashCode = 0;
	
	
	
	//=============//
	// constructor //
	//=============//
	
	public FullDataPointIdMap(long pos) { this.pos = pos; }
	
	
	
	//=========//
	// getters //
	//=========//
	
	/** @see FullDataPointIdMap#getEntry(int) */
	public IBiomeWrapper getBiomeWrapper(int id) throws IndexOutOfBoundsException { return this.getEntry(id).biome; }
	/** @see FullDataPointIdMap#getEntry(int) */
	public IBlockStateWrapper getBlockStateWrapper(int id) throws IndexOutOfBoundsException { return this.getEntry(id).blockState; }
	/** @throws IndexOutOfBoundsException if the given ID isn't in the {@link FullDataPointIdMap#blockBiomePairList} */
	private BlockBiomeWrapperPair getEntry(int id) throws IndexOutOfBoundsException
	{
		BlockBiomeWrapperPair pair;
		try
		{
			pair = this.blockBiomePairList.get(id);
		}
		catch (IndexOutOfBoundsException e)
		{
			throw new IndexOutOfBoundsException("FullData ID Map out of sync for pos: "+DhSectionPos.toString(this.pos)+". ID: ["+id+"] greater than the number of known ID's: ["+this.blockBiomePairList.size()+"].");
		}
		
		return pair;
	}
	
	
	/** @return -1 if the list is empty */
	public int getMaxValidId() { return this.blockBiomePairList.size() - 1; }
	public int size() { return this.blockBiomePairList.size(); }
	
	public boolean isEmpty() { return this.blockBiomePairList.isEmpty(); }
	
	public long getPos() { return this.pos; }
	
	
	
	//=========//
	// setters //
	//=========//
	
	/**
	 * If an entry with the given values already exists nothing will
	 * be added but the existing item's ID will still be returned.
	 */
	public int addIfNotPresentAndGetId(IBiomeWrapper biome, IBlockStateWrapper blockState) { return this.addIfNotPresentAndGetId(BlockBiomeWrapperPair.get(blockState, biome)); }
	private int addIfNotPresentAndGetId(BlockBiomeWrapperPair pair)
	{
		// try getting the existing ID
		Integer nullableId = this.idMap.get(pair);
		if (nullableId != null)
		{
			return nullableId;
		}
		
		
		// create the new ID
		return this.idMap.compute(pair, (BlockBiomeWrapperPair newPair, Integer currentId) -> 
		{
			if (currentId != null)
			{
				return currentId;
			}
			
			
			// Add the new ID
			currentId = this.blockBiomePairList.size();
			this.blockBiomePairList.add(newPair);
			
			// invalidate the cached hash code
			this.cachedHashCode = 0;
			
			return currentId;
		});
	}
	
	/**
	 * Adds every {@link BlockBiomeWrapperPair} from inputMap into this map. <br>
	 * Allows duplicate entries. <br><br>
	 * 
	 * Allowing duplicate entries should be done if a datasource is just being read in and 
	 * a merge step isn't being done afterwards. If duplicates are removed it may cause 
	 * the ID's to get out of sync since everything will be shifted down after the removed
	 * ID(s).
	 */
	public void addAll(FullDataPointIdMap inputMap)
	{
		ArrayList<BlockBiomeWrapperPair> pairsToMerge = inputMap.blockBiomePairList;
		for (int i = 0; i < pairsToMerge.size(); i++)
		{
			BlockBiomeWrapperPair pair = pairsToMerge.get(i);
			this.add(pair);
		}
	}
	/** allows for adding duplicate {@link BlockBiomeWrapperPair} */
	private void add(BlockBiomeWrapperPair pair)
	{
		int id = this.blockBiomePairList.size();
		this.blockBiomePairList.add(pair);
		this.idMap.put(pair, id);
		
		// invalidate the cached hash code
		this.cachedHashCode = 0;
	}
	
	/**
	 * Adds each entry from the given map to this map. <br><br>
	 * 
	 * Note: when using this function be careful about re-mapping the
	 * same data source multiple times.
	 * Doing so may cause indexOutOfBounds issues.
	 *
	 * @return an array of each added entry's ID in this map in order
	 */
	public int[] mergeAndReturnRemappedEntityIds(FullDataPointIdMap inputMap)
	{
		ArrayList<BlockBiomeWrapperPair> entriesToMerge = inputMap.blockBiomePairList;
		int[] remappedPairIds = new int[entriesToMerge.size()];
		for (int i = 0; i < entriesToMerge.size(); i++)
		{
			BlockBiomeWrapperPair entity = entriesToMerge.get(i);
			int id = this.addIfNotPresentAndGetId(entity);
			remappedPairIds[i] = id;
		}
		
		return remappedPairIds;
	}
	
	/** Should only be used if this map is going to be reused, otherwise bad things will happen. */
	public void clear(long pos)
	{
		this.pos = pos;
		this.blockBiomePairList.clear();
		this.idMap.clear();
		this.cachedHashCode = 0;
	}
	
	
	
	//=============//
	// serializing //
	//=============//
	
	/** Serializes all contained entries into the given stream, formatted in UTF */
	public void serialize(DhDataOutputStream outputStream) throws IOException
	{
		outputStream.writeInt(this.blockBiomePairList.size());
		
		// only used when debugging
		HashMap<String, BlockBiomeWrapperPair> dataPointEntryBySerialization = new HashMap<>();
		
		for (BlockBiomeWrapperPair pair : this.blockBiomePairList)
		{
			String entryString = pair.serialize();
			outputStream.writeUTF(entryString);
			
			if (RUN_SERIALIZATION_DUPLICATE_VALIDATION)
			{
				if (dataPointEntryBySerialization.containsKey(entryString))
				{
					LOGGER.error("Duplicate serialized pair found with serial: " + entryString);
				}
				if (dataPointEntryBySerialization.containsValue(pair))
				{
					LOGGER.error("Duplicate serialized pair found with value: " + pair.serialize());
				}
				dataPointEntryBySerialization.put(entryString, pair);
			}
		}
	}
	
	/** Creates a new IdBiomeBlockStateMap from the given UTF formatted stream */
	public static FullDataPointIdMap deserialize(DhDataInputStream inputStream, long pos, ILevelWrapper levelWrapper) throws IOException, InterruptedException, DataCorruptedException
	{
		int entityCount = inputStream.readInt();
		if (entityCount < 0)
		{
			throw new DataCorruptedException("FullDataPointIdMap deserialize entry count should have a number greater than or equal to 0, returned value ["+entityCount+"].");
		}
		
		
		// only used when debugging
		HashMap<String, BlockBiomeWrapperPair> dataPointEntryBySerialization = new HashMap<>();
		
		FullDataPointIdMap newMap = new FullDataPointIdMap(pos);
		for (int i = 0; i < entityCount; i++)
		{
			// necessary to prevent issues with deserializing objects after the level has been closed
			if (Thread.interrupted())
			{
				throw new InterruptedException("[" + FullDataPointIdMap.class.getSimpleName() + "] deserializing interrupted.");
			}
			
			
			String entryString = inputStream.readUTF();
			BlockBiomeWrapperPair newPair = BlockBiomeWrapperPair.deserialize(entryString, levelWrapper);
			newMap.blockBiomePairList.add(newPair);
			
			if (RUN_SERIALIZATION_DUPLICATE_VALIDATION)
			{
				if (dataPointEntryBySerialization.containsKey(entryString))
				{
					LOGGER.error("Duplicate deserialized entry found with serial: " + entryString);
				}
				if (dataPointEntryBySerialization.containsValue(newPair))
				{
					LOGGER.error("Duplicate deserialized entry found with value: " + newPair.serialize());
				}
				dataPointEntryBySerialization.put(entryString, newPair);
			}
		}
		
		if (newMap.size() != entityCount)
		{
			// if the mappings are out of sync then the LODs will render incorrectly due to IDs being wrong
			LodUtil.assertNotReach("ID maps failed to deserialize for pos: ["+ DhSectionPos.toString(pos)+"], incorrect entity count. Expected count ["+entityCount+"], actual count ["+newMap.size()+"]");
		}
		
		return newMap;
	}
	
	
	
	//===========//
	// overrides //
	//===========//
	
	@Override
	public boolean equals(Object other)
	{
		if (other == this)
			return true;
/*        if (!(other instanceof FullDataPointIdMap)) return false;
		FullDataPointIdMap otherMap = (FullDataPointIdMap) other;
        if (entries.size() != otherMap.entries.size()) return false;
        for (int i=0; i<entries.size(); i++) {
            if (!entries.get(i).equals(otherMap.entries.get(i))) return false;
        }*/
		return false;
	}
	
	/** Only includes the base data in this object, not the mapping */
	@Override
	public int hashCode()
	{
		if (this.cachedHashCode == 0)
		{
			this.generateHashCode();
		}
		return this.cachedHashCode;
	}
	private void generateHashCode()
	{
		int result = DhSectionPos.hashCode(this.pos);
		for (int i = 0; i < this.blockBiomePairList.size(); i++)
		{
			result = 31 * result + this.blockBiomePairList.hashCode();
		}
		this.cachedHashCode = result;
	}
	
	
	
}
