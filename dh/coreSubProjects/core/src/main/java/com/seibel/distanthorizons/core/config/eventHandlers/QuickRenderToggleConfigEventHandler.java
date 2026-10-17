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

import com.seibel.distanthorizons.api.enums.rendering.EDhApiRendererMode;
import com.seibel.distanthorizons.core.config.listeners.ConfigChangeListener;
import com.seibel.distanthorizons.core.config.Config;

public class QuickRenderToggleConfigEventHandler
{
	public static QuickRenderToggleConfigEventHandler INSTANCE = new QuickRenderToggleConfigEventHandler();
	
	private final ConfigChangeListener<Boolean> quickRenderChangeListener;
	private final ConfigChangeListener<EDhApiRendererMode> rendererModeChangeListener;
	
	
	
	/** private since we only ever need one handler at a time */
	private QuickRenderToggleConfigEventHandler()
	{
		this.quickRenderChangeListener = new ConfigChangeListener<>(Config.Client.quickEnableRendering,
				(config) -> 
				{
					if (config.apiIsOverriding())
					{
						Boolean apiValue = config.getApiValue();
						if (apiValue == null)
						{
							apiValue = false;
						}
						
						EDhApiRendererMode newMode = apiValue
							? EDhApiRendererMode.DEFAULT
							: EDhApiRendererMode.DISABLED;
						Config.Client.Advanced.Debugging.rendererMode.setApiValueWithoutFiringEvents(newMode, config.getApiUser());
					}
					else
					{
						EDhApiRendererMode newMode = config.get()
							? EDhApiRendererMode.DEFAULT
							: EDhApiRendererMode.DISABLED;
						Config.Client.Advanced.Debugging.rendererMode.setWithoutFiringEvents(newMode);
						Config.Client.Advanced.Debugging.rendererMode.setApiValueWithoutFiringEvents(null, null);
					}
				});
		this.rendererModeChangeListener = new ConfigChangeListener<>(Config.Client.Advanced.Debugging.rendererMode,
				(config) -> 
				{
					if (config.apiIsOverriding())
					{
						EDhApiRendererMode apiValue = config.getApiValue();
						if (apiValue == null)
						{
							apiValue = EDhApiRendererMode.DEFAULT;
						}
						
						boolean newMode = apiValue == EDhApiRendererMode.DEFAULT;
						Config.Client.quickEnableRendering.setApiValueWithoutFiringEvents(newMode, config.getApiUser());
					}
					else
					{
						boolean newValue = Config.Client.Advanced.Debugging.rendererMode.get() != EDhApiRendererMode.DISABLED;
						Config.Client.quickEnableRendering.setWithoutFiringEvents(newValue);
						Config.Client.quickEnableRendering.setApiValueWithoutFiringEvents(null, null);
					}
				});
	}
	
	/**
	 * Set the UI only config based on what is set in the file. <br>
	 * This should only be called once.
	 */
	public void setUiOnlyConfigValues()
	{
		boolean enableRendering = Config.Client.Advanced.Debugging.rendererMode.get() != EDhApiRendererMode.DISABLED;
		Config.Client.quickEnableRendering.set(enableRendering);
	}
	
}
