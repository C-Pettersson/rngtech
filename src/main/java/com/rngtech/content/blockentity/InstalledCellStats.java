package com.rngtech.content.blockentity;

import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.BatteryCellItem.CellStats;
import com.rngtech.content.registry.ModDataComponents;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The stats of the Battery Cells in a machine's slots, kept per slot until that slot's stack, its count, its traits
 * component, or its stripped flag changes, or the config or tags reload. Components are replaced rather than changed,
 * so comparing them by reference catches a refined cell; a cell's charge never affects its stats.
 */
final class InstalledCellStats {
    private ItemStack[] stacks = new ItemStack[0];
    private int[] counts = new int[0];
    private Object[] traits = new Object[0];
    private Object[] stripped = new Object[0];
    private CellStats[] stats = new CellStats[0];
    private int builtGeneration;
    private long version;
    private int builds;

    /**
     * Brings every slot up to date and returns a version that changes whenever any slot's stats were rebuilt, so
     * values derived from all the cells can be kept until it changes.
     */
    long refresh(IItemHandler inventory) {
        int slots = inventory.getSlots();
        int generation = MachineStatsCache.generation();
        if (stacks.length != slots || builtGeneration != generation) {
            resize(slots);
            builtGeneration = generation;
            version++;
        }
        for (int slot = 0; slot < slots; slot++) {
            refreshSlot(slot, inventory.getStackInSlot(slot));
        }
        return version;
    }

    /** For a machine with one cell slot: brings that slot up to date with {@code stack}, its current stack, and returns its stats. */
    CellStats refresh(ItemStack stack) {
        int generation = MachineStatsCache.generation();
        if (stacks.length != 1 || builtGeneration != generation) {
            resize(1);
            builtGeneration = generation;
            version++;
        }
        refreshSlot(0, stack);
        return stats[0];
    }

    /** The stats of {@code slot} as of the last {@link #refresh}. */
    CellStats get(int slot) {
        return slot >= 0 && slot < stats.length && stats[slot] != null ? stats[slot] : CellStats.NONE;
    }

    /** How many cell stats were built, for checks that a steady machine builds none. */
    int builds() {
        return builds;
    }

    private void refreshSlot(int slot, ItemStack stack) {
        Object stackTraits = stack.get(ModDataComponents.MACHINE_TRAITS.get());
        Object stackStripped = stack.get(ModDataComponents.RECYCLING_STRIPPED.get());
        if (stats[slot] != null
                && stack == stacks[slot]
                && stack.getCount() == counts[slot]
                && stackTraits == traits[slot]
                && stackStripped == stripped[slot]) {
            return;
        }
        stacks[slot] = stack;
        counts[slot] = stack.getCount();
        traits[slot] = stackTraits;
        stripped[slot] = stackStripped;
        stats[slot] = build(stack);
        version++;
    }

    private CellStats build(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BatteryCellItem)) {
            return CellStats.NONE;
        }
        builds++;
        return BatteryCellItem.stats(stack);
    }

    private void resize(int slots) {
        stacks = new ItemStack[slots];
        counts = new int[slots];
        traits = new Object[slots];
        stripped = new Object[slots];
        stats = new CellStats[slots];
    }
}
