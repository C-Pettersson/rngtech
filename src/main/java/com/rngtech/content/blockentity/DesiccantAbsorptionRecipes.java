package com.rngtech.content.blockentity;

import com.rngtech.content.recipe.DesiccantAbsorptionRecipe;
import com.rngtech.content.recipe.DesiccantAbsorptionRecipeInput;
import com.rngtech.content.registry.ModRecipes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import java.util.Optional;

final class DesiccantAbsorptionRecipes {
    static Optional<RecipeHolder<DesiccantAbsorptionRecipe>> find(Level level, ItemStack dryInput) {
        if (level == null || dryInput.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager()
                .getRecipeFor(ModRecipes.DESICCANT_ABSORPTION_TYPE.get(), new DesiccantAbsorptionRecipeInput(dryInput), level);
    }

    static boolean hasRecipe(Level level, ItemStack dryInput) {
        return find(level, dryInput).isPresent();
    }

    private DesiccantAbsorptionRecipes() {
    }
}
