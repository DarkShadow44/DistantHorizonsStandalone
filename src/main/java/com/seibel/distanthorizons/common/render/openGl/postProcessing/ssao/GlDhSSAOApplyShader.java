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

package com.seibel.distanthorizons.common.render.openGl.postProcessing.ssao;

import com.seibel.distanthorizons.common.render.openGl.GlDhMetaRenderer;
import com.seibel.distanthorizons.common.render.openGl.glObject.shader.GlShaderProgram;
import com.seibel.distanthorizons.common.render.openGl.postProcessing.GlScreenQuad;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftGLWrapper;
import com.seibel.distanthorizons.common.render.openGl.util.GlAbstractShaderRenderer;
import com.seibel.distanthorizons.core.render.RenderParams;
import com.seibel.distanthorizons.core.util.RenderUtil;
import org.lwjgl.opengl.GL32;

/**
 * Draws the SSAO texture onto DH's FrameBuffer. <br><br>
 * 
 * See Also: <br>
 * {@link GlDhSSAORenderer} - Parent to this shader. <br>
 * {@link GlDhSSAOShader} - draws the SSAO texture. <br>
 */
public class GlDhSSAOApplyShader extends GlAbstractShaderRenderer
{
	public static GlDhSSAOApplyShader INSTANCE = new GlDhSSAOApplyShader();
	
	private static final MinecraftGLWrapper GLMC = MinecraftGLWrapper.INSTANCE;
	
	
	public int ssaoTexture;
	
	// uniforms
	public int gSSAOMapUniform;
	public int gDepthMapUniform;
	public int gViewSizeUniform;
	public int gBlurRadiusUniform;
	public int gNearUniform;
	public int gFarUniform;
	
	
	
	//=============//
	// constructor //
	//=============//
	
	@Override
	public void onInit()
	{
		this.shader = new GlShaderProgram(
			"assets/distanthorizons/shaders/shared/gl/quad_apply.vert",
			"assets/distanthorizons/shaders/ssao/gl/apply.frag",
			"vPosition"
		);
		
		// uniform setup
		this.gSSAOMapUniform = this.shader.getUniformLocation("gSSAOMap");
		this.gDepthMapUniform = this.shader.getUniformLocation("gDepthMap");
		this.gViewSizeUniform = this.shader.tryGetUniformLocation("gViewSize");
		this.gBlurRadiusUniform = this.shader.tryGetUniformLocation("gBlurRadius");
		this.gNearUniform = this.shader.tryGetUniformLocation("gNear");
		this.gFarUniform = this.shader.tryGetUniformLocation("gFar");
	}
	
	
	
	//=============//
	// render prep //
	//=============//
	
	@Override
	protected void onApplyUniforms(RenderParams renderParams)
	{
		GLMC.glActiveTexture(GL32.GL_TEXTURE0);
		GLMC.glBindTexture(GlDhMetaRenderer.INSTANCE.getActiveDepthTextureId());
		GL32.glUniform1i(this.gDepthMapUniform, 0);
		
		GLMC.glActiveTexture(GL32.GL_TEXTURE1);
		GLMC.glBindTexture(this.ssaoTexture);
		GL32.glUniform1i(this.gSSAOMapUniform, 1);
		
		GL32.glUniform1i(this.gBlurRadiusUniform, 2);
		
		if (this.gViewSizeUniform >= 0)
		{
			GL32.glUniform2f(this.gViewSizeUniform,
					MC_RENDER.getTargetFramebufferViewportWidth(),
					MC_RENDER.getTargetFramebufferViewportHeight());
		}
		
		if (this.gNearUniform >= 0)
		{
			GL32.glUniform1f(this.gNearUniform,
					RenderUtil.getNearClipPlaneInBlocks());
		}
		
		if (this.gFarUniform >= 0)
		{
			float farClipPlane = RenderUtil.getFarClipPlaneDistanceInBlocks();
			GL32.glUniform1f(this.gFarUniform, farClipPlane);
		}
	}
	
	
	
	//========//
	// render //
	//========//
	
	@Override
	protected void onRender()
	{
		GLMC.enableBlend();
		GL32.glBlendEquation(GL32.GL_FUNC_ADD);
		GLMC.glBlendFuncSeparate(GL32.GL_ZERO, GL32.GL_SRC_ALPHA, GL32.GL_ZERO, GL32.GL_ONE);

		// Depth testing must be disabled otherwise this application shader won't apply anything.
		// setting this isn't necessary in vanilla, but some mods may change this, requiring it to be set manually, 
		// it should be automatically restored after rendering is complete.
		GLMC.disableDepthTest();
		
		// apply the rendered SSAO to the LODs 
		GLMC.glBindFramebuffer(GL32.GL_READ_FRAMEBUFFER, GlDhSSAOShader.INSTANCE.frameBuffer);
		GLMC.glBindFramebuffer(GL32.GL_DRAW_FRAMEBUFFER, GlDhMetaRenderer.INSTANCE.getActiveFramebufferId());
		
		
		GlScreenQuad.INSTANCE.render();
	}
	
	
	
}
