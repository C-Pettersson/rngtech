package com.rngtech.content.blockentity;

import com.rngtech.content.recipe.CavitationRecipe;
import com.rngtech.content.recipe.CavitationRecipeInput;
import com.rngtech.content.registry.ModRecipes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Comparator;
import java.util.Optional;

final class CavitationRecipes {
    static Optional<CavitationRecipe> find(Level level, FluidStack fluid, ItemStack rotor, ItemStack nozzle) {
        if (fluid.isEmpty()) {
            return Optional.empty();
        }
        CavitationRecipeInput input = new CavitationRecipeInput(fluid, rotor, nozzle);
        return level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.CAVITATION_TYPE.get())
                .stream()
                .map(holder -> holder.value())
                .filter(recipe -> recipe.matches(input, level))
                .max(Comparator.comparingInt(CavitationRecipe::specificity));
    }

    private CavitationRecipes() {
    }
}
