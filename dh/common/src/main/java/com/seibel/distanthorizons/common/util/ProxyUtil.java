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

package com.seibel.distanthorizons.common.util;

import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.common.wrappers.world.ServerLevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
#if MC_VER <= MC_1_12_2
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
#else
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
#endif
public class ProxyUtil
{
	
	public static ILevelWrapper getLevelWrapper(
		#if MC_VER <= MC_1_12_2 World #else LevelAccessor #endif level
		)
	{
		ILevelWrapper levelWrapper;
		if (level instanceof #if MC_VER <= MC_1_12_2 WorldServer #else ServerLevel #endif)
		{
			levelWrapper = ServerLevelWrapper.getWrapper(
				#if MC_VER <= MC_1_12_2 (WorldServer) #else (ServerLevel) #endif level
			);
		}
		else
		{
			levelWrapper = ClientLevelWrapper.getWrapper(
				#if MC_VER <= MC_1_12_2 (WorldClient) #else (ClientLevel) #endif level
			);
		}
		
		return levelWrapper;
	}
	
}
