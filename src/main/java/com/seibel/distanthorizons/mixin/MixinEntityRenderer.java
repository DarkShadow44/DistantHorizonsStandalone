package com.seibel.distanthorizons.mixin;

import com.seibel.distanthorizons.interfaces.IMixinEntityRenderer;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventBus;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjglx.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer implements IMixinEntityRenderer {
    @Override
    @Accessor("lightmapTexture")
    public abstract DynamicTexture getLightmapTexture();

    @Redirect(method = "setupFog", at = @At(value = "INVOKE", target = "Lcpw/mods/fml/common/eventhandler/EventBus;post(Lcpw/mods/fml/common/eventhandler/Event;)Z", remap = false))
    private boolean notFine$disableFog(EventBus instance, Event event) {
        // Extremely high values cause issues, but 15 mebimeters out should be practically infinite
        GL11.glFogf(GL11.GL_FOG_START, 1024 * 1024 * 15);
        GL11.glFogf(GL11.GL_FOG_END, 1024 * 1024 * 16);
        GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_LINEAR);
        return false;
    }
}
