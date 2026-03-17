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

package com.seibel.distanthorizons.core.dataObjects.transformers;

import com.seibel.distanthorizons.api.enums.config.EDhApiBlocksToAvoid;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dataObjects.fullData.FullDataPointIdMap;
import com.seibel.distanthorizons.core.dataObjects.fullData.sources.FullDataSourceV2;
import com.seibel.distanthorizons.core.dataObjects.render.ColumnRenderSource;
import com.seibel.distanthorizons.core.dataObjects.render.columnViews.ColumnRenderView;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPosMutable;
import com.seibel.distanthorizons.core.util.objects.pooling.PhantomArrayListCheckout;
import com.seibel.distanthorizons.core.util.objects.pooling.PhantomArrayListPool;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.util.*;
import com.seibel.distanthorizons.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.distanthorizons.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.distanthorizons.coreapi.util.BitShiftUtil;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import com.seibel.distanthorizons.core.logging.DhLogger;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;

/**
 * Handles converting {@link FullDataSourceV2}'s to {@link ColumnRenderSource}.
 */
public class FullDataToRenderDataTransformer
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	private static final IWrapperFactory WRAPPER_FACTORY = SingletonInjector.INSTANCE.get(IWrapperFactory.class);
	
	private static final LongOpenHashSet BROKEN_POS_SET = new LongOpenHashSet();
	private static final PhantomArrayListPool ARRAY_LIST_POOL = new PhantomArrayListPool("Data Transformer");
	
	private static HashSet<IBlockStateWrapper> snowLayerBlockStates = null;
	
	
	
	//==============================//
	// public transformer interface //
	//==============================//
	
	@Nullable
	public static ColumnRenderSource transformFullDataToRenderSource(
			@Nullable FullDataSourceV2 fullDataSource, @Nullable IClientLevelWrapper levelWrapper)
	{
		if (fullDataSource == null)
		{
			return null;
		}
		else if (levelWrapper == null)
		{
			// if the client is no longer loaded in the world, render sources cannot be created 
			return null;
		}
		
		
		try
		{
			return transformCompleteFullDataToColumnData(levelWrapper, fullDataSource);
		}
		catch (InterruptedException e)
		{
			return null;
		}
	}
	
	
	
	//==============//
	// transformers //
	//==============//
	
	/**
	 * Creates a LodNode for a chunk in the given world.
	 *
	 * @throws IllegalArgumentException thrown if either the chunk or world is null.
	 * @throws InterruptedException Can be caused by interrupting the thread upstream.
	 * Generally thrown if the method is running after the client leaves the current world.
	 */
	private static ColumnRenderSource transformCompleteFullDataToColumnData(
			IClientLevelWrapper levelWrapper, FullDataSourceV2 fullDataSource) throws InterruptedException
	{
 		final long pos = fullDataSource.getPos();
		final byte dataDetail = fullDataSource.getDataDetailLevel();
		
		final int maxVertSliceCount = Config.Client.Advanced.Graphics.Quality.verticalQuality.get().calculateMaxNumberOfVerticalSlicesAtDetailLevel(fullDataSource.getDataDetailLevel());
		
		
		
		final ColumnRenderSource columnSource = ColumnRenderSource.createEmpty(pos, maxVertSliceCount, levelWrapper.getMinHeight());
		if (fullDataSource.isEmpty)
		{
			return columnSource;
		}
		
		columnSource.markNotEmpty();
		int baseX = DhSectionPos.getMinCornerBlockX(pos);
		int baseZ = DhSectionPos.getMinCornerBlockZ(pos);
		
		try(ColumnRenderView columnArrayView = ColumnRenderView.getPooled();
			PhantomArrayListCheckout phantomCheckout = ARRAY_LIST_POOL.checkoutLongArrays(1);
			ColumnRenderView tempExpandingColumnView = ColumnRenderView.getPooled();
			RenderDataPointReducingList reducingList = new RenderDataPointReducingList())
		{
			for (int x = 0; x < FullDataSourceV2.WIDTH; x++)
			{
				for (int z = 0; z < FullDataSourceV2.WIDTH; z++)
				{
					columnSource.populateColumnView(columnArrayView, x, z);
					LongArrayList dataColumn = fullDataSource.getColumnAtRelPos(x, z);
					
					updateOrReplaceRenderDataViewColumnWithFullDataColumn(
						levelWrapper, fullDataSource,
						// bit shift is to account for LODs with a detail level greater than 0 so the block pos is correct
						baseX + BitShiftUtil.pow(x, dataDetail), baseZ + BitShiftUtil.pow(z, dataDetail),
						columnArrayView, dataColumn,
						// pooled references so we don't need to re-allocate/get them 4000 times per render source
						phantomCheckout, tempExpandingColumnView, reducingList);
				}
			}
		}
		
		return columnSource;
	}
	
	/** Updates the given {@link ColumnRenderView} to match the incoming Full data {@link LongArrayList} */
	public static void updateOrReplaceRenderDataViewColumnWithFullDataColumn(
			IClientLevelWrapper levelWrapper,
			FullDataSourceV2 fullDataSource, int blockX, int blockZ, 
			ColumnRenderView columnArrayView, 
			LongArrayList fullDataColumn,
			// pooled references
			PhantomArrayListCheckout phantomCheckout, ColumnRenderView tempExpandingColumnView, RenderDataPointReducingList reducingList)
	{
		// we can't do anything if the full data is missing or empty
		if (fullDataColumn == null 
			|| fullDataColumn.size() == 0)
		{
			return;
		}
		
		int fullDataLength = fullDataColumn.size();
		if (fullDataLength <= columnArrayView.maxVerticalSliceCount)
		{
			// Directly use the arrayView since it fits.
			setRenderColumnView(levelWrapper, fullDataSource, blockX, blockZ, columnArrayView, fullDataColumn);
		}
		else
		{
			LongArrayList dataArrayList = phantomCheckout.getLongArray(0, fullDataLength);
			
			// expand the ColumnArrayView to fit the new larger max vertical size
			tempExpandingColumnView.populate(dataArrayList, fullDataLength, 0, fullDataLength);
			setRenderColumnView(levelWrapper, fullDataSource, blockX, blockZ, tempExpandingColumnView, fullDataColumn);
			
			columnArrayView.changeVerticalSizeFrom(tempExpandingColumnView, reducingList);
		}
	}
	private static void setRenderColumnView(
			IClientLevelWrapper levelWrapper, FullDataSourceV2 fullDataSource,
			int blockX, int blockZ,
			ColumnRenderView renderColumnData, LongArrayList fullColumnData)
	{
		//===============//
		// config values //
		//===============//
		
		boolean ignoreNonCollidingBlocks = (Config.Client.Advanced.Graphics.Quality.blocksToIgnore.get() == EDhApiBlocksToAvoid.NON_COLLIDING);
		boolean colorBelowWithAvoidedBlocks = Config.Client.Advanced.Graphics.Quality.tintWithAvoidedBlocks.get();
		
		final ObjectOpenHashSet<IBlockStateWrapper> blockStatesToIgnore = WRAPPER_FACTORY.getRendererIgnoredBlocks(levelWrapper);
		final ObjectOpenHashSet<IBlockStateWrapper> caveBlockStatesToIgnore = WRAPPER_FACTORY.getRendererIgnoredCaveBlocks(levelWrapper);
		final ObjectOpenHashSet<IBlockStateWrapper> waterSubsurfaceReplacementBlocks = WRAPPER_FACTORY.getWaterSubsurfaceReplacementBlocks(levelWrapper);
		final ObjectOpenHashSet<IBlockStateWrapper> waterSurfaceReplacementBlocks = WRAPPER_FACTORY.getWaterSurfaceReplacementBlocks(levelWrapper);
		final IBlockStateWrapper water = WRAPPER_FACTORY.getWaterBlockStateWrapper(levelWrapper);
		
		
		// build snow block cache if needed
		if (snowLayerBlockStates == null)
		{
			snowLayerBlockStates = new HashSet<>();
			// ignore snow layers 1-3, everything above should be considered a full block
			snowLayerBlockStates.add(WRAPPER_FACTORY.deserializeBlockStateWrapperOrGetDefault("minecraft:snow_STATE_{layers:1}", levelWrapper));
			snowLayerBlockStates.add(WRAPPER_FACTORY.deserializeBlockStateWrapperOrGetDefault("minecraft:snow_STATE_{layers:2}", levelWrapper));
			snowLayerBlockStates.add(WRAPPER_FACTORY.deserializeBlockStateWrapperOrGetDefault("minecraft:snow_STATE_{layers:3}", levelWrapper));
		}
		
		int caveCullingMaxY = Config.Client.Advanced.Graphics.Culling.caveCullingHeight.get() - levelWrapper.getMinHeight();
		boolean caveCullingEnabled = 
			Config.Client.Advanced.Graphics.Culling.enableCaveCulling.get()
			&& (
				// dimensions with a ceiling will be all caves so we don't want cave culling
				!levelWrapper.hasCeiling()
				// the end has a lot of overhangs with 0 lighting above the void, which look broken with
				// the current cave culling logic (this could probably be improved, but just skipping it works best for now)
				&& !levelWrapper.getDimensionType().isTheEnd()
			);
		
		boolean isColumnVoid = true;
		
		int colorToApplyToNextBlock = -1;
		int lastColor = 0;
		int lastBottom = -10_000;
		IBlockStateWrapper lastBlock = null;
		
		int skylightToApplyToNextBlock = -1;
		int blocklightToApplyToNextBlock = -1;
		int renderDataIndex = 0;
		
		
		
		//==================================//
		// convert full data to render data //
		//==================================//
		
		FullDataPointIdMap fullDataMapping = fullDataSource.mapping;
		
		DhBlockPosMutable mutableBlockPos = new DhBlockPosMutable(blockX, 0, blockZ);
		
		// goes from the top down
		for (int fullDataIndex = 0; fullDataIndex < fullColumnData.size(); fullDataIndex++)
		{
			long fullData = fullColumnData.getLong(fullDataIndex);
			
			int bottomY = FullDataPointUtil.getBottomY(fullData);
			int blockHeight = FullDataPointUtil.getHeight(fullData);
			int topY = bottomY + blockHeight;
			int id = FullDataPointUtil.getId(fullData);
			int blockLight = FullDataPointUtil.getBlockLight(fullData);
			int skyLight = FullDataPointUtil.getSkyLight(fullData);
			
			mutableBlockPos.setY(bottomY + levelWrapper.getMinHeight());
			
			IBiomeWrapper biome;
			IBlockStateWrapper block;
			try
			{
				biome = fullDataMapping.getBiomeWrapper(id);
				block = fullDataMapping.getBlockStateWrapper(id);
			}
			catch (IndexOutOfBoundsException e)
			{
				if (!BROKEN_POS_SET.contains(fullDataMapping.getPos()))
				{
					BROKEN_POS_SET.add(fullDataMapping.getPos());
					String levelId = levelWrapper.getDhIdentifier();
					LOGGER.warn("Unable to get data point with id ["+id+"] " +
							"(Max possible ID: ["+fullDataMapping.getMaxValidId()+"]) " +
							"for pos ["+fullDataMapping.getPos()+"] in level ["+levelId+"]. " +
							"Error: ["+e.getMessage()+"]. " +
							"Further errors for this position won't be logged.");
				}
				
				// don't render broken data
				continue;
			}
			
			
			
			//====================//
			// ignored block and  //
			// cave culling check //
			//====================//
			
			if (waterSubsurfaceReplacementBlocks.contains(block)
				&& (lastBlock == null || lastBlock.isAir()))
			{
				block = water;
			}
			
			boolean ignoreBlock = blockStatesToIgnore.contains(block);
			boolean caveBlock = caveBlockStatesToIgnore.contains(block);
			if (caveBlock
				// caves also ignore transparent/non-solid blocks (IE grass and plants) without each being defined
				|| !block.isSolid()
				|| block.isLiquid()
				|| block.getOpacity() < LodUtil.BLOCK_FULLY_OPAQUE)
			{
				if (caveCullingEnabled
					// assume this data point is underground if it has no sky-light
					&& skyLight == LodUtil.MIN_MC_LIGHT
					// ignore caves above a certain height to prevent floating islands from having walls underneath them
					&& topY < caveCullingMaxY
					// cave culling shouldn't happen when at the top of the world
					&& renderDataIndex != 0 && fullDataIndex != 0
					// cave culling can't happen when at the bottom of the world
					&& (fullDataIndex + 1) < fullColumnData.size())
				{
					// we need to get the next sky/block lights because
					// the air block here will always have a light of 0/0 due to only the top of the LOD's light being saved.
					long nextFullData = fullColumnData.getLong(fullDataIndex + 1);
					int nextSkyLight = FullDataPointUtil.getSkyLight(nextFullData);
					
					if (nextSkyLight == LodUtil.MIN_MC_LIGHT
						&& ColorUtil.getAlpha(lastColor) == 255)
					{
						// replace the previous block with new bottom
						long columnData = renderColumnData.get(renderDataIndex - 1);
						columnData = RenderDataPointUtil.setYMin(columnData, bottomY);
						renderColumnData.set(renderDataIndex - 1, columnData);
					}
					
					continue;
				}
				
				
				if (ignoreBlock)
				{
					// this is a merged block and a cave block, so it should never be rendered
					continue;
				}
			}
			else if (ignoreBlock)
			{
				// this is an ignored block, but shouldn't be merged like a cave block
				
				// applying this sky light to the next block should prevent black spots for opaque covering blocks 
				skylightToApplyToNextBlock = skyLight;
				continue;
			}
			
			
			
			//=======================//
			// non-solid block check //
			//=======================//
			
			boolean ignoreNonSolidBlock =
				ignoreNonCollidingBlocks
				&& !block.isSolid()
				&& !block.isLiquid()
				&& block.getOpacity() != LodUtil.BLOCK_FULLY_OPAQUE;
			
			// handle height reduction
			boolean isSnowLayer = snowLayerBlockStates.contains(block);
			boolean isWaterSurfaceReplacement = waterSurfaceReplacementBlocks.contains(block);
			if (isSnowLayer || isWaterSurfaceReplacement)
			{
				if (isWaterSurfaceReplacement)
				{
					// replace the block with water
					block = WRAPPER_FACTORY.getWaterBlockStateWrapper(levelWrapper);
				}
				
				// sometimes a datapoint will be multiple blocks tall,
				// in that case we just want to drop the top by 1
				blockHeight -= 1;
				if (blockHeight == 0)
				{
					// this block was entirely removed, just color the block below it
					ignoreNonSolidBlock = true;
					
					
					if (isSnowLayer)
					{
						// snow is a special case where it should always tint the block
						// below it, if not done grass will appear as gray
						int snowColor = levelWrapper.getBlockColor(mutableBlockPos, biome, fullDataSource, block);
						colorToApplyToNextBlock = ColorUtil.setAlpha(snowColor, 255);
					}
					else //if (isWaterSurfaceReplacement)
					{
						colorToApplyToNextBlock = levelWrapper.getBlockColor(mutableBlockPos, biome, fullDataSource, block);
					}
				}
			}
			
			if (ignoreNonSolidBlock)
			{
				int ignoredColor = levelWrapper.getBlockColor(mutableBlockPos, biome, fullDataSource, block);
				int ignoredAlpha = ColorUtil.getAlpha(ignoredColor);
				
				if (colorBelowWithAvoidedBlocks)
				{
					// don't transfer the color when alpha is 0
					// this prevents issues if grass is transparent
					if (ignoredAlpha != 0)
					{
						colorToApplyToNextBlock = ColorUtil.setAlpha(ignoredColor, 255);
					}
				}
				
				// Don't transfer the lighting when alpha is 0
				// (the block below should have its own lighting).
				if (ignoredAlpha != 0)
				{
					// Lighting is transferred even when "colorBelowWithAvoidedBlocks"
					// is false, since otherwise the blocks underneath may have a light value of "0"
					// which makes things look darker than they should.
					// This can specifically manifest as grid lines on LOD borders
					// (not entire sure why grid lines on LOD borders, maybe it has to do with the fact that those LODs aren't occluded?).
					skylightToApplyToNextBlock = skyLight;
					blocklightToApplyToNextBlock = blockLight;
				}
				
				// skip this non-colliding block
				continue;
			}
			
			
			int color;
			if (colorToApplyToNextBlock == -1)
			{
				// use this block's color
				color = levelWrapper.getBlockColor(mutableBlockPos, biome, fullDataSource, block);
				
				// use the skylight override if present
				if (skylightToApplyToNextBlock != -1)
				{
					skyLight = skylightToApplyToNextBlock;
					// remove the override so we don't accidentally override the next datapoint
					skylightToApplyToNextBlock = -1;
				}
				
				if (blocklightToApplyToNextBlock != -1)
				{
					blockLight = blocklightToApplyToNextBlock;
					blocklightToApplyToNextBlock = -1;
				}
			}
			else
			{
				// use the previous block's color
				color = colorToApplyToNextBlock;
				colorToApplyToNextBlock = -1;
				skyLight = skylightToApplyToNextBlock;
				blockLight = blocklightToApplyToNextBlock;
			}
			
			
			
			//=============================//
			// merge same-colored adjacent //
			//=============================//
			
			// check if they share a top-bottom face and if they have same color
			if (color == lastColor 
				&& bottomY + blockHeight == lastBottom  
				&& renderDataIndex > 0)
			{
				//replace the previous block with new bottom
				long columnData = renderColumnData.get(renderDataIndex - 1);
				columnData = RenderDataPointUtil.setYMin(columnData, bottomY);
				renderColumnData.set(renderDataIndex - 1, columnData);
			}
			else
			{
				// add the block
				isColumnVoid = false;
				long columnData = RenderDataPointUtil.createDataPoint(bottomY + blockHeight, bottomY, color, skyLight, blockLight, block.getMaterialId());
				renderColumnData.set(renderDataIndex, columnData);
				renderDataIndex++;
			}
			lastBottom = bottomY;
			lastColor = color;
			lastBlock = block;
		}
		
		
		if (isColumnVoid)
		{
			renderColumnData.set(0, RenderDataPointUtil.EMPTY_DATA);
		}
	}
	
	
	
}
