/*
 * This file is part of the Distant Horizons mod
 * licensed under the GNU LGPL v3 License.
 * Copyright (C) 2021 Tom Lee (TomTheFurry)
 * Copyright (C) 2020 James Seibel
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.distanthorizons.common.wrappers.worldGeneration;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableMap;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiDistantGeneratorMode;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiWorldGenerationStep;
import com.seibel.distanthorizons.core.api.internal.SharedApi;
import com.seibel.distanthorizons.core.api.internal.chunkUpdating.ChunkUpdateQueueManager;
import com.seibel.distanthorizons.core.api.internal.chunkUpdating.WorldChunkUpdateManager;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.level.IDhServerLevel;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.util.TimerUtil;
import com.seibel.distanthorizons.core.util.gridList.ArrayGridList;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.worldGeneration.IBatchGeneratorEnvironmentWrapper;

public final class BatchGenerationEnvironment implements IBatchGeneratorEnvironmentWrapper {

    public static final DhLogger LOGGER = new DhLoggerBuilder().name("LOD World Gen")
        .fileLevelConfig(Config.Common.Logging.logWorldGenEventToFile)
        .build();

    public static final DhLogger RATE_LIMITED_LOGGER = new DhLoggerBuilder().name("LOD World Gen")
        .maxCountPerSecond(1)
        .build();

    @NotNull
    public static final ImmutableMap<EDhApiWorldGenerationStep, Integer> WORLD_GEN_CHUNK_BORDER_NEEDED_BY_GEN_STEP;
    public static final int MAX_WORLD_GEN_CHUNK_BORDER_NEEDED;

    public static final long EXCEPTION_TIMER_RESET_TIME = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);
    public static final int EXCEPTION_COUNTER_TRIGGER = 20;

    /**
     * Used to revert the ignore logic in {@link SharedApi} so
     * that a given chunk pos can be handled again.
     * A timer is used so we don't have to inject into MC's code and it works sell enough
     * most of the time.
     * If a chunk does get through due the timeout not being long enough that isn't the end of the world.
     */
    private static final int MS_TO_IGNORE_CHUNK_AFTER_COMPLETION = 5_000;

    private final IDhServerLevel dhServerLevel;
    @Nullable
    private final ChunkUpdateQueueManager updateManager;

    public final InternalServerGenerator internalServerGenerator;

    private final Timer chunkSaveIgnoreTimer = TimerUtil.CreateTimer("ChunkSaveIgnoreTimer");

    public final LinkedBlockingQueue<GenerationEvent> generationEventQueue = new LinkedBlockingQueue<>();
    public final GlobalWorldGenParams globalParams;

    public boolean unsafeThreadingRecorded = false;
    public boolean generatedChunkWithoutBiomeWarningLogged = false;
    public int unknownExceptionCount = 0;
    public long lastExceptionTriggerTime = 0;

    public static ThreadLocal<Boolean> isDhWorldGenThreadRef = new ThreadLocal<>();

    public static boolean isThisDhWorldGenThread() {
        return (isDhWorldGenThreadRef.get() != null);
    }

    // ==============//
    // constructors //
    // ==============//

    static {
        boolean isTerraFirmaCraftPresent = false;
        try {
            Class.forName("net.dries007.tfc.world.TFCChunkGenerator");
            isTerraFirmaCraftPresent = true;
            LOGGER.info("TerraFirmaCraft detected.");
        } catch (ClassNotFoundException ignore) {}

        ImmutableMap.Builder<EDhApiWorldGenerationStep, Integer> builder = ImmutableMap.builder();
        builder.put(EDhApiWorldGenerationStep.EMPTY, 1);
        builder.put(EDhApiWorldGenerationStep.STRUCTURE_START, 0);
        builder.put(EDhApiWorldGenerationStep.STRUCTURE_REFERENCE, 0);
        builder.put(EDhApiWorldGenerationStep.BIOMES, isTerraFirmaCraftPresent ? 1 : 0);
        builder.put(EDhApiWorldGenerationStep.NOISE, isTerraFirmaCraftPresent ? 1 : 0);
        builder.put(EDhApiWorldGenerationStep.SURFACE, 0);
        builder.put(EDhApiWorldGenerationStep.CARVERS, 0);
        builder.put(EDhApiWorldGenerationStep.LIQUID_CARVERS, 0);
        builder.put(EDhApiWorldGenerationStep.FEATURES, 0);
        builder.put(EDhApiWorldGenerationStep.LIGHT, 0);
        WORLD_GEN_CHUNK_BORDER_NEEDED_BY_GEN_STEP = builder.build();

        // in James' testing as of 2025-09-13 a border here of 2
        // and a getChunkPosToGenerateStream() radius of 14 provided more accurate
        // structure generation, however it also caused extreme server lag
        // a border of 0 here and a getChunkPosToGenerateStream() radius of 8 provided
        // good-enough structure generation while not lagging the server
        MAX_WORLD_GEN_CHUNK_BORDER_NEEDED = 0;
    }

    public BatchGenerationEnvironment(IDhServerLevel dhServerLevel) {
        this.dhServerLevel = dhServerLevel;
        this.updateManager = WorldChunkUpdateManager.INSTANCE
            .getByLevelWrapper(this.dhServerLevel.getServerLevelWrapper());
        this.globalParams = new GlobalWorldGenParams(dhServerLevel);
        this.internalServerGenerator = new InternalServerGenerator(this.globalParams, this.dhServerLevel);
    }

    // =================//
    // synchronization //
    // =================//

    public void updateAllFutures() {
        if (this.unknownExceptionCount > 0) {
            if (System.nanoTime() - this.lastExceptionTriggerTime >= EXCEPTION_TIMER_RESET_TIME) {
                this.unknownExceptionCount = 0;
            }
        }

        // Update all current out standing jobs
        Iterator<GenerationEvent> iter = this.generationEventQueue.iterator();
        while (iter.hasNext()) {
            GenerationEvent event = iter.next();
            if (event.future.isDone()) {
                if (event.future.isCompletedExceptionally() && !event.future.isCancelled()) {
                    try {
                        event.future.get(); // Should throw exception
                        LodUtil.assertNotReach(
                            "Exceptionally completed world gen Future should have thrown an exception.");
                    } catch (Exception e) {
                        this.unknownExceptionCount++;
                        this.lastExceptionTriggerTime = System.nanoTime();
                        LOGGER.error(
                            "Batching World Generator event [" + event + "] threw an exception: " + e.getMessage(),
                            e);
                    }
                }

                iter.remove();
            }
        }

        if (this.unknownExceptionCount > EXCEPTION_COUNTER_TRIGGER) {
            LOGGER.error("Too many exceptions in Batching World Generator! Disabling the generator.");
            this.unknownExceptionCount = 0;
            Config.Common.WorldGenerator.enableDistantGeneration.set(false);
        }
    }

    private static <T> ArrayGridList<T> GetCutoutFrom(ArrayGridList<T> total, int border) {
        return new ArrayGridList<>(total, border, total.gridSize - border);
    }

    private static <T> ArrayGridList<T> GetCutoutFrom(ArrayGridList<T> total, EDhApiWorldGenerationStep step) {
        return GetCutoutFrom(total, WORLD_GEN_CHUNK_BORDER_NEEDED_BY_GEN_STEP.get(step));
    }

    // queue task //

    @Override
    public CompletableFuture<Void> queueGenEvent(int minX, int minZ, int chunkWidthCount,
        EDhApiDistantGeneratorMode generatorMode, EDhApiWorldGenerationStep targetStep,
        ExecutorService worldGeneratorThreadPool, Consumer<IChunkWrapper> resultConsumer) {
        GenerationEvent genEvent = GenerationEvent.start(
            new DhChunkPos(minX, minZ),
            chunkWidthCount,
            this,
            generatorMode,
            targetStep,
            resultConsumer,
            worldGeneratorThreadPool);
        this.generationEventQueue.add(genEvent);
        return genEvent.future;
    }

    // ================//
    // base overrides //
    // ================//

    @Override
    public void close() {
        LOGGER.info("Closing [" + BatchGenerationEnvironment.class.getSimpleName() + "]");

        // cancel in-progress tasks
        Iterator<GenerationEvent> genEventIter = this.generationEventQueue.iterator();
        while (genEventIter.hasNext()) {
            GenerationEvent event = genEventIter.next();
            event.future.cancel(true);
            genEventIter.remove();
        }
    }

    // ================//
    // helper methods //
    // ================//

    /**
     * Called before code that may run for an extended period of time. <br>
     * This is necessary to allow canceling world gen since waiting
     * for some world gen requests to finish can take a while.
     */
    public static void throwIfThreadInterrupted() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException(
                "[" + BatchGenerationEnvironment.class.getSimpleName() + "] task interrupted.");
        }
    }
}
