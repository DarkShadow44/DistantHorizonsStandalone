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

package com.seibel.distanthorizons.core.render.renderer.shaders;

import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.render.glObject.GLState;
import com.seibel.distanthorizons.core.render.glObject.shader.ShaderProgram;
import com.seibel.distanthorizons.core.render.renderer.LodRenderer;
import com.seibel.distanthorizons.core.render.renderer.ScreenQuad;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftGLWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL32;

import java.nio.ByteBuffer;

/**
 * Copies {@link LodRenderer}'s currently active color and depth texture to Minecraft's framebuffer. 
 */
public class DhApplyShader extends AbstractShaderRenderer
{
	public static DhApplyShader INSTANCE = new DhApplyShader();
	
	private static final Logger LOGGER = LogManager.getLogger();
	private static final IMinecraftGLWrapper GLMC = SingletonInjector.INSTANCE.get(IMinecraftGLWrapper.class);
	
	
	// uniforms
	public int gDhColorTextureUniform;
	public int gDepthMapUniform;
	
	
	
	private DhApplyShader() { }
	
	@Override
	public void onInit()
	{
		this.shader = new ShaderProgram(
				"shaders/normal.vert",
				"shaders/apply.frag",
				"fragColor",
				new String[]{"vPosition"});
		
		// uniform setup
		this.gDhColorTextureUniform = this.shader.getUniformLocation("gDhColorTexture");
		this.gDepthMapUniform = this.shader.getUniformLocation("gDhDepthTexture");
		
	}
	
	@Override
	protected void onApplyUniforms(float partialTicks) { }
	
	
	//========//
	// render //
	//========//
	
	@Override
	protected void onRender()
	{
		if (MC_RENDER.mcRendersToFrameBuffer())
		{
			this.renderToFrameBuffer();
		}
		else
		{
			this.renderToMcTexture();
		}
	}
	// TODO merge duplicate code between these to render methods
	private void renderToFrameBuffer()
	{
		int targetFrameBuffer = MC_RENDER.getTargetFrameBuffer();
		if (targetFrameBuffer == -1)
		{
			return;
		}
		
		
		GLState state = new GLState();
		
		GLMC.disableDepthTest();
		
		// blending isn't needed, we're manually merging the MC and DH textures
		// Note: this prevents the sun/moon and stars from rendering through transparent LODs,
		// however this also fixes transparent LODs from glowing when rendered against the sky during the day
		GLMC.disableBlend();
		
		// old blending logic in case it's ever needed:
		//GLMC.enableBlend();
		//GL32.glBlendEquation(GL32.GL_FUNC_ADD);
		//GLMC.glBlendFunc(GL32.GL_ONE, GL32.GL_ONE_MINUS_SRC_ALPHA);
		
		GLMC.glActiveTexture(GL32.GL_TEXTURE0);
		GLMC.glBindTexture(LodRenderer.getActiveColorTextureId());
		GL32.glUniform1i(this.gDhColorTextureUniform, 0);
		
		GLMC.glActiveTexture(GL32.GL_TEXTURE1);
		GLMC.glBindTexture(LodRenderer.getActiveDepthTextureId());
		GL32.glUniform1i(this.gDepthMapUniform, 1);
		
		// Copy to MC's framebuffer
		GLMC.glBindFramebuffer(GL32.GL_FRAMEBUFFER, targetFrameBuffer);
		
		ScreenQuad.INSTANCE.render();
		
		
		// restore everything, except at this point the MC framebuffer should now be used instead
		state.restore();
		GLMC.glBindFramebuffer(GL32.GL_FRAMEBUFFER, targetFrameBuffer);
		
	}
	private void renderToMcTexture()
	{
		int targetColorTextureId = MC_RENDER.getColorTextureId();
		if (targetColorTextureId == -1)
		{
			return;
		}
		
		int dhFrameBufferId = LodRenderer.getActiveFramebufferId();
		if (dhFrameBufferId == -1)
		{
			return;
		}
		
		int mcFrameBufferId = MC_RENDER.getTargetFrameBuffer();
		if (mcFrameBufferId == -1)
		{
			return;
		}
		
		
		
		GLState state = new GLState();
		
		GLMC.disableDepthTest();
		
		// blending isn't needed, we're just directly merging the MC and DH textures
		// Note: this prevents the sun/moon and stars from rendering through transparent LODs,
		// however this also fixes
		GLMC.disableBlend();
		
		// old blending logic in case it's ever needed:
		//GLMC.enableBlend();
		//GL32.glBlendEquation(GL32.GL_FUNC_ADD);
		//GLMC.glBlendFunc(GL32.GL_ONE, GL32.GL_ONE_MINUS_SRC_ALPHA);
		
		GLMC.glActiveTexture(GL32.GL_TEXTURE0);
		GLMC.glBindTexture(LodRenderer.getActiveColorTextureId());
		GL32.glUniform1i(this.gDhColorTextureUniform, 0);
		
		GLMC.glActiveTexture(GL32.GL_TEXTURE1);
		GLMC.glBindTexture(LodRenderer.getActiveDepthTextureId());
		GL32.glUniform1i(this.gDepthMapUniform, 1);
		
		
		
		GL32.glFramebufferTexture(GL32.GL_DRAW_FRAMEBUFFER, GL32.GL_COLOR_ATTACHMENT0, targetColorTextureId, 0);
		
		// Copy to MC's texture via MC's framebuffer
		GLMC.glBindFramebuffer(GL32.GL_FRAMEBUFFER, dhFrameBufferId);
		
		ScreenQuad.INSTANCE.render();
		
		
		// restore everything, except at this point the MC framebuffer should now be used instead
		state.restore();
		GLMC.glBindFramebuffer(GL32.GL_FRAMEBUFFER, mcFrameBufferId);
		
	}
	
	
	
}
