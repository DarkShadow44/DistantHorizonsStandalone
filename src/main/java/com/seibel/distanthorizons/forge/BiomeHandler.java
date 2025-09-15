package com.seibel.distanthorizons.forge;

import net.minecraft.world.biome.BiomeGenBase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BiomeHandler {
    private static final List<BiomeGenBase> biomes = new ArrayList<>();
    private static HashMap<String, BiomeGenBase> biomeMap;

    public static void addBiome(BiomeGenBase biome) {
        biomes.add(biome);
    }

    public static BiomeGenBase getBiomeByName(String name) {
        if (biomeMap == null) {
            biomeMap = new HashMap<>();
            for (var biome : biomes) {
                biomeMap.put(biome.biomeName, biome);
            }
        }
        return biomeMap.get(name);
    }
}
