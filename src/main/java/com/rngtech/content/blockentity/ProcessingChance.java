package com.rngtech.content.blockentity;

import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

final class ProcessingChance {
    static boolean rollInstant(Level level, MachineStatAccumulator stats) {
        return rollPercent(level, stats.value(MachineStat.INSTANT_PROCESS_CHANCE));
    }

    static ItemStack applySuperOutput(Level level, MachineStatAccumulator stats, ItemStack output, ItemStack baseOutput) {
        if (output.isEmpty()
                || !canDuplicate(baseOutput)
                || !rollPercent(level, stats.value(MachineStat.SUPER_OUTPUT_CHANCE))) {
            return output;
        }
        ItemStack enhanced = output.copy();
        if (!ItemStack.isSameItemSameComponents(enhanced, baseOutput)) {
            return output;
        }
        int extra = baseOutput.getCount();
        int limit = enhanced.getMaxStackSize();
        if (enhanced.getCount() + extra > limit) {
            return output;
        }
        enhanced.grow(extra);
        return enhanced;
    }

    static ItemStack applyCrusherSalvage(Level level, MachineStatAccumulator stats, ItemStack output, ItemStack baseOutput) {
        if (output.isEmpty()
                || !canDuplicate(baseOutput)
                || !rollPercent(level, stats.value(MachineStat.CRUSHER_SALVAGE_CHANCE))) {
            return output;
        }
        if (!ItemStack.isSameItemSameComponents(output, baseOutput)) {
            return output;
        }
        ItemStack enhanced = output.copy();
        if (enhanced.getCount() + 1 > enhanced.getMaxStackSize()) {
            return output;
        }
        enhanced.grow(1);
        return enhanced;
    }

    static List<ItemStack> applySuperOutputs(Level level, MachineStatAccumulator stats, List<ItemStack> outputs) {
        if (outputs.isEmpty() || !rollPercent(level, stats.value(MachineStat.SUPER_OUTPUT_CHANCE))) {
            return outputs;
        }
        ArrayList<ItemStack> enhanced = new ArrayList<>(outputs.size() * 2);
        for (ItemStack output : outputs) {
            enhanced.add(output);
            if (canDuplicate(output)) {
                enhanced.add(output.copy());
            }
        }
        return List.copyOf(enhanced);
    }

    private static boolean rollPercent(Level level, double chancePercent) {
        return chancePercent > 0.0 && level.random.nextDouble() * 100.0 < chancePercent;
    }

    private static boolean canDuplicate(ItemStack stack) {
        return !stack.isEmpty() && stack.getMaxStackSize() > 1;
    }

    private ProcessingChance() {
    }
}
