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

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeRenderEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiCancelableEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.function.Supplier;

public abstract class AbstractImmersivePortalsAccessor implements IImmersivePortalsAccessor
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	private static MethodHandle isRenderingMethodHandle;
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	public AbstractImmersivePortalsAccessor() 
	{
		LOGGER.info("Immersive Portals detected: some DH features will be disabled or may only partially function.");
		
		BeforeRenderEvent event = new BeforeRenderEvent(this);
		DhApi.events.bind(DhApiBeforeRenderEvent.class, event); 
	}
	
	//endregion
	
	
	
	//=====================//
	// reflection handling //
	//=====================//
	//region
	
	private static Class<?> getPortalRenderingClass()
	{
		try
		{
			return Class.forName("qouteall.imm_ptl.core.render.context_management.PortalRendering");
		}
		catch (ClassNotFoundException first)
		{
			try
			{
				return Class.forName("com.qouteall.immersive_portals.render.context_management.PortalRendering"); // 1.16
			}
			catch (ClassNotFoundException second)
			{
				RuntimeException err = new RuntimeException(first);
				err.addSuppressed(second);
				throw err;
			}
		}
	}
	
	//endregion
	
	
	
	//===========//
	// overrides //
	//===========//
	//region
	
	@Override
	public String getModName() { return "Immersive Portals"; }
	
	@Override
	public boolean isRenderingPortal()
	{
		try
		{
			if (isRenderingMethodHandle == null)
			{
				isRenderingMethodHandle = MethodHandles.lookup().findStatic(
					getPortalRenderingClass(),
					"isRendering", MethodType.methodType(Boolean.TYPE)
				);
			}
			
			return (boolean) isRenderingMethodHandle.invoke();
		}
		catch (Throwable e)
		{
			throw new RuntimeException(e);
		}
	}
	
	//endregion
	
	
	
	//=======//
	// event //
	//=======//
	//region
	
	private static class BeforeRenderEvent extends DhApiBeforeRenderEvent
	{
		@NotNull
		private final IImmersivePortalsAccessor immersivePortals;
		
		
		public BeforeRenderEvent(@NotNull IImmersivePortalsAccessor portalAccessor) { this.immersivePortals = portalAccessor; }
		
		
		@Override
		public void beforeRender(DhApiCancelableEventParam<DhApiRenderParam> event)
		{
			// needed because otherwise DH doesn't render to the level anyway
			// and will probably render the level the player is currently in instead
			if (this.immersivePortals.isRenderingPortal())
			{
				event.cancelEvent();
			}
		}
		
	}
	
	//endregion
	
	
	
}
