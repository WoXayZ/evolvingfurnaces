package com.evolvingfurnaces.compat.recipeviewer.jei;

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
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.SmokerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

@JeiPlugin
public final class EFJeiPlugin implements IModPlugin {

    private static IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return EFConstants.id("recipe_viewer");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IJeiHelpers helpers = registration.getJeiHelpers();
        java.util.ArrayList<IRecipeCategory<?>> categories = new java.util.ArrayList<>();
        for (EFMachineKind kind : EFMachineKind.values()) {
            if (EFConfig.enabled(kind)) {
                categories.add(new EFRecipeViewerJeiCategory(helpers, kind, EFEnergyModule.NONE));
            }
        }
        if (EFRecipeViewer.showGeneratorCategory()) {
            categories.add(new EFRecipeViewerJeiCategory(helpers, EFMachineKind.FURNACE, EFEnergyModule.GENERATOR));
        }
        if (EFRecipeViewer.showElectricCategory()) {
            categories.add(new EFRecipeViewerJeiCategory(helpers, EFMachineKind.FURNACE, EFEnergyModule.ELECTRIC));
        }
        registration.addRecipeCategories(categories.toArray(IRecipeCategory[]::new));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (EFMachineKind kind : EFMachineKind.values()) {
            if (EFConfig.enabled(kind)) {
                registration.addRecipes(EFRecipeViewerJeiCategory.type(kind), EFTierManager.listClient(kind).levels());
            }
        }
        if (EFRecipeViewer.showGeneratorCategory()) {
            registration.addRecipes(
                    EFRecipeViewerJeiCategory.GENERATOR_TYPE,
                    EFTierManager.listClient(EFMachineKind.FURNACE).levels());
        }
        if (EFRecipeViewer.showElectricCategory()) {
            registration.addRecipes(
                    EFRecipeViewerJeiCategory.ELECTRIC_TYPE,
                    EFTierManager.listClient(EFMachineKind.FURNACE).levels());
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (EFMachineKind kind : EFMachineKind.values()) {
            if (!EFConfig.enabled(kind)) {
                continue;
            }
            var type = EFRecipeViewerJeiCategory.type(kind);
            for (EFMachineTier tier : EFTierManager.listClient(kind).levels()) {
                ItemStack stack = EvolvingMachineItem.withLevel(EFItems.item(kind), tier.level());
                registration.addRecipeCatalyst(stack, type);
                if (kind == EFMachineKind.FURNACE && EFRecipeViewer.showGeneratorCategory()) {
                    registration.addRecipeCatalyst(stack, EFRecipeViewerJeiCategory.GENERATOR_TYPE);
                }
                if (kind.isCooking() && EFRecipeViewer.showElectricCategory()) {
                    registration.addRecipeCatalyst(stack, EFRecipeViewerJeiCategory.ELECTRIC_TYPE);
                }
            }
        }
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(EvolvingMachineScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(EvolvingMachineScreen screen) {
                return screen.guiExtraAreas();
            }
        });
        registration.addGuiContainerHandler(EvolvingBrewingStandScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(EvolvingBrewingStandScreen screen) {
                return screen.guiExtraAreas();
            }
        });
        addVanillaExtraAreas(registration, FurnaceScreen.class);
        addVanillaExtraAreas(registration, BlastFurnaceScreen.class);
        addVanillaExtraAreas(registration, SmokerScreen.class);
        addVanillaExtraAreas(registration, BrewingStandScreen.class);
    }

    private static <T extends AbstractContainerScreen<?>> void addVanillaExtraAreas(
            IGuiHandlerRegistration registration,
            Class<T> type
    ) {
        registration.addGuiContainerHandler(type, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(T screen) {
                return EFVanillaInfoOverlay.extraAreas(screen);
            }
        });
    }

    public static final class EFRecipeViewerJeiCategory implements IRecipeCategory<EFMachineTier> {

        private static final java.util.EnumMap<EFMachineKind, RecipeType<EFMachineTier>> TYPES =
                new java.util.EnumMap<>(EFMachineKind.class);

        static {
            for (EFMachineKind kind : EFMachineKind.values()) {
                TYPES.put(kind, RecipeType.create(EFConstants.MOD_ID, kind.id() + "_upgrades", EFMachineTier.class));
            }
        }

        public static final RecipeType<EFMachineTier> GENERATOR_TYPE =
                RecipeType.create(EFConstants.MOD_ID, "generator_upgrades", EFMachineTier.class);
        public static final RecipeType<EFMachineTier> ELECTRIC_TYPE =
                RecipeType.create(EFConstants.MOD_ID, "electric_upgrades", EFMachineTier.class);

        public static RecipeType<EFMachineTier> type(EFMachineKind kind) {
            return TYPES.get(kind);
        }

        private static RecipeType<EFMachineTier> extraType(EFEnergyModule module) {
            return module == EFEnergyModule.ELECTRIC ? ELECTRIC_TYPE : GENERATOR_TYPE;
        }

        private final IJeiHelpers helpers;
        private final RecipeType<EFMachineTier> type;
        private final EFMachineKind kind;
        private final EFEnergyModule module;
        private final IDrawable background;
        private final IDrawable icon;

        public EFRecipeViewerJeiCategory(IJeiHelpers helpers, EFMachineKind kind, EFEnergyModule module) {
            this.helpers = helpers;
            this.kind = kind;
            this.module = module;
            this.type = module.installed() ? extraType(module) : type(kind);
            this.background = helpers.getGuiHelper().createBlankDrawable(EFRecipeViewer.WIDTH, EFRecipeViewer.HEIGHT);
            this.icon = module.installed() ? moduleIcon(module) : helpers.getGuiHelper().createDrawableIngredient(
                    VanillaTypes.ITEM_STACK, EvolvingMachineItem.withLevel(EFItems.item(kind), 1));
        }

        private static IDrawable moduleIcon(EFEnergyModule module) {
            return new IDrawable() {
                @Override
                public int getWidth() {
                    return 16;
                }

                @Override
                public int getHeight() {
                    return 16;
                }

                @Override
                public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
                    EFRecipeViewer.blitModuleIcon(graphics, xOffset, yOffset, module);
                }
            };
        }

        @Override
        public RecipeType<EFMachineTier> getRecipeType() {
            return type;
        }

        @Override
        public Component getTitle() {
            return module.installed() ? EFRecipeViewer.moduleTitle(module) : EFRecipeViewer.title(kind);
        }

        @Override
        public IDrawable getBackground() {
            return background;
        }

        @Override
        public @Nullable IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, EFMachineTier recipe, IFocusGroup focuses) {
            if (module.installed()) {
                var list = EFTierManager.listClient(kind);
                var cost = module == EFEnergyModule.GENERATOR ? list.generatorCost() : list.electricCost();
                int slotX = EFRecipeViewer.MODULE_ITEMS_X;
                for (var item : cost.items()) {
                    ItemStack stack = item.displayStack();
                    if (stack.isEmpty()) {
                        continue;
                    }
                    builder.addSlot(RecipeIngredientRole.INPUT, slotX, EFRecipeViewer.COST_Y).addItemStack(stack);
                    slotX += EFRecipeViewer.SLOT_STEP;
                }
            } else {
                int slotX = EFRecipeViewer.COST_X;
                for (var item : recipe.upgradeCost().items()) {
                    ItemStack stack = item.displayStack();
                    if (stack.isEmpty()) {
                        continue;
                    }
                    builder.addSlot(RecipeIngredientRole.INPUT, slotX, EFRecipeViewer.COST_Y).addItemStack(stack);
                    slotX += EFRecipeViewer.SLOT_STEP;
                }
            }
            builder.addSlot(RecipeIngredientRole.OUTPUT, EFRecipeViewer.MACHINE_X, EFRecipeViewer.COST_Y)
                    .addItemStack(EvolvingMachineItem.withLevel(EFItems.item(kind), recipe.level()));
        }

        @Override
        public void draw(EFMachineTier recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            if (module.installed()) {
                EFRecipeViewer.drawModule(graphics, Minecraft.getInstance().font, module, recipe, mouseX, mouseY, false);
            } else {
                EFRecipeViewer.draw(graphics, Minecraft.getInstance().font, kind, recipe, mouseX, mouseY, false);
            }
        }

        @Override
        public List<Component> getTooltipStrings(EFMachineTier recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
            if (module.installed()) {
                Component name = EFRecipeViewer.hoveredModuleButton(module, mouseX, mouseY);
                if (name != null) {
                    return List.of(name);
                }
            }
            return List.of();
        }

        @Override
        public boolean handleInput(EFMachineTier recipe, double mouseX, double mouseY, InputConstants.Key input) {
            int direction = EFRecipeViewer.navDirection(mouseX, mouseY);
            if (direction == 0) {
                return false;
            }
            if (input.getType() != InputConstants.Type.MOUSE || input.getValue() != 0) {
                return false;
            }
            showNeighbor(recipe, direction);
            return true;
        }

        private void showNeighbor(EFMachineTier recipe, int direction) {
            EFMachineTier next = EFRecipeViewer.neighbor(kind, recipe, direction, Screen.hasShiftDown());
            if (next == null || next.level() == recipe.level() || runtime == null) {
                return;
            }
            ItemStack stack = EvolvingMachineItem.withLevel(EFItems.item(kind), next.level());
            runtime.getRecipesGui().show(helpers.getFocusFactory().createFocus(
                    RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack));
        }

        @Override
        public @Nullable ResourceLocation getRegistryName(EFMachineTier recipe) {
            return module.installed()
                    ? EFConstants.id("recipe_viewer/" + module.getSerializedName() + "/" + recipe.level())
                    : EFConstants.id("recipe_viewer/" + kind.id() + "/" + recipe.level());
        }
    }
}
