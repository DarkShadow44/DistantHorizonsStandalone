package com.seibel.distanthorizons.common.render.blaze;

#if MC_VER <= MC_1_21_10
public class BlazeDhRenderApiDefinition {}

#else

import com.seibel.distanthorizons.api.enums.config.EDhApiDepthDirection;
import com.seibel.distanthorizons.api.enums.config.EDhApiDepthRange;
import com.seibel.distanthorizons.api.enums.config.EDhApiRenderingApi;
import com.seibel.distanthorizons.api.interfaces.render.renderDef.*;
import com.seibel.distanthorizons.api.interfaces.render.renderDef.objects.IDhApiVertexBufferWrapper;
import com.seibel.distanthorizons.common.render.blaze.objects.BlazeGenericObjectVertexContainer;
import com.seibel.distanthorizons.common.render.blaze.postProcessing.*;
import com.seibel.distanthorizons.common.render.blaze.test.BlazeDhTestTriangleRenderer;
import com.seibel.distanthorizons.common.render.blaze.wrappers.buffer.BlazeVertexBufferWrapper;
import com.seibel.distanthorizons.common.render.blaze.wrappers.uniform.BlazeLodUniformBufferWrapper;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftRenderWrapper;
import com.seibel.distanthorizons.api.interfaces.render.renderDef.AbstractDhApiRenderDefinition;
import com.seibel.distanthorizons.api.interfaces.render.renderDef.objects.IDhApiGenericObjectVertexBufferContainer;
import com.seibel.distanthorizons.api.interfaces.render.renderDef.objects.IDhApiTerrainContainerUniformBufferWrapper;
import com.seibel.distanthorizons.coreapi.interfaces.dependencyInjection.IOverrideInjector;

public class BlazeDhRenderApiDefinition extends AbstractDhApiRenderDefinition
{
	//=========//
	// getters //
	//=========//
	//region
	
	private final String engineName;
	public String getName() { return this.engineName; }
	
	@Override
	public EDhApiDepthDirection getDepthDirection()
	{
		#if MC_VER <= MC_26_1_2
		return EDhApiDepthDirection.FORWARD_Z;
		#else
		return EDhApiDepthDirection.REVERSE_Z;
		#endif
	}
	
	@Override
	public EDhApiDepthRange getDepthRange()
	{
		#if MC_VER <= MC_26_1_2
		return EDhApiDepthRange.NEG_ONE_TO_POS_ONE;
		#else
		// probably caused due to the starting changes to Vulkan
		return EDhApiDepthRange.ZERO_TO_POS_ONE;
		#endif
	}
	
	private final EDhApiRenderingApi renderApi;
	public EDhApiRenderingApi getRenderApi() { return this.renderApi; }
	public boolean isNativeRenderer() { return false; }
	
	@Override
	public int getPriority() { return IOverrideInjector.CORE_PRIORITY; }
	
	//endregion
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	public BlazeDhRenderApiDefinition()
	{
		#if MC_VER <= MC_26_1_2
		renderApi = EDhApiRenderingApi.OPEN_GL;
		#else
		// Blaze always uses the same rendering API as Minecraft
		this.renderApi = MinecraftRenderWrapper.INSTANCE.getMcRenderingApi();
		#endif
		
		this.engineName = "Blaze3D: " + this.getRenderApi();
	}
	
	//endregion
	
	
	
	//============//
	// singletons //
	//============//
	//region
	
	@Override public IDhApiMetaRenderer getMetaRenderer() { return BlazeDhMetaRenderer.INSTANCE; }
	@Override public IDhApiTerrainRenderer getTerrainRenderer() { return BlazeDhTerrainRenderer.INSTANCE; }
	@Override public IDhApiSsaoRenderer getSsaoRenderer() { return BlazeDhSsaoRenderer.INSTANCE; }
	@Override public IDhApiFogRenderer getFogRenderer() { return BlazeDhFogRenderer.INSTANCE; }
	@Override public IDhApiFarFadeRenderer getFarFadeRenderer() { return BlazeDhFarFadeRenderer.INSTANCE; }
	@Override public IDhApiAntiAliasRenderer getAntiAliasRenderer() { return BlazeDhTaaRenderer.INSTANCE; }
	@Override public IDhApiDebugWireframeRenderer getDebugWireframeRenderer() { return BlazeDebugWireframeRenderer.INSTANCE; }
	@Override public IDhApiVanillaFadeRenderer getVanillaFadeRenderer() { return BlazeVanillaFadeRenderer.INSTANCE; }
	@Override public IDhApiTestTriangleRenderer getTestTriangleRenderer() { return BlazeDhTestTriangleRenderer.INSTANCE; }
	@Override public IDhApiGenericRenderer getGenericRenderer() { return BlazeDhGenericObjectRenderer.INSTANCE; }
	
	//endregion
	
	
	
	//===========//
	// factories //
	//===========//
	//region
	
	@Override public IDhApiVertexBufferWrapper createVboWrapper(String name) { return new BlazeVertexBufferWrapper(name); }
	@Override public IDhApiTerrainContainerUniformBufferWrapper createLodContainerUniformWrapper() { return new BlazeLodUniformBufferWrapper(); }
	@Override public IDhApiGenericObjectVertexBufferContainer createGenericObjectVboContainer() { return new BlazeGenericObjectVertexContainer(); }
	
	//endregion
	
	
	
}
#endif