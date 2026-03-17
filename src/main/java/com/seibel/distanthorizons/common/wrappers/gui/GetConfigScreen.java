package com.seibel.distanthorizons.common.wrappers.gui;

import com.seibel.distanthorizons.core.config.ConfigBase;
import net.minecraft.client.gui.GuiScreen;

public class GetConfigScreen {
    public static GuiScreen getScreen(GuiScreen parent) {
        return ClassicConfigGUI.getScreen(ConfigBase.INSTANCE, parent, "client");
    }

}
