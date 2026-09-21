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

package tests;

import com.seibel.distanthorizons.core.enums.EDhDirection;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos2D;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class DhSectionPosTest
{
	
	@Test
	public void basicEncodeDecodeTest()
	{
		long pos;
		
		// zero pos
		pos = DhSectionPos.encode((byte) 0, 0, 0);
		assertSectionPosEqual(0, DhSectionPos.getDetailLevel(pos));
		assertSectionPosEqual(0, DhSectionPos.getX(pos));
		assertSectionPosEqual(0, DhSectionPos.getZ(pos));
		
		// positive values
		pos = DhSectionPos.encode((byte) 10, 4, 1);
		assertSectionPosEqual(10, DhSectionPos.getDetailLevel(pos));
		assertSectionPosEqual(4, DhSectionPos.getX(pos));
		assertSectionPosEqual(1, DhSectionPos.getZ(pos));
		
		// negative position, positive detail level
		pos = DhSectionPos.encode((byte) 2, -1, -4);
		assertSectionPosEqual(2, DhSectionPos.getDetailLevel(pos));
		assertSectionPosEqual(-1, DhSectionPos.getX(pos));
		assertSectionPosEqual(-4, DhSectionPos.getZ(pos));
		
	}
	
	
	
	@Test
	public void containsPosTest()
	{
		long root = DhSectionPos.encode((byte) 10, 0, 0);
		long child = DhSectionPos.encode((byte) 9, 1, 1);
		
		Assert.assertTrue("section pos contains fail", DhSectionPos.contains(root, child));
		Assert.assertFalse("section pos contains fail", DhSectionPos.contains(child, root));
		
		
		root = DhSectionPos.encode((byte) 10, 1, 0);
		
		// out of bounds
		child = DhSectionPos.encode((byte) 9, 0, 0);
		Assert.assertFalse("position should be out of bounds", DhSectionPos.contains(root, child));
		child = DhSectionPos.encode((byte) 9, 1, 1);
		Assert.assertFalse("position should be out of bounds", DhSectionPos.contains(root, child));
		
		// in bounds
		child = DhSectionPos.encode((byte) 9, 2, 0);
		Assert.assertTrue("position should be in bounds", DhSectionPos.contains(root, child));
		child = DhSectionPos.encode((byte) 9, 3, 1);
		Assert.assertTrue("position should be in bounds", DhSectionPos.contains(root, child));
		
		// out of bounds
		child = DhSectionPos.encode((byte) 9, 2, 2);
		Assert.assertFalse("position should be out of bounds", DhSectionPos.contains(root, child));
		child = DhSectionPos.encode((byte) 9, 3, 3);
		Assert.assertFalse("position should be out of bounds", DhSectionPos.contains(root, child));
		
		child = DhSectionPos.encode((byte) 9, 4, 4);
		Assert.assertFalse("position should be out of bounds", DhSectionPos.contains(root, child));
		child = DhSectionPos.encode((byte) 9, 5, 5);
		Assert.assertFalse("position should be out of bounds", DhSectionPos.contains(root, child));
		
		
		Assert.assertTrue(DhSectionPos.contains(DhSectionPos.encode((byte) 6, 0, 0), DhSectionPos.encode((byte) 0, 0, 0)));
		Assert.assertTrue(DhSectionPos.contains(DhSectionPos.encode((byte) 6, 0, 0), DhSectionPos.encode((byte) 1, 0, 0)));
		Assert.assertTrue(DhSectionPos.contains(DhSectionPos.encode((byte) 6, 0, 0), DhSectionPos.encode((byte) 2, 0, 0)));
		Assert.assertTrue(DhSectionPos.contains(DhSectionPos.encode((byte) 6, 0, 0), DhSectionPos.encode((byte) 3, 0, 0)));
		Assert.assertTrue(DhSectionPos.contains(DhSectionPos.encode((byte) 6, 0, 0), DhSectionPos.encode((byte) 4, 0, 0)));
		Assert.assertTrue(DhSectionPos.contains(DhSectionPos.encode((byte) 6, 0, 0), DhSectionPos.encode((byte) 5, 0, 0)));
		Assert.assertTrue(DhSectionPos.contains(DhSectionPos.encode((byte) 6, 0, 0), DhSectionPos.encode((byte) 6, 0, 0)));
	}
	
	@Test
	public void containsAdjacentPosTest()
	{
		// neither should contain the other, they are single blocks that are next to each other
		long left = DhSectionPos.encode((byte) 0, 4606, 0);
		long right = DhSectionPos.encode((byte) 0, 4607, 0);
		Assert.assertFalse(DhSectionPos.contains(left, right));
		Assert.assertFalse(DhSectionPos.contains(right, left));


		// 512 block wide sections that are adjacent, but not overlapping
		left = DhSectionPos.encode((byte) 9, 0, 0);
		right = DhSectionPos.encode((byte) 9, 1, 0);
		Assert.assertFalse(DhSectionPos.contains(left, right));
		Assert.assertFalse(DhSectionPos.contains(right, left));

	}
	
	@Test
	public void parentPosTest()
	{
		long leaf = DhSectionPos.encode((byte) 0, 0, 0);
		long convert = DhSectionPos.convertToDetailLevel(leaf, (byte) 1);
		long parent = DhSectionPos.getParentPos(leaf);
		assertSectionPosEqual("get parent at 0,0 fail", convert, parent);


		leaf = DhSectionPos.encode((byte) 0, 1, 1);
		convert = DhSectionPos.convertToDetailLevel(leaf, (byte) 1);
		parent = DhSectionPos.getParentPos(leaf);
		assertSectionPosEqual("get parent at 1,1 fail", convert, parent);


		leaf = DhSectionPos.encode((byte) 1, 2, 2);
		convert = DhSectionPos.convertToDetailLevel(leaf, (byte) 2);
		parent = DhSectionPos.getParentPos(leaf);
		assertSectionPosEqual("parent upscale fail", convert, parent);
		convert = DhSectionPos.convertToDetailLevel(leaf, (byte) 0);
		long childIndex = DhSectionPos.getChildByIndex(leaf, 0);
		assertSectionPosEqual("child detail fail", convert, childIndex);

	}
	
	@Test
	public void childPosTest()
	{
		long node = DhSectionPos.encode((byte) 1, 2302, 0);
		long nw = DhSectionPos.getChildByIndex(node, 0);
		long sw = DhSectionPos.getChildByIndex(node, 1);
		long ne = DhSectionPos.getChildByIndex(node, 2);
		long se = DhSectionPos.getChildByIndex(node, 3);
		
		// confirm no children have the same values
		Assert.assertNotEquals(nw, sw);
		Assert.assertNotEquals(sw, ne);
		Assert.assertNotEquals(ne, se);
		
		// confirm each child has the correct value
		assertSectionPosEqual(nw, DhSectionPos.encode((byte) 0, 4604, 0));
		assertSectionPosEqual(sw, DhSectionPos.encode((byte) 0, 4605, 0));
		assertSectionPosEqual(ne, DhSectionPos.encode((byte) 0, 4604, 1));
		assertSectionPosEqual(se, DhSectionPos.encode((byte) 0, 4605, 1));
		
	}
	
	@Test
	public void getCenterBlock2DTest()
	{
		long parentNode = DhSectionPos.encode((byte) 2, 1151, 0); // width 4 blocks
		long inputPos = DhSectionPos.encode((byte) 0, 4606, 0); // width 1 block
		Assert.assertTrue(DhSectionPos.contains(parentNode, inputPos));

		DhBlockPos2D parentCenter = DhSectionPos.getCenterBlockPos(parentNode);
		DhBlockPos2D inputCenter = DhSectionPos.getCenterBlockPos(inputPos);
		
		Assert.assertEquals(new DhBlockPos2D(4606, 2), parentCenter);
		Assert.assertEquals(new DhBlockPos2D(4606, 0), inputCenter);

	}
	
	@Test
	public void createFromBlockPos()
	{
		// origin pos //
		
		DhBlockPos originBlockPos = new DhBlockPos(0, 0, 0);
		long originSectionPos = DhSectionPos.encodeContaining(DhSectionPos.SECTION_BLOCK_DETAIL_LEVEL, originBlockPos);
		assertSectionPosEqual(DhSectionPos.encode(DhSectionPos.SECTION_BLOCK_DETAIL_LEVEL, 0, 0), originSectionPos);
		
		
		// offset pos //
		long offsetSectionPos;
		
		DhBlockPos offsetBlockPos = new DhBlockPos(1000, 0, 42000);
		offsetSectionPos = DhSectionPos.encodeContaining(DhSectionPos.SECTION_BLOCK_DETAIL_LEVEL, offsetBlockPos);
		assertSectionPosEqual(DhSectionPos.encode(DhSectionPos.SECTION_BLOCK_DETAIL_LEVEL, 15, 656), offsetSectionPos);
		
		offsetBlockPos = new DhBlockPos(-987654, 0, 46);
		offsetSectionPos = DhSectionPos.encodeContaining(DhSectionPos.SECTION_BLOCK_DETAIL_LEVEL, offsetBlockPos);
		assertSectionPosEqual(DhSectionPos.encode(DhSectionPos.SECTION_BLOCK_DETAIL_LEVEL, -15433, 0), offsetSectionPos);
		
	}
	
	@Test
	public void createFromChunkPos()
	{
		// origin pos //
		
		DhChunkPos originChunkPos = new DhChunkPos(0,0);
		long originSectionPos = DhSectionPos.encodeContaining(DhSectionPos.SECTION_CHUNK_DETAIL_LEVEL, originChunkPos);
		assertSectionPosEqual(DhSectionPos.encode(DhSectionPos.SECTION_CHUNK_DETAIL_LEVEL, 0, 0), originSectionPos);
		
		
		// offset pos //
		
		DhChunkPos offsetChunkPos = new DhChunkPos(1000, 42000);
		long offsetSectionPos = DhSectionPos.encodeContaining(DhSectionPos.SECTION_CHUNK_DETAIL_LEVEL, offsetChunkPos);
		assertSectionPosEqual(DhSectionPos.encode(DhSectionPos.SECTION_CHUNK_DETAIL_LEVEL, 15, 656), offsetSectionPos);
		
		offsetChunkPos = new DhChunkPos(-987654, 46);
		offsetSectionPos = DhSectionPos.encodeContaining(DhSectionPos.SECTION_CHUNK_DETAIL_LEVEL, offsetChunkPos);
		assertSectionPosEqual(DhSectionPos.encode(DhSectionPos.SECTION_CHUNK_DETAIL_LEVEL, -15433, 0), offsetSectionPos);
		
	}
	
	@Test
	public void convertToDetailLevel()
	{
		// origin pos //
		
		long originSectionPos = DhSectionPos.encode((byte) 0,0,0);
		
		originSectionPos = DhSectionPos.convertToDetailLevel(originSectionPos, (byte) 1);
		assertSectionPosEqual(DhSectionPos.encode((byte) 1, 0, 0), originSectionPos);
		
		originSectionPos = DhSectionPos.convertToDetailLevel(originSectionPos, DhSectionPos.SECTION_BLOCK_DETAIL_LEVEL);
		assertSectionPosEqual(DhSectionPos.encode(DhSectionPos.SECTION_BLOCK_DETAIL_LEVEL, 0, 0), originSectionPos);
		
		originSectionPos = DhSectionPos.convertToDetailLevel(originSectionPos, DhSectionPos.SECTION_REGION_DETAIL_LEVEL);
		assertSectionPosEqual(DhSectionPos.encode(DhSectionPos.SECTION_REGION_DETAIL_LEVEL, 0, 0), originSectionPos);
		
		
		// offset pos //
		
		long offsetSectionPos = DhSectionPos.encode((byte) 0,-10000,5000);
		
		offsetSectionPos = DhSectionPos.convertToDetailLevel(offsetSectionPos, (byte) 1);
		assertSectionPosEqual(DhSectionPos.encode((byte) 1, -5000, 2500), offsetSectionPos);
		
		offsetSectionPos = DhSectionPos.convertToDetailLevel(offsetSectionPos, DhSectionPos.SECTION_BLOCK_DETAIL_LEVEL);
		assertSectionPosEqual(DhSectionPos.encode(DhSectionPos.SECTION_BLOCK_DETAIL_LEVEL, -157, 78), offsetSectionPos);
		
		offsetSectionPos = DhSectionPos.convertToDetailLevel(offsetSectionPos, DhSectionPos.SECTION_REGION_DETAIL_LEVEL);
		assertSectionPosEqual(DhSectionPos.encode(DhSectionPos.SECTION_REGION_DETAIL_LEVEL, -1, 0), offsetSectionPos);
		
	}
	
	@Test
	public void getOffsetWidth()
	{
		long originSectionPos = DhSectionPos.encode((byte) 0,0,0);
		long sectionPos = DhSectionPos.encode((byte) 0,-10000,5000);
		
		
		
		// 1 -> 0
		byte returnDetailLevel = 0;
		originSectionPos = DhSectionPos.convertToDetailLevel(originSectionPos, (byte) 1);
		assertSectionPosEqual(2, DhSectionPos.getWidthCountForLowerDetailedSection(originSectionPos, returnDetailLevel));
		
		sectionPos = DhSectionPos.convertToDetailLevel(sectionPos, (byte) 1);
		assertSectionPosEqual(2, DhSectionPos.getWidthCountForLowerDetailedSection(sectionPos, returnDetailLevel));
		
		
		// 2 -> 1
		returnDetailLevel = 1;
		originSectionPos = DhSectionPos.convertToDetailLevel(originSectionPos, (byte) 2);
		assertSectionPosEqual(2, DhSectionPos.getWidthCountForLowerDetailedSection(originSectionPos, returnDetailLevel));
		
		sectionPos = DhSectionPos.convertToDetailLevel(sectionPos, (byte) 2);
		assertSectionPosEqual(2, DhSectionPos.getWidthCountForLowerDetailedSection(sectionPos, returnDetailLevel));
		
		
		// Block -> 0
		returnDetailLevel = 0;
		originSectionPos = DhSectionPos.convertToDetailLevel(originSectionPos, DhSectionPos.SECTION_BLOCK_DETAIL_LEVEL);
		assertSectionPosEqual(64, DhSectionPos.getWidthCountForLowerDetailedSection(originSectionPos, returnDetailLevel));
		
		sectionPos = DhSectionPos.convertToDetailLevel(sectionPos, DhSectionPos.SECTION_BLOCK_DETAIL_LEVEL);
		assertSectionPosEqual(64, DhSectionPos.getWidthCountForLowerDetailedSection(sectionPos, returnDetailLevel));
		
		
		// Region -> 3
		returnDetailLevel = 3;
		originSectionPos = DhSectionPos.convertToDetailLevel(originSectionPos, DhSectionPos.SECTION_REGION_DETAIL_LEVEL);
		assertSectionPosEqual(4096, DhSectionPos.getWidthCountForLowerDetailedSection(originSectionPos, returnDetailLevel));
		
		sectionPos = DhSectionPos.convertToDetailLevel(sectionPos, DhSectionPos.SECTION_REGION_DETAIL_LEVEL);
		assertSectionPosEqual(4096, DhSectionPos.getWidthCountForLowerDetailedSection(sectionPos, returnDetailLevel));
		
	}
	
	@Test
	public void getBlockWidth()
	{
		long originSectionPos = DhSectionPos.encode((byte) 0,0,0);
		long sectionPos = DhSectionPos.encode((byte) 0,-10000,5000);
		
		
		assertSectionPosEqual(1, DhSectionPos.getBlockWidth(originSectionPos));
		assertSectionPosEqual(1, DhSectionPos.getBlockWidth(sectionPos));
		
		originSectionPos = DhSectionPos.convertToDetailLevel(originSectionPos, (byte) 1);
		assertSectionPosEqual(2, DhSectionPos.getBlockWidth(originSectionPos));
		sectionPos = DhSectionPos.convertToDetailLevel(sectionPos, (byte) 1);
		assertSectionPosEqual(2, DhSectionPos.getBlockWidth(sectionPos));
		
		originSectionPos = DhSectionPos.convertToDetailLevel(originSectionPos, DhSectionPos.SECTION_BLOCK_DETAIL_LEVEL);
		assertSectionPosEqual(64, DhSectionPos.getBlockWidth(originSectionPos));
		sectionPos = DhSectionPos.convertToDetailLevel(sectionPos, DhSectionPos.SECTION_BLOCK_DETAIL_LEVEL);
		assertSectionPosEqual(64, DhSectionPos.getBlockWidth(sectionPos));
		
		originSectionPos = DhSectionPos.convertToDetailLevel(originSectionPos, DhSectionPos.SECTION_REGION_DETAIL_LEVEL);
		assertSectionPosEqual(32768, DhSectionPos.getBlockWidth(originSectionPos));
		sectionPos = DhSectionPos.convertToDetailLevel(sectionPos, DhSectionPos.SECTION_REGION_DETAIL_LEVEL);
		assertSectionPosEqual(32768, DhSectionPos.getBlockWidth(sectionPos));
		
	}
	
	@Test
	public void getCenterBlockPosOrigin()
	{
		long originSectionPos = DhSectionPos.encode((byte) 0,0,0);
		long sectionPos = DhSectionPos.encode((byte) 0,-10000,5000);
		
		
		
		// 1x1 blocks
		Assert.assertEquals(new DhBlockPos2D(0, 0), DhSectionPos.getCenterBlockPos(originSectionPos));
		Assert.assertEquals(new DhBlockPos2D(-10000, 5000), DhSectionPos.getCenterBlockPos(sectionPos));
		
		
		// 2x2 blocks
		originSectionPos = DhSectionPos.convertToDetailLevel(originSectionPos, (byte) 1);
		Assert.assertEquals(new DhBlockPos2D(0, 0), DhSectionPos.getCenterBlockPos(originSectionPos));
		sectionPos = DhSectionPos.convertToDetailLevel(sectionPos, (byte) 1);
		Assert.assertEquals(new DhBlockPos2D(-10000, 5000), DhSectionPos.getCenterBlockPos(sectionPos));
		//sectionPos = DhSectionPos.encode((byte) 1, 2303, 0);
		//Assert.assertEquals(new DhBlockPos2D(4606, 0), DhSectionPos.getCenterBlockPos(sectionPos));
		
		
		// 4x4 blocks
		originSectionPos = DhSectionPos.convertToDetailLevel(originSectionPos, (byte) 2);
		Assert.assertEquals(new DhBlockPos2D(2, 2), DhSectionPos.getCenterBlockPos(originSectionPos));
		sectionPos = DhSectionPos.convertToDetailLevel(sectionPos, (byte) 2);
		Assert.assertEquals(new DhBlockPos2D(-9998, 5002), DhSectionPos.getCenterBlockPos(sectionPos));
		
		
		// 64x64 blocks
		originSectionPos = DhSectionPos.convertToDetailLevel(originSectionPos, DhSectionPos.SECTION_BLOCK_DETAIL_LEVEL);
		Assert.assertEquals(new DhBlockPos2D(32, 32), DhSectionPos.getCenterBlockPos(originSectionPos));
		sectionPos = DhSectionPos.convertToDetailLevel(sectionPos, DhSectionPos.SECTION_BLOCK_DETAIL_LEVEL);
		Assert.assertEquals(new DhBlockPos2D(-10016, 5024), DhSectionPos.getCenterBlockPos(sectionPos));
		
		
		// 32,768 x 32,768 blocks
		originSectionPos = DhSectionPos.convertToDetailLevel(originSectionPos, DhSectionPos.SECTION_REGION_DETAIL_LEVEL);
		Assert.assertEquals(new DhBlockPos2D(16384, 16384), DhSectionPos.getCenterBlockPos(originSectionPos));
		sectionPos = DhSectionPos.convertToDetailLevel(sectionPos, DhSectionPos.SECTION_REGION_DETAIL_LEVEL);
		Assert.assertEquals(new DhBlockPos2D(-16384, 16384), DhSectionPos.getCenterBlockPos(sectionPos));
		
	}
	
	@Test
	public void getMinCornerBlockPos()
	{
		long pos;
		
		// origin block detail
		pos = DhSectionPos.encode((byte) 0,0,0);
		Assert.assertEquals(0, DhSectionPos.getMinCornerBlockX(pos));
		Assert.assertEquals(0, DhSectionPos.getMinCornerBlockZ(pos));
		
		// offset block detail
		pos = DhSectionPos.encode((byte) 0,2,3);
		Assert.assertEquals(2 * DhSectionPos.getBlockWidth(pos), DhSectionPos.getMinCornerBlockX(pos));
		Assert.assertEquals(3 * DhSectionPos.getBlockWidth(pos), DhSectionPos.getMinCornerBlockZ(pos));
		
		// negative offset block detail
		pos = DhSectionPos.encode((byte) 0,-1,-2);
		Assert.assertEquals(-1 * DhSectionPos.getBlockWidth(pos), DhSectionPos.getMinCornerBlockX(pos));
		Assert.assertEquals(-2 * DhSectionPos.getBlockWidth(pos), DhSectionPos.getMinCornerBlockZ(pos));
		
		
		// origin chunk detail
		pos = DhSectionPos.encode(DhSectionPos.SECTION_CHUNK_DETAIL_LEVEL,0,0);
		Assert.assertEquals(0, DhSectionPos.getMinCornerBlockX(pos));
		Assert.assertEquals(0, DhSectionPos.getMinCornerBlockZ(pos));
		
		// offset chunk detail
		pos = DhSectionPos.encode(DhSectionPos.SECTION_CHUNK_DETAIL_LEVEL,2,3);
		Assert.assertEquals(2 * DhSectionPos.getBlockWidth(pos), DhSectionPos.getMinCornerBlockX(pos));
		Assert.assertEquals(3 * DhSectionPos.getBlockWidth(pos), DhSectionPos.getMinCornerBlockZ(pos));
		
		
	}
	
	@Test
	public void getAdjacentPos()
	{
		long pos = DhSectionPos.encode((byte) 0, 0, 0);
		
		assertSectionPosEqual(DhSectionPos.encode((byte) 0, 0, -1), DhSectionPos.getAdjacentPos(pos, EDhDirection.NORTH));
		assertSectionPosEqual(DhSectionPos.encode((byte) 0, 0, 1), DhSectionPos.getAdjacentPos(pos, EDhDirection.SOUTH));
		
		assertSectionPosEqual(DhSectionPos.encode((byte) 0, 1, 0), DhSectionPos.getAdjacentPos(pos, EDhDirection.EAST));
		assertSectionPosEqual(DhSectionPos.encode((byte) 0, -1, 0), DhSectionPos.getAdjacentPos(pos, EDhDirection.WEST));
		
		// getting the adjacent position in the up and down position don't make sense
		Assert.assertThrows(IllegalArgumentException.class, () -> { DhSectionPos.getAdjacentPos(pos, EDhDirection.UP); });
		Assert.assertThrows(IllegalArgumentException.class, () -> { DhSectionPos.getAdjacentPos(pos, EDhDirection.DOWN); });
	}
	
	@Test
	public void forEachChildIterator()
	{
		long pos = DhSectionPos.encode((byte) 1, 0, 0);
		
		ArrayList<Long> childPosList = new ArrayList<>();
		AtomicInteger childCount = new AtomicInteger(0);
		DhSectionPos.forEachChild(pos, (childPos) -> 
		{
			childCount.incrementAndGet();
			childPosList.add(childPos);
		});
		
		Assert.assertTrue(childPosList.contains(DhSectionPos.encode((byte) 0, 0, 0)));
		Assert.assertTrue(childPosList.contains(DhSectionPos.encode((byte) 0, 1, 0)));
		Assert.assertTrue(childPosList.contains(DhSectionPos.encode((byte) 0, 0, 1)));
		Assert.assertTrue(childPosList.contains(DhSectionPos.encode((byte) 0, 1, 1)));
		
	}
	
	
	
	
	//================//
	// helper methods //
	//================//
	
	public static void assertSectionPosEqual(long expected, long actual) { assertSectionPosEqual("", expected, actual); }
	public static void assertSectionPosEqual(String messagePrefix, long expected, long actual)
	{
		if (!messagePrefix.endsWith(" "))
		{
			messagePrefix += " ";
		}
		
		String expectedString = DhSectionPos.toString(expected);
		String actualString = DhSectionPos.toString(actual);
		String mismatchSuffix = "expected: ["+expectedString+"] actual: ["+actualString+"].";
		
		Assert.assertEquals(messagePrefix+"Detail level mismatch, "+mismatchSuffix, DhSectionPos.getDetailLevel(expected), DhSectionPos.getDetailLevel(actual));
		Assert.assertEquals(messagePrefix+"X Pos mismatch, "+mismatchSuffix, DhSectionPos.getX(expected), DhSectionPos.getX(actual));
		Assert.assertEquals(messagePrefix+"Z Pos mismatch, "+mismatchSuffix, DhSectionPos.getZ(expected), DhSectionPos.getZ(actual));
	}
	
	
	
}
