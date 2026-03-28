/*
 * This file is part of the Distant Horizons mod
 * licensed under the GNU LGPL v3 License.
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

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiDistantGeneratorMode;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiWorldGenerationStep;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.util.ExceptionUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;

public final class GenerationEvent {

    private static final DhLogger LOGGER = new DhLoggerBuilder().build();;

    private static final AtomicInteger DEBUG_ID_REF = new AtomicInteger(0);

    /** can be used for troubleshooting */
    public final int id;

    public final DhChunkPos minPos;
    public final int widthInChunks;
    public final EDhApiWorldGenerationStep targetGenerationStep;
    public final EDhApiDistantGeneratorMode generatorMode;
    public final CompletableFuture<Void> future;
    public final Consumer<IChunkWrapper> resultConsumer;

    // =============//
    // constructor //
    // =============//

    private GenerationEvent(DhChunkPos minPos, int widthInChunks, BatchGenerationEnvironment generationGroup,
        EDhApiDistantGeneratorMode generatorMode, EDhApiWorldGenerationStep targetGenerationStep,
        Consumer<IChunkWrapper> resultConsumer) {
        this.id = DEBUG_ID_REF.getAndIncrement();

        this.minPos = minPos;
        this.widthInChunks = widthInChunks;
        this.targetGenerationStep = targetGenerationStep;
        this.generatorMode = generatorMode;
        this.future = new CompletableFuture<>();
        this.resultConsumer = resultConsumer;
    }

    // =======//
    // start //
    // =======//

    public static GenerationEvent start(DhChunkPos minPos, int widthInChunks, BatchGenerationEnvironment genEnvironment,
        EDhApiDistantGeneratorMode generatorMode, EDhApiWorldGenerationStep target,
        Consumer<IChunkWrapper> resultConsumer, ExecutorService worldGeneratorThreadPool) {
        GenerationEvent genEvent = new GenerationEvent(
            minPos,
            widthInChunks,
            genEnvironment,
            generatorMode,
            target,
            resultConsumer);

        try {
            worldGeneratorThreadPool.execute(() -> {
                try {
                    BatchGenerationEnvironment.isDhWorldGenThreadRef.set(true);

                    // Only support internal server
                    genEnvironment.internalServerGenerator.generateChunksViaInternalServer(genEvent);
                    genEvent.future.complete(null);
                } catch (Throwable initialThrowable) {
                    handleWorldGenThrowable(genEvent, initialThrowable);
                } finally {
                    BatchGenerationEnvironment.isDhWorldGenThreadRef.remove();
                }
            });
        } catch (RejectedExecutionException e) {
            genEvent.future.completeExceptionally(e);
        }

        return genEvent;
    }

    /** There's probably a better way to handle this, but it'll work for now */
    private static void handleWorldGenThrowable(GenerationEvent generationEvent, Throwable initialThrowable) {
        Throwable throwable = initialThrowable;
        while (throwable instanceof CompletionException) {
            throwable = throwable.getCause();
        }

        boolean isShutdownException = ExceptionUtil.isShutdownException(throwable);
        if (isShutdownException) {
            // these exceptions can be ignored, generally they just mean
            // the thread is busy so it'll need to try again later.
            // FIXME this should cause the world gen task to be re-queued so we can try again later
            // however, currently it can cause large gaps in the world gen instead.
            // These gaps will generate correctly if the level is reloaded and the world gen is re-queued,
            // however this is makes it look like the generator isn't working or skipped something.
        } else {
            generationEvent.future.completeExceptionally(throwable);
        }
    }

    // ================//
    // base overrides //
    // ================//

    @Override
    public String toString() {
        return this.id + ":" + this.widthInChunks + "@" + this.minPos + "(" + this.targetGenerationStep + ")";
    }

}
