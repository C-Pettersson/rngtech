package com.rngtech.content.blockentity;

import com.rngtech.rpg.MachineStatAccumulator;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Built machine stats, kept until one of their inputs changes. The inputs are two key objects compared by reference
 * (such as the traits and Mastery components, which are replaced rather than changed), a numeric key (such as the
 * Bulk Speed count), the tracked stacks, compared by reference and count, and a global generation bumped when the
 * config or tags reload. Stats never read randomness, the tick, or a stack's charge or wear, so nothing else can
 * change them.
 */
public final class MachineStatsCache {
    private static volatile int generation;

    private final List<TrackedSlots> tracked = new ArrayList<>();
    private ItemStack[] stacks = new ItemStack[0];
    private int[] counts = new int[0];
    private MachineStatAccumulator stats;
    private int builtGeneration;
    private Object builtKeyA;
    private Object builtKeyB;
    private long builtKeyC;
    private int builds;

    /** Rebuilds every machine's stats on next use. Called when the config or tags reload. */
    public static void invalidateAll() {
        generation++;
    }

    void track(ItemStackHandler inventory, int[] slots) {
        tracked.add(new TrackedSlots(inventory, slots));
        stats = null;
    }

    /**
     * The cached stats, or {@code builder}'s new ones when an input changed. The result is frozen, so callers that add
     * conditional effects copy it first. While a stat breakdown is recording, the stats are always built fresh.
     */
    MachineStatAccumulator get(Object keyA, Object keyB, long keyC, Supplier<MachineStatAccumulator> builder) {
        if (MachineStatAccumulator.isRecording()) {
            return builder.get();
        }
        if (stats == null
                || builtGeneration != generation
                || builtKeyA != keyA
                || builtKeyB != keyB
                || builtKeyC != keyC
                || stacksChanged()) {
            int buildGeneration = generation;
            MachineStatAccumulator built = builder.get().freeze();
            builds++;
            snapshotStacks();
            stats = built;
            builtGeneration = buildGeneration;
            builtKeyA = keyA;
            builtKeyB = keyB;
            builtKeyC = keyC;
        }
        return stats;
    }

    void invalidate() {
        stats = null;
    }

    /** How many times the stats were built, for checks that a machine does not rebuild them every tick. */
    int builds() {
        return builds;
    }

    private boolean stacksChanged() {
        int index = 0;
        for (TrackedSlots slots : tracked) {
            int count = slots.count();
            for (int slot = 0; slot < count; slot++) {
                if (index >= stacks.length) {
                    return true;
                }
                ItemStack stack = slots.stack(slot);
                if (stack != stacks[index] || stack.getCount() != counts[index]) {
                    return true;
                }
                index++;
            }
        }
        return index != stacks.length;
    }

    private void snapshotStacks() {
        int size = 0;
        for (TrackedSlots slots : tracked) {
            size += slots.count();
        }
        if (stacks.length != size) {
            stacks = new ItemStack[size];
            counts = new int[size];
        }
        int index = 0;
        for (TrackedSlots slots : tracked) {
            int count = slots.count();
            for (int slot = 0; slot < count; slot++) {
                ItemStack stack = slots.stack(slot);
                stacks[index] = stack;
                counts[index] = stack.getCount();
                index++;
            }
        }
    }

    /** Slots of one inventory; no slot list means every slot, so a resized inventory is tracked in full. */
    private record TrackedSlots(ItemStackHandler inventory, int[] slots) {
        int count() {
            return slots == null ? inventory.getSlots() : slots.length;
        }

        ItemStack stack(int index) {
            if (slots == null) {
                return inventory.getStackInSlot(index);
            }
            int slot = slots[index];
            return slot < inventory.getSlots() ? inventory.getStackInSlot(slot) : ItemStack.EMPTY;
        }
    }
}
