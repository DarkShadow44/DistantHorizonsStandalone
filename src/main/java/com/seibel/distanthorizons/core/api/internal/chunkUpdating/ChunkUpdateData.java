package com.seibel.distanthorizons.core.api.internal.chunkUpdating;

import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class ChunkUpdateData
{
	public IChunkWrapper chunkWrapper;
	public IDhLevel dhLevel;
	
	
	
	public ChunkUpdateData(IChunkWrapper chunkWrapper, IDhLevel dhLevel)
	{
		this.chunkWrapper = chunkWrapper;
		this.dhLevel = dhLevel;
	}
}
