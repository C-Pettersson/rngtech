package com.rngtech.content.blockentity;

import com.rngtech.content.recipe.MelterRecipe;
import com.rngtech.content.recipe.MelterRecipeInput;
import com.rngtech.content.registry.ModRecipes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Optional;

final class MelterRecipes {
    static Optional<MelterRecipe> find(Level level, ItemStack primary, ItemStack secondary, FluidStack fluid) {
        if (level == null || primary.isEmpty() || secondary.isEmpty() || fluid.isEmpty()) {
            return Optional.empty();
        }
        MelterRecipeInput input = new MelterRecipeInput(primary, secondary, fluid);
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.MELTER_TYPE.get()).stream()
                .map(holder -> holder.value())
                .filter(recipe -> recipe.matches(input, level))
                .findFirst();
    }

    private MelterRecipes() {
    }
}
