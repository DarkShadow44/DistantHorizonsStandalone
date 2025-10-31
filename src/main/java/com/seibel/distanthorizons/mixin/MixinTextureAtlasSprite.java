package com.seibel.distanthorizons.mixin;

import com.seibel.distanthorizons.interfaces.IMixinTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.image.BufferedImage;
import java.util.List;

@Mixin(TextureAtlasSprite.class)
public class MixinTextureAtlasSprite implements IMixinTextureAtlasSprite {

    @Shadow
    protected List<int[][]> framesTextureData;

    @Unique
    private int[] distanthorizons$spriteData;

    @Unique
    private boolean distanthorizons$dataLoaded;

    @Inject(method="loadSprite", at = @At("RETURN"))
    private void injectLoadSprite(BufferedImage[] bufferedImages, AnimationMetadataSection p_147964_2_, boolean p_147964_3_, CallbackInfo ci)
    {
        distanthorizons$loadData();
    }

    public void distanthorizons$loadData() {
        if (framesTextureData.isEmpty()) {
            return;
        }

        int[][] frameData = framesTextureData.get(0);
        if (frameData == null) {
            return;
        }
        int[] data = frameData[0];
        if (data == null) {
            return;
        }
        distanthorizons$spriteData = new int[data.length];
        for(int i = 0; i < data.length; i++)
        {
            int pixel = data[i];
            int b = pixel & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int r = (pixel >> 16) & 0xFF;
            int a = (pixel >> 24) & 0xFF;
            distanthorizons$spriteData[i] = r | (g << 8) | (b << 16) | (a << 24);
        }
    }

    @Override
    public int[] distanthorizons$getSpriteData()
    {
        return distanthorizons$spriteData;
    }
}
