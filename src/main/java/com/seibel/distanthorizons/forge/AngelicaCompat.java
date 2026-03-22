package com.seibel.distanthorizons.forge;

import com.gtnewhorizons.angelica.config.AngelicaConfig;
import com.gtnewhorizons.angelica.glsm.GLStateManager;
import net.coderbot.iris.rendertarget.IRenderTargetExt;
import net.minecraft.client.shader.Framebuffer;
import org.joml.Vector3d;
import java.awt.Color;

public class AngelicaCompat {
    public int getDepthTextureId(Framebuffer framebuffer) {
        return ((IRenderTargetExt)framebuffer).iris$getDepthTextureId();
    }

    public boolean canDoFadeShader() {
        return AngelicaConfig.enableIris;
    }

    public Color getFogColor() {
        Vector3d color = GLStateManager.getFogColor();
        return new Color((float)color.x, (float)color.y, (float)color.z);
    }
}
