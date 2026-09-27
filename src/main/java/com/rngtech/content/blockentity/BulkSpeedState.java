package com.rngtech.content.blockentity;

import com.rngtech.rpg.MachineBehavior;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.ModifierOperation;
import com.rngtech.rpg.ModifierSlot;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

final class BulkSpeedState {
    private static final String TAG_BULK_SPEED_PROCESSES = "BulkSpeedProcesses";
    private static final int MAX_BONUS_PERCENT = 100;

    private int completedProcesses;

    void apply(MachineStatAccumulator stats, MachineTraits traits) {
        if (!traits.hasBehavior(MachineBehavior.BULK_SPEED) || completedProcesses <= 0) {
            return;
        }
        stats.apply(new MachineModifier(
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
}
