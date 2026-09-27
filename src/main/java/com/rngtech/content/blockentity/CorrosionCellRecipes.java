package com.rngtech.content.blockentity;

import com.rngtech.content.recipe.CorrosionCellRecipe;
import com.rngtech.content.recipe.CorrosionCellRecipeInput;
import com.rngtech.content.registry.ModRecipes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import java.util.Optional;

final class CorrosionCellRecipes {
    static Optional<CorrosionCellRecipe> find(Level level, ItemStack plate, ItemStack electrolyte) {
        if (plate.isEmpty() || electrolyte.isEmpty()) {
            return Optional.empty();
        }
        CorrosionCellRecipeInput input = new CorrosionCellRecipeInput(plate, electrolyte);
        return level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.CORROSION_CELL_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .filter(recipe -> recipe.matches(input, level))
                .findFirst();
    }

    static boolean isPlateInput(Level level, ItemStack stack) {
        return !stack.isEmpty()
                && level.getRecipeManager()
                        .getAllRecipesFor(ModRecipes.CORROSION_CELL_TYPE.get())
                        .stream()
                        .map(RecipeHolder::value)
                        .anyMatch(recipe -> recipe.plate().test(stack));
    }

    static boolean isElectrolyteInput(Level level, ItemStack stack) {
        return !stack.isEmpty()
                && level.getRecipeManager()
                        .getAllRecipesFor(ModRecipes.CORROSION_CELL_TYPE.get())
                        .stream()
                        .map(RecipeHolder::value)
                        .anyMatch(recipe -> recipe.electrolyte().test(stack));
    }

    private CorrosionCellRecipes() {
    }
}
