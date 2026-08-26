package com.evolvingfurnaces;

import net.minecraft.resources.ResourceLocation;

public final class EFConstants {

    public static final String MOD_ID = "evolvingfurnaces";

    private EFConstants() {
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    public static ResourceLocation widget(String name) {
        return new ResourceLocation(MOD_ID, "textures/gui/sprites/widget/" + name + ".png");
    }
}
