package com.seibel.distanthorizons.forge112.mixins.client;

import com.seibel.distanthorizons.forge112.MixinFlags;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.IntBuffer;

import static com.seibel.distanthorizons.lwjgl.LWJGLServiceProvider.LWJGL;

@Mixin(Framebuffer.class)
public abstract class MixinFramebuffer
{
	@Shadow
	public int framebufferTextureWidth;
	@Shadow
	public int framebufferTextureHeight;
	
	@Shadow
	public boolean useDepth;
	
	@Shadow
	private boolean stencilEnabled;
	
	@Shadow
	public int framebufferObject;
	
	@Shadow
	public int depthBuffer;
	
	@Unique private int distantHorizons$oldDepthTextureToDelete = -1;
	
	//========================//
	// depth texture handling //
	//========================//
	//region
	
	
	@Inject(method = "createFramebuffer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/shader/Framebuffer;framebufferClear()V", shift = At.Shift.BEFORE, ordinal = 1), require = 1)
	private void createDepthTexture(CallbackInfo ci)
	{
		if (!MixinFlags.framebufferMixinEnabled || !this.useDepth)
		{
			return;
		}
		
		this.depthBuffer = TextureUtil.glGenTextures();
		
		if (this.distantHorizons$oldDepthTextureToDelete > -1)
		{
			GlStateManager.deleteTexture(this.distantHorizons$oldDepthTextureToDelete);
			this.distantHorizons$oldDepthTextureToDelete = -1;
		}
		
		GlStateManager.bindTexture(depthBuffer);
		
		LWJGL.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		LWJGL.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		LWJGL.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
		LWJGL.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
		LWJGL.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, 0);
		if (stencilEnabled)
		{
			GlStateManager.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_DEPTH24_STENCIL8, this.framebufferTextureWidth, this.framebufferTextureHeight, 0, GL30.GL_DEPTH_STENCIL, GL30.GL_UNSIGNED_INT_24_8, (IntBuffer) null);
		}
		else
		{
			GlStateManager.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_DEPTH_COMPONENT, this.framebufferTextureWidth, this.framebufferTextureHeight, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (IntBuffer) null);
		}
		OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, this.framebufferObject);
		OpenGlHelper.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, 3553, depthBuffer, 0);
		if (stencilEnabled)
		{
			OpenGlHelper.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER, GL30.GL_STENCIL_ATTACHMENT, 3553, depthBuffer, 0);
		}
	}
	
	@Redirect(method = "createFramebuffer", at = @At(value = "FIELD", target = "Lnet/minecraft/client/shader/Framebuffer;useDepth:Z", opcode = Opcodes.GETFIELD))
	private boolean noopDepthBuffer(Framebuffer instance)
	{
		if (!MixinFlags.framebufferMixinEnabled)
		{
			return useDepth;
		}
		
		return false;
	}
	
	@Redirect(method = "deleteFramebuffer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OpenGlHelper;glDeleteRenderbuffers(I)V"))
	private void deleteDepthTexture(int renderbuffer)
	{
		if (!MixinFlags.framebufferMixinEnabled)
		{
			OpenGlHelper.glDeleteRenderbuffers(renderbuffer);
			return;
		}
		
		this.distantHorizons$oldDepthTextureToDelete = renderbuffer;
	}
	
	//endregion
	
	
	
}