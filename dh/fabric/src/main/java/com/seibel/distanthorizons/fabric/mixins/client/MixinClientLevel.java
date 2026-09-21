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

package com.seibel.distanthorizons.fabric.mixins.client;

import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;
import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.api.internal.SharedApi;
import net.minecraft.client.multiplayer.ClientLevel;
#if MC_VER >= MC_1_18_2
#endif
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This class is used for world loading events
 *
 * @author Ran
 */

@Mixin(ClientLevel.class)
public class MixinClientLevel
{
	// Moved to overriding the enableChunkLight(...) method over at ClientPacketListener for 1.20+
	#if MC_VER >= MC_1_18_2 && MC_VER < MC_1_20_1 // Only the setLightReady is only available after 1.18. This ensures the light data is ready.
	@Inject(method = "setLightReady", at = @At("HEAD"))
	private void onChunkLightReady(int x, int z, CallbackInfo ci)
	{
		ClientLevel clientLevel = (ClientLevel) (Object) this;
		LevelChunk chunk = clientLevel.getChunkSource().getChunk(x, z, false);
		
		if (chunk != null && !chunk.isClientLightReady())
		{
			SharedApi.INSTANCE.applyChunkUpdate(
				new ChunkWrapper(chunk, ClientLevelWrapper.getWrapper(clientLevel)), 
				ClientLevelWrapper.getWrapper(clientLevel),
				true
			);
		}
	}
	#endif
	
}
