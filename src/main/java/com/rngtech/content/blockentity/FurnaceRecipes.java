package com.rngtech.content.blockentity;

import com.rngtech.content.recipe.FurnaceRecipe;
import com.rngtech.content.registry.ModRecipes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.Optional;

final class FurnaceRecipes {
    static Optional<FurnaceRecipe> find(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager()
                .getRecipeFor(ModRecipes.FURNACE_TYPE.get(), new SingleRecipeInput(stack), level)
                .map(recipe -> recipe.value());
    }

    private FurnaceRecipes() {
    }
}
