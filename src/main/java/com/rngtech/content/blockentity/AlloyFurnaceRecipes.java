package com.rngtech.content.blockentity;

import com.rngtech.content.recipe.AlloyFurnaceRecipe;
import com.rngtech.content.recipe.AlloyFurnaceRecipeInput;
import com.rngtech.content.registry.ModRecipes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

final class AlloyFurnaceRecipes {
    static Optional<AlloyFurnaceRecipe> find(Level level, ItemStack[] inputs) {
        return level.getRecipeManager()
                .getRecipeFor(ModRecipes.ALLOY_FURNACE_TYPE.get(), new AlloyFurnaceRecipeInput(inputs), level)
                .map(holder -> holder.value());
    }

    private AlloyFurnaceRecipes() {
    }
}
