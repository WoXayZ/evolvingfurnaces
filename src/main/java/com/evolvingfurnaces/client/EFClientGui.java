package com.evolvingfurnaces.client;

import com.evolvingfurnaces.EFConfig;
import com.evolvingfurnaces.client.gui.EFVanillaBrewingUpgradeButton;
import com.evolvingfurnaces.client.gui.EFVanillaUpgradeButton;
import com.evolvingfurnaces.client.gui.EvolvingMachineScreen;
import com.evolvingfurnaces.tier.EFMachineKind;
import com.evolvingfurnaces.tier.EFTierManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.SmokerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jetbrains.annotations.Nullable;

public final class EFClientGui {

    private EFClientGui() {
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof BrewingStandScreen brewing) {
            addBrewingButton(event, brewing);
            return;
        }
        if (!(event.getScreen() instanceof AbstractFurnaceScreen<?> screen) || screen instanceof EvolvingMachineScreen) {
            return;
        }
        EFMachineKind kind = kindOf(screen);
        if (kind == null || !EFConfig.enabled(kind) || EFTierManager.nextClient(kind, 0).isEmpty()) {
            return;
        }
        BlockPos pos = furnacePos(screen);
        if (pos == null) {
            return;
        }
        event.addListener(new EFVanillaUpgradeButton(screen, pos, kind));
    }

    private static void addBrewingButton(ScreenEvent.Init.Post event, BrewingStandScreen screen) {
        if (!EFConfig.enabled(EFMachineKind.BREWING_STAND)
                || EFTierManager.nextClient(EFMachineKind.BREWING_STAND, 0).isEmpty()) {
            return;
        }
        BlockPos pos = brewingPos(screen);
        if (pos == null) {
            return;
        }
        event.addListener(new EFVanillaBrewingUpgradeButton(screen, pos));
    }

    @Nullable
    private static EFMachineKind kindOf(AbstractFurnaceScreen<?> screen) {
        if (screen instanceof FurnaceScreen) {
            return EFMachineKind.FURNACE;
        }
        if (screen instanceof BlastFurnaceScreen) {
            return EFMachineKind.BLAST_FURNACE;
        }
        if (screen instanceof SmokerScreen) {
            return EFMachineKind.SMOKER;
        }
        if (screen.getMenu() instanceof AbstractFurnaceMenu menu) {
            AbstractFurnaceBlockEntity furnace = furnaceOf(menu);
            if (furnace != null) {
                return EFMachineKind.fromVanilla(furnace.getBlockState().getBlock());
            }
        }
        return null;
    }

    @Nullable
    private static BlockPos furnacePos(AbstractFurnaceScreen<?> screen) {
        if (screen.getMenu() instanceof AbstractFurnaceMenu menu) {
            AbstractFurnaceBlockEntity furnace = furnaceOf(menu);
            if (furnace != null) {
                return furnace.getBlockPos();
            }
        }
        if (Minecraft.getInstance().hitResult instanceof BlockHitResult hit) {
            return hit.getBlockPos();
        }
        return null;
    }

    @Nullable
    private static BlockPos brewingPos(BrewingStandScreen screen) {
        if (screen.getMenu() instanceof BrewingStandMenu menu) {
            for (var slot : menu.slots) {
                if (slot.container instanceof BrewingStandBlockEntity brewing) {
                    return brewing.getBlockPos();
                }
            }
        }
        if (Minecraft.getInstance().hitResult instanceof BlockHitResult hit) {
            return hit.getBlockPos();
        }
        return null;
    }

    @Nullable
    private static AbstractFurnaceBlockEntity furnaceOf(AbstractFurnaceMenu menu) {
        for (var slot : menu.slots) {
            if (slot.container instanceof AbstractFurnaceBlockEntity furnace) {
                return furnace;
            }
        }
        return null;
    }
}
