package com.seibel.distanthorizons.common.wrappers.worldGeneration;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.api.internal.SharedApi;
import com.seibel.distanthorizons.core.api.internal.chunkUpdating.ChunkUpdateQueueManager;
import com.seibel.distanthorizons.core.api.internal.chunkUpdating.WorldChunkUpdateManager;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dependencyInjection.ModAccessorInjector;
import com.seibel.distanthorizons.core.enums.MinecraftTextFormat;
import com.seibel.distanthorizons.core.generation.DhLightingEngine;
import com.seibel.distanthorizons.core.level.DhServerLevel;
import com.seibel.distanthorizons.core.level.IDhServerLevel;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.util.ExceptionUtil;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.util.TimerUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IC2meAccessor;
import com.seibel.distanthorizons.coreapi.ModInfo;

import com.seibel.distanthorizons.forge.ForgeMain;
import com.seibel.distanthorizons.forge.ForgeServerProxy;
import copy.com.gtnewhorizons.angelica.compat.mojang.ChunkPos;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.ForgeChunkManager;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

public class InternalServerGenerator
{
    public static final DhLogger LOGGER = new DhLoggerBuilder()
        .name("LOD World Gen - Internal Server")
        .fileLevelConfig(Config.Common.Logging.logWorldGenEventToFile)
        .build();

    public static final DhLogger CHUNK_LOAD_LOGGER = new DhLoggerBuilder()
        .name("LOD Chunk Loading")
        .fileLevelConfig(Config.Common.Logging.logWorldGenChunkLoadEventToFile)
        .build();

    private static final IC2meAccessor C2ME_ACCESSOR = ModAccessorInjector.INSTANCE.get(IC2meAccessor.class);

    /**
     * Used to revert the ignore logic in {@link SharedApi} so
     * that a given chunk pos can be handled again.
     * A timer is used so we don't have to inject into MC's code and it works sell enough
     * most of the time.
     * If a chunk does get through due the timeout not being long enough that isn't the end of the world.
     */
    private static final int MS_TO_IGNORE_CHUNK_AFTER_COMPLETION = 5_000;

    private final ForgeChunkManager.Ticket DH_SERVER_GEN_TICKET;

    private static boolean c2meMissingWarningLogged = false;

    private final GlobalWorldGenParams params;
    private final IDhServerLevel dhServerLevel;
    @Nullable
    private final ChunkUpdateQueueManager updateManager;
    private final Timer chunkSaveIgnoreTimer = TimerUtil.CreateTimer("ChunkSaveIgnoreTimer");



    //=============//
    // constructor //
    //=============//

    public InternalServerGenerator(GlobalWorldGenParams params, IDhServerLevel dhServerLevel)
    {
        this.params = params;
        this.dhServerLevel = dhServerLevel;
        this.updateManager = WorldChunkUpdateManager.INSTANCE.getByLevelWrapper(this.dhServerLevel.getServerLevelWrapper());

        DH_SERVER_GEN_TICKET = ForgeChunkManager.requestTicket(ForgeMain.instance, params.mcServerLevel, ForgeChunkManager.Type.NORMAL);
        increaseChunkLimit(DH_SERVER_GEN_TICKET, 1000);
    }

    static void increaseChunkLimit(ForgeChunkManager.Ticket ticket, int newMaxDepth) {
        try {
            Field maxDepthField = ticket.getClass().getDeclaredField("maxDepth");
            maxDepthField.setAccessible(true);
            maxDepthField.setInt(ticket, newMaxDepth);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    //============//
    // generation //
    //============//

    public void generateChunksViaInternalServer(GenerationEvent genEvent)
    {
        this.runValidation();

        try
        {
            //=====================//
            // create gen requests //
            //=====================//

            ArrayList<CompletableFuture<Chunk>> getChunkFutureList = new ArrayList<>();
            {
                Iterator<ChunkPos> chunkPosIterator = ChunkPosGenStream.getIterator(genEvent.minPos.getX(), genEvent.minPos.getZ(), genEvent.widthInChunks, 0);
                while (chunkPosIterator.hasNext())
                {
                    ChunkPos chunkPos = chunkPosIterator.next();

                    CompletableFuture<Chunk> requestChunkFuture =
                        this.requestChunkFromServerAsync(chunkPos)
                            // log errors if necessary
                            .whenCompleteAsync(
                                (chunk, throwable) ->
                                {
                                    // unwrap the CompletionException if necessary
                                    Throwable actualThrowable = throwable;
                                    while (actualThrowable instanceof CompletionException)
                                    {
                                        actualThrowable = actualThrowable.getCause();
                                    }

                                    if (actualThrowable != null)
                                    {
                                        // ignore expected shutdown exceptions
                                        boolean isShutdownException =
                                            ExceptionUtil.isShutdownException(actualThrowable)
                                                || actualThrowable.getMessage().contains("Unloaded chunk");
                                        if (!isShutdownException)
                                        {
                                            CHUNK_LOAD_LOGGER.warn("DistantHorizons: Couldn't load chunk [" + chunkPos + "] from server, error: [" + actualThrowable.getMessage() + "].", actualThrowable);
                                        }
                                    }
                                });

                    getChunkFutureList.add(requestChunkFuture);
                }
            }



            //==============================//
            // wait for generation requests //
            //==============================//

            // Join-ing each thread will prevent DH from working on anything else
            // but will also prevent over-queuing world gen tasks.
            // If C2ME is present the CPU will still be well utilized.

            ArrayList<IChunkWrapper> chunkWrappers = new ArrayList<>();
            for (int i = 0; i < getChunkFutureList.size(); i++)
            {
                CompletableFuture<Chunk> getChunkFuture = getChunkFutureList.get(i);
                Chunk chunk = getChunkFuture.join();
                if (chunk != null)
                {
                    ChunkWrapper chunkWrapper = new ChunkWrapper(chunk, this.dhServerLevel.getLevelWrapper(), false);
                    chunkWrapper.createDhHeightMaps();
                    chunkWrappers.add(chunkWrapper);
                }
            }



            //==========================//
            // process generated chunks //
            //==========================//

            int maxSkyLight = this.dhServerLevel.getServerLevelWrapper().hasSkyLight() ? LodUtil.MAX_MC_LIGHT : LodUtil.MIN_MC_LIGHT;
            for (int i = 0; i < chunkWrappers.size(); i++)
            {
                ChunkWrapper chunkWrapper = (ChunkWrapper)chunkWrappers.get(i);

                // pre-generated chunks should have lighting but new ones won't
                if (!chunkWrapper.isDhBlockLightingCorrect())
                {
                    DhLightingEngine.INSTANCE.bakeChunkBlockLighting(chunkWrapper, chunkWrappers, maxSkyLight);
                }

                this.dhServerLevel.updateBeaconBeamsForChunk(chunkWrapper, chunkWrappers);
                genEvent.resultConsumer.accept(chunkWrapper);
            }
        }
        finally
        {
            // release all chunks from the server to prevent out of memory issues
            Iterator<ChunkPos> chunkPosIterator = ChunkPosGenStream.getIterator(genEvent.minPos.getX(), genEvent.minPos.getZ(), genEvent.widthInChunks, 0);
            while (chunkPosIterator.hasNext())
            {
                ChunkPos chunkPos = chunkPosIterator.next();
                this.releaseChunkToServer(this.params.mcServerLevel, this.params.dhServerLevel, chunkPos);
            }
        }
    }
    private void runValidation()
    {
        // DH thread check
        if (!DhApi.isDhThread()
            && ModInfo.IS_DEV_BUILD)
        {
            throw new IllegalStateException("Internal server generation should be called from one of DH's world gen thread. Current thread: ["+Thread.currentThread().getName()+"]");
        }


        // C2ME present?
        if (C2ME_ACCESSOR == null
            && !c2meMissingWarningLogged)
        {
            c2meMissingWarningLogged = true;

            String c2meWarning = "C2ME missing, \n" +
                "low CPU usage and slow world gen speeds expected. \n" +
                "DH is set to use MC's internal server for world gen \n" +
                "this mode is less efficient unless a mod like C2ME is present."
                ;

            if (Config.Common.Logging.Warning.showSlowWorldGenSettingWarnings.get())
            {
                String message =
                    MinecraftTextFormat.ORANGE + "Distant Horizons: slow world gen." + MinecraftTextFormat.CLEAR_FORMATTING + "\n" +
                        c2meWarning;
                ClientApi.INSTANCE.showChatMessageNextFrame(message);
            }

            LOGGER.warn(c2meWarning);
        }
    }

    private static void loadChunkIfNotExists(IChunkProvider provider, int x, int z)
    {
        if (!provider.chunkExists(x, z)) {
            provider.loadChunk(x, z);
        }
    }

    private CompletableFuture<Chunk> requestChunkFromServerAsync(ChunkPos chunkPos)
    {
        return ForgeServerProxy.schedule(true, () ->
        {
            ChunkProviderServer provider = (ChunkProviderServer)params.mcServerLevel.getChunkProvider();
            ForgeChunkManager.forceChunk(DH_SERVER_GEN_TICKET, new ChunkCoordIntPair(chunkPos.x, chunkPos.z));

            for (int i = -1; i <= 1; i++)
            {
                for (int j = -1; j <= 1; j++)
                {
                    if (i != 0 || j != 0)
                    {
                        loadChunkIfNotExists(provider, chunkPos.x + i, chunkPos.z + j);
                    }
                }
            }

            return provider.provideChunk(chunkPos.x, chunkPos.z);
        });
    }

    private CompletableFuture<Void> releaseChunkToServer(WorldServer level, IDhServerLevel dhLevel, ChunkPos chunkPos)
    {
        return ForgeServerProxy.schedule(false, () ->
        {
            ForgeChunkManager.unforceChunk(DH_SERVER_GEN_TICKET, new ChunkCoordIntPair(chunkPos.x, chunkPos.z));
            return null;
        });
    }



}
