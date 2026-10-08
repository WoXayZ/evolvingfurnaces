package com.evolvingfurnaces.registry;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.machine.EvolvingBrewingStandBlockEntity;
import com.evolvingfurnaces.machine.EvolvingMachineBlockEntity;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EFBlockEntities {

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EFConstants.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EvolvingMachineBlockEntity>> MACHINE =
            BLOCK_ENTITIES.register("machine", () -> new BlockEntityType<>(
                    EvolvingMachineBlockEntity::new,
                    Set.of(EFBlocks.FURNACE.get(), EFBlocks.BLAST_FURNACE.get(), EFBlocks.SMOKER.get())
            ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EvolvingBrewingStandBlockEntity>> BREWING_STAND =
            BLOCK_ENTITIES.register("brewing_stand", () -> new BlockEntityType<>(
                    EvolvingBrewingStandBlockEntity::new,
                    Set.of(EFBlocks.BREWING_STAND.get())
            ));

    private EFBlockEntities() {
    }

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
