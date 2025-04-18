package com.seibel.distanthorizons.mixin;

import com.seibel.distanthorizons.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;
@Mixin(Tessellator.class)
public class MixinTesselator {
    @Inject(method = "draw", at = @At(value = "HEAD"))
    void drawMixin(CallbackInfoReturnable<Integer> cir) {
        RenderHelper.HelpTess();
    }
}
