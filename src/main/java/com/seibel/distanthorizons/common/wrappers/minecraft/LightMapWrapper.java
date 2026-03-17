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

import com.seibel.distanthorizons.core.wrapperInterfaces.misc.ILightMapWrapper;
import com.seibel.distanthorizons.forge.ForgeMain;
import com.seibel.distanthorizons.interfaces.IMixinEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.opengl.GL32;

public class LightMapWrapper implements ILightMapWrapper
{
    public static final int GL_BOUND_INDEX = 0;

    public int getOpenGlId() {
        if (ForgeMain.rpleCompat != null) {
            return ForgeMain.rpleCompat.getTextureId();
        } else {
            IMixinEntityRenderer entityRenderer = (IMixinEntityRenderer)Minecraft.getMinecraft().entityRenderer;
            DynamicTexture lightmapTexture = entityRenderer.getLightmapTexture();
            return lightmapTexture.getGlTextureId();
        }
    }
}

