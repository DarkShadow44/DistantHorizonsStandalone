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

package com.seibel.distanthorizons.core.render.glObject.shader;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;

import com.seibel.distanthorizons.core.render.glObject.GLProxy;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeType;

/**
 * This object holds a OpenGL reference to a shader
 * and allows for reading in and compiling a shader file.
 */
public class Shader
{
	/** OpenGL shader ID */
	public final int id;
	
	
	
	//==============//
	// constructors //
	//==============//
	
	/**
	 * Creates a shader with specified type.
	 *
	 * @param type Either GL_VERTEX_SHADER or GL_FRAGMENT_SHADER.
	 * @param path File path of the shader
	 * @param absoluteFilePath If false the file path is relative to the resource jar folder.
	 * @throws RuntimeException if the shader fails to compile
	 */
	public Shader(int type, String path, boolean absoluteFilePath)
	{
		GLProxy.GL_LOGGER.info("Loading shader at [" + path + "]");
		// Create an empty shader object
		this.id = GL32.glCreateShader(type);
		if (this.id == 0)
		{
			throw new IllegalArgumentException("Failed to create shader with type ["+type+"].");
		}
		
		StringBuilder source = loadFile(path, absoluteFilePath, new StringBuilder());
		safeShaderSource(this.id, source);
		
		GL32.glCompileShader(this.id);
		// check if the shader compiled
		int status = GL32.glGetShaderi(this.id, GL32.GL_COMPILE_STATUS);
		if (status != GL32.GL_TRUE)
		{
			String message = "Shader compiler error. Details: ["+GL32.glGetShaderInfoLog(this.id)+"].";
			this.free(); // important!
			throw new RuntimeException(message);
		}
		GLProxy.GL_LOGGER.info("Shader at " + path + " loaded successfully.");
	}
	
	public Shader(int type, String sourceString)
	{
		GLProxy.GL_LOGGER.info("Loading shader with type: ["+type+"]");
		GLProxy.GL_LOGGER.debug("Source: \n["+sourceString+"]");
		if (sourceString == null || sourceString.isEmpty())
		{
			throw new IllegalArgumentException("No shader source given.");
		}
		
		// Create an empty shader object
		this.id = GL32.glCreateShader(type);
		if (this.id == 0)
		{
			throw new IllegalArgumentException("Failed to create shader with type ["+type+"] and Source: \n["+sourceString+"].");
		}
		
		safeShaderSource(this.id, sourceString);
		GL32.glCompileShader(this.id);
		// check if the shader compiled
		int status = GL32.glGetShaderi(this.id, GL32.GL_COMPILE_STATUS);
		if (status != GL32.GL_TRUE)
		{
			
			String message = "Shader compiler error. Details: [" + GL32.glGetShaderInfoLog(this.id) + "]\n";
			message += "Source: \n[" + sourceString + "]";
			this.free(); // important!
			throw new RuntimeException(message);
		}
		GLProxy.GL_LOGGER.info("Shader loaded sucessfully.");
	}
	
	
	
	//=========//
	// helpers //
	//=========//
	
	/**
	 * Identical in function to {@link GL32C#glShaderSource(int, CharSequence)} but
	 * passes a null pointer for string length to force the driver to rely on the null
	 * terminator for string length.  This is a workaround for an apparent flaw with some
	 * AMD drivers that don't receive or interpret the length correctly, resulting in
	 * an access violation when the driver tries to read past the string memory.
	 *
	 * <p>Hat tip to fewizz for the find and the fix.
	 * 
	 * <p>Source: https://github.com/vram-guild/canvas/commit/820bf754092ccaf8d0c169620c2ff575722d7d96
	 */
	private static void safeShaderSource(@NativeType("GLuint") int glId, @NativeType("GLchar const **") CharSequence source)
	{
		final MemoryStack stack = MemoryStack.stackGet();
		final int stackPointer = stack.getPointer();

		try
		{
			final ByteBuffer sourceBuffer = MemoryUtil.memUTF8(source, true);
			final PointerBuffer pointers = stack.mallocPointer(1);
			pointers.put(sourceBuffer);

			GL32.nglShaderSource(glId, 1, pointers.address0(), 0);
			org.lwjgl.system.APIUtil.apiArrayFree(pointers.address0(), 1);
		}
		finally
		{
			stack.setPointer(stackPointer);
		}
	}
	
	public void free() { GL32.glDeleteShader(this.id); }
	
	public static StringBuilder loadFile(String path, boolean absoluteFilePath, StringBuilder stringBuilder)
	{
		try
		{
			// open the file
			InputStream in;
			if (absoluteFilePath)
			{
				// Throws FileNotFoundException
				in = new FileInputStream(path); // Note: this should use OS path seperator
			}
			else
			{
				in = Shader.class.getClassLoader().getResourceAsStream(path); // Note: path seperator should be '/'
				if (in == null)
				{
					throw new FileNotFoundException("Shader file not found in resource: " + path);
				}
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			
			// read in the file
			String line;
			while ((line = reader.readLine()) != null)
			{
				stringBuilder.append(line).append("\n");
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException("Unable to load shader from file [" + path + "]. Error: " + e.getMessage());
		}
		return stringBuilder;
	}
	
	
	
}
