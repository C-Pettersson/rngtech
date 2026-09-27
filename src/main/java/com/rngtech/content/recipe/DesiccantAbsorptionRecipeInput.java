package com.rngtech.content.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record DesiccantAbsorptionRecipeInput(ItemStack dryInput) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        if (index == 0) {
            return dryInput;
        }
        throw new IllegalArgumentException("Desiccant absorption recipes do not contain item slot " + index);
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return dryInput.isEmpty();
    }
}
