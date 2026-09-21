package com.seibel.distanthorizons.fabric.mixins.client;

import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.api.internal.SharedApi;
import com.seibel.distanthorizons.core.util.threading.ThreadPoolUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

#if MC_VER >= MC_1_20_1
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import net.minecraft.world.level.chunk.LevelChunk;
import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;

import java.util.concurrent.AbstractExecutorService;
#endif

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener
{
	@Shadow
	private ClientLevel level;
	
	@Inject(method = "handleLogin", at = @At("RETURN"))
	void onHandleLoginEnd(CallbackInfo ci) 
	{ 
		ClientApi.INSTANCE.onClientOnlyConnected();
	}
		
	#if MC_VER < MC_1_19_4
	@Inject(method = "cleanup", at = @At("HEAD"))
	#else
	@Inject(method = "close", at = @At("HEAD"))
	#endif
	void onCleanupStart(CallbackInfo ci)
	{
		ClientApi.INSTANCE.onClientOnlyDisconnected();
	}
	
	#if MC_VER >= MC_1_20_1
	@Inject(method = "enableChunkLight", at = @At("TAIL"))
	void onEnableChunkLight(LevelChunk chunk, int x, int z, CallbackInfo ci)
	{
		if (chunk == null)
		{
			return;
		}
		
		// executor to prevent locking up the render thread
		AbstractExecutorService executor = ThreadPoolUtil.getFileHandlerExecutor();
		if (executor == null)
		{
			return;
		}
		
		// Important to get the level from the chunk because the client level might be different if Immersive Portals is present.
		ClientLevel clientLevel = (ClientLevel) chunk.getLevel();
		executor.execute(() ->
		{
			IClientLevelWrapper clientLevelWrapper = ClientLevelWrapper.getWrapper(clientLevel);
			SharedApi.INSTANCE.applyChunkUpdate(new ChunkWrapper(chunk, clientLevelWrapper), clientLevelWrapper, true);
		});
	}

	#endif
	
}
