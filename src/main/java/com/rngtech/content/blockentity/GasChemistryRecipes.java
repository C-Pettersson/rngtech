package com.rngtech.content.blockentity;

import com.rngtech.content.recipe.CoalGasificationRecipe;
import com.rngtech.content.recipe.CoalGasificationRecipeInput;
import com.rngtech.content.recipe.GasCombustionRecipe;
import com.rngtech.content.recipe.GasCombustionRecipeInput;
import com.rngtech.content.recipe.GasReformingRecipe;
import com.rngtech.content.recipe.GasReformingRecipeInput;
import com.rngtech.content.registry.ModRecipes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Optional;

final class GasChemistryRecipes {
    static Optional<CoalGasificationRecipe> gasification(Level level, ItemStack carbonInput, FluidStack water) {
        if (level == null || carbonInput.isEmpty() || water.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager()
                .getRecipeFor(ModRecipes.COAL_GASIFICATION_TYPE.get(), new CoalGasificationRecipeInput(carbonInput, water), level)
                .map(holder -> holder.value());
    }

    static Optional<GasCombustionRecipe> combustion(Level level, FluidStack gas) {
        if (level == null || gas.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager()
                .getRecipeFor(ModRecipes.GAS_COMBUSTION_TYPE.get(), new GasCombustionRecipeInput(gas), level)
                .map(holder -> holder.value());
    }

    static Optional<GasReformingRecipe> reforming(Level level, ItemStack catalyst, FluidStack feedGas, FluidStack water) {
        if (level == null || catalyst.isEmpty() || feedGas.isEmpty() || water.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager()
                .getRecipeFor(ModRecipes.GAS_REFORMING_TYPE.get(), new GasReformingRecipeInput(catalyst, feedGas, water), level)
                .map(holder -> holder.value());
    }

    private GasChemistryRecipes() {
    }
}
