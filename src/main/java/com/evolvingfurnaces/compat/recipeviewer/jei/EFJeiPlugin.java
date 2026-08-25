package com.evolvingfurnaces.compat.recipeviewer.jei;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.EFConfig;
import com.evolvingfurnaces.client.gui.EFVanillaInfoOverlay;
import com.evolvingfurnaces.client.gui.EvolvingBrewingStandScreen;
import com.evolvingfurnaces.client.gui.EvolvingMachineScreen;
import com.evolvingfurnaces.compat.recipeviewer.EFRecipeViewer;
import com.evolvingfurnaces.machine.EvolvingMachineItem;
import com.evolvingfurnaces.registry.EFItems;
import com.evolvingfurnaces.tier.EFMachineKind;
import com.evolvingfurnaces.tier.EFMachineTier;
import com.evolvingfurnaces.tier.EFTierManager;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
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
import net.minecraft.client.gui.navigation.ScreenRectangle;
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
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IJeiHelpers helpers = registration.getJeiHelpers();
        for (EFMachineKind kind : EFMachineKind.values()) {
            if (!EFConfig.enabled(kind)) {
                continue;
            }
            registration.addRecipeCategories(new EFRecipeViewerJeiCategory(helpers, kind));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (EFMachineKind kind : EFMachineKind.values()) {
            if (!EFConfig.enabled(kind)) {
                continue;
            }
            registration.addRecipes(EFRecipeViewerJeiCategory.type(kind), List.of(kind));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (EFMachineKind kind : EFMachineKind.values()) {
            if (!EFConfig.enabled(kind)) {
                continue;
            }
            for (EFMachineTier tier : EFTierManager.listClient(kind).levels()) {
                ItemStack stack = EvolvingMachineItem.withLevel(EFItems.item(kind), tier.level());
                registration.addRecipeCatalyst(stack, EFRecipeViewerJeiCategory.type(kind));
                switch (kind) {
                    case FURNACE -> registration.addRecipeCatalyst(stack, RecipeTypes.SMELTING);
                    case BLAST_FURNACE -> registration.addRecipeCatalyst(stack, RecipeTypes.BLASTING);
                    case SMOKER -> registration.addRecipeCatalyst(stack, RecipeTypes.SMOKING);
                    case BREWING_STAND -> registration.addRecipeCatalyst(stack, RecipeTypes.BREWING);
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

    public static final class EFRecipeViewerJeiCategory implements IRecipeCategory<EFMachineKind> {

        private static final RecipeType<EFMachineKind> FURNACE =
                RecipeType.create(EFConstants.MOD_ID, "furnace_tiers", EFMachineKind.class);
        private static final RecipeType<EFMachineKind> BLAST =
                RecipeType.create(EFConstants.MOD_ID, "blast_furnace_tiers", EFMachineKind.class);
        private static final RecipeType<EFMachineKind> SMOKER =
                RecipeType.create(EFConstants.MOD_ID, "smoker_tiers", EFMachineKind.class);
        private static final RecipeType<EFMachineKind> BREWING =
                RecipeType.create(EFConstants.MOD_ID, "brewing_stand_tiers", EFMachineKind.class);

        private final EFMachineKind kind;
        private final IJeiHelpers helpers;
        private final IDrawable icon;

        public EFRecipeViewerJeiCategory(IJeiHelpers helpers, EFMachineKind kind) {
            this.kind = kind;
            this.helpers = helpers;
            this.icon = helpers.getGuiHelper().createDrawableIngredient(
                    VanillaTypes.ITEM_STACK, EvolvingMachineItem.withLevel(EFItems.item(kind), 1));
        }

        public static RecipeType<EFMachineKind> type(EFMachineKind kind) {
            return switch (kind) {
                case FURNACE -> FURNACE;
                case BLAST_FURNACE -> BLAST;
                case SMOKER -> SMOKER;
                case BREWING_STAND -> BREWING;
            };
        }

        @Override
        public RecipeType<EFMachineKind> getRecipeType() {
            return type(kind);
        }

        @Override
        public Component getTitle() {
            return EFRecipeViewer.title(kind);
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
        public void setRecipe(IRecipeLayoutBuilder builder, EFMachineKind recipe, IFocusGroup focuses) {
            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
                    .addItemStack(EvolvingMachineItem.withLevel(EFItems.item(recipe), 1));
        }

        @Override
        public void draw(EFMachineKind recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            EFRecipeViewer.draw(graphics, Minecraft.getInstance().font, recipe, mouseX, mouseY);
        }

        @Override
        public void getTooltip(ITooltipBuilder tooltip, EFMachineKind recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
            ItemStack stack = EFRecipeViewer.hoveredStack(recipe, mouseX, mouseY);
            if (stack.isEmpty()) {
                return;
            }
            tooltip.addAll(EFRecipeViewer.tooltipLines(stack));
            helpers.getIngredientManager()
                    .createTypedIngredient(VanillaTypes.ITEM_STACK, stack, false)
                    .ifPresent(tooltip::setIngredient);
        }

        @Override
        public @Nullable ResourceLocation getRegistryName(EFMachineKind recipe) {
            return EFConstants.id("recipe_viewer/" + recipe.id());
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, EFMachineKind recipe, IFocusGroup focuses) {
            builder.addInputHandler(new IJeiInputHandler() {
                @Override
                public ScreenRectangle getArea() {
                    return new ScreenRectangle(0, 0, EFRecipeViewer.WIDTH, EFRecipeViewer.HEIGHT);
                }

                @Override
                public boolean handleInput(double mouseX, double mouseY, IJeiUserInput input) {
                    int direction = EFRecipeViewer.navDirection(mouseX, mouseY);
                    if (direction != 0) {
                        if (input.getKey().getType() != InputConstants.Type.MOUSE || input.getKey().getValue() != 0) {
                            return false;
                        }
                        if (!input.isSimulate()) {
                            EFRecipeViewer.clickNav(recipe, mouseX, mouseY);
                        }
                        return true;
                    }
                    ItemStack stack = EFRecipeViewer.hoveredStack(recipe, mouseX, mouseY);
                    if (stack.isEmpty() || runtime == null) {
                        return false;
                    }
                    RecipeIngredientRole role = null;
                    var keys = runtime.getKeyMappings();
                    if (keys.getShowRecipe().isActiveAndMatches(input.getKey())) {
                        role = RecipeIngredientRole.OUTPUT;
                    } else if (keys.getShowUses().isActiveAndMatches(input.getKey())) {
                        role = RecipeIngredientRole.INPUT;
                    }
                    if (role == null) {
                        return false;
                    }
                    if (!input.isSimulate()) {
                        runtime.getRecipesGui().show(helpers.getFocusFactory().createFocus(role, VanillaTypes.ITEM_STACK, stack));
                    }
                    return true;
                }
            });
        }
    }
}
