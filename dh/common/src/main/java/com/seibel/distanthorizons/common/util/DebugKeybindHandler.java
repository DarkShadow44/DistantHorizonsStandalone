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

package com.seibel.distanthorizons.common.util;

import com.seibel.distanthorizons.api.enums.rendering.EDhApiDebugRendering;
import com.seibel.distanthorizons.api.enums.rendering.EDhApiRendererMode;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftClientWrapper;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import net.minecraft.client.Minecraft;

#if MC_VER > MC_1_12_2
import com.mojang.blaze3d.platform.InputConstants;
#if MC_VER <= MC_26_2_0
import org.lwjgl.glfw.GLFW;
#endif
#endif

/** 
 * Handles DH's debug keybinds (F6, F7 and F8). 
 * Since key codes change between Minecraft versions this lives in common.
 */
public class DebugKeybindHandler
{
	private static final IMinecraftClientWrapper MC_CLIENT = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	
	#if MC_VER <= MC_1_12_2
	// TODO: Verify and implement for 1.12.2 and older
	private static final int KEY_F6 = -1;
	private static final int KEY_F7 = -1;
	private static final int KEY_F8 = -1;
	#elif MC_VER <= MC_26_2_0
	private static final int KEY_F6 = GLFW.GLFW_KEY_F6;
	private static final int KEY_F7 = GLFW.GLFW_KEY_F7;
	private static final int KEY_F8 = GLFW.GLFW_KEY_F8;
	#else
	// 26.3 uses SDL key codes instead of GLFW
	private static final int KEY_F6 = InputConstants.KEY_F6;
	private static final int KEY_F7 = InputConstants.KEY_F7;
	private static final int KEY_F8 = InputConstants.KEY_F8;
	#endif
	
	private static final int[] DEBUG_KEYS = { KEY_F6, KEY_F7, KEY_F8 };
	private static final boolean[] wasDebugKeyDown = new boolean[DEBUG_KEYS.length];
	
	
	
	//==================//
	// key press events //
	//==================//
	//region
	
	/** @param keyCode Minecraft's key code for the key that was pressed */
	public static void onKeyPressed(int keyCode)
	{
		if (!Config.Client.Advanced.Debugging.enableDebugKeybindings.get())
		{
			return;
		}
		
		if (keyCode == KEY_F6)
		{
			Config.Client.Advanced.Debugging.rendererMode.set(EDhApiRendererMode.next(Config.Client.Advanced.Debugging.rendererMode.get()));
			MC_CLIENT.sendChatMessage("F6: Set rendering to " + Config.Client.Advanced.Debugging.rendererMode.get());
		}
		else if (keyCode == KEY_F7)
		{
			Config.Client.Advanced.Debugging.lodOnlyMode.set(!Config.Client.Advanced.Debugging.lodOnlyMode.get());
			MC_CLIENT.sendChatMessage("F7: Set LOD only mode to " + Config.Client.Advanced.Debugging.lodOnlyMode.get());
		}
		else if (keyCode == KEY_F8)
		{
			Config.Client.Advanced.Debugging.debugRenderingColors.set(EDhApiDebugRendering.next(Config.Client.Advanced.Debugging.debugRenderingColors.get()));
			MC_CLIENT.sendChatMessage("F8: Set debug mode to " + Config.Client.Advanced.Debugging.debugRenderingColors.get());
		}
	}
	
	//endregion
	
	
	
	//=============//
	// tick events //
	//=============//
	//region
	
	/** For loaders without a key press event (Fabric), checks the debug keys once per client tick */
	public static void checkDebugKeysEachTick()
	{
		// Don't check every tick if the debug keys setting is off
		if (!Config.Client.Advanced.Debugging.enableDebugKeybindings.get())
		{
			return;
		}

		// don't trigger while typing in a menu or text box
		boolean screenOpen = isScreenOpen();
		
		for (int i = 0; i < DEBUG_KEYS.length; i++)
		{
			boolean isDown = !screenOpen && isKeyDown(DEBUG_KEYS[i]);
			if (isDown && !wasDebugKeyDown[i])
			{
				onKeyPressed(DEBUG_KEYS[i]);
			}
			wasDebugKeyDown[i] = isDown;
		}
	}
	private static boolean isScreenOpen()
	{
		#if MC_VER <= MC_1_12_2
		// TODO: Verify and implement for 1.12.2 and older
		//return Minecraft.getMinecraft().currentScreen != null;
		return true;
		#elif MC_VER <= MC_26_2_0
		return Minecraft.getInstance().screen != null;
		#else
		return Minecraft.getInstance().gui.screen() != null;
		#endif
	}
	private static boolean isKeyDown(int keyCode)
	{
		#if MC_VER <= MC_1_12_2
		// TODO: Verify and implement for 1.12.2 and older
		//return Keyboard.isKeyDown(keyCode);
		return false;
		#elif MC_VER <= MC_26_2_0
		return InputConstants.isKeyDown(MinecraftClientWrapper.INSTANCE.getGlfwWindowId(), keyCode);
		#else
		return InputConstants.isKeyDown(keyCode);
		#endif
	}
	
	//endregion
	
	
	
}
