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

import org.apache.logging.log4j.Level;

/**
 * ALL
 * DEBUG
 * INFO
 * WARN
 * ERROR
 * DISABLED
 * 
 * @since API 5.0.0
 * @version 2024-4-6
 */
public enum EDhApiLoggerLevel
{
	// ordered from most to least broad
	ALL(Level.ALL),
	DEBUG(Level.DEBUG),
	INFO(Level.INFO),
	WARN(Level.WARN),
	ERROR(Level.ERROR),
	DISABLED(Level.OFF),
	;
	
	public final Level level;
	
	EDhApiLoggerLevel(Level level)
	{ this.level = level; }
	
	
	
}
