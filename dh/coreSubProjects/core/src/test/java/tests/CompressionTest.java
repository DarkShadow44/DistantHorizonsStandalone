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
import com.seibel.distanthorizons.core.dataObjects.fullData.sources.FullDataSourceV2;
import com.seibel.distanthorizons.core.sql.dto.FullDataSourceV2DTO;
import com.seibel.distanthorizons.core.sql.repo.FullDataSourceV2Repo;
import com.seibel.distanthorizons.coreapi.util.StringUtil;
import it.unimi.dsi.fastutil.longs.LongArrayList;
//import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream;
import org.junit.Assert;

import java.io.*;

/**
 * <strong>Note:</strong>
 * In order to test the compressors that aren't currently in use: <br>
 * 1. Generate DH data (64 DH render distance is suggest) 
 * 2. Point the {@link CompressionTest#TEST_DIR} variable to the world's "data" folder.
 * 3. (Optional) Add the following to build.gradle's dependencies block: <br>
 * <code>
 * forgeShadowMe('org.apache.commons:commons-compress:1.26.1')
 * forgeShadowMe('org.itadaki:bzip2:0.9.1')
 * forgeShadowMe('lzma:lzma:0.0.1')
 * </code><br>
 * 4. (Optional) Uncomment the tests in this file <br>
 * 5. Run tests like normal
 */
public class CompressionTest
{
	public static String TEST_DIR = "C:/Users/James_Seibel/Desktop/DH Files/test files";
	public static String DB_FILE_NAME_PREFIX = "DistantHorizons";
	public static String UNCOMPRESSED_DB_FILE_NAME = "DistantHorizons_uncompressed.sqlite";
	
	/** -1 will test all of them */
	public static int MAX_DTO_TEST_COUNT = -1;
	
	
	// collapse the following commented out code when looking at tests
	
	//@Test
	//public void GZIP() // DNF
	//{
	//	String compressorName = "GZIP";
	//	
	//	DhDataInputStream.CreateInputStreamFunc createInputStreamFunc = (inputStream) -> new GZIPInputStream(inputStream);
	//	DhDataOutputStream.CreateOutputStreamFunc createOutputStreamFunc = (outputStream) -> new GZIPOutputStream(outputStream);
	//	
	//	this.testCompressor(compressorName, createInputStreamFunc, createOutputStreamFunc);
	//}

	//@Test
	//public void BZip2() // DNF
	//{
	//	String compressorName = "bzip2";
	//	
	//	DhDataInputStream.CreateInputStreamFunc createInputStreamFunc = (inputStream) -> new BZip2InputStream(inputStream, true);
	//	DhDataOutputStream.CreateOutputStreamFunc createOutputStreamFunc = (outputStream) -> new BZip2OutputStream(outputStream);
	//	
	//	this.testCompressor(compressorName, createInputStreamFunc, createOutputStreamFunc);
	//}

	//@Test
	//public void blockLz4() // DNF
	//{
	//	String compressorName = "Block LZ4";
	//	
	//	DhDataInputStream.CreateInputStreamFunc createInputStreamFunc = (inputStream) -> new BlockLZ4CompressorInputStream(inputStream);
	//	DhDataOutputStream.CreateOutputStreamFunc createOutputStreamFunc = (outputStream) -> new BlockLZ4CompressorOutputStream(outputStream);
	//	
	//	this.testCompressor(compressorName, createInputStreamFunc, createOutputStreamFunc);
	//}

	//@Test
	//public void lzma() // DNF, doesn't support flushing
	//{
	//	String compressorName = "lzma";
	//	
	//	DhDataInputStream.CreateInputStreamFunc createInputStreamFunc = (inputStream) -> new LZMA2InputStream(inputStream, LZMA2Options.DICT_SIZE_MIN);
	//	DhDataOutputStream.CreateOutputStreamFunc createOutputStreamFunc = (outputStream) -> new LZMAOutputStream(outputStream, new LZMA2Options(), -1);
	//	
	//	this.testCompressor(compressorName, createInputStreamFunc, createOutputStreamFunc);
	//}

	//@Test
	//public void deflate() // DNF
	//{
	//	String compressorName = "deflate";
	//	
	//	DhDataInputStream.CreateInputStreamFunc createInputStreamFunc = (inputStream) -> new DeflateCompressorInputStream(inputStream);
	//	DhDataOutputStream.CreateOutputStreamFunc createOutputStreamFunc = (outputStream) -> new DeflateCompressorOutputStream(outputStream);
	//	
	//	this.testCompressor(compressorName, createInputStreamFunc, createOutputStreamFunc);
	//}
	
	//@Test
	//public void snappy() // DNF
	//{
	//	String compressorName = "snappy";
	//	
	//	DhDataInputStream.CreateInputStreamFunc createInputStreamFunc = (inputStream) -> new SnappyCompressorInputStream(inputStream);
	//	DhDataOutputStream.CreateOutputStreamFunc createOutputStreamFunc = (outputStream) -> new SnappyCompressorOutputStream(outputStream, Long.MAX_VALUE);
	//	
	//	this.testCompressor(compressorName, createInputStreamFunc, createOutputStreamFunc);
	//}
	
	
	////@Test
	//public void ZstdDictionary() throws SQLException // isn't any better than normal Zstd
	//{
	//	String compressorName = "ZstdDictionary";
	//
	//	BufferPool pool = RecyclingBufferPool.INSTANCE;
	//
	//
	//	// create the dictionary
	//	byte[] dictionary;
	//	{
	//		String uncompressedDatabaseFilePath = TEST_DIR + "/" + UNCOMPRESSED_DB_FILE_NAME;
	//		FullDataSourceV2Repo uncompressedRepo = new FullDataSourceV2Repo("jdbc:sqlite", uncompressedDatabaseFilePath);
	//		ArrayList<DhSectionPos> positionList = uncompressedRepo.getAllPositions();
	//
	//		// sample size of 10 MB or less
	//		// dictionary size of 64 KB (1 MB and 10 MB's both seemed to perform worse)
	//		ZstdDictTrainer dictTrainer = new ZstdDictTrainer(10 * 1024 * 1024, 64 * 1024);
	//
	//		for (int i = 0; i < positionList.size(); i++)
	//		{
	//			DhSectionPos pos = positionList.get(i);
	//			FullDataSourceV2DTO uncompressedDto = uncompressedRepo.getByKey(pos);
	//
	//			dictTrainer.addSample(uncompressedDto.dataByteArray);
	//		}
	//
	//		dictionary = dictTrainer.trainSamples();
	//	}
	//
	//
	//
	//
	//	DhDataInputStream.CreateInputStreamFunc createInputStreamFunc = (inputStream) ->
	//	{
	//		ZstdInputStream stream = new ZstdInputStream(inputStream, pool);
	//		stream.setDict(dictionary);
	//		return stream;
	//	};
	//	DhDataOutputStream.CreateOutputStreamFunc createOutputStreamFunc = (outputStream) ->
	//	{
	//		ZstdOutputStream stream = new ZstdOutputStream(outputStream, pool);
	//		stream.setDict(dictionary);
	//		return stream;
	//	};
	//
	//	this.testCompressor(compressorName, createInputStreamFunc, createOutputStreamFunc);
	//}
	
	//@Test
	//public void Lz4FastCompression() // DNF
	//{
	//	String compressorName = "LZ4FastCompression";
	//	
	//	LZ4FastDecompressor fastCompressor = LZ4Factory.fastestInstance().fastDecompressor();
	//	
	//	DhDataInputStream.CreateInputStreamFunc createInputStreamFunc = (inputStream) -> new LZ4BlockInputStream(inputStream, fastCompressor);
	//	DhDataOutputStream.CreateOutputStreamFunc createOutputStreamFunc = (outputStream) -> new LZ4BlockOutputStream(outputStream);
	//	
	//	this.testCompressor(compressorName, createInputStreamFunc, createOutputStreamFunc);
	//}
	
	//@Test
	public void NoCompression()
	{
		String compressorName = "Uncompressed";
		this.testCompressor(compressorName, EDhApiDataCompressionMode.UNCOMPRESSED);
	}
	
	//@Test
	public void Lz4() // fast, poor compression
	{
		String compressorName = "LZ4";
		this.testCompressor(compressorName, EDhApiDataCompressionMode.LZ4);
	}
	
	//@Test
	public void Zstd() // middle of the road
	{
		String compressorName = "Zstd";
		this.testCompressor(compressorName, EDhApiDataCompressionMode.Z_STD_BLOCK);
	}
	
	//@Test
	public void LZMA2() // very slow, very good compression though
	{
		String compressorName = "LZMA";
		this.testCompressor(compressorName, EDhApiDataCompressionMode.LZMA2);
	}
	
	
	
	//=================//
	// testing methods //
	//=================//
	
	private void testCompressor(String compressorName, EDhApiDataCompressionMode compressionMode)
	{
		System.out.println("\n");
		System.out.println("Testing " + compressorName);
		
		
		long minUncompressedDtoSizeInBytes = Long.MAX_VALUE;
		long maxUncompressedDtoSizeInBytes = 0;
		long avgUncompressedDtoSizeInBytes = 0;
		
		long minCompressedDtoSizeInBytes = Long.MAX_VALUE;
		long maxCompressedDtoSizeInBytes = 0;
		long avgCompressedDtoSizeInBytes = 0;
		
		long totalReadTimeInNano = 0;
		long totalWriteTimeInNano = 0;
		
		
		long totalUncompressedFileSizeInBytes;
		long totalCompressedFileSizeInBytes;
		
		
		FullDataSourceV2Repo uncompressedRepo = null;
		FullDataSourceV2Repo compressedRepo = null;
		try
		{
			String uncompressedDatabaseFilePath = TEST_DIR + "/" + UNCOMPRESSED_DB_FILE_NAME;
			File uncompressedDatabaseFile = new File(uncompressedDatabaseFilePath);
			Assert.assertTrue(uncompressedDatabaseFile.exists());
			
			uncompressedRepo = new FullDataSourceV2Repo("jdbc:sqlite", uncompressedDatabaseFile);
			
			
			String compressedDatabaseFilePath = TEST_DIR + "/output/" + DB_FILE_NAME_PREFIX + "_" + compressorName + ".sqlite";
			File compressedDatabaseFile = new File(compressedDatabaseFilePath);
			compressedDatabaseFile.mkdirs();
			compressedDatabaseFile.delete();
			Assert.assertTrue(!compressedDatabaseFile.exists());
			compressedRepo = new FullDataSourceV2Repo("jdbc:sqlite", compressedDatabaseFile);
			
			
			
			LongArrayList positionList = uncompressedRepo.getAllPositions();
			totalUncompressedFileSizeInBytes = uncompressedRepo.getTotalDataSizeInBytes();
			System.out.println("Found [" + positionList.size() + "] DTOs.");
			
			long processedDtoCount = 0;
			int maxTestPosition = (MAX_DTO_TEST_COUNT == -1) ? positionList.size() : MAX_DTO_TEST_COUNT;
			for (int i = 0; i < maxTestPosition; i++)
			{
				try
				{
					long pos = positionList.getLong(i);
					if (i % 20 == 0)
					{
						System.out.println(i + "/" + maxTestPosition);
					}
					
					
					
					// uncompressed input //
					
					try (FullDataSourceV2DTO uncompressedDto = uncompressedRepo.getByKey(pos))
					{
						Assert.assertEquals(uncompressedDto.compressionModeValue, EDhApiDataCompressionMode.UNCOMPRESSED.value);
						FullDataSourceV2 uncompressedDataSource = uncompressedDto.createUnitTestDataSource();
						
						long uncompressedDtoSize = uncompressedRepo.getDataSizeInBytes(pos);
						minUncompressedDtoSizeInBytes = Math.min(uncompressedDtoSize, minUncompressedDtoSizeInBytes);
						maxUncompressedDtoSizeInBytes = Math.max(uncompressedDtoSize, maxUncompressedDtoSizeInBytes);
						avgUncompressedDtoSizeInBytes += uncompressedDtoSize;
						
						
						// compress file //
						
						long startWriteNanoTime = System.nanoTime();
						
						FullDataSourceV2DTO compressedDto = FullDataSourceV2DTO.CreateFromDataSource(uncompressedDataSource, compressionMode);
						compressedRepo.save(compressedDto);
						
						long endWriteNanoTime = System.nanoTime();
						totalWriteTimeInNano += (endWriteNanoTime - startWriteNanoTime);
						
						
						long compressedDtoSize = compressedRepo.getDataSizeInBytes(pos);
						minCompressedDtoSizeInBytes = Math.min(compressedDtoSize, minCompressedDtoSizeInBytes);
						maxCompressedDtoSizeInBytes = Math.max(compressedDtoSize, maxCompressedDtoSizeInBytes);
						avgCompressedDtoSizeInBytes += compressedDtoSize;
						
						
						// read compressed file //
						
						long startReadNanoTime = System.nanoTime();
						
						compressedDto = compressedRepo.getByKey(pos);
						FullDataSourceV2 compressedDataSource = compressedDto.createUnitTestDataSource();
						
						long endReadMsTime = System.nanoTime();
						totalReadTimeInNano += (endReadMsTime - startReadNanoTime);
						
						
						processedDtoCount++;
					}
				}
				catch (Exception | Error e)
				{
					e.printStackTrace();
					Assert.fail(e.getMessage());
				}
			}
			
			
			
			totalCompressedFileSizeInBytes = compressedRepo.getTotalDataSizeInBytes();
			
			avgCompressedDtoSizeInBytes /= processedDtoCount;
			avgUncompressedDtoSizeInBytes /= processedDtoCount;
			
			
			double compressionRatio = (totalCompressedFileSizeInBytes / (double) totalUncompressedFileSizeInBytes);
			String compressionRatioString = compressionRatio + "";
			compressionRatioString = compressionRatioString.substring(0, Math.min(6, compressionRatioString.length()));
			
			
			System.out.println("\n");
			System.out.println("Results: " + compressorName);
			System.out.println();
			System.out.println("Total uncompressed data: [" + StringUtil.convertBytesToHumanReadable(totalUncompressedFileSizeInBytes) + "] Total compressed data: [" + StringUtil.convertBytesToHumanReadable(totalCompressedFileSizeInBytes) + "]. Compression ratio: [" + compressionRatioString + "].");
			System.out.println("Min uncompressed data: [" + StringUtil.convertBytesToHumanReadable(minUncompressedDtoSizeInBytes) + "] Min compressed data: [" + StringUtil.convertBytesToHumanReadable(minCompressedDtoSizeInBytes) + "].");
			System.out.println("Max uncompressed data: [" + StringUtil.convertBytesToHumanReadable(maxUncompressedDtoSizeInBytes) + "] Max compressed data: [" + StringUtil.convertBytesToHumanReadable(maxCompressedDtoSizeInBytes) + "].");
			System.out.println("Avg uncompressed data: [" + StringUtil.convertBytesToHumanReadable(avgUncompressedDtoSizeInBytes) + "] Avg compressed data: [" + StringUtil.convertBytesToHumanReadable(avgCompressedDtoSizeInBytes) + "].");
			System.out.println();
			System.out.println("Total read time in MS: [" + totalReadTimeInNano / 1_000_000.0 + "] Average read time per dto: [" + (totalReadTimeInNano / processedDtoCount) / 1_000_000.0 + "]");
			System.out.println("Total write time in MS: [" + totalWriteTimeInNano / 1_000_000.0 + "] Average write time per dto: [" + (totalWriteTimeInNano / processedDtoCount) / 1_000_000.0 + "]");
			System.out.println();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		finally
		{
			if(uncompressedRepo != null)
			{
				uncompressedRepo.close();
			}
			
			if(compressedRepo != null)
			{
				compressedRepo.close();
			}
		}
	}
	
	
	
}
