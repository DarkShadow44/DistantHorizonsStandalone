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

package com.seibel.distanthorizons.api.methods.events.abstractEvents;

import com.seibel.distanthorizons.api.methods.events.interfaces.IDhApiEvent;
import com.seibel.distanthorizons.api.methods.events.interfaces.IDhApiEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;

/**
 * Fires right before Distant Horizons clears all
 * its cached render data.
 * This includes: textures, renderable vertex data,
 * and potentially more.
 *
 * @author James Seibel
 * @version 2026-10-01
 * @since API 7.2.0
 */
public abstract class DhApiEBeforeRenderDataCacheClearEvent implements IDhApiEvent<DhApiEBeforeRenderDataCacheClearEvent.EventParam>
{
	/** Fires right before Distant Horizons clears all cached render data. */
	public abstract void onRenderCacheCleared(DhApiEventParam<DhApiEBeforeRenderDataCacheClearEvent.EventParam> event);
	
	
	//=========================//
	// internal DH API methods //
	//=========================//
	
	@Override
	public final void fireEvent(DhApiEventParam<DhApiEBeforeRenderDataCacheClearEvent.EventParam> event) { this.onRenderCacheCleared(event); }
	
	
	//==================//
	// parameter object //
	//==================//
	
	public static class EventParam implements IDhApiEventParam
	{
		public EventParam() { }
		
		
		@Override
		public DhApiEBeforeRenderDataCacheClearEvent.EventParam copy() { return new DhApiEBeforeRenderDataCacheClearEvent.EventParam(); }
	}
	
	
	
}