package com.seibel.distanthorizons.common.render.openGl;

import com.seibel.distanthorizons.api.enums.config.EDhApiDepthDirection;
import com.seibel.distanthorizons.api.enums.config.EDhApiRenderingApi;
import com.seibel.distanthorizons.api.enums.config.EDhApiDepthRange;
import com.seibel.distanthorizons.api.interfaces.render.renderDef.*;
import com.seibel.distanthorizons.api.interfaces.render.renderDef.objects.IDhApiVertexBufferWrapper;
import com.seibel.distanthorizons.common.render.openGl.generic.GlGenericObjectRenderer;
import com.seibel.distanthorizons.common.render.openGl.generic.GlGenericObjectVertexContainer;
import com.seibel.distanthorizons.common.render.openGl.glObject.GlDummyUniformData;
import com.seibel.distanthorizons.common.render.openGl.glObject.buffer.GLVertexBuffer;
import com.seibel.distanthorizons.common.render.openGl.postProcessing.antialiasing.GlDhTaaRenderer;
import com.seibel.distanthorizons.common.render.openGl.postProcessing.fade.GlDhFarFadeRenderer;
import com.seibel.distanthorizons.common.render.openGl.postProcessing.fade.GlVanillaFadeRenderer;
import com.seibel.distanthorizons.common.render.openGl.postProcessing.fog.GlDhFogRenderer;
import com.seibel.distanthorizons.common.render.openGl.postProcessing.ssao.GlDhSSAORenderer;
import com.seibel.distanthorizons.common.render.openGl.test.GlTestTriangleRenderer;
import com.seibel.distanthorizons.core.dependencyInjection.ModAccessorInjector;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IIrisAccessor;
import com.seibel.distanthorizons.api.interfaces.render.renderDef.AbstractDhApiRenderDefinition;
import com.seibel.distanthorizons.api.interfaces.render.renderDef.objects.IDhApiGenericObjectVertexBufferContainer;
import com.seibel.distanthorizons.api.interfaces.render.renderDef.objects.IDhApiTerrainContainerUniformBufferWrapper;
import com.seibel.distanthorizons.coreapi.interfaces.dependencyInjection.IOverrideInjector;

public class GlDhRenderApiDefinition extends AbstractDhApiRenderDefinition
{
	private static final IIrisAccessor IRIS_ACCESSOR = ModAccessorInjector.INSTANCE.get(IIrisAccessor.class); 
	
	
	
	//=========//
	// getters //
	//=========//
	//region
	
	public String getName() { return "OpenGL"; }
	
	public EDhApiDepthDirection getDepthDirection() 
	{
		if (IRIS_ACCESSOR != null
			&& IRIS_ACCESSOR.isShaderPackInUse()
			&& !IRIS_ACCESSOR.isReverseZDuringShaders())
		{
			// reversed Z shouldn't be used when shaders are active
			// in order to maintain legacy behavior
			return EDhApiDepthDirection.FORWARD_Z; 
		}
		
		// reverse Z is better behavior going forward because it prevents
		// issues with clouds and other extremely far objects
		return EDhApiDepthDirection.REVERSE_Z;
	}
	
	public EDhApiDepthRange getDepthRange()
	{
		#if MC_VER <= MC_26_1_2
		return EDhApiDepthRange.NEG_ONE_TO_POS_ONE;
		#else
		// probably caused due to the starting changes to Vulkan
		return EDhApiDepthRange.ZERO_TO_POS_ONE;
		#endif
	}
	
	public EDhApiRenderingApi getRenderApi() { return EDhApiRenderingApi.OPEN_GL; }
	public boolean isNativeRenderer() { return true; }
	
	@Override
	public int getPriority() { return IOverrideInjector.CORE_PRIORITY; }
	
	//endregion
	
	
	
	//============//
	// singletons //
	//============//
	//region
	
	@Override public IDhApiMetaRenderer getMetaRenderer() { return GlDhMetaRenderer.INSTANCE; }
	@Override public IDhApiTerrainRenderer getTerrainRenderer() { return GlDhTerrainRenderer.INSTANCE; }
	@Override public IDhApiSsaoRenderer getSsaoRenderer() { return GlDhSSAORenderer.INSTANCE; }
	@Override public IDhApiFogRenderer getFogRenderer() { return GlDhFogRenderer.INSTANCE; }
	@Override public IDhApiFarFadeRenderer getFarFadeRenderer() { return GlDhFarFadeRenderer.INSTANCE; }
	@Override public IDhApiAntiAliasRenderer getAntiAliasRenderer() { return GlDhTaaRenderer.INSTANCE; }
	@Override public IDhApiDebugWireframeRenderer getDebugWireframeRenderer() { return GlDhDebugWireframeRenderer.INSTANCE; }
	@Override public IDhApiVanillaFadeRenderer getVanillaFadeRenderer() { return GlVanillaFadeRenderer.INSTANCE; }
	@Override public IDhApiTestTriangleRenderer getTestTriangleRenderer() { return GlTestTriangleRenderer.INSTANCE; }
	@Override public IDhApiGenericRenderer getGenericRenderer() { return GlGenericObjectRenderer.INSTANCE; }
	
	//endregion
	
	
	
	//===========//
	// factories //
	//===========//
	//region
	
	@Override public IDhApiVertexBufferWrapper createVboWrapper(String name) { return new GLVertexBuffer(); }
	@Override public IDhApiTerrainContainerUniformBufferWrapper createLodContainerUniformWrapper() { return new GlDummyUniformData(); }
	@Override public IDhApiGenericObjectVertexBufferContainer createGenericObjectVboContainer() { return new GlGenericObjectVertexContainer(); }
	
	//endregion
	
	
	
}
