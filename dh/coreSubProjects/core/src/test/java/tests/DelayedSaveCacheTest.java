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
import com.seibel.distanthorizons.core.util.delayedSaveCache.DelayedDataSourceSaveCache;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.util.objects.pooling.PhantomArrayList.PhantomArrayListCheckout;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Assert;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A few very basic tests to confirm {@link DelayedDataSourceSaveCache}
 * is working properly.
 * <br><br>
 * Note: enabling this test will cause issues for {@link PooledDataSourceCheckoutTest}.
 * Probably due to creating additional checkouts in the global static state.
 * For now this issue can be ignored since this test is diabled.
 * 
 * @author James Seibel
 * @version 2025-10-02
 * 
 * @see PooledDataSourceCheckoutTest
 */
public class DelayedSaveCacheTest
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	static
	{
		// allow all logging levels
		Configurator.setRootLevel(Level.ALL);
	}
	
	
	/**
	 * commented out for now since it makes the normal build take longer
	 * and due to breaking {@link PooledDataSourceCheckoutTest}
	 */
	//@Test
	public void CacheExpirationAndPoolingTest() throws InterruptedException
	{
		// how many times any data source has been "written to disk"
		AtomicInteger diskSaveCountRef = new AtomicInteger(0);
		
		DelayedDataSourceSaveCache cache = new DelayedDataSourceSaveCache((FullDataSourceV2 fullDataSource) -> 
				{
					diskSaveCountRef.getAndIncrement();
					return CompletableFuture.completedFuture(null);
				}, 1_000);
		
		
		
		//==============================//
		// single item and manual flush //
		//==============================//
		
		//LOGGER.info("============ Single item - manual flush ============");
		
		PhantomArrayListCheckout initialCheckout;
		try (FullDataSourceV2 initialSource = FullDataSourceV2.createEmpty(DhSectionPos.encode((byte)6, 0, 0)))
		{
			initialCheckout = initialSource.getPhantomArrayCheckoutForUnitTesting();
			cache.writeToMemoryAndQueueSave( initialSource);
		}
		Assert.assertEquals("only 1 item should be in the cache", 1, cache.getUnsavedCount());
		Assert.assertEquals("no disk saves should have happened yet", 0, diskSaveCountRef.get());
		
		// manual flush
		cache.flush();
		Assert.assertEquals("memory cache should be empty after", 0, cache.getUnsavedCount());
		Assert.assertEquals("1 manual flush was expected", 1, diskSaveCountRef.get());
		
		
		
		//======================//
		// quick group position //
		//======================//
		
		//LOGGER.info("============ quick group - auto flush ============");
		
		// write multiple items for the same position
		for (int i = 0; i < 4; i++)
		{
			try (FullDataSourceV2 loopSource = FullDataSourceV2.createEmpty(DhSectionPos.encode((byte) 6, 0, 0)))
			{
				PhantomArrayListCheckout loopCheckout = loopSource.getPhantomArrayCheckoutForUnitTesting();
				Assert.assertEquals(initialCheckout, loopCheckout);
				
				cache.writeToMemoryAndQueueSave(loopSource);
			}
		}
		// each item writes to the same place
		Assert.assertEquals("exactly 1 item should be in the cache", 1, cache.getUnsavedCount());
		Assert.assertEquals("no new saves should have happened yet", 1, diskSaveCountRef.get());
		
		// wait for the cache to clear
		Thread.sleep(4_000);
		Assert.assertEquals("Cache should have automatically cleared due to inactivity", 0, cache.getUnsavedCount());
		Assert.assertEquals("second save after timeout expected", 2, diskSaveCountRef.get());
		
		
		
		//=====================//
		// slow group position //
		//=====================//
		
		//LOGGER.info("============ slow group - auto flush ============");
		
		// write multiple items for the same position
		for (int i = 0; i < 4; i++)
		{
			try (FullDataSourceV2 loopSource = FullDataSourceV2.createEmpty(DhSectionPos.encode((byte) 6, 0, 0)))
			{
				PhantomArrayListCheckout loopCheckout = loopSource.getPhantomArrayCheckoutForUnitTesting();
				Assert.assertEquals(initialCheckout, loopCheckout);
				
				cache.writeToMemoryAndQueueSave(loopSource);
			}
			
			// long enough to prevent a timeout, but short enough that they don't happen all at once
			Thread.sleep(500);
		}
		// each item writes to the same place
		Assert.assertEquals("exactly 1 item should be in the cache", 1, cache.getUnsavedCount());
		Assert.assertEquals("no new saves should have happened yet", 2, diskSaveCountRef.get());
		
		// wait for the cache to clear
		Thread.sleep(2_000);
		Assert.assertEquals("Cache should have automatically cleared due to inactivity", 0, cache.getUnsavedCount());
		Assert.assertEquals("third timeout expected", 3, diskSaveCountRef.get());
		
	}
	
	
	
}
