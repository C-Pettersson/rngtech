package com.rngtech.content.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

public record GasReformingRecipeInput(ItemStack catalyst, FluidStack feedGas, FluidStack water)
        implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return index == 0 ? catalyst : ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return 1;
    }
}
