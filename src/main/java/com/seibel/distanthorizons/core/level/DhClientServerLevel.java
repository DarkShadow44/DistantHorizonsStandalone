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

package com.seibel.distanthorizons.core.level;

import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.file.structure.ISaveStructure;
import com.seibel.distanthorizons.core.multiplayer.server.ServerPlayerStateManager;
import com.seibel.distanthorizons.core.render.RenderBufferHandler;
import com.seibel.distanthorizons.core.render.renderer.DebugRenderer;
import com.seibel.distanthorizons.core.render.renderer.generic.GenericObjectRenderer;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IServerLevelWrapper;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/** The level used for a singleplayer world */
public class DhClientServerLevel extends AbstractDhServerLevel implements IDhClientLevel
{
	private static final IMinecraftClientWrapper MC_CLIENT = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	
	public final ClientLevelModule clientside;
	
	
	
	//=============//
	// constructor //
	//=============//
	
	public DhClientServerLevel(
		ISaveStructure saveStructure, 
		IServerLevelWrapper serverLevelWrapper, 
		ServerPlayerStateManager serverPlayerStateManager
		) throws SQLException, IOException
	{
		super(saveStructure, serverLevelWrapper, serverPlayerStateManager, false);
		
		this.serverLevelWrapper.setDhLevel(this);
		this.clientside = new ClientLevelModule(this);
		this.runRepoReliantSetup();
	}
	
	
	
	//==============//
	// tick methods //
	//==============//
	
	@Override
	public void clientTick() { this.clientside.clientTick(); }
	
	
	
	//========//
	// render //
	//========//
	
	public void startRenderer() { this.clientside.startRenderer(); }
	
	public void stopRenderer() { this.clientside.stopRenderer(); }
	
	
	
	//================//
	// level handling //
	//================//
	
	@Nullable
	@Override
	public IClientLevelWrapper getClientLevelWrapper() { return MC_CLIENT.getWrappedClientLevel(); }
	
	@Override
	public void clearRenderCache() { this.clientside.clearRenderCache(); }
	
	
	
	//===========//
	// debugging //
	//===========//
	
	@Override
	public void addDebugMenuStringsToList(List<String> messageList)
	{
		// header
		String dimName = this.serverLevelWrapper.getDhIdentifier();
		boolean rendering = this.clientside.isRendering();
		messageList.add("["+dimName+"] rendering: "+(rendering ? "yes" : "no"));
		
		super.addDebugMenuStringsToList(messageList);
	}
	
	
	@Override
	public GenericObjectRenderer getGenericRenderer() { return this.clientside.genericRenderer; }
	@Override
	public RenderBufferHandler getRenderBufferHandler()
	{
		ClientLevelModule.ClientRenderState renderState = this.clientside.ClientRenderStateRef.get();
		return (renderState != null) ? renderState.renderBufferHandler : null;
	}
	
	
	
	//===============//
	// data handling //
	//===============//
	
	@Override
	public void onWorldGenTaskComplete(long pos)
	{
		super.onWorldGenTaskComplete(pos);
		this.clientside.reloadPos(pos);
	}
	
	
	
	//================//
	// base overrides //
	//================//
	
	@Override
	public String toString() { return "DhClientServerLevel{"+this.serverLevelWrapper.getKeyedLevelDimensionName()+"}"; }
	
	@Override
	public void close()
	{
		this.clientside.close();
		super.close();
		this.serverside.close();
		LOGGER.info("Closed " + this.getClass().getSimpleName() + " for " + this.getServerLevelWrapper());
	}
	
}
