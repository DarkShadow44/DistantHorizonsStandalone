package com.seibel.distanthorizons;

/**
 * Simple flags class with no dependencies, safe to load from any thread
 * including the FML splash thread.
 */
public class MixinFlags {
    public static volatile boolean framebufferMixinEnabled = false;
}
