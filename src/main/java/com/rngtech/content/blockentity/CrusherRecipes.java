package com.rngtech.content.blockentity;

import com.rngtech.content.recipe.CrusherRecipe;
import com.rngtech.content.registry.ModRecipes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.Optional;

final class CrusherRecipes {
    static Optional<CrusherRecipe> find(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager()
                .getRecipeFor(ModRecipes.CRUSHER_TYPE.get(), new SingleRecipeInput(stack), level)
                .map(recipe -> recipe.value());
    }

    private CrusherRecipes() {
    }
}
