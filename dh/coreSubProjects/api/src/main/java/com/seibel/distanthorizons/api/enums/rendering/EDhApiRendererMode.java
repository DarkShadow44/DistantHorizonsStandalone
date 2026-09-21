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

package com.seibel.distanthorizons.api.enums.rendering;

/**
 * DEFAULT <br>
 * DEBUG_TRIANGLE <br>
 * DISABLED <br>
 *
 * @since API 2.0.0
 * @version 2026-03-23
 */
public enum EDhApiRendererMode
{
	DEFAULT,
	DEBUG_TRIANGLE,
	DISABLED;
	
	
	/** Used by the config GUI to cycle through the available rendering options */
	public static EDhApiRendererMode next(EDhApiRendererMode type)
	{
		switch (type)
		{
			case DEFAULT:
				return DEBUG_TRIANGLE;
			case DEBUG_TRIANGLE:
				return DISABLED;
			default:
				return DEFAULT;
		}
	}
	
	/** Used by the config GUI to cycle through the available rendering options */
	public static EDhApiRendererMode previous(EDhApiRendererMode type)
	{
		switch (type)
		{
			case DEFAULT:
				return DISABLED;
			case DEBUG_TRIANGLE:
				return DEFAULT;
			default:
				return DEBUG_TRIANGLE;
		}
	}
	
}
