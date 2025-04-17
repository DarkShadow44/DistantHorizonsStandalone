package com.seibel.distanthorizons.mixin;

import com.seibel.distanthorizons.RenderHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderGlobal.class)
public class MixinRenderGlobal {
    @Inject(method = "sortAndRender", at = @At("HEAD"))
    void renderLods(EntityLivingBase p_72719_1_, int renderPass, double p_72719_3_, CallbackInfoReturnable<Integer> cir) {
        if (renderPass == 0) {
            RenderHelper.drawLods();
        }
    }
}
