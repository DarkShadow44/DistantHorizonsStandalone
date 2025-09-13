package com.seibel.distanthorizons.forge;

import com.gtnewhorizons.angelica.config.AngelicaConfig;
import net.coderbot.iris.rendertarget.IRenderTargetExt;
import net.minecraft.client.shader.Framebuffer;

public class AngelicaCompat {
    public int getDepthTextureId(Framebuffer framebuffer) {
        return ((IRenderTargetExt)framebuffer).getIris$depthTextureId();
    }

    public boolean canDoFadeShader() {
        return AngelicaConfig.enableIris;
    }
}
