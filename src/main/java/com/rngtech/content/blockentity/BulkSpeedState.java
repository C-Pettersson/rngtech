package com.rngtech.content.blockentity;

import com.rngtech.rpg.MachineBehavior;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.ModifierOperation;
import com.rngtech.rpg.ModifierSlot;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

final class BulkSpeedState {
    private static final String TAG_BULK_SPEED_PROCESSES = "BulkSpeedProcesses";
    private static final int MAX_BONUS_PERCENT = 100;
    private static final Component BULK_SPEED_SOURCE = Component.translatable(MachineBehavior.BULK_SPEED.translationKey());

    private int completedProcesses;
    private Object chainRecipe;

    /**
     * Records the recipe of a craft that is starting. A different recipe than the last one breaks the chain. The recipe
     * is not saved, so the first craft after a load keeps the loaded count.
     */
    boolean startRecipe(Object recipe) {
        Object previous = chainRecipe;
        chainRecipe = recipe;
        return previous != null && !previous.equals(recipe) && reset();
    }

    void apply(MachineStatAccumulator stats, MachineTraits traits) {
        if (!traits.hasBehavior(MachineBehavior.BULK_SPEED) || completedProcesses <= 0) {
            return;
        }
        stats.apply(BULK_SPEED_SOURCE, new MachineModifier(
                ModifierSlot.IMPLICIT,
                MachineStat.PROCESSING_SPEED,
                ModifierOperation.INCREASED_PERCENT,
                completedProcesses
        ));
    }

    boolean recordProcess(MachineTraits traits) {
        if (!traits.hasBehavior(MachineBehavior.BULK_SPEED) || completedProcesses >= MAX_BONUS_PERCENT) {
            return false;
        }
        completedProcesses++;
        return true;
    }

    boolean recordProcesses(MachineTraits traits, int processes) {
        boolean changed = false;
        for (int process = 0; process < processes; process++) {
            changed |= recordProcess(traits);
        }
        return changed;
    }

    boolean reset() {
        if (completedProcesses == 0) {
            return false;
        }
        completedProcesses = 0;
        return true;
    }

    void save(CompoundTag tag) {
        tag.putInt(TAG_BULK_SPEED_PROCESSES, completedProcesses);
    }

    void load(CompoundTag tag) {
        completedProcesses = Mth.clamp(tag.getInt(TAG_BULK_SPEED_PROCESSES), 0, MAX_BONUS_PERCENT);
    }

    /** The last item seen in an input slot, so a top-up with the same item keeps the chain. */
    static final class InputWatch {
        private ItemStack last = ItemStack.EMPTY;

        /** Whether {@code current} replaced a different item or components. */
        boolean changedTo(ItemStack current) {
            if (current.isEmpty() || ItemStack.isSameItemSameComponents(last, current)) {
                return false;
            }
            boolean replaced = !last.isEmpty();
            last = current.copyWithCount(1);
            return replaced;
        }

        void prime(ItemStack current) {
            last = current.isEmpty() ? ItemStack.EMPTY : current.copyWithCount(1);
        }
    }
}
