package com.evolvingfurnaces.client.gui;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.machine.EFMachineTooltips;
import com.evolvingfurnaces.tier.EFMachineKind;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class EFVanillaInfoOverlay {

    static final int INFO_W = 150;
    static final int INFO_H = 116;
    private static final int PANEL_TEX = 256;
    private static final int TEXT = 0x404040;
    private static final ResourceLocation INFO_PANEL = EFConstants.id("textures/gui/info_panel.png");

    private static boolean open;
    private static AbstractContainerScreen<?> screen;
    private static EFMachineKind kind;

    private EFVanillaInfoOverlay() {
    }

    public static void attach(AbstractContainerScreen<?> attached, EFMachineKind attachedKind) {
        screen = attached;
        kind = attachedKind;
    }

    public static void toggle() {
        open = !open;
    }

    public static boolean open() {
        return open;
    }

    public static boolean render(Screen current, GuiGraphics graphics, int mouseX, int mouseY) {
        if (!open || screen == null || current != screen || kind == null) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int x = panelX(screen);
        int y = panelY(screen);
        graphics.blit(INFO_PANEL, x, y, 0.0F, 0.0F, INFO_W, INFO_H, PANEL_TEX, PANEL_TEX);
        Component title = Component.translatable("evolvingfurnaces.gui.info");
        graphics.drawString(font, title, x + (INFO_W - font.width(title)) / 2, y + 5, TEXT, false);
        List<Component> lines = new ArrayList<>();
        EFMachineTooltips.append(lines, kind, 0, 0, minecraft.player);
        int lineY = y + 18;
        for (Component line : lines) {
            final int color;
            if (line.getStyle().getColor() != null
                    && ChatFormatting.GOLD.getColor() != null
                    && line.getStyle().getColor().getValue() == ChatFormatting.GOLD.getColor()) {
                color = 0xB36B00;
            } else {
                color = TEXT;
            }
            graphics.drawString(font, line.copy().withStyle(style -> style.withColor(color)), x + 8, lineY, color, false);
            lineY += 10;
        }
        return true;
    }

    public static boolean consumeClick(Screen current, double mouseX, double mouseY) {
        return open && screen != null && current == screen && inPanel(mouseX, mouseY);
    }

    public static List<Rect2i> extraAreas(AbstractContainerScreen<?> current) {
        List<Rect2i> areas = new ArrayList<>();
        if (screen != current) {
            return areas;
        }
        int buttonX = EFVanillaInfoButton.infoX(current);
        int buttonY = current.getGuiTop() - 20;
        areas.add(new Rect2i(buttonX, buttonY, current.getGuiLeft() + current.getXSize() - buttonX, EFSpriteButton.SIZE));
        if (open) {
            areas.add(new Rect2i(panelX(current), panelY(current), INFO_W, INFO_H));
        }
        return areas;
    }

    private static int panelX(AbstractContainerScreen<?> current) {
        return current.getGuiLeft() + current.getXSize();
    }

    private static int panelY(AbstractContainerScreen<?> current) {
        return current.getGuiTop();
    }

    private static boolean inPanel(double mouseX, double mouseY) {
        int x = panelX(screen);
        int y = panelY(screen);
        return mouseX >= x && mouseX < x + INFO_W && mouseY >= y && mouseY < y + INFO_H;
    }
}
