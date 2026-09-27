package com.rngtech.content.blockentity;

import com.rngtech.content.recipe.CalibrationRecipe;
import com.rngtech.content.recipe.CalibrationRecipeInput;
import com.rngtech.content.registry.ModRecipes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

final class CalibrationRecipes {
    static Optional<CalibrationRecipe> find(
            Level level,
            ItemStack input,
            ItemStack pattern,
            ItemStack catalyst,
            ItemStack stabilizer
    ) {
        if (input.isEmpty() || pattern.isEmpty() || catalyst.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager()
                .getRecipeFor(ModRecipes.CALIBRATION_TYPE.get(), new CalibrationRecipeInput(input, pattern, catalyst, stabilizer), level)
                .map(recipe -> recipe.value());
    }

    private CalibrationRecipes() {
    }
}
