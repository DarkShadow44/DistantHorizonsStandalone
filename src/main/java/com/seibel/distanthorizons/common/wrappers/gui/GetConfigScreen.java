package com.seibel.distanthorizons.common.wrappers.gui;

import net.minecraft.client.gui.GuiScreen;

import com.seibel.distanthorizons.core.config.ConfigHandler;

public class GetConfigScreen {

    public static GuiScreen getScreen(GuiScreen parent) {
        return ClassicConfigGUI.getScreen(ConfigHandler.INSTANCE, parent, "client");
    }

}
