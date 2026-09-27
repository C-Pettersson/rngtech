package com.rngtech.content.blockentity;

import com.rngtech.content.recipe.AlgaeGrowthRecipe;
import com.rngtech.content.recipe.AlgaeGrowthRecipeInput;
import com.rngtech.content.registry.ModRecipes;

import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Optional;

final class AlgaeGrowthRecipes {
    static Optional<AlgaeGrowthRecipe> find(Level level, FluidStack water, FluidStack carbon) {
        if (level == null || water.isEmpty() || carbon.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager()
                .getRecipeFor(ModRecipes.ALGAE_GROWTH_TYPE.get(), new AlgaeGrowthRecipeInput(water, carbon), level)
                .map(holder -> holder.value());
    }

    private AlgaeGrowthRecipes() {
    }
}
