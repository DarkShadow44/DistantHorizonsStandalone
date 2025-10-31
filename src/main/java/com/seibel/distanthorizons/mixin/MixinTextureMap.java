package com.seibel.distanthorizons.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.seibel.distanthorizons.interfaces.IMixinTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TextureMap.class)
public class MixinTextureMap {
    @WrapOperation(method = "loadTextureAtlas", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;load(Lnet/minecraft/client/resources/IResourceManager;Lnet/minecraft/util/ResourceLocation;)Z", remap = false))
    private boolean load(TextureAtlasSprite instance, IResourceManager manager, ResourceLocation location, Operation<Boolean> original)
    {
        boolean ret = original.call(instance, manager, location);
        IMixinTextureAtlasSprite mixinSprite = (IMixinTextureAtlasSprite) instance;
        mixinSprite.distanthorizons$loadData();
        return ret;
    }
}
