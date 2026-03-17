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

import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.render.glObject.shader.ShaderProgram;
import com.seibel.distanthorizons.core.render.renderer.LodRenderer;
import com.seibel.distanthorizons.core.render.renderer.SSAORenderer;
import com.seibel.distanthorizons.core.render.renderer.ScreenQuad;
import com.seibel.distanthorizons.core.util.math.Mat4f;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftGLWrapper;
import org.lwjgl.opengl.GL32;

/**
 * Draws the SSAO to a texture. <br><br>
 *
 * See Also: <br>
 * {@link SSAORenderer} - Parent to this shader. <br>
 * {@link SSAOApplyShader} - draws the SSAO texture to DH's FrameBuffer. <br>
 */
public class SSAOShader extends AbstractShaderRenderer
{
	public static SSAOShader INSTANCE = new SSAOShader();
	
	private static final IMinecraftGLWrapper GLMC = SingletonInjector.INSTANCE.get(IMinecraftGLWrapper.class);
	
	
	public int frameBuffer;
	
	private Mat4f projection;
	private Mat4f invertedProjection;
	
	
	// uniforms
	public int uProj;
	public int uInvProj;
	public int uSampleCount;
	public int uRadius;
	public int uStrength;
	public int uMinLight;
	public int uBias;
	public int uDepthMap;
	
	
	
	//=============//
	// constructor //
	//=============//
	
	@Override
	public void onInit()
	{
		this.shader = new ShaderProgram("shaders/normal.vert", "shaders/ssao/ao.frag",
				"fragColor", new String[]{ "vPosition" }
		);
		
		// uniform setup
		this.uProj = this.shader.getUniformLocation("uProj");
		this.uInvProj = this.shader.getUniformLocation("uInvProj");
		this.uSampleCount = this.shader.getUniformLocation("uSampleCount");
		this.uRadius = this.shader.getUniformLocation("uRadius");
		this.uStrength = this.shader.getUniformLocation("uStrength");
		this.uMinLight = this.shader.getUniformLocation("uMinLight");
		this.uBias = this.shader.getUniformLocation("uBias");
		this.uDepthMap = this.shader.getUniformLocation("uDepthMap");
	}
	
	
	
	//=============//
	// render prep //
	//=============//
	
	public void setProjectionMatrix(Mat4f projectionMatrix)
	{
		this.projection = projectionMatrix;
		
		this.invertedProjection = new Mat4f(projectionMatrix);
		this.invertedProjection.invert();
	}
	
	@Override
	protected void onApplyUniforms(float partialTicks)
	{
		this.shader.setUniform(this.uProj, this.projection);
		
		this.shader.setUniform(this.uInvProj, this.invertedProjection);
		
		this.shader.setUniform(this.uSampleCount, Config.Client.Advanced.Graphics.Ssao.sampleCount.get());
		
		// Explicit Number casts need to be done to prevent issues with the default value being an int
		Number radius = Config.Client.Advanced.Graphics.Ssao.radius.get(); 
		this.shader.setUniform(this.uRadius, radius.floatValue());
		
		Number strength = Config.Client.Advanced.Graphics.Ssao.strength.get();
		this.shader.setUniform(this.uStrength, strength.floatValue());
		
		Number minLight = Config.Client.Advanced.Graphics.Ssao.minLight.get();
		this.shader.setUniform(this.uMinLight, minLight.floatValue());
		
		Number bias = Config.Client.Advanced.Graphics.Ssao.bias.get();
		this.shader.setUniform(this.uBias, bias.floatValue());
		
		GL32.glUniform1i(this.uDepthMap, 0);
	}
	
	
	
	//========//
	// render //
	//========//
	
	@Override
	protected void onRender()
	{
		GLMC.glBindFramebuffer(GL32.GL_FRAMEBUFFER, this.frameBuffer);
		GLMC.disableScissorTest();
		GLMC.disableDepthTest();
		GLMC.disableBlend();
		
		GLMC.glActiveTexture(GL32.GL_TEXTURE0);
		GLMC.glBindTexture(LodRenderer.getActiveDepthTextureId());
		
		ScreenQuad.INSTANCE.render();
	}
}
