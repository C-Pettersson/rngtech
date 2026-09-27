package com.rngtech.content.blockentity;

import com.rngtech.content.recipe.AmmoniaPowerCycleRecipe;
import com.rngtech.content.recipe.AmmoniaPowerCycleRecipeInput;
import com.rngtech.content.recipe.AmmoniaSynthesisRecipe;
import com.rngtech.content.recipe.AmmoniaSynthesisRecipeInput;
import com.rngtech.content.registry.ModRecipes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Optional;

final class AmmoniaRecipes {
    static Optional<AmmoniaSynthesisRecipe> synthesis(Level level, ItemStack catalyst, FluidStack nitrogen, FluidStack hydrogen) {
        if (level == null || catalyst.isEmpty() || nitrogen.isEmpty() || hydrogen.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager()
                .getRecipeFor(ModRecipes.AMMONIA_SYNTHESIS_TYPE.get(), new AmmoniaSynthesisRecipeInput(catalyst, nitrogen, hydrogen), level)
                .map(holder -> holder.value());
    }

    static Optional<AmmoniaPowerCycleRecipe> powerCycle(Level level, ItemStack membrane, FluidStack ammonia) {
        if (level == null || membrane.isEmpty() || ammonia.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager()
                .getRecipeFor(ModRecipes.AMMONIA_POWER_CYCLE_TYPE.get(), new AmmoniaPowerCycleRecipeInput(membrane, ammonia), level)
                .map(holder -> holder.value());
    }

    private AmmoniaRecipes() {
    }
}
