package com.seibel.distanthorizons.mixin;

import com.seibel.distanthorizons.interfaces.IMixinEntityRenderer;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjglx.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer implements IMixinEntityRenderer {
    @Override
    @Accessor("lightmapTexture")
    public abstract DynamicTexture getLightmapTexture();

    @Inject(method = "setupFog", at = @At(value = "INVOKE", target = "Lcpw/mods/fml/common/eventhandler/EventBus;post(Lcpw/mods/fml/common/eventhandler/Event;)Z", shift = At.Shift.AFTER, remap = false))
    private void notFine$disableFog(int p_78468_1_, float p_78468_2_, CallbackInfo ci) {
        // Extremely high values cause issues, but 15 mebimeters out should be practically infinite
        GL11.glFogf(org.lwjgl.opengl.GL11.GL_FOG_START, 1024 * 1024 * 15);
        GL11.glFogf(GL11.GL_FOG_END, 1024 * 1024 * 16);
    }
}
