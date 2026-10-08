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
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.SmokerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

@JeiPlugin
public final class EFJeiPlugin implements IModPlugin {

    @Override
    public Identifier getPluginUid() {
        return EFConstants.id("recipe_viewer");
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
                registration.addRecipes(EFRecipeViewerJeiCategory.type(kind), onePage(kind));
            }
        }
        if (EFRecipeViewer.showGeneratorCategory()) {
            registration.addRecipes(EFRecipeViewerJeiCategory.GENERATOR_TYPE, onePage(EFMachineKind.FURNACE));
        }
        if (EFRecipeViewer.showElectricCategory()) {
            registration.addRecipes(EFRecipeViewerJeiCategory.ELECTRIC_TYPE, onePage(EFMachineKind.FURNACE));
        }
    }

    private static List<EFMachineTier> onePage(EFMachineKind kind) {
        List<EFMachineTier> levels = EFTierManager.listClient(kind).levels();
        return levels.isEmpty() ? List.of() : List.of(levels.getFirst());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (EFMachineKind kind : EFMachineKind.values()) {
            if (!EFConfig.enabled(kind)) {
                continue;
            }
            List<ItemStack> stacks = tierStacks(kind);
            if (stacks.isEmpty()) {
                continue;
            }
            addStations(registration, EFRecipeViewerJeiCategory.type(kind), stacks);
            switch (kind) {
                case FURNACE -> {
                    addStations(registration, RecipeTypes.SMELTING, stacks);
                    addStations(registration, RecipeTypes.SMELTING_FUEL, stacks);
                }
                case BLAST_FURNACE -> {
                    addStations(registration, RecipeTypes.BLASTING, stacks);
                    addStations(registration, RecipeTypes.BLASTING_FUEL, stacks);
                }
                case SMOKER -> {
                    addStations(registration, RecipeTypes.SMOKING, stacks);
                    addStations(registration, RecipeTypes.SMOKING_FUEL, stacks);
                }
                case BREWING_STAND -> addStations(registration, RecipeTypes.BREWING, stacks);
            }
            if (kind == EFMachineKind.FURNACE && EFRecipeViewer.showGeneratorCategory()) {
                addStations(registration, EFRecipeViewerJeiCategory.GENERATOR_TYPE, stacks);
            }
            if (kind.isCooking() && EFRecipeViewer.showElectricCategory()) {
                addStations(registration, EFRecipeViewerJeiCategory.ELECTRIC_TYPE, stacks);
            }
        }
    }

    private static void addStations(IRecipeCatalystRegistration registration, IRecipeType<?> type, List<ItemStack> stacks) {
        registration.addCraftingStations(type, VanillaTypes.ITEM_STACK, stacks);
    }

    private static List<ItemStack> tierStacks(EFMachineKind kind) {
        List<ItemStack> stacks = new ArrayList<>();
        for (EFMachineTier tier : EFTierManager.listClient(kind).levels()) {
            stacks.add(EvolvingMachineItem.withLevel(EFItems.item(kind), tier.level()));
        }
        return stacks;
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
        private final IDrawable icon;

        public EFRecipeViewerJeiCategory(IJeiHelpers helpers, EFMachineKind kind, EFEnergyModule module) {
            this.helpers = helpers;
            this.kind = kind;
            this.module = module;
            this.type = module.installed() ? extraType(module) : type(kind);
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
                public void draw(GuiGraphicsExtractor graphics, int xOffset, int yOffset) {
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
        public int getWidth() {
            return EFRecipeViewer.WIDTH;
        }

        @Override
        public int getHeight() {
            return EFRecipeViewer.HEIGHT;
        }

        @Override
        public @Nullable IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, EFMachineTier recipe, IFocusGroup focuses) {
            syncFocus(focuses);
            EFMachineTier shown = shownTier(recipe);
            List<EFMachineTier> levels = EFTierManager.listClient(pageKind()).levels();
            if (module.installed()) {
                var list = EFTierManager.listClient(kind);
                var cost = module == EFEnergyModule.GENERATOR ? list.generatorCost() : list.electricCost();
                int slotX = EFRecipeViewer.MODULE_ITEMS_X;
                int index = 0;
                for (var item : cost.items()) {
                    ItemStack stack = item.displayStack();
                    if (stack.isEmpty()) {
                        continue;
                    }
                    builder.addSlot(RecipeIngredientRole.INPUT, slotX, EFRecipeViewer.COST_Y)
                            .setSlotName("cost" + index)
                            .add(stack);
                    slotX += EFRecipeViewer.SLOT_STEP;
                    index++;
                }
            } else {
                int maxCosts = 0;
                for (EFMachineTier tier : levels) {
                    maxCosts = Math.max(maxCosts, tier.upgradeCost().items().size());
                }
                int slotX = EFRecipeViewer.COST_X;
                for (int index = 0; index < maxCosts; index++) {
                    ItemStack seed = costAt(shown, index);
                    if (seed.isEmpty()) {
                        seed = firstCost(levels, index);
                    }
                    if (seed.isEmpty()) {
                        continue;
                    }
                    builder.addSlot(RecipeIngredientRole.INPUT, slotX, EFRecipeViewer.COST_Y)
                            .setSlotName("cost" + index)
                            .add(seed);
                    slotX += EFRecipeViewer.SLOT_STEP;
                }
            }
            builder.addSlot(RecipeIngredientRole.OUTPUT, EFRecipeViewer.MACHINE_X, EFRecipeViewer.COST_Y)
                    .setSlotName("result")
                    .add(EvolvingMachineItem.withLevel(EFItems.item(pageKind()), shown.level()));
        }

        @Override
        public void onDisplayedIngredientsUpdate(EFMachineTier recipe, List<IRecipeSlotDrawable> slots, IFocusGroup focuses) {
            syncFocus(focuses);
            showTier(slots, shownTier(recipe));
        }

        @Override
        public void draw(EFMachineTier recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
            EFMachineTier tier = EFRecipeViewer.current(pageKind());
            if (tier == null) {
                tier = recipe;
            }
            showTier(slots.getSlotViews(), tier);
            if (module.installed()) {
                EFRecipeViewer.drawModule(graphics, Minecraft.getInstance().font, module, tier, mouseX, mouseY, false);
            } else {
                EFRecipeViewer.draw(graphics, Minecraft.getInstance().font, kind, tier, mouseX, mouseY, false);
            }
        }

        @Override
        public void getTooltip(ITooltipBuilder tooltip, EFMachineTier recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
            if (module.installed()) {
                Component name = EFRecipeViewer.hoveredModuleButton(module, mouseX, mouseY);
                if (name != null) {
                    tooltip.add(name);
                }
            }
        }

        @Override
        public @Nullable Identifier getRegistryName(EFMachineTier recipe) {
            return module.installed()
                    ? EFConstants.id("recipe_viewer/" + module.getSerializedName() + "/" + recipe.level())
                    : EFConstants.id("recipe_viewer/" + kind.id() + "/" + recipe.level());
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, EFMachineTier recipe, IFocusGroup focuses) {
            builder.addInputHandler(new IJeiInputHandler() {
                @Override
                public ScreenRectangle getArea() {
                    return new ScreenRectangle(0, 0, EFRecipeViewer.WIDTH, EFRecipeViewer.HEIGHT);
                }

                @Override
                public boolean handleInput(double mouseX, double mouseY, IJeiUserInput input) {
                    int direction = EFRecipeViewer.navDirection(mouseX, mouseY);
                    if (direction == 0) {
                        return false;
                    }
                    if (input.getKey().getType() != InputConstants.Type.MOUSE || input.getKey().getValue() != 0) {
                        return false;
                    }
                    if (!input.isSimulate()) {
                        EFRecipeViewer.cycle(pageKind(), direction, Minecraft.getInstance().hasShiftDown());
                    }
                    return true;
                }
            });
        }

        private EFMachineKind pageKind() {
            return module.installed() ? EFMachineKind.FURNACE : kind;
        }

        private void syncFocus(IFocusGroup focuses) {
            focuses.getItemStackFocuses().forEach(focus -> {
                ItemStack stack = focus.getTypedValue().getIngredient();
                if (stack.getItem() instanceof EvolvingMachineItem) {
                    EFRecipeViewer.focusLevel(pageKind(), EvolvingMachineItem.levelOf(stack));
                }
            });
        }

        private EFMachineTier shownTier(EFMachineTier fallback) {
            EFMachineTier tier = EFRecipeViewer.current(pageKind());
            return tier == null ? fallback : tier;
        }

        private void showTier(Iterable<? extends IRecipeSlotView> slots, EFMachineTier tier) {
            List<ItemStack> costs = currentCosts(tier);
            for (var view : slots) {
                if (!(view instanceof IRecipeSlotDrawable drawable)) {
                    continue;
                }
                String name = view.getSlotName().orElse("");
                drawable.clearDisplayOverrides();
                var overrides = drawable.createDisplayOverrides();
                if (name.equals("result")) {
                    overrides.add(EvolvingMachineItem.withLevel(EFItems.item(pageKind()), tier.level()));
                } else if (name.startsWith("cost")) {
                    int index = Integer.parseInt(name.substring(4));
                    if (index >= 0 && index < costs.size() && !costs.get(index).isEmpty()) {
                        overrides.add(costs.get(index));
                    }
                }
            }
        }

        private static ItemStack costAt(EFMachineTier tier, int index) {
            var items = tier.upgradeCost().items();
            if (index < 0 || index >= items.size()) {
                return ItemStack.EMPTY;
            }
            return items.get(index).displayStack();
        }

        private static ItemStack firstCost(List<EFMachineTier> levels, int index) {
            for (EFMachineTier tier : levels) {
                ItemStack stack = costAt(tier, index);
                if (!stack.isEmpty()) {
                    return stack;
                }
            }
            return ItemStack.EMPTY;
        }

        private List<ItemStack> currentCosts(EFMachineTier tier) {
            List<ItemStack> costs = new ArrayList<>();
            if (module.installed()) {
                var list = EFTierManager.listClient(kind);
                var cost = module == EFEnergyModule.GENERATOR ? list.generatorCost() : list.electricCost();
                for (var item : cost.items()) {
                    ItemStack stack = item.displayStack();
                    if (!stack.isEmpty()) {
                        costs.add(stack);
                    }
                }
            } else {
                for (var item : tier.upgradeCost().items()) {
                    costs.add(item.displayStack());
                }
            }
            return costs;
        }
    }
}
