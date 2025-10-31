package com.seibel.distanthorizons.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.seibel.distanthorizons.RenderHelper;
import com.seibel.distanthorizons.forge.ForgeMain;
import com.seibel.distanthorizons.interfaces.IMixinEntityRenderer;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventBus;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer implements IMixinEntityRenderer {
    @Override
    @Accessor("lightmapTexture")
    public abstract DynamicTexture getLightmapTexture();

    @Inject(method = "setupFog", at = @At(value = "HEAD"))
    private void enableFog(int p_78468_1_, float p_78468_2_, CallbackInfo ci) {
        RenderHelper.enableFog();
    }

    @Redirect(method = "setupFog", at = @At(value = "INVOKE", target = "Lcpw/mods/fml/common/eventhandler/EventBus;post(Lcpw/mods/fml/common/eventhandler/Event;)Z", remap = false, ordinal = 1))
    private boolean disableFog(EventBus instance, Event event, @Local(argsOnly = true) int p_78468_1_) {
        if (p_78468_1_ == -1)
        {
            return false;
        }
        RenderHelper.disableFog();
        return false;
    }

    @Redirect(method = "renderWorld", at=@At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glEnable(I)V", remap = false))
    private void disableFog2(int cap) {
        RenderHelper.glEnable(cap);
    }

    @Inject(method = "updateCameraAndRender", at = @At("HEAD"))
    private void updateLightmap(float partialTicks, CallbackInfo ci) {
        if (ForgeMain.rpleCompat != null) {
            ForgeMain.rpleCompat.updateLightmap();
        }
    }
}
