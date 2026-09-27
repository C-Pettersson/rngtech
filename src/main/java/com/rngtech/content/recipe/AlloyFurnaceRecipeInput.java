package com.rngtech.content.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record AlloyFurnaceRecipeInput(ItemStack first, ItemStack second, ItemStack third, ItemStack fourth)
        implements RecipeInput {
    public AlloyFurnaceRecipeInput(ItemStack[] stacks) {
        this(slot(stacks, 0), slot(stacks, 1), slot(stacks, 2), slot(stacks, 3));
    }

    public ItemStack[] stacks() {
        return new ItemStack[] {first, second, third, fourth};
    }

    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> first;
            case 1 -> second;
            case 2 -> third;
            case 3 -> fourth;
            default -> throw new IllegalArgumentException("Recipe does not contain slot " + index);
        };
    }

    @Override
    public int size() {
        return 4;
    }

    private static ItemStack slot(ItemStack[] stacks, int index) {
        return index < stacks.length ? stacks[index] : ItemStack.EMPTY;
    }
}
