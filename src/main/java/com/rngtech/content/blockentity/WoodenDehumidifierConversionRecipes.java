package com.rngtech.content.blockentity;

import com.rngtech.content.recipe.WoodenDehumidifierConversionRecipe;
import com.rngtech.content.recipe.WoodenDehumidifierConversionRecipeInput;
import com.rngtech.content.registry.ModRecipes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Optional;

final class WoodenDehumidifierConversionRecipes {
    static Optional<RecipeHolder<WoodenDehumidifierConversionRecipe>> find(Level level, ItemStack ingredient, FluidStack water) {
        if (level == null || ingredient.isEmpty() || water.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager()
                .getRecipeFor(ModRecipes.WOODEN_DEHUMIDIFIER_CONVERSION_TYPE.get(), new WoodenDehumidifierConversionRecipeInput(ingredient, water), level);
    }

    private WoodenDehumidifierConversionRecipes() {
    }
}
