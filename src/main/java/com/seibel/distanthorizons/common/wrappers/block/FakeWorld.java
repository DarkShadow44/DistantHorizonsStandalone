package com.seibel.distanthorizons.common.wrappers.block;

import net.minecraft.block.Block;
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
    private Block block;
    private int meta;

    public void update(IBlockAccess real, int blockX, int blockY, int blockZ, Block block, int meta) {
        this.real = real;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.block = block;
        this.meta = meta;
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        if (x == blockX && y == blockY && z == blockZ) {
            return block;
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
            return meta;
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
        return real.getBiomeGenForCoords(x, z);
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
