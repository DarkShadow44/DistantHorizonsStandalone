package com.seibel.distanthorizons.common.render.blaze;

#if MC_VER <= MC_1_21_10
public class BlazeDhMetaRenderer {}

#else

import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiAfterColorDepthTextureCreatedEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiTextureCreatedParam;
import com.seibel.distanthorizons.common.render.blaze.apply.BlazeDhCopyRenderer;
import com.seibel.distanthorizons.common.render.blaze.wrappers.texture.BlazeTextureViewWrapper;
import com.seibel.distanthorizons.common.render.blaze.wrappers.texture.BlazeTextureWrapper;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftRenderWrapper;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.render.DhApiRenderProxy;
import com.seibel.distanthorizons.coreapi.util.ColorUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.distanthorizons.api.interfaces.render.renderDef.IDhApiMetaRenderer;
import com.seibel.distanthorizons.coreapi.DependencyInjection.ApiEventInjector;

import java.awt.*;

public class BlazeDhMetaRenderer implements IDhApiMetaRenderer
{
	public static final BlazeDhMetaRenderer INSTANCE = new BlazeDhMetaRenderer();
	
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class);
	
	
	public final BlazeTextureWrapper dhDepthTextureWrapper = BlazeTextureWrapper.createDepth("DhDepthTexture");
	public final BlazeTextureWrapper dhColorTextureWrapper = BlazeTextureWrapper.createColor("DhColorTexture");
	public final BlazeTextureViewWrapper mcColorTextureWrapper = new BlazeTextureViewWrapper();
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	private BlazeDhMetaRenderer() { }
	
	//endregion
	
	
	
	//=================//
	// pre/post render //
	//=================//
	//region
	
	@Override
	public void runRenderPassSetup(DhApiRenderParam renderParams)
	{
		int oldWidth = this.dhDepthTextureWrapper.getWidth();
		int oldHeight = this.dhDepthTextureWrapper.getHeight();
		
		boolean texturesChanged = false;
		texturesChanged = this.dhDepthTextureWrapper.tryCreateOrResizeToScreenSize() | texturesChanged;
		texturesChanged = this.dhColorTextureWrapper.tryCreateOrResizeToScreenSize() | texturesChanged;
		
		DhApiRenderProxy.activeBlazeDhDepthTextureWrapper = this.dhDepthTextureWrapper;
		DhApiRenderProxy.activeBlazeDhColorTextureWrapper = this.dhColorTextureWrapper;
		
		if (texturesChanged)
		{
			int newTextureWidth = MC_RENDER.getTargetFramebufferViewportWidth();
			int newTextureHeight = MC_RENDER.getTargetFramebufferViewportHeight();
			
			DhApiTextureCreatedParam textureCreatedParam = new DhApiTextureCreatedParam(
				oldWidth, oldHeight,
				newTextureWidth, newTextureHeight
			);
			ApiEventInjector.INSTANCE.fireAllEvents(DhApiAfterColorDepthTextureCreatedEvent.class, textureCreatedParam);
		}
	}
	
	@Override
	public void runRenderPassCleanup(DhApiRenderParam renderParams) {}
	
	@Override
	public void copyToMcTexture(DhApiRenderParam renderParams)
	{
		this.mcColorTextureWrapper.tryWrap(MinecraftRenderWrapper.INSTANCE.getRenderTarget().getColorTexture());
		BlazeDhCopyRenderer.INSTANCE.render(this.dhColorTextureWrapper, this.mcColorTextureWrapper);
	}
	
	//endregion
	
	
	
	//================//
	// clear textures //
	//================//
	//region
	
	@Override
	public void clearDhDepthAndColorTextures(DhApiRenderParam renderParams)
	{
		this.dhDepthTextureWrapper.clearDepth(renderParams.renderDefinition.getDepthDirection().farDepth);
		
		Color color = MC_RENDER.getSkyColor();
		
		this.dhColorTextureWrapper.clearColor(
			// alpha of 0 done as a check to make sure DH is only applied to MC's framebuffer
			// where DH pixels were drawn
			ColorUtil.argbToInt(
				0,
				color.getRed(),
				color.getGreen(),
				color.getBlue()
			)
		);
	}
	
	//endregion
	
	
	
}
#endif