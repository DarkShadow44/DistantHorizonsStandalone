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

package com.seibel.distanthorizons.core.render;

import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dataObjects.fullData.sources.FullDataSourceV2;
import com.seibel.distanthorizons.core.dataObjects.render.ColumnRenderSource;
import com.seibel.distanthorizons.core.dataObjects.render.bufferBuilding.ColumnRenderBufferBuilder;
import com.seibel.distanthorizons.core.dataObjects.render.bufferBuilding.LodQuadBuilder;
import com.seibel.distanthorizons.core.dataObjects.transformers.FullDataToRenderDataTransformer;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.enums.EDhDirection;
import com.seibel.distanthorizons.core.file.fullDatafile.V2.FullDataSourceProviderV2;
import com.seibel.distanthorizons.core.level.IDhClientLevel;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos2D;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.render.glObject.GLProxy;
import com.seibel.distanthorizons.core.render.renderer.IDebugRenderable;
import com.seibel.distanthorizons.core.dataObjects.render.bufferBuilding.LodBufferContainer;
import com.seibel.distanthorizons.core.render.renderer.DebugRenderer;
import com.seibel.distanthorizons.core.render.renderer.generic.BeaconRenderHandler;
import com.seibel.distanthorizons.core.sql.dto.BeaconBeamDTO;
import com.seibel.distanthorizons.core.sql.repo.BeaconBeamRepo;
import com.seibel.distanthorizons.core.util.threading.PriorityTaskPicker;
import com.seibel.distanthorizons.core.util.threading.ThreadPoolUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import org.jetbrains.annotations.Nullable;

import javax.annotation.WillNotClose;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A render section represents an area that could be rendered.
 * For more information see {@link LodQuadTree}.
 */
public class LodRenderSection implements IDebugRenderable, AutoCloseable
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	private static final IMinecraftClientWrapper MC = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	
	
	
	public final long pos;
	
	private final IDhClientLevel level;
	private final IClientLevelWrapper levelWrapper;
	@WillNotClose
	private final FullDataSourceProviderV2 fullDataSourceProvider;
	private final LodQuadTree quadTree;
	private final AtomicInteger uploadTaskCountRef;
	
	/** 
	 * contains the list of beacons currently being rendered in this section 
	 * if this list is modified the {@link LodRenderSection#beaconRenderHandler} should be updated to match.
	 */
	private final List<BeaconBeamDTO> activeBeaconList = new ArrayList<>();
	@Nullable
	public final BeaconRenderHandler beaconRenderHandler;
	@Nullable
	public final BeaconBeamRepo beaconBeamRepo;
	
	
	private boolean renderingEnabled = false;
	
	/** this reference is necessary so we can determine what VBO to render */
	public LodBufferContainer bufferContainer; 
	
	
	/** 
	 * Encapsulates everything between pulling data from the database (including neighbors)
	 * up to the point when geometry data is uploaded to the GPU.
	 */
	private CompletableFuture<Void> getAndBuildRenderDataFuture = null;
	@Nullable
	public CompletableFuture<Void> getRenderDataBuildFuture() { return this.getAndBuildRenderDataFuture; } 
	
	/** 
	 * used alongside {@link LodRenderSection#getAndBuildRenderDataFuture} so we can remove
	 * unnecessary tasks from the executor.
	 */
	private Runnable getAndBuildRenderDataRunnable = null;
	
	/** 
	 * Represents just uploading the {@link LodQuadBuilder} to the GPU. <br>
	 * Separate from {@link LodRenderSection#getAndBuildRenderDataFuture} because they run on
	 * different threads (buffer uploading is on the MC render thread) and need to be canceled separately.
	 */
	private CompletableFuture<LodBufferContainer> bufferUploadFuture = null;
	
	
	
	//=============//
	// constructor //
	//=============//
	//region constructor
	
	public LodRenderSection(
			long pos, 
			LodQuadTree quadTree, 
			IDhClientLevel level, FullDataSourceProviderV2 fullDataSourceProvider,
			AtomicInteger uploadTaskCountRef)
	{
		this.pos = pos;
		this.quadTree = quadTree;
		this.level = level;
		this.levelWrapper = level.getClientLevelWrapper();
		this.fullDataSourceProvider = fullDataSourceProvider;
		this.uploadTaskCountRef = uploadTaskCountRef;
		
		this.beaconRenderHandler = this.quadTree.beaconRenderHandler;
		this.beaconBeamRepo = this.level.getBeaconBeamRepo();
		
		DebugRenderer.register(this, Config.Client.Advanced.Debugging.DebugWireframe.showRenderSectionStatus);
	}
	
	//endregion constructor
	
	
	
	//======================================//
	// render data generation and uploading //
	//======================================//
	//region render data uploading
	
	/** @return true if the upload started, false if it wasn't able to for any reason */
	public synchronized boolean uploadRenderDataToGpuAsync()
	{
		if (!GLProxy.hasInstance())
		{
			// it's possible to try uploading buffers before the GLProxy has been initialized
			// which would cause the system to crash
			return false;
		}
		
		if (this.getAndBuildRenderDataFuture != null)
		{
			// don't accidentally queue multiple uploads at the same time
			return false;
		}
		
		PriorityTaskPicker.Executor executor = ThreadPoolUtil.getRenderLoadingExecutor();
		if (executor == null || executor.isTerminated())
		{
			return false;
		}
		
		// Only queue a some of the upload tasks at a time,
		// this means the closer (higher priority) tasks will load first.
		// This also prevents issues where the nearby tasks are canceled due to
		// LOD detail level changing, and having holes in the world
		if (this.uploadTaskCountRef.getAndIncrement() > executor.getPoolSize())
		{
			this.uploadTaskCountRef.decrementAndGet();
			return false;
		}
		
		try
		{
			CompletableFuture<Void> future = new CompletableFuture<>();
			this.getAndBuildRenderDataFuture = future; // TODO should use a setter/getter to guard against replacing an incomplete future
			future.handle((voidObj, throwable) -> 
			{
				// this has to fire are the end of every added future, otherwise we'll lock up and nothing will load
				this.uploadTaskCountRef.decrementAndGet(); // TODO there is an issue where this variable isn't decremented properly, preventing LODs from loading in, or loading much slower
				return null; 
			});
			
			this.getAndBuildRenderDataRunnable = () ->
			{
				this.getAndRefreshRenderingBeacons();
				this.getAndUploadRenderDataToGpuAsync()
					.thenRun(() -> 
					{
						// the future is passed in separately (IE not using the local var) to prevent any possible race condition null pointers
						future.complete(null);
						// the task is done, we don't need to track these anymore
						this.getAndBuildRenderDataFuture = null;
						this.getAndBuildRenderDataRunnable = null;
					});
			};
			executor.execute(this.getAndBuildRenderDataRunnable);
			
			return true;
		}
		catch (RejectedExecutionException ignore)
		{
			this.getAndBuildRenderDataFuture.complete(null);
			this.getAndBuildRenderDataFuture = null;
			this.getAndBuildRenderDataRunnable = null;
			
			/* the thread pool was probably shut down because it's size is being changed, just wait a sec and it should be back */
			return false;
		}
	}
	private CompletableFuture<Void> getAndUploadRenderDataToGpuAsync()
	{
		// get the center pos data
		return this.getRenderSourceForPosAsync(this.pos, null)
			.thenCompose((ColumnRenderSource thisRenderSource) -> 
			{
				try
				{
					if (thisRenderSource == null)
					{
						// nothing needs to be rendered
						// TODO how doesn't this cause infinite file handler loops?
						//  to trigger an upload we check if the buffer is null, and we aren't
						//  setting the render buffer here
						return CompletableFuture.completedFuture(null);
					}
					
					
					boolean enableTransparency = Config.Client.Advanced.Graphics.Quality.transparency.get().transparencyEnabled;
					LodQuadBuilder lodQuadBuilder = new LodQuadBuilder(enableTransparency, this.level.getClientLevelWrapper());
					
					
					// get the adjacent positions
					// needs to be done async to prevent threads waiting on the same positions to be processed
					final CompletableFuture<ColumnRenderSource>[] adjacentLoadFutures = new CompletableFuture[4];
					adjacentLoadFutures[0] = this.getRenderSourceForPosAsync(this.pos, EDhDirection.NORTH);
					adjacentLoadFutures[1] = this.getRenderSourceForPosAsync(this.pos, EDhDirection.SOUTH);
					adjacentLoadFutures[2] = this.getRenderSourceForPosAsync(this.pos, EDhDirection.EAST);
					adjacentLoadFutures[3] = this.getRenderSourceForPosAsync(this.pos, EDhDirection.WEST);
					return CompletableFuture.allOf(adjacentLoadFutures).thenRun(() ->
					{
						try (ColumnRenderSource northRenderSource = adjacentLoadFutures[0].get();
							ColumnRenderSource southRenderSource = adjacentLoadFutures[1].get();
							ColumnRenderSource eastRenderSource = adjacentLoadFutures[2].get();
							ColumnRenderSource westRenderSource = adjacentLoadFutures[3].get())
						{
							ColumnRenderSource[] adjacentRenderSections = new ColumnRenderSource[EDhDirection.CARDINAL_COMPASS.length];
							adjacentRenderSections[EDhDirection.NORTH.compassIndex] = northRenderSource;
							adjacentRenderSections[EDhDirection.SOUTH.compassIndex] = southRenderSource;
							adjacentRenderSections[EDhDirection.EAST.compassIndex] = eastRenderSource;
							adjacentRenderSections[EDhDirection.WEST.compassIndex] = westRenderSource;
							
							boolean[] adjIsSameDetailLevel = new boolean[EDhDirection.CARDINAL_COMPASS.length];
							adjIsSameDetailLevel[EDhDirection.NORTH.compassIndex] = this.isAdjacentPosSameDetailLevel(EDhDirection.NORTH);
							adjIsSameDetailLevel[EDhDirection.SOUTH.compassIndex] = this.isAdjacentPosSameDetailLevel(EDhDirection.SOUTH);
							adjIsSameDetailLevel[EDhDirection.EAST.compassIndex] = this.isAdjacentPosSameDetailLevel(EDhDirection.EAST);
							adjIsSameDetailLevel[EDhDirection.WEST.compassIndex] = this.isAdjacentPosSameDetailLevel(EDhDirection.WEST);
							
							// the render sources are only needed by this synchronous method,
							// then they can be closed
							ColumnRenderBufferBuilder.makeLodRenderData(lodQuadBuilder, thisRenderSource, this.level, adjacentRenderSections, adjIsSameDetailLevel);
							this.uploadToGpuAsync(lodQuadBuilder);
						}
						catch (Exception e)
						{
							LOGGER.error("Unexpected error while loading LodRenderSection [" + DhSectionPos.toString(this.pos) + "] adjacent data, Error: [" + e.getMessage() + "].", e);
						}
						finally
						{
							// can only be closed after the data has been processed and uploaded to the GPU
							thisRenderSource.close();
						}
					});
				}
				catch (Exception e)
				{
					LOGGER.error("Unexpected error while loading LodRenderSection ["+DhSectionPos.toString(this.pos)+"], Error: [" + e.getMessage() + "].", e);
					return CompletableFuture.completedFuture(null);
				}
			});
	}
	/** 
	 * async is done so each thread can run without waiting on others 
	 * @param direction the direction to load relative to the given position, null will return the given position
	 */
	private CompletableFuture<ColumnRenderSource> getRenderSourceForPosAsync(long pos, @Nullable EDhDirection direction) 
	{
		if (direction != null)
		{
			pos = DhSectionPos.getAdjacentPos(pos, direction);
		}
		final long finalPos = pos;
		
		
		try
		{
			PriorityTaskPicker.Executor executor = ThreadPoolUtil.getRenderLoadingExecutor();
			if (executor == null || executor.isTerminated())
			{
				// should only happen if the threadpool is actively being re-sized
				return CompletableFuture.completedFuture(null);
			}
			
			
			// queue loading the render data
			CompletableFuture<ColumnRenderSource> loadFuture = new CompletableFuture<>();
			executor.execute(() ->
			{
				// generate new render source
				try (FullDataSourceV2 fullDataSource =
						// no direction means get the center LOD		
						(direction == null)
						? this.fullDataSourceProvider.get(finalPos)
						: this.fullDataSourceProvider.getAdjForDirection(finalPos, direction.opposite()))
				{
					ColumnRenderSource columnRenderSource = FullDataToRenderDataTransformer.transformFullDataToRenderSource(fullDataSource, this.levelWrapper);
					loadFuture.complete(columnRenderSource);
				}
				catch (Exception e)
				{
					LOGGER.error("Unexpected issue creating render data for pos: ["+DhSectionPos.toString(finalPos)+"], error: ["+e.getMessage()+"].", e);
				}
			});
			
			return loadFuture;
		}
		catch (RejectedExecutionException ignore)
		{
			// the thread pool was probably shut down because it's size is being changed, just wait a sec and it should be back
			return CompletableFuture.completedFuture(null);
		}
		catch (Exception e)
		{
			LOGGER.error("Unexpected issue getting and creating render data for pos: ["+DhSectionPos.toString(pos)+"], error: ["+e.getMessage()+"].", e);
			return CompletableFuture.completedFuture(null);
		}
	}
	private boolean isAdjacentPosSameDetailLevel(EDhDirection direction)
	{
		long adjPos = DhSectionPos.getAdjacentPos(this.pos, direction);
		byte detailLevel = this.quadTree.calculateExpectedDetailLevel(new DhBlockPos2D(MC.getPlayerBlockPos()), adjPos);
		detailLevel += DhSectionPos.SECTION_MINIMUM_DETAIL_LEVEL;
		return detailLevel == DhSectionPos.getDetailLevel(this.pos);
	}
	private void uploadToGpuAsync(LodQuadBuilder lodQuadBuilder)
	{
		if (this.bufferUploadFuture != null)
		{
			// shouldn't normally happen, but just in case canceling the previous future
			// prevents the CPU from working on something that won't be used
			this.bufferUploadFuture.cancel(true);
		}
		
		this.bufferUploadFuture = ColumnRenderBufferBuilder.uploadBuffersAsync(this.level, this.pos, lodQuadBuilder);
		this.bufferUploadFuture.thenAccept((buffer) ->
		{
			// needed to clean up the old data
			LodBufferContainer previousContainer = this.bufferContainer;
			
			// upload complete
			this.bufferContainer = buffer.buffersUploaded ? buffer : null;
			this.getAndBuildRenderDataFuture = null;
			
			if (previousContainer != null)
			{
				previousContainer.close();
			}
		});
	}
	
	//endregion render data uploading
	
	
	
	//====================//
	// enabling rendering //
	//====================//
	//region enabling rendering
	
	public boolean canRender() { return this.bufferContainer != null; }
	
	public boolean getRenderingEnabled() { return this.renderingEnabled; }
	/**
	 * Separate from {@link LodRenderSection#onRenderingEnabled} and {@link LodRenderSection#onRenderingDisabled}
	 * since we need to trigger external changes in disabled -> enabled order
	 * so beacons are removed and then re-added.
	 * However, to prevent holes in the world when disabling sections we need to
	 * enable the new section(s) first before disabling the old one(s).
	 */
	public void setRenderingEnabled(boolean enabled) { this.renderingEnabled = enabled;}
	
	/** @see LodRenderSection#setRenderingEnabled */
	public void onRenderingEnabled() { this.startRenderingBeacons(); }
	/** @see LodRenderSection#setRenderingEnabled */
	public void onRenderingDisabled() 
	{
		this.stopRenderingBeacons();
		
		if (Config.Client.Advanced.Debugging.DebugWireframe.showRenderSectionStatus.get())
		{
			// show that this position has just been disabled
			DebugRenderer.makeParticle(
				new DebugRenderer.BoxParticle(
					new DebugRenderer.Box(this.pos, 128f, 156f, 0.09f, Color.CYAN.darker()),
					0.2, 32f
				)
			);
		}
	}
	
	public boolean gpuUploadInProgress() { return this.getAndBuildRenderDataFuture != null; }
	
	//endregion enabling rendering
	
	
	
	//=================//
	// beacon handling //
	//=================//
	//region beacon handling
	
	/** gets the active beacon list and stops/starts beacon rendering as necessary */
	private void getAndRefreshRenderingBeacons()
	{
		// do nothing if beacon rendering or repos are unavailable
		if (this.beaconBeamRepo == null 
			|| this.beaconRenderHandler == null)
		{
			return;
		}
		
		
		// Synchronized to prevent two threads for starting/stopping rendering at once
		// Shouldn't be necessary, but just in case.
		synchronized (this.activeBeaconList)
		{
			List<BeaconBeamDTO> activeBeacons = this.beaconBeamRepo.getAllBeamsForPos(this.pos);
			
			
			// stop rendering current beacons
			for (BeaconBeamDTO beam : this.activeBeaconList)
			{
				this.beaconRenderHandler.stopRenderingBeaconAtPos(beam.blockPos);
			}
			
			// swap old and new active beacon list
			this.activeBeaconList.clear();
			this.activeBeaconList.addAll(activeBeacons);
			
			// start rendering new beacon list
			for (BeaconBeamDTO beam : this.activeBeaconList)
			{
				this.beaconRenderHandler.startRenderingBeacon(beam);
			}
		}
	}
	
	private void stopRenderingBeacons()
	{
		// do nothing if beacon rendering is unavailable
		if (this.beaconRenderHandler == null)
		{
			return;
		}
		
		
		synchronized (this.activeBeaconList)
		{
			for (BeaconBeamDTO beam : this.activeBeaconList)
			{
				this.beaconRenderHandler.stopRenderingBeaconAtPos(beam.blockPos);
			}
		}
	}
	
	private void startRenderingBeacons()
	{
		// do nothing if beacon rendering is unavailable 
		if (this.beaconRenderHandler == null)
		{
			return;
		}
		
		
		synchronized (this.activeBeaconList)
		{
			for (BeaconBeamDTO beam : this.activeBeaconList)
			{
				this.beaconRenderHandler.startRenderingBeacon(beam);
			}
		}
	}
	
	//endregion beacon handling
	
	
	
	//==============//
	// base methods //
	//==============//
	//region base methods
	
	@Override
	public void debugRender(DebugRenderer debugRenderer)
	{
		Color color = Color.red;
		if (this.renderingEnabled)
		{
			color = Color.green;
		}
		else if (this.getAndBuildRenderDataFuture != null)
		{
			color = Color.yellow;
		}
		else if (this.canRender())
		{
			color = Color.cyan;
		}
		
		debugRenderer.renderBox(new DebugRenderer.Box(this.pos, 400, 8f, Objects.hashCode(this), 0.1f, color));
	}
	
	@Override
	public String toString()
	{
		return  "pos=[" + DhSectionPos.toString(this.pos) + "] " +
				"enabled=[" + this.renderingEnabled + "] " +
				"uploading=[" + this.gpuUploadInProgress() + "] ";
	}
	
	@Override
	public void close()
	{
		DebugRenderer.unregister(this, Config.Client.Advanced.Debugging.DebugWireframe.showRenderSectionStatus);
		
		if (Config.Client.Advanced.Debugging.DebugWireframe.showRenderSectionStatus.get())
		{
			// show a particle for the closed section
			DebugRenderer.makeParticle(
				new DebugRenderer.BoxParticle(
					new DebugRenderer.Box(this.pos, 128f, 156f, 0.09f, Color.RED.darker()),
					0.5, 32f
				)
			);
		}
		
		
		this.stopRenderingBeacons();
		
		if (this.bufferContainer != null)
		{
			this.bufferContainer.close();
		}
		
		// removes any in-progress futures since they aren't needed any more
		CompletableFuture<Void> buildFuture = this.getAndBuildRenderDataFuture;
		if (buildFuture != null)
		{
			// remove the task from our executor if present
			// note: don't cancel the task since that prevents cleanup, we just don't want it to run
			PriorityTaskPicker.Executor renderLoaderExecutor = ThreadPoolUtil.getRenderLoadingExecutor();
			if (renderLoaderExecutor != null 
				&& !renderLoaderExecutor.isTerminated())
			{
				Runnable runnable = this.getAndBuildRenderDataRunnable;
				if (runnable != null)
				{
					renderLoaderExecutor.remove(runnable);
				}
			}
		}
		
		CompletableFuture<LodBufferContainer> uploadFuture = this.bufferUploadFuture;
		if (uploadFuture != null)
		{
			uploadFuture.cancel(true);
		}
		
	}
	
	//endregion base methods
	
	
	
}
