package com.evolvingfurnaces.compat.recipeviewer;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.client.gui.EFSpriteButton;
import com.evolvingfurnaces.machine.EvolvingMachineItem;
import com.evolvingfurnaces.registry.EFItems;
import com.evolvingfurnaces.tier.EFMachineKind;
import com.evolvingfurnaces.tier.EFMachineTier;
import com.evolvingfurnaces.tier.EFTierManager;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class EFRecipeViewer {

    public static final int WIDTH = 160;
    public static final int HEIGHT = 110;
    public static final int SHIFT_STEP = 10;
    public static final int SLOT = 16;
    public static final int SLOT_STEP = 18;
    public static final int COST_X = 8;
    public static final int COST_Y = 88;
    public static final int MACHINE_X = 132;

    private static final ResourceLocation LEFT = EFConstants.id("widget/page_left");
    private static final ResourceLocation RIGHT = EFConstants.id("widget/page_right");

    private static final Map<EFMachineKind, Integer> INDEX = new EnumMap<>(EFMachineKind.class);

    private EFRecipeViewer() {
    }

    public static Component title(EFMachineKind kind) {
        return Component.translatable("evolvingfurnaces.recipe_viewer.title." + kind.id());
    }

    @Nullable
    public static EFMachineTier current(EFMachineKind kind) {
        List<EFMachineTier> levels = EFTierManager.listClient(kind).levels();
        if (levels.isEmpty()) {
            return null;
        }
        int index = Math.floorMod(INDEX.getOrDefault(kind, 0), levels.size());
        return levels.get(index);
    }

    public static int currentIndex(EFMachineKind kind) {
        List<EFMachineTier> levels = EFTierManager.listClient(kind).levels();
        if (levels.isEmpty()) {
            return 0;
        }
        return Math.floorMod(INDEX.getOrDefault(kind, 0), levels.size());
    }

    public static ItemStack currentStack(EFMachineKind kind) {
        EFMachineTier tier = current(kind);
        if (tier == null) {
            return ItemStack.EMPTY;
        }
        return EvolvingMachineItem.withLevel(EFItems.item(kind), tier.level());
    }

    public static void cycle(EFMachineKind kind, int direction, boolean shift) {
        List<EFMachineTier> levels = EFTierManager.listClient(kind).levels();
        if (levels.isEmpty()) {
            return;
        }
        int step = (shift ? SHIFT_STEP : 1) * Integer.signum(direction);
        if (step == 0) {
            return;
        }
        int index = INDEX.getOrDefault(kind, 0);
        int next = Math.max(0, Math.min(levels.size() - 1, index + step));
        if (next == index) {
            return;
        }
        INDEX.put(kind, next);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    public static int navDirection(double mouseX, double mouseY) {
        if (mouseX >= 8 && mouseX <= 24 && mouseY >= 6 && mouseY <= 22) {
            return -1;
        }
        if (mouseX >= 128 && mouseX <= 144 && mouseY >= 6 && mouseY <= 22) {
            return 1;
        }
        return 0;
    }

    public static boolean clickNav(EFMachineKind kind, double mouseX, double mouseY) {
        int direction = navDirection(mouseX, mouseY);
        if (direction == 0) {
            return false;
        }
        if (direction < 0 && currentIndex(kind) <= 0) {
            return true;
        }
        List<EFMachineTier> levels = EFTierManager.listClient(kind).levels();
        if (direction > 0 && currentIndex(kind) >= levels.size() - 1) {
            return true;
        }
        cycle(kind, direction, Screen.hasShiftDown());
        return true;
    }

    public static void draw(GuiGraphics graphics, Font font, EFMachineKind kind) {
        draw(graphics, font, kind, -1, -1);
    }

    public static void draw(GuiGraphics graphics, Font font, EFMachineKind kind, double mouseX, double mouseY) {
        EFMachineTier tier = current(kind);
        if (tier == null) {
            return;
        }
        List<EFMachineTier> levels = EFTierManager.listClient(kind).levels();
        int index = currentIndex(kind);
        boolean canLeft = index > 0;
        boolean canRight = index < levels.size() - 1;
        boolean leftHover = mouseX >= 8 && mouseX <= 24 && mouseY >= 6 && mouseY <= 22;
        boolean rightHover = mouseX >= 128 && mouseX <= 144 && mouseY >= 6 && mouseY <= 22;
        EFSpriteButton.blit(graphics, 8, 6, LEFT, leftHover, !canLeft, false);
        EFSpriteButton.blit(graphics, 128, 6, RIGHT, rightHover, !canRight, false);
        Component level = Component.translatable("evolvingfurnaces.recipe_viewer.level", tier.level());
        graphics.drawString(font, level, 80 - font.width(level) / 2, 10, 0x404040, false);
        graphics.drawString(font, tier.displayName().toComponent(), 8, 28, 0x404040, false);
        graphics.drawString(font, Component.translatable(kind.timeLangKey(), tier.cookTime(), tier.cookTime() / 20.0), 8, 42, 0x404040, false);
        graphics.drawString(font, Component.translatable(kind.batchLangKey(), tier.batchSize()), 8, 54, 0x404040, false);
        graphics.drawString(font, Component.translatable(kind.progressLangKey(), 0, tier.requiredCooks()), 8, 66, 0x404040, false);
        graphics.drawString(font, Component.translatable("evolvingfurnaces.gui.xp_cost", tier.upgradeCost().experienceLevels()), 8, 78, 0x404040, false);
        int x = COST_X;
        for (var cost : tier.upgradeCost().items()) {
            ItemStack stack = cost.displayStack();
            if (!stack.isEmpty()) {
                blitItem(graphics, font, stack, x, COST_Y, mouseX, mouseY);
                x += SLOT_STEP;
            }
        }
        blitItem(graphics, font, currentStack(kind), MACHINE_X, COST_Y, mouseX, mouseY);
    }

    public static ItemStack hoveredStack(EFMachineKind kind, double mouseX, double mouseY) {
        if (inSlot(mouseX, mouseY, MACHINE_X, COST_Y)) {
            return currentStack(kind);
        }
        EFMachineTier tier = current(kind);
        if (tier == null) {
            return ItemStack.EMPTY;
        }
        int x = COST_X;
        for (var cost : tier.upgradeCost().items()) {
            ItemStack stack = cost.displayStack();
            if (stack.isEmpty()) {
                continue;
            }
            if (inSlot(mouseX, mouseY, x, COST_Y)) {
                return stack;
            }
            x += SLOT_STEP;
        }
        return ItemStack.EMPTY;
    }

    public static List<Component> tooltipLines(ItemStack stack) {
        if (stack.isEmpty()) {
            return List.of();
        }
        return Screen.getTooltipFromItem(Minecraft.getInstance(), stack);
    }

    private static boolean inSlot(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT;
    }

    private static void blitItem(GuiGraphics graphics, Font font, ItemStack stack, int x, int y, double mouseX, double mouseY) {
        if (stack.isEmpty()) {
            return;
        }
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(font, stack, x, y);
        if (inSlot(mouseX, mouseY, x, y)) {
            graphics.fillGradient(RenderType.guiOverlay(), x, y, x + SLOT, y + SLOT, 0x80FFFFFF, 0x80FFFFFF, 0);
        }
    }
}
