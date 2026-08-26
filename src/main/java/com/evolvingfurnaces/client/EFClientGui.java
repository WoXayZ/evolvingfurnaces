package com.evolvingfurnaces.client;

import com.evolvingfurnaces.EFConfig;
import com.evolvingfurnaces.client.gui.EFVanillaBrewingUpgradeButton;
import com.evolvingfurnaces.client.gui.EFVanillaInfoButton;
import com.evolvingfurnaces.client.gui.EFVanillaInfoOverlay;
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
import net.minecraftforge.client.event.ScreenEvent;
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
        EFVanillaInfoOverlay.attach(screen, kind);
        event.addListener(new EFVanillaInfoButton(screen));
        event.addListener(new EFVanillaUpgradeButton(screen, pos, kind));
    }

    public static void onScreenRender(ScreenEvent.Render.Post event) {
        EFVanillaInfoOverlay.render(event.getScreen(), event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
    }

    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() == 0 && EFVanillaInfoOverlay.consumeClick(event.getScreen(), event.getMouseX(), event.getMouseY())) {
            event.setCanceled(true);
        }
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
        EFVanillaInfoOverlay.attach(screen, EFMachineKind.BREWING_STAND);
        event.addListener(new EFVanillaInfoButton(screen));
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
        AbstractFurnaceBlockEntity furnace = furnaceOf(screen.getMenu());
        if (furnace != null) {
            return EFMachineKind.fromVanilla(furnace.getBlockState().getBlock());
        }
        return null;
    }

    @Nullable
    private static BlockPos furnacePos(AbstractFurnaceScreen<?> screen) {
        AbstractFurnaceBlockEntity furnace = furnaceOf(screen.getMenu());
        if (furnace != null) {
            return furnace.getBlockPos();
        }
        if (Minecraft.getInstance().hitResult instanceof BlockHitResult hit) {
            return hit.getBlockPos();
        }
        return null;
    }

    @Nullable
    private static BlockPos brewingPos(BrewingStandScreen screen) {
        BrewingStandMenu menu = screen.getMenu();
        for (var slot : menu.slots) {
            if (slot.container instanceof BrewingStandBlockEntity brewing) {
                return brewing.getBlockPos();
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
