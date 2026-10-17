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

package com.seibel.distanthorizons.core.render.renderer;

import com.seibel.distanthorizons.api.enums.rendering.EDhApiBlockMaterial;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiCustomRenderObjectFactory;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiCustomRenderRegister;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiRenderableBoxGroup;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.distanthorizons.api.objects.math.DhApiVec3d;
import com.seibel.distanthorizons.api.objects.render.DhApiRenderableBox;
import com.seibel.distanthorizons.api.objects.render.DhApiRenderableBoxGroupShading;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos;
import com.seibel.distanthorizons.core.render.RenderParam;
import com.seibel.distanthorizons.core.sql.dto.BeaconBeamDTO;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.util.RenderUtil;
import com.seibel.distanthorizons.core.util.math.DhVec3d;
import com.seibel.distanthorizons.core.util.threading.ThreadPoolUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.distanthorizons.coreapi.ModInfo;
import com.seibel.distanthorizons.core.logging.DhLogger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.ReentrantLock;

public class BeaconRenderHandler
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class);
	private static final IDhApiCustomRenderObjectFactory GENERIC_OBJECT_FACTORY = SingletonInjector.INSTANCE.get(IDhApiCustomRenderObjectFactory.class);
	
	/** how often should we check if a beacon should be culled? */
	private static final int MAX_CULLING_FREQUENCY_IN_MS = 1_000;
	
	/** the origin is snapped to this grid so it doesn't change every frame */
	private static final double ORIGIN_SNAP_GRID = 1024;
	/** if the camera gets this far from the origin the boxes are rebuilt around a new origin */
	private static final double ORIGIN_MAX_DRIFT = 2048;
	
	
	
	/** the origin the box group is currently relative to, guarded by {@link #updateLock} */
	private final DhApiVec3d currentOrigin = new DhApiVec3d(0, 0, 0);
	/** double buffered origin so we don't have to worry about locking the render thread */
	private final DhApiVec3d altOrigin = new DhApiVec3d(0, 0, 0);
	
	private final ReentrantLock updateLock = new ReentrantLock();
	
	/** only contains the beacons currently being rendered (culled beacons will be missing) */
	private final IDhApiRenderableBoxGroup activeBeaconBoxRenderGroup;
	/** contains all beacons that could be rendered (including those that are being culled) */
	private final ArrayList<BeaconBeamWithWidth> fullBeaconList = new ArrayList<>();
	
	private boolean cullingThreadRunning = false;
	private volatile boolean updateRenderDataNextFrame = false;
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	public BeaconRenderHandler(@NotNull IDhApiCustomRenderRegister renderer)
	{
		this.activeBeaconBoxRenderGroup = GENERIC_OBJECT_FACTORY.createAbsolutePositionedGroup(ModInfo.NAME+":Beacons", new ArrayList<>(0));
		this.activeBeaconBoxRenderGroup.setBlockLight(LodUtil.MAX_MC_LIGHT);
		this.activeBeaconBoxRenderGroup.setSkyLight(LodUtil.MAX_MC_LIGHT);
		this.activeBeaconBoxRenderGroup.setSsaoEnabled(false);
		this.activeBeaconBoxRenderGroup.setShading(DhApiRenderableBoxGroupShading.getUnshaded());
		this.activeBeaconBoxRenderGroup.setPreRenderFunc(this::beforeRender);
		
		renderer.add(this.activeBeaconBoxRenderGroup);
	}
	
	//endregion
	
	
	
	//===============//
	// before render //
	//===============//
	//region
	
	private void beforeRender(DhApiRenderParam apiRenderEventParam)
	{
		RenderParam renderParams = (RenderParam) apiRenderEventParam;
		
		if (Config.Client.Advanced.Graphics.Culling.disableBeaconDistanceCulling.get())
		{
			this.tryUpdateBeaconCullingAsync(renderParams.exactCameraPosition);
		}
		
		// re-center the origin if the camera has wandered too far from it
		double camX = renderParams.exactCameraPosition.x;
		double camZ = renderParams.exactCameraPosition.z;
		double driftX = camX - this.currentOrigin.x;
		double driftZ = camZ - this.currentOrigin.z;
		double driftDistance = Math.sqrt((driftX * driftX) + (driftZ * driftZ));
		if (driftDistance > ORIGIN_MAX_DRIFT)
		{
			this.repopulateRenderGroup(camX, camZ, false);
		}
		
		// origin and box data must change on the same frame
		if (this.updateRenderDataNextFrame)
		{
			// copy over the new active origin
			this.currentOrigin.set(this.altOrigin);
			
			this.activeBeaconBoxRenderGroup.setOriginBlockPos(this.currentOrigin);
			this.activeBeaconBoxRenderGroup.triggerBoxChange();
			this.updateRenderDataNextFrame = false;
		}
		
		this.activeBeaconBoxRenderGroup.setActive(Config.Client.Advanced.Graphics.GenericRendering.enableBeaconRendering.get());
	}
	
	/** does nothing if the culling thread is already running */
	private void tryUpdateBeaconCullingAsync(DhVec3d exactCameraPosition)
	{
		ThreadPoolExecutor executor = ThreadPoolUtil.getBeaconCullingExecutor();
		if (executor != null
			&& !this.cullingThreadRunning)
		{
			this.cullingThreadRunning = true;
			
			try
			{
				executor.execute(() ->
				{
					try
					{
						Thread.sleep(MAX_CULLING_FREQUENCY_IN_MS);
					}
					catch (InterruptedException ignore) { }
					
					try
					{
						// lock to make sure we don't try adding beacons to the arrays while processing them
						this.updateLock.lock();
						
						this.repopulateRenderGroup(exactCameraPosition.x, exactCameraPosition.z, true);
						
						this.updateRenderDataNextFrame = true;
					}
					catch (Exception e)
					{
						LOGGER.error("Unexpected issue while updating beacon culling. Error: " + e.getMessage(), e);
					}
					finally
					{
						this.updateLock.unlock();
						this.cullingThreadRunning = false;
					}
				});
			}
			catch (RejectedExecutionException ignore)
			{ /* If this happens that means everything is already shut down and no culling is necessary */ }
		}
	}
	
	/** Rebuilds the render group's boxes relative to the new origin. */
	private void repopulateRenderGroup(double camX, double camZ, boolean applyCulling)
	{
		// snapping is used so the origin stays stable while the camera moves around
		double originX = Math.floor(camX / ORIGIN_SNAP_GRID) * ORIGIN_SNAP_GRID;
		double originZ = Math.floor(camZ / ORIGIN_SNAP_GRID) * ORIGIN_SNAP_GRID;
		this.altOrigin.set(originX, 0, originZ); // the alt origin is used to make sure the renderable box positions and their origin are in sync without locking the render thread
		
		// fading by the overdraw prevention amount helps reduce beacons from rendering strangely
		// on the border of DH's render distance
		float dhFadeDistance = RenderUtil.getNearClipPlaneInBlocks();
		
		int beaconBeamHeight = Config.Client.Advanced.Graphics.GenericRendering.beaconRenderHeight.get();
		
		// Clear the existing box group so we can re-populate it.
		// Since the box group is only used when we trigger an update, clearing it here
		// and repopulating it is fine.
		this.activeBeaconBoxRenderGroup.clear();
		
		// While iterating over every beacon isn't a great way of doing this, 
		// when 940 beacons were tested this only took ~0.9 Milliseconds, so as long as
		// we aren't freezing the render thread this method of culling works just fine.
		for (BeaconBeamWithWidth beacon : this.fullBeaconList)
		{
			int blockX = beacon.blockPos.getX();
			int blockY = beacon.blockPos.getY();
			int blockZ = beacon.blockPos.getZ();
			
			// if a beacon is inside the vanilla render distance, skip it
			if (applyCulling)
			{
				double diffX = camX - blockX;
				double diffZ = camZ - blockZ;
				double distanceFromCamera = Math.sqrt((diffX * diffX) + (diffZ * diffZ));
				if (distanceFromCamera <= dhFadeDistance)
				{
					continue;
				}
			}
			
			// subtract in double BEFORE the values ever become floats
			this.activeBeaconBoxRenderGroup.add(new DhApiRenderableBox(
				new DhApiVec3d(
					blockX - originX, 
					blockY + 1, // +1 since a beacon will always start above the beacon block it originated from
					blockZ - originZ),
				new DhApiVec3d(
					blockX + beacon.beaconBlockWidth - originX,
					beaconBeamHeight, 
					blockZ + beacon.beaconBlockWidth - originZ),
				beacon.color,
				EDhApiBlockMaterial.ILLUMINATED
			));
		}
		
		this.updateRenderDataNextFrame = true;
	}
	
	//endregion
	
	
	
	//==============//
	// registration //
	//==============//
	//region
	
	public void replaceRenderingBeacons(ArrayList<BeaconBeamWithWidth> beaconList)
	{
		try
		{
			this.updateLock.lock();
			
			ArrayList<BeaconBeamWithWidth> sortedBeaconList = new ArrayList<>(beaconList);
			
			// merge distant beams if requested
			if (Config.Client.Advanced.Graphics.GenericRendering.expandDistantBeacons.get())
			{
				// sort beacons from neg inf -> pos inf
				// so we can consistently merge adjacent beacons
				sortedBeaconList.sort(NegativeInfiniteBlockPosComparator.INSTANCE);
				
				// go through each beacon...
				for (int outerIndex = 0; outerIndex < sortedBeaconList.size(); outerIndex++)
				{
					BeaconBeamWithWidth outerBeacon = sortedBeaconList.get(outerIndex);
					DhBlockPos outerBlockPos = outerBeacon.blockPos;
					
					// ...and remove any beacons that are within the block width to prevent overlaps
					for (int mergeIndex = outerIndex + 1; mergeIndex < sortedBeaconList.size(); mergeIndex++)
					{
						BeaconBeamWithWidth beaconToMerge = sortedBeaconList.get(mergeIndex);
						DhBlockPos mergeBlockPos = beaconToMerge.blockPos;
						
						int xDiff = mergeBlockPos.getX() - outerBlockPos.getX();
						int zDiff = mergeBlockPos.getZ() - outerBlockPos.getZ();
						
						// merge (remove) this beacon if
						// it's close to the outer beacon
						if (xDiff < beaconToMerge.beaconBlockWidth
							&& zDiff < beaconToMerge.beaconBlockWidth)
						{
							sortedBeaconList.remove(mergeIndex);
							mergeIndex--; // minus 1 so we don't go past the end of the array when incrementing in the for loop up top
						}
					}
				}
			}
			
			
			this.activeBeaconBoxRenderGroup.clear();
			
			this.fullBeaconList.clear();
			this.fullBeaconList.addAll(sortedBeaconList);
			
			DhVec3d cameraPos = MC_RENDER.getCameraExactPosition();
			this.repopulateRenderGroup(cameraPos.x, cameraPos.z, false);
		}
		finally
		{
			this.updateLock.unlock();
		}
	}
	
	//endregion
	
	
	
	//================//
 	// helper classes //
 	//================//
	//region
	
	public static class BeaconBeamWithWidth extends BeaconBeamDTO
	{
		public final int beaconBlockWidth;
		
		public BeaconBeamWithWidth(BeaconBeamDTO beaconBeamDTO, byte lodDetailLevel)
		{
			super(beaconBeamDTO.blockPos, beaconBeamDTO.color);
		
			
			// how wide should each beacon be?
			if (Config.Client.Advanced.Graphics.GenericRendering.expandDistantBeacons.get())
			{
				this.beaconBlockWidth = DhSectionPos.getBlockWidth(lodDetailLevel);
			}
			else
			{
				this.beaconBlockWidth = 1;
			}
		}
		
		@Override 
		public boolean equals(Object obj)
		{
			if (obj == null 
				|| obj.getClass() != this.getClass())
			{
				return false;
			}
			
			BeaconBeamWithWidth that = (BeaconBeamWithWidth) obj;
			if (that.beaconBlockWidth != this.beaconBlockWidth)
			{
				return false;
			}
			
			return super.equals(that);
		}
		
	}
	
	public static class NegativeInfiniteBlockPosComparator implements Comparator<BeaconBeamWithWidth>
	{
		public static final NegativeInfiniteBlockPosComparator INSTANCE = new NegativeInfiniteBlockPosComparator();
		
		@Override
		public int compare(BeaconBeamWithWidth beacon1, BeaconBeamWithWidth beacon2)
		{
			DhBlockPos blockPos1 = beacon1.blockPos;
			DhBlockPos blockPos2 = beacon2.blockPos;
			
			// sort by X, then by Z
			if (blockPos1.getX() != blockPos2.getX())
			{
				return Integer.compare(blockPos1.getX(), blockPos2.getX());
			}
			return Integer.compare(blockPos1.getZ(), blockPos2.getZ());
		}
	}
	
	//endregion
	
	
	
}
