package com.rngtech.content.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public record BatteryAssemblyRecipeInput(List<ItemStack> items, FluidStack fluid) implements RecipeInput {
    public BatteryAssemblyRecipeInput {
        items = List.copyOf(items);
        fluid = fluid.copy();
    }

    @Override
    public ItemStack getItem(int index) {
        if (index < 0 || index >= items.size()) {
            throw new IllegalArgumentException("Recipe does not contain slot " + index);
        }
        return items.get(index);
    }

    @Override
    public int size() {
        return items.size();
    }
}
