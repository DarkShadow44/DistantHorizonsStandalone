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

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiRenderProxy;
import com.seibel.distanthorizons.core.config.listeners.IConfigListener;
import com.seibel.distanthorizons.core.util.TimerUtil;

import java.util.Timer;
import java.util.TimerTask;

public class ReloadLodsConfigEventHandler extends AbstractDelayedConfigEventHandler
{
	/** 
	 * should be used for user facing UI options
	 * this allows the user a second to click through options before they're applied
	 */
	public static ReloadLodsConfigEventHandler DELAYED_INSTANCE = new ReloadLodsConfigEventHandler(AbstractDelayedConfigEventHandler.DEFAULT_TIMEOUT_IN_MS);
	/** should be used for debug options so their change can be seen instantly */
	public static ReloadLodsConfigEventHandler INSTANT_INSTANCE = new ReloadLodsConfigEventHandler(0);
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	public ReloadLodsConfigEventHandler(long timeoutInMs) { super(timeoutInMs); }
	
	//endregion
	
	
	
	//========//
	// events //
	//========//
	//region
	
	@Override
	public void onConfigTimeout()
	{
		IDhApiRenderProxy renderProxy = DhApi.Delayed.renderProxy;
		if (renderProxy != null)
		{
			renderProxy.clearRenderDataCache();
		}
	}
	
	//endregion
	
	
	
}
