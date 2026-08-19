package com.evolvingfurnaces.compat.recipeviewer.emi;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.EFConfig;
import com.evolvingfurnaces.client.gui.EvolvingBrewingStandScreen;
import com.evolvingfurnaces.client.gui.EvolvingMachineScreen;
import com.evolvingfurnaces.compat.recipeviewer.EFRecipeViewer;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
                    EFConstants.id("recipe_viewer/" + kind.id()),
                    EmiStack.of(EvolvingMachineItem.withLevel(EFItems.item(kind), 1)));
            registry.addCategory(category);
            for (EFMachineTier tier : EFTierManager.listClient(kind).levels()) {
                registry.addWorkstation(category, EmiStack.of(EvolvingMachineItem.withLevel(EFItems.item(kind), tier.level())));
            }
            registry.addRecipe(new EFEmiTierRecipe(kind, category));
        }
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
    }

    private record EFEmiTierRecipe(EFMachineKind kind, EmiRecipeCategory category) implements EmiRecipe {
        @Override
        public EmiRecipeCategory getCategory() {
            return category;
        }

        @Override
        public ResourceLocation getId() {
            return EFConstants.id("recipe_viewer/" + kind.id());
        }

        @Override
        public List<EmiIngredient> getInputs() {
            EFMachineTier tier = EFRecipeViewer.current(kind);
            if (tier == null) {
                return List.of();
            }
            return tier.upgradeCost().items().stream()
                    .<EmiIngredient>map(cost -> EmiStack.of(cost.displayStack()))
                    .toList();
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
            widgets.add(new ViewerWidget(kind));
        }
    }

    private static final class ViewerWidget extends Widget {
        private final EFMachineKind kind;
        private final Bounds bounds = new Bounds(0, 0, EFRecipeViewer.WIDTH, EFRecipeViewer.HEIGHT);

        private ViewerWidget(EFMachineKind kind) {
            this.kind = kind;
        }

        @Override
        public Bounds getBounds() {
            return bounds;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            EFRecipeViewer.draw(graphics, Minecraft.getInstance().font, kind, mouseX, mouseY);
        }

        @Override
        public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
            ItemStack stack = EFRecipeViewer.hoveredStack(kind, mouseX, mouseY);
            if (stack.isEmpty()) {
                return List.of();
            }
            return EFRecipeViewer.tooltipLines(stack).stream()
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
