package com.rngtech.content.blockentity;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.function.BiPredicate;

/** Keeps automation from inserting items that no recipe of a machine can use, so they cannot jam an input slot. */
final class RecipeInputFilter {
    static <I extends RecipeInput, R extends Recipe<I>> boolean anyAccepts(
            Level level,
            RecipeType<R> type,
            ItemStack stack,
            BiPredicate<R, ItemStack> accepts
    ) {
        if (level == null || stack.isEmpty()) {
            return false;
        }
        return level.getRecipeManager().getAllRecipesFor(type).stream()
                .anyMatch(holder -> accepts.test(holder.value(), stack));
    }

    private RecipeInputFilter() {
    }
}
