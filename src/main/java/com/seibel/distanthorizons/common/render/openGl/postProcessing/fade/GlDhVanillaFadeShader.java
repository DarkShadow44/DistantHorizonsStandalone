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

import com.seibel.distanthorizons.api.objects.math.DhApiMat4f;
import com.seibel.distanthorizons.common.render.openGl.GlDhMetaRenderer;
import com.seibel.distanthorizons.common.render.openGl.glObject.shader.GlShaderProgram;
import com.seibel.distanthorizons.common.render.openGl.postProcessing.GlScreenQuad;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftGLWrapper;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.common.render.openGl.util.GlAbstractShaderRenderer;
import com.seibel.distanthorizons.core.render.RenderParams;
import com.seibel.distanthorizons.core.util.RenderUtil;
import com.seibel.distanthorizons.core.util.math.Mat4f;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import org.lwjgl.opengl.GL32;

public class GlDhVanillaFadeShader extends GlAbstractShaderRenderer 
{
	public static GlDhVanillaFadeShader INSTANCE = new GlDhVanillaFadeShader();
	
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class);
	private static final MinecraftGLWrapper GLMC = MinecraftGLWrapper.INSTANCE;
	
	
	public int frameBuffer = -1;
	
	private Mat4f inverseMcMvmProjMatrix;
	private Mat4f inverseDhMvmProjMatrix;
	private float levelMaxHeight;
	
	
	// Uniforms
	public int uMcDepthTexture = -1;
	public int uDhDepthTexture = -1;
	public int uCombinedMcDhColorTexture = -1;
	public int uDhColorTexture = -1;
	
	/** Inverted Model View Projection matrix */
	public int uDhInvMvmProj = -1;
	public int uMcInvMvmProj = -1;
	
	public int uStartFadeBlockDistance = -1;
	public int uEndFadeBlockDistance = -1;
	public int uMaxLevelHeight = -1;
	
	public int uOnlyRenderLods = -1;
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	public GlDhVanillaFadeShader() {  }

	@Override
	public void onInit()
	{
		this.shader = new GlShaderProgram(
			"assets/distanthorizons/shaders/shared/gl/quad_apply.vert",
			"assets/distanthorizons/shaders/fade/gl/vanilla_fade.frag",
			"vPosition"
		);
		
		// all uniforms should be tryGet...
		// because disabling fade can cause the GLSL to optimize out most (if not all) uniforms
		
		// near fade
		this.uDhInvMvmProj = this.shader.tryGetUniformLocation("uDhInvMvmProj");
		this.uMcInvMvmProj = this.shader.tryGetUniformLocation("uMcInvMvmProj");
		
		this.uMcDepthTexture = this.shader.tryGetUniformLocation("uMcDepthTexture");
		this.uDhDepthTexture = this.shader.tryGetUniformLocation("uDhDepthTexture");
		this.uCombinedMcDhColorTexture = this.shader.tryGetUniformLocation("uCombinedMcDhColorTexture");
		this.uDhColorTexture = this.shader.tryGetUniformLocation("uDhColorTexture");
		
		this.uStartFadeBlockDistance = this.shader.tryGetUniformLocation("uStartFadeBlockDistance");
		this.uEndFadeBlockDistance = this.shader.tryGetUniformLocation("uEndFadeBlockDistance");
		this.uMaxLevelHeight = this.shader.tryGetUniformLocation("uMaxLevelHeight");
		
		this.uOnlyRenderLods = this.shader.tryGetUniformLocation("uOnlyRenderLods");
		
	}
	
	//endregion
	
	
	
	//=============//
	// render prep //
	//=============//
	//region
	
	@Override
	protected void onApplyUniforms(RenderParams renderParams)
	{
		this.shader.setUniform(this.uMcInvMvmProj, this.inverseMcMvmProjMatrix);
		this.shader.setUniform(this.uDhInvMvmProj, this.inverseDhMvmProjMatrix);
		
		
		float dhNearClipDistance = RenderUtil.getNearClipPlaneInBlocks();
		// this added value prevents the near clip plane and discard circle from touching, which looks bad
		dhNearClipDistance += 16f;
		
		// measured in blocks
		// these multipliers in James' tests should provide a fairly smooth transition
		// without having underdraw issues
		float fadeStartDistance = dhNearClipDistance * 1.5f;
		float fadeEndDistance = dhNearClipDistance * 1.9f;
		
		this.shader.setUniform(this.uStartFadeBlockDistance, fadeStartDistance);
		this.shader.setUniform(this.uEndFadeBlockDistance, fadeEndDistance);
		
		this.shader.setUniform(this.uMaxLevelHeight, this.levelMaxHeight);
		
		this.shader.setUniform(this.uOnlyRenderLods, Config.Client.Advanced.Debugging.lodOnlyMode.get());
	}
	
	public void setProjectionMatrix(DhApiMat4f mcModelViewMatrix, DhApiMat4f mcProjectionMatrix)
	{
		Mat4f inverseMcModelViewProjectionMatrix = new Mat4f(mcProjectionMatrix);
		inverseMcModelViewProjectionMatrix.multiply(mcModelViewMatrix);
		inverseMcModelViewProjectionMatrix.invert();
		this.inverseMcMvmProjMatrix = inverseMcModelViewProjectionMatrix;
		
		
		Mat4f dhProjectionMatrix = RenderUtil.createLodProjectionMatrix(mcProjectionMatrix);
		Mat4f dhModelViewMatrix = RenderUtil.createLodModelViewMatrix(mcModelViewMatrix);
		
		Mat4f inverseDhModelViewProjectionMatrix = new Mat4f(dhProjectionMatrix);
		inverseDhModelViewProjectionMatrix.multiply(dhModelViewMatrix);
		inverseDhModelViewProjectionMatrix.invert();
		this.inverseDhMvmProjMatrix = inverseDhModelViewProjectionMatrix;
	}
	public void setLevelMaxHeight(int levelMaxHeight) { this.levelMaxHeight = levelMaxHeight; }
	
	//endregion
	
	
	
	//========//
	// render //
	//========//
	//region
	
	@Override
	protected void onRender()
	{
		int depthTextureId = GlDhMetaRenderer.INSTANCE.getActiveDepthTextureId();
		int colorTextureId = GlDhMetaRenderer.INSTANCE.getActiveColorTextureId();
		
		if (depthTextureId == -1
			|| colorTextureId == -1)
		{
			// the renderer is currently being re-built and/or inactive,
			// we don't need to/can't render fading
			return;
		}
		
		
		
		GLMC.glBindFramebuffer(GL32.GL_FRAMEBUFFER, this.frameBuffer);
		GLMC.disableScissorTest();
		GLMC.disableDepthTest();
		GLMC.disableBlend();
		
		GLMC.glActiveTexture(GL32.GL_TEXTURE0);
		GLMC.glBindTexture(MC_RENDER.getDepthTextureId());
		GL32.glUniform1i(this.uMcDepthTexture, 0);
		
		GLMC.glActiveTexture(GL32.GL_TEXTURE1);
		GLMC.glBindTexture(depthTextureId);
		GL32.glUniform1i(this.uDhDepthTexture, 1);
		
		GLMC.glActiveTexture(GL32.GL_TEXTURE2);
		GLMC.glBindTexture(MC_RENDER.getColorTextureId());
		GL32.glUniform1i(this.uCombinedMcDhColorTexture, 2);
		
		GLMC.glActiveTexture(GL32.GL_TEXTURE3);
		GLMC.glBindTexture(colorTextureId);
		GL32.glUniform1i(this.uDhColorTexture, 3);
		
		
		GlScreenQuad.INSTANCE.render();
	}
	
	//endregion
	
	
	
}
