package com.evolvingfurnaces.client.gui;

import com.evolvingfurnaces.EFConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class EFVanillaInfoButton extends EFSpriteButton {

    private static final Identifier INFO = EFConstants.id("widget/info");

    private final AbstractContainerScreen<?> screen;

    public EFVanillaInfoButton(AbstractContainerScreen<?> screen) {
        super(
                infoX(screen),
                screen.getGuiTop() - 20,
                () -> INFO,
                button -> EFVanillaInfoOverlay.toggle(),
                Component.translatable("evolvingfurnaces.gui.info"));
        this.screen = screen;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        setPosition(infoX(screen), screen.getGuiTop() - 20);
        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }

    static int infoX(AbstractContainerScreen<?> screen) {
        return screen.getGuiLeft() + screen.getXSize() - 40;
    }
}
