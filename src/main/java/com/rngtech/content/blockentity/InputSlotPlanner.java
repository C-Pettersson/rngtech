package com.rngtech.content.blockentity;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Decides which input slots automation may fill, so one ingredient never takes the slots another ingredient needs.
 * The answer depends only on what the slots hold, never on which item arrived first.
 *
 * <p>Shapeless machines pool their input slots, so an item never needs a second slot to craft. An item may open an
 * empty slot only if every recipe that could still be completed before stays completable after: a recipe is
 * completable while each held item fits one of its ingredients and enough empty slots remain for the ingredients
 * nothing holds yet. Positional machines need ingredient {@code i} in slot {@code i}, so a slot takes an item only if
 * some recipe needs it there and agrees with every other filled slot.
 *
 * <p>Each planner remembers its answers for stacks without component changes, on the server, until the recipes
 * reload. Keep one planner per machine type, for example in a static field, so the remembered answers are shared.
 */
final class InputSlotPlanner<I extends RecipeInput, R extends Recipe<I>> {
    private static final int MAX_REMEMBERED = 4096;

    private final Supplier<RecipeType<R>> type;
    private final Function<R, List<Ingredient>> ingredients;
    private final boolean positional;
    private final Map<Key, Boolean> answers = new ConcurrentHashMap<>();
    private volatile List<List<Ingredient>> recipes = List.of();
    private volatile RecipeManager manager;
    private volatile int generation;

    private InputSlotPlanner(Supplier<RecipeType<R>> type, Function<R, List<Ingredient>> ingredients, boolean positional) {
        this.type = type;
        this.ingredients = ingredients;
        this.positional = positional;
    }

    static <I extends RecipeInput, R extends Recipe<I>> InputSlotPlanner<I, R> shapeless(
            Supplier<RecipeType<R>> type,
            Function<R, List<Ingredient>> ingredients
    ) {
        return new InputSlotPlanner<>(type, ingredients, false);
    }

    static <I extends RecipeInput, R extends Recipe<I>> InputSlotPlanner<I, R> positional(
            Supplier<RecipeType<R>> type,
            Function<R, List<Ingredient>> ingredients
    ) {
        return new InputSlotPlanner<>(type, ingredients, true);
    }

    /** Whether automation may add {@code incoming} to {@code slots[target]} without starving another ingredient. */
    boolean accepts(Level level, ItemStack[] slots, int target, ItemStack incoming) {
        if (level == null || incoming.isEmpty() || target < 0 || target >= slots.length) {
            return false;
        }
        ItemStack held = slots[target];
        if (!held.isEmpty() && !ItemStack.isSameItemSameComponents(held, incoming)) {
            return false;
        }
        if (!positional && !held.isEmpty()) {
            return true;
        }
        if (level.isClientSide()) {
            return decide(load(level.getRecipeManager()), slots, target, incoming);
        }
        List<List<Ingredient>> known = recipes(level.getRecipeManager());
        if (!componentFree(slots, incoming)) {
            return decide(known, slots, target, incoming);
        }
        Key key = Key.of(slots, target, incoming);
        Boolean answer = answers.get(key);
        if (answer == null) {
            if (answers.size() >= MAX_REMEMBERED) {
                answers.clear();
            }
            answer = decide(known, slots, target, incoming);
            answers.put(key, answer);
        }
        return answer;
    }

    private boolean decide(List<List<Ingredient>> known, ItemStack[] slots, int target, ItemStack incoming) {
        ItemStack[] after = slots.clone();
        after[target] = incoming;
        if (positional) {
            return known.stream().anyMatch(recipe -> fitsPositions(recipe, after));
        }
        boolean completable = false;
        for (List<Ingredient> recipe : known) {
            if (!isCompletable(recipe, slots)) {
                continue;
            }
            boolean stillCompletable = isCompletable(recipe, after);
            if (!stillCompletable && everyItemFits(recipe, after)) {
                return false;
            }
            completable |= stillCompletable;
        }
        return completable;
    }

    private static boolean fitsPositions(List<Ingredient> recipe, ItemStack[] slots) {
        for (int slot = 0; slot < slots.length; slot++) {
            if (!slots[slot].isEmpty() && (slot >= recipe.size() || !recipe.get(slot).test(slots[slot]))) {
                return false;
            }
        }
        return recipe.size() <= slots.length;
    }

    private static boolean isCompletable(List<Ingredient> recipe, ItemStack[] slots) {
        if (!everyItemFits(recipe, slots)) {
            return false;
        }
        int empty = 0;
        for (ItemStack stack : slots) {
            if (stack.isEmpty()) {
                empty++;
            }
        }
        int missing = 0;
        for (Ingredient ingredient : recipe) {
            if (!anyHeld(ingredient, slots)) {
                missing++;
            }
        }
        return missing <= empty;
    }

    private static boolean everyItemFits(List<Ingredient> recipe, ItemStack[] slots) {
        for (ItemStack stack : slots) {
            if (!stack.isEmpty() && recipe.stream().noneMatch(ingredient -> ingredient.test(stack))) {
                return false;
            }
        }
        return true;
    }

    private static boolean anyHeld(Ingredient ingredient, ItemStack[] slots) {
        for (ItemStack stack : slots) {
            if (!stack.isEmpty() && ingredient.test(stack)) {
                return true;
            }
        }
        return false;
    }

    private List<List<Ingredient>> recipes(RecipeManager currentManager) {
        int currentGeneration = RecipeCache.generation();
        if (manager != currentManager || generation != currentGeneration) {
            answers.clear();
            recipes = load(currentManager);
            manager = currentManager;
            generation = currentGeneration;
        }
        return recipes;
    }

    private List<List<Ingredient>> load(RecipeManager recipeManager) {
        List<List<Ingredient>> loaded = new ArrayList<>();
        recipeManager.getAllRecipesFor(type.get()).forEach(holder -> loaded.add(List.copyOf(ingredients.apply(holder.value()))));
        return List.copyOf(loaded);
    }

    private static boolean componentFree(ItemStack[] slots, ItemStack incoming) {
        if (!incoming.isComponentsPatchEmpty()) {
            return false;
        }
        for (ItemStack stack : slots) {
            if (!stack.isEmpty() && !stack.isComponentsPatchEmpty()) {
                return false;
            }
        }
        return true;
    }

    private record Key(List<Item> slots, int target, Item incoming) {
        static Key of(ItemStack[] stacks, int target, ItemStack incoming) {
            List<Item> items = new ArrayList<>(stacks.length);
            for (ItemStack stack : stacks) {
                items.add(stack.isEmpty() ? Items.AIR : stack.getItem());
            }
            return new Key(List.copyOf(items), target, incoming.getItem());
        }
    }
}
