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

package com.seibel.distanthorizons.core.util.threading;

import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.util.ThreadUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.*;

/**
 * Holds each thread pool the system uses.
 * 
 * @see ThreadUtil
 */
public class ThreadPoolUtil
{
	//=========================//
	// standalone thread pools //
	//=========================//
	
	// standalone thread pools all handle independent systems
	// and don't interfere with any other pool
	
	private static PriorityTaskPicker taskPicker;
	
	private static PriorityTaskPicker.Executor fileHandlerThreadPool;
	@Nullable
	public static PriorityTaskPicker.Executor getFileHandlerExecutor() { return fileHandlerThreadPool; }
	
	private static PriorityTaskPicker.Executor renderSectionLoadThreadPool;
	@Nullable
	public static PriorityTaskPicker.Executor getRenderLoadingExecutor() { return renderSectionLoadThreadPool; }
	
	private static PriorityTaskPicker.Executor updatePropagatorThreadPool;
	@Nullable
	public static PriorityTaskPicker.Executor getUpdatePropagatorExecutor() { return updatePropagatorThreadPool; }
	
	private static PriorityTaskPicker.Executor worldGenThreadPool;
	@Nullable
	public static PriorityTaskPicker.Executor getWorldGenExecutor() { return worldGenThreadPool; }
	
	public static final String CLEANUP_THREAD_NAME = "Cleanup";
	private static final ThreadPoolExecutor cleanupThreadPool = ThreadUtil.makeSingleThreadPool(CLEANUP_THREAD_NAME);
	/** not null since cleanup always needs to be run even when DH has been shut down */
	@NotNull
	public static ThreadPoolExecutor getCleanupExecutor() { return cleanupThreadPool; }
	
	public static final String BEACON_CULLING_THREAD_NAME = "Beacon Culling";
	private static ThreadPoolExecutor beaconCullingThreadPool;
	@Nullable
	public static ThreadPoolExecutor getBeaconCullingExecutor() { return beaconCullingThreadPool; }
	
	// The main distinction between these thread pools is that one for compression has multiple threads and client handler is single-threaded
	private static PriorityTaskPicker.Executor networkCompressionThreadPool;
	@Nullable
	public static PriorityTaskPicker.Executor getNetworkCompressionExecutor() { return networkCompressionThreadPool; }
	
	private static ThreadPoolExecutor networkClientHandlerThreadPool;
	@Nullable
	public static ThreadPoolExecutor networkClientHandlerExecutor() { return networkClientHandlerThreadPool; }
	
	
	public static final String FULL_DATA_MIGRATION_THREAD_NAME = "Full Data Migration";
	private static ThreadPoolExecutor fullDataMigrationThreadPool;
	@Nullable
	public static ThreadPoolExecutor getFullDataMigrationExecutor() { return fullDataMigrationThreadPool; }
	
	
	private static PriorityTaskPicker.Executor chunkToLodBuilderThreadPool;
	@Nullable
	public static PriorityTaskPicker.Executor getChunkToLodBuilderExecutor() { return chunkToLodBuilderThreadPool; }
	
	
	
	//=================//
	// setup / cleanup //
	//=================//
	///region
	
	public static void setupThreadPools()
	{
		//==================//
		// main thread pool //
		//==================//
		
		if (taskPicker != null)
		{
			taskPicker.shutdownNow();
		}
		taskPicker = new PriorityTaskPicker();
		
		networkCompressionThreadPool = taskPicker.createExecutor("Network Compression");
		networkClientHandlerThreadPool = ThreadUtil.makeSingleThreadPool("Network Client Handler");
		fileHandlerThreadPool = taskPicker.createExecutor("IO");
		renderSectionLoadThreadPool = taskPicker.createExecutor("Render Loader");
		chunkToLodBuilderThreadPool = taskPicker.createExecutor("LOD Builder");
		updatePropagatorThreadPool = taskPicker.createExecutor("Update Propagator", ThreadPoolUtil::worldGenThreadsCanRun); // the update propagator isn't necessary when moving through the world, so we'll pause it along with the world generator when moving fast
		worldGenThreadPool = taskPicker.createExecutor("World Gen", ThreadPoolUtil::worldGenThreadsCanRun);
		
		
		
		
		//=========================//
		// standalone thread pools //
		//=========================//
		
		if (beaconCullingThreadPool != null)
		{
			beaconCullingThreadPool.shutdown();
		}
		beaconCullingThreadPool = ThreadUtil.makeSingleThreadPool(BEACON_CULLING_THREAD_NAME);
		
		if (fullDataMigrationThreadPool != null)
		{
			fullDataMigrationThreadPool.shutdown();
		}
		fullDataMigrationThreadPool = ThreadUtil.makeSingleThreadPool(FULL_DATA_MIGRATION_THREAD_NAME);
		
	}
	
	public static void shutdownThreadPools()
	{
		// standalone threads
		networkClientHandlerThreadPool.shutdownNow();
		taskPicker.shutdownNow();
		beaconCullingThreadPool.shutdown();
		fullDataMigrationThreadPool.shutdown();
	}
	
	///endregion
	
	
	
	//================//
	// helper methods //
	//================//
	///region
	
	/**
	 * Some thread pools are very heavy (IE world gen)
	 * making LOD load times slower. Pausing those
	 * threads when the player is moving can provide a better experience. <br><br>
	 * 
	 * all speeds are measured in blocks per second 
	 * 
	 * @see LodUtil#WALKING_SPEED_IN_BLOCKS_PER_SEC
	 * @see LodUtil#SPRINTING_SPEED_IN_BLOCKS_PER_SEC
	 * @see LodUtil#ROCKET_ELYTRA_SPEED_IN_BLOCKS_PER_SEC
	 * @see LodUtil#MAX_SPECTATOR_SPEED_IN_BLOCKS_PER_SEC
	 */
	public static boolean worldGenThreadsCanRun()
	{
		double cameraSpeed = ClientApi.INSTANCE.cameraSpeedRollingAverage.getAverage();
		// stop these threads if moving a little bit slower than max elytra speed
		double maxAllowedSpeed = (LodUtil.ROCKET_ELYTRA_SPEED_IN_BLOCKS_PER_SEC - 10.0);
		if (cameraSpeed > maxAllowedSpeed)
		{
			// pause if the user is moving too fast
			return false;
		}
		
		PriorityTaskPicker.Executor executor = getRenderLoadingExecutor();
		if (executor != null
			&& executor.getQueueSize() > 0)
		{
			// pause if LODs are being loaded for rendering
			return false;
		}
		
		return true;
	}
	
	///endregion
	
	
	
}
