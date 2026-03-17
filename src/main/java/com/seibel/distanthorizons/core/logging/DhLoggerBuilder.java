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

package com.seibel.distanthorizons.core.logging;

import com.seibel.distanthorizons.api.enums.config.EDhApiLoggerLevel;
import com.seibel.distanthorizons.core.config.listeners.IConfigListener;
import com.seibel.distanthorizons.core.config.types.AbstractConfigBase;
import com.seibel.distanthorizons.core.config.types.ConfigEntry;
import com.seibel.distanthorizons.coreapi.ModInfo;
import org.apache.logging.log4j.LogManager;
import com.seibel.distanthorizons.core.logging.DhLogger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @see DhLogger
 */
public class DhLoggerBuilder
{
	private String name;
	private @Nullable ConfigEntry<EDhApiLoggerLevel> chatLevelConfig;
	private @Nullable ConfigEntry<EDhApiLoggerLevel> fileLevelConfig;
	private int maxLogPerSec = -1;
	
	
	
	//=============//
	// constructor //
	//=============//
	
	public DhLoggerBuilder() { this.name = ModInfo.NAME + "-" + getCallingClassName(); }
	/** @return "??" if no name could be found */
	private static String getCallingClassName()
	{
		StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
		String callerClassName = "??";
		for (int i = 1; i < stElements.length; i++)
		{
			StackTraceElement ste = stElements[i];
			if (!ste.getClassName().equals(DhLoggerBuilder.class.getName())
					&& ste.getClassName().indexOf("java.lang.Thread") != 0)
			{
				callerClassName = ste.getClassName();
				break;
			}
		}
		
		return callerClassName;
	}
	
	
	
	//===========//
	// variables //
	//===========//
	
	public DhLoggerBuilder name(String name)
	{
		this.name = name;
		return this;
	}
	
	public DhLoggerBuilder chatLevelConfig(ConfigEntry<EDhApiLoggerLevel> chatLevelConfig)
	{
		this.chatLevelConfig = chatLevelConfig;
		return this;
	}
	
	public DhLoggerBuilder fileLevelConfig(ConfigEntry<EDhApiLoggerLevel> fileLevelConfig)
	{
		this.fileLevelConfig = fileLevelConfig;
		return this;
	}
	
	public DhLoggerBuilder maxCountPerSecond(int maxLogPerSec)
	{
		this.maxLogPerSec = maxLogPerSec;
		return this;
	}
	
	
	
	//=======//
	// build //
	//=======//
	
	public DhLogger build()
	{
		try
		{
			return new DhLogger(
					this.name,
					this.chatLevelConfig, this.fileLevelConfig,
					this.maxLogPerSec
			);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	
	
}
