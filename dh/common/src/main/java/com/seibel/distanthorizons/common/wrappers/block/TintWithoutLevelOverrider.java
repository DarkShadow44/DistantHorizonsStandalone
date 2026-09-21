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

package com.seibel.distanthorizons.common.wrappers.block;
#if MC_VER > MC_1_12_2
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

#if MC_VER <= MC_1_21_11
#else
import net.minecraft.world.level.CardinalLighting;
#endif


public class TintWithoutLevelOverrider extends AbstractDhTintGetter
{
	
	//=============//
	// constructor //
	//=============//
	
	public TintWithoutLevelOverrider()
	{ }
	
	
	
	//=========//
	// methods //
	//=========//
	
	#if MC_VER <= MC_1_21_11
	@Override
	public float getShade(Direction direction, boolean shade)
	{ throw new UnsupportedOperationException("ERROR: getShade() called on TintWithoutLevelOverrider. Object is for tinting only."); }
	#endif
	
	@Override
	public LevelLightEngine getLightEngine()
	{ throw new UnsupportedOperationException("ERROR: getLightEngine() called on TintWithoutLevelOverrider. Object is for tinting only."); }
	@Nullable
	@Override
	public BlockEntity getBlockEntity(BlockPos pos)
	{ throw new UnsupportedOperationException("ERROR: getBlockEntity() called on TintWithoutLevelOverrider. Object is for tinting only."); }
	
	@Override
	public BlockState getBlockState(BlockPos pos)
	{ throw new UnsupportedOperationException("ERROR: getBlockState() called on TintWithoutLevelOverrider. Object is for tinting only."); }
	@Override
	public FluidState getFluidState(BlockPos pos)
	{ throw new UnsupportedOperationException("ERROR: getFluidState() called on TintWithoutLevelOverrider. Object is for tinting only."); }
	
	
	//==============//
	// post MC 1.17 //
	//==============//
	
	#if MC_VER >= MC_1_17_1
	
	@Override
	public int getHeight()
	{ throw new UnsupportedOperationException("ERROR: getHeight() called on TintWithoutLevelOverrider. Object is for tinting only."); }
	
	#if MC_VER < MC_1_21_3
	@Override
	public int getMinBuildHeight() 
	{ throw new UnsupportedOperationException("ERROR: getMinBuildHeight() called on TintWithoutLevelSmoothOverrider. Object is for tinting only."); }
	#else
	@Override
	public int getMinY()
	{ throw new UnsupportedOperationException("ERROR: getMinY() called on TintWithoutLevelOverrider. Object is for tinting only."); }
	#endif
	
	#endif
	
	
	//=================//
	// post MC 1.21.11 //
	//=================//
	
	#if MC_VER <= MC_1_21_11
	#else
	@Override 
	public CardinalLighting cardinalLighting()
	{ throw new UnsupportedOperationException("ERROR: cardinalLighting() called on TintWithoutLevelOverrider. Object is for tinting only."); }
	#endif
	
	
	
}
#endif