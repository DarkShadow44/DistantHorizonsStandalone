package com.seibel.distanthorizons.mixin;

import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.ByteBuffer;

@Mixin(Framebuffer.class)
public class MixinFramebuffer {
    @Shadow
    public int framebufferTextureWidth;

    @Shadow
    public int framebufferTextureHeight;

    @Redirect(method = "createFramebuffer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OpenGlHelper;func_153185_f()I"))
    private int createDepthTexture() {
        int depthTextureId = TextureUtil.glGenTextures();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTextureId);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT24, framebufferTextureWidth, framebufferTextureHeight, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (ByteBuffer)null);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);

        return depthTextureId;
    }

    @Redirect(method = "createFramebuffer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OpenGlHelper;func_153176_h(II)V"))
    private void bindDepthTexture(int frameBufferId, int depthTexture) {
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, depthTexture, 0);
    }

    @Redirect(method = "createFramebuffer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OpenGlHelper;func_153190_b(IIII)V"))
    private void oldBindStuff(int p_153190_0_, int p_153190_1_, int p_153190_2_, int p_153190_3_) {

    }

    @Redirect(method = "deleteFramebuffer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OpenGlHelper;func_153184_g(I)V"))
    private void deleteDepthTexture(int depthTexture) {
        GL11.glDeleteTextures(depthTexture);

    }
}
