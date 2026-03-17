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

package com.seibel.distanthorizons.core.dataObjects.render;

import com.seibel.distanthorizons.api.enums.config.EDhApiVerticalQuality;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.util.objects.pooling.AbstractPhantomArrayList;
import com.seibel.distanthorizons.core.util.objects.pooling.PhantomArrayListPool;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.dataObjects.render.columnViews.ColumnRenderView;
import com.seibel.distanthorizons.core.util.RenderDataPointUtil;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import com.seibel.distanthorizons.core.logging.DhLogger;

/**
 * Stores the render data used to generate OpenGL buffers.
 *
 * @see RenderDataPointUtil
 */
public class ColumnRenderSource extends AbstractPhantomArrayList
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	/** measured in data columns */
	public static final int WIDTH = 64;
	
	public static final PhantomArrayListPool ARRAY_LIST_POOL = new PhantomArrayListPool("Render Source");
	
	
	
	/** 
	 * will be zero if an empty data source was created 
	 * @see EDhApiVerticalQuality#calculateMaxNumberOfVerticalSlicesAtDetailLevel(byte) 
	 */
	public int maxVerticalSliceCount;
	public long pos;
	public int yOffset;
	
	public final LongArrayList renderDataContainer;
	
	private boolean isEmpty = true;
	
	
	
	//==============//
	// constructors //
	//==============//
	//region
	
	public static ColumnRenderSource createEmpty(long pos, int maxVertSliceCount, int yOffset)
	{ return new ColumnRenderSource(pos, maxVertSliceCount, yOffset); }
	/**
	 * Creates an empty ColumnRenderSource.
	 *
	 * @param pos the relative position of the container
	 * @param maxVertSliceCount the maximum vertical size of the container
	 */
	private ColumnRenderSource(long pos, int maxVertSliceCount, int yOffset)
	{
		super(ARRAY_LIST_POOL, 0, 0, 1, 0);
		
		this.pos = pos;
		this.yOffset = yOffset;
		
		this.maxVerticalSliceCount = maxVertSliceCount;
		
		this.renderDataContainer = this.pooledArraysCheckout.getLongArray(0, WIDTH * WIDTH * this.maxVerticalSliceCount);
	}
	
	//endregion
	
	
	
	//========================//
	// datapoint manipulation //
	//========================//
	//region
	
	public long getDataPoint(int posX, int posZ, int verticalIndex) { return this.renderDataContainer.getLong(posX * WIDTH * this.maxVerticalSliceCount + posZ * this.maxVerticalSliceCount + verticalIndex); }
	
	public void populateColumnView(ColumnRenderView view, int posX, int posZ) throws IllegalArgumentException
	{
		int offset = posX * WIDTH * this.maxVerticalSliceCount + posZ * this.maxVerticalSliceCount;
		
		// don't allow returning views that are outside this render source's bounds
		if (offset >= this.renderDataContainer.size())
		{
			throw new IllegalArgumentException("Column View offset ["+offset+"] greater than parent render data container ["+DhSectionPos.toString(this.pos)+"] size ["+this.renderDataContainer.size()+"].");
		}
		else if (posX < 0 || posX >= WIDTH
				|| posZ < 0 || posZ >= WIDTH)
		{
			throw new IllegalArgumentException("Column View pos outside valid range ["+posX+","+posZ+"].");
		}
		
		view.populate(
			this.renderDataContainer, this.maxVerticalSliceCount,
			offset, this.maxVerticalSliceCount);
	}
	
	//endregion
	
	
	
	//=====================//
	// data helper methods //
	//=====================//
	//region
	
	public Long getPos() { return this.pos; }
	public Long getKey() { return this.pos; }
	
	public byte getDataDetailLevel() { return (byte) (DhSectionPos.getDetailLevel(this.pos) - DhSectionPos.SECTION_MINIMUM_DETAIL_LEVEL); }
	
	public boolean isEmpty() { return this.isEmpty; }
	public void markNotEmpty() { this.isEmpty = false; }
	
	/** can be used when debugging */
	public boolean hasNonVoidDataPoints()
	{
		if (this.isEmpty)
		{
			return false;
		}
		
		try (ColumnRenderView columnView = ColumnRenderView.getPooled())
		{
			for (int x = 0; x < WIDTH; x++)
			{
				for (int z = 0; z < WIDTH; z++)
				{
					this.populateColumnView(columnView, x, z);
					for (int i = 0; i < columnView.size; i++)
					{
						long dataPoint = columnView.get(i);
						if (!RenderDataPointUtil.hasZeroHeight(dataPoint))
						{
							return true;
						}
					}
				}
			}
		}
		
		return false;
	}
	
	//endregion
	
	
	
	//==============//
	// base methods //
	//==============//
	//region
	
	@Override
	public String toString()
	{
		String LINE_DELIMITER = "\n";
		String DATA_DELIMITER = " ";
		String SUBDATA_DELIMITER = ",";
		StringBuilder stringBuilder = new StringBuilder();
		
		stringBuilder.append(DhSectionPos.toString(this.pos));
		stringBuilder.append(LINE_DELIMITER);
		
		int size = 1;
		for (int z = 0; z < size; z++)
		{
			for (int x = 0; x < size; x++)
			{
				for (int y = 0; y < this.maxVerticalSliceCount; y++)
				{
					//Converting the dataToHex
					stringBuilder.append(Long.toHexString(this.getDataPoint(x, z, y)));
					if (y != this.maxVerticalSliceCount - 1)
						stringBuilder.append(SUBDATA_DELIMITER);
				}
				
				if (x != size - 1)
					stringBuilder.append(DATA_DELIMITER);
			}
			
			if (z != size - 1)
				stringBuilder.append(LINE_DELIMITER);
		}
		return stringBuilder.toString();
	}
	
	//endregion
	
	
	
}
