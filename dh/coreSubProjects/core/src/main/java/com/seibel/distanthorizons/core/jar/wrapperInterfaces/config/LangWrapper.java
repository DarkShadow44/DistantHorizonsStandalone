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

package com.seibel.distanthorizons.core.jar.wrapperInterfaces.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.io.ParsingMode;
import com.electronwill.nightconfig.json.JsonFormat;
import com.seibel.distanthorizons.core.jar.JarUtils;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.config.ILangWrapper;
import com.seibel.distanthorizons.core.logging.DhLogger;

import java.util.Locale;

public class LangWrapper implements ILangWrapper
{
	public static final LangWrapper INSTANCE = new LangWrapper();
	
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	private static final Config JSON_OBJECT = Config.inMemory();
	
	
	
	public static void init()
	{
		try
		{
			// FIXME: Is there something in the config that the parser cant read?
			JsonFormat.fancyInstance().createParser().parse(
					JarUtils.convertInputStreamToString(JarUtils.accessFile("assets/lod/lang/" + Locale.getDefault().toString().toLowerCase() + ".json")),
					JSON_OBJECT, ParsingMode.REPLACE
			);
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to read lang file, error: ["+e.getMessage()+"]", e);
		}
	}
	
	@Override
	public boolean langExists(String str) { return JSON_OBJECT.get(str) != null; }
	
	@Override
	public String getLang(String str)
	{
		if (JSON_OBJECT.get(str) != null)
		{
			return (String) JSON_OBJECT.get(str);
		}
		else
		{
			return str;
		}
	}
	
	
	
}
