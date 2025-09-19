package com.seibel.distanthorizons.forge;

import com.falsepattern.rple.api.common.ServerColorHelper;
import com.falsepattern.rple.api.common.block.RPLEBlock;
import com.falsepattern.rple.internal.client.lightmap.LightMapPipeline;
import com.seibel.distanthorizons.common.wrappers.block.FakeBlockState;
import net.minecraft.client.renderer.texture.DynamicTexture;

import java.lang.reflect.Field;

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

    public int getColor(FakeBlockState blockState) {
        short color = RPLEBlock.of(blockState.block).rple$getBrightnessColor(blockState.meta);
        return ServerColorHelper.lightValueFromRGB16(color);
    }

    public void updateLightmap(float partialTicks) {
        int[] pixels = LightMapPipeline.lightMapPipeline().update(partialTicks);
        System.arraycopy(pixels, 0, lightmapColors, 0, pixels.length);
        lightmapTexture.updateDynamicTexture();
    }
}
