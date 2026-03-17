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

package com.seibel.distanthorizons.common.render.openGl.postProcessing.fog;

import com.seibel.distanthorizons.common.render.openGl.glObject.GLState;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftGLWrapper;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.render.RenderParams;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.render.renderPass.IDhFogRenderer;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL43C;

import java.nio.ByteBuffer;

/**
 * Handles adding SSAO via {@link GlDhFogShader} and {@link GlDhFogApplyShader}. <br><br>
 * 
 * {@link GlDhFogShader} - draws the Fog to a texture. <br>
 * {@link GlDhFogApplyShader} - draws the Fog texture to DH's FrameBuffer. <br>
 */
public class GlDhFogRenderer implements IDhFogRenderer
{
	public static GlDhFogRenderer INSTANCE = new GlDhFogRenderer();
	
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class);
	private static final MinecraftGLWrapper GLMC = MinecraftGLWrapper.INSTANCE;
	
	
	private boolean init = false;
	
	private int width = -1;
	private int height = -1;
	private int fogFramebuffer = -1;
	
	private int fogTexture = -1;
	
	
	
	//=============//
	// constructor //
	//=============//
	
	private GlDhFogRenderer() { }
	
	public void init()
	{
		if (this.init) return;
		this.init = true;
		
		GlDhFogShader.INSTANCE.init();
		GlDhFogApplyShader.INSTANCE.init();
	}
	
	private void createFramebuffer(int width, int height)
	{
		if (this.fogFramebuffer != -1)
		{
			GL32.glDeleteFramebuffers(this.fogFramebuffer);
			this.fogFramebuffer = -1;
		}
		
		if (this.fogTexture != -1)
		{
			GLMC.glDeleteTextures(this.fogTexture);
			this.fogTexture = -1;
		}
		
		this.fogFramebuffer = GL32.glGenFramebuffers();
		GLMC.glBindFramebuffer(GL32.GL_FRAMEBUFFER, this.fogFramebuffer);
		
		this.fogTexture = GLMC.glGenTextures();
		{
			GLMC.glBindTexture(this.fogTexture);
			GL32.glTexImage2D(GL32.GL_TEXTURE_2D, 0, GL32.GL_RGBA16, width, height, 0, GL32.GL_RGBA, GL32.GL_UNSIGNED_SHORT_4_4_4_4, (ByteBuffer) null);
			GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_MIN_FILTER, GL32.GL_LINEAR);
			GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_MAG_FILTER, GL32.GL_LINEAR);
			GL32.glFramebufferTexture2D(GL32.GL_FRAMEBUFFER, GL32.GL_COLOR_ATTACHMENT0, GL32.GL_TEXTURE_2D, this.fogTexture, 0);
			
			// disable mip-mapping since DH is just going to draw straight to the screen
			GL43C.glTexParameteri(GL43C.GL_TEXTURE_2D, GL43C.GL_TEXTURE_BASE_LEVEL, 0);
			GL43C.glTexParameteri(GL43C.GL_TEXTURE_2D, GL43C.GL_TEXTURE_MAX_LEVEL, 0);
		}
	}
	
	
	
	//========//
	// render //
	//========//
	//region
	
	@Override
	public void render(RenderParams renderParams)
	{
		// GLState needed in MC 1.16.5 probably due to MC not manually setting each GL state they need before the next rendering step
		try (GLState state = new GLState())
		{
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
			
			GlDhFogShader.INSTANCE.frameBuffer = this.fogFramebuffer;
			GlDhFogShader.INSTANCE.setProjectionMatrix(renderParams.dhMvmProjMatrix);
			GlDhFogShader.INSTANCE.render(renderParams);
			
			GlDhFogApplyShader.INSTANCE.fogTexture = this.fogTexture;
			GlDhFogApplyShader.INSTANCE.render(renderParams);
		}
	}
	
	//endregion
	
	
	
	//================//
	// base overrides //
	//================//
	//region
	
	public void free()
	{
		GlDhFogShader.INSTANCE.free();
		GlDhFogApplyShader.INSTANCE.free();
	}
	
	//endregion
	
	
	
}
