package com.rngtech.content.recipe;

import com.rngtech.content.registry.ModFluids;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

public record AmmoniaPowerCycleRecipeInput(ItemStack membrane, FluidStack ammonia) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return index == 0 ? membrane : ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return 1;
    }

    public static AmmoniaPowerCycleRecipeInput of(ItemStack membrane, int ammoniaAmount) {
        return new AmmoniaPowerCycleRecipeInput(membrane, new FluidStack(ModFluids.AMMONIA_SOURCE.get(), ammoniaAmount));
    }
}
