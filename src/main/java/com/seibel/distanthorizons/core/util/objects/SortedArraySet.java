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

package com.seibel.distanthorizons.core.util.objects;

import java.util.*;

public class SortedArraySet<E>
{
	private final ArrayList<E> list = new ArrayList<>();
	private final HashSet<E> set = new HashSet<>();
	
	private final Comparator<? super E> comparator;
	
	
	
	//==============//
	// constructors //
	//==============//
	
	public SortedArraySet(Comparator<? super E> comparator)
	{
		this.comparator = comparator;
	}
	
	public SortedArraySet(Collection<? extends E> collection, Comparator<? super E> comparator)
	{
		this.comparator = comparator;
		this.list.addAll(collection);
		this.list.sort(comparator);
	}
	
	
	
	//==============//
	// list methods //
	//==============//
	
	public void add(E element)
	{
		if (this.set.add(element))
		{
			this.list.add(element);
		}
	}
	
	public E get(int index) { return this.list.get(index); }
	
	public int size() { return this.list.size(); }
	
	public void clear()
	{
		this.list.clear();
		this.set.clear();
	}
	
	
	
	//================//
	// base overrides //
	//================//
	
	@Override
	public String toString()
	{
		return "SortedArraySet{" +
				"list=" + this.list +
				", comparator=" + this.comparator +
				'}';
	}
	
	
	
}
