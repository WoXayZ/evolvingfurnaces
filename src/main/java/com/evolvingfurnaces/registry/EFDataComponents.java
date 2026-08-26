package com.evolvingfurnaces.registry;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.machine.EFEnergyModule;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EFDataComponents {

    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, EFConstants.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> MACHINE_LEVEL =
            COMPONENTS.registerComponentType("machine_level", builder -> builder
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EFEnergyModule>> ENERGY_MODULE =
            COMPONENTS.registerComponentType("energy_module", builder -> builder
                    .persistent(EFEnergyModule.CODEC)
                    .networkSynchronized(EFEnergyModule.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY_STORED =
            COMPONENTS.registerComponentType("energy_stored", builder -> builder
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    private EFDataComponents() {
    }

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }
}
