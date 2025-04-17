package com.seibel.distanthorizons.mixin;

import com.seibel.distanthorizons.RenderHelper;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.FloatBuffer;

@Mixin(ActiveRenderInfo.class)
public class MixinActiveRenderInfo {
    @Shadow
    private static FloatBuffer modelview;
    @Shadow private static FloatBuffer projection;
    @Inject(method = "updateRenderInfo", at = @At(value = "TAIL"))
    private static void asini$onUpdateRenderInfo(EntityPlayer p_74583_0_, boolean p_74583_1_, CallbackInfo ci) {
        RenderHelper.setProjectionMatrix(projection);
        RenderHelper.setModelViewMatrix(modelview);
    }
}
