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

package com.seibel.distanthorizons.core.render.QuadTree;

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
import com.seibel.distanthorizons.core.render.renderer.AbstractDebugWireframeRenderer;
import com.seibel.distanthorizons.core.render.renderer.BeaconRenderHandler;
import com.seibel.distanthorizons.core.render.renderer.IDebugRenderable;
import com.seibel.distanthorizons.core.dataObjects.render.bufferBuilding.LodBufferContainer;
import com.seibel.distanthorizons.core.sql.dto.BeaconBeamDTO;
import com.seibel.distanthorizons.core.sql.repo.BeaconBeamRepo;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.util.threading.PriorityTaskPicker;
import com.seibel.distanthorizons.core.util.threading.ThreadPoolUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import org.jetbrains.annotations.Nullable;

import javax.annotation.WillNotClose;
import java.awt.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A render section represents an area that could be rendered.
 * For more information see {@link LodQuadTree}.
 */
public class LodRenderSection implements IDebugRenderable, AutoCloseable
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	private static final IMinecraftClientWrapper MC = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	private static final AbstractDebugWireframeRenderer DEBUG_RENDERER = SingletonInjector.INSTANCE.get(AbstractDebugWireframeRenderer.class);
	
	
	
	public final long pos;
	
	private final IDhClientLevel level;
	private final IClientLevelWrapper levelWrapper;
	@WillNotClose
	private final FullDataSourceProviderV2 fullDataSourceProvider;
	private final LodQuadTree quadTree;
	
	/** 
	 * contains the list of beacons currently being rendered in this section 
	 * if this list is modified the {@link LodRenderSection#beaconRenderHandler} should be updated to match.
	 */
	private final ArrayList<BeaconBeamDTO> activeBeaconList = new ArrayList<>();
	@Nullable
	public final BeaconRenderHandler beaconRenderHandler;
	@Nullable
	public final BeaconBeamRepo beaconBeamRepo;
	/** 
	 * locking is necessary to prevent some weird threading issues 
	 * causing beacons to appear/disappear at the wrong times.
	 */
	private final ReentrantLock beaconRenderHandlingLock = new ReentrantLock(); 
	
	
	private boolean renderingEnabled = false;
	private boolean beaconsRendering = false;
	public boolean retreivedMissingSectionsForRetreival = false;
	
	/** this reference is necessary so we can determine what VBO to render */
	public LodBufferContainer renderBufferContainer; 
	
	
	/** 
	 * Encapsulates everything between pulling data from the database (including neighbors)
	 * up to the point when geometry data is uploaded to the GPU.
	 */
	private final AtomicReference<CompletableFuture<Void>> getAndBuildRenderDataFutureRef = new AtomicReference<>(null);
	
	/** 
	 * used alongside {@link LodRenderSection#getAndBuildRenderDataFutureRef} so we can remove
	 * unnecessary tasks from the executor.
	 */
	private Runnable getAndBuildRenderDataRunnable = null;
	
	/** 
	 * Represents just uploading the {@link LodQuadBuilder} to the GPU. <br>
	 * Separate from {@link LodRenderSection#getAndBuildRenderDataFutureRef} because they run on
	 * different threads (buffer uploading is on the MC render thread) and need to be canceled separately.
	 */
	private final AtomicReference<CompletableFuture<LodBufferContainer>> bufferUploadFutureRef = new AtomicReference<>(null);
	
	
	
	//=============//
	// constructor //
	//=============//
	//region constructor
	
	public LodRenderSection(
			long pos, 
			LodQuadTree quadTree, 
			IDhClientLevel level, FullDataSourceProviderV2 fullDataSourceProvider)
	{
		this.pos = pos;
		this.quadTree = quadTree;
		this.level = level;
		this.levelWrapper = level.getClientLevelWrapper();
		this.fullDataSourceProvider = fullDataSourceProvider;
		
		this.beaconRenderHandler = this.quadTree.beaconRenderHandler;
		this.beaconBeamRepo = this.level.getBeaconBeamRepo();
		
		DEBUG_RENDERER.register(this, Config.Client.Advanced.Debugging.DebugWireframe.showRenderSectionStatus);
	}
	
	//endregion constructor
	
	
	
	//======================================//
	// render data generation and uploading //
	//======================================//
	//region render data uploading
	
	/** @return true if the upload started, false if it wasn't able to for any reason */
	public synchronized boolean uploadRenderDataToGpuAsync()
	{
		if (this.getAndBuildRenderDataFutureRef.get() != null)
		{
			// don't accidentally queue multiple uploads at the same time
			return false;
		}
		
		PriorityTaskPicker.Executor executor = ThreadPoolUtil.getRenderLoadingExecutor();
		if (executor == null || executor.isTerminated())
		{
			return false;
		}
		
		CompletableFuture<Void> future = new CompletableFuture<>();
		future.handle((voidObj, throwable) ->
		{
			// the task is done, we don't need to track these anymore
			this.getAndBuildRenderDataFutureRef.compareAndSet(future, null);
			this.getAndBuildRenderDataRunnable = null;
			
			return null;
		});
		
		try
		{
			if (!this.getAndBuildRenderDataFutureRef.compareAndSet(null, future))
			{
				CompletableFuture<Void> oldFuture = this.getAndBuildRenderDataFutureRef.get();
				LodUtil.assertTrue(oldFuture != null, "Concurrency error");
				return true;
			}
			
			
			this.getAndBuildRenderDataRunnable = () ->
			{
				try
				{
					this.refreshActiveBeaconList();
					
					LodQuadBuilder lodQuadBuilder = this.getAndBuildRenderData();
					if (lodQuadBuilder == null)
					{
						future.complete(null);
						return;
					}
					
					this.uploadToGpuAsync(lodQuadBuilder)
						.thenRun(() ->
						{
							// the future is passed in separately (IE not using the local var) to prevent any possible race condition null pointers
							future.complete(null);
						});
				}
				catch (Exception e)
				{
					LOGGER.error("Unexpected issue creating render data for pos: ["+DhSectionPos.toString(this.pos)+"], error: ["+e.getMessage()+"].", e);
					future.complete(null);
				}
			};
			executor.execute(this.getAndBuildRenderDataRunnable);
			
			return true;
		}
		catch (RejectedExecutionException ignore)
		{
			future.complete(null);
			
			/* the thread pool was probably shut down because it's size is being changed, just wait a sec and it should be back */
			return false;
		}
	}
	@Nullable
	private synchronized LodQuadBuilder getAndBuildRenderData()
	{
		try (ColumnRenderSource thisRenderSource = this.getRenderSourceForPos(this.pos, null))
		{
			if (thisRenderSource == null)
			{
				// nothing needs to be rendered
				return null;
			}
			
			
			boolean enableTransparency = Config.Client.Advanced.Graphics.Quality.transparency.get().transparencyEnabled;
			LodQuadBuilder lodQuadBuilder = new LodQuadBuilder(enableTransparency, this.level.getClientLevelWrapper());
			
			
			// get the adjacent positions
			try (ColumnRenderSource northRenderSource = this.getRenderSourceForPos(this.pos, EDhDirection.NORTH);
				ColumnRenderSource southRenderSource = this.getRenderSourceForPos(this.pos, EDhDirection.SOUTH);
				ColumnRenderSource eastRenderSource = this.getRenderSourceForPos(this.pos, EDhDirection.EAST);
				ColumnRenderSource westRenderSource = this.getRenderSourceForPos(this.pos, EDhDirection.WEST))
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
				return lodQuadBuilder;
			}
			catch (Exception e)
			{
				LOGGER.error("Unexpected error while loading LodRenderSection [" + DhSectionPos.toString(this.pos) + "] adjacent data, Error: [" + e.getMessage() + "].", e);
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Unexpected error while loading LodRenderSection ["+DhSectionPos.toString(this.pos)+"], Error: [" + e.getMessage() + "].", e);
		}
		
		return null;
	}
	/** 
	 * async is done so each thread can run without waiting on others 
	 * @param direction the direction to load relative to the given position, null will return the given position
	 */
	private ColumnRenderSource getRenderSourceForPos(long pos, @Nullable EDhDirection direction) 
	{
		if (direction != null)
		{
			pos = DhSectionPos.getAdjacentPos(pos, direction);
		}
		final long finalPos = pos;
		
		
		try (FullDataSourceV2 fullDataSource =
			// no direction means get the center LOD		
			(direction == null)
				? this.fullDataSourceProvider.get(finalPos)
				: this.fullDataSourceProvider.getAdjForDirection(finalPos, direction.opposite()))
		{
			ColumnRenderSource columnRenderSource = FullDataToRenderDataTransformer.transformFullDataToRenderSource(fullDataSource, this.levelWrapper);
			return columnRenderSource;
		}
		catch (Exception e)
		{
			LOGGER.error("Unexpected issue creating render data for pos: ["+DhSectionPos.toString(finalPos)+"], error: ["+e.getMessage()+"].", e);
			return null;
		}
	}
	private boolean isAdjacentPosSameDetailLevel(EDhDirection direction)
	{
		long adjPos = DhSectionPos.getAdjacentPos(this.pos, direction);
		byte detailLevel = this.quadTree.calcExpectedDetailLevel(new DhBlockPos2D(MC.getPlayerBlockPos()), adjPos);
		detailLevel += DhSectionPos.SECTION_MINIMUM_DETAIL_LEVEL;
		return detailLevel == DhSectionPos.getDetailLevel(this.pos);
	}
	private synchronized CompletableFuture<LodBufferContainer> uploadToGpuAsync(LodQuadBuilder lodQuadBuilder)
	{
		CompletableFuture<LodBufferContainer> oldFuture = this.bufferUploadFutureRef.getAndSet(null);
		if (oldFuture != null)
		{
			// canceling the previous future
			// prevents the CPU from working on something that won't be used
			oldFuture.cancel(true);
		}
		
		CompletableFuture<LodBufferContainer> future = ColumnRenderBufferBuilder.uploadBuffersAsync(this.level, this.pos, lodQuadBuilder);
		future.handle((lodBufferContainer, throwable) -> 
		{
			if (!this.bufferUploadFutureRef.compareAndSet(future, null)
				// if the old future is canceled then the future ref will be different and that's expected
				&& !future.isCancelled()
				// if the old future is already done, then we don't care about the ref being swapped
				&& !future.isDone())
			{
				LOGGER.warn("Buffer upload future ref changed for pos: ["+DhSectionPos.toString(this.pos)+"].");
			}
			
			return null;
		});
		
		future.thenAccept((LodBufferContainer buffer) ->
		{
			// needed to clean up the old data
			LodBufferContainer previousContainer = this.renderBufferContainer;
			
			// upload complete
			this.renderBufferContainer = buffer.buffersUploaded ? buffer : null;
			
			if (previousContainer != null)
			{
				previousContainer.close();
			}
		});
		
		
		if (!this.bufferUploadFutureRef.compareAndSet(null, future))
		{
			LodUtil.assertNotReach("Buffer upload future ref couldn't be set due to concurrency error, pos: ["+DhSectionPos.toString(this.pos)+"].");
		}
		
		return future;
	}
	
	//endregion render data uploading
	
	
	
	//=================//
	// rendering state //
	//=================//
	//region
	
	public boolean gpuUploadComplete() { return this.renderBufferContainer != null; }
	
	public boolean getRenderingEnabled() { return this.renderingEnabled; }
	public void setRenderingEnabled(boolean enabled) { this.renderingEnabled = enabled;}
	
	public boolean gpuUploadInProgress() { return this.getAndBuildRenderDataFutureRef.get() != null; }
	
	//endregion
	
	
	
	//=================//
	// beacon handling //
	//=================//
	//region beacon handling
	
	/** gets the active beacon list and stops/starts beacon rendering as necessary */
	private void refreshActiveBeaconList()
	{
		try
		{
			this.beaconRenderHandlingLock.lock();
			
			// do nothing if beacon rendering or repos are unavailable
			if (this.beaconBeamRepo == null
				|| this.beaconRenderHandler == null)
			{
				return;
			}
			
			
			
			// Synchronized to prevent two threads for accessing the array at once
			synchronized (this.activeBeaconList)
			{
				ArrayList<BeaconBeamDTO> activeBeacons = this.beaconBeamRepo.getAllBeamsForPos(this.pos);
				
				// swap old and new active beacon list
				this.activeBeaconList.clear();
				this.activeBeaconList.addAll(activeBeacons);
				
				// if the beacons are currently rendering, 
				// re-create them so we can see any potential changes
				if (this.beaconsRendering)
				{
					this.tryDisableBeacons();
					this.tryEnableBeacons();
				}
			}
		}
		finally
		{
			this.beaconRenderHandlingLock.unlock();
		}
	}
	
	public void tryDisableBeacons()
	{
		try
		{
			this.beaconRenderHandlingLock.lock();
			
			
			// do nothing if beacon rendering is unavailable
			if (this.beaconRenderHandler == null)
			{
				return;
			}
			
			if (!this.beaconsRendering)
			{
				return;
			}
			this.beaconsRendering = false;
			
			
			
			if (Config.Client.Advanced.Debugging.DebugWireframe.showRenderSectionStatus.get())
			{
				// show that this position has just been disabled
				DEBUG_RENDERER.makeParticle(
					new AbstractDebugWireframeRenderer.BoxParticle(
						new AbstractDebugWireframeRenderer.Box(this.pos, 128f, 156f, 0.09f, Color.CYAN.darker()),
						0.2, 32f
					)
				);
			}
			
			synchronized (this.activeBeaconList)
			{
				this.beaconRenderHandler.stopRenderingBeaconsInRange(this.pos);
			}
		}
		finally
		{
			this.beaconRenderHandlingLock.unlock();
		}
	}
	
	public void tryEnableBeacons()
	{
		try
		{
			this.beaconRenderHandlingLock.lock();
			
			
			// do nothing if beacon rendering is unavailable 
			if (this.beaconRenderHandler == null)
			{
				return;
			}
			
			if (this.beaconsRendering)
			{
				return;
			}
			this.beaconsRendering = true;
			
			
			synchronized (this.activeBeaconList)
			{
				byte absoluteDetailLevel = (byte)(DhSectionPos.getDetailLevel(this.pos) - DhSectionPos.SECTION_MINIMUM_DETAIL_LEVEL);
				this.beaconRenderHandler.startRenderingBeacons(this.activeBeaconList, absoluteDetailLevel);
			}
		}
		finally
		{
			this.beaconRenderHandlingLock.unlock();
		}
	}
	
	//endregion beacon handling
	
	
	
	//==============//
	// base methods //
	//==============//
	//region base methods
	
	@Override
	public void debugRender(AbstractDebugWireframeRenderer debugRenderer)
	{
		Color color = Color.red;
		if (this.renderingEnabled)
		{
			//color = Color.green;
			return;
		}
		else if (this.getAndBuildRenderDataFutureRef.get() != null)
		{
			color = Color.yellow;
		}
		else if (this.gpuUploadComplete())
		{
			//color = Color.cyan;
			return;
		}
		
		int levelMinY = this.level.getLevelWrapper().getMinHeight();
		int levelMaxY = this.level.getLevelWrapper().getMaxHeight();
		
		// show the wireframe a bit lower than world max height,
		// since most worlds don't render all the way up to the max height
		int levelHeightRange = (levelMaxY - levelMinY);
		int maxY = levelMaxY - (levelHeightRange / 2);
		
		debugRenderer.renderBox(new AbstractDebugWireframeRenderer.Box(this.pos, levelMinY, maxY, 0.01f, color));
	}
	
	@Override
	public String toString()
	{
		return  "pos=[" + DhSectionPos.toString(this.pos) + "] " +
				"enabled=[" + this.renderingEnabled + "] " +
				"canRender=[" + (this.renderBufferContainer != null) + "] " +	
				"uploading=[" + this.gpuUploadInProgress() + "] "
				;
	}
	
	@Override
	public void close()
	{
		DEBUG_RENDERER.unregister(this, Config.Client.Advanced.Debugging.DebugWireframe.showRenderSectionStatus);
		
		if (Config.Client.Advanced.Debugging.DebugWireframe.showRenderSectionStatus.get())
		{
			// show a particle for the closed section
			DEBUG_RENDERER.makeParticle(
				new AbstractDebugWireframeRenderer.BoxParticle(
					new AbstractDebugWireframeRenderer.Box(this.pos, 128f, 156f, 0.09f, Color.RED.darker()),
					0.5, 32f
				)
			);
		}
		
		
		this.tryDisableBeacons();
		
		if (this.renderBufferContainer != null)
		{
			this.renderBufferContainer.close();
		}
		
		// removes any in-progress futures since they aren't needed any more
		CompletableFuture<Void> buildFuture = this.getAndBuildRenderDataFutureRef.get();
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
		
		CompletableFuture<LodBufferContainer> uploadFuture = this.bufferUploadFutureRef.get();
		if (uploadFuture != null)
		{
			uploadFuture.cancel(true);
		}
		
	}
	
	//endregion base methods
	
	
	
}
