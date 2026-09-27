package com.rngtech.content.blockentity;

import com.rngtech.content.recipe.MetalPressRecipe;
import com.rngtech.content.recipe.MetalPressRecipeInput;
import com.rngtech.content.registry.ModRecipes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

final class MetalPressRecipes {
    static Optional<MetalPressRecipe> find(Level level, ItemStack input, ItemStack mold) {
        if (input.isEmpty() || mold.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager()
                .getRecipeFor(ModRecipes.METAL_PRESS_TYPE.get(), new MetalPressRecipeInput(input, mold), level)
                .map(recipe -> recipe.value());
    }

    private MetalPressRecipes() {
    }
}
