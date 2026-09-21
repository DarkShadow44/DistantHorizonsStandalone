package com.seibel.distanthorizons.common.wrappers.gui;

import net.minecraft.client.Minecraft;
#if MC_VER <= MC_1_12_2
import net.minecraft.client.gui.GuiScreen;
#else
import net.minecraft.client.gui.screens.Screen;
#endif

import java.util.Objects;

public class DhScreenUtil
{
	//================//
	// helper methods //
	//================//
	//region
	
	#if MC_VER <= MC_1_12_2
	public static void setScreen(GuiScreen screen)
	#else
	public static void setScreen(Screen screen)
	#endif
	{
		#if MC_VER <= MC_1_12_2
		Objects.requireNonNull(Minecraft.getMinecraft()).displayGuiScreen(screen);
		#elif MC_VER <= MC_26_1_2
		Objects.requireNonNull(Minecraft.getInstance()).setScreen(screen);
		#else
		Objects.requireNonNull(Minecraft.getInstance()).setScreenAndShow(screen);
		#endif
	}
	
	//endregion
	
	
	
}
