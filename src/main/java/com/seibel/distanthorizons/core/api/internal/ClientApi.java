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

package com.seibel.distanthorizons.core.api.internal;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.config.EDhApiMcRenderingFadeMode;
import com.seibel.distanthorizons.api.enums.rendering.EDhApiRenderPass;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.*;
import com.seibel.distanthorizons.core.api.internal.rendering.DhRenderState;
import com.seibel.distanthorizons.core.enums.MinecraftTextFormat;
import com.seibel.distanthorizons.core.file.structure.ClientOnlySaveStructure;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.logging.f3.F3Screen;
import com.seibel.distanthorizons.core.network.messages.MessageRegistry;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.render.DhApiRenderProxy;
import com.seibel.distanthorizons.core.render.RenderParams;
import com.seibel.distanthorizons.core.render.RenderThreadTaskHandler;
import com.seibel.distanthorizons.core.render.renderer.*;
import com.seibel.distanthorizons.core.util.TimerUtil;
import com.seibel.distanthorizons.core.util.math.Vec3d;
import com.seibel.distanthorizons.core.util.objects.Pair;
import com.seibel.distanthorizons.core.util.objects.RollingAverage;
import com.seibel.distanthorizons.core.util.threading.ThreadPoolUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.render.renderPass.IDhMetaRenderer;
import com.seibel.distanthorizons.core.wrapperInterfaces.render.renderPass.IDhTerrainRenderer;
import com.seibel.distanthorizons.core.wrapperInterfaces.render.renderPass.IDhVanillaFadeRenderer;
import com.seibel.distanthorizons.core.wrapperInterfaces.render.renderPass.IDhTestTriangleRenderer;
import com.seibel.distanthorizons.coreapi.DependencyInjection.ApiEventInjector;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.network.messages.AbstractNetworkMessage;
import com.seibel.distanthorizons.core.network.session.NetworkSession;
import com.seibel.distanthorizons.coreapi.ModInfo;
import com.seibel.distanthorizons.api.enums.rendering.EDhApiDebugRendering;
import com.seibel.distanthorizons.api.enums.rendering.EDhApiRendererMode;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.level.IServerKeyedClientLevel;
import com.seibel.distanthorizons.core.world.AbstractDhWorld;
import com.seibel.distanthorizons.core.world.DhClientWorld;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.distanthorizons.core.logging.DhLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * This holds the methods that should be called
 * by the host mod loader (Fabric, Forge, etc.).
 * Specifically for the client.
 */
public class ClientApi
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	private static final DhLogger RATE_LIMITED_LOGGER = new DhLoggerBuilder().maxCountPerSecond(1).build();
	
	public static final ClientApi INSTANCE = new ClientApi();
	
	private static final IMinecraftClientWrapper MC_CLIENT = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class);
	
	/** this includes the is dev build message and low allocated memory warning */
	private static final int MS_BETWEEN_STATIC_STARTUP_MESSAGES = 4_000;
	
	/** 
	 * This isn't the cleanest way of storing variables before passing them to the LOD renderer, 
	 * but due to how mixins work and the inconsistency between MC versions,
	 * having a static object that stores a single frame's data
	 * is often the easiest solution. <br><br>
	 * 
	 * Only downside is making sure each variable is populated before rendering.
	 */
	public static final DhRenderState RENDER_STATE = new DhRenderState();
	
	/**
	 * 50ms = 20 FPS
	 * @link https://fpstoms.com/ 
	 * @see ClientApi#cameraSpeedRollingAverage
	 */
	private static final long MIN_MS_BETWEEN_SPEED_CHECKS = 50;
	
	
	private boolean isDevBuildMessagePrinted = false;
	private boolean lowMemoryWarningPrinted = false;
	private boolean highVanillaRenderDistanceWarningPrinted = false;
	
	private long lastStaticWarningMessageSentMsTime = 0L;
	
	private final Queue<String> chatMessageQueueForNextFrame = new LinkedBlockingQueue<>();
	private final Queue<String> overlayMessageQueueForNextFrame = new LinkedBlockingQueue<>();
	
	public boolean rendererDisabledBecauseOfExceptions = false;
	
	private final ClientPluginChannelApi pluginChannelApi = new ClientPluginChannelApi(this::clientLevelLoadEvent, this::clientLevelUnloadEvent);
	
	/** Delay loading the first level to give the server some time to respond with level to actually load */
	private Timer firstLevelLoadTimer;
	private static final long FIRST_LEVEL_LOAD_DELAY_IN_MS = 1_000;
	
	/** Holds any levels that were loaded before the {@link ClientApi#onClientOnlyConnected} was fired. */
	public final HashSet<IClientLevelWrapper> waitingClientLevels = new HashSet<>();
	/** Holds any chunks that were loaded before the {@link ClientApi#clientLevelLoadEvent(IClientLevelWrapper)} was fired. */
	public final HashMap<Pair<IClientLevelWrapper, DhChunkPos>, IChunkWrapper> waitingChunkByClientLevelAndPos = new HashMap<>();
	
	/** publicly available so {@link F3Screen} can display the error */
	@Nullable
	public String lastRenderParamValidationMessage = null;
	
	
	/** 
	 * measured in blocks/second <br>
	 * 
	 * The number of points tracked here is related
	 * to the rate at which we check for speed.
	 * So if the ms_between is changed the number of points
	 * tracked should also be to keep the ratio roughly the same.
	 * @see ClientApi#MIN_MS_BETWEEN_SPEED_CHECKS
	 */
	public RollingAverage cameraSpeedRollingAverage = new RollingAverage(40);
	private Vec3d lastCameraPosForSpeedCheck = new Vec3d();
	private long msSinceLastSpeedCheck = 0L;
	
	
	
	
	//==============//
	// constructors //
	//==============//
	
	private ClientApi() { }
	
	
	
	//==============//
	// world events //
	//==============//
	///region
	
	/**
	 * May be fired slightly before or after the associated
	 * {@link ClientApi#clientLevelLoadEvent(IClientLevelWrapper)} event
	 * depending on how the host mod loader functions. <br><br>
	 * 
	 * Synchronized shouldn't be necessary, but is present to match {@see onClientOnlyDisconnected} and prevent any unforeseen issues. 
	 */
	public synchronized void onClientOnlyConnected()
	{
		// only continue if the client is connected to a different server
		boolean connectedToServer = MC_CLIENT.clientConnectedToDedicatedServer();
		boolean connectedToReplay = MC_CLIENT.connectedToReplay();
		if (connectedToServer || connectedToReplay)
		{
			if (connectedToServer)
			{
				LOGGER.info("Client on ClientOnly mode connecting.");
			}
			else
			{
				LOGGER.info("Replay on ClientServer mode connecting.");
				
				if (Config.Common.Logging.Warning.showReplayWarningOnStartup.get())
				{
					MC_CLIENT.sendChatMessage(MinecraftTextFormat.ORANGE + "Distant Horizons: Replay detected." + MinecraftTextFormat.CLEAR_FORMATTING);
					MC_CLIENT.sendChatMessage("DH may behave strangely or have missing functionality.");
					MC_CLIENT.sendChatMessage("In order to use pre-generated LODs, put your DH database(s) in:");
					MC_CLIENT.sendChatMessage(MinecraftTextFormat.GRAY +".Minecraft" + File.separator + ClientOnlySaveStructure.SERVER_DATA_FOLDER_NAME + File.separator + ClientOnlySaveStructure.REPLAY_SERVER_FOLDER_NAME + File.separator + "DIMENSION_NAME"+ MinecraftTextFormat.CLEAR_FORMATTING);
					MC_CLIENT.sendChatMessage("This message can be disabled in DH's config under Advanced -> Logging.");
					MC_CLIENT.sendChatMessage("");
				}
			}
			
			
			DhClientWorld world = new DhClientWorld();
			SharedApi.setDhWorld(world);
			
			this.pluginChannelApi.onJoinServer(world.networkState.getSession());
			world.networkState.sendConfigMessage();
			
			LOGGER.info("Loading [" + this.waitingClientLevels.size() + "] waiting client level wrappers.");
			for (IClientLevelWrapper level : this.waitingClientLevels)
			{
				this.clientLevelLoadEvent(level);
			}
			
			this.waitingClientLevels.clear();
		}
	}
	
	/** Synchronized to prevent a rare issue where multiple disconnect events are triggered on top of each other. */
	public synchronized void onClientOnlyDisconnected()
	{
		// clear the first time timer
		if (this.firstLevelLoadTimer != null)
		{
			this.firstLevelLoadTimer.cancel();
			this.firstLevelLoadTimer = null;
		}
		
		AbstractDhWorld world = SharedApi.getAbstractDhWorld();
		if (world != null)
		{
			LOGGER.info("Client on ClientOnly mode disconnecting.");
			
			world.close();
			SharedApi.setDhWorld(null);
		}
		
		this.pluginChannelApi.reset();
		
		// remove any waiting items
		this.waitingChunkByClientLevelAndPos.clear();
		this.waitingClientLevels.clear();
	}
	
	///endregion
	
	
	
	//==============//
	// level events //
	//==============//
	///region
	
	public void clientLevelUnloadEvent(IClientLevelWrapper level)
	{
		try
		{
			LOGGER.info("Unloading client level [" + level.getClass().getSimpleName() + "]-[" + level.getDhIdentifier() + "].");
			
			if (level instanceof IServerKeyedClientLevel)
			{
				this.pluginChannelApi.onClientLevelUnload();
			}
			
			AbstractDhWorld world = SharedApi.getAbstractDhWorld();
			if (world != null)
			{
				world.unloadLevel(level);
				ApiEventInjector.INSTANCE.fireAllEvents(DhApiLevelUnloadEvent.class, new DhApiLevelUnloadEvent.EventParam(level));
			}
			else
			{
				this.waitingClientLevels.remove(level);
			}
		}
		catch (Exception e)
		{
			// handle errors here to prevent blowing up a mixin or API up stream
			LOGGER.error("Unexpected error in ClientApi.clientLevelUnloadEvent(), error: "+e.getMessage(), e);
		}
	}
	
	public void clientLevelLoadEvent(@Nullable IClientLevelWrapper levelWrapper)
	{
		// can happen if there was an issue during level load
		if (levelWrapper == null)
		{
			return;
		}
		
		
		// wait a moment before loading the level to give the server a chance to handle the client's login request
		if (MC_CLIENT.clientConnectedToDedicatedServer())
		{
			if (this.firstLevelLoadTimer == null)
			{
				this.firstLevelLoadTimer = TimerUtil.CreateTimer("FirstLevelLoadTimer");
				this.firstLevelLoadTimer.schedule(new TimerTask()
				{
					@Override
					public void run() { ClientApi.this.clientLevelLoadEvent(levelWrapper); }
				}, FIRST_LEVEL_LOAD_DELAY_IN_MS);
				return;
			}
			this.firstLevelLoadTimer.cancel();
		}
		
		
		try
		{
			LOGGER.info("Loading client level [" + levelWrapper + "]-[" + levelWrapper.getDhIdentifier() + "].");
			
			AbstractDhWorld world = SharedApi.getAbstractDhWorld();
			if (world != null)
			{
				if (!this.pluginChannelApi.allowLevelLoading(levelWrapper))
				{
					LOGGER.info("Levels in this connection are managed by the server, skipping auto-load.");
					
					// Instead of attempting to load themselves, send the config and wait for a server provided level key.
					((DhClientWorld) world).networkState.sendConfigMessage();
					return;
				}
				
				
				world.getOrLoadLevel(levelWrapper);
				ApiEventInjector.INSTANCE.fireAllEvents(DhApiLevelLoadEvent.class, new DhApiLevelLoadEvent.EventParam(levelWrapper));
				
				this.loadWaitingChunksForLevel(levelWrapper);
			}
			else
			{
				this.waitingClientLevels.add(levelWrapper);
			}
		}
		catch (Exception e)
		{
			// handle errors here to prevent blowing up a mixin or API up stream
			LOGGER.error("Unexpected error in ClientApi.clientLevelLoadEvent(), error: "+e.getMessage(), e);
		}
	}
	private void loadWaitingChunksForLevel(IClientLevelWrapper level)
	{
		HashSet<Pair<IClientLevelWrapper, DhChunkPos>> keysToRemove = new HashSet<>();
		for (Pair<IClientLevelWrapper, DhChunkPos> levelChunkPair : this.waitingChunkByClientLevelAndPos.keySet())
		{
			// only load chunks that came from this level
			IClientLevelWrapper levelWrapper = levelChunkPair.first;
			if (levelWrapper.equals(level))
			{
				IChunkWrapper chunkWrapper = this.waitingChunkByClientLevelAndPos.get(levelChunkPair);
				SharedApi.INSTANCE.applyChunkUpdate(chunkWrapper, levelWrapper);
				keysToRemove.add(levelChunkPair);
			}
		}
		LOGGER.info("Loaded [" + keysToRemove.size() + "] waiting chunk wrappers.");
		
		for (Pair<IClientLevelWrapper, DhChunkPos> keyToRemove : keysToRemove)
		{
			this.waitingChunkByClientLevelAndPos.remove(keyToRemove);
		}
	}
	
	///endregion
	
	
	
	//============//
	// networking //
	//============//
	///region
	
	/**
	 * Forwards a decoded message into the registered handlers.
	 *
	 * @see MessageRegistry
	 */
	public void pluginMessageReceived(@NotNull AbstractNetworkMessage message)
	{
		@Nullable ThreadPoolExecutor executor = ThreadPoolUtil.networkClientHandlerExecutor();
		if (executor == null)
		{
			LOGGER.warn("warn");
			return;
		}
		
		try
		{
			executor.execute(() ->
			{
				NetworkSession networkSession = this.pluginChannelApi.networkSession;
				if (networkSession != null)
				{
					networkSession.tryHandleMessage(message);
				}
			});
		}
		catch (RejectedExecutionException e)
		{
			LOGGER.warn("Plugin message executor rejected");
		}
	}
	
	///endregion
	
	
	
	//===============//
	// LOD rendering //
	//===============//
	///region
	
	/** Should be called before {@link ClientApi#renderDeferredLodsForShaders} */
	public void renderLods() { this.renderLodLayer(false); }
	
	/** 
	 * Only necessary when Shaders are in use.
	 * Should be called after {@link ClientApi#renderLods} 
	 */
	public void renderDeferredLodsForShaders() { this.renderLodLayer(true); }
	
	public static long firstRenderTimeMs = 0;
	
	private void renderLodLayer(boolean renderingDeferredLayer)
	{
		IProfilerWrapper profiler = MC_CLIENT.getProfiler();
		profiler.push("DH-RenderLevel");
		
		
		
		//===========//
		// debugging //
		//===========//
		//region
		
		//DhApiTerrainDataRepo.asyncDebugMethod(
		//	RENDER_STATE.clientLevelWrapper,
		//	MC_CLIENT.getPlayerBlockPos().getX(),
		//	MC_CLIENT.getPlayerBlockPos().getY(),
		//	MC_CLIENT.getPlayerBlockPos().getZ()
		//);
		
		//endregion
		
		
		
		//=====================//
		// render thread tasks //
		//=====================//
		///region
		
		// only run these tasks once per frame
		if (!renderingDeferredLayer)
		{
			profiler.push("DH render thread tasks");
			
			
			
			//===============//
			// chat messages //
			//===============//
			
			this.sendQueuedChatMessages();
			
			
			
			//======================//
			// GL Proxy queued jobs //
			//======================//
			
			try
			{
				// these tasks always need to be called, regardless of whether the renderer is enabled or not to prevent memory leaks
				RenderThreadTaskHandler.INSTANCE.runRenderThreadTasks();
			}
			catch (Exception e)
			{
				LOGGER.error("Unexpected issue running render thread tasks, error: [" + e.getMessage() + "].", e);
			}
			
			
			
			//==============//
			// camera speed //
			//==============//
			
			long nowMs = System.currentTimeMillis();
			if (this.msSinceLastSpeedCheck + MIN_MS_BETWEEN_SPEED_CHECKS < nowMs)
			{
				// calc time since last check
				double secSinceLastCheck = (nowMs - this.msSinceLastSpeedCheck) / 1_000.0;
				this.msSinceLastSpeedCheck = nowMs;
				
				// get the distance traveled since last frame
				Vec3d camPos = MC_RENDER.getCameraExactPosition();
				double distanceInBlocks = camPos.getDistance(this.lastCameraPosForSpeedCheck);
				double speed = distanceInBlocks / secSinceLastCheck;
				
				// record new values for next check
				this.cameraSpeedRollingAverage.add(speed);
				this.lastCameraPosForSpeedCheck = camPos;
			}
			
			
			profiler.pop();
		}
		
		///endregion
		
		
		
		
		//=================//
		// parameter setup //
		//=================//
		///region
		
		EDhApiRenderPass renderPass;
		if (DhApiRenderProxy.INSTANCE.getDeferTransparentRendering())
		{
			if (renderingDeferredLayer)
			{
				renderPass = EDhApiRenderPass.TRANSPARENT;
			}
			else
			{
				renderPass = EDhApiRenderPass.OPAQUE;
			}
		}
		else
		{
			renderPass = EDhApiRenderPass.OPAQUE_AND_TRANSPARENT;
		}
		
		// A global render state variable is used since MC has split up their
		// render prep and actual rendering into different threads/methods
		// this is annoying since it's possible to start a render with only
		// partially complete info, but there isn't a better option at the moment
		RenderParams renderParams = new RenderParams(renderPass, RENDER_STATE);
		
		///endregion
		
		
		
		//============//
		// validation //
		//============//
		///region
		
		if (firstRenderTimeMs == 0)
		{
			firstRenderTimeMs = System.currentTimeMillis();
		}
		
		String validationMessage = renderParams.getValidationErrorMessage(firstRenderTimeMs);
		if (validationMessage != null)
		{
			// store the error message so it can be seen on the F3 screen
			this.lastRenderParamValidationMessage = validationMessage;
			return;
		}
		else
		{
			this.lastRenderParamValidationMessage = null;
		}
		
		if (this.rendererDisabledBecauseOfExceptions)
		{
			// re-enable rendering if the user toggles DH rendering
			if (!Config.Client.quickEnableRendering.get())
			{
				LOGGER.info("DH Renderer re-enabled after exception. Some rendering issues may occur. Please reboot Minecraft if you see any rendering issues.");
				this.rendererDisabledBecauseOfExceptions = false;
				Config.Client.quickEnableRendering.set(true);
			}
			
			return;
		}
		
		if (Config.Client.Advanced.Debugging.rendererMode.get() == EDhApiRendererMode.DISABLED)
		{
			return;
		}
		
		///endregion
		
		
		
		//===========//
		// rendering //
		//===========//
		///region
		
		try
		{
			// render pass //
			if (Config.Client.Advanced.Debugging.rendererMode.get() == EDhApiRendererMode.DEFAULT)
			{
				if (!renderingDeferredLayer)
				{
					boolean renderingCancelled = ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeRenderEvent.class, renderParams);
					if (!renderingCancelled)
					{
						LodRenderer.INSTANCE.render(renderParams, profiler);
					}
					
					if (!DhApi.Delayed.renderProxy.getDeferTransparentRendering())
					{
						ApiEventInjector.INSTANCE.fireAllEvents(DhApiAfterRenderEvent.class, null);
					}
				}
				else
				{
					boolean renderingCancelled = ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeDeferredRenderEvent.class, renderParams);
					if (!renderingCancelled)
					{
						LodRenderer.INSTANCE.renderDeferred(renderParams, profiler);
					}
					
					
					if (DhApi.Delayed.renderProxy.getDeferTransparentRendering())
					{
						ApiEventInjector.INSTANCE.fireAllEvents(DhApiAfterRenderEvent.class, null);
					}
				}
			}
			else
			{
				if (!renderingDeferredLayer)
				{
					IDhMetaRenderer metaRenderer = SingletonInjector.INSTANCE.get(IDhMetaRenderer.class);
					IDhTestTriangleRenderer testRenderer = SingletonInjector.INSTANCE.get(IDhTestTriangleRenderer.class);
					if (testRenderer != null
						&& metaRenderer != null)
					{
						// meta renderer needed for render state/texture
						// for setup on some APIs (IE openGL)
						metaRenderer.runRenderPassSetup(renderParams);
						
						testRenderer.render(renderParams);
						
						metaRenderer.runRenderPassCleanup(renderParams);
					}
					else
					{
						RATE_LIMITED_LOGGER.warn("Unable to find singleton [" + IDhTestTriangleRenderer.class.getSimpleName() + "]");
					}
				}
			}
		}
		catch (Exception e)
		{
			this.rendererDisabledBecauseOfExceptions = true;
			LOGGER.error("Unexpected Renderer error in render pass [" + renderPass + "]. Error: " + e.getMessage(), e);
			
			MC_CLIENT.sendChatMessage(MinecraftTextFormat.DARK_RED + "" + MinecraftTextFormat.BOLD + "ERROR: Distant Horizons renderer has encountered an exception!" + MinecraftTextFormat.CLEAR_FORMATTING);
			MC_CLIENT.sendChatMessage(MinecraftTextFormat.DARK_RED + "Renderer disabled to try preventing GL state corruption." + MinecraftTextFormat.CLEAR_FORMATTING);
			MC_CLIENT.sendChatMessage(MinecraftTextFormat.DARK_RED + "Toggle DH rendering via the config UI to re-activate DH rendering." + MinecraftTextFormat.CLEAR_FORMATTING);
			MC_CLIENT.sendChatMessage(MinecraftTextFormat.DARK_RED + "Error: " + MinecraftTextFormat.CLEAR_FORMATTING + e);
		}
		
		///endregion
		
		
		
		profiler.pop(); // end LOD
	}
	
	///endregion
	
	
	
	//================//
	// fade rendering //
	//================//
	///region
	
	/** 
	 * The first fade pass.
	 * Called after MC finishes rendering the opaque passes. 
	 */
	public void renderFadeOpaque()
	{
		IDhVanillaFadeRenderer fadeRenderer = SingletonInjector.INSTANCE.get(IDhVanillaFadeRenderer.class);
		if (fadeRenderer == null)
		{
			return;
		}
		
		// only fade when DH is rendering
		if (Config.Client.Advanced.Debugging.rendererMode.get() != EDhApiRendererMode.DISABLED
			&&
			(
				// only fade when requested
				Config.Client.Advanced.Graphics.Quality.vanillaFadeMode.get() == EDhApiMcRenderingFadeMode.DOUBLE_PASS
				// or if LOD-only mode is enabled (fading is used to remove the MC render pass)
				|| Config.Client.Advanced.Debugging.lodOnlyMode.get()
			)
			// don't fade when Iris shaders are active, otherwise the rendering can get weird
			&& !DhApiRenderProxy.INSTANCE.getDeferTransparentRendering())
		{
			RenderParams renderParams = new RenderParams(EDhApiRenderPass.OPAQUE, RENDER_STATE);
			fadeRenderer.render(renderParams);
		}
	}
	/** 
	 * The second fade pass.
	 * Called after MC finishes rendering both opaque
	 * and transparent passes. 
	 */
	public void renderFadeTransparent()
	{
		IDhVanillaFadeRenderer fadeRenderer = SingletonInjector.INSTANCE.get(IDhVanillaFadeRenderer.class);
		if (fadeRenderer == null)
		{
			return;
		}
		
		// only fade when DH is rendering
		if (Config.Client.Advanced.Debugging.rendererMode.get() != EDhApiRendererMode.DISABLED)
		{
			boolean renderFade =
				(
					// only fade when requested
					Config.Client.Advanced.Graphics.Quality.vanillaFadeMode.get() != EDhApiMcRenderingFadeMode.NONE
					// or if LOD-only mode is enabled (fading is used to remove the MC render pass)
					|| Config.Client.Advanced.Debugging.lodOnlyMode.get()
				)
				// don't fade when Iris shaders are active, otherwise the rendering can get weird
				&& !DhApiRenderProxy.INSTANCE.getDeferTransparentRendering();
			if (renderFade)
			{
				RenderParams renderParams = new RenderParams(EDhApiRenderPass.TRANSPARENT, RENDER_STATE);
				fadeRenderer.render(renderParams);
			}
		}
	}
	
	///endregion
	
	
	
	//==========//
	// keyboard //
	//==========//
	///region
	
	/** Trigger once on key press, with CLIENT PLAYER. */
	public void keyPressedEvent(int glfwKey)
	{
		if (!Config.Client.Advanced.Debugging.enableDebugKeybindings.get())
		{
			// keybindings are disabled
			return;
		}
		
		
		if (glfwKey == GLFW.GLFW_KEY_F6)
		{
			Config.Client.Advanced.Debugging.rendererMode.set(EDhApiRendererMode.next(Config.Client.Advanced.Debugging.rendererMode.get()));
			MC_CLIENT.sendChatMessage("F6: Set rendering to " + Config.Client.Advanced.Debugging.rendererMode.get());
		}
		else if (glfwKey == GLFW.GLFW_KEY_F7)
		{
			Config.Client.Advanced.Debugging.lodOnlyMode.set(!Config.Client.Advanced.Debugging.lodOnlyMode.get());
			MC_CLIENT.sendChatMessage("F7: Set LOD only mode to " + Config.Client.Advanced.Debugging.lodOnlyMode.get());
		}
		else if (glfwKey == GLFW.GLFW_KEY_F8)
		{
			Config.Client.Advanced.Debugging.debugRendering.set(EDhApiDebugRendering.next(Config.Client.Advanced.Debugging.debugRendering.get()));
			MC_CLIENT.sendChatMessage("F8: Set debug mode to " + Config.Client.Advanced.Debugging.debugRendering.get());
		}
	}
	
	///endregion
	
	
	
	//======//
	// chat //
	//======//
	///region
	
	private void sendQueuedChatMessages()
	{
		// this includes if the current build is a dev build
		// and configuration warnings (IE Java memory amount and MC settings)
		this.detectAndSendBootTimeWarnings();
		
		// Don't send any generic messages until the static ones have been sent.
		// This makes sure the more critical messages are seen first.
		if (this.staticStartupMessageSentRecently())
		{
			return;
		}
			
		
		// chat messages
		while (!this.chatMessageQueueForNextFrame.isEmpty())
		{
			String message = this.chatMessageQueueForNextFrame.poll();
			if (message == null)
			{
				// done to prevent potential null pointers
				message = "";
			}
			MC_CLIENT.sendChatMessage(message);
		}
		
		// overlay messages
		while (!this.overlayMessageQueueForNextFrame.isEmpty())
		{
			String message = this.overlayMessageQueueForNextFrame.poll();
			if (message == null)
			{
				// done to prevent potential null pointers
				message = "";
			}
			MC_CLIENT.sendOverlayMessage(message);
		}
	}
	private void detectAndSendBootTimeWarnings()
	{
		// dev build
		if (ModInfo.IS_DEV_BUILD 
			&& !this.isDevBuildMessagePrinted 
			&& MC_CLIENT.playerExists())
		{
			this.isDevBuildMessagePrinted = true;
			this.lastStaticWarningMessageSentMsTime = System.currentTimeMillis();
			
			// remind the user that this is a development build
			String message =
					MinecraftTextFormat.DARK_GREEN + "Distant Horizons: nightly/unstable build, version: [" + ModInfo.VERSION+"]." + MinecraftTextFormat.CLEAR_FORMATTING + "\n" +
							"Issues may occur with this version.\n" +
							"Here be dragons!\n";
			MC_CLIENT.sendChatMessage(message);
		}
		
		
		// memory
		if (this.staticStartupMessageSentRecently()) return;
		if (!this.lowMemoryWarningPrinted 
			&& Config.Common.Logging.Warning.showLowMemoryWarningOnStartup.get())
		{
			this.lowMemoryWarningPrinted = true;
			this.lastStaticWarningMessageSentMsTime = System.currentTimeMillis();
			
			// 4 GB
			long minimumRecommendedMemoryInBytes = 4L * 1_000_000_000L;
			
			// Java returned 17,171,480,576 for 16 GB so it might be slightly off what you'd expect
			long maxMemoryInBytes = Runtime.getRuntime().maxMemory();
			if (maxMemoryInBytes < minimumRecommendedMemoryInBytes)
			{
				String message =
						// orange text		
						MinecraftTextFormat.ORANGE + "Distant Horizons: Low memory detected." + MinecraftTextFormat.CLEAR_FORMATTING + "\n" +
						"Stuttering or low FPS may occur. \n" +
						"Please increase Minecraft's available memory to 4 GB or more. \n" +
						"This warning can be disabled in DH's config under Advanced -> Logging. \n";
				MC_CLIENT.sendChatMessage(message);
			}
		}
		
		
		// high vanilla render distance
		if (this.staticStartupMessageSentRecently()) return;
		if (!this.highVanillaRenderDistanceWarningPrinted 
			&& Config.Common.Logging.Warning.showHighVanillaRenderDistanceWarning.get())
		{
			this.highVanillaRenderDistanceWarningPrinted = true;
			
			// DH generally doesn't need a vanilla render distance above 12 
			if (MC_RENDER.getRenderDistance() > 12)
			{
				this.lastStaticWarningMessageSentMsTime = System.currentTimeMillis();
				
				String message =
					MinecraftTextFormat.YELLOW + "Distant Horizons: High vanilla render distance detected." + MinecraftTextFormat.CLEAR_FORMATTING + "\n" +
					"Using a high vanilla render distance uses a lot of CPU power \n" +
					"and doesn't improve graphics much after about 12.\n" +
					"Lowering your vanilla render distance will give you better FPS\n" +
					"and reduce stuttering at a similar visual quality.\n" +
					MinecraftTextFormat.GRAY + "A vanilla render distance of 8 is recommended." + MinecraftTextFormat.CLEAR_FORMATTING + "\n" +
					"This message can be disabled in DH's config under Advanced -> Logging.\n";
				MC_CLIENT.sendChatMessage(message);
			}
		}
	}
	/** done to prevent sending a bunch of startup messages all at once, causing some to be missed. */
	private boolean staticStartupMessageSentRecently()
	{
		if (this.lastStaticWarningMessageSentMsTime == 0)
		{
			// no static message has ever been sent
			return false;
		}
		
		long timeSinceLastMessage = System.currentTimeMillis() - this.lastStaticWarningMessageSentMsTime; 
		return timeSinceLastMessage <= MS_BETWEEN_STATIC_STARTUP_MESSAGES;
	}
	
	
	/** 
	 * Queues the given message to appear in chat the next valid frame.
	 * Useful for queueing up messages that may be triggered before the user has loaded into the world. 
	 */
	public void showChatMessageNextFrame(String chatMessage) { this.chatMessageQueueForNextFrame.add(chatMessage); }
	
	/**
	 * Similar to {@link ClientApi#showChatMessageNextFrame(String)} but appears above the toolbar.
	 */
	public void showOverlayMessageNextFrame(String message) { this.overlayMessageQueueForNextFrame.add(message); }
	
	///endregion
	
	
	
}
