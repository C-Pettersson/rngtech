package com.rngtech.content.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

public record CoalGasificationRecipeInput(ItemStack carbonInput, FluidStack water) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return index == 0 ? carbonInput : ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return 1;
    }
}
