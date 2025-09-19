package com.seibel.distanthorizons.common.wrappers.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

public class FakeWorld implements IBlockAccess {

    private IBlockAccess real;
    private int blockX;
    private int blockY;
    private int blockZ;
    private FakeBlockState blockState;
    private BiomeGenBase biome;

    public void update(IBlockAccess real, BiomeGenBase biome, int blockX, int blockY, int blockZ, FakeBlockState blockState) {
        this.real = real;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.blockState = blockState;
        this.biome = biome;
        if (this.biome == null) {
            this.biome = BiomeGenBase.plains; // Fallback, at least we don't crash
        }
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        if (x == blockX && y == blockY && z == blockZ) {
            return blockState.block;
        }
        return Blocks.air;
    }

    @Override
    public TileEntity getTileEntity(int x, int y, int z) {
        return null;
    }

    @Override
    public int getLightBrightnessForSkyBlocks(int x, int y, int z, int p_72802_4_) {
        return 0;
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        if (x == blockX && y == blockY && z == blockZ) {
            return blockState.meta;
        }
        if (blockState.block instanceof BlockDoublePlant) {
            return 2; // TODO
        }
        return 0;
    }

    @Override
    public int isBlockProvidingPowerTo(int x, int y, int z, int directionIn) {
        return 0;
    }

    @Override
    public boolean isAirBlock(int x, int y, int z) {
        return false;
    }

    @Override
    public BiomeGenBase getBiomeGenForCoords(int x, int z) {
        return biome; // Not 100% accurate since grass samples the surrounding blocks, but good enough for now
    }

    @Override
    public int getHeight() {
        return real.getHeight();
    }

    @Override
    public boolean extendedLevelsInChunkCache() {
        return real.extendedLevelsInChunkCache();
    }

    @Override
    public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean _default) {
        throw new RuntimeException();
    }
}
