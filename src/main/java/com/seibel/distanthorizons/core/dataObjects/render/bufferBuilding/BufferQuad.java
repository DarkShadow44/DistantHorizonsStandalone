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

package com.seibel.distanthorizons.core.dataObjects.render.bufferBuilding;

import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.enums.EDhDirection;
import com.seibel.distanthorizons.core.util.LodUtil;

/** Represents a render-able quad. */
public final class BufferQuad
{
	/**
	 * The maximum number of blocks wide a quad can be. <br><br>
	 *
	 * This could be increased beyond 2048, for use with
	 * extremely low detail levels if the need arises.
	 */
	public static final int NORMAL_MAX_QUAD_WIDTH = 2048;
	/**
	 * The maximum number of blocks wide a quad can be
	 * when {@link Config.Client.Advanced.Graphics.Experimental#earthCurveRatio earthCurveRatio}
	 * is enabled.
	 */
	public static final int MAX_QUAD_WIDTH_FOR_EARTH_CURVATURE = LodUtil.CHUNK_WIDTH;
	
	
	public final short x;
	public final short y;
	public final short z;
	
	public short widthEastWest;
	/** This is both North/South and Up/Down since the merging logic is the same either way */
	public short widthNorthSouthOrHeight;
	
	public final int color;
	/** used by the Iris shader mod to determine how each LOD should be rendered */
	public final byte irisBlockMaterialId;
	
	public final byte skyLight;
	public final byte blockLight;
	public final EDhDirection direction;

	public boolean hasError = false;

	// Pre-computed sort keys to avoid recomputing on every comparision
	// Slight increase in memory for reduction in cpu usage
	public final long sortKeyEW;
	public final long sortKeyNS;



	BufferQuad(
			short x, short y, short z, short widthEastWest, short widthNorthSouthOrHeight,
			int color, byte irisBlockMaterialId, byte skylight, byte blockLight,
			EDhDirection direction)
	{
		if (widthEastWest == 0 || widthNorthSouthOrHeight == 0)
		{
			throw new IllegalArgumentException("Size 0 quad!");
		}
		if (widthEastWest < 0 || widthNorthSouthOrHeight < 0)
		{
			throw new IllegalArgumentException("Negative sized quad!");
		}

		this.x = x;
		this.y = y;
		this.z = z;
		this.widthEastWest = widthEastWest;
		this.widthNorthSouthOrHeight = widthNorthSouthOrHeight;
		this.color = color;
		this.irisBlockMaterialId = irisBlockMaterialId;
		this.skyLight = skylight;
		this.blockLight = blockLight;
		this.direction = direction;
		this.sortKeyEW = computeSortKey(direction, true);
		this.sortKeyNS = computeSortKey(direction, false);
	}

	private long computeSortKey(EDhDirection dir, boolean eastWest)
	{
		if (eastWest)
		{
			switch (dir.axis)
			{
				case X: return (long) x << 48 | (long) y << 32 | (long) z << 16;
				case Y: return (long) y << 48 | (long) z << 32 | (long) x << 16;
				case Z: return (long) z << 48 | (long) y << 32 | (long) x << 16;
				default: return 0;
			}
		}
		else
		{
			switch (dir.axis)
			{
				case X: return (long) x << 48 | (long) z << 32 | (long) y << 16;
				case Y: return (long) y << 48 | (long) x << 32 | (long) z << 16;
				case Z: return (long) z << 48 | (long) x << 32 | (long) y << 16;
				default: return 0;
			}
		}
	}
	
	
	
	/** a rough but fast calculation */
	double calculateDistance(double relativeX, double relativeY, double relativeZ)
	{
		return Math.pow(relativeX - this.x, 2) + Math.pow(relativeY - this.y, 2) + Math.pow(relativeZ - this.z, 2);
	}
	
	/** compares this quad's position to the given quad using pre-computed sort keys */
	public int compare(BufferQuad quad, BufferMergeDirectionEnum compareDirection)
	{
		return compareDirection == BufferMergeDirectionEnum.EastWest
			? Long.compare(this.sortKeyEW, quad.sortKeyEW)
			: Long.compare(this.sortKeyNS, quad.sortKeyNS);
	}
	
	
	/**
	 * Attempts to merge the given quad into this one.
	 *
	 * @return true if the quads were merged, false otherwise.
	 */
	public boolean tryMerge(BufferQuad quad, BufferMergeDirectionEnum mergeDirection)
	{
		if (quad.hasError || this.hasError)
			return false;
		
		// only merge quads that are in the same direction
		if (this.direction != quad.direction)
			return false;
		
		// make sure these quads share the same perpendicular axis
		if ((mergeDirection == BufferMergeDirectionEnum.EastWest && this.y != quad.y)
			|| (mergeDirection == BufferMergeDirectionEnum.NorthSouthOrUpDown && this.x != quad.x))
		{
			return false;
		}
		
		
		// get the position of each quad to compare against
		short thisPerpendicularCompareStartPos; // edge perpendicular to the merge direction
		short thisParallelCompareStartPos; // edge parallel to the merge direction
		short otherPerpendicularCompareStartPos;
		short otherParallelCompareStartPos;
		switch (this.direction.axis)
		{
			default: // shouldn't normally happen, just here to make the compiler happy
			case X:
				if (mergeDirection == BufferMergeDirectionEnum.EastWest)
				{
					thisPerpendicularCompareStartPos = this.z;
					thisParallelCompareStartPos = this.x;
					
					otherPerpendicularCompareStartPos = quad.z;
					otherParallelCompareStartPos = quad.x;
				}
				else //if (mergeDirection == MergeDirection.NorthSouthOrUpDown)
				{
					thisPerpendicularCompareStartPos = this.y;
					thisParallelCompareStartPos = this.z;
					
					otherPerpendicularCompareStartPos = quad.y;
					otherParallelCompareStartPos = quad.z;
				}
				break;
			
			case Y:
				if (mergeDirection == BufferMergeDirectionEnum.EastWest)
				{
					thisPerpendicularCompareStartPos = this.x;
					thisParallelCompareStartPos = this.z;
					
					otherPerpendicularCompareStartPos = quad.x;
					otherParallelCompareStartPos = quad.z;
				}
				else //if (mergeDirection == MergeDirection.NorthSouthOrUpDown)
				{
					thisPerpendicularCompareStartPos = this.z;
					thisParallelCompareStartPos = this.y;
					
					otherPerpendicularCompareStartPos = quad.z;
					otherParallelCompareStartPos = quad.y;
				}
				break;
			
			case Z:
				if (mergeDirection == BufferMergeDirectionEnum.EastWest)
				{
					thisPerpendicularCompareStartPos = this.x;
					thisParallelCompareStartPos = this.z;
					
					otherPerpendicularCompareStartPos = quad.x;
					otherParallelCompareStartPos = quad.z;
				}
				else //if (mergeDirection == MergeDirection.NorthSouthOrUpDown)
				{
					thisPerpendicularCompareStartPos = this.y;
					thisParallelCompareStartPos = this.z;
					
					otherPerpendicularCompareStartPos = quad.y;
					otherParallelCompareStartPos = quad.z;
				}
				break;
		}
		
		// get the width of this quad in the relevant axis
		short thisPerpendicularCompareWidth;
		short thisParallelCompareWidth;
		short otherPerpendicularCompareWidth;
		short otherParallelCompareWidth;
		if (mergeDirection == BufferMergeDirectionEnum.EastWest)
		{
			thisPerpendicularCompareWidth = this.widthEastWest;
			thisParallelCompareWidth = this.widthNorthSouthOrHeight;
			
			otherPerpendicularCompareWidth = quad.widthEastWest;
			otherParallelCompareWidth = quad.widthNorthSouthOrHeight;
		}
		else
		{
			thisPerpendicularCompareWidth = this.widthNorthSouthOrHeight;
			thisParallelCompareWidth = this.widthEastWest;
			
			otherPerpendicularCompareWidth = quad.widthNorthSouthOrHeight;
			otherParallelCompareWidth = quad.widthEastWest;
		}
		
		
		
		// quad width should only be limited when earth curvature is enabled
		int maxQuadWidth = NORMAL_MAX_QUAD_WIDTH;
		if (Config.Client.Advanced.Graphics.Experimental.earthCurveRatio.get() != 0)
		{
			maxQuadWidth = MAX_QUAD_WIDTH_FOR_EARTH_CURVATURE;
		}
		
		// FIXME: TEMP: Hard limit for width
		if (thisPerpendicularCompareWidth >= maxQuadWidth)
		{
			return false;
		}
		if (Math.floorDiv(otherPerpendicularCompareStartPos, maxQuadWidth)
				!= Math.floorDiv(thisPerpendicularCompareStartPos, maxQuadWidth))
		{
			return false;
		}
		
		
		// check if these quads are adjacent
		if (thisPerpendicularCompareStartPos + thisPerpendicularCompareWidth < otherPerpendicularCompareStartPos ||
				thisParallelCompareStartPos != otherParallelCompareStartPos)
		{
			// these quads aren't adjacent, they can't be merged
			return false;
		}
		else if (thisPerpendicularCompareStartPos + thisPerpendicularCompareWidth > otherPerpendicularCompareStartPos)
		{
			if (thisPerpendicularCompareStartPos < otherPerpendicularCompareStartPos + otherPerpendicularCompareWidth)
			{
				// these quads are overlapping, they can't be merged
				
				// Overlapping quads appear to render correctly, why are we marking them as errored?
				// Is it possible the wrong quad will be extended thus the wrong color is rendered?
				// Or is that the height/depth might be wrong?
				if (Config.Client.Advanced.Debugging.showOverlappingQuadErrors.get())
				{
					quad.hasError = true;
					this.hasError = true;
				}
			}
			
			return false;
		}
		
		// only merge quads that have the same width edges
		if (thisParallelCompareWidth != otherParallelCompareWidth)
		{
			return false;
		}
		
		// do the quads' color, light, etc. match?
		if (this.color != quad.color ||
				this.irisBlockMaterialId != quad.irisBlockMaterialId ||
				this.skyLight != quad.skyLight ||
				this.blockLight != quad.blockLight)
		{
			// we can only merge identically colored/lit quads
			return false;
		}
		
		// merge the two quads
		if (mergeDirection == BufferMergeDirectionEnum.NorthSouthOrUpDown)
		{
			this.widthNorthSouthOrHeight += quad.widthNorthSouthOrHeight;
		}
		else // if (mergeDirection == MergeDirection.EastWest)
		{
			this.widthEastWest += quad.widthEastWest;
		}
		
		// merge successful
		return true;
	}
	
}
