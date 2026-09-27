package com.rngtech.content.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

public record WoodenDehumidifierConversionRecipeInput(ItemStack ingredient, FluidStack water) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        if (index == 0) {
            return ingredient;
        }
        throw new IllegalArgumentException("Wooden Dehumidifier conversion recipes do not contain item slot " + index);
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return ingredient.isEmpty() || water.isEmpty();
    }
}
