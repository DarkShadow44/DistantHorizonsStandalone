
package com.seibel.distanthorizons.common.wrappers.worldGeneration;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

import com.mitchej123.hodgepodge.SimulationDistanceHelper;

public class HodgePodgeCompat {

    public static void preventChunkSimulation(World world, int x, int z, boolean prevent) {
        SimulationDistanceHelper.preventChunkSimulation(world, ChunkCoordIntPair.chunkXZ2Int(x, z), prevent);
    }
}
