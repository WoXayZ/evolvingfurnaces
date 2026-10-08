package com.evolvingfurnaces.compat.recipeviewer.rei;

import com.mojang.serialization.MapCodec;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.compat.GuiGraphics;
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
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.codec.StreamCodec;
import org.joml.Matrix3x2fStack;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.SmokerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

@REIPluginClient
public final class EFReiClientPlugin implements REIClientPlugin {

    @Override
    public void registerCategories(CategoryRegistry registry) {
        for (EFMachineKind kind : EFMachineKind.values()) {
            if (!EFConfig.enabled(kind)) {
                continue;
            }
            registry.add(new EFRecipeViewerReiCategory(kind, EFEnergyModule.NONE));
            for (EFMachineTier tier : EFTierManager.listClient(kind).levels()) {
                ItemStack stack = EvolvingMachineItem.withLevel(EFItems.item(kind), tier.level());
                registry.addWorkstations(EFRecipeViewerReiCategory.id(kind), EntryStacks.of(stack));
            }
        }
        addModuleCategory(registry, EFEnergyModule.GENERATOR);
        addModuleCategory(registry, EFEnergyModule.ELECTRIC);
    }

    private static void addModuleCategory(CategoryRegistry registry, EFEnergyModule module) {
        boolean show = module == EFEnergyModule.GENERATOR
                ? EFRecipeViewer.showGeneratorCategory()
                : EFRecipeViewer.showElectricCategory();
        if (!show) {
            return;
        }
        registry.add(new EFRecipeViewerReiCategory(EFMachineKind.FURNACE, module));
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
                ItemStack stack = EvolvingMachineItem.withLevel(EFItems.item(kind), tier.level());
                registry.addWorkstations(EFRecipeViewerReiCategory.moduleId(module), EntryStacks.of(stack));
            }
        }
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        for (EFMachineKind kind : EFMachineKind.values()) {
            if (!EFConfig.enabled(kind)) {
                continue;
            }
            registry.add(new EFRecipeViewerReiDisplay(kind, EFEnergyModule.NONE));
        }
        if (EFRecipeViewer.showGeneratorCategory()) {
            registry.add(new EFRecipeViewerReiDisplay(EFMachineKind.FURNACE, EFEnergyModule.GENERATOR));
        }
        if (EFRecipeViewer.showElectricCategory()) {
            registry.add(new EFRecipeViewerReiDisplay(EFMachineKind.FURNACE, EFEnergyModule.ELECTRIC));
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
        zones.register(EvolvingBrewingStandScreen.class, screen -> {
            List<Rectangle> areas = new ArrayList<>();
            for (var area : screen.guiExtraAreas()) {
                areas.add(new Rectangle(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
            }
            return areas;
        });
        zones.register(FurnaceScreen.class, EFReiClientPlugin::vanillaZones);
        zones.register(BlastFurnaceScreen.class, EFReiClientPlugin::vanillaZones);
        zones.register(SmokerScreen.class, EFReiClientPlugin::vanillaZones);
        zones.register(BrewingStandScreen.class, EFReiClientPlugin::vanillaZones);
    }

    private static List<Rectangle> vanillaZones(AbstractContainerScreen<?> screen) {
        List<Rectangle> areas = new ArrayList<>();
        for (var area : EFVanillaInfoOverlay.extraAreas(screen)) {
            areas.add(new Rectangle(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
        }
        return areas;
    }

    public record EFRecipeViewerReiDisplay(EFMachineKind kind, EFEnergyModule module) implements Display {
        private static final EFRecipeViewerReiDisplay EMPTY = new EFRecipeViewerReiDisplay(EFMachineKind.FURNACE, EFEnergyModule.NONE);
        private static final DisplaySerializer<EFRecipeViewerReiDisplay> SERIALIZER = DisplaySerializer.of(
                MapCodec.unit(EMPTY),
                StreamCodec.unit(EMPTY),
                false);

        @Override
        public DisplaySerializer<? extends Display> getSerializer() {
            return SERIALIZER;
        }

        @Override
        public Optional<net.minecraft.resources.Identifier> getDisplayLocation() {
            return Optional.empty();
        }

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
            return module.installed() ? EFRecipeViewerReiCategory.moduleId(module) : EFRecipeViewerReiCategory.id(kind);
        }
    }

    public static final class EFRecipeViewerReiCategory implements DisplayCategory<EFRecipeViewerReiDisplay> {
        private static final java.util.EnumMap<EFMachineKind, CategoryIdentifier<EFRecipeViewerReiDisplay>> IDS =
                new java.util.EnumMap<>(EFMachineKind.class);

        static {
            for (EFMachineKind kind : EFMachineKind.values()) {
                IDS.put(kind, CategoryIdentifier.of(EFConstants.MOD_ID, kind.id() + "_upgrades"));
            }
        }

        public static final CategoryIdentifier<EFRecipeViewerReiDisplay> GENERATOR_ID =
                CategoryIdentifier.of(EFConstants.MOD_ID, "generator_upgrades");
        public static final CategoryIdentifier<EFRecipeViewerReiDisplay> ELECTRIC_ID =
                CategoryIdentifier.of(EFConstants.MOD_ID, "electric_upgrades");

        public static CategoryIdentifier<EFRecipeViewerReiDisplay> id(EFMachineKind kind) {
            return IDS.get(kind);
        }

        public static CategoryIdentifier<EFRecipeViewerReiDisplay> moduleId(EFEnergyModule module) {
            return module == EFEnergyModule.ELECTRIC ? ELECTRIC_ID : GENERATOR_ID;
        }

        private final EFMachineKind kind;
        private final EFEnergyModule module;

        public EFRecipeViewerReiCategory(EFMachineKind kind, EFEnergyModule module) {
            this.kind = kind;
            this.module = module;
        }

        @Override
        public CategoryIdentifier<? extends EFRecipeViewerReiDisplay> getCategoryIdentifier() {
            return module.installed() ? moduleId(module) : id(kind);
        }

        @Override
        public Component getTitle() {
            return module.installed() ? EFRecipeViewer.moduleTitle(module) : EFRecipeViewer.title(kind);
        }

        @Override
        public Renderer getIcon() {
            if (!module.installed()) {
                return EntryStacks.of(EvolvingMachineItem.withLevel(EFItems.item(kind), 1));
            }
            return new Renderer() {
                @Override
                public void render(GuiGraphics graphics, Rectangle bounds, int mouseX, int mouseY, float delta) {
                    EFRecipeViewer.blitModuleIcon(graphics, bounds.x, bounds.y, module);
                }
            };
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
                    new ViewerWidget(display.kind(), display.module(), new Rectangle(bounds.x + 4, bounds.y + 4, EFRecipeViewer.WIDTH, EFRecipeViewer.HEIGHT))
            );
        }
    }

    private static final class ViewerWidget extends WidgetWithBounds {
        private final EFMachineKind kind;
        private final EFEnergyModule module;
        private final Rectangle bounds;

        private ViewerWidget(EFMachineKind kind, EFEnergyModule module, Rectangle bounds) {
            this.kind = kind;
            this.module = module;
            this.bounds = bounds;
        }

        @Override
        public Rectangle getBounds() {
            return bounds;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            Matrix3x2fStack pose = graphics.pose();
            pose.pushMatrix();
            pose.translate(bounds.x, bounds.y);
            if (module.installed()) {
                EFRecipeViewer.drawModule(graphics, Minecraft.getInstance().font, module, mouseX - bounds.x, mouseY - bounds.y);
            } else {
                EFRecipeViewer.draw(graphics, Minecraft.getInstance().font, kind, mouseX - bounds.x, mouseY - bounds.y);
            }
            pose.popMatrix();
            List<Component> lines = module.installed()
                    ? EFRecipeViewer.hoveredModuleTooltip(module, mouseX - bounds.x, mouseY - bounds.y)
                    : EFRecipeViewer.tooltipLines(EFRecipeViewer.hoveredStack(kind, mouseX - bounds.x, mouseY - bounds.y));
            if (!lines.isEmpty()) {
                Tooltip.create(new Point(mouseX, mouseY), lines).queue();
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (event.button() != 0 || !containsMouse(event.x(), event.y())) {
                return false;
            }
            return EFRecipeViewer.clickNav(kind, event.x() - bounds.x, event.y() - bounds.y);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return Collections.emptyList();
        }
    }
}
