package com.rngtech.rpg;

import java.util.EnumSet;
import java.util.Set;

/**
 * The Phase Array's fixed numbers: part base stats, line limits, and the Glyph tier each Coil position needs. They are the
 * {@code prd} scenario of {@code tools/balance/phase-array-scenarios.json}; change that file and rerun
 * {@code phaseArraySim} before changing a number here.
 */
public final class PhaseArrayRules {
    public static final int MIN_COILS = 3;
    public static final int MAX_COILS = 9;
    public static final int GLYPH_TIERS = 5;
    public static final int PART_STAGE = 8;

    public static final long EMITTER_CHARGE_CAPACITY = 16_000_000L;
    public static final int EMITTER_CHARGE_RATE = 8_000;
    public static final long COIL_CHARGE_CAPACITY = 4_000_000L;
    public static final int COIL_CHARGE_RATE = 2_000;
    /** Percent. */
    public static final double COIL_HOLD_CHANCE = 92.0;
    /** Coils short of an operation's count that still count as Partial. */
    public static final int BASE_PARTIAL_WINDOW = 2;

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
            MachineStat.STRAIN_MERCY
    );

    private PhaseArrayRules() {
    }

    /** The lowest Glyph tier a Coil at {@code position} (1 next to the Emitter) must carry for the line to use it. */
    public static int lowestGlyphTier(int position) {
        if (position <= 3) {
            return 1;
        }
        if (position <= 5) {
            return 2;
        }
        if (position <= 7) {
            return 3;
        }
        return position <= 8 ? 4 : 5;
    }

    public static int clampGlyphTier(int tier) {
        return Math.max(1, Math.min(GLYPH_TIERS, tier));
    }
}
