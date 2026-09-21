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

package com.seibel.distanthorizons.forge;

import com.seibel.distanthorizons.common.AbstractModInitializer;
import com.seibel.distanthorizons.common.util.ProxyUtil;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftRenderWrapper;
import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.api.internal.ServerApi;
import com.seibel.distanthorizons.core.api.internal.SharedApi;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.network.messages.AbstractNetworkMessage;
import com.seibel.distanthorizons.core.util.threading.ThreadPoolUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;

import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IPluginPacketSender;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IServerPlayerWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import net.minecraft.world.level.LevelAccessor;

import net.minecraft.client.multiplayer.ClientLevel;
#if MC_VER < MC_1_19_2
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
#else
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
#endif

#if MC_VER >= MC_1_18_2
import net.minecraftforge.client.event.RenderLevelStageEvent;
#endif
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.level.chunk.ChunkAccess;

import net.minecraftforge.common.MinecraftForge;
import com.seibel.distanthorizons.core.logging.DhLogger;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.opengl.GL33;

import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * This handles all events sent to the client,
 * and is the starting point for most of the mod.
 *
 * @author James_Seibel
 * @version 2023-7-27
 */
public class ForgeClientProxy implements AbstractModInitializer.IEventProxy
{
	private static final IMinecraftClientWrapper MC = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	private static final ForgePluginPacketSender PACKET_SENDER = (ForgePluginPacketSender) SingletonInjector.INSTANCE.get(IPluginPacketSender.class);
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	
	#if MC_VER < MC_1_19_2
	private static LevelAccessor GetEventLevel(WorldEvent e) { return e.getWorld(); }
	#else
	private static LevelAccessor GetEventLevel(LevelEvent e) { return e.getLevel(); }
	#endif
	
	
	
	@Override
	public void registerEvents()
	{
		MinecraftForge.EVENT_BUS.register(this);
		
		// handles singleplayer, LAN, and connecting to a server
		PACKET_SENDER.setPacketHandler((IServerPlayerWrapper player, @NotNull AbstractNetworkMessage message) ->
		{
			ClientApi.INSTANCE.pluginMessageReceived(message);
			ServerApi.INSTANCE.pluginMessageReceived(player, message);
		});
	}
	
	
	//==============//
	// chunk events //
	//==============//
	
	@SubscribeEvent
	public void rightClickBlockEvent(PlayerInteractEvent.RightClickBlock event)
	{
		if (MC.clientConnectedToDedicatedServer())
		{
			#if MC_VER < MC_1_19_2
			LevelAccessor level = event.getWorld();
			#else
			LevelAccessor level = event.getLevel();
			#endif
			
			ILevelWrapper wrappedLevel = ProxyUtil.getLevelWrapper(level);
			if (SharedApi.isChunkAtBlockPosAlreadyUpdating(wrappedLevel, event.getPos().getX(), event.getPos().getZ()))
			{
				return;
			}
			
			AbstractExecutorService executor = ThreadPoolUtil.getFileHandlerExecutor();
			if (executor != null)
			{
				executor.execute(() ->
				{
					ChunkAccess chunk = level.getChunk(event.getPos());
					SharedApi.INSTANCE.applyChunkUpdate(
						new ChunkWrapper(chunk, wrappedLevel), 
						wrappedLevel, 
						true
					);
				});
			}
		}
	}
	@SubscribeEvent
	public void leftClickBlockEvent(PlayerInteractEvent.LeftClickBlock event)
	{
		if (MC.clientConnectedToDedicatedServer())
		{
			#if MC_VER < MC_1_19_2
			LevelAccessor level = event.getWorld();
			#else
			LevelAccessor level = event.getLevel();
			#endif
			
			ILevelWrapper wrappedLevel = ProxyUtil.getLevelWrapper(level);
			if (SharedApi.isChunkAtBlockPosAlreadyUpdating(wrappedLevel, event.getPos().getX(), event.getPos().getZ()))
			{
				return;
			}
			
			AbstractExecutorService executor = ThreadPoolUtil.getFileHandlerExecutor();
			if (executor != null)
			{
				executor.execute(() ->
				{
					ChunkAccess chunk = level.getChunk(event.getPos());
					SharedApi.INSTANCE.applyChunkUpdate(
						new ChunkWrapper(chunk, wrappedLevel), 
						wrappedLevel,
						true
					);
				});
			}
		}
	}

	@SubscribeEvent
	public void clientChunkLoadEvent(ChunkEvent.Load event)
	{
		if (MC.clientConnectedToDedicatedServer())
		{
			ILevelWrapper wrappedLevel = ProxyUtil.getLevelWrapper(GetEventLevel(event));
			SharedApi.INSTANCE.applyChunkUpdate(
				new ChunkWrapper(event.getChunk(), wrappedLevel), 
				wrappedLevel, 
				true
			);
		}
	}
	
	
	
	//==============//
	// key bindings //
	//==============//
	
	@SubscribeEvent
	public void registerKeyBindings(#if MC_VER < MC_1_19_2 InputEvent.KeyInputEvent #else InputEvent.Key #endif event)
	{
		if (Minecraft.getInstance().player == null)
		{
			return;
		}
		if (event.getAction() != GLFW.GLFW_PRESS)
		{
			return;
		}
		
		ClientApi.INSTANCE.keyPressedEvent(event.getKey());
	}
	
	
	//===========//
	// rendering //
	//===========//
	
	@SubscribeEvent
	#if MC_VER >= MC_1_18_2
	public void afterLevelRenderEvent(RenderLevelStageEvent event)
	#else
	public void afterLevelRenderEvent(TickEvent.RenderTickEvent event)
	#endif
	{
		#if MC_VER >= MC_1_20_1
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL)
		#elif MC_VER >= MC_1_18_2
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS)
		#else
		if (event.type.equals(TickEvent.RenderTickEvent.Type.RENDER))
		#endif
		{
			try
			{
				// should generally only need to be set once per game session
				// allows DH to render directly to Optifine's level frame buffer,
				// allowing better shader support
				MinecraftRenderWrapper.INSTANCE.finalLevelFrameBufferId = GL33.glGetInteger(GL33.GL_FRAMEBUFFER_BINDING);
			}
			catch (Exception | Error e)
			{
				LOGGER.error("Unexpected error in afterLevelRenderEvent: "+e.getMessage(), e);
			}
		}
	}
	
	
}
