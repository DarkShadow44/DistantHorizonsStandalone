package com.seibel.distanthorizons.common.wrappers.gui;

import net.minecraft.client.gui.GuiScreen;

public class GetConfigScreen {
    public static GuiScreen getScreen(GuiScreen parent) {
        return ClassicConfigGUI.getScreen(ConfigBase.INSTANCE, parent, "client");
    }

}
