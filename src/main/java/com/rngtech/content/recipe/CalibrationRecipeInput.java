package com.rngtech.content.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record CalibrationRecipeInput(
        ItemStack input,
        ItemStack pattern,
        ItemStack catalyst,
        ItemStack stabilizer
) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> input;
            case 1 -> pattern;
            case 2 -> catalyst;
            case 3 -> stabilizer;
            default -> throw new IllegalArgumentException("Recipe does not contain slot " + index);
        };
    }

    @Override
    public int size() {
        return 4;
    }
}
