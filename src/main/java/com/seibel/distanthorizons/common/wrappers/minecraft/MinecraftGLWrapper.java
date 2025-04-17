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

package com.seibel.distanthorizons.common.wrappers.minecraft;

import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftGLWrapper;

import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL32;

public class MinecraftGLWrapper implements IMinecraftGLWrapper
{
    public static final MinecraftGLWrapper INSTANCE = new MinecraftGLWrapper();

    private static final Logger LOGGER = DhLoggerBuilder.getLogger();



	/*
    private static final StencilState STENCIL;
	 */


    // scissor //

    /** @see GL32#GL_SCISSOR_TEST */
    @Override
    public void enableScissorTest()
    {
        GL32.glEnable(GL32.GL_SCISSOR_TEST);
    }
    /** @see GL32#GL_SCISSOR_TEST */
    @Override
    public void disableScissorTest()
    {
        GL32.glDisable(GL32.GL_SCISSOR_TEST);
    }


    // stencil //
//
//	/** @see GL32#GL_SCISSOR_TEST */
//	public void enableScissorTest() { GLStateManager._stencilFunc(); }
//	/** @see GL32#GL_SCISSOR_TEST */
//	public void disableScissorTest() { GLStateManager._disableScissorTest(); }


    // depth //

    /** @see GL32#GL_DEPTH_TEST */
    @Override
    public void enableDepthTest()
    {
        GL32.glEnable(GL32.GL_DEPTH_TEST);
    }
    /** @see GL32#GL_DEPTH_TEST */
    @Override
    public void disableDepthTest()
    {
        GL32.glDisable(GL32.GL_DEPTH_TEST);
    }

    /** @see GL32#glDepthFunc(int)  */
    @Override
    public void glDepthFunc(int func)
    {
        GL32.glDepthFunc(func);
    }

    /** @see GL32#glDepthMask(boolean) */
    @Override
    public void enableDepthMask()
    {
        GL32.glDepthMask(true);
    }
    /** @see GL32#glDepthMask(boolean) */
    @Override
    public void disableDepthMask()
    {
        GL32.glDepthMask(false);
    }


    // blending //

    /** @see GL32#GL_BLEND */
    @Override
    public void enableBlend()
    {
        GL32.glEnable(GL32.GL_BLEND);
    }
    /** @see GL32#GL_BLEND */
    @Override
    public void disableBlend()
    {
        GL32.glDisable(GL32.GL_BLEND);
    }

    /** @see GL32#glBlendFunc */
    @Override
    public void glBlendFunc(int sfactor, int dfactor)
    {
        GL32.glBlendFunc(sfactor, dfactor);
    }
    /** @see GL32#glBlendFuncSeparate */
    @Override
    public void glBlendFuncSeparate(int sfactorRGB, int dfactorRGB, int sfactorAlpha, int dfactorAlpha)
    {
        GL32.glBlendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha);
    }


    // frame buffers //

    /** @see GL32#glBindFramebuffer */
    @Override
    public void glBindFramebuffer(int target, int framebuffer)
    {
        GL32.glBindFramebuffer(target, framebuffer);
    }


    // buffers //

    /** @see GL32#glGenBuffers() */
    @Override
    public int glGenBuffers()
    { return GL32.glGenBuffers(); }

    /** @see GL32#glDeleteBuffers(int)  */
    @Override
    public void glDeleteBuffers(int buffer)
    { GL32.glDeleteBuffers(buffer); }


    // culling //

    /** @see GL32#GL_CULL_FACE */
    @Override
    public void enableFaceCulling()
    {
        GL32.glEnable(GL32.GL_CULL_FACE);
    }
    /** @see GL32#GL_CULL_FACE */
    @Override
    public void disableFaceCulling()
    {
        GL32.glDisable(GL32.GL_CULL_FACE);
    }


    // textures //

    /** @see GL32#glGenTextures() */
    @Override
    public int glGenTextures() { return GL32.glGenTextures(); }
    /** @see GL32#glDeleteTextures(int) */
    @Override
    public void glDeleteTextures(int texture) {
        GL32.glDeleteTextures(texture);
    }

    /** @see GL32#glActiveTexture(int) */
    @Override
    public void glActiveTexture(int textureId)
    {
        GL32.glActiveTexture(textureId);
    }
    @Override
    public int getActiveTexture() { return GL32.glGetInteger(GL32.GL_ACTIVE_TEXTURE); }

    /**
     * Always binds to {@link GL32#GL_TEXTURE_2D}
     * @see GL32#glBindTexture(int, int)
     */
    @Override
    public void glBindTexture(int texture)
    {
        GL32.glBindTexture(GL32.GL_TEXTURE_2D, texture);
    }




}
