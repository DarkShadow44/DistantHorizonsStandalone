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

package com.seibel.distanthorizons.core.util.objects.quadTree;

import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos2D;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.coreapi.util.BitShiftUtil;
import com.seibel.distanthorizons.coreapi.util.MathUtil;
import com.seibel.distanthorizons.core.util.gridList.MovableGridRingList;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongIterator;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

/**
 * This class represents a quadTree of T type values.
 */
public class QuadTree<T>
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	
	
	/** 
	 * The largest numerical detail level this tree supports. <br>
	 * IE: the detail level used by the root nodes.
	 */
	public final byte treeRootDetailLevel;
	/** 
	 * The smallest numerical detail level this tree supports. <br> 
	 * IE: the detail level used by the leaf nodes.
	 */
	public final byte treeLeafDetailLevel;
	
	private final int diameterInBlocks;
	
	/** contain the actual data in the quad tree structure */
	private final MovableGridRingList<QuadNode<T>> topRingList;
	
	private DhBlockPos2D centerBlockPos;
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	/**
	 * Constructor of the quadTree
	 *
	 * @param diameterInBlocks equivalent to the distance between the two opposing sides
	 */
	public QuadTree(int diameterInBlocks, DhBlockPos2D centerBlockPos, byte treeLeafDetailLevel)
	{
		this.centerBlockPos = centerBlockPos;
		this.diameterInBlocks = diameterInBlocks;
		
		this.treeLeafDetailLevel = treeLeafDetailLevel;
		// the min detail level must be greater than 0 (to prevent divide by 0 errors) and greater than the maximum detail level
		this.treeRootDetailLevel = (byte) Math.max(Math.max(1, this.treeLeafDetailLevel), MathUtil.log2(diameterInBlocks));
		
		int halfSizeInRootNodes = Math.floorDiv(this.diameterInBlocks, 2) / BitShiftUtil.powerOfTwo(this.treeRootDetailLevel);
		halfSizeInRootNodes = halfSizeInRootNodes + 1; // always add 1 so nodes will always have a parent, even if the tree's center is offset from the root node grid 
		
		MovableGridRingList.Pos2D ringListCenterPos = new MovableGridRingList.Pos2D(
				BitShiftUtil.divideByPowerOfTwo(this.centerBlockPos.x, this.treeRootDetailLevel),
				BitShiftUtil.divideByPowerOfTwo(this.centerBlockPos.z, this.treeRootDetailLevel));
		this.topRingList = new MovableGridRingList<>(halfSizeInRootNodes, ringListCenterPos.getX(), ringListCenterPos.getY());
		
	}
	
	//endregion
	
	
	
	//==================//
	// property getters //
	//==================//
	//region
	
	/** @return the number of non-null nodes in the tree */
	public int nodeCount()
	{
		int count = 0;
		for (QuadNode<T> node : this.topRingList)
		{
			if (node == null)
			{
				continue;
			}
			
			Iterator<QuadNode<T>> nodeIterator = node.getNodeIterator();
			while (nodeIterator.hasNext())
			{
				if (nodeIterator.next().value != null)
				{
					count++;
				}
			}
		}
		
		return count;
	}
	
	/** @return the number of leaf nodes in the tree */
	public int leafNodeCount()
	{
		int count = 0;
		for (QuadNode<T> node : this.topRingList)
		{
			if (node == null)
			{
				continue;
			}
			
			Iterator<QuadNode<T>> leafNodeIterator = node.getLeafNodeIterator();
			while (leafNodeIterator.hasNext())
			{
				leafNodeIterator.next();
				count++;
			}
		}
		
		return count;
	}
	
	
	/**
	 * A tree will always have 9 root nodes, 
	 * this is because the tree will grow all the way up to the top where it is a 3x3 grid.
	 * If this is ever changed then these values must also change
	 *
	 */
	public int ringListWidth() { return 3; }
	public int ringListHalfWidth() { return 1; }
	public int diameterInBlocks() { return this.diameterInBlocks; }
	
	//endregion
	
	
	
	//==========================//
	// node getters and setters //
	//==========================//
	//region
	
	/** @return the value at the given section position. Null will be returned if the value is missing or the position is out of bounds. */
	@Nullable
	public final T tryGetValue(long pos)
	{
		QuadNode<T> node = this.tryGetNode(pos);
		if (node != null)
		{
			return node.value;
		}
		return null;
	}
	
	/** @return the node at the given section position, null if out of bounds */
	@Nullable
	public final QuadNode<T> tryGetNode(long pos) { return this.getOrSetNode(pos, false, null, false); }
	
	
	/** @return the node at the given section position */
	@Nullable
	public final QuadNode<T> getNode(long pos) throws IndexOutOfBoundsException { return this.getOrSetNode(pos, false, null, true); }
	
	/** @return the value at the given section position */
	@Nullable
	public final T getValue(long pos) throws IndexOutOfBoundsException
	{
		QuadNode<T> node = this.getNode(pos);
		if (node != null)
		{
			return node.value;
		}
		return null;
	}
	
	/** @return the value that was previously in the given position, null if nothing */
	@Nullable
	public final T setValue(long pos, T value) throws IndexOutOfBoundsException
	{
		T previousValue = this.getValue(pos);
		this.getOrSetNode(pos, true, value, true);
		return previousValue;
	}
	
	/** @param throwIfOutOfBounds if false returns null */
	@Nullable
	protected final QuadNode<T> getOrSetNode(long pos, boolean setNewValue, T newValue, boolean throwIfOutOfBounds) throws IndexOutOfBoundsException
	{
		if (!this.isSectionPosInBounds(pos))
		{
			// how should out-of-bounds positions be handled?
			if (throwIfOutOfBounds)
			{
				int radius = this.diameterInBlocks() / 2;
				DhBlockPos2D minBlockPos = this.getCenterBlockPos().add(new DhBlockPos2D(-radius, -radius));
				DhBlockPos2D maxBlockPos = this.getCenterBlockPos().add(new DhBlockPos2D(radius, radius));
				throw new IndexOutOfBoundsException("QuadTree GetOrSet failed. Position out of bounds, min block pos: [" + minBlockPos + "], max block pos: [" + maxBlockPos + "], leaf detail level: [" + this.treeLeafDetailLevel + "], root detail level: [" + this.treeRootDetailLevel + "]. Requested section pos: [" + DhSectionPos.toString(pos) + "].");
			}
			else
			{
				return null;
			}
		}
		
		
		
		long rootPos = DhSectionPos.convertToDetailLevel(pos, this.treeRootDetailLevel);
		int ringListPosX = DhSectionPos.getX(rootPos);
		int ringListPosZ = DhSectionPos.getZ(rootPos);
		
		QuadNode<T> topQuadNode = this.topRingList.get(ringListPosX, ringListPosZ);
		if (topQuadNode == null)
		{
			if (!setNewValue)
			{
				return null;
			}
			
			topQuadNode = new QuadNode<T>(rootPos, this.treeLeafDetailLevel);
			boolean successfullyAdded = this.topRingList.set(ringListPosX, ringListPosZ, topQuadNode);
			if (!successfullyAdded)
			{
				LodUtil.assertNotReach("Failed to add top quadTree node at position: " + rootPos);
			}
		}
		
		if (!DhSectionPos.contains(topQuadNode.sectionPos, pos))
		{
			LodUtil.assertNotReach("failed to get a root node that contains the input position: " + pos + " root node pos: " + topQuadNode.sectionPos);
		}
		
		
		QuadNode<T> returnNode = topQuadNode.getNode(pos);
		if (setNewValue)
		{
			topQuadNode.setValue(pos, newValue);
		}
		return returnNode;
	}
	
	public boolean isSectionPosInBounds(long testPos)
	{
		// check if the testPos is within the detail level limits of the tree
		boolean detailLevelWithinBounds = this.treeLeafDetailLevel <= DhSectionPos.getDetailLevel(testPos) && DhSectionPos.getDetailLevel(testPos) <= this.treeRootDetailLevel;
		if (!detailLevelWithinBounds)
		{
			return false;
		}
		
		
		// check if the testPos is within the X,Z boundary of the tree
		DhBlockPos2D treeBlockCorner = this.centerBlockPos.add(new DhBlockPos2D(-this.diameterInBlocks / 2, -this.diameterInBlocks / 2));
		long treeCornerPos = DhSectionPos.encode((byte) 0, treeBlockCorner.x, treeBlockCorner.z);
		
		long inputSectionCorner = DhSectionPos.convertToDetailLevel(testPos, (byte) 0);
		long inputCornerPos = DhSectionPos.encode((byte) 0, DhSectionPos.getX(inputSectionCorner), DhSectionPos.getZ(inputSectionCorner));
		int inputBlockWidth = BitShiftUtil.powerOfTwo(DhSectionPos.getDetailLevel(testPos));
		
		return DoSquaresOverlap(treeCornerPos, this.diameterInBlocks, inputCornerPos, inputBlockWidth);
	}
	private static boolean DoSquaresOverlap(long square1Min, int square1Width, long square2Min, int square2Width)
	{
		// Determine the coordinates of the squares (the variables say rect[angle] because this logic would also work there and was simplified to work for squares)
		float rect1MinX = DhSectionPos.getX(square1Min);
		float rect1MaxX = DhSectionPos.getX(square1Min) + square1Width;
		float rect1MinZ = DhSectionPos.getZ(square1Min);
		float rect1MaxZ = DhSectionPos.getZ(square1Min) + square1Width;
		
		float rect2MinX = DhSectionPos.getX(square2Min);
		float rect2MaxX = DhSectionPos.getX(square2Min) + square2Width;
		float rect2MinZ = DhSectionPos.getZ(square2Min);
		float rect2MaxZ = DhSectionPos.getZ(square2Min) + square2Width;
		
		// Check if the squares overlap
		return
				rect1MinX < rect2MaxX &&
						rect1MaxX > rect2MinX &&
						rect1MinZ < rect2MaxZ &&
						rect1MaxZ > rect2MinZ;
	}
	
	
	public int getNonNullChildCountAtPos(long pos) { return this.getChildCountAtPos(pos, false); }
	public int getChildCountAtPos(long pos, boolean includeNullValues)
	{
		int childCount = 0;
		for (int i = 0; i < 4; i++)
		{
			long childPos = DhSectionPos.getChildByIndex(pos, i);
			if (this.isSectionPosInBounds(childPos))
			{
				T value = this.getValue(childPos);
				if (includeNullValues || value != null)
				{
					childCount++;
				}
			}
		}
		
		return childCount;
	}
	
	//endregion
	
	
	
	//===========//
	// iterators //
	//===========//
	//region
	
	/** can include null nodes */
	public LongIterator rootNodePosIterator() { return new QuadTreeRootPosIterator(true, null); }
	
	/** @see INodeIteratorStoppingFunc */
	public Iterator<QuadNode<T>> nodeIteratorWithStoppingFilter(INodeIteratorStoppingFunc<T> stoppingFilterFunc) { return new QuadTreeNodeIterator(false, stoppingFilterFunc); }
	public Iterator<QuadNode<T>> nodeIterator() { return new QuadTreeNodeIterator(false, null); }
	public Iterator<QuadNode<T>> leafNodeIterator() { return new QuadTreeNodeIterator(true, null); }
	
	//endregion
	
	
	
	//================//
	// get/set center //
	//================//
	//region
	
	public void setCenterBlockPos(DhBlockPos2D newCenterPos) { this.setCenterBlockPos(newCenterPos, null); }
	public void setCenterBlockPos(DhBlockPos2D newCenterPos, Consumer<? super T> removedItemConsumer)
	{
		this.centerBlockPos = newCenterPos;
		
		MovableGridRingList.Pos2D expectedCenterPos = new MovableGridRingList.Pos2D(
				BitShiftUtil.divideByPowerOfTwo(this.centerBlockPos.x, this.treeRootDetailLevel),
				BitShiftUtil.divideByPowerOfTwo(this.centerBlockPos.z, this.treeRootDetailLevel));
		
		if (this.topRingList.getCenter().equals(expectedCenterPos))
		{
			// tree doesn't need to be moved
			return;
		}
		
		
		// remove out of bounds root nodes
		this.topRingList.moveTo(expectedCenterPos.getX(), expectedCenterPos.getY(), (quadNode) ->
		{
			if (quadNode != null && removedItemConsumer != null)
			{
				quadNode.deleteAllChildren(removedItemConsumer);
				
				removedItemConsumer.accept(quadNode.value);
			}
		});
	}
	
	public final DhBlockPos2D getCenterBlockPos() { return this.centerBlockPos; }
	
	//endregion
	
	
	
	//==============//
	// base methods //
	//==============//
	//region
	
	public boolean isEmpty() { return this.nodeCount() == 0; } // this should be rewritten to short-circuit
	
	@Override
	public String toString() 
	{ 
		return "center block: " + this.centerBlockPos + 
				", block width: " + this.diameterInBlocks + 
				", detail level range: [" + this.treeLeafDetailLevel + "-" + this.treeRootDetailLevel + "], " +
				"leaf #: " + this.leafNodeCount(); 
	}
	
	//endregion
	
	
	
	//==================//
	// iterator classes //
	//==================//
	//region
	
	/** @see INodeIteratorStoppingFunc#iteratorShouldStop(QuadNode)  */
	@FunctionalInterface
	public interface INodeIteratorStoppingFunc<T>
	{
		/** if this function returns true then the iterator will stop walking down the tree */
		boolean iteratorShouldStop(QuadNode<T> node);
	}
	
	private class QuadTreeRootPosIterator implements LongIterator
	{
		private final LongArrayFIFOQueue iteratorPosQueue;
		@Nullable
		private final INodeIteratorStoppingFunc<T> stopIteratingFunc;
		
		
		
		public QuadTreeRootPosIterator(boolean includeNullNodes, @Nullable INodeIteratorStoppingFunc<T> stopIteratingFunc)
		{
			this.iteratorPosQueue = new LongArrayFIFOQueue();
			this.stopIteratingFunc = stopIteratingFunc;
			
			QuadTree.this.topRingList.forEachPosOrdered((node, pos2D) ->
			{
				if (this.stopIteratingFunc != null
					&& this.stopIteratingFunc.iteratorShouldStop(node))
				{
					return;
				}
				
				if (node != null 
					|| includeNullNodes)
				{
					long rootPos = DhSectionPos.encode(QuadTree.this.treeRootDetailLevel, pos2D.getX(), pos2D.getY());
					if (QuadTree.this.isSectionPosInBounds(rootPos))
					{
						this.iteratorPosQueue.enqueue(rootPos);
					}
				}
			});
		}// constructor
		
		
		
		@Override
		public boolean hasNext() { return this.iteratorPosQueue.size() != 0; }
		
		@Override
		public long nextLong()
		{
			if (this.iteratorPosQueue.size() == 0)
			{
				throw new NoSuchElementException();
			}
			
			
			long sectionPos = this.iteratorPosQueue.dequeueLong();
			return sectionPos;
		}
		
		
		/** Unimplemented */
		@Override
		public void remove() { throw new UnsupportedOperationException("remove"); }
		
		@Override
		public void forEachRemaining(LongConsumer action) { LongIterator.super.forEachRemaining(action); }
		
	}
	
	private class QuadTreeNodeIterator implements Iterator<QuadNode<T>>
	{
		private final QuadTreeRootPosIterator rootNodeIterator;
		private Iterator<QuadNode<T>> currentNodeIterator;
		
		private QuadNode<T> lastNode = null;
		
		private final boolean onlyReturnLeaves;
		@Nullable
		private final INodeIteratorStoppingFunc<T> stopIteratingFunc;
		
		
		
		public QuadTreeNodeIterator(boolean onlyReturnLeaves, @Nullable INodeIteratorStoppingFunc<T> stopIteratingFunc)
		{
			this.rootNodeIterator = new QuadTreeRootPosIterator(false, stopIteratingFunc);
			this.onlyReturnLeaves = onlyReturnLeaves;
			
			this.stopIteratingFunc = stopIteratingFunc;
		}
		
		
		
		@Override
		public boolean hasNext()
		{
			if (!this.rootNodeIterator.hasNext() 
				&& this.currentNodeIterator != null 
				&& !this.currentNodeIterator.hasNext())
			{
				return false;
			}
			
			
			if (this.currentNodeIterator == null 
				|| !this.currentNodeIterator.hasNext())
			{
				this.currentNodeIterator = this.getNextChildNodeIterator();
			}
			
			return this.currentNodeIterator != null && this.currentNodeIterator.hasNext();
		}
		
		@Override
		public QuadNode<T> next()
		{
			if (this.currentNodeIterator == null 
				|| !this.currentNodeIterator.hasNext())
			{
				this.currentNodeIterator = this.getNextChildNodeIterator();
			}
			
			
			this.lastNode = this.currentNodeIterator.next();
			return this.lastNode;
		}
		
		/** @return null if no new iterator could be found */
		private Iterator<QuadNode<T>> getNextChildNodeIterator()
		{
			Iterator<QuadNode<T>> nodeIterator = null;
			while ((nodeIterator == null || !nodeIterator.hasNext()) 
					&& this.rootNodeIterator.hasNext())
			{
				long sectionPos = this.rootNodeIterator.nextLong();
				
				// try-get to prevent concurrency errors if the tree is being moved while we walk through it
				QuadNode<T> rootNode = QuadTree.this.tryGetNode(sectionPos);
				if (rootNode != null)
				{
					nodeIterator = this.onlyReturnLeaves ? rootNode.getLeafNodeIterator() : rootNode.getNodeIterator(this.stopIteratingFunc);
				}
			}
			return nodeIterator;
		}
		
		
		@Override
		public void remove()
		{
			if (this.lastNode == null)
			{
				throw new NoSuchElementException("No last node found.");
			}
			
			
			QuadNode<T> node = QuadTree.this.getOrSetNode(this.lastNode.sectionPos, true, null, false);
			if (node != null)
			{
				node.deleteAllChildren();
			}
		}
		
		@Override
		public void forEachRemaining(Consumer<? super QuadNode<T>> action) { Iterator.super.forEachRemaining(action); }
		
	}
	
	//endregion
	
	
}
