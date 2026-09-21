package com.seibel.distanthorizons.core.util;

import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos;

public class WorldGenUtil
{
	
	/** will always return true if a generation max radius isn't set */
	public static boolean isPosInWorldGenRange(
		long requestedPos,
		int centerChunkX, int centerChunkZ,
		int maxChunkRadius)
	{
		if (Config.Common.WorldGenerator.generationMaxChunkRadius.get() <= 0)
		{
			return true;
		}
		
		
		DhBlockPos centerBlockPos = new DhChunkPos(centerChunkX, centerChunkZ).centerBlockPos();
		int blockDistanceFromCenter = DhSectionPos.getChebyshevSignedBlockDistance(requestedPos, centerBlockPos);
		int maxBlockRadius = maxChunkRadius * LodUtil.CHUNK_WIDTH;
		boolean requestInRadius = (blockDistanceFromCenter <= maxBlockRadius);
		return requestInRadius;
	}
	
	/** @return -1 for infinity */
	public static int getMaxRegenDistanceInBlocks()
	{
		int lodChunkDist = Config.Client.Advanced.Graphics.Quality.lodChunkRenderDistanceRadius.get();
		int lodBlockDist = lodChunkDist * LodUtil.CHUNK_WIDTH;
		
		double percent = Config.Common.WorldGenerator.surfaceRegenMaxDistancePercent.get();
		if (percent < 0.0f)
		{
			return -1;
		}
		
		return (int)(lodBlockDist * percent);
	}
	
	
	
}
