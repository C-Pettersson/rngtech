package com.rngtech.content.blockentity;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A machine's last recipe lookups, one per lookup slot (a Furnace lane, a stored Mold), including failed ones. An entry
 * holds until an input stack changes item or fluid, components, or amount, or the recipes reload. A reload replaces the
 * server's {@link RecipeManager}, and {@link #invalidateAll} covers the client's, which is updated in place. A lookup
 * whose inputs are all empty never searches.
 */
public final class RecipeCache<R> {
    private static volatile int generation;

    private final Lookup<R>[] entries;
    private int searches;

    /** Looks up a recipe for the given inputs, for example through a {@code *Recipes.find} helper. */
    @FunctionalInterface
    interface Search<R> {
        Optional<R> find(Level level, ItemStack first, ItemStack second);
    }

    @SuppressWarnings("unchecked")
    RecipeCache(int slots) {
        entries = new Lookup[slots];
        for (int slot = 0; slot < slots; slot++) {
            entries[slot] = new Lookup<>(this);
        }
    }

    /** Forgets every machine's lookups. Called when tags or recipes sync. */
    public static void invalidateAll() {
        generation++;
    }

    /** Changes on every {@link #invalidateAll}, for other caches of recipe results. */
    static int generation() {
        return generation;
    }

    /** The recipe for one input, or {@code null}. */
    R find(int slot, Level level, ItemStack input, Search<R> search) {
        return find(slot, level, input, ItemStack.EMPTY, search);
    }

    /** The recipe for two inputs, such as an ingot and a Mold, or {@code null}. */
    R find(int slot, Level level, ItemStack first, ItemStack second, Search<R> search) {
        Lookup<R> lookup = lookup(slot, level).item(first).item(second);
        return lookup.needsSearch() ? lookup.store(search.find(level, first, second)) : lookup.recipe;
    }

    /**
     * Starts a lookup in {@code slot} for any mix of item and fluid inputs. Add every input in the same order each time
     * with {@link Lookup#item} and {@link Lookup#fluid}, then call {@link Lookup#find}.
     */
    Lookup<R> lookup(int slot, Level level) {
        return entries[slot].start(level.getRecipeManager());
    }

    /** How many lookups missed the cache and searched the recipes, for checks that idle machines stop searching. */
    int searches() {
        return searches;
    }

    /** One lookup slot. It is reused for every lookup in that slot, so a lookup allocates nothing on a hit. */
    static final class Lookup<R> {
        private final RecipeCache<R> cache;
        private final List<Key> keys = new ArrayList<>();
        private RecipeManager manager;
        private int generation;
        private R recipe;
        private int cursor;
        private boolean changed;
        private boolean anyInput;

        private Lookup(RecipeCache<R> cache) {
            this.cache = cache;
        }

        private Lookup<R> start(RecipeManager currentManager) {
            int currentGeneration = RecipeCache.generation;
            changed = manager != currentManager || generation != currentGeneration;
            manager = currentManager;
            generation = currentGeneration;
            cursor = 0;
            anyInput = false;
            return this;
        }

        Lookup<R> item(ItemStack stack) {
            Key key = nextKey();
            if (!key.matches(stack)) {
                key.remember(stack);
                changed = true;
            }
            anyInput |= !stack.isEmpty();
            return this;
        }

        Lookup<R> fluid(FluidStack stack) {
            Key key = nextKey();
            if (!key.matches(stack)) {
                key.remember(stack);
                changed = true;
            }
            anyInput |= !stack.isEmpty();
            return this;
        }

        /** The remembered recipe, or the result of {@code search} when an input changed since the last lookup. */
        R find(Supplier<Optional<R>> search) {
            return needsSearch() ? store(search.get()) : recipe;
        }

        private boolean needsSearch() {
            if (cursor != keys.size()) {
                keys.subList(cursor, keys.size()).clear();
                changed = true;
            }
            if (changed && !anyInput) {
                recipe = null;
                changed = false;
            }
            return changed;
        }

        private R store(Optional<R> found) {
            recipe = found.orElse(null);
            changed = false;
            cache.searches++;
            return recipe;
        }

        private Key nextKey() {
            if (cursor == keys.size()) {
                keys.add(new Key());
                changed = true;
            }
            return keys.get(cursor++);
        }
    }

    /** One input, by item or fluid, components, and amount; recipes can need several of an item or a minimum fluid amount. */
    private static final class Key {
        private ItemStack item = ItemStack.EMPTY;
        private FluidStack fluid;
        private int amount;

        boolean matches(ItemStack current) {
            return fluid == null && current.getCount() == amount && ItemStack.isSameItemSameComponents(item, current);
        }

        boolean matches(FluidStack current) {
            return fluid != null && current.getAmount() == amount && FluidStack.isSameFluidSameComponents(fluid, current);
        }

        void remember(ItemStack current) {
            item = current.isEmpty() ? ItemStack.EMPTY : current.copyWithCount(1);
            fluid = null;
            amount = current.getCount();
        }

        void remember(FluidStack current) {
            item = ItemStack.EMPTY;
            fluid = current.isEmpty() ? FluidStack.EMPTY : current.copyWithAmount(1);
            amount = current.getAmount();
        }
    }
}
