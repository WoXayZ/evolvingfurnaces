package com.evolvingfurnaces;

import net.minecraft.resources.ResourceLocation;

public final class EFConstants {

    public static final String MOD_ID = "evolvingfurnaces";

    private EFConstants() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
