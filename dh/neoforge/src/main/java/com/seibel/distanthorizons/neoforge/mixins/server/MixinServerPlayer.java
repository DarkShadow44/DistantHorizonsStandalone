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

package com.seibel.distanthorizons.neoforge.mixins.server;

import com.seibel.distanthorizons.common.wrappers.misc.IMixinServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

#if MC_VER < MC_1_20_6
import net.minecraft.world.level.portal.DimensionTransition;
#elif MC_VER < MC_1_21_1
import net.neoforged.neoforge.common.util.ITeleporter;
#elif MC_VER < MC_1_21_3
import net.minecraft.world.level.portal.DimensionTransition;
#else
import net.minecraft.world.level.portal.TeleportTransition;
#endif

@Mixin(ServerPlayer.class)
public class MixinServerPlayer implements IMixinServerPlayer
{
	@Unique
	@Nullable
	private ServerLevel distantHorizons$dimensionChangeDestination;
	
	
	
	@Unique
	@Override
	@Nullable
	public ServerLevel distantHorizons$getDimensionChangeDestination() 
	{ return this.distantHorizons$dimensionChangeDestination; }
	
	#if MC_VER < MC_1_20_6
	@Inject(at = @At("HEAD"), method = "changeDimension")
	public void changeDimension(ServerLevel destination, CallbackInfoReturnable<Entity> cir)
	{ this.distantHorizons$dimensionChangeDestination = destination; }
	#elif MC_VER < MC_1_21_1
	@Inject(at = @At("HEAD"), method = "changeDimension")
	public void changeDimension(ServerLevel destination, ITeleporter teleporter, CallbackInfoReturnable<Entity> cir)
	{ this.distantHorizons$dimensionChangeDestination = destination; }
	#elif MC_VER < MC_1_21_3
	@Inject(at = @At("HEAD"), method = "changeDimension")
	public void changeDimension(DimensionTransition dimensionTransition, CallbackInfoReturnable<Entity> cir)
	{ this.distantHorizons$dimensionChangeDestination = dimensionTransition.newLevel(); }
	#else
	@Inject(at = @At("HEAD"), method = "teleport")
	public void changeDimension(TeleportTransition teleportTransition, CallbackInfoReturnable<ServerPlayer> cir)
	{ this.distantHorizons$dimensionChangeDestination = teleportTransition.newLevel(); }
	#endif
	
	#if MC_VER < MC_1_20_1
	@Inject(at = @At("RETURN"), method = "setLevel")
	public void setLevel(ServerLevel level, CallbackInfo ci)
	#else
	@Inject(at = @At("RETURN"), method = "setServerLevel")
	public void setServerLevel(ServerLevel level, CallbackInfo ci)
	#endif
	{
		this.distantHorizons$dimensionChangeDestination = null;
	}
	
}