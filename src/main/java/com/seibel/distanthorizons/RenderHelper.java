package com.seibel.distanthorizons;

import com.seibel.distanthorizons.common.wrappers.McObjectConverter;
import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.util.math.Mat4f;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.distanthorizons.interfaces.IMixinMinecraft;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL32;

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
    }

    public static Matrix4f getModelViewMatrixMC() {
        float[] buffer = new float[16];
        GL32.glGetFloatv(GL32.GL_MODELVIEW_MATRIX, buffer);
        return new Matrix4f(FloatBuffer.wrap(buffer));
    }

    public static Matrix4f getProjectionMatrixMC() {
        float[] buffer = new float[16];
        GL32.glGetFloatv(GL32.GL_PROJECTION_MATRIX, buffer);
        return new Matrix4f(FloatBuffer.wrap(buffer));
    }

    public static Mat4f getModelViewMatrix() {
        return McObjectConverter.Convert(getModelViewMatrixMC());
    }

    public static Mat4f getProjectionMatrix() {
       return McObjectConverter.Convert(getProjectionMatrixMC());
    }
}
