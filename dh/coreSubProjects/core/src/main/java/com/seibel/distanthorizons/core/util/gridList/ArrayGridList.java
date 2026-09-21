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

package com.seibel.distanthorizons.core.util.gridList;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class ArrayGridList<T> extends ArrayList<T>
{
	public final int gridSize;
	
	
	
	//==============//
	// constructors //
	//==============//
	//region
	
	/** @param filler the function called for each index to set the initial values */
	public ArrayGridList(int gridSize, BiFunction<Integer, Integer, T> filler)
	{
		super((gridSize) * (gridSize));
		this.gridSize = gridSize;
		this.forEachPos((x, y) -> super.add(filler.apply(x, y)));
	}
	
	public ArrayGridList(int gridSize) { this(gridSize, (x, y) -> null); }
	
	public ArrayGridList(ArrayGridList<T> copy)
	{
		super(copy);
		this.gridSize = copy.gridSize;
	}
	
	public ArrayGridList(ArrayGridList<T> from, int minR, int maxR)
	{
		super(maxR - minR);
		if (minR > maxR) throw new IndexOutOfBoundsException("minR greater than maxR");
		if (minR < 0) throw new IndexOutOfBoundsException("minR less than 0");
		if (maxR > from.gridSize) throw new IndexOutOfBoundsException("maxR greater than gridSize");
		
		this.gridSize = maxR - minR;
		for (int oy = minR; oy < maxR; oy++)
		{
			int begin = minR + oy * from.gridSize;
			int end = maxR + oy * from.gridSize;
			super.addAll(from.subList(begin, end));
		}
		
		if (super.size() != this.gridSize * this.gridSize)
		{
			throw new IllegalStateException("subgrid clone failure");
		}
	}
	
	//endregion
	
	
	
	//=========//
	// methods //
	//=========//
	//region
	
	protected int getIndexOf(int x, int y) { return x + y * this.gridSize; }
	
	public final T get(MovableGridRingList.Pos2D pos) { return this.get(pos.getX(), pos.getY()); }
	public final T set(MovableGridRingList.Pos2D pos, T e) { return this.set(pos.getX(), pos.getY(), e); }
	public T get(int x, int y)
	{
		if (!this.inRange(x, y))
		{
			return null;
		}
		return this.get(this.getIndexOf(x, y));
	}
	public T getFirst() { return this.get(0, 0); }
	public T getLast() { return this.get(this.gridSize - 1, this.gridSize - 1); }
	
	public T set(int x, int y, T e)
	{
		if (!this.inRange(x, y))
		{
			return null;
		}
		return this.set(this.getIndexOf(x, y), e);
	}
	public T setFirst(T e) { return this.set(0, 0, e); }
	public T setLast(T e) { return this.set(this.gridSize - 1, this.gridSize - 1, e); }
	
	public boolean inRange(int x, int y)
	{
		return (x >= 0 && x < this.gridSize &&
				y >= 0 && y < this.gridSize);
	}
	
	public final void clear() { this.clear(null); }
	public final void fill(BiFunction<Integer, Integer, T> filler) { this.fill(null, filler); }
	public final void clear(Consumer<? super T> dealloc) { this.fill(dealloc, (x, y) -> null); }
	public final void fill(
			Consumer<? super T> dealloc,
			BiFunction<Integer, Integer, T> filler)
	{
		this.forEachPos((x, y) -> {
			T t = this.set(x, y, filler.apply(x, y));
			if (t != null) dealloc.accept(t);
		});
	}
	
	//endregion
	
	
	
	//===========//
	// iterators //
	//===========//
	//region
	
	public void forEachPos(BiConsumer<Integer, Integer> consumer)
	{
		for (int y = 0; y < this.gridSize; y++)
		{
			for (int x = 0; x < this.gridSize; x++)
			{
				consumer.accept(x, y);
			}
		}
	}
	
	@Override
	public final void forEach(Consumer<? super T> consumer) { super.forEach(consumer); }
	
	//endregion
	
	
	
	//================//
	// base overrides //
	//================//
	//region
	
	@Override
	public String toString() { return this.getClass().toString() + " " + this.gridSize + "*" + this.gridSize + "[" + this.size() + "]"; }
	
	public String toDetailString()
	{
		StringBuilder str = new StringBuilder("\n");
		int i = 0;
		str.append(this);
		str.append("\n");
		for (T t : this)
		{
			
			str.append(t != null ? t.toString() : "NULL");
			str.append(", ");
			i++;
			if (i % this.gridSize == 0)
			{
				str.append("\n");
			}
		}
		return str.toString();
	}
	
	//endregion
	
	
	
}
