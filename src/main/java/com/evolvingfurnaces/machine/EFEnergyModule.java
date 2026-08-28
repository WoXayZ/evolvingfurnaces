package com.evolvingfurnaces.machine;

import com.evolvingfurnaces.tier.EFMachineKind;
import net.minecraft.util.StringRepresentable;

public enum EFEnergyModule implements StringRepresentable {
    NONE("none"),
    GENERATOR("generator"),
    ELECTRIC("electric");

    private final String name;

    EFEnergyModule(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public boolean installed() {
        return this != NONE;
    }

    public boolean supportedOn(EFMachineKind kind) {
        return switch (this) {
            case NONE -> true;
            case GENERATOR -> kind == EFMachineKind.FURNACE;
            case ELECTRIC -> kind.isCooking();
        };
    }
}
