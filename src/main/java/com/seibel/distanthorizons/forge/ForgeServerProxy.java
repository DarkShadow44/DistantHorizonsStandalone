package com.seibel.distanthorizons.forge;

import com.seibel.distanthorizons.common.AbstractModInitializer;
import com.seibel.distanthorizons.common.util.ProxyUtil;
import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;
import com.seibel.distanthorizons.common.wrappers.misc.ServerPlayerWrapper;
import com.seibel.distanthorizons.common.wrappers.world.ServerLevelWrapper;
import com.seibel.distanthorizons.common.wrappers.worldGeneration.BatchGenerationEnvironment;
import com.seibel.distanthorizons.common.wrappers.worldGeneration.InternalServerGenerator;
import com.seibel.distanthorizons.core.api.internal.ServerApi;
import com.seibel.distanthorizons.core.api.internal.SharedApi;
import com.seibel.distanthorizons.core.generation.BatchGenerator;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.util.threading.ThreadPoolUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IServerPlayerWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IServerLevelWrapper;
import com.seibel.distanthorizons.coreapi.DependencyInjection.WorldGeneratorInjector;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldServerMulti;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

public class ForgeServerProxy implements AbstractModInitializer.IEventProxy
{
	private static World GetEventLevel(WorldEvent e) { return e.world; }

	private final ServerApi serverApi = ServerApi.INSTANCE;
	private final boolean isDedicated;



	@Override
	public void registerEvents()
	{
		MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);
		if (this.isDedicated)
		{
			ForgePluginPacketSender.setPacketHandler(ServerApi.INSTANCE::pluginMessageReceived);
		}
	}



	//=============//
	// constructor //
	//=============//

	public ForgeServerProxy(boolean isDedicated)
	{
		this.isDedicated = isDedicated;
	}



	//========//
	// events //
	//========//

    public static boolean connected = false;

    public static void serverStopping() {
        while (!taskQueue.isEmpty()) {
            ScheduledTask<?> scheduledTask = taskQueue.poll();
            if (scheduledTask == null) {
                continue;
            }
            scheduledTask.future.complete(null);
        }
    }

    private class ChunkLoadEvent
    {
        public final ChunkWrapper chunk;
        public final ILevelWrapper level;
        public int age;

        private ChunkLoadEvent(ChunkWrapper chunk, ILevelWrapper level) {
            this.chunk = chunk;
            this.level = level;
        }
    }

    private static final Queue<ChunkLoadEvent> chunkLoadEvents = new ConcurrentLinkedQueue<>();

	// ServerTickEvent (at end)
	@SubscribeEvent
	public void serverTickEvent(TickEvent.ServerTickEvent event)
	{
        if(event.phase == TickEvent.Phase.END) {
            Iterator<ChunkLoadEvent> iterator = chunkLoadEvents.iterator();
            while (iterator.hasNext()) {
                ChunkLoadEvent chunkLoadEvent = iterator.next();
                if (chunkLoadEvent.chunk.isChunkReady()) {
                    this.serverApi.serverChunkLoadEvent(chunkLoadEvent.chunk, chunkLoadEvent.level);
                    iterator.remove();
                } else {
                    // Cleanup old events if they never got ready
                    chunkLoadEvent.age++;
                    if (chunkLoadEvent.age > 200) {
                        iterator.remove();
                    }
                }
            }

            int count = 0;
            while (!taskQueue.isEmpty()) {
                ScheduledTask<?> scheduledTask = taskQueue.poll();
                if (scheduledTask == null) {
                    continue;
                }
                scheduledTask.run();
                if (scheduledTask.isLimited()) {
                    count++;
                }
                if (count >= 5)
                {
                    break;
                }
            }
        }
	}

	// ServerLevelLoadEvent
	@SubscribeEvent
	public void serverLevelLoadEvent(WorldEvent.Load event)
	{
		if (GetEventLevel(event) instanceof WorldServer)
		{
			this.serverApi.serverLevelLoadEvent(getServerLevelWrapper((WorldServer) GetEventLevel(event)));
		}
	}

	// ServerLevelUnloadEvent
	@SubscribeEvent
	public void serverLevelUnloadEvent(WorldEvent.Unload event)
	{
		if (GetEventLevel(event) instanceof WorldServer)
		{
			this.serverApi.serverLevelUnloadEvent(getServerLevelWrapper((WorldServer) GetEventLevel(event)));
		}
        chunkLoadEvents.removeIf(x -> x.level.getWrappedMcObject() == event.world);
	}

	@SubscribeEvent
	public void serverChunkLoadEvent(ChunkEvent.Load event)
	{
        if (!(event.world instanceof WorldServer)) {
            return;
        }
		ILevelWrapper levelWrapper = ProxyUtil.getLevelWrapper(GetEventLevel(event));
		ChunkWrapper chunk = new ChunkWrapper(event.getChunk(), levelWrapper);
        if (chunk.isChunkReady()) {
            this.serverApi.serverChunkLoadEvent(chunk, levelWrapper);
            return;
        }
        chunkLoadEvents.add(new ChunkLoadEvent(chunk, levelWrapper));
	}

    @SubscribeEvent
    public void serverChunkUnLoadEvent(ChunkDataEvent.Save event)
    {
        if (!(event.world instanceof WorldServer)) {
            return;
        }
        ILevelWrapper levelWrapper = ProxyUtil.getLevelWrapper(GetEventLevel(event));
        Chunk chunk = event.getChunk();
        ChunkWrapper chunkWrapper = new ChunkWrapper(chunk, levelWrapper);
        ServerApi.INSTANCE.serverChunkSaveEvent(chunkWrapper, levelWrapper);

        BatchGenerator generator = (BatchGenerator) WorldGeneratorInjector.INSTANCE.get(levelWrapper);
        if (generator != null) {
            BatchGenerationEnvironment batchGenerationEnvironment = (BatchGenerationEnvironment) generator.generationEnvironment;

            if (batchGenerationEnvironment != null && batchGenerationEnvironment.internalServerGenerator.updateManager != null) {
                batchGenerationEnvironment.internalServerGenerator.updateManager.removePosToIgnore(new DhChunkPos(chunk.xPosition, chunk.zPosition));
            }
        }
    }

	@SubscribeEvent
	public void playerLoggedInEvent(PlayerEvent.PlayerLoggedInEvent event)
	{ this.serverApi.serverPlayerJoinEvent(getServerPlayerWrapper(event)); }
	@SubscribeEvent
	public void playerLoggedOutEvent(PlayerEvent.PlayerLoggedOutEvent event)
	{ this.serverApi.serverPlayerDisconnectEvent(getServerPlayerWrapper(event)); }
	@SubscribeEvent
	public void playerChangedDimensionEvent(PlayerEvent.PlayerChangedDimensionEvent event)
	{
		this.serverApi.serverPlayerLevelChangeEvent(
				getServerPlayerWrapper(event),
				getServerLevelWrapper(event.fromDim, event),
				getServerLevelWrapper(event.toDim, event)
		);
	}

    @SubscribeEvent
    public void clickBlockEvent(PlayerInteractEvent event)
    {
        ILevelWrapper wrappedLevel = ProxyUtil.getLevelWrapper(event.world);
        if (SharedApi.isChunkAtBlockPosAlreadyUpdating(wrappedLevel, event.x, event.z))
        {
            return;
        }

        schedule(false, () ->
        {
            Chunk chunk = event.world.getChunkFromBlockCoords(event.x, event.z);
            ChunkWrapper chunkWrapper = new ChunkWrapper(chunk, wrappedLevel);
            SharedApi.INSTANCE.applyChunkUpdate(chunkWrapper, wrappedLevel);
            return null;
        });
    }

    private static final Queue<ScheduledTask<?>> taskQueue = new ConcurrentLinkedQueue<>();

    // Schedule a task that runs on the main thread and returns a CompletableFuture result
    public static <T> CompletableFuture<T> schedule(boolean limited, Supplier<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        taskQueue.add(new ScheduledTask<>(task, future, limited));
        return future;
    }

    private static class ScheduledTask<T> {
        private final Supplier<T> task;
        private final CompletableFuture<T> future;
        private final boolean limited;

        public ScheduledTask(Supplier<T> task, CompletableFuture<T> future, boolean limited) {
            this.task = task;
            this.future = future;
            this.limited = limited;
        }

        public void run() {
            try {
                future.complete(task.get());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }

        public boolean isLimited() {
            return limited;
        }
    }


	//================//
	// helper methods //
	//================//

	private static IServerLevelWrapper getServerLevelWrapper(WorldServer level) { return ServerLevelWrapper.getWrapper(level); }


	private static IServerLevelWrapper getServerLevelWrapper(int dim, PlayerEvent event)
	{
        WorldServer world = (WorldServer) event.player.worldObj;
        WorldServer worldDim = world.func_73046_m().worldServerForDimension(dim);
		return getServerLevelWrapper(worldDim);
	}

	private static IServerPlayerWrapper getServerPlayerWrapper(PlayerEvent event) {
		return ServerPlayerWrapper.getWrapper(
            (EntityPlayerMP) event.player
		);
	}

}
