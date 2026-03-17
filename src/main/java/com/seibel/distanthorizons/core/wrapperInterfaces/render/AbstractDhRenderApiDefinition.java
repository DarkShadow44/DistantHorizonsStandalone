package com.seibel.distanthorizons.core.wrapperInterfaces.render;

import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.render.renderer.AbstractDebugWireframeRenderer;
import com.seibel.distanthorizons.core.wrapperInterfaces.render.objects.IDhGenericObjectVertexBufferContainer;
import com.seibel.distanthorizons.core.wrapperInterfaces.render.objects.ILodContainerUniformBufferWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.render.objects.IVertexBufferWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.render.renderPass.*;
import com.seibel.distanthorizons.coreapi.interfaces.dependencyInjection.IBindable;

public abstract class AbstractDhRenderApiDefinition implements IBindable
{
	//=========//
	// getters //
	//=========//
	//region
	
	/** Used for debugging */
	public abstract String getApiName();
	
	//endregion
	
	
	
	//============//
	// singletons //
	//============//
	//region
	
	public abstract IDhMetaRenderer getMetaRenderer();
	public abstract IDhTerrainRenderer getTerrainRenderer();
	public abstract IDhSsaoRenderer getSsaoRenderer();
	public abstract IDhFogRenderer getFogRenderer();
	public abstract IDhFarFadeRenderer getFarFadeRenderer();
	public abstract AbstractDebugWireframeRenderer getDebugWireframeRenderer();
	public abstract IDhVanillaFadeRenderer getVanillaFadeRenderer();
	public abstract IDhTestTriangleRenderer getTestTriangleRenderer();
	
	public void bindRenderers()
	{
		SingletonInjector.INSTANCE.bind(AbstractDhRenderApiDefinition.class, this);
		
		SingletonInjector.INSTANCE.bind(IDhMetaRenderer.class, this.getMetaRenderer());
		SingletonInjector.INSTANCE.bind(IDhTerrainRenderer.class, this.getTerrainRenderer());
		SingletonInjector.INSTANCE.bind(IDhSsaoRenderer.class, this.getSsaoRenderer());
		SingletonInjector.INSTANCE.bind(IDhFogRenderer.class, this.getFogRenderer());
		SingletonInjector.INSTANCE.bind(IDhFarFadeRenderer.class, this.getFarFadeRenderer());
		SingletonInjector.INSTANCE.bind(AbstractDebugWireframeRenderer.class, this.getDebugWireframeRenderer());
		SingletonInjector.INSTANCE.bind(IDhVanillaFadeRenderer.class, this.getVanillaFadeRenderer());
		SingletonInjector.INSTANCE.bind(IDhTestTriangleRenderer.class, this.getTestTriangleRenderer());
	}
	
	//endregion
	
	
	
	//===========//
	// factories //
	//===========//
	//region
	
	// these methods are used by WrapperFactory
	
	/** 
	 * Generic renderers are created for each level they're used in
	 * so we can't just define a single instance.
	 */
	public abstract IDhGenericRenderer createGenericRenderer();
	
	public abstract IVertexBufferWrapper createVboWrapper(String name);
	public abstract ILodContainerUniformBufferWrapper createLodContainerUniformWrapper();
	public abstract IDhGenericObjectVertexBufferContainer createGenericVboContainer();
	
	//endregion
	
	
	
}
