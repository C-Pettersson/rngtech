package com.rngtech.content.blockentity;

import com.rngtech.content.item.TankContents;
import com.rngtech.content.recipe.ComponentRecyclingRecipe;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModRecipes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.Optional;

final class ComponentRecyclingRecipes {
    static Optional<ComponentRecyclingRecipe> find(Level level, ItemStack stack) {
        if (stack.isEmpty() || holdsFluid(stack)) {
            return Optional.empty();
        }
        return level.getRecipeManager()
                .getRecipeFor(ModRecipes.COMPONENT_RECYCLING_TYPE.get(), new SingleRecipeInput(stack), level)
                .map(holder -> holder.value());
    }

    /** Only purging destroys fluid, so a tank or fuel part has to be empty before it can be recycled. */
    private static boolean holdsFluid(ItemStack stack) {
        TankContents contents = stack.get(ModDataComponents.TANK_CONTENTS.get());
        return contents != null && !contents.isEmpty();
    }

    private ComponentRecyclingRecipes() {
    }
}
