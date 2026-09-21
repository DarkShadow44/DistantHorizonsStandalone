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


package com.seibel.distanthorizons.common.wrappers.worldGeneration.params;

#if MC_VER > MC_1_12_2
import com.seibel.distanthorizons.common.wrappers.worldGeneration.mimicObject.WorldGenStructFeatManager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
#if MC_VER >= MC_1_18_2
import net.minecraft.world.level.levelgen.structure.StructureCheck;
#endif

public final class ThreadWorldGenParams
{
	private static final ThreadLocal<ThreadWorldGenParams> LOCAL_PARAM_REF = new ThreadLocal<>();
	
	
	final ServerLevel level;
	public WorldGenStructFeatManager structFeatManager = null;
	
	#if MC_VER >= MC_1_18_2
	public StructureCheck structCheck;
	#endif
	
	// used for some older MC versions
	private static GlobalWorldGenParams previousGlobalWorldGenParams = null;
	
	
	
	//=============//
	// constructor //
	//=============//
	
	public static ThreadWorldGenParams getOrMake(GlobalWorldGenParams globalParams)
	{
		ThreadWorldGenParams threadParam = LOCAL_PARAM_REF.get();
		if (threadParam != null
			&& threadParam.level == globalParams.mcServerLevel)
		{
			return threadParam;
		}
		
		threadParam = new ThreadWorldGenParams(globalParams);
		LOCAL_PARAM_REF.set(threadParam);
		return threadParam;
	}
	
	private ThreadWorldGenParams(GlobalWorldGenParams param)
	{
		previousGlobalWorldGenParams = param;
		
		this.level = param.mcServerLevel;
		
		#if MC_VER < MC_1_18_2
		this.structFeatManager = new WorldGenStructFeatManager(param.worldGenSettings, this.level);
		#elif MC_VER < MC_1_19_2
		this.structCheck = this.createStructureCheck(param);
		#else
		this.structCheck = new StructureCheck(param.chunkScanner, param.registry, param.structures,
				param.mcServerLevel.dimension(), param.generator, param.randomState, this.level, param.generator.getBiomeSource(), param.worldSeed,
				param.dataFixer);
		#endif
	}
	
	
	
	//==========//
	// builders //
	//==========//
	
	public void makeStructFeatManager(WorldGenLevel genLevel, GlobalWorldGenParams param)
	{
		#if MC_VER < MC_1_18_2
		this.structFeatManager = new WorldGenStructFeatManager(param.worldGenSettings, genLevel);
		#elif MC_VER < MC_1_19_4
		this.structFeatManager = new WorldGenStructFeatManager(param.worldGenSettings, genLevel, this.structCheck);
		#else
		this.structFeatManager = new WorldGenStructFeatManager(param.worldOptions, genLevel, this.structCheck);
		#endif
	}
	
	#if MC_VER < MC_1_18_2
	#elif MC_VER < MC_1_19_2
	public void recreateStructureCheck()
	{
		if (previousGlobalWorldGenParams != null)
		{
			this.structCheck = this.createStructureCheck(previousGlobalWorldGenParams);
		}
	}
	private StructureCheck createStructureCheck(GlobalWorldGenParams param)
	{
		return new StructureCheck(param.chunkScanner, param.registry, param.structures,
				param.mcServerLevel.dimension(), param.generator, this.level, param.generator.getBiomeSource(), param.worldSeed,
				param.dataFixer);
	}
	#else
	public void recreateStructureCheck() { /* do nothing */ }	
	#endif
	
	
	
}
#endif