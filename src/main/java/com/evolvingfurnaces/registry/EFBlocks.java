package com.evolvingfurnaces.registry;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.machine.EvolvingBrewingStandBlock;
import com.evolvingfurnaces.machine.EvolvingMachineBlock;
import com.evolvingfurnaces.tier.EFMachineKind;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EFBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EFConstants.MOD_ID);

    public static final DeferredBlock<EvolvingMachineBlock> FURNACE = BLOCKS.register(
            "furnace",
            () -> new EvolvingMachineBlock(furnaceProperties(), EFMachineKind.FURNACE));
    public static final DeferredBlock<EvolvingMachineBlock> BLAST_FURNACE = BLOCKS.register(
            "blast_furnace",
            () -> new EvolvingMachineBlock(furnaceProperties(), EFMachineKind.BLAST_FURNACE));
    public static final DeferredBlock<EvolvingMachineBlock> SMOKER = BLOCKS.register(
            "smoker",
            () -> new EvolvingMachineBlock(furnaceProperties(), EFMachineKind.SMOKER));
    public static final DeferredBlock<EvolvingBrewingStandBlock> BREWING_STAND = BLOCKS.register(
            "brewing_stand",
            () -> new EvolvingBrewingStandBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BREWING_STAND)));

    private EFBlocks() {
    }

    private static BlockBehaviour.Properties furnaceProperties() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.FURNACE)
                .mapColor(MapColor.STONE)
                .sound(SoundType.STONE);
    }

    public static Block block(EFMachineKind kind) {
        return switch (kind) {
            case FURNACE -> FURNACE.get();
            case BLAST_FURNACE -> BLAST_FURNACE.get();
            case SMOKER -> SMOKER.get();
            case BREWING_STAND -> BREWING_STAND.get();
        };
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
