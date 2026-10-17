package com.seibel.distanthorizons.api.interfaces.render.renderDef;

import com.seibel.distanthorizons.api.enums.config.EDhApiDepthDirection;
import com.seibel.distanthorizons.api.enums.config.EDhApiDepthRange;
import com.seibel.distanthorizons.api.enums.config.EDhApiRenderingApi;
import com.seibel.distanthorizons.api.interfaces.override.IDhApiOverrideable;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiRenderProxy;
import com.seibel.distanthorizons.api.interfaces.render.renderDef.objects.IDhApiVertexBufferWrapper;
import com.seibel.distanthorizons.api.interfaces.render.renderDef.objects.IDhApiGenericObjectVertexBufferContainer;
import com.seibel.distanthorizons.api.interfaces.render.renderDef.objects.IDhApiTerrainContainerUniformBufferWrapper;
import com.seibel.distanthorizons.coreapi.DependencyInjection.OverrideInjector;

/**
 * Encapsulates all the necessary objects and methods needed by
 * Distant Horizons rendering and GPU interaction. <br><br>
 * 
 * Should be set via {@link IDhApiRenderProxy#setRenderDefinitionOverrideAsync(AbstractDhApiRenderDefinition, String)}
 * attempting to bind directly via the {@link OverrideInjector#bind(Class, IDhApiOverrideable)} will cause
 * crashing due to render data being in the wrong format/class for the new renderer.
 * 
 * @version 2026-10-02
 * @since API 7.3.0
 * @see IDhApiRenderProxy#setRenderDefinitionOverrideAsync(AbstractDhApiRenderDefinition, String) 
 * @see IDhApiRenderProxy#clearRenderDefinitionOverrideAsync() 
 */
public abstract class AbstractDhApiRenderDefinition implements IDhApiOverrideable
{
	//=========//
	// getters //
	//=========//
	//region
	
	/** Shown in the log and F3 screen to track which renderer is active */
	public abstract String getName();
	
	public abstract EDhApiDepthDirection getDepthDirection();
	public abstract EDhApiDepthRange getDepthRange();
	public abstract EDhApiRenderingApi getRenderApi();
	/** 
	 * Returns true if the current renderer
	 * is calling the base rendering API's method calls. <br>
	 * ie <code>GL.drawArrays()</code> for OpenGL. <Br><br>
	 *
	 * If DH is using Blaze3D (Mojang's rendering API) 
	 * this will return false.
	 */
	public abstract boolean isNativeRenderer();
	
	//endregion
	
	
	
	//============//
	// singletons //
	//============//
	//region
	
	public abstract IDhApiMetaRenderer getMetaRenderer();
	public abstract IDhApiTerrainRenderer getTerrainRenderer();
	public abstract IDhApiSsaoRenderer getSsaoRenderer();
	public abstract IDhApiFogRenderer getFogRenderer();
	public abstract IDhApiFarFadeRenderer getFarFadeRenderer();
	public abstract IDhApiAntiAliasRenderer getAntiAliasRenderer();
	public abstract IDhApiDebugWireframeRenderer getDebugWireframeRenderer();
	public abstract IDhApiVanillaFadeRenderer getVanillaFadeRenderer();
	public abstract IDhApiTestTriangleRenderer getTestTriangleRenderer();
	public abstract IDhApiGenericRenderer getGenericRenderer();
	
	//endregion
	
	
	
	//===========//
	// factories //
	//===========//
	//region
	
	public abstract IDhApiVertexBufferWrapper createVboWrapper(String name);
	public abstract IDhApiTerrainContainerUniformBufferWrapper createLodContainerUniformWrapper();
	public abstract IDhApiGenericObjectVertexBufferContainer createGenericObjectVboContainer();
	
	//endregion
	
	
	
}
