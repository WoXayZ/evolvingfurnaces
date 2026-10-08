package com.evolvingfurnaces;

import net.minecraft.resources.Identifier;

public final class EFConstants {

    public static final String MOD_ID = "evolvingfurnaces";

    private EFConstants() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
