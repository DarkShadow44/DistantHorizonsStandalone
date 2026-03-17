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

package com.seibel.distanthorizons.core.util;

import java.util.Arrays;
import java.util.List;

import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.render.vertexFormat.DefaultLodVertexFormats;
import com.seibel.distanthorizons.core.render.vertexFormat.LodVertexFormat;
import com.seibel.distanthorizons.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.distanthorizons.core.logging.DhLogger;

/**
 * This class holds methods and constants that may be used in multiple places.
 */
public class LodUtil
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	
	/**
	 * In order of nearest to farthest: <br>
	 * Red, Orange, Yellow, Green, Cyan, Blue, Magenta, white, gray, black
	 */
	public static final int[] DEBUG_DETAIL_LEVEL_COLORS = new int[]{
			ColorUtil.rgbToInt(255, 0, 0), ColorUtil.rgbToInt(255, 127, 0),
			ColorUtil.rgbToInt(255, 255, 0), ColorUtil.rgbToInt(127, 255, 0),
			ColorUtil.rgbToInt(0, 255, 0), ColorUtil.rgbToInt(0, 255, 127),
			ColorUtil.rgbToInt(0, 255, 255), ColorUtil.rgbToInt(0, 127, 255),
			ColorUtil.rgbToInt(0, 0, 255), ColorUtil.rgbToInt(127, 0, 255),
			ColorUtil.rgbToInt(255, 0, 255), ColorUtil.rgbToInt(255, 127, 255),
			ColorUtil.rgbToInt(255, 255, 255)};
	
	
	/** 512 blocks wide */
	public static final byte REGION_DETAIL_LEVEL = 9;
	/** 16 blocks wide */
	public static final byte CHUNK_DETAIL_LEVEL = 4;
	/** 1 block wide */
	public static final byte BLOCK_DETAIL_LEVEL = 0;
	
	/**
	 * measured in Blocks <br>
	 * detail level 9
	 * 512 x 512 blocks
	 */
	public static final short REGION_WIDTH = 512;
	/**
	 * measured in Blocks <br>
	 * detail level 4
	 * 16 x 16 blocks
	 */
	public static final short CHUNK_WIDTH = 16;
	
	
	/** number of chunks wide */
	public static final int REGION_WIDTH_IN_CHUNKS = REGION_WIDTH / CHUNK_WIDTH;
	
	
	/** maximum possible light level handled by Minecraft */
	public static final byte MAX_MC_LIGHT = 15;
	/** lowest possible light level handled by Minecraft */
	public static final byte MIN_MC_LIGHT = 0;
	
	/** the opacity value returned by {@link IBlockStateWrapper#getOpacity()} if a block is fully transparent */
	public static final int BLOCK_FULLY_TRANSPARENT = 0;
	/** the opacity value returned by {@link IBlockStateWrapper#getOpacity()} if a block is fully opaque */
	public static final int BLOCK_FULLY_OPAQUE = 16;
	
	/**
	 * List of every block that can be used in a beacon's base. <br> 
	 * Should be all lowercase 
	 */
	public static final List<String> BEACON_BASE_BLOCK_NAME_LIST = Arrays.asList(
			"iron_block",
			"gold_block",
			"diamond_block",
			"emerald_block",
			"netherite_block"
	);
	
	
	
	/**
	 * This regex finds any characters that are invalid for use in a windows
	 * (and by extension mac and linux) file path
	 */
	public static final String INVALID_FILE_CHARACTERS_REGEX = "[\\\\/:*?\"<>|]";
	
	/**
	 * 64 MB by default is the maximum amount of memory that
	 * can be directly allocated. <br><br>
	 * <p>
	 * James knows there are commands to change that amount
	 * (specifically "-XX:MaxDirectMemorySize"), but
	 * He has no idea how to access that amount. <br>
	 * So for now this will be the hard limit. <br><br>
	 * <p>
	 * https://stackoverflow.com/questions/50499238/bytebuffer-allocatedirect-and-xmx
	 */
	public static final int MAX_ALLOCATABLE_DIRECT_MEMORY = 64 * 1024 * 1024;
	
	/** the format of data stored in the GPU buffers */
	public static final LodVertexFormat LOD_VERTEX_FORMAT = DefaultLodVertexFormats.POSITION_COLOR_BLOCK_LIGHT_SKY_LIGHT_MATERIAL_ID_NORMAL_INDEX;
	
	
	
	//=========//
	// methods //
	//=========//
	
	/** Returns the chunk int position for the given double position */
	public static int getChunkPosFromDouble(double value) { return (int) Math.floor(value / CHUNK_WIDTH); }
	/** Returns the float position inside the chunk for the given double position */
	public static float getSubChunkPosFromDouble(double value)
	{
		double chunkPos = Math.floor(value / CHUNK_WIDTH);
		return (float) (value - chunkPos * CHUNK_WIDTH);
	}
	
	
	/**
	 * Returns true if the requested threshold passes, false otherwise.
	 * For details, see:
	 * https://stackoverflow.com/questions/3571203/what-are-runtime-getruntime-totalmemory-and-freememory
	 */
	public static boolean checkRamUsage(double minFreeMemoryPercent, int minFreeMemoryMB)
	{
		long freeMem = Runtime.getRuntime().freeMemory() + Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory();
		if (freeMem < minFreeMemoryMB * 1024L * 1024L) return false;
		
		long maxMem = Runtime.getRuntime().maxMemory();
		if (freeMem / (double) maxMem < minFreeMemoryPercent) return false;
		
		return true;
	}
	
	public static class AssertFailureException extends RuntimeException
	{
		public AssertFailureException(String message)
		{
			super(message);
		}
		
	}
	
	public static void assertTrue(boolean condition)
	{
		if (!condition)
		{
			throw new AssertFailureException("Assertion failed");
		}
	}
	public static void assertTrue(boolean condition, String message)
	{
		if (!condition)
		{
			throw new AssertFailureException("Assertion failed:\n " + message);
		}
	}
	public static void assertNotReach(String message)
	{
		throw new AssertFailureException("Assert Not Reach failed:\n " + message);
	}
	
	
	
}
