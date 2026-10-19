package com.seibel.distanthorizons.forge17;

import com.seibel.distanthorizons.common.commonMixins.MixinChunkMapCommon;
import com.seibel.distanthorizons.common.util.threading.ServerThreadTaskHandler;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;

import com.seibel.distanthorizons.common.AbstractModInitializer;
import com.seibel.distanthorizons.common.util.ProxyUtil;
import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;
import com.seibel.distanthorizons.common.wrappers.misc.ServerPlayerWrapper;
import com.seibel.distanthorizons.common.wrappers.world.ServerLevelWrapper;
import com.seibel.distanthorizons.core.api.internal.ServerApi;
import com.seibel.distanthorizons.core.api.internal.SharedApi;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IServerPlayerWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IServerLevelWrapper;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ForgeServerProxy implements AbstractModInitializer.IEventProxy
{
    private final boolean isDedicated;
	
	private static final int MAX_PENDING_CLICK_TASKS = 2_048;
	private static final Set<Long> pendingClickTaskKeys = ConcurrentHashMap.newKeySet();
	private static final AtomicInteger pendingClickTaskCount = new AtomicInteger(0);
	
	
	
	//=============//
    // constructor //
    //=============//
	//region

    public ForgeServerProxy(boolean isDedicated) 
    { 
		this.isDedicated = isDedicated;
	    
    }
	
	@Override
	public void registerEvents()
	{
		MinecraftForge.EVENT_BUS.register(this);
		FMLCommonHandler.instance()
			.bus()
			.register(this);
		
		if (this.isDedicated)
		{
			ForgePluginPacketSender.setPacketHandler(ServerApi.INSTANCE::pluginMessageReceived);
		}
	}
	
	//endregion
	
	
	
    //========//
    // events //
    //========//
	//region
	
	public static void serverStopping()
	{
		ServerThreadTaskHandler.INSTANCE.cancelPendingTasks();
	}

    // ServerTickEvent (at end)
    @SubscribeEvent
	public void serverTickEvent(TickEvent.ServerTickEvent event)
	{
		if (event.phase == TickEvent.Phase.START)
		{
			ServerThreadTaskHandler.INSTANCE.onTickStart();
		}
		else
		{
			ServerThreadTaskHandler.INSTANCE.runTasks();
		}
	}

    // ServerLevelLoadEvent
    @SubscribeEvent 
    public void serverLevelLoadEvent(WorldEvent.Load event)
    {
		if (GetEventLevel(event) instanceof WorldServer)
	    {
			ServerApi.INSTANCE.serverLevelLoadEvent(getServerLevelWrapper((WorldServer) GetEventLevel(event)));
		}
	}

    // ServerLevelUnloadEvent
    @SubscribeEvent
	public void serverLevelUnloadEvent(WorldEvent.Unload event)
    {
		if (GetEventLevel(event) instanceof WorldServer)
	    {
			ServerApi.INSTANCE.serverLevelUnloadEvent(getServerLevelWrapper((WorldServer) GetEventLevel(event)));
		}
	}

    @SubscribeEvent
	public void serverChunkLoadEvent(ChunkEvent.Load event)
    {
		if (!(event.world instanceof WorldServer))
	    {
			return;
		}
		
		Chunk chunk = event.getChunk();
		ILevelWrapper levelWrapper = ProxyUtil.getLevelWrapper(GetEventLevel(event));
		ChunkWrapper chunkWrapper = new ChunkWrapper(chunk, levelWrapper);
		// Only handle event if chunk is ready, otherwise drop the update - we'll get it later during save
		if (!chunk.isTerrainPopulated || !chunk.isLightPopulated)
	    {
			return;
		}
	    ServerApi.INSTANCE.serverChunkLoadEvent(chunkWrapper, levelWrapper);
	}

    @SubscribeEvent
    public void serverChunkSaveEvent(ChunkDataEvent.Save event) 
    {
        if (!(event.world instanceof WorldServer)) 
		{
            return;
        }
	    MixinChunkMapCommon.onChunkSave((WorldServer) event.world, event.getChunk());
    }

    @SubscribeEvent
    public void playerLoggedInEvent(PlayerEvent.PlayerLoggedInEvent event) 
    {
	    IServerPlayerWrapper serverPlayerWrapper = getServerPlayerWrapper(event);
	    if (serverPlayerWrapper == null)
	    {
		    return;
	    }
		ServerApi.INSTANCE.serverPlayerJoinEvent(serverPlayerWrapper);
	}

    @SubscribeEvent
    public void playerLoggedOutEvent(PlayerEvent.PlayerLoggedOutEvent event) 
    {
	    IServerPlayerWrapper serverPlayerWrapper = getServerPlayerWrapper(event);
	    if (serverPlayerWrapper == null)
	    {
		    return;
	    }
		ServerApi.INSTANCE.serverPlayerDisconnectEvent(serverPlayerWrapper);
	}

    @SubscribeEvent
    public void playerChangedDimensionEvent(PlayerEvent.PlayerChangedDimensionEvent event) 
    {
        ServerApi.INSTANCE.serverPlayerLevelChangeEvent(
            getServerPlayerWrapper(event),
            getServerLevelWrapper(event.fromDim, event),
            getServerLevelWrapper(event.toDim, event));
    }

    @SubscribeEvent
	public void clickBlockEvent(PlayerInteractEvent event)
    {
		final World world = event.world;
		final int x = event.x;
		final int z = event.z;
		ILevelWrapper wrappedLevel = ProxyUtil.getLevelWrapper(world);
		if (SharedApi.isChunkAtBlockPosAlreadyUpdating(wrappedLevel, x, z))
	    {
			return;
		}
		
		if (pendingClickTaskCount.get() > MAX_PENDING_CLICK_TASKS)
		{
			return;
		}
		
		final long chunkKey = (((long) System.identityHashCode(world)) << 32) ^ ChunkCoordIntPair.chunkXZ2Int(x >> 4, z >> 4);
		if (!pendingClickTaskKeys.add(chunkKey))
		{
			return;
		}
		
		pendingClickTaskCount.incrementAndGet();
		AtomicBoolean taskStarted = new AtomicBoolean(false);
		ServerThreadTaskHandler.INSTANCE.queueTask(() ->
		{
			taskStarted.set(true);
			pendingClickTaskKeys.remove(chunkKey);
			try
			{
				// the task may run ticks later, don't load the chunk again if it was unloaded in the meantime
				if (!world.getChunkProvider().chunkExists(x >> 4, z >> 4))
				{
					return null;
				}

				Chunk chunk = world.getChunkFromBlockCoords(x, z);
				ChunkWrapper chunkWrapper = new ChunkWrapper(chunk, wrappedLevel);
				SharedApi.INSTANCE.applyChunkUpdate(chunkWrapper, wrappedLevel, true);
				return null;
			}
			finally
			{
				pendingClickTaskCount.decrementAndGet();
			}
		}).whenComplete((result, throwable) ->
		{
			if (throwable != null && !taskStarted.get())
			{
				pendingClickTaskKeys.remove(chunkKey);
				pendingClickTaskCount.decrementAndGet();
			}
		});
	}
	
	//endregion
	
	
	
    //================//
    // helper methods //
    //================//
	//region
	
	private static World GetEventLevel(WorldEvent e) { return e.world; }
	
	private static IServerLevelWrapper getServerLevelWrapper(WorldServer level) 
    { return ServerLevelWrapper.getWrapper(level); }
	
    private static IServerLevelWrapper getServerLevelWrapper(int dim, PlayerEvent event) 
    {
        WorldServer world = (WorldServer) event.player.worldObj;
        WorldServer worldDim = world.func_73046_m()
            .worldServerForDimension(dim);
        return getServerLevelWrapper(worldDim);
    }
	
    private static IServerPlayerWrapper getServerPlayerWrapper(PlayerEvent event) 
    { return ServerPlayerWrapper.getWrapper((EntityPlayerMP) event.player); }
	
	//endregion
	
	
	
	//================//
	// helper classes //
	//================//
	//region
	
	private static class ChunkLoadEvent
	{
		public final ChunkWrapper chunkWrapper;
		public final ILevelWrapper level;
		
		/** 
		 * used to track how long this event has
		 * been queued so we can clean up old tasks.
		 */
		public int numberOfTicksSinceQueue = 0;
		
		
		
		private ChunkLoadEvent(ChunkWrapper chunkWrapper, ILevelWrapper level)
		{
			this.chunkWrapper = chunkWrapper;
			this.level = level;
		}
	}
	
}
