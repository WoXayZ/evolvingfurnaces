package com.evolvingfurnaces.compat.recipeviewer;

import com.evolvingfurnaces.EFConfig;
import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.client.gui.EFSpriteButton;
import com.evolvingfurnaces.machine.EFEnergyModule;
import com.evolvingfurnaces.machine.EvolvingMachineItem;
import com.evolvingfurnaces.registry.EFItems;
import com.evolvingfurnaces.tier.EFCostIngredient;
import com.evolvingfurnaces.tier.EFMachineKind;
import com.evolvingfurnaces.tier.EFMachineTier;
import com.evolvingfurnaces.tier.EFTierManager;
import com.evolvingfurnaces.tier.EFUpgradeCost;
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
    public static final int HEIGHT = 166;
    public static final int SHIFT_STEP = 10;
    public static final int SLOT = 16;
    public static final int SLOT_STEP = 18;
    public static final int COST_X = 8;
    public static final int COST_Y = 112;
    public static final int MODULE_ITEMS_X = 28;
    public static final int MACHINE_X = 132;

    private static final ResourceLocation LEFT = EFConstants.id("widget/page_left");
    private static final ResourceLocation RIGHT = EFConstants.id("widget/page_right");
    private static final ResourceLocation GENERATOR = EFConstants.id("widget/generator");
    private static final ResourceLocation ELECTRIC = EFConstants.id("widget/electric");

    private static final Map<EFMachineKind, Integer> INDEX = new EnumMap<>(EFMachineKind.class);

    private EFRecipeViewer() {
    }

    public static Component title() {
        return Component.translatable("evolvingfurnaces.recipe_viewer.title");
    }

    public static Component title(EFMachineKind kind) {
        return Component.translatable("evolvingfurnaces.recipe_viewer.title." + kind.id());
    }

    public static Component generatorTitle() {
        return Component.translatable("evolvingfurnaces.recipe_viewer.title.generator");
    }

    public static Component electricTitle() {
        return Component.translatable("evolvingfurnaces.recipe_viewer.title.electric");
    }

    public static ResourceLocation categoryId(EFMachineKind kind) {
        return EFConstants.id(kind.id() + "_upgrades");
    }

    public static ResourceLocation generatorCategoryId() {
        return EFConstants.id("generator_upgrades");
    }

    public static ResourceLocation electricCategoryId() {
        return EFConstants.id("electric_upgrades");
    }

    public static ResourceLocation moduleCategoryId(EFEnergyModule module) {
        return module == EFEnergyModule.ELECTRIC ? electricCategoryId() : generatorCategoryId();
    }

    public static Component moduleTitle(EFEnergyModule module) {
        return module == EFEnergyModule.ELECTRIC ? electricTitle() : generatorTitle();
    }

    public static boolean showGeneratorCategory() {
        return EFConfig.moduleAvailable(EFMachineKind.FURNACE, EFEnergyModule.GENERATOR);
    }

    public static boolean showElectricCategory() {
        if (!EFConfig.electricEnabled()) {
            return false;
        }
        for (EFMachineKind kind : EFMachineKind.values()) {
            if (kind.isCooking() && EFConfig.enabled(kind)) {
                return true;
            }
        }
        return false;
    }

    public static void blitModuleIcon(GuiGraphics graphics, int x, int y, EFEnergyModule module) {
        ResourceLocation icon = module == EFEnergyModule.ELECTRIC ? ELECTRIC : GENERATOR;
        EFSpriteButton.blit(graphics, x, y, icon, false, false, false);
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
        return machineStack(kind, current(kind));
    }

    public static ItemStack machineStack(EFMachineKind kind, @Nullable EFMachineTier tier) {
        if (tier == null) {
            return ItemStack.EMPTY;
        }
        return EvolvingMachineItem.withLevel(EFItems.item(kind), tier.level());
    }

    public static int indexOf(EFMachineKind kind, EFMachineTier tier) {
        List<EFMachineTier> levels = EFTierManager.listClient(kind).levels();
        for (int i = 0; i < levels.size(); i++) {
            if (levels.get(i).level() == tier.level()) {
                return i;
            }
        }
        return 0;
    }

    @Nullable
    public static EFMachineTier neighbor(EFMachineKind kind, EFMachineTier current, int direction, boolean shift) {
        List<EFMachineTier> levels = EFTierManager.listClient(kind).levels();
        if (levels.isEmpty()) {
            return null;
        }
        int step = (shift ? SHIFT_STEP : 1) * Integer.signum(direction);
        if (step == 0) {
            return current;
        }
        int next = Math.max(0, Math.min(levels.size() - 1, indexOf(kind, current) + step));
        return levels.get(next);
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
        draw(graphics, font, kind, current(kind), mouseX, mouseY, true);
    }

    public static void draw(
            GuiGraphics graphics,
            Font font,
            EFMachineKind kind,
            @Nullable EFMachineTier tier,
            double mouseX,
            double mouseY,
            boolean renderItems
    ) {
        if (tier == null) {
            return;
        }
        drawNav(graphics, font, kind, tier, mouseX, mouseY);
        graphics.drawString(font, tier.displayName().toComponent(), 8, 28, 0x404040, false);
        graphics.drawString(font, Component.translatable(kind.timeLangKey(), tier.cookTime(), tier.cookTime() / 20.0), 8, 42, 0x404040, false);
        graphics.drawString(font, Component.translatable(kind.batchLangKey(), tier.batchSize()), 8, 54, 0x404040, false);
        graphics.drawString(font, Component.translatable(kind.progressLangKey(), 0, tier.requiredCooks()), 8, 66, 0x404040, false);
        graphics.drawString(font, Component.translatable("evolvingfurnaces.gui.xp_cost", tier.upgradeCost().experienceLevels()), 8, 78, 0x404040, false);
        if (renderItems) {
            blitCosts(graphics, font, tier.upgradeCost().items(), COST_X, COST_Y, mouseX, mouseY);
            blitItem(graphics, font, machineStack(kind, tier), MACHINE_X, COST_Y, mouseX, mouseY);
        }
    }

    public static void drawModule(GuiGraphics graphics, Font font, EFEnergyModule module, double mouseX, double mouseY) {
        drawModule(graphics, font, module, current(EFMachineKind.FURNACE), mouseX, mouseY, true);
    }

    public static void drawModule(
            GuiGraphics graphics,
            Font font,
            EFEnergyModule module,
            @Nullable EFMachineTier tier,
            double mouseX,
            double mouseY,
            boolean renderItems
    ) {
        EFMachineKind kind = EFMachineKind.FURNACE;
        if (tier == null || !module.installed()) {
            return;
        }
        drawNav(graphics, font, kind, tier, mouseX, mouseY);
        graphics.drawString(font, tier.displayName().toComponent(), 8, 28, 0x404040, false);
        if (module == EFEnergyModule.GENERATOR) {
            graphics.drawString(
                    font,
                    Component.translatable("evolvingfurnaces.gui.energy_generate_short", tier.generatorOutput()),
                    8,
                    42,
                    0x404040,
                    false);
        } else {
            graphics.drawString(
                    font,
                    Component.translatable("evolvingfurnaces.gui.energy_consume_short", tier.energyConsume()),
                    8,
                    42,
                    0x404040,
                    false);
        }
        graphics.drawString(
                font,
                Component.translatable("evolvingfurnaces.gui.energy_capacity", tier.energyCapacity()),
                8,
                54,
                0x404040,
                false);
        var list = EFTierManager.listClient(kind);
        EFUpgradeCost cost = module == EFEnergyModule.GENERATOR ? list.generatorCost() : list.electricCost();
        blitModuleRow(graphics, font, module, cost, COST_Y, mouseX, mouseY, renderItems);
        if (renderItems) {
            blitItem(graphics, font, machineStack(kind, tier), MACHINE_X, COST_Y, mouseX, mouseY);
        }
    }

    private static void drawNav(
            GuiGraphics graphics,
            Font font,
            EFMachineKind kind,
            EFMachineTier tier,
            double mouseX,
            double mouseY
    ) {
        List<EFMachineTier> levels = EFTierManager.listClient(kind).levels();
        int index = indexOf(kind, tier);
        boolean canLeft = index > 0;
        boolean canRight = index < levels.size() - 1;
        boolean leftHover = mouseX >= 8 && mouseX <= 24 && mouseY >= 6 && mouseY <= 22;
        boolean rightHover = mouseX >= 128 && mouseX <= 144 && mouseY >= 6 && mouseY <= 22;
        EFSpriteButton.blit(graphics, 8, 6, LEFT, leftHover, !canLeft, false);
        EFSpriteButton.blit(graphics, 128, 6, RIGHT, rightHover, !canRight, false);
        Component level = Component.translatable("evolvingfurnaces.recipe_viewer.level", tier.level());
        graphics.drawString(font, level, 80 - font.width(level) / 2, 10, 0x404040, false);
    }

    private static void blitModuleRow(
            GuiGraphics graphics,
            Font font,
            EFEnergyModule module,
            EFUpgradeCost cost,
            int y,
            double mouseX,
            double mouseY,
            boolean renderItems
    ) {
        ResourceLocation icon = module == EFEnergyModule.GENERATOR ? GENERATOR : ELECTRIC;
        boolean hover = inSlot(mouseX, mouseY, COST_X, y);
        EFSpriteButton.blit(graphics, COST_X, y, icon, hover, false, false);
        if (renderItems) {
            blitCosts(graphics, font, cost.items(), MODULE_ITEMS_X, y, mouseX, mouseY);
        }
        graphics.drawString(
                font,
                Component.translatable("evolvingfurnaces.gui.xp_short", cost.experienceLevels()),
                90,
                y + 4,
                0x404040,
                false);
    }

    public static ItemStack hoveredStack(EFMachineKind kind, double mouseX, double mouseY) {
        if (inSlot(mouseX, mouseY, MACHINE_X, COST_Y)) {
            return currentStack(kind);
        }
        EFMachineTier tier = current(kind);
        if (tier == null) {
            return ItemStack.EMPTY;
        }
        ItemStack hovered = hoveredCost(tier.upgradeCost().items(), COST_X, COST_Y, mouseX, mouseY);
        if (!hovered.isEmpty()) {
            return hovered;
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack hoveredModuleStack(EFEnergyModule module, double mouseX, double mouseY) {
        EFMachineKind kind = EFMachineKind.FURNACE;
        if (inSlot(mouseX, mouseY, MACHINE_X, COST_Y)) {
            return currentStack(kind);
        }
        if (hoveredModuleButton(module, mouseX, mouseY) != null) {
            return ItemStack.EMPTY;
        }
        var list = EFTierManager.listClient(kind);
        EFUpgradeCost cost = module == EFEnergyModule.GENERATOR ? list.generatorCost() : list.electricCost();
        return hoveredCost(cost.items(), MODULE_ITEMS_X, COST_Y, mouseX, mouseY);
    }

    @Nullable
    public static Component hoveredModuleButton(EFEnergyModule module, double mouseX, double mouseY) {
        if (module.installed() && inSlot(mouseX, mouseY, COST_X, COST_Y)) {
            return Component.translatable("evolvingfurnaces.gui.module." + module.getSerializedName());
        }
        return null;
    }

    public static List<Component> hoveredModuleTooltip(EFEnergyModule module, double mouseX, double mouseY) {
        Component name = hoveredModuleButton(module, mouseX, mouseY);
        if (name != null) {
            return List.of(name);
        }
        return tooltipLines(hoveredModuleStack(module, mouseX, mouseY));
    }

    public static List<Component> tooltipLines(ItemStack stack) {
        if (stack.isEmpty()) {
            return List.of();
        }
        return Screen.getTooltipFromItem(Minecraft.getInstance(), stack);
    }

    private static void blitCosts(
            GuiGraphics graphics,
            Font font,
            List<EFCostIngredient> costs,
            int x,
            int y,
            double mouseX,
            double mouseY
    ) {
        int slotX = x;
        for (var cost : costs) {
            ItemStack stack = cost.displayStack();
            if (stack.isEmpty()) {
                continue;
            }
            blitItem(graphics, font, stack, slotX, y, mouseX, mouseY);
            slotX += SLOT_STEP;
        }
    }

    private static ItemStack hoveredCost(
            List<EFCostIngredient> costs,
            int x,
            int y,
            double mouseX,
            double mouseY
    ) {
        int slotX = x;
        for (var cost : costs) {
            ItemStack stack = cost.displayStack();
            if (stack.isEmpty()) {
                continue;
            }
            if (inSlot(mouseX, mouseY, slotX, y)) {
                return stack;
            }
            slotX += SLOT_STEP;
        }
        return ItemStack.EMPTY;
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
