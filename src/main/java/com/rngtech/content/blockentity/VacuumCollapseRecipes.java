package com.rngtech.content.blockentity;

import com.rngtech.content.recipe.VacuumCollapseRecipe;
import com.rngtech.content.registry.ModRecipes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.Optional;

final class VacuumCollapseRecipes {
    static Optional<VacuumCollapseRecipe> find(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager()
                .getRecipeFor(ModRecipes.VACUUM_COLLAPSE_TYPE.get(), new SingleRecipeInput(stack), level)
                .map(recipe -> recipe.value());
    }

    private VacuumCollapseRecipes() {
    }
}
