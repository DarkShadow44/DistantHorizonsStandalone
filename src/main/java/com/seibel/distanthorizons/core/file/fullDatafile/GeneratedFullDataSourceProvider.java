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

package com.seibel.distanthorizons.core.file.fullDatafile;

import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiWorldGenerationStep;
import com.seibel.distanthorizons.core.api.internal.SharedApi;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dataObjects.fullData.sources.FullDataSourceV2;
import com.seibel.distanthorizons.core.file.fullDatafile.V2.FullDataSourceProviderV2;
import com.seibel.distanthorizons.core.file.fullDatafile.V2.FullDataUpdatePropagatorV2;
import com.seibel.distanthorizons.core.file.structure.ISaveStructure;
import com.seibel.distanthorizons.core.generation.DhLightingEngine;
import com.seibel.distanthorizons.core.generation.IFullDataSourceRetrievalQueue;
import com.seibel.distanthorizons.core.generation.tasks.DataSourceRetrievalResult;
import com.seibel.distanthorizons.core.generation.tasks.ERetrievalResultState;
import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pooling.PhantomArrayListCheckout;
import com.seibel.distanthorizons.core.pooling.PhantomArrayListPool;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos2D;
import com.seibel.distanthorizons.core.render.renderer.IDebugRenderable;
import com.seibel.distanthorizons.core.util.ExceptionUtil;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.util.threading.PriorityTaskPicker;
import com.seibel.distanthorizons.core.util.threading.ThreadPoolUtil;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class GeneratedFullDataSourceProvider extends FullDataSourceProviderV2 implements IDebugRenderable
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();;
	
	/** 
	 * Having this number too high causes the system to become overwhelmed by
	 * world gen requests and other jobs won't be done. <br>
	 * IE: LODs won't update or render because world gen is hogging the CPU.
	 * <br><br>
	 * TODO this should be dynamically allocated based on CPU load
	 *  and abilities.
	 */
	public static final int MAX_WORLD_GEN_REQUESTS_PER_THREAD = 20;
	
	public static final PhantomArrayListPool ARRAY_LIST_POOL = new PhantomArrayListPool("Generated Provider");
	
	
	private final AtomicReference<IFullDataSourceRetrievalQueue> worldGenQueueRef = new AtomicReference<>(null);
	private final ArrayList<IOnWorldGenCompleteListener> onWorldGenTaskCompleteListeners = new ArrayList<>();
	
	protected final DelayedFullDataSourceSaveCache delayedFullDataSourceSaveCache = new DelayedFullDataSourceSaveCache(this::onDataSourceSaveAsync, 10_000);
	
	
	
	//=============//
	// constructor //
	//=============//
	
	public GeneratedFullDataSourceProvider(IDhLevel level, ISaveStructure saveStructure) throws SQLException, IOException 
	{ this(level, saveStructure, null); }
	public GeneratedFullDataSourceProvider(IDhLevel level, ISaveStructure saveStructure, @Nullable File saveDirOverride) throws SQLException, IOException
	{ super(level, saveStructure, saveDirOverride); }
	
	
	
	//=================//
	// event listeners //
	//=================//
	
	public void addWorldGenCompleteListener(IOnWorldGenCompleteListener listener) 
	{
		synchronized (this.onWorldGenTaskCompleteListeners)
		{
			this.onWorldGenTaskCompleteListeners.add(listener);
		}
	}
	public void removeWorldGenCompleteListener(IOnWorldGenCompleteListener listener) 
	{
		synchronized (this.onWorldGenTaskCompleteListeners)
		{
			this.onWorldGenTaskCompleteListeners.remove(listener);
		}
	}
	
	
	
	//========//
	// events //
	//========//
	
	private void onWorldGenTaskComplete(DataSourceRetrievalResult genTaskResult, Throwable exception)
	{
		try
		{
			if (exception != null)
			{
				return;
			}
			
			if (genTaskResult.state == ERetrievalResultState.FAIL)
			{
				LodUtil.assertTrue(genTaskResult.dataSource == null, "Errored retrieval object should not have a datasource.");
				
				// don't log shutdown exceptions
				if (!ExceptionUtil.isInterruptOrReject(exception))
				{
					LOGGER.error("Uncaught Gen Task Exception at [" + genTaskResult.pos + "], error: [" + exception.getMessage() + "].", exception);
				}
			}
			else if (genTaskResult.state == ERetrievalResultState.SUCCESS)
			{
				LodUtil.assertTrue(genTaskResult.dataSource != null, "Successful retrieval object should have a datasource.");
				
				this.dataUpdater.updateDataSource(genTaskResult.dataSource);
				this.fireOnGenPosSuccessListeners(genTaskResult.pos);
				genTaskResult.dataSource.close();
			}
			else if (genTaskResult.state == ERetrievalResultState.REQUIRES_SPLITTING)
			{
				// task was split
				LodUtil.assertTrue(genTaskResult.dataSource == null, "Split retrieval object should not have a datasource.");
			}
			else
			{
				// shouldn't happen, but just in case
				LOGGER.warn("Unexpected gen Task state at: [" + DhSectionPos.toString(genTaskResult.pos) + "], state: [" + genTaskResult.state + "], datasource: NULL, exception: NULL.");
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Unexpected issue during onWorldGenTaskComplete, error: ["+e.getMessage()+"].", e);
		}
	}
	
	// TODO only fire after the section has finished generated or once every X seconds
	private void fireOnGenPosSuccessListeners(long pos)
	{
		// synchronized to prevent a rare issue where the world generator is being shut down while this listener is firing
		synchronized (this.onWorldGenTaskCompleteListeners)
		{
			// fire the event listeners 
			for (IOnWorldGenCompleteListener listener : this.onWorldGenTaskCompleteListeners)
			{
				listener.onWorldGenTaskComplete(pos);
			}
		}
	}
	
	
	
	//===================================//
	// world gen (data source retrieval) //
	//===================================//
	
	public byte lowestDataDetailLevel()
	{
		IFullDataSourceRetrievalQueue fullDataSourceRetrievalQueue = this.worldGenQueueRef.get();
		if (fullDataSourceRetrievalQueue == null)
		{
			return DhSectionPos.SECTION_MINIMUM_DETAIL_LEVEL;
		}
		
		return (byte) (DhSectionPos.SECTION_MINIMUM_DETAIL_LEVEL + fullDataSourceRetrievalQueue.lowestDataDetail());
	}
	
	/**
	 * Assigns the queue for handling world gen and does first time setup as well. <br> 
	 * Assumes there isn't a pre-existing queue. 
	 */
	public void setWorldGenerationQueue(IFullDataSourceRetrievalQueue newWorldGenQueue)
	{
		boolean oldQueueExists = this.worldGenQueueRef.compareAndSet(null, newWorldGenQueue);
		LodUtil.assertTrue(oldQueueExists, "previous world gen queue is still here!");
		LOGGER.info("Set world gen queue for level [" + this.levelId + "].");
	}
	
	@Override
	public boolean canRetrieveMissingDataSources() { return true; }
	
	@Override
	public void setEstimatedRemainingRetrievalChunkCount(int newCount) 
	{
		IFullDataSourceRetrievalQueue worldGenQueue = this.worldGenQueueRef.get();
		if (worldGenQueue != null)
		{
			worldGenQueue.setRetrievalEstimatedRemainingChunkCount(newCount);
		}
	}
	
	@Override
	public boolean canQueueRetrievalNow() { return this.canQueueRetrievalNow(false); }
	public boolean canQueueRetrievalNow(boolean pruneWaitingTasksAboveLimit)
	{
		if (!super.canQueueRetrievalNow())
		{
			return false;
		}
		
		
		IFullDataSourceRetrievalQueue worldGenQueue = this.worldGenQueueRef.get();
		if (worldGenQueue == null)
		{
			// we can't queue anything if the world generator isn't set up yet
			return false;
		}
		
		
		PriorityTaskPicker.Executor renderLoadExecutor = ThreadPoolUtil.getRenderLoadingExecutor();
		if (renderLoadExecutor == null 
			|| renderLoadExecutor.getQueueSize() >= FullDataUpdatePropagatorV2.getMaxPropagateTaskCount() / 2)
		{
			// don't queue additional world gen requests if the render loader handler is overwhelmed,
			// otherwise LODs may not load in properly
			return false;
		}
		
		PriorityTaskPicker.Executor fileHandlerExecutor = ThreadPoolUtil.getFileHandlerExecutor();
		if (fileHandlerExecutor == null 
			|| fileHandlerExecutor.getQueueSize() >= FullDataUpdatePropagatorV2.getMaxPropagateTaskCount() / 2)
		{
			// don't queue additional world gen requests if the file handler is overwhelmed,
			// otherwise LODs may not load in properly
			return false;
		}
		
		
		int maxQueuedChunkCount = MAX_WORLD_GEN_REQUESTS_PER_THREAD * Config.Common.MultiThreading.numberOfThreads.get(); // for now we're just using the same logic as the world gen threads, it works well enough
		if (SharedApi.INSTANCE.getQueuedChunkUpdateCount() >= maxQueuedChunkCount)
		{
			// don't queue additional world gen requests if there are
			// a lot of chunks waiting to update
			// (this is done to reduce thread starvation for chunk updates)
			return false;
		}
		
		
		int maxWorldGenQueueCount = MAX_WORLD_GEN_REQUESTS_PER_THREAD * Config.Common.MultiThreading.numberOfThreads.get();

		if (this.delayedFullDataSourceSaveCache.getUnsavedCount() >= maxWorldGenQueueCount)
		{
			// don't queue additional world gen requests if there are
			// a lot of data sources in memory 
			// (this is done to prevent infinite memory growth)
			
			// clear out the data sources that are in memory so
			// we can start queuing new world gen tasks
			this.delayedFullDataSourceSaveCache.flush();
			
			return false;
		}
		
		
		int availableTaskSlots = maxWorldGenQueueCount - worldGenQueue.getWaitingTaskCount();
		if (availableTaskSlots == 0)
		{
			return false;
		}
		else if (availableTaskSlots < 0)
		{
			if (pruneWaitingTasksAboveLimit)
			{
				AtomicInteger tasksToCancel = new AtomicInteger(availableTaskSlots * -1);
				worldGenQueue.removeRetrievalRequestIf(taskPos -> tasksToCancel.getAndDecrement() > 0);
			}
			else
			{
				// don't queue additional world gen requests beyond the max allotted count
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public CompletableFuture<DataSourceRetrievalResult> queuePositionForRetrieval(Long genPos)
	{
		IFullDataSourceRetrievalQueue worldGenQueue = this.worldGenQueueRef.get();
		if (worldGenQueue == null)
		{
			return null;
		}
		
		CompletableFuture<DataSourceRetrievalResult> worldGenFuture = worldGenQueue.submitRetrievalTask(genPos, (byte) (DhSectionPos.getDetailLevel(genPos) - DhSectionPos.SECTION_MINIMUM_DETAIL_LEVEL));
		worldGenFuture.whenComplete(this::onWorldGenTaskComplete);
		
		return worldGenFuture;
	}
	
	@Override
	public void removeRetrievalRequestIf(DhSectionPos.ICancelablePrimitiveLongConsumer removeIf)
	{
		IFullDataSourceRetrievalQueue worldGenQueue = this.worldGenQueueRef.get();
		if (worldGenQueue != null)
		{
			worldGenQueue.removeRetrievalRequestIf(removeIf);
		}
	}
	
	@Override
	public void clearRetrievalQueue() { this.worldGenQueueRef.set(null); }
	
	
	public boolean generationStepsAreFullyGenerated(ByteArrayList columnGenerationSteps)
	{
		return IntStream.range(0, columnGenerationSteps.size())
			.noneMatch((int intValue) ->
			{
				byte value = columnGenerationSteps.getByte(intValue);
				return value == EDhApiWorldGenerationStep.EMPTY.value
					|| value == EDhApiWorldGenerationStep.DOWN_SAMPLED.value;
			});
	}
	
	
	@Override
	public LongArrayList getPositionsToRetrieve(long pos)
	{
		IFullDataSourceRetrievalQueue worldGenQueue = this.worldGenQueueRef.get();
		if (worldGenQueue == null)
		{
			return null;
		}
		
		
		// don't check any child positions if this position is already fully generated 
		if (this.repo.existsWithKey(pos))
		{
			try(PhantomArrayListCheckout checkout = ARRAY_LIST_POOL.checkoutArrays(1, 0, 0))
			{
				ByteArrayList columnGenStepArray = checkout.getByteArray(0, FullDataSourceV2.WIDTH*FullDataSourceV2.WIDTH);
				this.repo.getColumnGenerationStepForPos(pos, columnGenStepArray);
				if (columnGenStepArray.size() != 0)
				{
					boolean positionFullyGenerated = true;
					
					// check if any positions are ungenerated
					for (int i = 0; i < columnGenStepArray.size(); i++)
					{
						if (columnGenStepArray.getByte(i) == EDhApiWorldGenerationStep.EMPTY.value
							|| columnGenStepArray.getByte(i) == EDhApiWorldGenerationStep.DOWN_SAMPLED.value)
						{
							positionFullyGenerated = false;
							break;
						}
					}
					
					if (positionFullyGenerated)
					{
						return new LongArrayList();
					}
				}
			}
		}
		
		
		
		// this section is missing one or more columns, queue the missing ones for generation.
		LongArrayList generationList = new LongArrayList();
		
		byte lowestGeneratorDetailLevel = (byte) Math.min(
			worldGenQueue.lowestDataDetail() + DhSectionPos.SECTION_MINIMUM_DETAIL_LEVEL,
			DhSectionPos.getDetailLevel(pos));
		
		DhSectionPos.forEachChildAtDetailLevel(pos, lowestGeneratorDetailLevel, (genPos) ->
		{
			if (!this.repo.existsWithKey(genPos))
			{
				// nothing exists for this position, it needs generation
				generationList.add(genPos);
			}
			else
			{
				
				EDhApiWorldGenerationStep currentMinWorldGenStep = EDhApiWorldGenerationStep.LIGHT;
				try(PhantomArrayListCheckout checkout = ARRAY_LIST_POOL.checkoutArrays(1, 0, 0))
				{
					ByteArrayList columnGenerationSteps = checkout.getByteArray(0, FullDataSourceV2.WIDTH*FullDataSourceV2.WIDTH);
					this.repo.getColumnGenerationStepForPos(genPos, columnGenerationSteps);
					if (columnGenerationSteps.isEmpty())
					{
						// shouldn't happen, but just in case
						return;
					}
					
					
					
					checkWorldGenLoop:
					for (int x = 0; x < FullDataSourceV2.WIDTH; x++)
					{
						for (int z = 0; z < FullDataSourceV2.WIDTH; z++)
						{
							int index = FullDataSourceV2.relativePosToIndex(x, z);
							byte genStepValue = columnGenerationSteps.getByte(index);
							
							if (genStepValue < currentMinWorldGenStep.value)
							{
								EDhApiWorldGenerationStep newWorldGenStep = EDhApiWorldGenerationStep.fromValue(genStepValue);
								if (newWorldGenStep != null && newWorldGenStep.value < currentMinWorldGenStep.value)
								{
									currentMinWorldGenStep = newWorldGenStep;
								}
							}
							
							if (currentMinWorldGenStep == EDhApiWorldGenerationStep.EMPTY 
								|| currentMinWorldGenStep == EDhApiWorldGenerationStep.DOWN_SAMPLED)
							{
								// queue the task
								break checkWorldGenLoop;
							}
						}
					}
				}
				
				if (currentMinWorldGenStep != EDhApiWorldGenerationStep.EMPTY
					&& currentMinWorldGenStep != EDhApiWorldGenerationStep.DOWN_SAMPLED)
				{
					// no world gen needed for this position
					return;
				}
				
				generationList.add(genPos);
			}
		});
		
		return generationList;
	}
	
	
	
	//================//
	// base overrides //
	//================//
	
	@Override 
	public void close()
	{
		super.close();
		
		this.delayedFullDataSourceSaveCache.close();
	}
	
	
	
	
	
	//================//
	// helper classes //
	//================//
	
	private CompletableFuture<Void> onDataSourceSaveAsync(FullDataSourceV2 fullDataSource) 
	{
		// block lights should have been populated at the chunkWrapper stage
		// waiting to populate the data source's skylight at this stage prevents re-lighting and
		// allows us to reduce cross-chunk lighting issues by lighting the whole 4x4 LOD at once
		int skyLight = this.level.getLevelWrapper().hasSkyLight() ? LodUtil.MAX_MC_LIGHT : LodUtil.MIN_MC_LIGHT;
		DhLightingEngine.INSTANCE.bakeDataSourceSkyLight(fullDataSource, skyLight);
		
		return this.updateDataSourceAsync(fullDataSource);
	}
	
	
	
	/** used by external event listeners */
	public interface IOnWorldGenCompleteListener
	{
		boolean shouldDoWorldGen();
		
		DhBlockPos2D getTargetPosForGeneration();
		
		/** Fired whenever a section has completed generating */
		void onWorldGenTaskComplete(long pos);
		
	}
	
}
