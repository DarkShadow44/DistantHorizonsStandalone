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

package com.seibel.distanthorizons.core.dataObjects.render.columnViews;


import com.seibel.distanthorizons.core.dataObjects.render.ColumnRenderSource;
import com.seibel.distanthorizons.core.util.RenderDataPointUtil;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.util.Arrays;
import java.util.ConcurrentModificationException;

public final class ColumnArrayView implements IColumnDataView
{
	public final LongArrayList data;
    public final ByteArrayList dataAdditional;

	/**
	 * How many data points are currently being represented by this view. <br>
	 * Will be equal to or less than {@link ColumnArrayView#verticalSize}.
	 */
	public final int size;
	/** 
	 * Vertical size in data points. <Br>
	 * Can be 0 if this column was created for an empty data source.
	 */
	public final int verticalSize;
	
	/**
	 * Where the relative starting index is in the {@link ColumnArrayView#data} array
	 * if this view is representing part of a {@link ColumnRenderSource}.
	 */
	public final int offset;
	
	
	
	//=============//
	// constructor //
	//=============//
	
	/** @throws IllegalArgumentException if the offset is greater than the data's size */
	public ColumnArrayView(LongArrayList data, ByteArrayList dataAdditional, int size, int offset, int verticalSize) throws IllegalArgumentException
	{
		this.data = data;
        this.dataAdditional = dataAdditional;
		this.size = size;
		this.offset = offset;
		this.verticalSize = verticalSize;
		
		if (this.data.size() < this.offset)
		{
			throw new IllegalArgumentException("data size ["+this.data.size()+"] is shorter than offset ["+this.offset+"].");
		}
	}
	
	
	
	//=====================//
	// getters and setters //
	//=====================//
	
	@Override
    public long get(int index)
    {
        try
        {
            return this.data.getLong(index + this.offset);
        }
        catch (IndexOutOfBoundsException e)
        {
            // we can fairly confidently say this is a concurrent exception over an actual
            // index out of bounds, since we're generally iterating over the whole
            // array any time we use this getter.
            throw new ConcurrentModificationException("Potential concurrent modification detected. Make sure the parent ColumnRenderSource isn't being closed before the ColumnArrayView processing is complete.", e);
        }
    }
    public byte getAdditional(int index)
    {
        return this.dataAdditional.getByte(index + this.offset);
    }
    public void set(int index, long value) { data.set(index + offset, value); }

    public void setAdditional(int index, byte value) { dataAdditional.set(index + offset, value); }

	@Override
	public int size() { return size; }
	@Override
	public int verticalSize() { return verticalSize; }

	@Override
	public int dataCount() { return (this.verticalSize != 0) ? (this.size / this.verticalSize) : 0; } // TODO what does the divide by mean?

	@Override
	public ColumnArrayView subView(int dataIndexStart, int dataCount)
	{
		return new ColumnArrayView(data, dataAdditional, dataCount * verticalSize, offset + dataIndexStart * verticalSize, verticalSize);
	}

	public void fill(long value, byte valueAdditional) { Arrays.fill(data.elements(), offset, offset + size, value);
        Arrays.fill(dataAdditional.elements(), offset, offset + size, valueAdditional);}

	public void copyFrom(IColumnDataView source) { copyFrom(source, 0); }
	public void copyFrom(IColumnDataView source, int outputDataIndexOffset)
	{
		if (source.verticalSize() > verticalSize)
		{
			throw new IllegalArgumentException("source verticalSize must be <= self's verticalSize to copy");
		}
		else if (source.dataCount() + outputDataIndexOffset > dataCount())
		{
			throw new IllegalArgumentException("dataIndexStart + source.dataCount() must be <= self.dataCount() to copy");
		}
		else if (source.verticalSize() != verticalSize)
		{
			for (int i = 0; i < source.dataCount(); i++)
			{
				int outputOffset = offset + outputDataIndexOffset * verticalSize + i * verticalSize;
				source.subView(i, 1).copyTo(data.elements(), dataAdditional.elements(), outputOffset, source.verticalSize());
                Arrays.fill(data.elements(), outputOffset + source.verticalSize(),
                    outputOffset + verticalSize, 0);
                Arrays.fill(dataAdditional.elements(), outputOffset + source.verticalSize(),
                    outputOffset + verticalSize, (byte)0);
			}
		}
		else
		{
			source.copyTo(data.elements(), dataAdditional.elements(), offset + outputDataIndexOffset * verticalSize, source.size());
		}
	}

	@Override
	public void copyTo(long[] target, byte[] targetAdditional, int offset, int size) {
        System.arraycopy(data.elements(), this.offset, target, offset, size);
        System.arraycopy(dataAdditional.elements(), this.offset, targetAdditional, offset, size);
    }

	public boolean mergeWith(ColumnArrayView source, boolean override)
	{
		if (size != source.size)
		{
			throw new IllegalArgumentException("Cannot merge views of different sizes");
		}
		if (verticalSize != source.verticalSize)
		{
			throw new IllegalArgumentException("Cannot merge views of different vertical sizes");
		}
		boolean anyChange = false;
		for (int o = 0; o < (source.size() * verticalSize); o += verticalSize)
		{
			if (override)
			{
				if (RenderDataPointUtil.compareDatapointPriority(source.get(o), get(o)) >= 0)
				{
					anyChange = true;
					System.arraycopy(source.data, source.offset + o, data, offset + o, verticalSize);
				}
			}
			else
			{
				if (RenderDataPointUtil.compareDatapointPriority(source.get(o), get(o)) > 0)
				{
					anyChange = true;
					System.arraycopy(source.data, source.offset + o, data, offset + o, verticalSize);
				}
			}
		}
		return anyChange;
	}
	
	public void changeVerticalSizeFrom(IColumnDataView source)
	{
		if (this.dataCount() != source.dataCount())
		{
			throw new IllegalArgumentException("Cannot copy and resize to views with different dataCounts");
		}
		
		if (this.verticalSize >= source.verticalSize())
		{
			this.copyFrom(source);
		}
		else
		{
			for (int i = 0; i < this.dataCount(); i++)
			{
				RenderDataPointUtil.mergeMultiData(source.subView(i, 1), subView(i, 1));
			}
		}
	}
	
	public void mergeMultiDataFrom(IColumnDataView source)
	{
		if (dataCount() != 1)
		{
			throw new IllegalArgumentException("output dataCount must be 1");
		}
		
		RenderDataPointUtil.mergeMultiData(source, this);
	}
	
	
	
	//================//
	// base overrides //
	//================//
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("S:").append(size);
		sb.append(" V:").append(verticalSize);
		sb.append(" O:").append(offset);
		
		sb.append(" [");
		for (int i = 0; i < size; i++)
		{
			sb.append(RenderDataPointUtil.toString(data.getLong(offset + i)));
			if (i < size - 1)
			{
				sb.append(",\n");
			}
		}
		sb.append("]");
		
		return sb.toString();
	}
	
	
	public int getDataHash()
	{
		return arrayHash(data, offset, size);
	}
	
	private static int arrayHash(LongArrayList a, int offset, int length)
	{
		if (a == null)
		{
			return 0;
		}
		
		int result = 1;
		int end = offset + length;
		for (int i = offset; i < end; i++)
		{
			long element = a.getLong(i);
			int elementHash = (int) (element ^ (element >>> 32));
			result = 31 * result + elementHash;
		}
		return result;
	}
	
}