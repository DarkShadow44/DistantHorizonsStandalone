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

package com.seibel.distanthorizons.core.config.eventHandlers;

import com.seibel.distanthorizons.api.enums.config.EDhApiRenderingApi;
import com.seibel.distanthorizons.api.enums.config.EDhApiRenderingEngine;
import com.seibel.distanthorizons.core.config.listeners.IConfigListener;
import com.seibel.distanthorizons.core.config.types.ConfigEntry;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.render.RenderThreadTaskHandler;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IDependencySetup;

public class RenderingEngineConfigListener implements IConfigListener, ConfigEntry.IShowEnumOptionFunc<EDhApiRenderingEngine>
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	private static final IDependencySetup DEPENDENCY_SETUP = SingletonInjector.INSTANCE.get(IDependencySetup.class);
private static final IMinecraftRenderWrapper MC_RENDER = SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class);
	
	public static final RenderingEngineConfigListener INSTANCE = new RenderingEngineConfigListener();
	
	
	
	//=============//
 	// constructor //
	//=============//
	
	/** private since we only ever need one handler at a time */
	private RenderingEngineConfigListener() { }
	
	
	
	//=================//
	// config handling //
	//=================//
	
	@Override
	public void onConfigValueSet()
	{
		RenderThreadTaskHandler.INSTANCE.queueRunningOnRenderThread("Config Render Engine Change", () -> 
		{
			try
			{
				DEPENDENCY_SETUP.setRenderingApiBindingsFromConfigAsync();
			}
			catch (Exception e)
			{
				LOGGER.warn(e.getMessage(), e);
			}
		});
	}
	
	@Override 
	public boolean shouldShowEnum(EDhApiRenderingEngine enumValue)
	{
		EDhApiRenderingApi renderApi = MC_RENDER.getMcRenderingApi();
		if (renderApi == EDhApiRenderingApi.VULKAN
			&& enumValue == EDhApiRenderingEngine.OPEN_GL)
		{
			return false;
		}
		
		return true;
	}
	
	
	
}
