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

package com.seibel.distanthorizons.core.render.renderer;

import com.seibel.distanthorizons.api.methods.events.abstractEvents.*;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dataObjects.render.bufferBuilding.LodBufferContainer;
import com.seibel.distanthorizons.core.dependencyInjection.ModAccessorInjector;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.render.DhApiRenderProxy;
import com.seibel.distanthorizons.core.render.RenderBufferHandler;
import com.seibel.distanthorizons.core.render.RenderParams;
import com.seibel.distanthorizons.core.util.objects.SortedArraySet;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IIrisAccessor;
import com.seibel.distanthorizons.core.wrapperInterfaces.render.renderPass.*;
import com.seibel.distanthorizons.coreapi.DependencyInjection.ApiEventInjector;

/**
 * This is where all the magic happens. <br>
 * This is where LODs are draw to the world.
 */
public class LodRenderer
{
	public static final DhLogger LOGGER = new DhLoggerBuilder()
			.fileLevelConfig(Config.Common.Logging.logRendererEventToFile)
			.build();
	
	public static final DhLogger RATE_LIMITED_LOGGER = new DhLoggerBuilder()
			.fileLevelConfig(Config.Common.Logging.logRendererEventToFile)
			.maxCountPerSecond(4)
			.build();
	
	private static final IMinecraftClientWrapper MC = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	private static final IIrisAccessor IRIS_ACCESSOR = ModAccessorInjector.INSTANCE.get(IIrisAccessor.class);
	
	public static final LodRenderer INSTANCE = new LodRenderer();
	
	
	private boolean vanillaSettingsOverridden = false;
	private boolean renderersBound = false;
	
	private IDhMetaRenderer metaRenderer;
	private IDhTerrainRenderer terrainRenderer;
	private IDhSsaoRenderer ssaoRenderer;
	private IDhFogRenderer fogRenderer;
	private IDhFarFadeRenderer farFadeRenderer;
	private AbstractDebugWireframeRenderer debugWireframeRenderer;
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	private LodRenderer() { }
	
	private void bindRenderers()
	{
		this.metaRenderer = SingletonInjector.INSTANCE.get(IDhMetaRenderer.class);
		this.terrainRenderer = SingletonInjector.INSTANCE.get(IDhTerrainRenderer.class);
		this.ssaoRenderer = SingletonInjector.INSTANCE.get(IDhSsaoRenderer.class);
		this.fogRenderer = SingletonInjector.INSTANCE.get(IDhFogRenderer.class);
		this.farFadeRenderer = SingletonInjector.INSTANCE.get(IDhFarFadeRenderer.class);
		this.debugWireframeRenderer = SingletonInjector.INSTANCE.get(AbstractDebugWireframeRenderer.class);
	}
	
	//endregion
	
	
	
	//===========//
	// rendering //
	//===========//
	//region
	
	/**
	 * This will draw both opaque and transparent LODs if 
	 * {@link DhApiRenderProxy#getDeferTransparentRendering()} is false,
	 * otherwise it will only render opaque LODs.
	 */
	public void render(RenderParams renderParams, IProfilerWrapper profiler)
	{  this.renderLodPass(renderParams, profiler, false);  }
	
	/**
	 * This method is designed for Iris to be able 
	 * to draw water in a deferred rendering context. 
	 * It needs to be updated with any major changes, 
	 * but shouldn't be activated as per deferWaterRendering.
	 */
	public void renderDeferred(RenderParams renderParams, IProfilerWrapper profiler)
	{ this.renderLodPass(renderParams, profiler, true); }
	
	private void renderLodPass(RenderParams renderParams, IProfilerWrapper profiler, boolean runningDeferredPass)
	{
		//====================//
		// validate rendering //
		//====================//
		//region
		
		boolean deferTransparentRendering = DhApiRenderProxy.INSTANCE.getDeferTransparentRendering();
		if (runningDeferredPass 
			&& !deferTransparentRendering)
		{
			return;
		}
		boolean firstPass = !runningDeferredPass;
		
		// RenderParams parameter validation should be done before this
		if (!renderParams.hasBeenValidated)
		{
			throw new IllegalArgumentException("Render parameters validation");
		}
		
		//endregion
		
		
		
		//=================//
		// rendering setup //
		//=================//
		//region
		
		ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeRenderSetupEvent.class, renderParams);
		profiler.push("LOD GL setup");
		
		if (!this.renderersBound)
		{
			this.bindRenderers();
			this.renderersBound = true;
		}
		
		RenderBufferHandler renderBufferHandler = renderParams.renderBufferHandler;
		IDhGenericRenderer genericRenderer = renderParams.genericRenderer;
		
		
		this.metaRenderer.runRenderPassSetup(renderParams);
		
		if (!this.vanillaSettingsOverridden)
		{
			// only do this once, that way they can still be reverted if desired
			if (Config.Client.Advanced.Graphics.overrideVanillaGraphicsSettings.get())
			{
				LOGGER.info("Overriding vanilla MC settings to better fit Distant Horizons... This behavior can be disabled in the Distant Horizons config.");
				
				MC.disableVanillaClouds();
				MC.disableVanillaChunkFadeIn();
				MC.disableFabulousTransparency();
			}
			
			this.vanillaSettingsOverridden = true;
		}
		
		if (firstPass)
		{
			// we only need to sort/cull the LODs at the start of the frame
			profiler.popPush("LOD build render list");
			renderBufferHandler.buildRenderList(renderParams);
		}
		
		//endregion
		
		
		
		//===========//
		// rendering //
		//===========//
		
		if (!runningDeferredPass)
		{
			this.metaRenderer.clearDhDepthAndColorTextures(renderParams);
			
			
			
			//=========================//
			// opaque and non-deferred //
			// transparent rendering   //
			//=========================//
			
			// opaque LODs
			profiler.popPush("LOD Opaque");
			
			this.renderLodPass(this.terrainRenderer, renderBufferHandler, renderParams, /*opaquePass*/ true, profiler);
			
			// custom objects with SSAO
			if (Config.Client.Advanced.Graphics.GenericRendering.enableGenericRendering.get())
			{
				profiler.popPush("Custom Objects");
				genericRenderer.render(renderParams, profiler, true);
			}
			
			// SSAO
			if (Config.Client.Advanced.Graphics.Ssao.enableSsao.get())
			{
				profiler.popPush("LOD SSAO");
				this.ssaoRenderer.render(renderParams);
			}
			
			// custom objects without SSAO
			if (Config.Client.Advanced.Graphics.GenericRendering.enableGenericRendering.get())
			{
				profiler.popPush("Custom Objects");
				genericRenderer.render(renderParams, profiler, false);
			}
			
			// combined pass transparent rendering
			if (!deferTransparentRendering 
				&& Config.Client.Advanced.Graphics.Quality.transparency.get().transparencyEnabled)
			{
				profiler.popPush("LOD Transparent");
				this.renderLodPass(this.terrainRenderer, renderBufferHandler, renderParams, /*opaquePass*/ false, profiler);
			}
			
			// far plane clip fading
			if (Config.Client.Advanced.Graphics.Quality.dhFadeFarClipPlane.get()
				&& IRIS_ACCESSOR == null)
			{
				profiler.popPush("Fade Far Clip Fade");
				this.farFadeRenderer.render(renderParams);
			}
			
			// fog
			if (Config.Client.Advanced.Graphics.Fog.enableDhFog.get() 
				// this is done to fix issues with: underwater fog, blindness effect, etc.
				|| renderParams.vanillaFogEnabled)
			{
				profiler.popPush("LOD Fog");

				this.fogRenderer.render(renderParams);
			}
			
			
			
			//=================//
			// debug rendering //
			//=================//
			
			if (Config.Client.Advanced.Debugging.DebugWireframe.enableRendering.get())
			{
				profiler.popPush("Debug wireframes");

				// Note: this can be very slow if a lot of boxes are being rendered
				this.debugWireframeRenderer.render(renderParams);
			}
			
			
			
			//=============================//
			// Apply to the MC Framebuffer //
			//=============================//
			
			boolean cancelApplyShader = ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeApplyShaderRenderEvent.class, renderParams);
			if (!cancelApplyShader)
			{
				profiler.popPush("Apply to MC");
				this.metaRenderer.applyToMcTexture(renderParams);
			}
			
		}
		else
		{
			//====================//
			// deferred rendering //
			//====================//

			if (Config.Client.Advanced.Graphics.Quality.transparency.get().transparencyEnabled)
			{
				profiler.popPush("LOD Transparent");
				this.renderLodPass(this.terrainRenderer, renderBufferHandler, renderParams, /*opaquePass*/ false, profiler);


				if (Config.Client.Advanced.Graphics.Fog.enableDhFog.get()
					// this is done to fix issues with: underwater fog, blindness effect, etc.
					|| renderParams.vanillaFogEnabled)
				{
					profiler.popPush("LOD Fog");

					this.fogRenderer.render(renderParams);
				}
			}
		}
		
		
		
		//================//
		// render cleanup //
		//================//
		
		profiler.popPush("LOD cleanup");
		ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeRenderCleanupEvent.class, renderParams);
		
		this.metaRenderer.runRenderPassCleanup(renderParams);
		
		
		
		// end of internal LOD profiling
		profiler.pop();
	}
	
	//endregion
	
	
	
	//===============//
	// LOD rendering //
	//===============//
	//region
	
	private void renderLodPass(IDhTerrainRenderer lodRenderer, RenderBufferHandler lodBufferHandler, RenderParams renderEventParam, boolean opaquePass, IProfilerWrapper profilerWrapper)
	{
		//===========//
		// rendering //
		//===========//
		
		ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeRenderPassEvent.class, renderEventParam);
		
		SortedArraySet<LodBufferContainer> lodBufferContainer = lodBufferHandler.getColumnRenderBuffers();
		if (lodBufferContainer != null)
		{
			lodRenderer.render(renderEventParam, opaquePass, lodBufferContainer, profilerWrapper);
		}
	}
	
	//endregion
	
	
	
}
