package com.rngtech.content.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

final class ToolDamageRecipeSupport {
    private ToolDamageRecipeSupport() {
    }

    static boolean toolMatchesIgnoringDamage(Ingredient tool, ItemStack stack) {
        if (tool.test(stack)) {
            return true;
        }
        ItemStack undamaged = undamagedCopy(stack);
        return undamaged != stack && tool.test(undamaged);
    }

    static boolean ingredientMatchesIgnoringToolDamage(Ingredient ingredient, Ingredient tool, ItemStack stack) {
        if (ingredient.test(stack)) {
            return true;
        }
        ItemStack undamaged = undamagedToolCopy(tool, stack);
        return undamaged != stack && ingredient.test(undamaged);
    }

    private static ItemStack undamagedToolCopy(Ingredient tool, ItemStack stack) {
        ItemStack undamaged = undamagedCopy(stack);
        return undamaged != stack && tool.test(undamaged) ? undamaged : stack;
    }

    private static ItemStack undamagedCopy(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem() || stack.getDamageValue() == 0) {
            return stack;
        }
        ItemStack undamaged = stack.copyWithCount(1);
        undamaged.setDamageValue(0);
        return undamaged;
    }
}
