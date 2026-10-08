package com.evolvingfurnaces.client.gui;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.machine.EFUpgradeHandler;
import com.evolvingfurnaces.network.EFUpgradeMachinePayload;
import com.evolvingfurnaces.tier.EFMachineKind;
import com.evolvingfurnaces.tier.EFTierManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class EFVanillaUpgradeButton extends EFSpriteButton {

    private static final Identifier UPGRADE = EFConstants.id("widget/upgrade");
    private static final Identifier UPGRADE_READY = EFConstants.id("widget/upgrade_ready");

    private final AbstractFurnaceScreen<?> screen;
    private final EFMachineKind kind;

    public EFVanillaUpgradeButton(AbstractFurnaceScreen<?> screen, BlockPos pos, EFMachineKind kind) {
        super(
                screen.getGuiLeft() + screen.getXSize() - 20,
                screen.getGuiTop() - 20,
                () -> icon(kind),
                button -> ClientPacketDistributor.sendToServer(new EFUpgradeMachinePayload(pos)),
                Component.translatable("evolvingfurnaces.gui.upgrade"));
        this.screen = screen;
        this.kind = kind;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        setPosition(screen.getGuiLeft() + screen.getXSize() - 20, screen.getGuiTop() - 20);
        setTooltip(net.minecraft.client.gui.components.Tooltip.create(narration()));
        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }

    private Component narration() {
        if (ready()) {
            return Component.translatable("evolvingfurnaces.gui.upgrade_ready");
        }
        return Component.translatable("evolvingfurnaces.gui.upgrade");
    }

    private static Identifier icon(EFMachineKind kind) {
        Player player = Minecraft.getInstance().player;
        if (player != null && EFUpgradeHandler.canUpgradeClient(player, kind, 0, 0)) {
            return UPGRADE_READY;
        }
        return UPGRADE;
    }

    private boolean ready() {
        Player player = Minecraft.getInstance().player;
        return player != null && EFUpgradeHandler.canUpgradeClient(player, kind, 0, 0);
    }
}
