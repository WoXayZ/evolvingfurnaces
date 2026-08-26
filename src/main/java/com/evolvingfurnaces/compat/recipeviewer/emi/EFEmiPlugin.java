package com.evolvingfurnaces.compat.recipeviewer.emi;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.EFConfig;
import com.evolvingfurnaces.client.gui.EFVanillaInfoOverlay;
import com.evolvingfurnaces.client.gui.EvolvingBrewingStandScreen;
import com.evolvingfurnaces.client.gui.EvolvingMachineScreen;
import com.evolvingfurnaces.compat.recipeviewer.EFRecipeViewer;
import com.evolvingfurnaces.machine.EFEnergyModule;
import com.evolvingfurnaces.machine.EvolvingMachineItem;
import com.evolvingfurnaces.registry.EFItems;
import com.evolvingfurnaces.tier.EFMachineKind;
import com.evolvingfurnaces.tier.EFMachineTier;
import com.evolvingfurnaces.tier.EFTierManager;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.SmokerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@EmiEntrypoint
public final class EFEmiPlugin implements EmiPlugin {

    public EFEmiPlugin() {
    }

    @Override
    public void register(EmiRegistry registry) {
        for (EFMachineKind kind : EFMachineKind.values()) {
            if (!EFConfig.enabled(kind)) {
                continue;
            }
            EmiRecipeCategory category = new EmiRecipeCategory(
                    EFRecipeViewer.categoryId(kind),
                    EmiStack.of(EvolvingMachineItem.withLevel(EFItems.item(kind), 1)));
            registry.addCategory(category);
            for (EFMachineTier tier : EFTierManager.listClient(kind).levels()) {
                registry.addWorkstation(category, EmiStack.of(EvolvingMachineItem.withLevel(EFItems.item(kind), tier.level())));
            }
            registry.addRecipe(new EFEmiTierRecipe(kind, category, EFEnergyModule.NONE));
        }
        addModuleCategory(registry, EFEnergyModule.GENERATOR);
        addModuleCategory(registry, EFEnergyModule.ELECTRIC);
        registry.addExclusionArea(EvolvingMachineScreen.class, (screen, consumer) -> {
            for (Rect2i area : screen.guiExtraAreas()) {
                consumer.accept(new Bounds(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
            }
        });
        registry.addExclusionArea(EvolvingBrewingStandScreen.class, (screen, consumer) -> {
            for (Rect2i area : screen.guiExtraAreas()) {
                consumer.accept(new Bounds(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
            }
        });
        registry.addExclusionArea(FurnaceScreen.class, EFEmiPlugin::vanillaExclusion);
        registry.addExclusionArea(BlastFurnaceScreen.class, EFEmiPlugin::vanillaExclusion);
        registry.addExclusionArea(SmokerScreen.class, EFEmiPlugin::vanillaExclusion);
        registry.addExclusionArea(BrewingStandScreen.class, EFEmiPlugin::vanillaExclusion);
    }

    private static void addModuleCategory(EmiRegistry registry, EFEnergyModule module) {
        boolean show = module == EFEnergyModule.GENERATOR
                ? EFRecipeViewer.showGeneratorCategory()
                : EFRecipeViewer.showElectricCategory();
        if (!show) {
            return;
        }
        EmiRecipeCategory category = new EmiRecipeCategory(
                EFRecipeViewer.moduleCategoryId(module),
                (graphics, x, y, delta) -> EFRecipeViewer.blitModuleIcon(graphics, x, y, module));
        registry.addCategory(category);
        for (EFMachineKind kind : EFMachineKind.values()) {
            if (!EFConfig.enabled(kind)) {
                continue;
            }
            if (module == EFEnergyModule.GENERATOR && kind != EFMachineKind.FURNACE) {
                continue;
            }
            if (module == EFEnergyModule.ELECTRIC && !kind.isCooking()) {
                continue;
            }
            for (EFMachineTier tier : EFTierManager.listClient(kind).levels()) {
                registry.addWorkstation(
                        category,
                        EmiStack.of(EvolvingMachineItem.withLevel(EFItems.item(kind), tier.level())));
            }
        }
        registry.addRecipe(new EFEmiTierRecipe(EFMachineKind.FURNACE, category, module));
    }

    private static void vanillaExclusion(AbstractContainerScreen<?> screen, Consumer<Bounds> consumer) {
        for (Rect2i area : EFVanillaInfoOverlay.extraAreas(screen)) {
            consumer.accept(new Bounds(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
        }
    }

    private record EFEmiTierRecipe(EFMachineKind kind, EmiRecipeCategory category, EFEnergyModule module) implements EmiRecipe {
        @Override
        public EmiRecipeCategory getCategory() {
            return category;
        }

        @Override
        public ResourceLocation getId() {
            return module.installed()
                    ? EFConstants.id("recipe_viewer/" + module.getSerializedName())
                    : EFConstants.id("recipe_viewer/" + kind.id());
        }

        @Override
        public List<EmiIngredient> getInputs() {
            if (module.installed()) {
                java.util.ArrayList<EmiIngredient> inputs = new java.util.ArrayList<>();
                var list = EFTierManager.listClient(kind);
                var cost = module == EFEnergyModule.GENERATOR ? list.generatorCost() : list.electricCost();
                for (var item : cost.items()) {
                    inputs.add(EmiStack.of(item.displayStack()));
                }
                return inputs;
            }
            EFMachineTier tier = EFRecipeViewer.current(kind);
            if (tier == null) {
                return List.of();
            }
            java.util.ArrayList<EmiIngredient> inputs = new java.util.ArrayList<>();
            for (var cost : tier.upgradeCost().items()) {
                inputs.add(EmiStack.of(cost.displayStack()));
            }
            return inputs;
        }

        @Override
        public List<EmiStack> getOutputs() {
            return List.of(EmiStack.of(EFRecipeViewer.currentStack(kind)));
        }

        @Override
        public int getDisplayWidth() {
            return EFRecipeViewer.WIDTH;
        }

        @Override
        public int getDisplayHeight() {
            return EFRecipeViewer.HEIGHT;
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.add(new ViewerWidget(kind, module));
        }
    }

    private static final class ViewerWidget extends Widget {
        private final EFMachineKind kind;
        private final EFEnergyModule module;
        private final Bounds bounds = new Bounds(0, 0, EFRecipeViewer.WIDTH, EFRecipeViewer.HEIGHT);

        private ViewerWidget(EFMachineKind kind, EFEnergyModule module) {
            this.kind = kind;
            this.module = module;
        }

        @Override
        public Bounds getBounds() {
            return bounds;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            if (module.installed()) {
                EFRecipeViewer.drawModule(graphics, Minecraft.getInstance().font, module, mouseX, mouseY);
            } else {
                EFRecipeViewer.draw(graphics, Minecraft.getInstance().font, kind, mouseX, mouseY);
            }
        }

        @Override
        public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
            List<net.minecraft.network.chat.Component> lines = module.installed()
                    ? EFRecipeViewer.hoveredModuleTooltip(module, mouseX, mouseY)
                    : EFRecipeViewer.tooltipLines(EFRecipeViewer.hoveredStack(kind, mouseX, mouseY));
            if (lines.isEmpty()) {
                return List.of();
            }
            return lines.stream()
                    .map(line -> ClientTooltipComponent.create(line.getVisualOrderText()))
                    .toList();
        }

        @Override
        public boolean mouseClicked(int mouseX, int mouseY, int button) {
            if (button != 0) {
                return false;
            }
            return EFRecipeViewer.clickNav(kind, mouseX, mouseY);
        }
    }
}
