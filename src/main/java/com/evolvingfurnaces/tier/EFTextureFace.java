package com.evolvingfurnaces.tier;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum EFTextureFace implements StringRepresentable {
    TOP("top"),
    BOTTOM("bottom"),
    NORTH("north"),
    SOUTH("south"),
    EAST("east"),
    WEST("west"),
    SIDE("side");

    public static final Codec<EFTextureFace> CODEC = StringRepresentable.fromEnum(EFTextureFace::values);

    private final String name;

    EFTextureFace(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
