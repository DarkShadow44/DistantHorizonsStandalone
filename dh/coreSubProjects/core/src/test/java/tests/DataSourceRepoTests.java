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

import com.seibel.distanthorizons.api.enums.config.EDhApiDataCompressionMode;
import com.seibel.distanthorizons.core.dataObjects.fullData.FullDataPointIdMap;
import com.seibel.distanthorizons.core.dataObjects.fullData.sources.FullDataSourceV2;
import com.seibel.distanthorizons.core.enums.EDhDirection;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.sql.dto.FullDataSourceV2DTO;
import com.seibel.distanthorizons.core.sql.dto.util.FullDataMinMaxPosUtil;
import com.seibel.distanthorizons.core.sql.repo.FullDataSourceV2Repo;
import com.seibel.distanthorizons.core.util.FullDataPointUtil;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.util.ThreadUtil;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Can also be used to test if there are memory leaks in SQLite
 * and if the {@link FullDataSourceV2DTO}/{@link FullDataSourceV2}'s are using pooled objects correctly
 */
public class DataSourceRepoTests
{
	public static String DATABASE_TYPE = "jdbc:sqlite";
	public static String DB_FILE_NAME = "test.sqlite";
	
	
	
	@BeforeClass
	public static void testSetup()
	{
		File dbFile = new File(DB_FILE_NAME);
		if (dbFile.exists())
		{
			Assert.assertTrue("unable to delete old test DB File.", dbFile.delete());
		}
	}
	
	
	
	@Test
	public void test()
	{
		try (final FullDataSourceV2Repo repo = new FullDataSourceV2Repo(DATABASE_TYPE, new File(DB_FILE_NAME)))
		{
			
			
			
			//========================//
			// create test datasource //
			//========================//
			
			long pos = DhSectionPos.encode((byte)6, 1, 2);
			FullDataPointIdMap dataMapping = new FullDataPointIdMap(pos);
			LongArrayList[] fullDataArray =  new LongArrayList[FullDataSourceV2.WIDTH * FullDataSourceV2.WIDTH];
            
			Random seededRandom = new Random(3);
			
			for (int arrayIndex = 0; arrayIndex < FullDataSourceV2.WIDTH * FullDataSourceV2.WIDTH; arrayIndex++)
			{
				fullDataArray[arrayIndex] = new LongArrayList(1);
				
				// random column heights so we can differentiate
				// columns from each other
				int columnCount = Math.abs(seededRandom.nextInt() % 31) + 1;
				for (int colIndex = 0; colIndex < columnCount; colIndex++)
				{
					long datapoint = FullDataPointUtil.encode(
							colIndex, // id 
							1, // height
							colIndex, // relative min Y
							(byte)(colIndex % LodUtil.MAX_MC_LIGHT), // block light 
							(byte)((colIndex + 2) % LodUtil.MAX_MC_LIGHT) // sky light
					);
					fullDataArray[arrayIndex].add(datapoint);
				}
			}
			
			byte[] columnGenStep = new byte[FullDataSourceV2.WIDTH * FullDataSourceV2.WIDTH];
			Arrays.fill(columnGenStep, (byte)3);
			
			byte[] columnWorldCompressionMode = new byte[FullDataSourceV2.WIDTH * FullDataSourceV2.WIDTH];
			Arrays.fill(columnWorldCompressionMode, (byte)3);
			
			
			
			FullDataSourceV2 originalDataSource = FullDataSourceV2.createWithData(pos, dataMapping, fullDataArray, columnGenStep, columnWorldCompressionMode);
			FullDataSourceV2DTO originalDto = FullDataSourceV2DTO.CreateFromDataSource(originalDataSource, EDhApiDataCompressionMode.LZMA2);
			repo.save(originalDto);
			
			
			// also create format-1 encoded version to ensure backwards compatibility
			long posV1 = DhSectionPos.encode((byte) 6, 2, 3);
			FullDataSourceV2 dataSourceFormatV1 = FullDataSourceV2.createWithData(posV1, dataMapping, fullDataArray, columnGenStep, columnWorldCompressionMode);
			FullDataSourceV2DTO dtoFormatV1 = FullDataSourceV2DTO.CreateFromDataSource(dataSourceFormatV1, EDhApiDataCompressionMode.LZMA2);
			FullDataSourceV2DTO.writeDataSourceDataArrayToBlobV1(
					dataSourceFormatV1.dataPoints,
					dtoFormatV1.compressedDataByteArray,
					EDhApiDataCompressionMode.LZMA2);
			dtoFormatV1.dataFormatVersion = FullDataSourceV2DTO.DATA_FORMAT.V1_NO_ADJACENT_DATA;
			repo.save(dtoFormatV1);
			
			
			
			//=======================//
			// confirm DTO data is   // 
			// the same after saving //
			//=======================//
			
			FullDataSourceV2DTO savedDto = repo.getByKey(pos);
			
			Assert.assertNotNull("Failed to find DTO", savedDto);
			Assert.assertEquals("Pos mismatch", originalDto.pos, savedDto.pos);
			assertArraysAreEqual(originalDto.compressedDataByteArray, savedDto.compressedDataByteArray);
			assertArraysAreEqual(originalDto.compressedColumnGenStepByteArray, savedDto.compressedColumnGenStepByteArray);
			assertArraysAreEqual(originalDto.compressedWorldCompressionModeByteArray, savedDto.compressedWorldCompressionModeByteArray);
			
			assertArraysAreEqual(originalDto.compressedNorthAdjDataByteArray, savedDto.compressedNorthAdjDataByteArray);
			assertArraysAreEqual(originalDto.compressedSouthAdjDataByteArray, savedDto.compressedSouthAdjDataByteArray);
			assertArraysAreEqual(originalDto.compressedEastAdjDataByteArray, savedDto.compressedEastAdjDataByteArray);
			assertArraysAreEqual(originalDto.compressedWestAdjDataByteArray, savedDto.compressedWestAdjDataByteArray);
			
			
			
			//========================//
			// confirm data source is // 
			// the same after saving  //
			//========================//
			
			try (FullDataSourceV2 savedDataSource = savedDto.createUnitTestDataSource())
			{
				Assert.assertNotNull("Failed to create DataSource", savedDataSource);
				Assert.assertEquals("Pos mismatch", originalDataSource.getPos(), savedDataSource.getPos());
				assertArraysAreEqual(originalDataSource.columnGenerationSteps, savedDataSource.columnGenerationSteps);
				assertArraysAreEqual(originalDataSource.columnWorldCompressionMode, savedDataSource.columnWorldCompressionMode);
				Assert.assertEquals(originalDataSource.dataPoints.length, savedDataSource.dataPoints.length);
				
				for (int x = 0; x < FullDataSourceV2.WIDTH; x++)
				{
					for (int z = 0; z < FullDataSourceV2.WIDTH; z++)
					{
						int index = FullDataSourceV2.relativePosToIndex(x, z);
						assertArraysAreEqual("Saved data column at rel pos ["+x+","+z+"] ", originalDataSource.dataPoints[index], savedDataSource.dataPoints[index]);
					}
				}
			}
			
			// check that we have proper backwards compatability to V1
			try (FullDataSourceV2 savedDataSource = repo.getByKey(posV1).createUnitTestDataSource())
			{
				Assert.assertNotNull("Failed to create DataSource", savedDataSource);
				assertArraysAreEqual(originalDataSource.columnGenerationSteps, savedDataSource.columnGenerationSteps);
				assertArraysAreEqual(originalDataSource.columnWorldCompressionMode,
						savedDataSource.columnWorldCompressionMode);
				Assert.assertTrue(originalDataSource.dataPoints.length == savedDataSource.dataPoints.length);
				
				for (int i = 0; i < FullDataSourceV2.WIDTH * FullDataSourceV2.WIDTH; i++)
				{
					assertArraysAreEqual(originalDataSource.dataPoints[i], savedDataSource.dataPoints[i]);
				}
			}
			
			
			
			//==============//
			// adjacent DTO //
			//==============//
			
			try (FullDataSourceV2DTO adjDto = repo.getAdjByPosAndDirection(pos, EDhDirection.NORTH))
			{
				assertArraysAreEqual(adjDto.compressedDataByteArray, savedDto.compressedNorthAdjDataByteArray);
			}
			
			try (FullDataSourceV2DTO adjDto = repo.getAdjByPosAndDirection(pos, EDhDirection.SOUTH))
			{
				assertArraysAreEqual(adjDto.compressedDataByteArray, savedDto.compressedSouthAdjDataByteArray);
			}
			
			try (FullDataSourceV2DTO adjDto = repo.getAdjByPosAndDirection(pos, EDhDirection.EAST))
			{
				assertArraysAreEqual(adjDto.compressedDataByteArray, savedDto.compressedEastAdjDataByteArray);
			}
			
			try (FullDataSourceV2DTO adjDto = repo.getAdjByPosAndDirection(pos, EDhDirection.WEST))
			{
				assertArraysAreEqual(adjDto.compressedDataByteArray, savedDto.compressedWestAdjDataByteArray);
			}
			
			
			
			//======================//
			// adjacent datasources //
			//======================//
			
			for (EDhDirection direction : EDhDirection.CARDINAL_COMPASS)
			{
				try (FullDataSourceV2DTO adjDto = repo.getAdjByPosAndDirection(pos, direction);
						FullDataSourceV2 adjSource = adjDto.createUnitTestDataSource(direction))
				{
					long encodedMinMaxPos = FullDataMinMaxPosUtil.getEncodedMinMaxPos(direction);
					int minX = FullDataMinMaxPosUtil.getAdjMinX(encodedMinMaxPos);
					int maxX = FullDataMinMaxPosUtil.getAdjMaxX(encodedMinMaxPos);
					int minZ = FullDataMinMaxPosUtil.getAdjMinZ(encodedMinMaxPos);
					int maxZ = FullDataMinMaxPosUtil.getAdjMaxZ(encodedMinMaxPos);
					
					for (int x = minX; x < maxX; x++)
					{
						for (int z = minZ; z < maxZ; z++)
						{
							int index = FullDataSourceV2.relativePosToIndex(x, z);
							LongArrayList adjDataColumn = adjSource.dataPoints[index];
							LongArrayList originalDataColumn = originalDataSource.dataPoints[index];
							
							assertArraysAreEqual(adjDataColumn, originalDataColumn);
						}
					}
					
					for (int x = 0; x < FullDataSourceV2.WIDTH; x++)
					{
						for (int z = 0; z < FullDataSourceV2.WIDTH; z++)
						{
							if (x >= minX && x < maxX
									&& z >= minZ && z < maxZ)
							{
								continue;
							}
							
							
							int index = FullDataSourceV2.relativePosToIndex(x, z);
							LongArrayList adjDataColumn = adjSource.dataPoints[index];
							Assert.assertEquals(1, adjDataColumn.size());
							Assert.assertEquals(FullDataPointUtil.EMPTY_DATA_POINT, adjDataColumn.getLong(0));
						}
					}
				}
			}
			
			
			
			
			//=========================//
			// (optional) loop forever //
			//=========================//
			
			// this can be set to "true"
			// so we can profile the DTO creation process and
			// see if there are any object leaks and how the GC
			// handles it
			if (false)
			{
				System.out.println("Initial save/get success, starting long update test for GC validation...");
				
				AtomicLong lastLogMsTime = new AtomicLong(0);
				LongAdder iterateCount = new LongAdder();
				
				int poolSize = Runtime.getRuntime().availableProcessors();
				CompletableFuture<?>[] futures = new CompletableFuture[poolSize];
				ThreadPoolExecutor pool = ThreadUtil.makeThreadPool(poolSize, "test pool");
				for (int threadIndex = 0; threadIndex < poolSize; threadIndex++)
				{
					final int finalThreadIndex = threadIndex;
					futures[threadIndex] = CompletableFuture.runAsync(() ->
					{
						// create a new DTO so each thread can have their own to work with,
						// otherwise they'll get stuck on locks for interacting with the same row
						FullDataSourceV2DTO threadDto = null;
						try
						{
							threadDto = FullDataSourceV2DTO.CreateFromDataSource(originalDataSource, EDhApiDataCompressionMode.LZMA2);
						}
						catch (IOException e)
						{
							throw new RuntimeException(e);
						}
						
						// new position so each DTO is different and saved to a different row in the DB
						long threadPos = DhSectionPos.encode((byte)6, finalThreadIndex, 0);
						threadDto.pos = threadPos;
						
						repo.save(threadDto); // runs significantly faster if we don't save
						Assert.assertNotNull(threadDto);
						
						// run for a long time
						for (int j = 0; j < 100_000_000; j++)
						{
							try (FullDataSourceV2DTO pooledDto = repo.getByKey(threadPos))
							{
								Assert.assertEquals(pooledDto.pos, threadDto.pos);
								Assert.assertFalse(pooledDto.compressedDataByteArray.isEmpty());
								Assert.assertFalse(pooledDto.compressedColumnGenStepByteArray.isEmpty());
								Assert.assertFalse(pooledDto.compressedWorldCompressionModeByteArray.isEmpty());
								
								try (FullDataSourceV2 dataSource = pooledDto.createUnitTestDataSource();
									FullDataSourceV2DTO compressedDto = FullDataSourceV2DTO.CreateFromDataSource(dataSource, EDhApiDataCompressionMode.Z_STD_BLOCK))
								{
									repo.save(compressedDto);
									
									
									
									iterateCount.increment();
									
									long time = System.currentTimeMillis();
									if (time - lastLogMsTime.get() > 30_000)
									{
										lastLogMsTime.set(time);
										
										Runtime runtime = Runtime.getRuntime();
										long free = runtime.freeMemory();
										long total = runtime.totalMemory();
										long max = runtime.maxMemory();
										
										System.out.println("count: "+iterateCount.sum()+"\tfree: "+free+"\ttotal: "+total+"\tmax: "+max);
									}
								}
								catch (Exception ignore)
								{
								}
							}
						}
					}, pool);
				}
				
				CompletableFuture.allOf(futures).join();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Assert.fail(e.getMessage());
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
