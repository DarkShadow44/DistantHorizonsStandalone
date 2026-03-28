package com.seibel.distanthorizons.mixin;

import net.minecraft.world.biome.BiomeGenBase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.seibel.distanthorizons.forge.BiomeHandler;

@Mixin(BiomeGenBase.class)
public class MixinBiomeGenBase {

    @Inject(method = "<init>(IZ)V", at = @At("TAIL"))
    private void captureBiome(int p_i1971_1_, boolean register, CallbackInfo ci) {
        BiomeHandler.addBiome((BiomeGenBase) (Object) this);
    }
}
