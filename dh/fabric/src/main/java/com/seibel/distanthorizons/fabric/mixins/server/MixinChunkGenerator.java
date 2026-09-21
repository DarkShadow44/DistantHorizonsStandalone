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

package com.seibel.distanthorizons.fabric.mixins.server;

import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.world.level.chunk.ChunkGenerator;

#if MC_VER < MC_1_18_2
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.WorldgenRandom;

@Mixin(ChunkGenerator.class)
public class MixinChunkGenerator
{
	@Redirect(method = "applyBiomeDecoration", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/biome/Biome;generate(Lnet/minecraft/world/level/StructureFeatureManager;"
					+ "Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/server/level/WorldGenRegion;J"
					+ "Lnet/minecraft/world/level/levelgen/WorldgenRandom;Lnet/minecraft/core/BlockPos;)V"
	
	))
	private void wrapBiomeGenerateCall(
			Biome biome, StructureFeatureManager structFeatManager, ChunkGenerator generator,
			WorldGenRegion genRegion, long l, WorldgenRandom random, BlockPos pos)
	{
		synchronized (ChunkGenerator.class)
		{
			biome.generate(structFeatManager, (ChunkGenerator) (Object) this, genRegion, l, random, pos);
		}
	}
	
}

#else
@Mixin(ChunkGenerator.class)
public class MixinChunkGenerator { }
#endif