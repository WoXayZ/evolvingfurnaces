package com.evolvingfurnaces.client;

import com.evolvingfurnaces.client.gui.EFVanillaUpgradeButton;
import com.evolvingfurnaces.client.gui.EvolvingMachineScreen;
import com.evolvingfurnaces.tier.EFMachineKind;
import com.evolvingfurnaces.tier.EFTierManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.SmokerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jetbrains.annotations.Nullable;

public final class EFClientGui {

    private EFClientGui() {
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof AbstractFurnaceScreen<?> screen) || screen instanceof EvolvingMachineScreen) {
            return;
        }
        EFMachineKind kind = kindOf(screen);
        if (kind == null || EFTierManager.nextClient(kind, 0).isEmpty()) {
            return;
        }
        BlockPos pos = furnacePos(screen);
        if (pos == null) {
            return;
        }
        event.addListener(new EFVanillaUpgradeButton(screen, pos, kind));
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
    private static AbstractFurnaceBlockEntity furnaceOf(AbstractFurnaceMenu menu) {
        for (var slot : menu.slots) {
            if (slot.container instanceof AbstractFurnaceBlockEntity furnace) {
                return furnace;
            }
        }
        return null;
    }
}
