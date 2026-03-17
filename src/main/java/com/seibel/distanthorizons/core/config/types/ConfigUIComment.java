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

package com.seibel.distanthorizons.core.config.types;

import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.config.types.enums.EConfigCommentTextPosition;
import com.seibel.distanthorizons.core.config.types.enums.EConfigEntryAppearance;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Adds something like a ConfigEntry but without a button to change the input
 *
 * @author coolGi
 */
public class ConfigUIComment extends AbstractConfigType<String, ConfigUIComment>
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	
	public String parentConfigPath = null;
	@Nullable
	public EConfigCommentTextPosition textPosition = null;
	
	
	
	public ConfigUIComment() { this(null, null); }
	public ConfigUIComment(String parentConfigPath, EConfigCommentTextPosition textPosition)
	{
		super(EConfigEntryAppearance.ONLY_IN_GUI, "");
		this.parentConfigPath = parentConfigPath;
		this.textPosition = textPosition;
	}
	
	
	
	/** Appearance shouldn't be changed */
	@Override
	public void setAppearance(EConfigEntryAppearance newAppearance) { }
	
	/** Pointless to set the value */
	@Override
	public void set(String newValue) { }
	
	
	
	public static class Builder extends AbstractConfigType.Builder<String, Builder>
	{
		public String tempParentConfigPath = null;
		public EConfigCommentTextPosition tempTextPosition = null;
		
		
		
		/** Appearance shouldn't be changed */
		@Deprecated
		@Override
		public Builder setAppearance(EConfigEntryAppearance newAppearance) { return this; }
		
		/** Pointless to set the value */
		@Deprecated
		@Override
		public Builder set(String newValue)
		{ return this; }
		
		
		public Builder setParentConfigClass(@NotNull Class<?> parentConfigClass)
		{
			// expected format: "Config.Client.Advanced"
			String packageName = parentConfigClass.getPackage().getName(); // com.seibel.distanthorizons.core.config
			String fullName = parentConfigClass.getName(); // com.seibel.distanthorizons.core.config.Config$Common$MultiThreading
			
			try
			{
				String configPath = fullName.substring(
						packageName.length() + // "com.seibel.distanthorizons.core.config"
								1 + // "." before "Config"
								Config.class.getSimpleName().length() + // "Config" 
								1); // "$" before the inner class name
				
				// configPath after substring:
				// Config$Common$MultiThreading
				
				this.tempParentConfigPath = convertPackageNameToLangPath(configPath); // client.advanced.graphics.Quality
			}
			catch (Exception e)
			{
				this.tempParentConfigPath = parentConfigClass.getSimpleName();
				LOGGER.warn("Failed to parse config class: ["+fullName+"], error: ["+e.getMessage()+"], defaulting to: ["+this.tempParentConfigPath+"].", e);
			}
			
			return this;
		}
		/** 
		 * example:
		 * input:  "Client$Advanced$multiThreading"
		 * output: "client.advanced.multiThreading"
		 */
		public static String convertPackageNameToLangPath(String input)
		{
			StringBuilder result = new StringBuilder(input.length());
			
			for (int i = 0; i < input.length(); i++)
			{
				char ch = input.charAt(i);
				if (i == 0)
				{
					result.append(Character.toLowerCase(ch));
					continue;
				}
				
				// replace '$' -> '.' to match lang path naming
				if (ch == '$')
				{
					result.append('.');
					continue;
				}
				
				char lastCh = input.charAt(i-1);
				if (lastCh == '$')
				{
					result.append(Character.toLowerCase(ch));
					continue;
				}
				
				result.append(ch);
			}
			return result.toString();
		}
		
		
		public Builder setTextPosition(EConfigCommentTextPosition textPosition)
		{
			this.tempTextPosition = textPosition;
			return this; 
		}
		
		
		
		public ConfigUIComment build()
		{ return new ConfigUIComment(this.tempParentConfigPath, this.tempTextPosition); }
		
	}
	
}
