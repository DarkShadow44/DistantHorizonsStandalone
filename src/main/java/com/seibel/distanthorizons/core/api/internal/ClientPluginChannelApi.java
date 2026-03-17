package com.seibel.distanthorizons.core.api.internal;

import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.level.IKeyedClientLevelManager;
import com.seibel.distanthorizons.core.level.IServerKeyedClientLevel;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.network.event.internal.CloseInternalEvent;
import com.seibel.distanthorizons.core.network.messages.base.LevelInitMessage;
import com.seibel.distanthorizons.core.network.session.NetworkSession;
import com.seibel.distanthorizons.core.render.RenderThreadTaskHandler;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * This class is used to manage the level keys.
 */
public class ClientPluginChannelApi
{
	private static final DhLogger LOGGER = new DhLoggerBuilder()
			.fileLevelConfig(Config.Common.Logging.logNetworkEventToFile)
			.build();  
			
	private static final IMinecraftClientWrapper MC = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	private static final IKeyedClientLevelManager KEYED_CLIENT_LEVEL_MANAGER = SingletonInjector.INSTANCE.get(IKeyedClientLevelManager.class);
	
	private final Consumer<IServerKeyedClientLevel> levelLoadHandler;
	private final Consumer<IClientLevelWrapper> levelUnloadHandler;
	
	@Nullable
	public NetworkSession networkSession;
	
	
	
	//=============//
	// constructor //
	//=============//
	
	public ClientPluginChannelApi(Consumer<IServerKeyedClientLevel> levelLoadHandler, Consumer<IClientLevelWrapper> levelUnloadHandler)
	{
		this.levelLoadHandler = levelLoadHandler;
		this.levelUnloadHandler = levelUnloadHandler;
	}
	
	
	
	//============//
	// properties //
	//============//
	
	/** @return true if the level loading is handled by the server */
	public boolean allowLevelLoading(IClientLevelWrapper level)
	{
		return (KEYED_CLIENT_LEVEL_MANAGER.isEnabled() && level instanceof IServerKeyedClientLevel)
				|| !KEYED_CLIENT_LEVEL_MANAGER.isEnabled();
	}
	
	
	
	//================//
	// network events //
	//================//
	
	/** fired when this client connects to a server with DH support */
	public void onJoinServer(@NotNull NetworkSession networkSession)
	{
		Objects.requireNonNull(networkSession);
		this.networkSession = networkSession;
		this.networkSession.registerHandler(LevelInitMessage.class, this::onLevelInitMessage);
		this.networkSession.registerHandler(CloseInternalEvent.class, this::onClose);
	}
	
	private void onLevelInitMessage(LevelInitMessage msg)
	{
		if (!msg.serverKey.isEmpty() && !msg.serverKey.matches(LevelInitMessage.SERVER_KEY_REGEX))
		{
			throw new IllegalArgumentException("Server sent invalid server key.");
		}
		
		if (!msg.levelKey.matches(LevelInitMessage.LEVEL_KEY_REGEX))
		{
			throw new IllegalArgumentException("Server sent invalid level key.");
		}
		
		LOGGER.info("Server level key received: [" + msg.levelKey + "].");
		
		RenderThreadTaskHandler.INSTANCE.queueRunningOnRenderThread("ClientPluginChannelApi onLevelInitMessage", () -> 
		{
			IClientLevelWrapper clientLevel = MC.getWrappedClientLevel(true);
			IServerKeyedClientLevel existingKeyedClientLevel = KEYED_CLIENT_LEVEL_MANAGER.getServerKeyedLevel();

			if (existingKeyedClientLevel != null)
			{
				if (!existingKeyedClientLevel.getServerLevelKey().equals(msg.levelKey))
				{
					LOGGER.info("Unloading previous level with key: [" + existingKeyedClientLevel.getServerLevelKey() + "].");
					this.levelUnloadHandler.accept(existingKeyedClientLevel);
				}
				else
				{
					LOGGER.info("Level key matches the previous level key, ignoring the message.");
				}
			}
			else
			{
				LOGGER.info("Unloading non-keyed level: [" + clientLevel.getDhIdentifier() + "].");
				this.levelUnloadHandler.accept(clientLevel);
			}
			
			if (existingKeyedClientLevel == null
					|| !existingKeyedClientLevel.getServerKey().equals(msg.serverKey)
					|| !existingKeyedClientLevel.getServerLevelKey().equals(msg.levelKey))
			{
				LOGGER.info("Loading level with key: [" + msg.levelKey + "].");
				IServerKeyedClientLevel keyedLevel = KEYED_CLIENT_LEVEL_MANAGER.setServerKeyedLevel(clientLevel, msg.serverKey, msg.levelKey);
				this.levelLoadHandler.accept(keyedLevel);
			}
		});
	}
	
	public void onClientLevelUnload() { KEYED_CLIENT_LEVEL_MANAGER.clearKeyedLevel(); }
	
	
	
	//==========//
	// shutdown //
	//==========//
	
	private void onClose(CloseInternalEvent event) { this.reset(); }
	public void reset()
	{
		this.networkSession = null;
		KEYED_CLIENT_LEVEL_MANAGER.disable();
	}
	
}