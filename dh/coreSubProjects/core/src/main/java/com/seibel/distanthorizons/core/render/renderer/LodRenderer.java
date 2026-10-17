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

import com.seibel.distanthorizons.api.enums.rendering.EDhApiTransparency;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiCustomRenderRegister;
import com.seibel.distanthorizons.api.interfaces.render.renderDef.*;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.*;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dataObjects.render.bufferBuilding.LodBufferContainer;
import com.seibel.distanthorizons.core.dependencyInjection.ModAccessorInjector;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.render.DhApiRenderProxy;
import com.seibel.distanthorizons.core.render.RenderBufferHandler;
import com.seibel.distanthorizons.core.render.RenderParam;
import com.seibel.distanthorizons.core.util.objects.SortedArraySet;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IIrisAccessor;
import com.seibel.distanthorizons.coreapi.DependencyInjection.ApiEventInjector;

import java.awt.*;

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
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	private LodRenderer() { }
	
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
	public void render(RenderParam renderParams, IProfilerWrapper profiler)
	{  this.renderTerrain(renderParams, profiler, false);  }
	
	/**
	 * This method is designed for Iris to be able 
	 * to draw water in a deferred rendering context. 
	 * It needs to be updated with any major changes, 
	 * but shouldn't be activated as per deferWaterRendering.
	 */
	public void renderDeferred(RenderParam renderParams, IProfilerWrapper profiler)
	{ this.renderTerrain(renderParams, profiler, true); }
	
	private void renderTerrain(RenderParam renderParams, IProfilerWrapper profiler, boolean runningDeferredPass)
	{
		//===============//
		// validate pass //
		//===============//
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
		
		ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeRenderSetupEvent.class, renderParams.apiCopy);
		try (IProfilerWrapper.IProfileBlock terrainRender_profile = profiler.push("LOD GL setup")) // starts the new profile block for most DH rendering
		{
			AbstractDhApiRenderDefinition renderDefinition = renderParams.renderDefinition;
			IDhApiMetaRenderer metaRenderer = renderDefinition.getMetaRenderer();
			IDhApiTerrainRenderer terrainRenderer = renderDefinition.getTerrainRenderer();
			IDhApiSsaoRenderer ssaoRenderer = renderDefinition.getSsaoRenderer();
			IDhApiFogRenderer fogRenderer = renderDefinition.getFogRenderer();
			IDhApiFarFadeRenderer farFadeRenderer = renderDefinition.getFarFadeRenderer();
			IDhApiAntiAliasRenderer antiAliasRenderer = renderDefinition.getAntiAliasRenderer();
			IDhApiDebugWireframeRenderer debugWireframeRenderer = renderDefinition.getDebugWireframeRenderer();
			IDhApiGenericRenderer genericRenderer = renderDefinition.getGenericRenderer();
			
			
			RenderBufferHandler renderBufferHandler = renderParams.renderBufferHandler;
			IDhApiCustomRenderRegister genericRegister = renderParams.clientLevelWrapper.getRenderRegister();
			
			
			metaRenderer.runRenderPassSetup(renderParams);
			
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
			
			
			boolean renderFog;
			Boolean apiFogOverride = Config.Client.Advanced.Graphics.Fog.enableDhFog.getApiValue();
			if (apiFogOverride != null)
			{
				// use whatever the API dictates if set
				// (this could cause issues when underwater if a shader or something
				// doesn't add their own, but that's relatively unlikely)
				renderFog = apiFogOverride;
			}
			else
			{
				renderFog = Config.Client.Advanced.Graphics.Fog.enableDhFog.get();
				// allow enabling fog when: underwater fog, blind, etc.
				// otherwise LODs won't appear correctly
				renderFog |= renderParams.vanillaFogEnabled;
			}
			
			DhApiBeforeFogRenderEvent.EventParam fogRenderEventParam = FogRenderParamFactory.getRenderParam(renderParams);
			
			//endregion
			
			
			
			//===========//
			// rendering //
			//===========//
			
			if (!runningDeferredPass)
			{
				// needs to be fired after all the textures have been created/bound
				boolean clearTextures = !ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeTextureClearEvent.class, renderParams.apiCopy);
				if (clearTextures)
				{
					profiler.popPush("LOD Clear");
					metaRenderer.clearDhDepthAndColorTextures(renderParams);
				}
				
				
				
				//=========================//
				// opaque and non-deferred //
				// transparent rendering   //
				//=========================//
				
				// opaque LODs
				profiler.popPush("LOD Opaque");
				
				this.renderTerrain(terrainRenderer, renderBufferHandler, renderParams, /*opaquePass*/ true);
				
				// custom objects with SSAO
				if (Config.Client.Advanced.Graphics.GenericRendering.enableGenericRendering.get())
				{
					profiler.popPush("Custom Objects");
					genericRenderer.render(renderParams, genericRegister, true);
				}
				
				// SSAO
				if (Config.Client.Advanced.Graphics.enableSsao.get())
				{
					profiler.popPush("LOD SSAO");
					ssaoRenderer.render(renderParams);
				}
				
				// custom objects without SSAO
				if (Config.Client.Advanced.Graphics.GenericRendering.enableGenericRendering.get())
				{
					profiler.popPush("Custom Objects");
					genericRenderer.render(renderParams, genericRegister, false);
				}
				
				// combined pass transparent rendering
				if (!deferTransparentRendering
					&& Config.Client.Advanced.Graphics.Quality.transparency.get() == EDhApiTransparency.COMPLETE)
				{
					profiler.popPush("LOD Transparent");
					this.renderTerrain(terrainRenderer, renderBufferHandler, renderParams, /*opaquePass*/ false);
				}
				
				// fog
				boolean cancelFogEvent = ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeFogRenderEvent.class, fogRenderEventParam);
				if (renderFog
					&& !cancelFogEvent)
				{
					profiler.popPush("LOD Fog");
					
					fogRenderer.render(renderParams, fogRenderEventParam.getFogRenderParam());
				}
				
				// far plane clip fading
				if (IRIS_ACCESSOR == null
					|| !IRIS_ACCESSOR.isShaderPackInUse())
				{
					// far fading would probably break shaders
					// but is necessary for Anti-aliasing to apply to the sky/surrounding pixels properly
					
					profiler.popPush("Fade Far Clip Fade");
					farFadeRenderer.render(renderParams);
				}
				
				// Anti-Aliasing
				if (Config.Client.Advanced.Graphics.enableAntiAliasing.get()
					&&
					(
						// AA should be handled by the shader
						IRIS_ACCESSOR == null
						|| !IRIS_ACCESSOR.isShaderPackInUse()
					)
				)
				{
					profiler.popPush("Anti-Aliasing");
					antiAliasRenderer.render(renderParams);
				}
				
				
				
				//=================//
				// debug rendering //
				//=================//
				
				if (Config.Client.Advanced.Debugging.DebugWireframe.enableRendering.get())
				{
					profiler.popPush("Debug wireframes");
					
					// Note: this can be very slow if a lot of boxes are being rendered
					DebugWireframeHandler.INSTANCE.render(renderParams, debugWireframeRenderer);
				}
				
				
				if (Config.Client.Advanced.Debugging.PositionFinder.positionFinderEnable.get())
				{
					// can be used to find specific positions when debugging
					debugWireframeRenderer.renderBox(new DebugWireframeHandler.Box(
						DhSectionPos.encode(
							Config.Client.Advanced.Debugging.PositionFinder.positionFinderDetailLevel.get().byteValue(),
							Config.Client.Advanced.Debugging.PositionFinder.positionFinderXPos.get(),
							Config.Client.Advanced.Debugging.PositionFinder.positionFinderZPos.get()),
						Config.Client.Advanced.Debugging.PositionFinder.positionFinderMinBlockY.get(),
						Config.Client.Advanced.Debugging.PositionFinder.positionFinderMaxBlockY.get(),
						Config.Client.Advanced.Debugging.PositionFinder.positionFinderMarginPercent.get(),
						Color.GREEN
					));
				}
				
				
				
				//=========================//
				// Apply to the MC Texture //
				//=========================//
				
				boolean cancelApplyShader = ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeApplyShaderRenderEvent.class, renderParams.apiCopy);
				if (!cancelApplyShader)
				{
					profiler.popPush("Apply to MC");
					metaRenderer.copyToMcTexture(renderParams);
				}
				
			}
			else
			{
				//====================//
				// deferred rendering //
				//====================//
				
				if (Config.Client.Advanced.Graphics.Quality.transparency.get() == EDhApiTransparency.COMPLETE)
				{
					profiler.popPush("LOD Transparent");
					this.renderTerrain(terrainRenderer, renderBufferHandler, renderParams, /*opaquePass*/ false);
					
					
					boolean cancelFogEvent = ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeFogRenderEvent.class, fogRenderEventParam);
					if (renderFog
						&& !cancelFogEvent)
					{
						profiler.popPush("LOD Fog");
						
						fogRenderer.render(renderParams, fogRenderEventParam.getFogRenderParam());
					}
				}
			}
			
			
			
			//================//
			// render cleanup //
			//================//
			
			profiler.popPush("LOD cleanup");
			ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeRenderCleanupEvent.class, renderParams.apiCopy);
			
			metaRenderer.runRenderPassCleanup(renderParams);
		}
	}
	
	//endregion
	
	
	
	//===============//
	// LOD rendering //
	//===============//
	//region
	
	private void renderTerrain(IDhApiTerrainRenderer terrainRenderer, RenderBufferHandler lodBufferHandler, RenderParam renderEventParam, boolean opaquePass)
	{
		//===========//
		// rendering //
		//===========//
		
		SortedArraySet<LodBufferContainer> lodBufferContainer = lodBufferHandler.getColumnRenderBuffers();
		if (lodBufferContainer != null)
		{
			terrainRenderer.render(renderEventParam, opaquePass, lodBufferContainer);
		}
	}
	
	//endregion
	
	
	
}
