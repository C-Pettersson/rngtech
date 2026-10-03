package com.rngtech.content.blockentity;

import com.rngtech.content.recipe.BonusOutputRecipe;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

final class ProcessingChance {
    static boolean rollInstant(Level level, MachineStatAccumulator stats) {
        return rollPercent(level, stats.value(MachineStat.INSTANT_PROCESS_CHANCE));
    }

    static ItemStack applySuperOutput(Level level, MachineStatAccumulator stats, BonusOutputRecipe recipe, ItemStack output, ItemStack baseOutput) {
        if (output.isEmpty()
                || !recipe.allowsBonusOutput()
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

    static boolean rollSuperOutput(Level level, MachineStatAccumulator stats, BonusOutputRecipe recipe, ItemStack baseOutput) {
        return recipe.allowsBonusOutput() && canDuplicate(baseOutput) && rollPercent(level, stats.value(MachineStat.SUPER_OUTPUT_CHANCE));
    }

    /** Crusher salvage, rolled at {@code chanceScale} of the Salvage chance. */
    static boolean rollCrusherSalvage(Level level, MachineStatAccumulator stats, BonusOutputRecipe recipe, ItemStack baseOutput, double chanceScale) {
        return recipe.allowsBonusOutput() && canDuplicate(baseOutput)
                && rollPercent(level, stats.value(MachineStat.CRUSHER_SALVAGE_CHANCE) * chanceScale);
    }

    /** {@code output} with {@code extra} more of the same item, or unchanged when it would not stack. */
    static ItemStack grow(ItemStack output, ItemStack baseOutput, int extra) {
        if (output.isEmpty() || extra <= 0 || !ItemStack.isSameItemSameComponents(output, baseOutput)
                || output.getCount() + extra > output.getMaxStackSize()) {
            return output;
        }
        return output.copyWithCount(output.getCount() + extra);
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
