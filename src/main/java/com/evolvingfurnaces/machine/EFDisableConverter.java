package com.evolvingfurnaces.machine;

import com.evolvingfurnaces.EFConfig;
import com.evolvingfurnaces.registry.EFBlocks;
import com.evolvingfurnaces.tier.EFMachineKind;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class EFDisableConverter {

    private EFDisableConverter() {
    }

    public static boolean convertIfDisabled(ServerLevel level, BlockPos pos, EFIoHost machine) {
        if (EFConfig.enabled(machine.kind())) {
            return false;
        }
        if (machine instanceof EvolvingMachineBlockEntity cooking) {
            convertCooking(level, pos, cooking);
            return true;
        }
        if (machine instanceof EvolvingBrewingStandBlockEntity brewing) {
            convertBrewing(level, pos, brewing);
            return true;
        }
        return false;
    }

    private static void convertCooking(ServerLevel level, BlockPos pos, EvolvingMachineBlockEntity machine) {
        CompoundTag tag = machine.saveWithoutMetadata();
        ItemStack[] stacks = copyAndClear(machine);
        machine.markConvertingToVanilla();
        BlockState oldState = machine.getBlockState();
        BlockState newState = machine.kind().vanillaBlock().defaultBlockState();
        if (oldState.hasProperty(BlockStateProperties.HORIZONTAL_FACING) && newState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            newState = newState.setValue(BlockStateProperties.HORIZONTAL_FACING, oldState.getValue(BlockStateProperties.HORIZONTAL_FACING));
        }
        if (oldState.hasProperty(BlockStateProperties.LIT) && newState.hasProperty(BlockStateProperties.LIT)) {
            newState = newState.setValue(BlockStateProperties.LIT, oldState.getValue(BlockStateProperties.LIT));
        }
        level.setBlock(pos, newState, Block.UPDATE_ALL);
        BlockEntity created = level.getBlockEntity(pos);
        if (created instanceof AbstractFurnaceBlockEntity vanilla) {
            vanilla.load(tag);
            for (int i = 0; i < Math.min(stacks.length, vanilla.getContainerSize()); i++) {
                vanilla.setItem(i, stacks[i]);
            }
        }
    }

    private static void convertBrewing(ServerLevel level, BlockPos pos, EvolvingBrewingStandBlockEntity machine) {
        CompoundTag tag = machine.saveWithoutMetadata();
        ItemStack[] stacks = copyAndClear(machine);
        machine.markConvertingToVanilla();
        BlockState oldState = machine.getBlockState();
        BlockState newState = machine.kind().vanillaBlock().defaultBlockState();
        for (int i = 0; i < BrewingStandBlock.HAS_BOTTLE.length; i++) {
            if (oldState.hasProperty(BrewingStandBlock.HAS_BOTTLE[i])) {
                newState = newState.setValue(BrewingStandBlock.HAS_BOTTLE[i], oldState.getValue(BrewingStandBlock.HAS_BOTTLE[i]));
            }
        }
        level.setBlock(pos, newState, Block.UPDATE_ALL);
        BlockEntity created = level.getBlockEntity(pos);
        if (created instanceof BrewingStandBlockEntity vanilla) {
            vanilla.load(tag);
            for (int i = 0; i < Math.min(stacks.length, vanilla.getContainerSize()); i++) {
                ItemStack stack = stacks[i];
                if (i < 3 && stack.getCount() > stack.getMaxStackSize()) {
                    int extra = stack.getCount() - stack.getMaxStackSize();
                    stack.setCount(stack.getMaxStackSize());
                    while (extra > 0) {
                        int drop = Math.min(extra, Math.max(1, stack.getMaxStackSize()));
                        net.minecraft.world.Containers.dropItemStack(
                                level, pos.getX(), pos.getY(), pos.getZ(), EFStacks.copyWithCount(stack, drop));
                        extra -= drop;
                    }
                }
                vanilla.setItem(i, stack);
            }
        }
    }

    private static ItemStack[] copyAndClear(net.minecraft.world.Container container) {
        ItemStack[] stacks = new ItemStack[container.getContainerSize()];
        for (int i = 0; i < container.getContainerSize(); i++) {
            stacks[i] = container.getItem(i).copy();
            container.setItem(i, ItemStack.EMPTY);
        }
        return stacks;
    }

    public static boolean isOurMachine(Block block) {
        return block == EFBlocks.FURNACE.get()
                || block == EFBlocks.BLAST_FURNACE.get()
                || block == EFBlocks.SMOKER.get()
                || block == EFBlocks.BREWING_STAND.get();
    }

    public static EFMachineKind kindOf(Block block) {
        if (block instanceof EvolvingMachineBlock machine) {
            return machine.kind();
        }
        if (block instanceof EvolvingBrewingStandBlock) {
            return EFMachineKind.BREWING_STAND;
        }
        return EFMachineKind.fromVanilla(block);
    }
}
