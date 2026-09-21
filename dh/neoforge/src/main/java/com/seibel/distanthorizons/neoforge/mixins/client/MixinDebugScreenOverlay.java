package com.seibel.distanthorizons.neoforge.mixins.client;

#if MC_VER < MC_1_21_9
import com.seibel.distanthorizons.core.logging.f3.F3Screen;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
#else
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.gui.components.DebugScreenOverlay;
#endif

@Mixin(DebugScreenOverlay.class)
public class MixinDebugScreenOverlay
{
	
	#if MC_VER < MC_1_21_9
	@Inject(method = "getSystemInformation", at = @At("RETURN"))
	private void addCustomF3(CallbackInfoReturnable<List<String>> cir)
	{
		List<String> messages = cir.getReturnValue();
		F3Screen.addStringToDisplay(messages);
	}
	#else
	// handled by DhDebugScreenEntry for MC versions after 1.21.10
	#endif
	
}
