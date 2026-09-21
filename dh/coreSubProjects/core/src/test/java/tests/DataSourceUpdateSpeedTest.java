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

import com.seibel.distanthorizons.api.enums.config.EDhApiWorldCompressionMode;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiWorldGenerationStep;
import com.seibel.distanthorizons.core.dataObjects.fullData.FullDataPointIdMap;
import com.seibel.distanthorizons.core.dataObjects.fullData.sources.FullDataSourceV2;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.util.FullDataPointUtil;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.util.objects.DataCorruptedException;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.junit.Assert;
import org.junit.Test;
import testItems.wrappers.TestBiomeWrapper;
import testItems.wrappers.TestBlockStateWrapper;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Random;

/**
 * 
 */
public class DataSourceUpdateSpeedTest
{
	//@Test
	public void test() throws DataCorruptedException
	{	
		Random seededRandom = new Random(3);
		
		
		//===================//
		// parent datasource //
		//===================//
		
		long parentPos = DhSectionPos.encode((byte)7, 0, 0);
		
		FullDataSourceV2 parentDataSource;
		{
			FullDataPointIdMap dataMapping = new FullDataPointIdMap(parentPos);
			LongArrayList[] fullDataArray =  new LongArrayList[FullDataSourceV2.WIDTH * FullDataSourceV2.WIDTH];
			
			for (int arrayIndex = 0; arrayIndex < FullDataSourceV2.WIDTH * FullDataSourceV2.WIDTH; arrayIndex++)
			{
				fullDataArray[arrayIndex] = new LongArrayList(1);
				
				// random column heights so we can differentiate
				// columns from each other
				int columnCount = Math.abs(seededRandom.nextInt() % 31) + 1;
				int lastMaxY = 4000;
				for (int colIndex = columnCount; colIndex >= 0; colIndex--)
				{
					int height = (Math.abs(seededRandom.nextInt()) % 20) + 1;
					lastMaxY -= height;
					
					long datapoint = FullDataPointUtil.encode(
							colIndex, // id 
							height, // height
							lastMaxY, // relative min Y
							LodUtil.MIN_MC_LIGHT, // block light 
							LodUtil.MIN_MC_LIGHT // sky light
					);
					fullDataArray[arrayIndex].add(datapoint);
					
					dataMapping.addIfNotPresentAndGetId(
							new TestBiomeWrapper(colIndex+""),
							new TestBlockStateWrapper(colIndex+""));
				}
				
				FullDataSourceV2.throwIfDataColumnInWrongOrder(parentPos, fullDataArray[arrayIndex]);
			}
			
			byte[] columnGenStep = new byte[FullDataSourceV2.WIDTH * FullDataSourceV2.WIDTH];
			Arrays.fill(columnGenStep, EDhApiWorldGenerationStep.FEATURES.value);
			
			byte[] columnWorldCompressionMode = new byte[FullDataSourceV2.WIDTH * FullDataSourceV2.WIDTH];
			Arrays.fill(columnWorldCompressionMode, EDhApiWorldCompressionMode.VISUALLY_EQUAL.value);
			
			parentDataSource = FullDataSourceV2.createWithData(parentPos, dataMapping, fullDataArray, columnGenStep, columnWorldCompressionMode);
		}
		
		
		
		
		//==================//
		// child datasource //
		//==================//
		
		long childPos = DhSectionPos.encode((byte)6, 0, 0);
		
		FullDataSourceV2 childDataSource;
		{
			FullDataPointIdMap dataMapping = new FullDataPointIdMap(childPos);
			LongArrayList[] fullDataArray =  new LongArrayList[FullDataSourceV2.WIDTH * FullDataSourceV2.WIDTH];
			
			for (int arrayIndex = 0; arrayIndex < FullDataSourceV2.WIDTH * FullDataSourceV2.WIDTH; arrayIndex++)
			{
				fullDataArray[arrayIndex] = new LongArrayList(1);
				
				// random column heights so we can differentiate
				// columns from each other
				int columnCount = Math.abs(seededRandom.nextInt() % 31) + 1;
				int lastMaxY = 4000;
				for (int colIndex = columnCount; colIndex >= 0; colIndex--)
				{
					int height = (Math.abs(seededRandom.nextInt()) % 20) + 1;
					lastMaxY -= height;
					
					long datapoint = FullDataPointUtil.encode(
							colIndex, // id 
							height, // height
							lastMaxY, // relative min Y
							LodUtil.MAX_MC_LIGHT, // block light 
							LodUtil.MAX_MC_LIGHT // sky light
					);
					fullDataArray[arrayIndex].add(datapoint);
					
					dataMapping.addIfNotPresentAndGetId(
							new TestBiomeWrapper(colIndex+""), 
							new TestBlockStateWrapper(colIndex+""));
				}
				
				FullDataSourceV2.throwIfDataColumnInWrongOrder(childPos, fullDataArray[arrayIndex]);
			}
			
			byte[] columnGenStep = new byte[FullDataSourceV2.WIDTH * FullDataSourceV2.WIDTH];
			Arrays.fill(columnGenStep, EDhApiWorldGenerationStep.FEATURES.value);
			
			byte[] columnWorldCompressionMode = new byte[FullDataSourceV2.WIDTH * FullDataSourceV2.WIDTH];
			Arrays.fill(columnWorldCompressionMode, EDhApiWorldCompressionMode.VISUALLY_EQUAL.value);
			
			
			
			childDataSource = FullDataSourceV2.createWithData(childPos, dataMapping, fullDataArray, columnGenStep, columnWorldCompressionMode);
		}
		
		
		
		//=========================//
		// (optional) loop forever //
		//=========================//
		
		// this can be set to "true"
		// so we can profile the DTO creation process and
		// see if there are any object leaks and how the GC
		// handles it
		if (true)
		{
			System.out.println("Starting long update test for time testing...");
			
			long lastLogMsTime = 0;
			
			long totalNano = 0;
			
			NumberFormat numberFormat = NumberFormat.getNumberInstance();
			
			// run for a long time
			for (int i = 1; i < 100_000_000; i++)
			{
				long startNano = System.nanoTime();
				
				boolean updated = parentDataSource.updateFromDataSource(childDataSource);
				//Assert.assertTrue(updated);
				
				long updateTimeNano = System.nanoTime() - startNano;
				totalNano += updateTimeNano;
				
				long nowMs = System.currentTimeMillis();
				if (nowMs - lastLogMsTime > 5_000)
				{
					lastLogMsTime = nowMs;
					
					double avgMs = ((totalNano / 1_000_000.0)/i);
					double totalMs = totalNano / 1_000_000.0;
					System.out.println("count: "+numberFormat.format(i)+"\tavg ms: "+numberFormat.format(avgMs)+"\ttotal ms: "+numberFormat.format(totalMs));
				}
			}
		}
	}
	
	
	//================//
	// helper methods //
	//================//
	
	private static void assertArraysAreEqual(ByteArrayList expectedArray, ByteArrayList actualArray)
	{
		Assert.assertEquals("size mismatch", expectedArray.size(), actualArray.size());
		
		for (int i = 0; i < expectedArray.size(); i++)
		{
			byte expectedNumb = expectedArray.getByte(i);
			byte actualNumb = actualArray.getByte(i);
			
			Assert.assertEquals("value mismatch at index ["+i+"]", expectedNumb, actualNumb);
		}
	}
	
	private static void assertArraysAreEqual(LongArrayList expectedArray, LongArrayList actualArray)
	{ assertArraysAreEqual(null, expectedArray, actualArray); }
	private static void assertArraysAreEqual(String message, LongArrayList expectedArray, LongArrayList actualArray)
	{
		Assert.assertEquals(message + "size mismatch", expectedArray.size(), actualArray.size());
		
		for (int i = 0; i < expectedArray.size(); i++)
		{
			long expectedNumb = expectedArray.getLong(i);
			long actualNumb = actualArray.getLong(i);
			
			Assert.assertEquals(message + "value mismatch at index ["+i+"]", expectedNumb, actualNumb);
		}
	}
	
	
	
}
