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

import com.seibel.distanthorizons.core.dataObjects.fullData.sources.FullDataSourceV2;
import com.seibel.distanthorizons.core.util.objects.pooling.PhantomArrayList.PhantomArrayListCheckout;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import org.junit.Assert;

/**
 * Note: this test will have issues if {@link DelayedSaveCacheTest} is also enabled.
 * Probably due to creating additional checkouts in the global static state.
 * For now this issue can be ignored.
 * 
 * @author James Seibel
 * @version 2025-10-02
 * 
 * @see PhantomArrayListCheckout
 * @see DelayedSaveCacheTest
 */
public class PooledDataSourceCheckoutTest
{
	
	/**
	 * commented out for now since it has
	 * a chance of breaking if any other tests
	 * using the same pools run at the same time
	 * or before this one.
	 */
	//@Test
	public void TestCheckouts()
	{
		// something like this should probably be called before starting the test to ensure
		// we have a clean slate, otherwise other tests could clutter up the static state
		// and cause the test to fail
		//FullDataSourceV2.ARRAY_LIST_POOL.clear();
		
		PhantomArrayListCheckout initialCheckout;
		try (FullDataSourceV2 initialSource = FullDataSourceV2.createEmpty(DhSectionPos.encode((byte)6, 0, 0)))
		{
			initialCheckout = initialSource.getPhantomArrayCheckoutForUnitTesting();
		}
		
		try (FullDataSourceV2 outerSource = FullDataSourceV2.createEmpty(DhSectionPos.encode((byte) 6, 0, 0)))
		{
			PhantomArrayListCheckout outerCheckout = outerSource.getPhantomArrayCheckoutForUnitTesting();
			// Note: this can fail if this test is run at the same time as other tasks (generating additional checkouts)
			Assert.assertEquals("the first checkout object should be pooled", initialCheckout, outerCheckout);
			
			try (FullDataSourceV2 innerSource = FullDataSourceV2.createEmpty(DhSectionPos.encode((byte) 6, 0, 0)))
			{
				PhantomArrayListCheckout innerCheckout = innerSource.getPhantomArrayCheckoutForUnitTesting();
				Assert.assertNotEquals("the second checkout object should not be shared when the first is still in use", initialCheckout, innerCheckout);
				Assert.assertNotEquals("the second checkout object should not be shared when the first is still in use", outerCheckout, innerCheckout);
			}
		}
	}
	
}
