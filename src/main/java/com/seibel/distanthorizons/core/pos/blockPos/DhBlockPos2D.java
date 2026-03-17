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

package com.seibel.distanthorizons.core.pos.blockPos;

import com.seibel.distanthorizons.core.pos.Pos2D;
import com.seibel.distanthorizons.coreapi.util.MathUtil;

/** immutable */
public class DhBlockPos2D
{
	public static final DhBlockPos2D ZERO = new DhBlockPos2D(0, 0);
	public final int x;
	public final int z;
	
	
	
	//==============//
	// constructors //
	//==============//
	
	public DhBlockPos2D(int x, int z)
	{
		this.x = x;
		this.z = z;
	}
	
	public DhBlockPos2D(DhBlockPos blockPos)
	{
		this.x = blockPos.getX();
		this.z = blockPos.getZ();
	}
	
	public static DhBlockPos2D fromPos2D(Pos2D pos) { return new DhBlockPos2D(pos.getX(), pos.getY()); }
	
	
	
	//==========//
	// mutators //
	//==========//
	
	public DhBlockPos2D add(DhBlockPos2D other) { return new DhBlockPos2D(this.x + other.x, this.z + other.z); }
	
	public DhBlockPos2D add(int offsetX, int offsetZ) { return new DhBlockPos2D(this.x + offsetX, this.z + offsetZ); }
	
	public DhBlockPos2D subtract(DhBlockPos2D other) { return new DhBlockPos2D(this.x - other.x, this.z - other.z); }
	
	public Pos2D toPos2D() { return new Pos2D(this.x, this.z); }
	
	
	
	//==============//
	// calculations //
	//==============//
	
	public double dist(DhBlockPos2D other) { return this.dist(other.x, other.z); }
	public double dist(int x, int z) { return Math.sqrt(Math.pow(this.x - x, 2) + Math.pow(this.z - z, 2)); }
	
	public long distSquared(DhBlockPos2D other) { return this.distSquared(other.x, other.z); }
	public long distSquared(int x, int z) { return MathUtil.pow2((long) this.x - x) + MathUtil.pow2((long) this.z - z); }
	
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
	public int chebyshevDist(DhBlockPos2D other) { return Math.max(Math.abs(this.x - other.x), Math.abs(this.z - other.z)); }
	
	/**
	 * Can be used to quickly determine the rough distance between two points<Br>
	 * or determine the taxi cab (manhattan) distance between two points. <Br><Br>
	 *
	 * Manhattan distance is equivalent to determining the distance between two street intersections,
	 * where you can only drive along each street, instead of directly to the other point.
	 */
	public int manhattanDist(DhBlockPos2D other) { return Math.abs(this.x - other.x) + Math.abs(this.z - other.z); }
	
	
	
	
	//===========//
	// overrides //
	//===========//
	
	@Override
	public String toString() { return "(" + this.x + ", " + this.z + ")"; }
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof DhBlockPos2D)
		{
			DhBlockPos2D other = (DhBlockPos2D) obj;
			return this.x == other.x && this.z == other.z;
		}
		
		return false;
	}
	
	@Override
	public int hashCode() { return Integer.hashCode(this.x) ^ Integer.hashCode(this.z); }
	
}
