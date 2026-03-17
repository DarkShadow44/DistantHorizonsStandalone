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

import java.awt.Color;
import java.lang.invoke.MethodHandles;

import com.seibel.distanthorizons.api.enums.config.EDhApiLodShading;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.enums.EDhDirection;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.forge.ForgeMain;
import copy.com.gtnewhorizons.angelica.compat.mojang.Camera;
import com.seibel.distanthorizons.common.wrappers.WrapperFactory;

import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.ILightMapWrapper;

import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.distanthorizons.core.util.math.Vec3d;
import com.seibel.distanthorizons.core.util.math.Vec3f;
import com.seibel.distanthorizons.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.distanthorizons.interfaces.IMixinMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.potion.Potion;
import net.minecraft.util.Vec3;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3d;
import org.lwjgl.opengl.GL15;


/**
 * A singleton that contains everything
 * related to rendering in Minecraft.
 *
 * @author James Seibel
 * @version 12-12-2021
 */
//@Environment(EnvType.CLIENT)
public class MinecraftRenderWrapper implements IMinecraftRenderWrapper
{
    public static final MinecraftRenderWrapper INSTANCE = new MinecraftRenderWrapper();

    private static final DhLogger LOGGER = new DhLoggerBuilder().build();
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final IWrapperFactory FACTORY = WrapperFactory.INSTANCE;

    /**
     * In the case of immersive portals multiple levels may be active at once, causing conflicting lightmaps. <br>
     * Requiring the use of multiple LightMapWrapper.
     */
   // public ConcurrentHashMap<IDimensionTypeWrapper, LightMapWrapper> lightmapByDimensionType = new ConcurrentHashMap<>();

    /**
     * Holds the render buffer that should be used when displaying levels to the screen.
     * This is used for Optifine shader support so we can render directly to Optifine's level frame buffer.
     */
    public int finalLevelFrameBufferId = -1;



    @Override
    public Vec3f getLookAtVector()
    {
        Vec3 look = MC.renderViewEntity.getLookVec();
        return new Vec3f((float)look.xCoord, (float)look.yCoord, (float)look.zCoord);
    }

    @Override
    /** Unless you really need to know if the player is blind, use {@link MinecraftRenderWrapper#isFogStateSpecial()}/{@link IMinecraftRenderWrapper#isFogStateSpecial()} instead */
    public boolean playerHasBlindingEffect()
    {
        return MC.thePlayer.getActivePotionEffect(Potion.blindness) != null;
    }

    @Override
    public Vec3d getCameraExactPosition()
    {
        float frameTime = ((IMixinMinecraft)Minecraft.getMinecraft()).getTimer().renderPartialTicks;
        Camera camera = new Camera(MC.renderViewEntity, frameTime);
        Vector3d projectedView = camera.getPos();

        return new Vec3d(projectedView.x, projectedView.y, projectedView.z);
    }

    @Override
    public Color getFogColor(float partialTicks)
    {
        float[] colorValues = new float[4];
        GL15.glGetFloatv(GL15.GL_FOG_COLOR, colorValues);
        return new Color(
            Math.max(0f, Math.min(colorValues[0], 1f)), // r
            Math.max(0f, Math.min(colorValues[1], 1f)), // g
            Math.max(0f, Math.min(colorValues[2], 1f)), // b
            Math.max(0f, Math.min(colorValues[3], 1f))  // a
        );
        // TODO ?
    }
    // getSpecialFogColor() is the same as getFogColor()

    @Override
    public Color getSkyColor()
    {
        if (!MC.theWorld.provider.hasNoSky)
        {
            float frameTime = ((IMixinMinecraft)Minecraft.getMinecraft()).getTimer().renderPartialTicks;
            Vec3 color = MC.theWorld.provider.getSkyColor(MC.renderViewEntity, frameTime);

            return new Color((float) color.xCoord, (float) color.yCoord, (float) color.zCoord);
        }
        else
        {
            return new Color(0, 0, 0);
        }
    }

    @Override
    public double getFov(float partialTicks)
    {
        return MC.gameSettings.fovSetting;
    }

    /** Measured in chunks */
    @Override
    public int getRenderDistance()
    {
        if (ForgeMain.angelicaCompat != null) {
            return MC.gameSettings.renderDistanceChunks - 2;
        }
        return MC.gameSettings.renderDistanceChunks;
    }

    @Override
    public int getTargetFramebufferViewportWidth()
    {
        return MC.getFramebuffer().framebufferWidth;
    }
    @Override
    public int getTargetFramebufferViewportHeight()
    {
        return MC.getFramebuffer().framebufferHeight;
    }

    @Override
    public boolean mcRendersToFrameBuffer() {
        return true;
    }

    @Override
    public boolean runningLegacyOpenGL() {
        return false;
    }


    @Override
    public int getTargetFramebuffer()
    {
        return Minecraft.getMinecraft().getFramebuffer().framebufferObject;
    }

    @Override
    public void clearTargetFrameBuffer() { this.finalLevelFrameBufferId = -1; }

    @Override
    public int getDepthTextureId() {
        final Framebuffer framebuffer = Minecraft.getMinecraft().getFramebuffer();

        if (ForgeMain.angelicaCompat != null) {
            return ForgeMain.angelicaCompat.getDepthTextureId(framebuffer);
        }

        return framebuffer.depthBuffer;
    }

    @Override
    public int getColorTextureId() {
        int texture = Minecraft.getMinecraft().getFramebuffer().framebufferTexture;
        return texture;
    }

    @Override
    public ILightMapWrapper getLightmapWrapper(ILevelWrapper level) { return new LightMapWrapper(); }

    @Override
    public float getShade(EDhDirection lodDirection)
    {
        EDhApiLodShading lodShading = Config.Client.Advanced.Graphics.Quality.lodShading.get();
        switch (lodShading)
        {
            default:
            case AUTO:
            case ENABLED:
                switch (lodDirection)
                {
                    case DOWN:
                        return 0.5F;
                    default:
                    case UP:
                        return 1.0F;
                    case NORTH:
                    case SOUTH:
                        return 0.8F;
                    case WEST:
                    case EAST:
                        return 0.6F;
                }

            case DISABLED:
                return 1.0F;
        }
    }

    @Override
    public boolean isFogStateSpecial()
    {
        boolean isBlind = this.playerHasBlindingEffect();
        //isBlind |= fluidState.is(FluidTags.WATER);
        //isBlind |= fluidState.is(FluidTags.LAVA);
        /* TODO */
        return isBlind;
    }

    /**
     * It's better to use {@link MinecraftRenderWrapper#setLightmapId(int, IClientLevelWrapper)} if possible,
     * however old MC versions don't support it.
     */
    /*
    public void updateLightmap(NativeImage lightPixels, IClientLevelWrapper level)
    {
        // Using ClientLevelWrapper as the key would be better, but we don't have a consistent way to create the same
        // object for the same MC level and/or the same hash,
        // so this will have to do for now
        IDimensionTypeWrapper dimensionType = level.getDimensionType();

        LightMapWrapper wrapper = this.lightmapByDimensionType.computeIfAbsent(dimensionType, (dimType) -> new LightMapWrapper());
        wrapper.uploadLightmap(lightPixels);
    }
    public void setLightmapId(int tetxureId, IClientLevelWrapper level)
    {
        // Using ClientLevelWrapper as the key would be better, but we don't have a consistent way to create the same
        // object for the same MC level and/or the same hash,
        // so this will have to do for now
        IDimensionTypeWrapper dimensionType = level.getDimensionType();

        LightMapWrapper wrapper = this.lightmapByDimensionType.computeIfAbsent(dimensionType, (dimType) -> new LightMapWrapper());
        wrapper.setLightmapId(tetxureId);
    }*/

}
