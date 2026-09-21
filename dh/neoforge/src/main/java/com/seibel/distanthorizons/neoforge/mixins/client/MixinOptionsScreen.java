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

import com.seibel.distanthorizons.common.wrappers.gui.DhScreenUtil;
import com.seibel.distanthorizons.common.wrappers.gui.GetConfigScreen;
import com.seibel.distanthorizons.common.wrappers.gui.TexturedButtonWidget;
import com.seibel.distanthorizons.coreapi.ModInfo;
import com.seibel.distanthorizons.core.config.Config;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
#if MC_VER < MC_1_19_2
import net.minecraft.network.chat.TranslatableComponent;
#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

#if MC_VER >= MC_1_20_6
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
#endif

#if MC_VER < MC_1_21_1
import net.minecraft.client.gui.screens.OptionsScreen;
#else
import net.minecraft.client.gui.screens.options.OptionsScreen;
#endif

#if MC_VER <= MC_1_21_10
import net.minecraft.resources.ResourceLocation;
#else
import net.minecraft.resources.Identifier;
#endif

/**
 * Adds a button to the menu to goto the config
 *
 * @author coolGi
 * @version 2024-5-20
 */
@Mixin(OptionsScreen.class)
public class MixinOptionsScreen extends Screen
{
	/** Texture used for the config opening button */
	#if MC_VER <= MC_1_20_6
	@Unique
	private static final ResourceLocation ICON_TEXTURE = new ResourceLocation(ModInfo.ID, "textures/gui/button.png");
	#elif MC_VER <= MC_1_21_10
	@Unique
	private static final ResourceLocation ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath(ModInfo.ID, "textures/gui/button.png");
	#else
	@Unique
	private static final Identifier ICON_TEXTURE = Identifier.fromNamespaceAndPath(ModInfo.ID, "textures/gui/button.png");
	#endif
	
	
	@Unique
	private TexturedButtonWidget optionsButton = null;
	
	#if MC_VER >= MC_1_20_6
	@Shadow
	@Final
	protected HeaderAndFooterLayout layout;
	#endif
	
	
	
	//==============//
	// constructors //
	//==============//
	
	protected MixinOptionsScreen(Component title) { super(title); }
	
	@Inject(at = @At("RETURN"), method = "init")
	private void dhConfig$init(CallbackInfo ci)
	{
		if (Config.Client.showDhOptionsButtonInMinecraftUi.get())
		{
			#if MC_VER < MC_1_17_1
			this.addButton(this.getOptionsButton());
			#elif MC_VER < MC_1_20_6
			this.addRenderableWidget(this.getOptionsButton());
			#else
			
			// add the button so it's rendered 
			this.addRenderableWidget(this.getOptionsButton());
			
			// add the button to the correct location in the UI
			LinearLayout layout = (LinearLayout) this.layout.headerFrame.children.get(0).child;
			
			// determine how wide the other option buttons are so we can put our botton to the left of them all
			AtomicInteger width = new AtomicInteger(0);
			layout.visitChildren(x -> { width.addAndGet(x.getWidth()); });
			width.addAndGet(-10); // padding between the DH button and the FOV slider
			
			layout.wrapped.addChild(this.getOptionsButton(), 1, 2, (settings) -> { settings.paddingLeft(width.get() * -1); });
			layout.arrangeElements();
			
		    #endif
		}
	}
	
	
	
	//================//
	// helper methods //
	//================//
	
	@Unique
	public TexturedButtonWidget getOptionsButton()
	{
		if (this.optionsButton == null)
		{
			this.optionsButton
					= new TexturedButtonWidget(
					// Where the button is on the screen
					this.width / 2 - 180, this.height / 6 - 12,
					// Width and height of the button
					20, 20,
					// texture UV Offset
					0, 0,
					// Some textuary stuff
					20, ICON_TEXTURE, 20, 40,
					// Create the button and tell it where to go
					// For now it goes to the client option by default
					(buttonWidget) -> DhScreenUtil.setScreen(GetConfigScreen.getScreen(this)),
					// Add a title to the button
                    #if MC_VER < MC_1_19_2
					new TranslatableComponent(ModInfo.ID + ".title"));
                    #else
					Component.translatable(ModInfo.ID + ".title"));
                    #endif
		}
		
		return this.optionsButton;
	}
	
}
