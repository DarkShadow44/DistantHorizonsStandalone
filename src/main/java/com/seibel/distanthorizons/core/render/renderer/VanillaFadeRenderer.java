/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020 James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.distanthorizons.core.render.renderer;

import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.render.glObject.GLState;
import com.seibel.distanthorizons.core.render.renderer.shaders.DhFadeShader;
import com.seibel.distanthorizons.core.render.renderer.shaders.FadeApplyShader;
import com.seibel.distanthorizons.core.render.renderer.shaders.VanillaFadeShader;
import com.seibel.distanthorizons.core.util.math.Mat4f;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftGLWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.distanthorizons.core.logging.DhLogger;
import org.lwjgl.opengl.GL32;

import java.nio.ByteBuffer;

/**
 * Handles fading MC and DH together via {@link VanillaFadeShader} and {@link FadeApplyShader}. <br><br>
 * 
 * {@link VanillaFadeShader} - draws the Fade to a texture. <br>
 * {@link FadeApplyShader} - draws the Fade texture to MC's FrameBuffer. <br>
 */
public class VanillaFadeRenderer
{
	public static VanillaFadeRenderer INSTANCE = new VanillaFadeRenderer();
	
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	private static final IMinecraftClientWrapper MC_CLIENT = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class);
	private static final IMinecraftGLWrapper GLMC = SingletonInjector.INSTANCE.get(IMinecraftGLWrapper.class);
	
	
	private boolean init = false;
	
	private int width = -1;
	private int height = -1;
	private int fadeFramebuffer = -1;
	
	private int fadeTexture = -1;
	
	
	
	//=============//
	// constructor //
	//=============//
	
	private VanillaFadeRenderer() { }
	
	public void init()
	{
		if (this.init) return;
		this.init = true;
		
		VanillaFadeShader.INSTANCE.init();
		FadeApplyShader.INSTANCE.init();
	}
	
	private void createFramebuffer(int width, int height)
	{
		if (this.fadeFramebuffer != -1)
		{
			GL32.glDeleteFramebuffers(this.fadeFramebuffer);
			this.fadeFramebuffer = -1;
		}
		
		this.fadeFramebuffer = GL32.glGenFramebuffers();
		GLMC.glBindFramebuffer(GL32.GL_FRAMEBUFFER, this.fadeFramebuffer);
		
		
		// Applying the fade texture is only needed if MC is drawing to their own frame buffer,
		// otherwise we can directly render to their texture
		if (MC_RENDER.mcRendersToFrameBuffer())
		{
			if (this.fadeTexture != -1)
			{
				GLMC.glDeleteTextures(this.fadeTexture);
				this.fadeTexture = -1;
			}
			
			this.fadeTexture = GL32.glGenTextures();
			GLMC.glBindTexture(this.fadeTexture);
			GL32.glTexImage2D(GL32.GL_TEXTURE_2D, 0, GL32.GL_RGBA16, width, height, 0, GL32.GL_RGBA, GL32.GL_UNSIGNED_SHORT_4_4_4_4, (ByteBuffer) null);
			GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_MIN_FILTER, GL32.GL_LINEAR);
			GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_MAG_FILTER, GL32.GL_LINEAR);
			GL32.glFramebufferTexture2D(GL32.GL_FRAMEBUFFER, GL32.GL_COLOR_ATTACHMENT0, GL32.GL_TEXTURE_2D, this.fadeTexture, 0);
		}
		else
		{
			GL32.glFramebufferTexture2D(GL32.GL_FRAMEBUFFER, GL32.GL_COLOR_ATTACHMENT0, GL32.GL_TEXTURE_2D, MC_RENDER.getColorTextureId(), 0);
		}
	}
	
	
	
	//========//
	// render //
	//========//
	
	public void render(Mat4f mcModelViewMatrix, Mat4f mcProjectionMatrix, float partialTicks, IClientLevelWrapper level)
	{
		int depthTextureId = LodRenderer.INSTANCE.getActiveDepthTextureId();
		if (depthTextureId == -1)
		{
			// the renderer hasn't been set up yet
			// trying to render fading may cause GL errors
			return;
		}
		
		
		
		IProfilerWrapper profiler = MC_CLIENT.getProfiler();
		profiler.pop(); // get out of "terrain"
		profiler.push("DH-Vanilla Fade");
		
		
		GLState mcState = new GLState();
		
		try
		{
			profiler.push("Vanilla Fade Generate");
			
			this.init();
			
			// resize the framebuffer if necessary
			int width = MC_RENDER.getTargetFramebufferViewportWidth();
			int height = MC_RENDER.getTargetFramebufferViewportHeight();
			if (this.width != width || this.height != height)
			{
				this.width = width;
				this.height = height;
				this.createFramebuffer(width, height);
			}
			
			
			VanillaFadeShader.INSTANCE.frameBuffer = this.fadeFramebuffer;
			VanillaFadeShader.INSTANCE.setProjectionMatrix(mcModelViewMatrix, mcProjectionMatrix, partialTicks);
			VanillaFadeShader.INSTANCE.setLevelMaxHeight(level.getMaxHeight());
			VanillaFadeShader.INSTANCE.render(partialTicks);
			
			// Applying the fade texture is only needed if MC is drawing to their own frame buffer,
			// otherwise we can directly render to their texture
			if (MC_RENDER.mcRendersToFrameBuffer())
			{
				profiler.popPush("Vanilla Fade Apply");
				
				FadeApplyShader.INSTANCE.fadeTexture = this.fadeTexture;
				FadeApplyShader.INSTANCE.readFramebuffer = DhFadeShader.INSTANCE.frameBuffer;
				FadeApplyShader.INSTANCE.drawFramebuffer = MC_RENDER.getTargetFramebuffer();
				FadeApplyShader.INSTANCE.render(partialTicks);
			}
			
			profiler.pop(); 
		}
		catch (Exception e)
		{
			LOGGER.error("Unexpected error during fade render, error: ["+e.getMessage()+"].", e);
		}
		finally
		{
			// make sure we always revert to MC's state to prevent GL state corruption
			// this is especially important on MC 1.16.5 or when other rendering mods are present
			mcState.restore();
		}
	}
	
	public void free()
	{
		VanillaFadeShader.INSTANCE.free();
		FadeApplyShader.INSTANCE.free();
	}
	
}
