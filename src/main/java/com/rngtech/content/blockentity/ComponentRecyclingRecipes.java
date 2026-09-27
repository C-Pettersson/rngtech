package com.rngtech.content.blockentity;

import com.rngtech.content.recipe.ComponentRecyclingRecipe;
import com.rngtech.content.registry.ModRecipes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.Optional;

final class ComponentRecyclingRecipes {
    static Optional<ComponentRecyclingRecipe> find(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager()
                .getRecipeFor(ModRecipes.COMPONENT_RECYCLING_TYPE.get(), new SingleRecipeInput(stack), level)
                .map(holder -> holder.value());
    }

    private ComponentRecyclingRecipes() {
    }
}
