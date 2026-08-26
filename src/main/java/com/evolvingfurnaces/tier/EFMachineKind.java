package com.evolvingfurnaces.tier;

import com.evolvingfurnaces.EFConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

public enum EFMachineKind {
    FURNACE("furnace", RecipeBookType.FURNACE),
    BLAST_FURNACE("blast_furnace", RecipeBookType.BLAST_FURNACE),
    SMOKER("smoker", RecipeBookType.SMOKER),
    BREWING_STAND("brewing_stand", null);

    private final String id;
    @Nullable
    private final RecipeBookType recipeBookType;

    EFMachineKind(String id, @Nullable RecipeBookType recipeBookType) {
        this.id = id;
        this.recipeBookType = recipeBookType;
    }

    public String id() {
        return id;
    }

    public String dataPath() {
        return id;
    }

    public ResourceLocation dataFile() {
        return EFConstants.id(id);
    }

    public boolean isCooking() {
        return this != BREWING_STAND;
    }

    public RecipeBookType recipeBookType() {
        if (recipeBookType == null) {
            throw new IllegalStateException(id + " has no furnace recipe book");
        }
        return recipeBookType;
    }

    @SuppressWarnings("unchecked")
    public RecipeType<AbstractCookingRecipe> recipeType() {
        return switch (this) {
            case FURNACE -> (RecipeType<AbstractCookingRecipe>) (RecipeType<?>) RecipeType.SMELTING;
            case BLAST_FURNACE -> (RecipeType<AbstractCookingRecipe>) (RecipeType<?>) RecipeType.BLASTING;
            case SMOKER -> (RecipeType<AbstractCookingRecipe>) (RecipeType<?>) RecipeType.SMOKING;
            case BREWING_STAND -> throw new IllegalStateException("brewing stand has no cooking recipe type");
        };
    }

    public Block vanillaBlock() {
        return switch (this) {
            case FURNACE -> Blocks.FURNACE;
            case BLAST_FURNACE -> Blocks.BLAST_FURNACE;
            case SMOKER -> Blocks.SMOKER;
            case BREWING_STAND -> Blocks.BREWING_STAND;
        };
    }

    public ResourceLocation vanillaFrontOffPng() {
        return ResourceLocation.withDefaultNamespace("textures/block/" + id + "_front.png");
    }

    public String timeLangKey() {
        return this == BREWING_STAND ? "evolvingfurnaces.gui.brew_time" : "evolvingfurnaces.gui.cook_time";
    }

    public String batchLangKey() {
        return this == BREWING_STAND ? "evolvingfurnaces.gui.potions_per_slot" : "evolvingfurnaces.gui.batch";
    }

    public String progressLangKey() {
        return this == BREWING_STAND ? "evolvingfurnaces.gui.brews" : "evolvingfurnaces.gui.cooks";
    }

    public static EFMachineKind fromVanilla(Block block) {
        if (block == Blocks.FURNACE) {
            return FURNACE;
        }
        if (block == Blocks.BLAST_FURNACE) {
            return BLAST_FURNACE;
        }
        if (block == Blocks.SMOKER) {
            return SMOKER;
        }
        if (block == Blocks.BREWING_STAND) {
            return BREWING_STAND;
        }
        return null;
    }
}
