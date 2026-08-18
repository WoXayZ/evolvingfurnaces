package com.evolvingfurnaces.tier;

import com.evolvingfurnaces.EFConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public enum EFMachineKind {
    FURNACE("furnace", RecipeBookType.FURNACE),
    BLAST_FURNACE("blast_furnace", RecipeBookType.BLAST_FURNACE),
    SMOKER("smoker", RecipeBookType.SMOKER);

    private final String id;
    private final RecipeBookType recipeBookType;

    EFMachineKind(String id, RecipeBookType recipeBookType) {
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

    public RecipeBookType recipeBookType() {
        return recipeBookType;
    }

    @SuppressWarnings("unchecked")
    public RecipeType<AbstractCookingRecipe> recipeType() {
        return switch (this) {
            case FURNACE -> (RecipeType<AbstractCookingRecipe>) (RecipeType<?>) RecipeType.SMELTING;
            case BLAST_FURNACE -> (RecipeType<AbstractCookingRecipe>) (RecipeType<?>) RecipeType.BLASTING;
            case SMOKER -> (RecipeType<AbstractCookingRecipe>) (RecipeType<?>) RecipeType.SMOKING;
        };
    }

    public Block vanillaBlock() {
        return switch (this) {
            case FURNACE -> Blocks.FURNACE;
            case BLAST_FURNACE -> Blocks.BLAST_FURNACE;
            case SMOKER -> Blocks.SMOKER;
        };
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
        return null;
    }
}
