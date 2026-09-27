package com.rngtech.content.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

public record AlgaeGrowthRecipeInput(FluidStack water, FluidStack carbon) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        throw new IllegalArgumentException("Algae growth recipes do not contain item slot " + index);
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return water.isEmpty() || carbon.isEmpty();
    }
}
