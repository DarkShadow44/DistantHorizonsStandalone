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

import com.seibel.distanthorizons.api.interfaces.block.IDhApiBiomeWrapper;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBlockStateWrapper;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.methods.events.interfaces.IDhApiCancelableEvent;
import com.seibel.distanthorizons.api.methods.events.interfaces.IDhApiEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiCancelableEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiFogRenderParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiMutableFogRenderParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.distanthorizons.api.objects.data.IDhApiFullDataSource;
import com.seibel.distanthorizons.coreapi.util.ColorUtil;

/**
 * Fired before DH renders its fog.
 * Canceling this event disables fog for that frame.
 * 
 * @author James Seibel
 * @version 2026-05-20
 * @since API 7.0.0
 */
public abstract class DhApiBeforeFogRenderEvent implements IDhApiCancelableEvent<DhApiBeforeFogRenderEvent.EventParam>
{
	/** Fired before fog is generated. */
	public abstract void beforeRender(DhApiCancelableEventParam<DhApiBeforeFogRenderEvent.EventParam> event);
	
	
	//=========================//
	// internal DH API methods //
	//=========================//
	
	@Override
	public final void fireEvent(DhApiCancelableEventParam<DhApiBeforeFogRenderEvent.EventParam> event) { this.beforeRender(event); }
	
	
	
	//==================//
	// parameter object //
	//==================//
	
	public static class EventParam implements IDhApiEventParam
	{
		private DhApiRenderParam renderParam;
		private DhApiFogRenderParam originalFogRenderParam;
		private DhApiMutableFogRenderParam fogRenderParam;
		
		
		
		//=============//
		// constructor //
		//=============//
		//region
		
		public EventParam() {}
		
		public void update(DhApiRenderParam renderParam, DhApiFogRenderParam fogRenderParam)
		{
			this.renderParam = renderParam;
			this.originalFogRenderParam = fogRenderParam;
			this.fogRenderParam = new DhApiMutableFogRenderParam(fogRenderParam);
		}
		
		//endregion
		
		
		
		//=================//
		// getters/setters //
		//=================//
		//region
		
		public DhApiRenderParam getRenderParam() { return this.renderParam; }
		
		/** immutable, stores what DH would do without API intervention so API users have a reference point */
		public DhApiFogRenderParam getOriginalFogRenderParam() { return this.originalFogRenderParam; }
		/** mutable, can be modified by API users */
		public DhApiMutableFogRenderParam getFogRenderParam() { return this.fogRenderParam; }
		
		//endregion
		
		
		
		//==========================//
		// base api event overrides //
		//==========================//
		//region
		
		/**
		 * Returns the same instance of this event.
		 * Copying this event isn't supported
		 * since the internal parameters must be mutated
		 * by API users in order to be tracked by DH's internal
		 * logic.
		 */
		@Override
		public DhApiBeforeFogRenderEvent.EventParam copy() { return this; }
		
		@Override
		public boolean getCopyBeforeFire() { return false; }
		
		//endregion
		
		
		
	}
	
	
}