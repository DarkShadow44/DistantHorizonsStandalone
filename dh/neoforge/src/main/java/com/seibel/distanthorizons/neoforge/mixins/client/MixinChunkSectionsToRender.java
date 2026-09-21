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

package com.seibel.distanthorizons.neoforge.mixins.client;

#if MC_VER <= MC_1_21_11
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Entity.class)
public class MixinChunkSectionsToRender
{ /* rendering before was handled via Fabric API events */ }
#else

import com.mojang.blaze3d.textures.GpuSampler;
import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkSectionsToRender.class)
public class MixinChunkSectionsToRender
{
	
	//============//
	// post MC 26 //
	//============//
	//region
	
	#if MC_VER <= MC_1_21_11
	#else
	
	// needs to fire at HEAD with a lower than normal order (less than 1000)
	// otherwise it will be canceled by Sodium
	@Inject(at = @At("HEAD"), method = "renderGroup", order = 800)
	private void renderDeferredLayerHead(ChunkSectionLayerGroup chunkSectionLayerGroup, GpuSampler gpuSampler, CallbackInfo ci)
	{
		#if MC_VER <= MC_26_1_2
		ClientApi.RENDER_STATE.clientLevelWrapper = ClientLevelWrapper.getWrapperIfDifferent(ClientApi.RENDER_STATE.clientLevelWrapper, Minecraft.getInstance().levelRenderer.level);
		#else
		#endif
		
		
		ClientApi.RENDER_STATE.canRenderOrThrow();
		
		if (chunkSectionLayerGroup == ChunkSectionLayerGroup.TRANSLUCENT)
		{
			ClientApi.INSTANCE.renderDeferredLodsForShaders();
		}
		else if (chunkSectionLayerGroup == ChunkSectionLayerGroup.OPAQUE)
		{
			ClientApi.INSTANCE.renderLods();
		}
	}
	
	//endregion
	#endif
	
	
	
}

#endif

