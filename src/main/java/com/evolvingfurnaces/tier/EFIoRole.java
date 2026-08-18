package com.evolvingfurnaces.tier;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum EFIoRole implements StringRepresentable {
    INPUT("input"),
    OUTPUT("output"),
    FUEL("fuel"),
    DISABLED("disabled");

    public static final Codec<EFIoRole> CODEC = StringRepresentable.fromEnum(EFIoRole::values);

    private final String name;

    EFIoRole(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public EFIoRole next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
