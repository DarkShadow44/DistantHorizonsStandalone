package com.seibel.distanthorizons.forge;

import com.gtnewhorizons.angelica.config.AngelicaConfig;
import com.gtnewhorizons.angelica.glsm.GLStateManager;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.versioning.ArtifactVersion;
import cpw.mods.fml.common.versioning.DefaultArtifactVersion;
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

    public void verifyAngelicaVersion()
    {
        ModContainer angelica = Loader.instance().getIndexedModList().get(ForgeMain.ANGELICA_MOD_ID);
        if (angelica == null)
        {
            throw new IllegalStateException("Angelica mod container could not be found.");
        }

        String installedVersion = angelica.getVersion();
        ArtifactVersion installedArtifactVersion = new DefaultArtifactVersion(installedVersion);
        if (ForgeMain.SUPPORTED_ANGELICA_RANGE.containsVersion(installedArtifactVersion))
        {
            return;
        }

        throw new AngelicaVersionGuiException(installedVersion, ForgeMain.MINIMUM_ANGELICA_VERSION);
    }
}
