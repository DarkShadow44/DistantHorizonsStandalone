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

package com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor;

import com.seibel.distanthorizons.api.enums.rendering.EDhApiFogDrawMode;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.logging.DhLogger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 * Contains any shared code between Optifine for Forge (official Optifine)
 * and Optifine on Fabric (unofficial ports).
 *
 * @author James Seibel
 * @version 2022-11-24
 */
public abstract class AbstractOptifineAccessor implements IOptifineAccessor
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	
	public Field ofFogField = null;
	public Object mcOptionsObject = null;
	
	
	
	//=======//
	// setup //
	//=======//
	
	public void finishDelayedSetup()
	{
		this.mcOptionsObject = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class).getOptionsObject();
		this.ofFogField = getOptifineFogField();
	}
	
	/** Returns null if Optifine isn't installed. */
	public static Field getOptifineFogField()
	{
		Object mcOptions = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class).getOptionsObject();
		
		// try and find the ofFogType variable in gameSettings
		for (Field field : mcOptions.getClass().getDeclaredFields())
		{
			if (field.getName().equals("ofFogType"))
			{
				return field;
			}
		}
		return null;
	}
	
	public static boolean optifinePresent() { return getOptifineFogField() != null; }
	
	
	
	//===================//
	// interface methods //
	//===================//
	
	
	public boolean getIsShaderActive()
	{
		try
		{
			// returns null if no shader, some string if shader active
			// IE "(internal)", "SEUS-v10.1-Standard.zip"
			String activeShaderName = (String) Class.forName("net.optifine.shaders.Shaders").getDeclaredMethod("getShaderPackName").invoke(null);
			return activeShaderName != null;
		}
		catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e)
		{
			return false;
		}
	}
	
}
