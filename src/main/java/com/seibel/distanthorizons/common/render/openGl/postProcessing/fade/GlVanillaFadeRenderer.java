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

package com.seibel.distanthorizons.common.render.openGl.postProcessing.fade;

import com.seibel.distanthorizons.common.render.openGl.GlDhMetaRenderer;
import com.seibel.distanthorizons.common.render.openGl.glObject.GLState;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftGLWrapper;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.render.RenderParams;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.render.renderPass.IDhVanillaFadeRenderer;
import com.seibel.distanthorizons.core.logging.DhLogger;
import org.lwjgl.opengl.GL32;

import java.nio.ByteBuffer;

/**
 * Handles fading MC and DH together via {@link GlDhVanillaFadeShader} and {@link GlDhFarFadeApplyShader}. <br><br>
 * 
 * {@link GlDhVanillaFadeShader} - draws the Fade to a texture. <br>
 * {@link GlDhFarFadeApplyShader} - draws the Fade texture to MC's FrameBuffer. <br>
 */
public class GlVanillaFadeRenderer implements IDhVanillaFadeRenderer
{
	public static GlVanillaFadeRenderer INSTANCE = new GlVanillaFadeRenderer();
	
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	private static final IMinecraftClientWrapper MC_CLIENT = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class);
	private static final MinecraftGLWrapper GLMC = MinecraftGLWrapper.INSTANCE;
	
	
	private boolean init = false;
	
	private int width = -1;
	private int height = -1;
	private int fadeFramebuffer = -1;
	
	private int fadeTexture = -1;
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	private GlVanillaFadeRenderer() { }
	
	public void init()
	{
		if (this.init) return;
		this.init = true;
		
		GlDhVanillaFadeShader.INSTANCE.init();
		GlDhFarFadeApplyShader.INSTANCE.init();
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
	
	//endregion
	
	
	
	//========//
	// render //
	//========//
	//region
	
	@Override
	public void render(RenderParams renderParams)
	{
		int depthTextureId = GlDhMetaRenderer.INSTANCE.getActiveDepthTextureId();
		if (depthTextureId == -1)
		{
			// the renderer hasn't been set up yet
			// trying to render fading may cause GL errors
			return;
		}
		
		
		
		IProfilerWrapper profiler = MC_CLIENT.getProfiler();
		profiler.pop(); // get out of "terrain"
		profiler.push("DH-Vanilla Fade");
		
		
		try(GLState mcState = new GLState())
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
			
			
			GlDhVanillaFadeShader.INSTANCE.frameBuffer = this.fadeFramebuffer;
			GlDhVanillaFadeShader.INSTANCE.setProjectionMatrix(renderParams.mcModelViewMatrix, renderParams.mcProjectionMatrix);
			GlDhVanillaFadeShader.INSTANCE.setLevelMaxHeight(renderParams.clientLevelWrapper.getMaxHeight());
			GlDhVanillaFadeShader.INSTANCE.render(renderParams);
			
			// Applying the fade texture is only needed if MC is drawing to their own frame buffer,
			// otherwise we can directly render to their texture
			if (MC_RENDER.mcRendersToFrameBuffer())
			{
				profiler.popPush("Vanilla Fade Apply");
				
				GlDhFarFadeApplyShader.INSTANCE.fadeTexture = this.fadeTexture;
				GlDhFarFadeApplyShader.INSTANCE.readFramebuffer = GlDhVanillaFadeShader.INSTANCE.frameBuffer;
				GlDhFarFadeApplyShader.INSTANCE.drawFramebuffer = MC_RENDER.getTargetFramebuffer();
				GlDhFarFadeApplyShader.INSTANCE.render(renderParams);
			}
			
			profiler.pop(); 
		}
		catch (Exception e)
		{
			LOGGER.error("Unexpected error during fade render, error: ["+e.getMessage()+"].", e);
		}
	}
	
	//endregion
	
	
	
	//================//
	// base overrides // 
	//================//
	//region
	
	public void free()
	{
		GlDhVanillaFadeShader.INSTANCE.free();
		GlDhFarFadeApplyShader.INSTANCE.free();
	}
	
	//endregion
	
	
	
}
