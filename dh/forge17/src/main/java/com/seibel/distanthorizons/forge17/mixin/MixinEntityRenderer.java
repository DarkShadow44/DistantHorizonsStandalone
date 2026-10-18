package com.seibel.distanthorizons.forge17.mixin;

import com.seibel.distanthorizons.core.dependencyInjection.ModAccessorInjector;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IRpleAccessor;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.seibel.distanthorizons.forge17.RenderHelper;
import com.seibel.distanthorizons.forge17.ForgeMain;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventBus;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer 
{
	//=====//
	// fog //
	//=====//
	//region
	
    @Inject(method = "setupFog", at = @At(value = "HEAD"))
    private void enableFog(int p_78468_1_, float p_78468_2_, CallbackInfo ci) 
    { RenderHelper.enableFog(); }

    @Redirect(
        method = "setupFog",
        at = @At(
            value = "INVOKE",
            target = "Lcpw/mods/fml/common/eventhandler/EventBus;post(Lcpw/mods/fml/common/eventhandler/Event;)Z",
            remap = false,
            ordinal = 1))
    private boolean disableFogDuringSetup(EventBus instance, Event event, @Local(argsOnly = true) int p_78468_1_)
    {
	    // p_78468_1_ fogMode: -1 = sky fog pass, >=0 = terrain fog pass
	    if (p_78468_1_ == -1)
	    {
		    return false;
	    }
		
	    RenderHelper.disableFogDuringSetup();
	    return false;
    }
	
	@Redirect(
		method = "renderWorld",
		at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glEnable(I)V", remap = false))
	private void disableFogDuringRender(int cap)
	{ RenderHelper.disableFogDuringRender(cap); }
	
	//endregion
	
	
	
	//==========//
	// lightmap //
	//==========//
	//region
	
	@Inject(method = "updateCameraAndRender", at = @At("HEAD"))
	private void updateLightmap(float partialTicks, CallbackInfo ci)
	{
		// Not cached in a static: EntityRenderer is created before FMLInitializationEvent
		// binds the accessor, so a static would stay null and the RPLE lightmap
		// handed to the LOD shader would never be filled (black LODs).
		IRpleAccessor rpleAccessor = ModAccessorInjector.INSTANCE.get(IRpleAccessor.class);
		if (rpleAccessor != null)
		{
			rpleAccessor.updateLightmap();
		}
	}
	
	//endregion
	
	
	
}
