package com.rngtech.rpg;

/** Crusher yield: one soft-capped Output Amount bucket, paid for in cycle time. */
public final class CrusherYield {
    private CrusherYield() {
    }

    /** The soft-capped yield bonus in percent, with {@code atLevelPercent} joining the bucket before the cap. */
    public static double bonusPercent(MachineStatAccumulator stats, double atLevelPercent) {
        return Math.max(0.0, stats.effectiveIncreasedPercent(MachineStat.OUTPUT_AMOUNT, atLevelPercent));
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
