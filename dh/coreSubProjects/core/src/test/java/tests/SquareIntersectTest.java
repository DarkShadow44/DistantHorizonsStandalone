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

import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import org.apache.logging.log4j.Level;
import com.seibel.distanthorizons.core.logging.DhLogger;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Assert;
import org.junit.Test;

public class SquareIntersectTest
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	static
	{
		// allow all logging levels
		Configurator.setRootLevel(Level.ALL);
	}
	
	
	
	public static boolean DoSquaresOverlap(long rect1MinPos, int rect1Width, long rect2MinPos, int rect2Width)
	{
		// Determine the coordinates of the rectangles
		float rect1MinX = DhSectionPos.getX(rect1MinPos);
		float rect1MaxX = DhSectionPos.getX(rect1MinPos) + rect1Width;
		float rect1MinZ = DhSectionPos.getZ(rect1MinPos);
		float rect1MaxZ = DhSectionPos.getZ(rect1MinPos) + rect1Width;
		
		float rect2MinX = DhSectionPos.getX(rect2MinPos);
		float rect2MaxX = DhSectionPos.getX(rect2MinPos) + rect2Width;
		float rect2MinZ = DhSectionPos.getZ(rect2MinPos);
		float rect2MaxZ = DhSectionPos.getZ(rect2MinPos) + rect2Width;
		
		// Check if the rectangles overlap
		return rect1MinX < rect2MaxX && rect1MaxX > rect2MinX && rect1MinZ < rect2MaxZ && rect1MaxZ > rect2MinZ;
	}
	
	
	
	// The first test case checks that two overlapping rectangles are detected as overlapping.
	@Test
	public void TestOverlappingSquares()
	{
		long rect1Min = DhSectionPos.encode((byte) 0, 1, 1);
		int rect1Width = 4;
		
		long rect2Min = DhSectionPos.encode((byte) 0, 3, 3);
		int rect2Width = 4;
		
		boolean result = DoSquaresOverlap(rect1Min, rect1Width, rect2Min, rect2Width);
		Assert.assertTrue(result);
	}
	
	// The second test case checks that two non-overlapping rectangles are detected as not overlapping.
	@Test
	public void TestNonOverlappingSquares()
	{
		long rect1Min = DhSectionPos.encode((byte) 0, 1, 1);
		int rect1Width = 2;
		
		long rect2Min = DhSectionPos.encode((byte) 0, 4, 4);
		int rect2Width = 2;
		
		boolean result = DoSquaresOverlap(rect1Min, rect1Width, rect2Min, rect2Width);
		Assert.assertFalse(result);
	}
	
	// The third test case checks that two rectangles with different sizes and overlapping are detected as overlapping.
	@Test
	public void TestSquaresWithDifferentSizes()
	{
		long rect1Min = DhSectionPos.encode((byte) 0, 1, 1);
		int rect1Width = 4;
		
		long rect2Min = DhSectionPos.encode((byte) 0, 3, 3);
		int rect2Width = 3;
		
		boolean result = DoSquaresOverlap(rect1Min, rect1Width, rect2Min, rect2Width);
		Assert.assertTrue(result);
	}
	
	// The fourth test case checks that a rectangle that contains another rectangle is detected as overlapping.
	@Test
	public void TestOneRectangleContainsTheOther()
	{
		long rect1Min = DhSectionPos.encode((byte) 0, 1, 1);
		int rect1Width = 9;
		
		long rect2Min = DhSectionPos.encode((byte) 0, 3, 3);
		int rect2Width = 3;
		
		boolean result = DoSquaresOverlap(rect1Min, rect1Width, rect2Min, rect2Width);
		Assert.assertTrue(result);
	}
	
	// The fifth test case checks that the same as the fourth, but with the rectangles swapped to make sure the method can detect overlapping in any configuration.
	@Test
	public void TestOneRectangleContainsTheOtherInverted()
	{
		long rect1Min = DhSectionPos.encode((byte) 0, 3, 3);
		int rect1Width = 3;
		
		long rect2Min = DhSectionPos.encode((byte) 0, 1, 1);
		int rect2Width = 9;
		
		boolean result = DoSquaresOverlap(rect1Min, rect1Width, rect2Min, rect2Width);
		Assert.assertTrue(result);
	}
	
}
