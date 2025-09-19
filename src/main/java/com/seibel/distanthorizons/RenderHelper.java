package com.seibel.distanthorizons;

import com.seibel.distanthorizons.common.wrappers.McObjectConverter;
import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.util.math.Mat4f;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.distanthorizons.forge.ForgeMain;
import com.seibel.distanthorizons.interfaces.IMixinMinecraft;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class RenderHelper {
    public static void drawLods()
    {
        GL32.glDisable(GL32.GL_ALPHA_TEST);
        Mat4f mcModelViewMatrix = getModelViewMatrix();
        Mat4f mcProjectionMatrix = getProjectionMatrix();
        float frameTime = ((IMixinMinecraft) Minecraft.getMinecraft()).getTimer().renderPartialTicks;
        IClientLevelWrapper levelWrapper = ClientLevelWrapper.getWrapper(Minecraft.getMinecraft().theWorld);
        ClientApi.INSTANCE.renderLods(levelWrapper, mcModelViewMatrix, mcProjectionMatrix, frameTime);
        GL32.glDepthFunc(GL32.GL_LEQUAL);
        GL32.glEnable(GL32.GL_ALPHA_TEST);
        GL32.glDisable(GL32.GL_BLEND);
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
        GL32.glDisable(GL32.GL_ALPHA_TEST);
        Mat4f mcModelViewMatrix = getModelViewMatrix();
        Mat4f mcProjectionMatrix = getProjectionMatrix();
        float frameTime = ((IMixinMinecraft) Minecraft.getMinecraft()).getTimer().renderPartialTicks;
        IClientLevelWrapper levelWrapper = ClientLevelWrapper.getWrapper(Minecraft.getMinecraft().theWorld);
        //Minecraft.getMinecraft().getFramebuffer().unbindFramebuffer();
        if (translucent) {
            ClientApi.INSTANCE.renderFade(mcModelViewMatrix, mcProjectionMatrix, frameTime, levelWrapper);
        } else {
            ClientApi.INSTANCE.renderFadeOpaque(mcModelViewMatrix, mcProjectionMatrix, frameTime, levelWrapper);
        }
        //Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
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
}
