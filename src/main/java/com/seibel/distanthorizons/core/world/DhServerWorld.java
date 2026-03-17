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

package com.seibel.distanthorizons.core.world;

import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.enums.MinecraftTextFormat;
import com.seibel.distanthorizons.core.level.DhServerLevel;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IServerLevelWrapper;
import org.jetbrains.annotations.NotNull;

public class DhServerWorld extends AbstractDhServerWorld<DhServerLevel>
{
	//==============//
	// constructors //
	//==============//
	
	public DhServerWorld()
	{
		super(EWorldEnvironment.SERVER_ONLY);
		LOGGER.info("Started ["+DhServerWorld.class.getSimpleName()+"] of type ["+this.environment+"].");
	}
	
	
	
	//================//
	// level handling //
	//================//
	
	@Override
	public DhServerLevel getOrLoadLevel(@NotNull ILevelWrapper wrapper)
	{
		if (!(wrapper instanceof IServerLevelWrapper))
		{
			return null;
		}
		
		return this.dhLevelByLevelWrapper.computeIfAbsent(wrapper, 
			(serverLevelWrapper) ->
			{
				try
				{
					return new DhServerLevel(this.saveStructure, (IServerLevelWrapper) serverLevelWrapper, this.getServerPlayerStateManager());
				}
				catch (Exception e)
				{
					LOGGER.fatal("Failed to load server level, error: ["+e.getMessage()+"].", e);
					
					ClientApi.INSTANCE.showChatMessageNextFrame(
						MinecraftTextFormat.RED + "Distant Horizons: Server level loading failed." + MinecraftTextFormat.CLEAR_FORMATTING + "\n" +
						"Unable to load level ["+serverLevelWrapper.getDhIdentifier()+"], LODs may not appear. See log for more information.");
					
					return null;
				}
			});
	}
	
	@Override
	public void unloadLevel(@NotNull ILevelWrapper wrapper)
	{
		if (!(wrapper instanceof IServerLevelWrapper))
		{
			return;
		}
		
		if (this.dhLevelByLevelWrapper.containsKey(wrapper))
		{
			DhServerLevel level = this.dhLevelByLevelWrapper.get(wrapper);
			wrapper.onUnload();
			this.dhLevelByLevelWrapper.remove(wrapper).close();
		}
	}
	
}
