package com.seibel.distanthorizons;

import com.seibel.distanthorizons.common.wrappers.McObjectConverter;
import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.util.math.Mat4f;
import com.seibel.distanthorizons.forge.ForgeMain;
import com.seibel.distanthorizons.interfaces.IMixinMinecraft;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;

import java.nio.FloatBuffer;

public class RenderHelper {
    public static void drawLods()
    {
        ClientApi.RENDER_STATE.mcModelViewMatrix = getModelViewMatrix();
        ClientApi.RENDER_STATE.mcProjectionMatrix = getProjectionMatrix();
        ClientApi.RENDER_STATE.clientLevelWrapper = ClientLevelWrapper.getWrapper(Minecraft.getMinecraft().theWorld);

        if (ForgeMain.angelicaCompat == null) {
            GL32.glDisable(GL32.GL_ALPHA_TEST);
        }
        GL11.glClearColor(1, 1, 1, 0.0F);
        int oldActiveTex = GL11.glGetInteger(GL32.GL_ACTIVE_TEXTURE);
        int oldBoundTex = GL11.glGetInteger(GL32.GL_TEXTURE_BINDING_2D);
        ClientApi.INSTANCE.renderLods();
        GL32.glDepthFunc(GL32.GL_LEQUAL);
        if (ForgeMain.angelicaCompat == null) {
            GL32.glEnable(GL32.GL_ALPHA_TEST);
        }
        GL32.glDisable(GL32.GL_BLEND);
        GL32.glActiveTexture(oldActiveTex);
        GL32.glBindTexture(GL32.GL_TEXTURE_2D, oldBoundTex);
    }

    public static void beforeWater()
    {
        GL11.glDepthMask(true);
    }

    public static void drawLodsFade(boolean translucent)
    {
        if (ForgeMain.angelicaCompat != null) {
            if (!ForgeMain.angelicaCompat.canDoFadeShader()) {
                return;
            }
        }
        ClientApi.RENDER_STATE.mcModelViewMatrix = getModelViewMatrix();
        ClientApi.RENDER_STATE.mcProjectionMatrix = getProjectionMatrix();
        ClientApi.RENDER_STATE.clientLevelWrapper = ClientLevelWrapper.getWrapper(Minecraft.getMinecraft().theWorld);

        if (ForgeMain.angelicaCompat == null) {
            GL32.glDisable(GL32.GL_ALPHA_TEST);
        }
        if (translucent) {
            ClientApi.INSTANCE.renderDeferredLodsForShaders();
            ClientApi.INSTANCE.renderFadeTransparent();
        } else {
            ClientApi.INSTANCE.renderFadeOpaque();
        }
        if (ForgeMain.angelicaCompat == null) {
            GL32.glEnable(GL32.GL_ALPHA_TEST);
        }

        GL32.glDepthFunc(GL32.GL_LEQUAL);
        GL32.glDisable(GL32.GL_BLEND);
    }

    private static Matrix4f modelViewMatrix;
    private static Matrix4f projectionMatrix;

    public static Matrix4f getModelViewMatrixMC() {
        return new Matrix4f(modelViewMatrix);
    }

    public static Matrix4f getProjectionMatrixMC() {
        return new Matrix4f(projectionMatrix);
    }

    public static Mat4f getModelViewMatrix() {
        return McObjectConverter.Convert(modelViewMatrix);
    }

    public static Mat4f getProjectionMatrix() {
       return McObjectConverter.Convert(projectionMatrix);
    }

    public static void setModelViewMatrix(FloatBuffer modelview) {
        modelViewMatrix = new Matrix4f(modelview);
    }

    public static void setProjectionMatrix(FloatBuffer projection) {
        projectionMatrix = new Matrix4f(projection);
    }

    public static void HelpTess() {
        GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
    }

    public static void enableFog() {
        GL11.glEnable(GL11.GL_FOG);
    }

    public static void disableFog() {
        if (!Config.Client.quickEnableRendering.get()) {
            return;
        }
        GL11.glDisable(GL11.GL_FOG);

        // Extremely high values cause issues, but 15 mebimeters out should be practically infinite
        // For Angelica
        GL11.glFogf(GL11.GL_FOG_START, 1024 * 1024 * 15);
        GL11.glFogf(GL11.GL_FOG_END, 1024 * 1024 * 16);
    }

    public static void glEnable(int cap) {
        if (Config.Client.quickEnableRendering.get() && cap == GL11.GL_FOG) {
            return;
        }
        GL11.glEnable(cap);
    }
}
