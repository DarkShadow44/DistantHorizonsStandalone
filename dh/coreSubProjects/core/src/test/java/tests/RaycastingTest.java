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

import com.seibel.distanthorizons.core.util.RayCastUtil;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author James Seibel
 * @version 2022-11-19
 */
public class RaycastingTest
{
	
	@Test
	public void HorizontalSquareTests()
	{
		// 1x1 square at (1,1) - (2,2)
		double squareMinX = 1;
		double squareMinY = 1;
		int squareWidth = 1;
		
		
		
		// ray points right - direction <1,0>
		double xDir = 1;
		double yDir = 0;
		
		// ray origin left of square
		double rayX = 0;
		testRay(false, rayX, 0, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(false, rayX, 0.5, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
		testRay(true, rayX, 1, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(true, rayX, 1.5, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(true, rayX, 2, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
		testRay(false, rayX, 2.5, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(false, rayX, 3, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
		// ray origin right of square
		rayX = 2.5;
		testRay(false, rayX, 1, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(false, rayX, 1.5, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(false, rayX, 2, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
		
		// ray points left - direction <-1,0>
		xDir = -1;
		yDir = 0;
		
		// ray origin left of square
		rayX = 0;
		testRay(false, rayX, 0, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(false, rayX, 0.5, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
		testRay(false, rayX, 1, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(false, rayX, 1.5, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(false, rayX, 2, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
		testRay(false, rayX, 2.5, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(false, rayX, 3, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
		// ray origin right of square
		rayX = 2.5;
		testRay(true, rayX, 1, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(true, rayX, 1.5, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(true, rayX, 2, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
	}
	
	@Test
	public void VerticalSquareTests()
	{
		// 1x1 square at (1,1) - (2,2)
		double squareMinX = 1;
		double squareMinY = 1;
		int squareWidth = 1;
		
		
		
		// ray points up - direction <0,1>
		double xDir = 0;
		double yDir = 1;
		
		// ray origin under square //
		double rayY = 0;
		
		// ray origin below square
		testRay(false, 0, rayY, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(false, 0.5, rayY, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
		testRay(true, 1, rayY, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(true, 1.5, rayY, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(true, 2, rayY, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
		testRay(false, 2.5, rayY, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(false, 3, rayY, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
		
		// ray origin in square
		testRay(true, 1, 1, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(true, 1.5, 1.5, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(true, 2, 2, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
		
		// ray origin above square
		rayY = 2.5;
		testRay(false, 1, rayY, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(false, 1.5, rayY, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(false, 2, rayY, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
		
		
		// ray points down - direction <0,-1>
		xDir = 0;
		yDir = -1;
		
		// ray origin under square //
		rayY = 0;
		
		// ray origin below square
		testRay(false, 1, rayY, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(false, 1.5, rayY, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(false, 2, rayY, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
		
		// ray origin in square
		testRay(true, 1, 1, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(true, 1.5, 1.5, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(true, 2, 2, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
		
		// ray origin above square
		rayY = 2.5;
		testRay(false, 0, rayY, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(false, 0.5, rayY, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
		testRay(true, 1, rayY, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(true, 1.5, rayY, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(true, 2, rayY, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
		testRay(false, 2.5, rayY, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(false, 3, rayY, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
	}
	
	@Test
	public void PerpendicularSquareTests()
	{
		// 1x1 square at (1,1) - (2,2)
		double squareMinX = 1;
		double squareMinY = 1;
		int squareWidth = 1;
		
		
		
		// AKA the slope is perpendicular to this plane
		// direction <0,0>
		double xDir = 0;
		double yDir = 0;
		testRay(false, 0, 0, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(false, 0.5, 0.5, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
		testRay(true, 1, 1, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(true, 1.5, 1.5, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(true, 2, 2, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
		testRay(false, 2.5, 2.5, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(false, 3, 3, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
	}
	
	@Test
	public void DiagonalSquareTests()
	{
		// 1x1 square at (1,1) - (2,2)
		double squareMinX = 1;
		double squareMinY = 1;
		int squareWidth = 1;
		
		
		
		// ray points up right - direction <4,3> (a slope of 3/4) //
		double xDir = 4;
		double yDir = 3;
		
		// ray origin bottom left of square
		double rayX = 0;
		testRay(false, rayX, -1, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
		testRay(true, rayX, -0.5, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(true, rayX, 0, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(true, rayX, 0.5, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(true, rayX, 1, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
		testRay(false, rayX, 1.5, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(false, rayX, 2, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
		
		// ray origin in square
		testRay(true, 1, 1, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(true, 1.5, 1.5, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(true, 2, 2, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
		
		// ray origin right of square
		rayX = 2.5;
		double rayY = (yDir / xDir) * rayX; // y = mx + b // where b is the constants defined below
		testRay(false, rayX, -0.5 + rayY, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(false, rayX, 0 + rayY, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(false, rayX, 0.5 + rayY, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(false, rayX, 1 + rayY, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
		
		
		// ray points down right - direction <-4,3> (a slope of -3/4) //
		
		// move the cube to (1,-2) - (2,-1)
		squareMinY = -2;
		
		//xDir = 4;
		yDir = -3;
		
		// ray origin bottom left of square
		rayX = 0;
		testRay(false, rayX, 1, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
		testRay(true, rayX, 0.5, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(true, rayX, 0, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(true, rayX, -0.5, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(true, rayX, -1, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
		testRay(false, rayX, -1.5, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(false, rayX, -2, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
		
		// ray origin in square
		testRay(true, 1, -1, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(true, 1.5, -1.5, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(true, 2, -2, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
		
		
		// ray points down left - direction <-4,-3> (a slope of 3/4) //
		
		// move the cube to (-2,-2) - (-1,-1)
		//squareMinY = -2;
		squareMinX = -2;
		
		xDir = -4;
		//yDir = -3;
		
		// ray origin bottom left of square
		rayX = 0;
		testRay(false, rayX, 1, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
		testRay(true, rayX, 0.5, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(true, rayX, 0, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(true, rayX, -0.5, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(true, rayX, -1, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
		testRay(false, rayX, -1.5, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(false, rayX, -2, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
		
		// ray origin in square
		testRay(true, -1, -1, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(true, -1.5, -1.5, xDir, yDir, squareMinX, squareMinY, squareWidth);
		testRay(true, -2, -2, xDir, yDir, squareMinX, squareMinY, squareWidth);
		
		
	}
	
	
	private static void testRay(boolean expectedToIntersect, double rayX, double rayY, double xDir, double yDir, double squareMinX, double squareMinY, double squareWidth)
	{
		boolean intersects = RayCastUtil.rayIntersectsSquare(rayX, rayY, xDir, yDir, squareMinX, squareMinY, squareWidth);
		Assert.assertEquals(failMessage(rayX, rayY, xDir, yDir, squareMinX, squareMinY, squareWidth), expectedToIntersect, intersects);
	}
	
	private static String failMessage(double rayX, double rayY, double xDir, double yDir, double squareMinX, double squareMinY, double squareWidth)
	{
		return "ray: [" + rayX + ", " + rayY + "] <" + xDir + ", " + yDir + ">     square: [" + squareMinX + ", " + squareMinY + "] - [" + (squareMinX + squareWidth) + ", " + (squareMinY + squareWidth) + "]";
	}
	
}
