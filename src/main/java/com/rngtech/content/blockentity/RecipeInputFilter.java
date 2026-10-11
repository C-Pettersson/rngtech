package com.rngtech.content.blockentity;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

/**
 * Keeps automation from inserting items that no recipe of a machine can use, so they cannot jam an input slot. Each
 * filter remembers its answer per item for stacks without component changes, on the server, until the recipes reload.
 * Keep one filter per rule, for example in a static field, so the remembered answers are shared.
 */
final class RecipeInputFilter<I extends RecipeInput, R extends Recipe<I>> {
    private final Supplier<RecipeType<R>> type;
    private final BiPredicate<R, ItemStack> accepts;
    private final Map<Item, Boolean> plainItems = new ConcurrentHashMap<>();
    private volatile RecipeManager manager;
    private volatile int generation;

    RecipeInputFilter(Supplier<RecipeType<R>> type, BiPredicate<R, ItemStack> accepts) {
        this.type = type;
        this.accepts = accepts;
    }

    boolean accepts(Level level, ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return false;
        }
        RecipeManager currentManager = level.getRecipeManager();
        if (level.isClientSide() || !stack.isComponentsPatchEmpty()) {
            return search(currentManager, stack);
        }
        int currentGeneration = RecipeCache.generation();
        if (manager != currentManager || generation != currentGeneration) {
            plainItems.clear();
            manager = currentManager;
            generation = currentGeneration;
        }
        Boolean known = plainItems.get(stack.getItem());
        if (known == null) {
            known = search(currentManager, stack);
            plainItems.put(stack.getItem(), known);
        }
        return known;
    }

    private boolean search(RecipeManager recipes, ItemStack stack) {
        return recipes.getAllRecipesFor(type.get()).stream()
                .anyMatch(holder -> accepts.test(holder.value(), stack));
    }
}
