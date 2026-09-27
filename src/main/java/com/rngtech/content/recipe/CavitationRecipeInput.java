package com.rngtech.content.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

public record CavitationRecipeInput(FluidStack fluid, ItemStack rotor, ItemStack nozzle) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> rotor;
            case 1 -> nozzle;
            default -> throw new IllegalArgumentException("No item for index " + index);
        };
    }

    @Override
    public int size() {
        return 2;
    }

    @Override
    public boolean isEmpty() {
        return fluid.isEmpty() && rotor.isEmpty() && nozzle.isEmpty();
    }
}
