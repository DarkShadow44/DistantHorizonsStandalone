package com.seibel.distanthorizons.common.wrappers.gui;

import com.seibel.distanthorizons.core.config.ConfigHandler;
import net.minecraft.client.gui.GuiScreen;

public class GetConfigScreen {
    public static GuiScreen getScreen(GuiScreen parent) {
        return ClassicConfigGUI.getScreen(ConfigHandler.INSTANCE, parent, "client");
    }

}
