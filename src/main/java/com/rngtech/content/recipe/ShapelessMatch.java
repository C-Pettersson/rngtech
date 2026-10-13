package com.rngtech.content.recipe;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Matches counted ingredients against input slots in any order. An ingredient's count may come from several slots, and
 * one slot may pay for several ingredients, so a recipe that takes two of an item needs only one slot of it.
 */
public final class ShapelessMatch {
    /**
     * Units each slot gives to {@code crafts} crafts, or {@code null} when the slots cannot pay for them or hold an
     * item no ingredient takes.
     */
    public static int[] consumed(List<CountedIngredient> ingredients, ItemStack[] stacks, int crafts) {
        if (crafts <= 0 || !allNonEmptyStacksAreEligible(ingredients, stacks)) {
            return null;
        }
        int[] remaining = new int[stacks.length];
        for (int slot = 0; slot < stacks.length; slot++) {
            remaining[slot] = stacks[slot].getCount();
        }
        int[] consumed = new int[stacks.length];
        return matchIngredient(ingredients, crafts, 0, stacks, remaining, consumed) ? consumed : null;
    }

    /** The most crafts, up to {@code limit}, that the slots can pay for at once; 0 when they cannot pay for one. */
    public static int crafts(List<CountedIngredient> ingredients, ItemStack[] stacks, int limit) {
        for (int crafts = limit; crafts > 0; crafts--) {
            if (consumed(ingredients, stacks, crafts) != null) {
                return crafts;
            }
        }
        return 0;
    }

    private static boolean allNonEmptyStacksAreEligible(List<CountedIngredient> ingredients, ItemStack[] stacks) {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty() && ingredients.stream().noneMatch(ingredient -> ingredient.test(stack))) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchIngredient(
            List<CountedIngredient> ingredients,
            int crafts,
            int ingredientIndex,
            ItemStack[] stacks,
            int[] remaining,
            int[] consumed
    ) {
        if (ingredientIndex >= ingredients.size()) {
            return true;
        }
        CountedIngredient ingredient = ingredients.get(ingredientIndex);
        return consumeIngredient(ingredients, crafts, ingredient, ingredient.count() * crafts, 0, stacks, remaining, consumed,
                ingredientIndex);
    }

    private static boolean consumeIngredient(
            List<CountedIngredient> ingredients,
            int crafts,
            CountedIngredient ingredient,
            int needed,
            int slot,
            ItemStack[] stacks,
            int[] remaining,
            int[] consumed,
            int ingredientIndex
    ) {
        if (needed <= 0) {
            return matchIngredient(ingredients, crafts, ingredientIndex + 1, stacks, remaining, consumed);
        }
        if (slot >= stacks.length) {
            return false;
        }

        int available = ingredient.test(stacks[slot]) ? remaining[slot] : 0;
        int maximum = Math.min(available, needed);
        for (int amount = maximum; amount >= 0; amount--) {
            remaining[slot] -= amount;
            consumed[slot] += amount;
            if (consumeIngredient(ingredients, crafts, ingredient, needed - amount, slot + 1, stacks, remaining, consumed,
                    ingredientIndex)) {
                return true;
            }
            consumed[slot] -= amount;
            remaining[slot] += amount;
        }
        return false;
    }

    private ShapelessMatch() {
    }
}
