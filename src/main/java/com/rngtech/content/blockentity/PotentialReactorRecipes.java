package com.rngtech.content.blockentity;

import com.rngtech.content.item.MalformedIngotItem;
import com.rngtech.content.recipe.PotentialReactorRecipe;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.registry.ModRecipes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.Optional;

final class PotentialReactorRecipes {
    static Optional<PotentialReactorRecipe> find(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        if (MalformedIngotItem.isMalformedIngot(stack)) {
            return Optional.of(new PotentialReactorRecipe(
                    "malformed_ingot",
                    Ingredient.of(ModItems.MALFORMED_INGOT.get()),
                    3600,
                    100,
                    MalformedIngotItem.recoveryResidue(stack),
                    4
            ));
        }
        return level.getRecipeManager()
                .getRecipeFor(ModRecipes.POTENTIAL_REACTOR_TYPE.get(), new SingleRecipeInput(stack), level)
                .map(recipe -> recipe.value());
    }

    private PotentialReactorRecipes() {
    }
}
