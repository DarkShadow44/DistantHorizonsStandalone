package com.seibel.distanthorizons;

import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.TargetModBuilder;
import org.jetbrains.annotations.NotNull;

public enum TargetedMod implements ITargetMod {

    ANGELICA("loading.AngelicaTweaker", "angelica");

    private final TargetModBuilder builder;

    TargetedMod(String coreModClass, String modId) {
        this.builder = new TargetModBuilder().setCoreModClass(coreModClass).setModId(modId);
    }

    @Override
    @NotNull
    public TargetModBuilder getBuilder() {
        return builder;
    }
}
