package com.evolvingfurnaces.machine;

import com.evolvingfurnaces.registry.EFBlocks;
import com.evolvingfurnaces.tier.EFMachineKind;
import com.evolvingfurnaces.tier.EFMachineTier;
import com.evolvingfurnaces.tier.EFTierManager;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class EFUpgradeHandler {

    private EFUpgradeHandler() {
    }

    public static boolean tryUpgrade(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        if (!level.isLoaded(pos) || player.distanceToSqr(pos.getCenter()) > 64.0D) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        EFMachineKind kind;
        int currentLevel;
        if (blockEntity instanceof EvolvingMachineBlockEntity machine) {
            kind = machine.kind();
            currentLevel = machine.machineLevel();
        } else if (blockEntity instanceof AbstractFurnaceBlockEntity) {
            kind = EFMachineKind.fromVanilla(state.getBlock());
            currentLevel = 0;
        } else {
            return false;
        }
        if (kind == null) {
            return false;
        }
        EFMachineTier next = EFTierManager.next(kind, currentLevel).orElse(null);
        if (next == null) {
            return false;
        }
        boolean creative = player.getAbilities().instabuild;
        if (!creative) {
            if (currentLevel > 0 && blockEntity instanceof EvolvingMachineBlockEntity machine
                    && machine.cooks() < next.requiredCooks()) {
                return false;
            }
            if (!EFPlayerCosts.canAffordXp(player, next.upgradeCost().experienceLevels())) {
                return false;
            }
            if (!EFPlayerCosts.canAffordItems(player, next.upgradeCost().items())) {
                return false;
            }
        }
        EFPlayerCosts.pay(player, next.upgradeCost().items(), next.upgradeCost().experienceLevels());
        if (blockEntity instanceof EvolvingMachineBlockEntity machine) {
            machine.setMachineLevel(next.level());
        } else {
            convertVanilla(level, pos, state, (AbstractFurnaceBlockEntity) blockEntity, kind, next.level(), player.registryAccess());
        }
        closeViewers(level, pos);
        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.8F, 1.15F);
        return true;
    }

    public static boolean canUpgradeClient(Player player, EFMachineKind kind, int currentLevel, int cooks) {
        EFMachineTier next = EFTierManager.nextClient(kind, currentLevel).orElse(null);
        if (next == null) {
            return false;
        }
        if (player.getAbilities().instabuild) {
            return true;
        }
        if (currentLevel > 0 && cooks < next.requiredCooks()) {
            return false;
        }
        if (!EFPlayerCosts.canAffordXp(player, next.upgradeCost().experienceLevels())) {
            return false;
        }
        return EFPlayerCosts.canAffordItems(player, next.upgradeCost().items());
    }

    private static void convertVanilla(
            ServerLevel level,
            BlockPos pos,
            BlockState oldState,
            AbstractFurnaceBlockEntity vanilla,
            EFMachineKind kind,
            int newLevel,
            HolderLookup.Provider registries
    ) {
        CompoundTag tag = vanilla.saveWithoutMetadata(registries);
        ItemStack[] stacks = new ItemStack[vanilla.getContainerSize()];
        for (int i = 0; i < vanilla.getContainerSize(); i++) {
            stacks[i] = vanilla.getItem(i).copy();
            vanilla.setItem(i, ItemStack.EMPTY);
        }
        Block evolving = EFBlocks.block(kind);
        BlockState newState = evolving.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, oldState.getValue(BlockStateProperties.HORIZONTAL_FACING))
                .setValue(BlockStateProperties.LIT, oldState.getValue(BlockStateProperties.LIT));
        level.setBlock(pos, newState, Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof EvolvingMachineBlockEntity machine) {
            machine.loadVanillaData(tag, stacks, newLevel, registries);
        }
    }

    private static void closeViewers(ServerLevel level, BlockPos pos) {
        List<ServerPlayer> players = level.getServer().getPlayerList().getPlayers();
        for (ServerPlayer player : players) {
            AbstractContainerMenu menu = player.containerMenu;
            if (menu instanceof EvolvingMachineMenu evolving && pos.equals(evolving.blockPos())) {
                player.closeContainer();
            } else if (menu instanceof AbstractFurnaceMenu && views(menu, pos)) {
                player.closeContainer();
            }
        }
    }

    private static boolean views(AbstractContainerMenu menu, BlockPos pos) {
        if (menu.getSlot(0).container instanceof BlockEntity blockEntity) {
            return pos.equals(blockEntity.getBlockPos());
        }
        if (menu.getSlot(0).container instanceof Container) {
            return false;
        }
        return false;
    }
}
