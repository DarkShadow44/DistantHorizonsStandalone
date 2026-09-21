package com.seibel.distanthorizons.core.dataObjects.transformers;

import com.seibel.distanthorizons.core.dataObjects.fullData.sources.FullDataSourceV2;
import com.seibel.distanthorizons.core.util.FullDataPointUtil;
import com.seibel.distanthorizons.core.util.LodUtil;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.jetbrains.annotations.NotNull;

public class FullDataOcclusionCuller
{
	/**
	 * Mutates the given datasource so blocks that aren't visible
	 * (IE completely surrounded by other opaque blocks)
	 * are removed from the data column.
	 * 
	 * @param dataSource
	 * @param relX relative X position in the datasource
	 * @param relZ relative Z position in the datasource
	 */
	public static void cullHiddenDatapointsInColumn(
			FullDataSourceV2 dataSource,
			int relX, int relZ
	)
	{
		LongArrayList centerColumn = dataSource.getColumnAtRelPos(relX, relZ);
		LongArrayList posXColumn = dataSource.tryGetColumnAtRelPos(relX + 1, relZ);
		LongArrayList negXColumn = dataSource.tryGetColumnAtRelPos(relX - 1, relZ);
		LongArrayList posZColumn = dataSource.tryGetColumnAtRelPos(relX, relZ + 1);
		LongArrayList negZColumn = dataSource.tryGetColumnAtRelPos(relX, relZ - 1);
		
		if (posXColumn == null || posXColumn.size() == 0
			|| negXColumn == null || negXColumn.size() == 0
			|| posZColumn == null || posZColumn.size() == 0
			|| negZColumn == null || negZColumn.size() == 0)
		{
			// if any adjacent columns are empty then we can't
			// cull this column, since at least one side will be open
			// to air/void
			return;
		}
		
		
		int centerIndex = centerColumn.size() - 1;
		int posXIndex = (posXColumn.size() - 1);
		int negXIndex = (negXColumn.size() - 1);
		int posZIndex = (posZColumn.size() - 1);
		int negZIndex = (negZColumn.size() - 1);
		
		for (; centerIndex >= 0; centerIndex--)
		{
			long currentPoint = centerColumn.getLong(centerIndex);
			
			// Translucent data points are not eligible to be culled.
			if (isTranslucent(dataSource, currentPoint))
			{
				continue;
			}
			
			// the top segment should never be culled.
			if (centerIndex == 0
				|| isTranslucent(dataSource, centerColumn.getLong(centerIndex - 1)))
			{
				continue;
			}
			
			// the bottom segment can sometimes be culled.
			// assume it will not be seen from below,
			// because this would imply the player is in the void.
			if (centerIndex + 1 < centerColumn.size()
				&& isTranslucent(dataSource, centerColumn.getLong(centerIndex + 1)))
			{
				continue;
			}
			
			// the lowest/bedrock segment should not be culled
			if (centerIndex + 1 == centerColumn.size())
			{
				continue;
			}
			
			
			posXIndex = checkOcclusion(dataSource, currentPoint, posXColumn, posXIndex);
			if (posXIndex < 0)
			{
				posXIndex = ~posXIndex;
				continue;
			}
			
			negXIndex = checkOcclusion(dataSource, currentPoint, negXColumn, negXIndex);
			if (negXIndex < 0)
			{
				negXIndex = ~negXIndex;
				continue;
			}
			
			posZIndex = checkOcclusion(dataSource, currentPoint, posZColumn, posZIndex);
			if (posZIndex < 0)
			{
				posZIndex = ~posZIndex;
				continue;
			}
			
			negZIndex = checkOcclusion(dataSource, currentPoint, negZColumn, negZIndex);
			if (negZIndex < 0)
			{
				negZIndex = ~negZIndex;
				continue;
			}
			
			
			// Current point is fully surrounded. remove it.
			centerColumn.removeLong(centerIndex);
			
			// Make the above data point cover the area that the current point used to occupy.
			// The element that was at `centerIndex - 1` is still at that position even after removal of centerIndex.
			long above = centerColumn.getLong(centerIndex - 1);
			above = FullDataPointUtil.setBottomY(above, FullDataPointUtil.getBottomY(currentPoint));
			above = FullDataPointUtil.setHeight(above, FullDataPointUtil.getHeight(currentPoint) + FullDataPointUtil.getHeight(above));
			centerColumn.set(centerIndex - 1, above);
		}
	}
	/**
	 checks if centerPoint is "covered" by opaque data points in adjacentColumn.
	 centerPoint counts as covered if, and only if, for all Y levels in its height range,
	 there exists an opaque data point in adjacentColumn which overlaps with that Y level.
	 
	 @param source used to lookup blocks (and their opacities) based on their IDs.
	 @param centerPoint the point being checked to see if it's fully covered.
	 @param adjacentColumn the data points which might cover centerPoint.
	 @param adjacentIndex the starting index in adjacentColumn to start scanning at.
	 indices greater than adjacentIndex have already been checked and confirmed to
	 not overlap or only overlap partially with centerPoint's Y range.
	 
	 @return if centerPoint is covered, returns the index of the segment which finishes covering it.
	 the start of the covering may be a smaller index. in this case, the returned index may be used
	 as the adjacentIndex provided to this method on the next iteration which yields a new centerPoint.
	 
	 if centerPoint is NOT covered, returns the bitwise negation of the index of the
	 segment which did not cover it. this guarantees that the returned value is negative.
	 the caller should check for negative return values and manually un-negate them to proceed with the loop.
	 
	 in other words, this function returns the index of the next adjacent data
	 point to use in the loop, AND a boolean indicating whether or not the
	 centerPoint is covered;	both are packed into the same int, and returned.
	 */
	private static int checkOcclusion(@NotNull FullDataSourceV2 source, long centerPoint, @NotNull LongArrayList adjacentColumn, int adjacentIndex)
	{
		// check if this point is adjacent to an empty column
		// if so it will always be shown
		if (adjacentColumn.isEmpty())
		{
			return ~adjacentIndex;
		}
		else if (adjacentColumn.size() == 1
				&& adjacentColumn.getLong(0) == FullDataPointUtil.EMPTY_DATA_POINT)
		{	
			return ~adjacentIndex;
		}
		
		
		int bottomOfCenter = FullDataPointUtil.getBottomY(centerPoint);
		int topOfCenter = bottomOfCenter + FullDataPointUtil.getHeight(centerPoint);
		for (; adjacentIndex >= 0; adjacentIndex--)
		{
			long adjacentPoint = adjacentColumn.getLong(adjacentIndex);
			int topOfAdjacent = FullDataPointUtil.getBottomY(adjacentPoint) + FullDataPointUtil.getHeight(adjacentPoint);
			if (topOfAdjacent <= bottomOfCenter)
			{
				// the adjacent point is below the center point,
				// check the next one
				continue;
			}
			else if (isTranslucent(source, adjacentPoint))
			{
				// this point is adjacent to a transparent LOD and should be shown
				return ~adjacentIndex;
			}
			else if (topOfAdjacent >= topOfCenter)
			{
				// the adjacent point covers the center point
				return adjacentIndex;
			}
		}
		
		
		// the Adjacent column ends before center column does,
		// this point should be visible
		return ~adjacentIndex;
	}
	private static boolean isTranslucent(FullDataSourceV2 source, long point)
	{
		int id = FullDataPointUtil.getId(point);
		int opacity = source.mapping.getBlockStateWrapper(id).getOpacity();
		return opacity < LodUtil.BLOCK_FULLY_OPAQUE;
	}
	
	
	
}
