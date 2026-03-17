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

package com.seibel.distanthorizons.core.wrapperInterfaces.minecraft;

import java.util.ArrayList;
import java.util.UUID;

import com.seibel.distanthorizons.core.enums.EDhDirection;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.render.glObject.GLProxy;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.distanthorizons.coreapi.ModInfo;
import com.seibel.distanthorizons.coreapi.interfaces.dependencyInjection.IBindable;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import org.apache.logging.log4j.Level;

public interface IMinecraftClientWrapper extends IBindable
{
	//======================//
	// multiplayer handling //
	//======================//
	
	boolean hasSinglePlayerServer();
	boolean clientConnectedToDedicatedServer();
	/** for use with the Replay mod */
	boolean connectedToReplay();
	
	String getCurrentServerName();
	String getCurrentServerIp();
	String getCurrentServerVersion();
	
	
	
	//=================//
	// player handling //
	//=================//
	
	boolean playerExists();
	
	/** @return (0,0,0) if no player is loaded */
	DhBlockPos getPlayerBlockPos();
	
	/** @return (0,0) if no player is loaded */
	DhChunkPos getPlayerChunkPos();
	
	
	
	//================//
	// level handling //
	//================//
	
	/**
	 * Returns the level the client is currently in. <br>
	 * Returns null if the client isn't in a level.
	 */
	IClientLevelWrapper getWrappedClientLevel();
	/**
	 * Returns the level the client is currently in. <br>
	 * Returns null if the client isn't in a level.
	 */
	IClientLevelWrapper getWrappedClientLevel(boolean bypassLevelKeyManager);
	
	
	
	//===========//
	// messaging //
	//===========//
	
	void sendChatMessage(String string);
	
	/** 
	 * Will default to sending a chat message if not supported by 
	 * the current MC version (1.19.2 and older).
	 */
	void sendOverlayMessage(String string);
	
	
	
	//==========================//
	// vanilla option overrides //
	//==========================//
	
	void disableVanillaClouds();
	
	void disableVanillaChunkFadeIn();
	
	
	
	//======//
	// misc //
	//======//
	
	IProfilerWrapper getProfiler();
	
	/**
	 * Crashes Minecraft, displaying the given errorMessage <br> <br>
	 * In the following format: <br>
	 *
	 * The game crashed whilst <strong>errorMessage</strong>  <br>
	 * Error: <strong>ExceptionClass: exceptionErrorMessage</strong>  <br>
	 * Exit Code: -1  <br>
	 */
	void crashMinecraft(String errorMessage, Throwable exception);
	
	
	
	//=============//
	// mod support //
	//=============//
	
	/** used for Optifine */
	Object getOptionsObject();
	
	
	
}
