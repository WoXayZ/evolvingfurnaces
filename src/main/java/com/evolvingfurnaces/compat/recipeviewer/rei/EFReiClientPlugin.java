package com.evolvingfurnaces.compat.recipeviewer.rei;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.client.gui.EvolvingMachineScreen;
import com.evolvingfurnaces.compat.recipeviewer.EFRecipeViewer;
import com.evolvingfurnaces.machine.EvolvingMachineItem;
import com.evolvingfurnaces.registry.EFItems;
import com.evolvingfurnaces.tier.EFMachineKind;
import com.evolvingfurnaces.tier.EFMachineTier;
import com.evolvingfurnaces.tier.EFTierManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.WidgetWithBounds;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

@REIPluginClient
public final class EFReiClientPlugin implements REIClientPlugin {

    @Override
    public void registerCategories(CategoryRegistry registry) {
        for (EFMachineKind kind : EFMachineKind.values()) {
            registry.add(new EFRecipeViewerReiCategory(kind));
            for (EFMachineTier tier : EFTierManager.listClient(kind).levels()) {
                ItemStack stack = EvolvingMachineItem.withLevel(EFItems.item(kind), tier.level());
                registry.addWorkstations(EFRecipeViewerReiCategory.id(kind), EntryStacks.of(stack));
            }
        }
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        for (EFMachineKind kind : EFMachineKind.values()) {
            registry.add(new EFRecipeViewerReiDisplay(kind));
        }
    }

    @Override
    public void registerExclusionZones(ExclusionZones zones) {
        zones.register(EvolvingMachineScreen.class, screen -> {
            List<Rectangle> areas = new ArrayList<>();
            for (var area : screen.guiExtraAreas()) {
                areas.add(new Rectangle(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
            }
            return areas;
        });
    }

    public record EFRecipeViewerReiDisplay(EFMachineKind kind) implements Display {
        @Override
        public List<EntryIngredient> getInputEntries() {
            return List.of();
        }

        @Override
        public List<EntryIngredient> getOutputEntries() {
            return List.of(EntryIngredient.of(EntryStacks.of(EFRecipeViewer.currentStack(kind))));
        }

        @Override
        public CategoryIdentifier<?> getCategoryIdentifier() {
            return EFRecipeViewerReiCategory.id(kind);
        }
    }

    public static final class EFRecipeViewerReiCategory implements DisplayCategory<EFRecipeViewerReiDisplay> {
        private final EFMachineKind kind;

        public EFRecipeViewerReiCategory(EFMachineKind kind) {
            this.kind = kind;
        }

        public static CategoryIdentifier<EFRecipeViewerReiDisplay> id(EFMachineKind kind) {
            return CategoryIdentifier.of(EFConstants.MOD_ID, kind.id() + "_tiers");
        }

        @Override
        public CategoryIdentifier<? extends EFRecipeViewerReiDisplay> getCategoryIdentifier() {
            return id(kind);
        }

        @Override
        public Component getTitle() {
            return EFRecipeViewer.title(kind);
        }

        @Override
        public Renderer getIcon() {
            return EntryStacks.of(EvolvingMachineItem.withLevel(EFItems.item(kind), 1));
        }

        @Override
        public int getDisplayHeight() {
            return EFRecipeViewer.HEIGHT + 8;
        }

        @Override
        public int getDisplayWidth(EFRecipeViewerReiDisplay display) {
            return EFRecipeViewer.WIDTH + 8;
        }

        @Override
        public List<Widget> setupDisplay(EFRecipeViewerReiDisplay display, Rectangle bounds) {
            return List.of(
                    Widgets.createRecipeBase(bounds),
                    new ViewerWidget(display.kind(), new Rectangle(bounds.x + 4, bounds.y + 4, EFRecipeViewer.WIDTH, EFRecipeViewer.HEIGHT))
            );
        }
    }

    private static final class ViewerWidget extends WidgetWithBounds {
        private final EFMachineKind kind;
        private final Rectangle bounds;

        private ViewerWidget(EFMachineKind kind, Rectangle bounds) {
            this.kind = kind;
            this.bounds = bounds;
        }

        @Override
        public Rectangle getBounds() {
            return bounds;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            graphics.pose().pushPose();
            graphics.pose().translate(bounds.x, bounds.y, 0);
            EFRecipeViewer.draw(graphics, Minecraft.getInstance().font, kind, mouseX - bounds.x, mouseY - bounds.y);
            graphics.pose().popPose();
            ItemStack stack = EFRecipeViewer.hoveredStack(kind, mouseX - bounds.x, mouseY - bounds.y);
            if (!stack.isEmpty()) {
                Tooltip.create(new Point(mouseX, mouseY), EFRecipeViewer.tooltipLines(stack)).queue();
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0 || !containsMouse(mouseX, mouseY)) {
                return false;
            }
            return EFRecipeViewer.clickNav(kind, mouseX - bounds.x, mouseY - bounds.y);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return Collections.emptyList();
        }
    }
}
