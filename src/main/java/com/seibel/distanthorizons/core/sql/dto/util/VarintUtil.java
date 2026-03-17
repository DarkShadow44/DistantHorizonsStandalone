package com.seibel.distanthorizons.core.sql.dto.util;

import com.seibel.distanthorizons.core.sql.dto.FullDataSourceV2DTO;
import com.seibel.distanthorizons.core.util.objects.dataStreams.DhDataInputStream;
import com.seibel.distanthorizons.core.util.objects.dataStreams.DhDataOutputStream;

import java.io.IOException;

public class VarintUtil
{
	
	/**
	 * zigzagEncode maps 0=>0, -1=>1, 1=>2, -2=>3, 3=>4, etc.
	 * this helps encode small magnitude signed numbers as small varints.
	 * https://lemire.me/blog/2022/11/25/making-all-your-integers-positive-with-zigzag-encoding/
	 */
	public static int zigzagEncode(int n)
	{
		// if n is (byte)-1, this results in:
		// 0b1111_1110 ^ 0b1111_1111 == 0b0000_0001
		return (n << 1) ^ (n >> 31);
	}
	
	public static int zigzagDecode(int n)
	{ return (n >>> 1) ^ -(n & 1); }
	
	
	
	/**
	 * @param value should be a zigzag encoded value 
	 *          created via {@link VarintUtil#zigzagEncode(int)}
	 */
	public static void writeVarint(DhDataOutputStream out, int value) throws IOException
	{
		if (value < 0)
		{
			throw new IllegalArgumentException("varint given ["+value+"], varint only accepts positive values.");
		}
		
		while (value >= 128)
		{
			out.writeByte(value | 128);
			value >>>= 7; // 128 = 2^7
		}
		out.writeByte(value);
	}
	
	public static int readVarint(DhDataInputStream in) throws IOException
	{
		int value = 0;
		int shift = 0;
		byte b;
		do
		{
			if (shift >= 32)
			{
				throw new IOException("invalid varint");
			}
			b = in.readByte();
			value |= (b & 127) << shift;
			shift += 7;
		}
		while ((b & 128) != 0);
		return value;
	}
	
	
	
}
