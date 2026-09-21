package com.seibel.distanthorizons.common.wrappers.misc;

#if MC_VER <= MC_1_12_2
import net.minecraft.world.WorldServer;
#else
import net.minecraft.server.level.ServerLevel;
#endif

import org.jetbrains.annotations.Nullable;

public interface IMixinServerPlayer
{
	@Nullable
	#if MC_VER <= MC_1_12_2
	WorldServer distantHorizons$getDimensionChangeDestination();
	#else
	ServerLevel distantHorizons$getDimensionChangeDestination();
	#endif
	
	#if MC_VER == MC_1_16_5
	void distantHorizons$setDimensionChangeDestination(ServerLevel dimensionChangeDestination);
	#endif
	
}
