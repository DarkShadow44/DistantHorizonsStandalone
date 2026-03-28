package com.seibel.distanthorizons.common.wrappers.minecraft;

import java.io.File;

import net.minecraft.server.dedicated.DedicatedServer;

import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftSharedWrapper;

// @Environment(EnvType.SERVER)
public class MinecraftServerWrapper implements IMinecraftSharedWrapper {

    public static final MinecraftServerWrapper INSTANCE = new MinecraftServerWrapper();

    public DedicatedServer dedicatedServer = null;

    // =============//
    // constructor //
    // =============//

    private MinecraftServerWrapper() {}

    // =========//
    // methods //
    // =========//

    @Override
    public boolean isDedicatedServer() {
        return true;
    }

    @Override
    public File getInstallationDirectory() {
        if (this.dedicatedServer == null) {
            throw new IllegalStateException(
                "Trying to get Installation Direction before Dedicated server completed initialization!");
        }

        return this.dedicatedServer.getFile("test")
            .getParentFile();
    }

    @Override
    public int getPlayerCount() {
        return this.dedicatedServer.getCurrentPlayerCount();
    }

}
