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

package com.seibel.distanthorizons.core.util;

import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.distanthorizons.coreapi.util.MathUtil;
import com.seibel.distanthorizons.core.util.math.Mat4f;

/**
 * This holds miscellaneous helper code
 * used in the rendering process.
 */
public class RenderUtil
{
	private static final IMinecraftClientWrapper MC = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class);
	
	/** 
	 * all speeds are measured in blocks per second 
	 * 
	 * @see LodUtil#WALKING_SPEED_IN_BLOCKS_PER_SEC
	 * @see LodUtil#SPRINTING_SPEED_IN_BLOCKS_PER_SEC
	 * @see LodUtil#ROCKET_ELYTRA_SPEED_IN_BLOCKS_PER_SEC
	 * @see LodUtil#MAX_SPECTATOR_SPEED_IN_BLOCKS_PER_SEC
	 */
	private static class DynamicOverdraw
	{
		public static final float MIN_SPEED = 10.0f; // a little faster than sprinting (7)
		public static final float MAX_SPEED = (float)LodUtil.MAX_SPECTATOR_SPEED_IN_BLOCKS_PER_SEC;
		public static final float MIN_OVERDRAW_RATIO = 0.2f;
	}
	
	
	
	//=====================//
	// matrix manipulation //
	//=====================//
	//region
	
	/**
	 * create and return a new projection matrix based on MC's modelView and projection matrices
	 *
	 * @param mcProjMat Minecraft's current projection matrix
	 */
	public static Mat4f createLodProjectionMatrix(Mat4f mcProjMat)
	{
		// in James' testing a near clip plane distance of 2 blocks is enough to allow the fragment
		// culling to take effect instead of seeing the near clip plane.
		float nearClipDist = RenderUtil.getNearClipPlaneInBlocks();
		// limit the near clip plane if we are close to the ground
		if (getHeightBasedNearClipOverrideBlockDistance() == -1)
		{
			// min() used to prevent the near clip plane from becoming visible at large vanilla render distances
			// DH's dithering/discard shader handles everything farther away anyway so the near clip plane
			// would just need to be far enough to prevent depth precision errors
			nearClipDist = Math.min(nearClipDist, 7.5f);
		}
		
		float farClipDist = (float) RenderUtil.getFarClipPlaneDistanceInBlocks();
		
		// Create a copy of the current matrix, so it won't be modified.
		Mat4f lodProj = mcProjMat.copy();
		// Set new far and near clip plane values.
		lodProj.setClipPlanes(nearClipDist, farClipDist);
		return lodProj;
	}
	
	/** create and return a new projection matrix based on MC's modelView and projection matrices */
	public static Mat4f createLodModelViewMatrix(Mat4f mcModelViewMat)
	{
		// nothing beyond copying needs to be done to MC's MVM currently,
		// this method is just here in case that changes in the future
		return mcModelViewMat.copy();
	}
	
	//endregion
	
	
	
	//=================//
	// near clip plane //
	//=================//
	//region
	
	public static float getNearClipPlaneInBlocks()
	{
		float overdraw = Config.Client.Advanced.Graphics.Culling.overdrawPrevention.get();
		if (overdraw < 0)
		{
			// automatic mode,
			// get overdraw based on vanilla render distance.
			// At low render distances this hides the vanilla RD border
			
			int chunkRenderDistance = MC_RENDER.getRenderDistance();
			if (chunkRenderDistance <= 2)
			{
				overdraw = 0.2f;
			}
			else if (chunkRenderDistance <= 4)
			{
				overdraw = 0.3f;
			}
			else if (chunkRenderDistance <= 6)
			{
				overdraw = 0.6f;
			}
			else if (chunkRenderDistance <= 10)
			{
				overdraw = 0.8f;
			}
			else
			{
				overdraw = 0.9f;
			}
		}
		else
		{
			// prevent setting an overdraw of 0
			// since that will cause rendering issues
			overdraw = MathUtil.clamp(0.05f, overdraw, 1.0f);
		}
		
		
		if (Config.Client.Advanced.Graphics.Culling.reduceOverdrawWithFastMovement.get())
		{
			double avgSpeed = ClientApi.INSTANCE.cameraSpeedRollingAverage.getAverage();
			if (avgSpeed >= DynamicOverdraw.MIN_SPEED)
			{
				// if the player is moving fast enough,
				// smoothly decrease the fade distance
				// to give MC have a chance to load/generate.
				
				// convert the speed into a range of 0.0 - 1.0
				float speedRange = (float)((DynamicOverdraw.MAX_SPEED - avgSpeed) / DynamicOverdraw.MAX_SPEED);
				// if math.max isn't done here we could completely
				// remove vanilla rendering at high speeds
				speedRange = Math.max(speedRange, DynamicOverdraw.MIN_OVERDRAW_RATIO);
				
				overdraw *= speedRange;
			}
		}
		
		return getNearClipPlaneDistanceInBlocks(overdraw);
	}
	private static float getNearClipPlaneDistanceInBlocks(float overdrawPreventionPercent)
	{
		int chunkRenderDistance = MC_RENDER.getRenderDistance();
		int vanillaBlockRenderedDistance = chunkRenderDistance * LodUtil.CHUNK_WIDTH;
		
		float nearClipPlane;
		if (Config.Client.Advanced.Debugging.lodOnlyMode.get())
		{
			nearClipPlane = 0.1f;
		}
		else
		{
			nearClipPlane = vanillaBlockRenderedDistance;
			nearClipPlane *= overdrawPreventionPercent; 
			
			// the near clip plane should never be closer than 1/10th of a block,
			// otherwise Z-fighting and other issues may occur
			if (nearClipPlane < 0.1f)
			{
				nearClipPlane = 0.1f;
			}
		}
		
		
		float heightOverride = getHeightBasedNearClipOverrideBlockDistance();
		if (heightOverride != -1.0f)
		{
			nearClipPlane = heightOverride;
		}
		
		
		// the player's FOV setting doesn't affect vanilla's render distance,
		// which can cause issues for certain zoom mods.
		// So the FOV setting should not affect DH's near clip plane;
		// therefore, the FOV is left at a fixed value of 70 (MC's default)
		double fov = 70;
		
		double aspectRatio = (double) MC_RENDER.getTargetFramebufferViewportWidth() / MC_RENDER.getTargetFramebufferViewportHeight();
		
		// source: https://stackoverflow.com/questions/8101119/how-do-i-methodically-choose-the-near-clip-plane-distance-for-a-perspective-proj/8101234#8101234
		return (float) (nearClipPlane
				/ Math.sqrt(1d + MathUtil.pow2(Math.tan(fov / 180d * Math.PI / 2d))
				* (MathUtil.pow2(aspectRatio) + 1d)));
	}
	
	/** 
	 * Returns a new distance if the player is sufficiently far above the world.
	 * @return -1 if no override is necessary 
	 */
	public static float getHeightBasedNearClipOverrideBlockDistance()
	{
		// always using the client level like this might cause issues with immersive portals and the like,
		// but for now it works well enough
		IClientLevelWrapper level = MC.getWrappedClientLevel();
		// a level should always be loaded, but just in case
		if (level != null)
		{
			// if the player is a significant distance above the work, increase the
			// near clip plane to fix Z imprecision issues
			int playerHeight = MC.getPlayerBlockPos().getY();
			int levelMaxHeight = level.getMaxHeight();
			if (playerHeight > levelMaxHeight + 1_000)
			{
				return playerHeight - (levelMaxHeight + 1000);
			}
		}
		
		return -1.0f;
	}
	
	//endregion
	
	
	
	//================//
	// far clip plane //
	//================//
	
	//region
	
	public static int getFarClipPlaneDistanceInBlocks()
	{
		int lodChunkDist = Config.Client.Advanced.Graphics.Quality.lodChunkRenderDistanceRadius.get();
		int lodBlockDist = lodChunkDist * LodUtil.CHUNK_WIDTH;
		// * 2 to prevent clipping when high above the world
		return (lodBlockDist + LodUtil.REGION_WIDTH) * 2;
	}
	
	//endregion
	
	
	
}
