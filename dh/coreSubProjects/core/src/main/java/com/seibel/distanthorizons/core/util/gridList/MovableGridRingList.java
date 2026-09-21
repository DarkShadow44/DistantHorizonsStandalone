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

import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.coreapi.ModInfo;
import com.seibel.distanthorizons.coreapi.util.MathUtil;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MovableGridRingList<T> extends ArrayList<T> implements List<T>
{
	/** the position of this grid closest to negative x/z infinity */
	private final AtomicReference<Pos2D> minPosRef = new AtomicReference<>();
	
	/** width of this grid list */
	private final int width;
	/** radius or half-width of this grid list */
	private final int halfWidth;
	
	private final ReentrantReadWriteLock moveLock = new ReentrantReadWriteLock();
	
	/** used to iterate over each item in the list in an in-to-out order */
	private final Pos2D[] ringPositionIteratorArray;
	
	
	
	//==============//
	// constructors //
	//==============//
	//region
	
	public MovableGridRingList(int halfWidth, int centerX, int centerY)
	{
		super((halfWidth * 2 + 1) * (halfWidth * 2 + 1));
		
		this.width = halfWidth * 2 + 1;
		this.halfWidth = halfWidth;
		this.minPosRef.set(new Pos2D(centerX - halfWidth, centerY - halfWidth));
		this.ringPositionIteratorArray = this.createRingIteratorList();
		
		this.clear();
	}
	private Pos2D[] createRingIteratorList()
	{
		Pos2D[] posArray = new Pos2D[this.width * this.width];
		
		int i = 0;
		for (int xPos = -this.halfWidth; xPos <= this.halfWidth; xPos++)
		{
			for (int zPos = -this.halfWidth; zPos <= this.halfWidth; zPos++)
			{
				posArray[i] = new Pos2D(xPos, zPos);
				i++;
			}
		}
		
		// sort the positions from nearest to farthest from the world origin
		Arrays.sort(posArray, (a, b) ->
		{
			long disSqrA = (long) a.getX() * a.getX() + (long) a.getY() * a.getY();
			long disSqrB = (long) b.getX() * b.getX() + (long) b.getY() * b.getY();
			return Double.compare(disSqrA, disSqrB);
		});
		
		//noinspection SuspiciousNameCombination
		Pos2D halfPos = new Pos2D(this.halfWidth, this.halfWidth);
		for (int j = 0; j < posArray.length; j++)
		{
			posArray[j] = posArray[j].add(halfPos);
		}
		
		// assert all the positions are in the correct range
		if (ModInfo.IS_DEV_BUILD)
		{
			for (Pos2D pos2D : posArray)
			{
				LodUtil.assertTrue(pos2D.getX() >= 0 && pos2D.getX() < this.width);
				LodUtil.assertTrue(pos2D.getY() >= 0 && pos2D.getY() < this.width);
			}
		}
		
		return posArray;
	}
	
	//endregion
	
	
	
	//=====================//
	// getters and setters //
	//=====================//
	//region
	
	/** see {@link MovableGridRingList#get(int, int)} for full documentation */
	public T get(Pos2D pos) { return this.get(pos.getX(), pos.getY()); }
	/** returns null if x,y is outside the grid */
	public T get(int x, int y)
	{
		Pos2D min = this.minPosRef.get();
		if (!this.inRangeAcquired(x, y, min))
		{
			return null;
		}
		
		this.moveLock.readLock().lock();
		try
		{
			Pos2D newMin = this.minPosRef.get();
			// Use EXACT compare here
			if (min != newMin)
			{
				if (!this.inRangeAcquired(x, y, newMin))
				{
					return null;
				}
			}
			return this.getUnsafe(x, y);
		}
		finally
		{
			this.moveLock.readLock().unlock();
		}
	}
	
	
	/** see {@link MovableGridRingList#set(int, int, T)} for full documentation */
	public boolean set(Pos2D pos, T item) { return this.set(pos.getX(), pos.getY(), item); }
	/** returns false if x,y is outside the grid */
	public boolean set(int x, int y, T item)
	{
		Pos2D min = this.minPosRef.get();
		if (!this.inRangeAcquired(x, y, min))
		{
			return false;
		}
		
		this.moveLock.readLock().lock();
		try
		{
			Pos2D newMin = this.minPosRef.get();
			// Use EXACT compare here
			if (min != newMin)
			{
				if (!this.inRangeAcquired(x, y, newMin))
				{
					return false;
				}
			}
			this.setUnsafe(x, y, item);
			return true;
		}
		finally
		{
			this.moveLock.readLock().unlock();
		}
	}
	
	//endregion
	
	
	
	//================//
	// list modifiers //
	//================//
	//region
	
	/** see {@link MovableGridRingList#swap(int, int, T)} for full documentation */
	public T swap(Pos2D pos, T item) { return this.swap(pos.getX(), pos.getY(), item); }
	/** returns the input item if x,y is outside the grid */
	public T swap(int x, int y, T item)
	{
		Pos2D min = this.minPosRef.get();
		if (!this.inRangeAcquired(x, y, min))
		{
			return item;
		}
		
		this.moveLock.readLock().lock();
		try
		{
			Pos2D newMin = this.minPosRef.get();
			// Use EXACT compare here
			if (min != newMin)
			{
				if (!this.inRangeAcquired(x, y, newMin))
				{
					return item;
				}
			}
			return this.swapUnsafe(x, y, item);
		}
		finally
		{
			this.moveLock.readLock().unlock();
		}
	}
	
	
	/** see {@link MovableGridRingList#remove(int, int)} for full documentation */
	public T remove(Pos2D pos) { return this.remove(pos.getX(), pos.getY()); }
	/** remove and return the item at x,y; returns null if the x,y are outside the grid */
	public T remove(int x, int y) { return this.swap(x, y, null); }
	
	
	
	/** see {@link MovableGridRingList#clear(Consumer)} for full documentation */
	@Override
	public void clear() { this.clear(null); }
	/** @param removedItemConsumer the consumer run on each item before it is removed from the list */
	public void clear(Consumer<? super T> removedItemConsumer)
	{
		this.moveLock.writeLock().lock();
		try
		{
			if (removedItemConsumer != null)
			{
				super.forEach((item) ->
				{
					if (item != null)
					{
						removedItemConsumer.accept(item);
					}
				});
			}
			
			super.clear();
			super.ensureCapacity(this.width * this.width);
			// fill the array with nulls so we can get/set indicies
			for (int i = 0; i < this.width * this.width; i++)
			{
				super.add(null);
			}
		}
		finally
		{
			this.moveLock.writeLock().unlock();
		}
	}
	
	
	
	/** see {@link MovableGridRingList#moveTo(int, int, Consumer)} for full documentation */
	public boolean moveTo(int newCenterX, int newCenterY) { return this.moveTo(newCenterX, newCenterY, null); }
	
	public boolean moveTo(int newCenterX, int newCenterY, Consumer<? super T> removedItemConsumer) { return this.moveTo(newCenterX, newCenterY, removedItemConsumer, null); }
	/** Returns true if the grid was successfully moved, false otherwise */
	public boolean moveTo(int newCenterX, int newCenterY, Consumer<? super T> removedItemConsumer, BiConsumer<Pos2D, ? super T> nullableRemovedItemConsumer)
	{
		Pos2D cPos = this.minPosRef.get();
		int newMinX = newCenterX - this.halfWidth;
		int newMinY = newCenterY - this.halfWidth;
		if (cPos.getX() == newMinX && cPos.getY() == newMinY)
		{
			return false;
		}
		
		this.moveLock.writeLock().lock();
		try
		{
			cPos = this.minPosRef.get();
			int deltaX = newMinX - cPos.getX();
			int deltaY = newMinY - cPos.getY();
			if (deltaX == 0 && deltaY == 0)
			{
				return false;
			}
			
			// if the x or z offset is equal to or greater than
			// the total width, just delete the current data
			// and update the pos
			if (Math.abs(deltaX) >= this.width || Math.abs(deltaY) >= this.width)
			{
				this.clear(removedItemConsumer);
			}
			else
			{
				for (int x = 0; x < this.width; x++)
				{
					for (int y = 0; y < this.width; y++)
					{
						Pos2D itemPos = new Pos2D(x + cPos.getX(), y + cPos.getY());
						
						if (x - deltaX < 0
								|| y - deltaY < 0
								|| x - deltaX >= this.width
								|| y - deltaY >= this.width)
						{
							T item = this.swapUnsafe(itemPos.getX(), itemPos.getY(), null);
							if (item != null && removedItemConsumer != null)
							{
								removedItemConsumer.accept(item);
							}
							
							if (nullableRemovedItemConsumer != null)
							{
								nullableRemovedItemConsumer.accept(itemPos, item);
							}
						}
						else if (nullableRemovedItemConsumer != null)
						{
							nullableRemovedItemConsumer.accept(itemPos, null);
						}
					}
				}
			}
			
			this.minPosRef.set(new Pos2D(newMinX, newMinY));
			return true;
		}
		finally
		{
			this.moveLock.writeLock().unlock();
		}
	}
	
	//endregion
	
	
	
	//==================//
	// position getters //
	//==================//
	//region
	
	public Pos2D getCenter() { return new Pos2D(this.minPosRef.get().getX() + this.halfWidth, this.minPosRef.get().getY() + this.halfWidth); }
	
	public Pos2D getMinPosInRange() { return this.minPosRef.get(); }
	public Pos2D getMaxPosInRange() { return new Pos2D(this.minPosRef.get().getX() + this.width - 1, this.minPosRef.get().getY() + this.width - 1); }
	
	public int getWidth() { return this.width; }
	public int getHalfWidth() { return this.halfWidth; }
	
	//endregion
	
	
	
	//================//
	// helper methods //
	//================//
	//region
	
	/**
	 * Warning: Be careful with race conditions!
	 * The grid may move after this query!
	 */
	public boolean inRange(int x, int y)
	{
		Pos2D minPos = this.minPosRef.get();
		return (x >= minPos.getX()
				&& x < minPos.getX() + this.width
				&& y >= minPos.getY()
				&& y < minPos.getY() + this.width);
	}
	
	private boolean inRangeAcquired(int x, int y, Pos2D min)
	{
		return (x >= min.getX()
				&& x < min.getX() + this.width
				&& y >= min.getY()
				&& y < min.getY() + this.width);
	}
	
	private T getUnsafe(int x, int y) { return super.get(Math.floorMod(x, this.width) + Math.floorMod(y, this.width) * this.width); }
	private void setUnsafe(int x, int y, T item) { super.set(Math.floorMod(x, this.width) + Math.floorMod(y, this.width) * this.width, item); }
	private T swapUnsafe(int x, int y, T item) { return super.set(Math.floorMod(x, this.width) + Math.floorMod(y, this.width) * this.width, item); }
	
	//endregion
	
	
	
	//===========//
	// iterators //
	//===========//
	//region
	
	/** Will pass in null entries */
	public void forEachPos(BiConsumer<? super T, Pos2D> consumer)
	{
		this.moveLock.readLock().lock();
		try
		{
			Pos2D min = this.minPosRef.get();
			for (int x = min.getX(); x < min.getX() + this.width; x++)
			{
				for (int y = min.getY(); y < min.getY() + this.width; y++)
				{
					T t = this.getUnsafe(x, y);
					consumer.accept(t, new Pos2D(x, y));
				}
			}
		}
		finally
		{
			this.moveLock.readLock().unlock();
		}
	}
	
	/** Will skip null entries */
	public void forEachOrdered(Consumer<? super T> consumer)
	{
		this.moveLock.readLock().lock();
		try
		{
			Pos2D min = this.minPosRef.get();
			for (Pos2D offset : this.ringPositionIteratorArray)
			{
				T item = this.getUnsafe(min.getX() + offset.getX(), min.getY() + offset.getY());
				if (item != null)
				{
					consumer.accept(item);
				}
			}
		}
		finally
		{
			this.moveLock.readLock().unlock();
		}
	}
	
	/** Will pass in null entries */
	public void forEachPosOrdered(BiConsumer<? super T, Pos2D> consumer)
	{
		this.moveLock.readLock().lock();
		try
		{
			Pos2D min = this.minPosRef.get();
			for (Pos2D offset : this.ringPositionIteratorArray)
			{
				LodUtil.assertTrue(this.inRangeAcquired(min.getX() + offset.getX(), min.getY() + offset.getY(), min));
				T item = this.getUnsafe(min.getX() + offset.getX(), min.getY() + offset.getY());
				consumer.accept(item, new Pos2D(min.getX() + offset.getX(), min.getY() + offset.getY()));
			}
		}
		finally
		{
			this.moveLock.readLock().unlock();
		}
	}
	
	//endregion
	
	
	
	//==============//
	// base methods //
	//==============//
	//region
	
	@Override
	public String toString()
	{
		Pos2D p = this.minPosRef.get();
		return this.getClass().getSimpleName() + "[" + (p.getX() + this.halfWidth) + "," + (p.getY() + this.halfWidth) + "] " + this.width + "*" + this.width + "[" + this.size() + "]";
	}
	
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
			if (i % this.width == 0)
			{
				str.append("\n");
			}
		}
		return str.toString();
	}
	
	//endregion
	
	
	
	//================//
	// helper classes //
	//================//
	//region
	
	public static class Pos2D
	{
		public static final Pos2D ZERO = new Pos2D(0, 0);
		
		private final int x;
		public int getX() { return this.x; }
		
		private final int y;
		public int getY() { return this.y; }
		
		
		
		//==============//
		// constructors //
		//==============//
		//region
		
		public Pos2D(int x, int y)
		{
			this.x = x;
			this.y = y;
		}
		
		//endregion
		
		
		
		//======//
		// math //
		//======//
		//region
		
		public Pos2D add(Pos2D other) { return new Pos2D(this.x + other.x, this.y + other.y); }
		public Pos2D subtract(Pos2D other) { return new Pos2D(this.x - other.x, this.y - other.y); }
		public Pos2D subtract(int value) { return new Pos2D(this.x - value, this.y - value); }
		
		public double dist(Pos2D other) { return Math.sqrt(Math.pow(this.x - other.x, 2) + Math.pow(this.y - other.y, 2)); }
		public long distSquared(Pos2D other) { return MathUtil.pow2((long) this.x - other.x) + MathUtil.pow2((long) this.y - other.y); }
		
		/**
		 * Returns the maximum distance along either the X or Z axis <br><br>
		 *
		 * Example chebyshev distance between X and every point around it: <br>
		 * <code>
		 * 2 2 2 2 2 <br>
		 * 2 1 1 1 2 <br>
		 * 2 1 X 1 2 <br>
		 * 2 1 1 1 2 <br>
		 * 2 2 2 2 2 <br>
		 * </code>
		 */
		public int chebyshevDist(Pos2D other) { return Math.max(Math.abs(this.x - other.x), Math.abs(this.y - other.y)); }
		
		/**
		 * Can be used to quickly determine the rough distance between two points<Br>
		 * or determine the taxi cab (manhattan) distance between two points. <Br><Br>
		 *
		 * Manhattan distance is equivalent to determining the distance between two street intersections,
		 * where you can only drive along each street, instead of directly to the other point.
		 */
		public int manhattanDist(Pos2D other) { return Math.abs(this.x - other.x) + Math.abs(this.y - other.y); }
		
		//endregion
		
		
		
		//================//
		// base overrides //
		//================//
		//region
		
		@Override
		public int hashCode() { return Objects.hash(this.x, this.y); }
		
		@Override
		public String toString() { return "[" + this.x + ", " + this.y + "]"; }
		
		@Override
		public boolean equals(Object otherObj)
		{
			if (otherObj == this)
				return true;
			if (otherObj instanceof Pos2D)
			{
				Pos2D otherPos = (Pos2D) otherObj;
				return this.x == otherPos.x && this.y == otherPos.y;
			}
			return false;
		}
		
		//endregion
	}
	
	//endregion
	
	
	
}
