/*
 * This file is part of the Distant Horizons mod
 * licensed under the GNU LGPL v3 License.
 * Copyright (C) 2020 James Seibel
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.distanthorizons.common.wrappers.world;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.WorldServer;

import org.jetbrains.annotations.Nullable;

import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiLevelType;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiCustomRenderRegister;
import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IServerLevelWrapper;

public class ServerLevelWrapper implements IServerLevelWrapper {

    private static final DhLogger LOGGER = new DhLoggerBuilder().build();
    private static final ConcurrentHashMap<WorldServer, ServerLevelWrapper> LEVEL_WRAPPER_BY_SERVER_LEVEL = new ConcurrentHashMap<>();

    private final WorldServer level;
    private IDhLevel dhLevel;

    // ==============//
    // constructors //
    // ==============//

    public static ServerLevelWrapper getWrapper(WorldServer level) {
        return LEVEL_WRAPPER_BY_SERVER_LEVEL.computeIfAbsent(level, ServerLevelWrapper::new);
    }

    public ServerLevelWrapper(WorldServer level) {
        this.level = level;
    }

    // =========//
    // methods //
    // =========//

    @Override
    public File getMcSaveFolder() {
        return new File(this.level.getChunkSaveLocation(), "data");
    }

    @Override
    public String getKeyedLevelDimensionName() {
        return getDimensionName();
    }

    @Override
    public DimensionTypeWrapper getDimensionType() {
        return DimensionTypeWrapper.getDimensionTypeWrapper(this.level.provider.dimensionId);
    }

    @Override
    public String getDimensionName() {
        return DimensionTypeWrapper.getDimensionTypeWrapper(this.level.provider.dimensionId)
            .getName();
    }

    @Override
    public long getHashedSeed() {
        return this.level.getSeed();
    } // TODO?

    @Override
    public String getDhIdentifier() {
        return this.getDimensionName();
    }

    @Override
    public EDhApiLevelType getLevelType() {
        return EDhApiLevelType.SERVER_LEVEL;
    }

    public WorldServer getLevel() {
        return this.level;
    }

    @Override
    public boolean hasCeiling() {
        return DimensionTypeWrapper.getDimensionTypeWrapper(this.level.provider.dimensionId)
            .hasCeiling();
    }

    @Override
    public boolean hasSkyLight() {
        return DimensionTypeWrapper.getDimensionTypeWrapper(this.level.provider.dimensionId)
            .hasSkyLight();
    }

    @Override
    public int getMaxHeight() {
        return this.level.getHeight();
    }

    @Override
    public int getMinHeight() {
        return 0;
    }

    @Override
    public WorldServer getWrappedMcObject() {
        return this.level;
    }

    @Override
    public void onUnload() {
        LEVEL_WRAPPER_BY_SERVER_LEVEL.remove(this.level);
    }

    @Override
    public void setDhLevel(IDhLevel level) {
        dhLevel = level;
    }

    @Override
    public @Nullable IDhLevel getDhLevel() {
        return dhLevel;
    }

    @Override
    public IDhApiCustomRenderRegister getRenderRegister() {
        if (this.dhLevel == null) {
            return null;
        }

        return this.dhLevel.getGenericRenderer();
    }

    @Override
    public File getDhSaveFolder() {
        if (this.dhLevel == null) {
            return null;
        }

        return this.dhLevel.getSaveStructure()
            .getSaveFolder(this);
    }

    // ================//
    // base overrides //
    // ================//

    @Override
    public String toString() {
        return "Wrapped{" + this.level.toString() + "@" + this.getDhIdentifier() + "}";
    }

}
