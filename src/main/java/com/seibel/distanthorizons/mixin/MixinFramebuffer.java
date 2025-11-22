package com.seibel.distanthorizons.mixin;

import com.seibel.distanthorizons.interfaces.IMixinFramebuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

@Mixin(Framebuffer.class)
public class MixinFramebuffer implements IMixinFramebuffer {
    @Shadow
    public int framebufferTextureWidth;

    @Shadow
    public int framebufferTextureHeight;

    @Shadow
    public int framebufferObject;

    @Shadow
    public int depthBuffer;

    @Unique
    private int distanthorizons$fbo;

    @Unique
    int distanthorizons$depthTexture;

    @Unique
    int distanthorizons$colorDummy;

    @Inject(method = "createFramebuffer", at = @At("TAIL"))
    private void createDepthTexture(int p_147605_1_, int p_147605_2_, CallbackInfo ci) {
        distanthorizons$fbo = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, distanthorizons$fbo);

        distanthorizons$depthTexture = TextureUtil.glGenTextures();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, distanthorizons$depthTexture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_DEPTH_COMPONENT24, framebufferTextureWidth, framebufferTextureHeight, 0, GL30.GL_DEPTH_COMPONENT, GL30.GL_FLOAT, (ByteBuffer) null);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE);

        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, distanthorizons$depthTexture, 0);

        distanthorizons$colorDummy = TextureUtil.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, distanthorizons$colorDummy);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, framebufferTextureWidth, framebufferTextureHeight, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, distanthorizons$colorDummy, 0);


        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Offscreen FBO incomplete: " + GL11.glGetString(status));
        }

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    @Inject(method = "deleteFramebuffer", at = @At("TAIL"))
    private void deleteDepthTexture(CallbackInfo ci) {
        GL11.glDeleteTextures(distanthorizons$depthTexture);
        GL11.glDeleteTextures(distanthorizons$colorDummy);
        GL30.glDeleteFramebuffers(distanthorizons$fbo);
    }

    @Override
    public void distanthorizons$BlitDepthToTexture() {
        GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
        GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, framebufferObject);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, distanthorizons$fbo);

        GL30.glBlitFramebuffer(
            0, 0, framebufferTextureWidth, framebufferTextureHeight,
            0, 0, framebufferTextureWidth, framebufferTextureHeight,
            GL30.GL_DEPTH_BUFFER_BIT,
            GL11.GL_NEAREST
        );
    }

    @Override
    public int distanthorizons$getDepthTexture() {
        return distanthorizons$depthTexture;
    }
}
