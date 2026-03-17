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

import com.seibel.distanthorizons.api.enums.config.EDhApiGpuUploadMethod;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.render.glObject.buffer.GLVertexBuffer;
import com.seibel.distanthorizons.core.render.glObject.shader.ShaderProgram;
import com.seibel.distanthorizons.core.render.glObject.vertexAttribute.AbstractVertexAttribute;
import com.seibel.distanthorizons.core.render.glObject.vertexAttribute.VertexPointer;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftGLWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;

import org.lwjgl.opengl.GL32;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Renders a UV colored quad
 * to the center of the screen to confirm DH's
 * apply shader is running correctly
 */
public class TestRenderer
{
	public static final DhLogger LOGGER = new DhLoggerBuilder().build(); 
	
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class);
	private static final IMinecraftGLWrapper GLMC = SingletonInjector.INSTANCE.get(IMinecraftGLWrapper.class);
	
	public static final TestRenderer INSTANCE = new TestRenderer();
	
	// Render a square with uv color
	private static final float[] VERTICES = {
		// PosX,Y, ColorR,G,B,A
		-0.5f, -0.5f, 0.0f, 0.0f, 0.0f, 1.0f,
		0.4f, -0.4f, 1.0f, 0.0f, 0.0f, 1.0f,
		0.3f, 0.3f, 1.0f, 1.0f, 0.0f, 0.0f,
		-0.2f, 0.2f, 0.0f, 1.0f, 1.0f, 1.0f
	};
	
	
	
	ShaderProgram basicShader;
	GLVertexBuffer vbo;
	AbstractVertexAttribute va;
	boolean init = false;
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	private TestRenderer() { }
	
	public void init()
	{
		if (this.init)
		{
			return;
		}
		
		LOGGER.info("init");
		this.init = true;
		this.va = AbstractVertexAttribute.create();
		this.va.bind();
		// Pos
		this.va.setVertexAttribute(0, 0, VertexPointer.addVec2Pointer(false));
		// Color
		this.va.setVertexAttribute(0, 1, VertexPointer.addVec4Pointer(false));
		this.va.completeAndCheck(Float.BYTES * 6);
		this.basicShader = new ShaderProgram(
			"shaders/test/vert.vert",
			"shaders/test/frag.frag",
			new String[]{"vPosition", "color"});
		
		this.createBuffer();
	}
	
	private void createBuffer()
	{
		ByteBuffer buffer = ByteBuffer.allocateDirect(VERTICES.length * Float.BYTES);
		// Fill buffer with vertices.
		buffer.order(ByteOrder.nativeOrder());
		buffer.asFloatBuffer().put(VERTICES);
		buffer.rewind();
		
		this.vbo = new GLVertexBuffer(false);
		this.vbo.bind();
		this.vbo.uploadBuffer(buffer, 4, EDhApiGpuUploadMethod.DATA, VERTICES.length * Float.BYTES);
	}
	
	//endregion
	
	
	
	//========//
	// render //
	//========//
	//region
	
	public void render()
	{
		this.init();
		
		this.basicShader.bind();
		this.va.bind();
		
		this.vbo.bind();
		this.va.bindBufferToAllBindingPoints(this.vbo.getId());
		
		// Render the square
		GL32.glDrawArrays(GL32.GL_TRIANGLE_FAN, 0, 4);
	}
	
	//endregion
	
	
	
}
