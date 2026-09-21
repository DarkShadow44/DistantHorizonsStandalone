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
import com.seibel.distanthorizons.core.sql.dto.FullDataSourceV2DTO;
import com.seibel.distanthorizons.core.sql.dto.util.VarintUtil;
import com.seibel.distanthorizons.core.util.objects.dataStreams.DhDataInputStream;
import com.seibel.distanthorizons.core.util.objects.dataStreams.DhDataOutputStream;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class VarintTest
{
	
	@Test
	public void Test()
	{
		Assert.assertEquals(0x80, 128);
		
		
		// zig zag encoding is needed for varint handling, so test it first
		for (int i = -256; i < 256; i++)
		{
			testZigZagEncoding(i);
		}
		
		for (int i = -256; i < 256; i++)
		{
			testSingleVarint(i);
		}
	}
	
	private static void testZigZagEncoding(int value)
	{
		int encodedValue = VarintUtil.zigzagEncode(value);
		int decodedValue = VarintUtil.zigzagDecode(encodedValue);
		Assert.assertEquals(value, decodedValue);
	}
	
	private static void testSingleVarint(int value)
	{
		// write to stream
		ByteArrayList byteArrayList = new ByteArrayList();
		try (DhDataOutputStream outputStream = DhDataOutputStream.create(EDhApiDataCompressionMode.UNCOMPRESSED, byteArrayList))
		{
			int encodedValue = VarintUtil.zigzagEncode(value);
			VarintUtil.writeVarint(outputStream, encodedValue); // varint requires zig-zag encoding to function
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Assert.fail("Fail writing varint ["+value+"], error: ["+e.getMessage()+"]");
		}
		
		
		// read stream
		try (DhDataInputStream inputStream = DhDataInputStream.create(byteArrayList, EDhApiDataCompressionMode.UNCOMPRESSED, null /*no checkout needed for uncompressed*/))
		{
			int encodedValue = VarintUtil.readVarint(inputStream);
			int decodedValue = VarintUtil.zigzagDecode(encodedValue);
			Assert.assertEquals(value, decodedValue);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Assert.fail("Fail reading varint ["+value+"], error: ["+e.getMessage()+"]");
		}
	}
	
}
