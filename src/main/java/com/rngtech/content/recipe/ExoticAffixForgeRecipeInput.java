package com.rngtech.content.recipe;

import com.rngtech.rpg.refinement.ExoticAffixForgeAction;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record ExoticAffixForgeRecipeInput(ItemStack target, ItemStack catalyst, ExoticAffixForgeAction action)
        implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> target;
            case 1 -> catalyst;
            default -> throw new IllegalArgumentException("Recipe does not contain slot " + index);
        };
    }

    @Override
    public int size() {
        return 2;
    }
}
