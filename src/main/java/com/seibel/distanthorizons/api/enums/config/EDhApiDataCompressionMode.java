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

package com.seibel.distanthorizons.api.enums.config;

/**
 * UNCOMPRESSED <br>
 * LZ4 <br>
 * XZ <br><br>
 * 
 * Note: speed and compression ratios are examples
 * and should only be used for estimated comparisons.
 * 
 * @version 2024-3-16
 * @since API 2.0.0
 */
public enum EDhApiDataCompressionMode
{
	// Reminder:
	// when adding items up the API minor version
	// when removing items up the API major version
	
	/** 
	 * Should only be used internally and for unit testing. <br><br> 
	 * 
	 * Read Speed: 6.09 MS / DTO <br>
	 * Write Speed: 6.01 MS / DTO <br>
	 * Compression ratio: 1.0 <br>
	 */
	@DisallowSelectingViaConfigGui
	UNCOMPRESSED(0),
	
	/** 
	 * Extremely fast (often faster than uncompressed), but generally poor compression. <br><br> 
	 * 
	 * Read Speed: 3.25 MS / DTO <br>
	 * Write Speed: 5.99 MS / DTO <br>
	 * Compression ratio: 0.4513 <br>
	 */
	LZ4(1),
	
	/**
	 * Decent speed and good compression. <br><br> 
	 *
	 * Read Speed: 9.31 MS / DTO <br>
	 * Write Speed: 15.13 MS / DTO <br>
	 * Compression ratio: 0.2606 <br>
	 */
	Z_STD(2),
	
	
	/** 
	 * Extremely slow, but very good compression. <br><br> 
	 *
	 * Read Speed: 13.29 MS / DTO <br>
	 * Write Speed: 70.95 MS / DTO <br>
	 * Compression ratio: 0.2068 <br>
	 */
	LZMA2(3);
	
	
	
	/** More stable than using the ordinal of the enum */
	public final byte value;
	
	EDhApiDataCompressionMode(int value) { this.value = (byte) value; }
	
	
	/** @throws IllegalArgumentException if the value doesn't map to a value */
	public static EDhApiDataCompressionMode getFromValue(byte value) throws IllegalArgumentException
	{
		EDhApiDataCompressionMode[] enumList = EDhApiDataCompressionMode.values();
		for (int i = 0; i < enumList.length; i++)
		{
			if (enumList[i].value == value)
			{
				return enumList[i];
			}
		}
		
		throw new IllegalArgumentException("No compression mode with the value ["+value+"]");
	}
	
	
}
