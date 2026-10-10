package com.rngtech.content.blockentity;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * A machine's last recipe lookups, one per lookup slot (a Furnace lane, a stored Mold), including failed ones. An entry
 * holds until an input stack changes item, components, or count, or the recipes reload. A reload replaces the server's
 * {@link RecipeManager}, and {@link #invalidateAll} covers the client's, which is updated in place.
 */
public final class RecipeCache<R> {
    private static volatile int generation;

    private final Entry<R>[] entries;
    private int searches;

    /** Looks up a recipe for the given inputs, for example through a {@code *Recipes.find} helper. */
    @FunctionalInterface
    interface Search<R> {
        Optional<R> find(Level level, ItemStack first, ItemStack second);
    }

    @SuppressWarnings("unchecked")
    RecipeCache(int slots) {
        entries = new Entry[slots];
        for (int slot = 0; slot < slots; slot++) {
            entries[slot] = new Entry<>();
        }
    }

    /** Forgets every machine's lookups. Called when tags or recipes sync. */
    public static void invalidateAll() {
        generation++;
    }

    /** The recipe for one input, or {@code null}. */
    R find(int slot, Level level, ItemStack input, Search<R> search) {
        return find(slot, level, input, ItemStack.EMPTY, search);
    }

    /** The recipe for two inputs, such as an ingot and a Mold, or {@code null}. */
    R find(int slot, Level level, ItemStack first, ItemStack second, Search<R> search) {
        Entry<R> entry = entries[slot];
        RecipeManager manager = level.getRecipeManager();
        if (entry.manager != manager
                || entry.generation != generation
                || !entry.first.matches(first)
                || !entry.second.matches(second)) {
            entry.generation = generation;
            entry.manager = manager;
            entry.recipe = search.find(level, first, second).orElse(null);
            entry.first.remember(first);
            entry.second.remember(second);
            searches++;
        }
        return entry.recipe;
    }

    /** How many lookups missed the cache and searched the recipes, for checks that idle machines stop searching. */
    int searches() {
        return searches;
    }

    private static final class Entry<R> {
        private final Key first = new Key();
        private final Key second = new Key();
        private RecipeManager manager;
        private int generation;
        private R recipe;
    }

    /** One input, by item, components, and count; recipes can need several of an item. */
    private static final class Key {
        private ItemStack stack = ItemStack.EMPTY;
        private int count;

        boolean matches(ItemStack current) {
            return current.getCount() == count && ItemStack.isSameItemSameComponents(stack, current);
        }

        void remember(ItemStack current) {
            stack = current.isEmpty() ? ItemStack.EMPTY : current.copyWithCount(1);
            count = current.getCount();
        }
    }
}
