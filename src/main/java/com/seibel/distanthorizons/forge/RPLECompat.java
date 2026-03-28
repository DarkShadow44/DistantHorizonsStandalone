package com.seibel.distanthorizons.forge;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.DynamicTexture;

import com.falsepattern.rple.api.client.RPLELightMapAPI;
import com.falsepattern.rple.api.common.ServerColorHelper;
import com.falsepattern.rple.api.common.block.RPLEBlock;

public class RPLECompat {

    DynamicTexture lightmapTexture;
    int[] lightmapColors;

    public RPLECompat() {
        lightmapTexture = new DynamicTexture(16, 16);
        lightmapColors = lightmapTexture.getTextureData();
    }

    public int getTextureId() {
        return lightmapTexture.getGlTextureId();
    }

    public int getColor(Block block, int meta) {
        short color = RPLEBlock.of(block)
            .rple$getBrightnessColor(meta);
        return ServerColorHelper.lightValueFromRGB16(color);
    }

    public void updateLightmap() {
        RPLELightMapAPI.getMixedLightMapData(lightmapColors);
        lightmapTexture.updateDynamicTexture();
    }
}
