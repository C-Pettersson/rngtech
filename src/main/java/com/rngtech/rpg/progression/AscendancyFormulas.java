package com.rngtech.rpg.progression;

import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;

/** Launch ascendancy math that machines share with the domain checks. Stat values are read from final machine stats. */
public final class AscendancyFormulas {
    /** Under-Level Efficiency stops here, so every penalized level still costs something. */
    public static final double MAX_UNDER_LEVEL_EFFICIENCY = 90.0;
    public static final double REFINERS_OATH_PER_JOB = 0.05;
    public static final double REFINERS_OATH_CAP = 0.5;
    /** Shared Hearth gives every lane this share of the hottest installed Heat Core's maximum. */
    public static final double SHARED_HEARTH_SHARE = 0.9;
    /** Fluxed Blend lowers blend smelt temperatures by this many degrees. */
    public static final int FLUXED_BLEND_DEGREES = 100;

    private AscendancyFormulas() { }

    /** Missing hardness levels that still cost time, FE, and jam risk after Hardness Tolerance. */
    public static int penalizedDeficit(int deficit, MachineStatAccumulator stats) {
        return Math.max(0, deficit - stats.intValue(MachineStat.HARDNESS_TOLERANCE));
    }

    /** Time and FE multiplier for penalized levels, each reduced by Under-Level Efficiency. */
    public static double underLevelPenaltyMultiplier(int penalizedDeficit, double perLevel, MachineStatAccumulator stats) {
        double efficiency = Math.max(0.0, Math.min(MAX_UNDER_LEVEL_EFFICIENCY, stats.value(MachineStat.UNDER_LEVEL_EFFICIENCY)));
        return 1.0 + Math.max(0, penalizedDeficit) * Math.max(0.0, perLevel) * (1.0 - efficiency / 100.0);
    }

    public static int jamChancePerThousand(int penalizedDeficit, int perLevel, MachineStatAccumulator stats) {
        double chance = Math.max(0, penalizedDeficit) * (double) Math.max(0, perLevel) * Math.max(0.0, stats.value(MachineStat.JAM_CHANCE));
        return (int) Math.max(0, Math.min(1000, Math.round(chance)));
    }

    public static int jamTicks(int penalizedDeficit, int perLevel, MachineStatAccumulator stats) {
        double ticks = Math.max(0, penalizedDeficit) * (double) Math.max(0, perLevel)
                / (1.0 + Math.max(0.0, stats.value(MachineStat.JAM_RECOVERY)) / 100.0);
        return (int) Math.min(Integer.MAX_VALUE, Math.ceil(ticks));
    }

    /** Refiner's Oath: 5% more Output Amount per Parallel Job from every source, up to 50%. */
    public static double refinersOathMultiplier(int parallelJobs) {
        return 1.0 + Math.min(REFINERS_OATH_CAP, REFINERS_OATH_PER_JOB * Math.max(1, parallelJobs));
    }

    /** Overdrive's Processing Speed multiplier for a lane running {@code temperature} against a recipe target. */
    public static double overdriveSpeedMultiplier(int temperature, int target, MachineStatAccumulator stats) {
        int over = temperature - target;
        double cap = Math.max(0.0, stats.value(MachineStat.OVERDRIVE_CAP));
        if (over <= 0 || cap <= 0.0) {
            return 1.0;
        }
        return 1.0 + Math.min(cap, Math.max(0.0, stats.value(MachineStat.OVERDRIVE_SPEED)) * over / 10.0) / 100.0;
    }

    /** Items banked in the Bloom Ledger for one smelt; Crusher Line feeds crushed inputs twice. */
    public static double ledgerShare(MachineStatAccumulator stats, int baseCount, boolean doubled) {
        return Math.max(0.0, stats.value(MachineStat.LEDGER_RATE)) / 100.0 * Math.max(0, baseCount) * (doubled ? 2 : 1);
    }
}
