package com.seibel.distanthorizons;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;
import org.jetbrains.annotations.NotNull;

public enum Mixins implements IMixins {
    CORE(new MixinBuilder()
        .setPhase(Phase.EARLY)
        .addCommonMixins(
            "MixinBiomeGenBase"
        )
    ),
    CLIENT_CORE(new MixinBuilder()
        .setPhase(Phase.EARLY)
        .addClientMixins(
            "MixinActiveRenderInfo",
            "MixinEntityRenderer",
            "MixinMinecraft",
            "MixinNetHandlerPlayClient",
            "MixinOptionsScreen",
            "MixinRenderGlobal",
            "MixinTesselator",
            "MixinTextureAtlasSprite"
        )
    ),

    CLIENT_FADE(new MixinBuilder()
        .setPhase(Phase.EARLY)
        .addExcludedMod(TargetedMod.ANGELICA)
        .addClientMixins(
            "MixinFramebuffer"
        )
    );

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Override
    @NotNull
    public MixinBuilder getBuilder() {
        return builder;
    }
}
