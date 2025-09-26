package com.seibel.distanthorizons.mixin;

import net.minecraft.world.ChunkCache;
import net.minecraft.world.chunk.Chunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkCache.class)
public interface MixinChunkCache_SideFacingUnloaded {

    @Accessor
    Chunk[][] getChunkArray();

    @Accessor
    int getChunkX();

    @Accessor
    int getChunkZ();
}
