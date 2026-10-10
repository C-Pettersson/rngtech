package com.rngtech.rpg.progression;

import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;

/** Launch ascendancy math that machines share with the domain checks. Stat values are read from final machine stats. */
public final class AscendancyFormulas {
    /** Under-Level Efficiency stops here, so every penalized level still costs something. */
    public static final double MAX_UNDER_LEVEL_EFFICIENCY = 90.0;
    /** Fused Crucibles turns batching off for this much Processing Speed per point of Batch Size. */
    public static final double FUSED_CRUCIBLES_SPEED_PER_BATCH = 0.2;
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

    /** Jam chance in thousandths: the under-level chance plus any Jam Chance per Cycle, both scaled by Jam Chance. */
    public static int jamChancePerThousand(int penalizedDeficit, int perLevel, MachineStatAccumulator stats) {
        double underLevel = Math.max(0, penalizedDeficit) * (double) Math.max(0, perLevel);
        double everyCycle = Math.max(0.0, stats.value(MachineStat.CYCLE_JAM_CHANCE)) * 10.0;
        double chance = (underLevel + everyCycle) * Math.max(0.0, stats.value(MachineStat.JAM_CHANCE));
        return (int) Math.max(0, Math.min(1000, Math.round(chance)));
    }

    /** Jam length; a jam from Jam Chance per Cycle on an at-level recipe lasts as long as a one-level jam. */
    public static int jamTicks(int penalizedDeficit, int perLevel, MachineStatAccumulator stats) {
        int levels = Math.max(Math.max(0, penalizedDeficit), stats.value(MachineStat.CYCLE_JAM_CHANCE) > 0.0 ? 1 : 0);
        double ticks = levels * (double) Math.max(0, perLevel)
                / (1.0 + Math.max(0.0, stats.value(MachineStat.JAM_RECOVERY)) / 100.0);
        return (int) Math.min(Integer.MAX_VALUE, Math.ceil(ticks));
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

    /** Units of the largest input banked by Flux for one alloy craft; Reactive Flux doubles it on direct-ingot recipes. */
    public static double fluxShare(MachineStatAccumulator stats, boolean doubled) {
        return Math.max(0.0, stats.value(MachineStat.FLUX_RATE)) / 100.0 * (doubled ? 2 : 1);
    }

    /**
     * The ingredient an input saving may skip: the one with the largest count, first on a tie, and only when it needs at
     * least two units, so no saving removes an input entirely. Returns -1 when there is none.
     */
    public static int savableIngredient(int[] counts) {
        int best = -1;
        for (int index = 0; index < counts.length; index++) {
            if (counts[index] >= 2 && (best < 0 || counts[index] > counts[best])) {
                best = index;
            }
        }
        return best;
    }

    /** The Resonant Streak's stability floor bonus: Streak Floor per earlier calibration, up to the Streak Cap. */
    public static int streakFloor(int streak, MachineStatAccumulator stats) {
        return (int) Math.min(Math.max(0.0, stats.value(MachineStat.STREAK_CAP)), Math.max(0, streak) * Math.max(0.0, stats.value(MachineStat.STREAK_FLOOR)));
    }

    /**
     * Echo Streak: a calibration of another family is a detour the streak waits out, once per streak, unless Pattern
     * Memory would carry the streak over instead.
     */
    public static boolean echoDetour(int streak, String family, String nextFamily, boolean echoUsed, boolean patternMemoryCarries) {
        return streak > 0 && !echoUsed && !family.equals(nextFamily) && !patternMemoryCarries;
    }

    /** Fluid made by one melt: the recipe amount plus Fluid Yield, rounded down to whole millibuckets. */
    public static int yieldedFluid(int amount, double yieldPercent) {
        int base = Math.max(0, amount);
        return base + (int) Math.floor(base * Math.max(0.0, yieldPercent) / 100.0 + 1.0E-9);
    }

    /** How far a recipe's minimum and safe maximum sit from its target, as a share of the authored window. */
    public static double heatWindowScale(MachineStatAccumulator stats) {
        return Math.max(0.1, 1.0 + stats.value(MachineStat.HEAT_WINDOW) / 100.0);
    }

    /** A window edge moved toward or away from the target by the Heat Window scale. */
    public static int windowEdge(int target, int edge, MachineStatAccumulator stats) {
        return (int) Math.round(target + (edge - target) * heatWindowScale(stats));
    }

    /** Bone meal uses in one growth pulse: the whole part of Growth Pulse, plus one more when {@code roll} falls under the rest. */
    public static int pulseAttempts(MachineStatAccumulator stats, double roll) {
        double strength = Math.max(0.0, stats.value(MachineStat.GROWTH_PULSE));
        int whole = (int) Math.floor(strength + 1.0E-9);
        return whole + (roll < strength - whole - 1.0E-9 ? 1 : 0);
    }

    /** Ticks without work after which Idle Cart Speed applies: 4 seconds. */
    public static final int IDLE_CART_TICKS = 80;

    /** Extra increased Cart Speed once the cart has gone {@link #IDLE_CART_TICKS} without work. */
    public static double idleCartSpeedPercent(MachineStatAccumulator stats, int ticksSinceWork) {
        return ticksSinceWork >= IDLE_CART_TICKS ? Math.max(0.0, stats.value(MachineStat.IDLE_CART_SPEED)) : 0.0;
    }

    /** Items banked in the Bloom Ledger for one smelt; Crusher Line feeds crushed inputs twice. */
    public static double ledgerShare(MachineStatAccumulator stats, int baseCount, boolean doubled) {
        return Math.max(0.0, stats.value(MachineStat.LEDGER_RATE)) / 100.0 * Math.max(0, baseCount) * (doubled ? 2 : 1);
    }
}
