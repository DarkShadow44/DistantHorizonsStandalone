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

import com.seibel.distanthorizons.core.sql.dto.util.FullDataMinMaxPosUtil;
import org.junit.Assert;
import org.junit.Test;

public class FullDataMinMaxPosTest
{
	
	@Test
	public void EncodeAdjacentMinMaxPosTest()
	{
		int maxTest = 3;
		for (short minX = 0; minX < maxTest; minX++)
		{
			for (short maxX = 0; maxX < maxTest; maxX++)
			{
				for (short minZ = 0; minZ < maxTest; minZ++)
				{
					for (short maxZ = 0; maxZ < maxTest; maxZ++)
					{
						long encodedPos = FullDataMinMaxPosUtil.encodeAdjMinMaxPos(minX, maxX, minZ, maxZ);
						
						Assert.assertEquals(minX, FullDataMinMaxPosUtil.getAdjMinX(encodedPos));
						Assert.assertEquals(maxX, FullDataMinMaxPosUtil.getAdjMaxX(encodedPos));
						Assert.assertEquals(minZ, FullDataMinMaxPosUtil.getAdjMinZ(encodedPos));
						Assert.assertEquals(maxZ, FullDataMinMaxPosUtil.getAdjMaxZ(encodedPos));
					}
				}
			}
		}
		
	}
	
}
