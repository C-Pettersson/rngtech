package com.rngtech.content.recipe;

import com.rngtech.rpg.refinement.PhaseArrayAction;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** The Cradle's target and the operation it is set to; recipes match on the operation. */
public record PhaseArrayRecipeInput(ItemStack target, PhaseArrayAction action) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return target;
    }

    @Override
    public int size() {
        return 1;
    }
}
