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
import com.seibel.distanthorizons.api.interfaces.render.IDhApiRenderableBoxGroup;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.distanthorizons.api.objects.math.DhApiVec3d;
import com.seibel.distanthorizons.api.objects.render.DhApiRenderableBox;
import com.seibel.distanthorizons.api.objects.render.DhApiRenderableBoxGroupShading;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.level.IDhClientLevel;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.util.math.Vec3d;
import com.seibel.distanthorizons.core.util.math.Vec3f;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.render.renderPass.IDhGenericRenderer;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.distanthorizons.coreapi.ModInfo;
import com.seibel.distanthorizons.core.logging.DhLogger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class CloudRenderHandler
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class);
	private static final IDhApiCustomRenderObjectFactory GENERIC_OBJECT_FACTORY = SingletonInjector.INSTANCE.get(IDhApiCustomRenderObjectFactory.class);
	
	private static final String CLOUD_RESOURCE_TEXTURE_PATH = "assets/distanthorizons/textures/clouds.png";
	
	private static final boolean DEBUG_BORDER_COLORS = false;
	
	/** 
	 * How wide an individual box is. <br>
	 * Measured in blocks.
	 */
	private static final int CLOUD_BOX_WIDTH = 128;
	/** measured in blocks */
	private static final int CLOUD_BOX_THICKNESS = 32;
	
	/** 
	 * How many cloud groups wide can we render at maximum? <br> 
	 * 1 = 3x3 or 9 total <br>
	 * 2 = 5x5 or 25 total <br> <br>
	 * 
	 * 5 seems like a good count since it can cover up to around 2048 render distance.
	 */
	private static final int CLOUD_INSTANCE_RADIUS_COUNT = 5;
	
	private static final float MOVE_SPEED_IN_BLOCKS_PER_SECOND = 6.0f;
	
	
	private final IDhApiRenderableBoxGroup[][] boxGroupByOffset
			// radius * 2 to get the diameter
			// + 1 so we get an odd number wide (needed so we can have a center position)
			= new IDhApiRenderableBoxGroup[(CLOUD_INSTANCE_RADIUS_COUNT * 2) + 1][(CLOUD_INSTANCE_RADIUS_COUNT * 2) + 1];
	
	private final IDhClientLevel level;
	private final IDhGenericRenderer renderer;
	
	/** cached array so we don't need to re-create it each frame for each cloud group */
	private final Vec3d[] cullingCorners = new Vec3d[]
		{
			// the values of each will be overwritten during the culling pass
			new Vec3d(),
			new Vec3d(),
			new Vec3d(),
			new Vec3d(),
		};
	
	
	private boolean disabledWarningLogged = false;
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	public CloudRenderHandler(IDhClientLevel level, IDhGenericRenderer renderer) 
	{
		this.level = level;
		this.renderer = renderer;
		
		
		
		//=======================//
		// get the cloud texture //
		//=======================//
		//region
		
		// default to a single empty slot in case the texture is broken
		boolean[][] cloudLocations = new boolean[1][1];
		try
		{
			cloudLocations = getCloudsFromTexture();
		}
		catch (FileNotFoundException e)
		{
			LOGGER.error(e.getMessage(), e);
		}
		catch (IOException e)
		{
			LOGGER.error("Unexpected issue getting cloud texture, error: ["+e.getMessage()+"].", e);
		}
		
		if (cloudLocations.length != 0 &&
			cloudLocations.length != cloudLocations[0].length)
		{
			LOGGER.warn("Non-square cloud texture found, some parts of the texture will be clipped off.");
		}
		
		//endregion
		
		
		
		//===================//
		// parse the texture //
		//===================//
		//region
		
		int textureWidth = cloudLocations.length; 
		ArrayList<DhApiRenderableBox> boxList = new ArrayList<>(512);
		for (int x = 0; x < textureWidth; x ++)
		{
			for (int z = 0; z < textureWidth; z ++)
			{
				if (cloudLocations[x][z])
				{
					// start a new box in Z direction
					int startZ = z;
					int startX = x;
					int endZ = startZ;
					int endX = x+1;
					
					
					
					//==========================//
					// merge in the Z direction //
					//==========================//
					
					// Find the cloud's length in the Z direction
					while (endZ < textureWidth
							&& cloudLocations[x][endZ])
					{
						endZ++;
					}
					// update the z iterator so we can skip over everything included in this cloud
					z = endZ - 1;
					
					
					
					//==========================//
					// merge in the X direction //
					//==========================//
					
					for (int currentX = startX + 1; currentX < textureWidth; currentX++)
					{
						boolean canMergeInXDir = true;
						
						// check if all locations in this column are true
						for (int adjacentZ = startZ; adjacentZ < endZ; adjacentZ++)
						{
							if (!cloudLocations[currentX][adjacentZ])
							{
								// at least one pixel in the texture is false,
								// so we can't merge in this direction
								canMergeInXDir = false;
								break;
							}
						}
						
						
						if (canMergeInXDir)
						{
							// mark the adjacent column as processed
							for (int currentZ = startZ; currentZ < endZ; currentZ++)
							{
								// by flipping all the pixels in the adjacent column to false,
								// we don't have to worry about adding another cloud
								cloudLocations[currentX][currentZ] = false;
							}
							
							endX = (currentX + 1);
						}
						else
						{
							break;
						}
					}
					
					
					
					//============================//
					//  Create the renderable box //
					//============================//
					
					// endZ contains the last cloud index
					// so the cloud now goes from startZ to endZ (inclusive)
					int minXBlockPos = startX * CLOUD_BOX_WIDTH;
					int minZBlockPos = startZ * CLOUD_BOX_WIDTH;
					int maxXBlockPos = endX * CLOUD_BOX_WIDTH;
					int maxZBlockPos = endZ * CLOUD_BOX_WIDTH;
					
					// this color is changed at render time based on the level time
					Color color = new Color(255,255,255,255);
					if (DEBUG_BORDER_COLORS)
					{
						// equals is included so the boarder is 2 blocks wide, making it easier to see
						if (x <= 1) { color = Color.RED; }
						else if (x >= textureWidth - 2) { color = Color.GREEN; }
						if (z <= 1) { color = Color.BLUE; }
						else if (z >= textureWidth - 2) { color = Color.BLACK; }
					}
					
					DhApiRenderableBox box = new DhApiRenderableBox(
							new DhApiVec3d(minXBlockPos, 0, minZBlockPos),
							new DhApiVec3d(maxXBlockPos, CLOUD_BOX_THICKNESS, maxZBlockPos),
							color,
							EDhApiBlockMaterial.UNKNOWN
					);
					boxList.add(box);
				}
			}
		}
		
		//endregion
		
		
		
		//========================//
		// create the renderables //
		//========================//
		//region
		
		// slightly lighter shading than the default
		DhApiRenderableBoxGroupShading cloudShading = DhApiRenderableBoxGroupShading.getUnshaded();
		cloudShading.north = cloudShading.south = 0.9f;
		cloudShading.east = cloudShading.west = 0.8f;
		cloudShading.top = 1.0f;
		cloudShading.bottom = 0.7f;
		
		
		for (int x = -CLOUD_INSTANCE_RADIUS_COUNT; x <= CLOUD_INSTANCE_RADIUS_COUNT; x++)
		{
			for (int z = -CLOUD_INSTANCE_RADIUS_COUNT; z <= CLOUD_INSTANCE_RADIUS_COUNT; z++)
			{
				IDhApiRenderableBoxGroup boxGroup = GENERIC_OBJECT_FACTORY.createRelativePositionedGroup(
						ModInfo.NAME + ":Clouds",
						new DhApiVec3d(0, 0, 0), // the offset will be set during rendering
						boxList);
				
				// since cloud colors are set by the level based on the time of day lighting should affect it
				boxGroup.setBlockLight(LodUtil.MAX_MC_LIGHT);
				boxGroup.setSkyLight(LodUtil.MAX_MC_LIGHT);
				boxGroup.setSsaoEnabled(false);
				boxGroup.setShading(cloudShading);
				
				CloudParams cloudParams = new CloudParams(textureWidth, x, z);
				boxGroup.setPreRenderFunc((renderParam) -> this.preRender(renderParam, cloudParams));
				
				renderer.add(boxGroup);
				this.boxGroupByOffset[x+CLOUD_INSTANCE_RADIUS_COUNT][z+CLOUD_INSTANCE_RADIUS_COUNT] = boxGroup;
			}
		}
	}
	
	//endregion
	
	
	
	//===========//
 	// rendering //
 	//===========//
	//region
	
	private void preRender(DhApiRenderParam renderParam, CloudParams cloudParams)
	{
		IDhApiRenderableBoxGroup boxGroup = this.boxGroupByOffset[cloudParams.instanceOffsetX+CLOUD_INSTANCE_RADIUS_COUNT][cloudParams.instanceOffsetZ+CLOUD_INSTANCE_RADIUS_COUNT];
		
		
		
		//===================//
		// should we render? //
		//===================//
		
		boolean renderClouds = Config.Client.Advanced.Graphics.GenericRendering.enableCloudRendering.get();
		boxGroup.setActive(renderClouds);
		if(!renderClouds)
		{
			return;
		}
		
		//if (!this.renderer.getInstancedRenderingAvailable())
		//{
		//	if (!this.disabledWarningLogged)
		//	{
		//		this.disabledWarningLogged = true;
		//		LOGGER.warn("Instanced rendering unavailable, cloud rendering disabled.");
		//	}
		//	boxGroup.setActive(false);
		//	return;
		//}
		
		IClientLevelWrapper clientLevelWrapper = this.level.getClientLevelWrapper();
		if (clientLevelWrapper == null)
		{
			return;
		}
		
		
		
		//================//
		// cloud movement //
		//================//
		
		long currentTime = System.currentTimeMillis();
		float deltaTime = (currentTime - cloudParams.lastFrameTime) / 1000.0f; // Delta time in seconds
		cloudParams.lastFrameTime = currentTime;
		
		float deltaX = MOVE_SPEED_IN_BLOCKS_PER_SECOND * deltaTime;
		// negative delta is to match vanilla's cloud movement
		cloudParams.deltaOffsetX -= deltaX;
		// wrap the cloud around after reaching the edge
		cloudParams.deltaOffsetX %= cloudParams.widthInBlocks;
		
		
		
		//============================//
		// camera movement and offset //
		//============================//
		
		// camera position
		int cameraPosX = (int)MC_RENDER.getCameraExactPosition().x;
		int cameraPosZ = (int)MC_RENDER.getCameraExactPosition().z;
		// offset the camera position by negative 1 width when below zero to fix off-by-one errors in the negative direction
		if (cameraPosX < 0) { cameraPosX -= cloudParams.widthInBlocks; }
		if (cameraPosZ < 0) { cameraPosZ -= cloudParams.widthInBlocks; }
		
		// determine how many cloud instances away from the origin we are
		int cloudInstanceOffsetCountX = (cameraPosX / cloudParams.widthInBlocks);
		int cloudInstanceOffsetCountZ = (cameraPosZ / cloudParams.widthInBlocks);
		// calculate the new offset
		float instanceOffsetX = (cloudInstanceOffsetCountX * cloudParams.widthInBlocks);
		float instanceOffsetZ = (cloudInstanceOffsetCountZ * cloudParams.widthInBlocks);
		
		
		float newMinPosX = 
				cloudParams.deltaOffsetX
				+ (cloudParams.instanceOffsetX * cloudParams.widthInBlocks)
				+ instanceOffsetX + cloudParams.halfWidthInBlocks;
		float newMinPosY = this.level.getLevelWrapper().getMaxHeight() + 200;
		float newMinPosZ = cloudParams.deltaOffsetZ
				+ (cloudParams.instanceOffsetZ * cloudParams.widthInBlocks)
				+ instanceOffsetZ + cloudParams.halfWidthInBlocks;
		
		boolean cullCloud = this.shouldCloudBeCulled(
				newMinPosX, newMinPosY, newMinPosZ,
				cloudParams
			);
		if(cullCloud)
		{
			boxGroup.setActive(false);
		}
		
		
		
		//===========================//
		// update color and position //
		//===========================//
		
		// if debug colors are enabled don't change them
		if (!DEBUG_BORDER_COLORS
			// don't modify cloud groups that aren't active
			&& boxGroup.isActive())
		{
			// cloud color changes based on the time of day and weather so we need to get it from the level
			Color newCloudColor = clientLevelWrapper.getCloudColor(renderParam.partialTicks);
			
			
			// all boxes should have the same color, so we can get their current color
			// via the first box
			DhApiRenderableBox firstBox = boxGroup.get(0);
			Color currentBoxColor = firstBox.color;
			
			// update the boxes if their color should be changed
			if (!newCloudColor.equals(currentBoxColor))
			{
				// Note: cloud instances may share boxes
				// because of that this method may only need to be called once per all clouds
				for (DhApiRenderableBox box : boxGroup)
				{
					box.color = newCloudColor;
				}
			}
			
			
			// trigger an update if this cloud section has a different color
			if (!cloudParams.previousColor.equals(newCloudColor))
			{
				cloudParams.previousColor = newCloudColor;
				
				boxGroup.triggerBoxChange();
			}
		}
		
		boxGroup.setOriginBlockPos(new DhApiVec3d(newMinPosX, newMinPosY, newMinPosZ));
	}
	private boolean shouldCloudBeCulled(
			float minPosX, float minPosY, float minPosZ,
			CloudParams cloudParams)
	{
		//========================//
		// skip center 3x3 clouds //
		//========================//
		
		// always render the center 3x3 clouds, otherwise we may see 
		// an un-rendered border
		if (cloudParams.instanceOffsetX >= -1 && cloudParams.instanceOffsetX <= 1
			&& cloudParams.instanceOffsetZ >= -1 && cloudParams.instanceOffsetZ <= 1)
		{
			return false;
		}
		
		
		
		//==============//
		// culling prep //
		//==============//
		
		// we need all 4 corners since we want to draw any clouds that
		// could potentially be within render distance
		this.cullingCorners[0].x = minPosX;
		this.cullingCorners[0].y = minPosY;
		this.cullingCorners[0].z = minPosZ;
		
		this.cullingCorners[1].x = minPosX;
		this.cullingCorners[1].y = minPosY;
		this.cullingCorners[1].z = minPosZ + cloudParams.widthInBlocks;
		
		this.cullingCorners[2].x = minPosX + cloudParams.widthInBlocks;
		this.cullingCorners[2].y = minPosY;
		this.cullingCorners[2].z = minPosZ;
		
		this.cullingCorners[3].x = minPosX + cloudParams.widthInBlocks;
		this.cullingCorners[3].y = minPosY;
		this.cullingCorners[3].z = minPosZ + cloudParams.widthInBlocks;
		
		Vec3d cameraPos = MC_RENDER.getCameraExactPosition();
		Vec3f cameraLookAtVector = MC_RENDER.getLookAtVector();
		cameraLookAtVector.normalize();
		
		double renderDistance = Config.Client.Advanced.Graphics.Quality.lodChunkRenderDistanceRadius.get()
				// * 1.5 is so we have a little extra buffer where clouds will render further than
				// necessary to prevent seeing the cloud border
				* LodUtil.CHUNK_WIDTH * 1.5;
		
		
		
		//===================//
		// check each corner //
		//===================//
		
		boolean allOutsideRenderDistance = true;
		boolean allBehindCamera = true;
		
		for (Vec3d corner : this.cullingCorners)
		{
			// Check if the corner is within the render distance
			// (ignoring height, since LODs also ignore height)
			
			Vec3d cornerNoHeight = new Vec3d(corner); 
			cornerNoHeight.y = 0;
			Vec3d cameraPosNoHeight = new Vec3d(cameraPos); 
			cameraPosNoHeight.y = 0;
			
			double cornerDistance = cornerNoHeight.getDistance(cameraPosNoHeight);
			if (cornerDistance <= renderDistance)
			{
				allOutsideRenderDistance = false;
			}
			
			
			// Check if the corner is in front of the camera (dot product > 0 means in front)
			Vec3f toCorner = new Vec3f(
					(float) (corner.x - cameraPos.x),
					(float) (corner.y - cameraPos.y),
					(float) (corner.z - cameraPos.z));
			toCorner.normalize();
			
			if (cameraLookAtVector.dotProduct(toCorner) > 0)
			{
				allBehindCamera = false;
			}
		}
		
		// Cull if all corners are either behind the camera or outside the render distance
		return allOutsideRenderDistance || allBehindCamera;
	}
	
	//endregion
	
	
	
	//==================//
	// texture handling //
	//==================//
	//region
	
	private static boolean[][] getCloudsFromTexture() throws FileNotFoundException, IOException
	{
		final ClassLoader loader = CloudRenderHandler.class.getClassLoader();
		
		boolean[][] whitePixels = null;
		try(InputStream imageInputStream = loader.getResourceAsStream(CLOUD_RESOURCE_TEXTURE_PATH))
		{
			if (imageInputStream == null)
			{
				throw new FileNotFoundException("Unable to find cloud texture at resource path: ["+CLOUD_RESOURCE_TEXTURE_PATH+"].");
			}
			
			BufferedImage image = ImageIO.read(imageInputStream);
			
			int width = image.getWidth();
			int height = image.getHeight();
			
			whitePixels = new boolean[width][height];
			
			for (int x = 0; x < width; x ++)
			{
				for (int z = 0; z < width; z ++)
				{
					Color color = new Color(image.getRGB(x,z));
					whitePixels[x][z] = color.equals(Color.WHITE);
				}
			}
		}
		
		return whitePixels;
	}
	
	//endregion
	
	
	
	//================//
	// helper classes //
	//================//
	//region
	
	private static class CloudParams
	{
		public final int textureWidth;
		public final int widthInBlocks;
		public final int halfWidthInBlocks;
		
		public final int instanceOffsetX;
		public final int instanceOffsetZ;
		
		
		/** how far this cloud group has moved in the X direction based on time */
		public float deltaOffsetX = 0;
		/** how far this cloud group has moved in the Z direction based on time */
		public float deltaOffsetZ = 0;
		
		public long lastFrameTime = System.currentTimeMillis();
		
		/** used so we can trigger a VBO update when necessary */
		public Color previousColor = Color.WHITE;
		
		
		
		// constructor //
		
		public CloudParams(int textureWidth, int instanceOffsetX, int instanceOffsetZ)
		{
			this.textureWidth = textureWidth;
			this.widthInBlocks = (this.textureWidth * CLOUD_BOX_WIDTH);
			this.halfWidthInBlocks = this.widthInBlocks / 2;
			
			this.instanceOffsetX = instanceOffsetX;
			this.instanceOffsetZ = instanceOffsetZ;
		}
		
	}
	
	//endregion
	
	
	
}
