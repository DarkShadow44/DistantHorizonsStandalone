package com.seibel.distanthorizons.forge.mixins.client;

import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener
{
	@Inject(method = "handleLogin", at = @At("RETURN"))
	void onHandleLoginEnd(CallbackInfo ci) { ClientApi.INSTANCE.onClientOnlyConnected(); }
	
	#if MC_VER < MC_1_19_4
	@Inject(method = "cleanup", at = @At("HEAD"))
	#else
	@Inject(method = "close", at = @At("HEAD"))
	#endif
	void onCleanupStart(CallbackInfo ci) { ClientApi.INSTANCE.onClientOnlyDisconnected(); }
	
}
