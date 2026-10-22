package com.rngtech.rpg;

import com.rngtech.RNGTechConfig;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The Phase Array's numbers: part base stats, line limits, the price curve, and the Glyph tier each Coil position
 * needs. The defaults are the {@code prd} scenario of {@code tools/balance/phase-array-scenarios.json}; change that file
 * and rerun {@code phaseArraySim} before changing one here. A pack retunes every balance number under
 * {@code phaseArray.*} in {@code rngtech-common.toml}; the line's shape (three to nine Coils, five Glyph tiers) and the
 * operations' own costs (their recipes) live elsewhere.
 */
public final class PhaseArrayRules {
    public static final int MIN_COILS = 3;
    public static final int MAX_COILS = 9;
    public static final int GLYPH_TIERS = 5;
    public static final int PART_STAGE = 8;

    public static final long DEFAULT_EMITTER_CHARGE_CAPACITY = 16_000_000L;
    public static final int DEFAULT_EMITTER_CHARGE_RATE = 8_000;
    public static final long DEFAULT_COIL_CHARGE_CAPACITY = 4_000_000L;
    public static final int DEFAULT_COIL_CHARGE_RATE = 2_000;
    /** Percent. */
    public static final double DEFAULT_COIL_HOLD_CHANCE = 92.0;
    /** Percent: the chance a surplus Coil's hold triggers Overhold, before affixes. */
    public static final double DEFAULT_COIL_OVERHOLD_CHANCE = 8.0;
    /** Coils short of an operation's count that still count as Partial. */
    public static final int DEFAULT_PARTIAL_WINDOW = 2;
    /** Fracture chance per point of Strain above the line's Tolerance, as a fraction. */
    public static final double DEFAULT_FRACTURE_PER_STRAIN = 0.01;
    /** PRD, Costs: {@code FE = base x stage x QUALITY_BASE ^ (Quality / QUALITY_DIVISOR)}. */
    public static final double DEFAULT_QUALITY_BASE = 1.5;
    public static final double DEFAULT_QUALITY_DIVISOR = 3.5;
    /** The lowest Glyph tier each position needs, position 1 first. */
    public static final List<Integer> DEFAULT_GLYPH_FLOORS = List.of(1, 1, 1, 2, 2, 3, 3, 4, 5);

    /** Every stat the line reads from its parts. A part affix on any other stat would do nothing. */
    public static final Set<MachineStat> LINE_STATS = EnumSet.of(
            MachineStat.CHARGE_CAPACITY,
            MachineStat.CHARGE_RATE,
            MachineStat.HOLD_CHANCE,
            MachineStat.SECOND_HOLD,
            MachineStat.OVERHOLD_CHANCE,
            MachineStat.PARTIAL_WINDOW,
            MachineStat.STRAIN_TOLERANCE,
            MachineStat.FE_THRIFT,
            MachineStat.FE_RECOVERY,
            MachineStat.GLYPH_SALVAGE,
            MachineStat.STRAIN_MERCY,
            MachineStat.PRIMED_CHARGE_RATE
    );

    private PhaseArrayRules() {
    }

    /** The configured value, or the default while no config is loaded, as in the domain checks and the simulation. */
    private static <T> T configured(ModConfigSpec.ConfigValue<T> value, T fallback) {
        return RNGTechConfig.SPEC.isLoaded() ? value.get() : fallback;
    }

    public static long emitterChargeCapacity() {
        return configured(RNGTechConfig.PHASE_EMITTER_CHARGE_CAPACITY, DEFAULT_EMITTER_CHARGE_CAPACITY);
    }

    public static int emitterChargeRate() {
        return configured(RNGTechConfig.PHASE_EMITTER_CHARGE_RATE, DEFAULT_EMITTER_CHARGE_RATE);
    }

    public static long coilChargeCapacity() {
        return configured(RNGTechConfig.PHASE_COIL_CHARGE_CAPACITY, DEFAULT_COIL_CHARGE_CAPACITY);
    }

    public static int coilChargeRate() {
        return configured(RNGTechConfig.PHASE_COIL_CHARGE_RATE, DEFAULT_COIL_CHARGE_RATE);
    }

    /** Percent. */
    public static double coilHoldChance() {
        return configured(RNGTechConfig.PHASE_COIL_HOLD_CHANCE, DEFAULT_COIL_HOLD_CHANCE);
    }

    /** Percent. */
    public static double coilOverholdChance() {
        return configured(RNGTechConfig.PHASE_COIL_OVERHOLD_CHANCE, DEFAULT_COIL_OVERHOLD_CHANCE);
    }

    public static int basePartialWindow() {
        return configured(RNGTechConfig.PHASE_PARTIAL_WINDOW, DEFAULT_PARTIAL_WINDOW);
    }

    /** As a fraction per point of Strain. */
    public static double fracturePerStrain() {
        return RNGTechConfig.SPEC.isLoaded() ? RNGTechConfig.PHASE_FRACTURE_PER_STRAIN.get() / 100.0 : DEFAULT_FRACTURE_PER_STRAIN;
    }

    public static double qualityBase() {
        return configured(RNGTechConfig.PHASE_QUALITY_BASE, DEFAULT_QUALITY_BASE);
    }

    public static double qualityDivisor() {
        return configured(RNGTechConfig.PHASE_QUALITY_DIVISOR, DEFAULT_QUALITY_DIVISOR);
    }

    /** The price factor for an item's stage: cheap at low stages, 1.5 at Stage 8 by default. */
    public static double stageFactor(int stage) {
        if (stage <= 2) {
            return configured(RNGTechConfig.PHASE_STAGE_FACTOR_0_TO_2, 0.1);
        }
        if (stage <= 4) {
            return configured(RNGTechConfig.PHASE_STAGE_FACTOR_3_TO_4, 0.25);
        }
        if (stage <= 6) {
            return configured(RNGTechConfig.PHASE_STAGE_FACTOR_5_TO_6, 0.5);
        }
        return stage == 7 ? configured(RNGTechConfig.PHASE_STAGE_FACTOR_7, 1.0) : configured(RNGTechConfig.PHASE_STAGE_FACTOR_8, 1.5);
    }

    /** PRD, Costs: the FE one pulse costs on an item of {@code stage} and {@code quality}, before Thrift. */
    public static long pulseCost(long baseFe, int stage, int quality) {
        return Math.round(baseFe * stageFactor(stage) * Math.pow(qualityBase(), quality / qualityDivisor()));
    }

    /** The lowest Glyph tier a Coil at {@code position} (1 next to the Emitter) must carry for the line to use it. */
    public static int lowestGlyphTier(int position) {
        List<? extends Integer> floors = RNGTechConfig.SPEC.isLoaded() ? RNGTechConfig.PHASE_GLYPH_FLOORS.get() : DEFAULT_GLYPH_FLOORS;
        if (floors.size() != MAX_COILS) {
            floors = DEFAULT_GLYPH_FLOORS;
        }
        int index = Math.max(1, Math.min(MAX_COILS, position)) - 1;
        return clampGlyphTier(floors.get(index));
    }

    public static int clampGlyphTier(int tier) {
        return Math.max(1, Math.min(GLYPH_TIERS, tier));
    }
}
