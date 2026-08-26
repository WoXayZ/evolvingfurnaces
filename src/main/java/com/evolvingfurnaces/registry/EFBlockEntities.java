package com.evolvingfurnaces.registry;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.machine.EvolvingBrewingStandBlockEntity;
import com.evolvingfurnaces.machine.EvolvingMachineBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class EFBlockEntities {

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, EFConstants.MOD_ID);

    public static final RegistryObject<BlockEntityType<EvolvingMachineBlockEntity>> MACHINE =
            BLOCK_ENTITIES.register("machine", () -> BlockEntityType.Builder.of(
                    EvolvingMachineBlockEntity::new,
                    EFBlocks.FURNACE.get(),
                    EFBlocks.BLAST_FURNACE.get(),
                    EFBlocks.SMOKER.get()
            ).build(null));

    public static final RegistryObject<BlockEntityType<EvolvingBrewingStandBlockEntity>> BREWING_STAND =
            BLOCK_ENTITIES.register("brewing_stand", () -> BlockEntityType.Builder.of(
                    EvolvingBrewingStandBlockEntity::new,
                    EFBlocks.BREWING_STAND.get()
            ).build(null));

    private EFBlockEntities() {
    }

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
