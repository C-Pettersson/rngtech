package com.rngtech.util;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Value equality for ItemStacks held in records. ItemStack compares by identity, so a record component holding one never
 * equals its own decoded copy unless its equals and hashCode go through these.
 */
public final class ItemStackValues {
    private ItemStackValues() {
    }

    public static boolean matches(ItemStack first, ItemStack second) {
        return ItemStack.matches(first, second);
    }

    public static boolean matches(List<ItemStack> first, List<ItemStack> second) {
        if (first.size() != second.size()) {
            return false;
        }
        for (int index = 0; index < first.size(); index++) {
            if (!ItemStack.matches(first.get(index), second.get(index))) {
                return false;
            }
        }
        return true;
    }

    public static int hash(ItemStack stack) {
        return stack.isEmpty() ? 0 : 31 * ItemStack.hashItemAndComponents(stack) + stack.getCount();
    }

    public static int hash(List<ItemStack> stacks) {
        int hash = 1;
        for (ItemStack stack : stacks) {
            hash = 31 * hash + hash(stack);
        }
        return hash;
    }
}
