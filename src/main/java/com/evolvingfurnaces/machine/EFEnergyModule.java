package com.evolvingfurnaces.machine;

import com.evolvingfurnaces.tier.EFMachineKind;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum EFEnergyModule implements StringRepresentable {
    NONE("none"),
    GENERATOR("generator"),
    ELECTRIC("electric");

    public static final Codec<EFEnergyModule> CODEC = StringRepresentable.fromEnum(EFEnergyModule::values);
    public static final StreamCodec<ByteBuf, EFEnergyModule> STREAM_CODEC = ByteBufCodecs.idMapper(
            value -> values()[value],
            EFEnergyModule::ordinal);

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
