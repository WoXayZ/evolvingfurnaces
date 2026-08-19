package com.evolvingfurnaces.client.gui;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.machine.EFUpgradeHandler;
import com.evolvingfurnaces.network.EFUpgradeMachinePayload;
import com.evolvingfurnaces.tier.EFMachineKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public class EFVanillaBrewingUpgradeButton extends EFSpriteButton {

    private static final ResourceLocation UPGRADE = EFConstants.id("widget/upgrade");
    private static final ResourceLocation UPGRADE_READY = EFConstants.id("widget/upgrade_ready");

    private final BrewingStandScreen screen;

    public EFVanillaBrewingUpgradeButton(BrewingStandScreen screen, BlockPos pos) {
        super(
                screen.getGuiLeft() + screen.getXSize() - 20,
                screen.getGuiTop() - 20,
                EFVanillaBrewingUpgradeButton::icon,
                button -> PacketDistributor.sendToServer(new EFUpgradeMachinePayload(pos)),
                Component.translatable("evolvingfurnaces.gui.upgrade"));
        this.screen = screen;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        setPosition(screen.getGuiLeft() + screen.getXSize() - 20, screen.getGuiTop() - 20);
        setTooltip(net.minecraft.client.gui.components.Tooltip.create(narration()));
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
    }

    private Component narration() {
        if (ready()) {
            return Component.translatable("evolvingfurnaces.gui.upgrade_ready");
        }
        return Component.translatable("evolvingfurnaces.gui.upgrade");
    }

    private static ResourceLocation icon() {
        Player player = Minecraft.getInstance().player;
        if (player != null && EFUpgradeHandler.canUpgradeClient(player, EFMachineKind.BREWING_STAND, 0, 0)) {
            return UPGRADE_READY;
        }
        return UPGRADE;
    }

    private boolean ready() {
        Player player = Minecraft.getInstance().player;
        return player != null && EFUpgradeHandler.canUpgradeClient(player, EFMachineKind.BREWING_STAND, 0, 0);
    }
}
