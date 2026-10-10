package com.rngtech.rpg;

import net.minecraft.network.chat.Component;

/** Crusher yield: one soft-capped Output Amount bucket, paid for in cycle time. */
public final class CrusherYield {
    /** Labels the yield cost where the Stats tab shows it as less Processing Speed. */
    public static final Component SPEED_SOURCE = Component.translatable("rngtech.stat.breakdown.source.yield");

    private CrusherYield() {
    }

    /** The soft-capped yield bonus in percent, with {@code atLevelPercent} joining the bucket before the cap. */
    public static double bonusPercent(MachineStatAccumulator stats, double atLevelPercent) {
        return Math.max(0.0, stats.effectiveIncreasedPercent(MachineStat.OUTPUT_AMOUNT, atLevelPercent));
    }

    /** The yield cost as a Processing Speed factor, for recipes that get bonus output and are not at the head's hardness. */
    public static double speedFactor(MachineStatAccumulator stats) {
        return 1.0 / (1.0 + bonusPercent(stats, 0.0) / 100.0);
    }

    /**
     * One job's ticks after Processing Speed, lengthened by the yield bonus. Output and time grow by the same factor, so
     * yield alone never raises items per tick above a machine without it.
     */
    public static int jobTicks(MachineStatAccumulator stats, int recipeTicks, double bonusPercent) {
        double ticks = stats.adjustedProcessingTicks(recipeTicks) * (1.0 + Math.max(0.0, bonusPercent) / 100.0);
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, (long) Math.ceil(ticks - 1.0E-9)));
    }
}
