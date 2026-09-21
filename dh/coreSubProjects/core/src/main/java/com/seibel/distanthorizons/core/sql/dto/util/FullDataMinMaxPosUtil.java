package com.seibel.distanthorizons.core.sql.dto.util;

import com.seibel.distanthorizons.core.dataObjects.fullData.sources.FullDataSourceV2;
import com.seibel.distanthorizons.core.enums.EDhDirection;

/**
 * Handles encoding/decoding of min/max X/Z relative {@link FullDataSourceV2#dataPoints}
 * positions. <br>
 * Needed so we can keep the same format between complete data sources
 * and incomplete adjacent-only data sources.
 */
public class FullDataMinMaxPosUtil
{
	private static final int ADJ_POS_MASK = (int) Math.pow(2, Short.SIZE) - 1;
	private static final int MIN_X_OFFSET = 0;
	private static final int MAX_X_OFFSET = Short.SIZE;
	private static final int MIN_Z_OFFSET = Short.SIZE * 2;
	private static final int MAX_Z_OFFSET = Short.SIZE * 3;
	
	
	
	/**
	 * Encodes min/max X/Z relative {@link FullDataSourceV2#dataPoints}
	 * positions. <br>
	 * Needed so we can keep the same format between complete data sources
	 * and incomplete adjacent-only data sources.
	 */
	public static long getEncodedMinMaxPos(EDhDirection direction)
	{
		// 4 shorts can fit in a long, and we won't need anything longer than 64 anyway
		short minX;
		short maxX;
		short minZ;
		short maxZ;
		
		switch (direction)
		{
			case NORTH:
				// one row closest to the negative Z axis
				minX = 0;
				maxX = FullDataSourceV2.WIDTH;
				
				minZ = 0;
				maxZ = 1;
				break;
			
			case SOUTH:
				// one row closest to the positive Z axis
				minX = 0;
				maxX = FullDataSourceV2.WIDTH;
				
				minZ = FullDataSourceV2.WIDTH - 1;
				maxZ = FullDataSourceV2.WIDTH;
				break;
			
			case EAST:
				// one row closest to the positive X axis
				minX = FullDataSourceV2.WIDTH - 1;
				maxX = FullDataSourceV2.WIDTH;
				
				minZ = 0;
				maxZ = FullDataSourceV2.WIDTH;
				break;
			
			case WEST:
				// one row closest to the Negative X axis
				minX = 0;
				maxX = 1;
				
				minZ = 0;
				maxZ = FullDataSourceV2.WIDTH;
				break;
			
			default:
				throw new IllegalArgumentException("Unsupported direction [" + direction + "].");
		}
		
		return encodeAdjMinMaxPos(
				minX, maxX,
				minZ, maxZ);
	}
	
	public static long encodeAdjMinMaxPos(
			short minX, short maxX,
			short minZ, short maxZ
	)
	{
		long data = 0L;
		data |= (long) minX << MIN_X_OFFSET;
		data |= (long) maxX << MAX_X_OFFSET;
		data |= (long) minZ << MIN_Z_OFFSET;
		data |= (long) maxZ << MAX_Z_OFFSET;
		return data;
	}
	
	public static int getAdjMinX(long encodedMinMaxPos)
	{ return (int) ((encodedMinMaxPos >> MIN_X_OFFSET) & ADJ_POS_MASK); }
	public static int getAdjMaxX(long encodedMinMaxPos)
	{ return (int) ((encodedMinMaxPos >> MAX_X_OFFSET) & ADJ_POS_MASK); }
	
	public static int getAdjMinZ(long encodedMinMaxPos)
	{ return (int) ((encodedMinMaxPos >> MIN_Z_OFFSET) & ADJ_POS_MASK); }
	public static int getAdjMaxZ(long encodedMinMaxPos)
	{ return (int) ((encodedMinMaxPos >> MAX_Z_OFFSET) & ADJ_POS_MASK); }
	
	
	
}
