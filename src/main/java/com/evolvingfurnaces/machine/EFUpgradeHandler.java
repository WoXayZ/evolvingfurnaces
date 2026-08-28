package com.evolvingfurnaces.machine;

import com.evolvingfurnaces.EFConfig;
import com.evolvingfurnaces.registry.EFBlocks;
import com.evolvingfurnaces.tier.EFMachineKind;
import com.evolvingfurnaces.tier.EFMachineTier;
import com.evolvingfurnaces.tier.EFTierManager;
import com.evolvingfurnaces.tier.EFUpgradeCost;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public final class EFUpgradeHandler {

    private EFUpgradeHandler() {
    }

    public static boolean tryUpgrade(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        if (!level.isLoaded(pos) || player.distanceToSqr(Vec3.atCenterOf(pos)) > 64.0D) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        EFMachineKind kind;
        int currentLevel;
        if (blockEntity instanceof EFIoHost machine) {
            kind = machine.kind();
            currentLevel = machine.machineLevel();
        } else if (blockEntity instanceof AbstractFurnaceBlockEntity) {
            kind = EFMachineKind.fromVanilla(state.getBlock());
            currentLevel = 0;
        } else if (blockEntity instanceof BrewingStandBlockEntity) {
            kind = EFMachineKind.BREWING_STAND;
            currentLevel = 0;
        } else {
            return false;
        }
        if (kind == null || !EFConfig.enabled(kind)) {
            return false;
        }
        EFMachineTier next = EFTierManager.next(kind, currentLevel).orElse(null);
        if (next == null) {
            return false;
        }
        boolean creative = player.getAbilities().instabuild;
        if (!creative) {
            if (currentLevel > 0 && blockEntity instanceof EFIoHost machine
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
        if (blockEntity instanceof EFIoHost machine) {
            machine.setMachineLevel(next.level());
        } else if (blockEntity instanceof AbstractFurnaceBlockEntity furnace) {
            convertVanillaFurnace(level, pos, state, furnace, kind, next.level());
        } else if (blockEntity instanceof BrewingStandBlockEntity brewing) {
            convertVanillaBrewing(level, pos, state, brewing, next.level());
        }
        closeViewers(level, pos);
        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.8F, 1.15F);
        return true;
    }

    public static boolean canUpgradeClient(Player player, EFMachineKind kind, int currentLevel, int cooks) {
        if (!EFConfig.enabled(kind)) {
            return false;
        }
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

    public static boolean tryInstallModule(ServerPlayer player, BlockPos pos, EFEnergyModule module, boolean shift) {
        ServerLevel level = player.serverLevel();
        if (!level.isLoaded(pos) || player.distanceToSqr(Vec3.atCenterOf(pos)) > 64.0D) {
            return false;
        }
        if (!(level.getBlockEntity(pos) instanceof EvolvingMachineBlockEntity machine)) {
            return false;
        }
        if (!module.installed()) {
            if (!shift || !machine.energyModule().installed()) {
                return false;
            }
            machine.setEnergyModule(EFEnergyModule.NONE);
            level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.4F, 0.6F);
            return true;
        }
        if (!EFConfig.moduleAvailable(machine.kind(), module)) {
            return false;
        }
        if (machine.energyModule() == module) {
            return false;
        }
        if (machine.energyModule().installed() && !shift) {
            return false;
        }
        EFUpgradeCost cost = EFTierManager.list(machine.kind()).moduleCost(module);
        boolean creative = player.getAbilities().instabuild;
        if (!creative) {
            if (!EFPlayerCosts.canAffordXp(player, cost.experienceLevels())) {
                return false;
            }
            if (!EFPlayerCosts.canAffordItems(player, cost.items())) {
                return false;
            }
        }
        EFPlayerCosts.pay(player, cost.items(), cost.experienceLevels());
        machine.ejectUnusedSlots(player, module);
        machine.setEnergyModule(module);
        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6F, 1.4F);
        return true;
    }

    public static boolean canInstallModuleClient(Player player, EFMachineKind kind, EFEnergyModule current, EFEnergyModule target) {
        if (!target.installed() || current == target || !EFConfig.enabled(kind) || !EFConfig.moduleAvailable(kind, target)) {
            return false;
        }
        if (player.getAbilities().instabuild) {
            return true;
        }
        EFUpgradeCost cost = EFTierManager.listClient(kind).moduleCost(target);
        if (!EFPlayerCosts.canAffordXp(player, cost.experienceLevels())) {
            return false;
        }
        return EFPlayerCosts.canAffordItems(player, cost.items());
    }

    private static void convertVanillaFurnace(
            ServerLevel level,
            BlockPos pos,
            BlockState oldState,
            AbstractFurnaceBlockEntity vanilla,
            EFMachineKind kind,
            int newLevel
    ) {
        CompoundTag tag = vanilla.saveWithoutMetadata();
        ItemStack[] stacks = copyAndClear(vanilla);
        Block evolving = EFBlocks.block(kind);
        BlockState newState = evolving.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, oldState.getValue(BlockStateProperties.HORIZONTAL_FACING))
                .setValue(BlockStateProperties.LIT, oldState.getValue(BlockStateProperties.LIT));
        level.setBlock(pos, newState, Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof EvolvingMachineBlockEntity machine) {
            machine.loadVanillaData(tag, stacks, newLevel);
        }
    }

    private static void convertVanillaBrewing(
            ServerLevel level,
            BlockPos pos,
            BlockState oldState,
            BrewingStandBlockEntity vanilla,
            int newLevel
    ) {
        CompoundTag tag = vanilla.saveWithoutMetadata();
        ItemStack[] stacks = copyAndClear(vanilla);
        BlockState newState = EFBlocks.BREWING_STAND.get().defaultBlockState();
        for (int i = 0; i < BrewingStandBlock.HAS_BOTTLE.length; i++) {
            if (oldState.hasProperty(BrewingStandBlock.HAS_BOTTLE[i])) {
                newState = newState.setValue(BrewingStandBlock.HAS_BOTTLE[i], oldState.getValue(BrewingStandBlock.HAS_BOTTLE[i]));
            }
        }
        level.setBlock(pos, newState, Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof EvolvingBrewingStandBlockEntity machine) {
            machine.loadVanillaData(tag, stacks, newLevel);
        }
    }

    private static ItemStack[] copyAndClear(Container container) {
        ItemStack[] stacks = new ItemStack[container.getContainerSize()];
        for (int i = 0; i < container.getContainerSize(); i++) {
            stacks[i] = container.getItem(i).copy();
            container.setItem(i, ItemStack.EMPTY);
        }
        return stacks;
    }

    private static void closeViewers(ServerLevel level, BlockPos pos) {
        List<ServerPlayer> players = level.getServer().getPlayerList().getPlayers();
        for (ServerPlayer player : players) {
            AbstractContainerMenu menu = player.containerMenu;
            if (menu instanceof EvolvingMachineMenu evolving && pos.equals(evolving.blockPos())) {
                player.closeContainer();
            } else if (menu instanceof EvolvingBrewingStandMenu brewing && pos.equals(brewing.blockPos())) {
                player.closeContainer();
            } else if (menu instanceof AbstractFurnaceMenu && views(menu, pos)) {
                player.closeContainer();
            } else if (menu instanceof BrewingStandMenu && views(menu, pos)) {
                player.closeContainer();
            }
        }
    }

    private static boolean views(AbstractContainerMenu menu, BlockPos pos) {
        if (menu.getSlot(0).container instanceof BlockEntity blockEntity) {
            return pos.equals(blockEntity.getBlockPos());
        }
        return false;
    }
}
