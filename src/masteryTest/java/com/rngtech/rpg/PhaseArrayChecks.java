package com.rngtech.rpg;

import java.util.List;

/** The Phase Array parts: pools deep enough to roll, every affix read by the line, and the PRD's base numbers. */
public final class PhaseArrayChecks {
    private static final int MIN_AFFIXES_PER_SLOT = 6;
    private static int checks;

    private PhaseArrayChecks() {
    }

    public static int run() {
        checks = 0;
        for (ModifierEligibilityProfile profile : List.of(ModifierEligibilityProfiles.PHASE_EMITTER, ModifierEligibilityProfiles.PHASE_COIL)) {
            poolsAreDeepEnough(profile);
            everyAffixIsRead(profile);
        }
        baseStatsMatchTheRules();
        positionFloorsClimb();
        return checks;
    }

    /** A Rare rolls three of each slot, so a pool with fewer than six per slot would hand every Rare the same affixes. */
    private static void poolsAreDeepEnough(ModifierEligibilityProfile profile) {
        for (ModifierSlot slot : List.of(ModifierSlot.PREFIX, ModifierSlot.SUFFIX)) {
            long count = profile.definitions().stream()
                    .filter(definition -> definition.slot() == slot && definition.flags().contains(ModifierFlag.CAN_ROLL))
                    .count();
            check(count >= MIN_AFFIXES_PER_SLOT, profile.id() + " rolls " + count + " " + slot + " affixes; at least "
                    + MIN_AFFIXES_PER_SLOT + " are needed");
        }
    }

    private static void everyAffixIsRead(ModifierEligibilityProfile profile) {
        for (ModifierDefinition definition : profile.definitions()) {
            for (ModifierEffectDefinition effect : definition.effects()) {
                check(PhaseArrayRules.LINE_STATS.contains(effect.stat()),
                        profile.id() + " affix " + definition.id() + " rolls " + effect.stat() + ", which the line never reads");
            }
        }
    }

    private static void baseStatsMatchTheRules() {
        MachineStatAccumulator emitter = MachineBaseStatCatalog.phaseEmitter();
        near(emitter.value(MachineStat.CHARGE_CAPACITY), PhaseArrayRules.EMITTER_CHARGE_CAPACITY, "Emitter Charge Capacity");
        near(emitter.value(MachineStat.CHARGE_RATE), PhaseArrayRules.EMITTER_CHARGE_RATE, "Emitter Charge Rate");
        near(emitter.value(MachineStat.PARTIAL_WINDOW), PhaseArrayRules.BASE_PARTIAL_WINDOW, "Emitter Partial Window");
        near(emitter.value(MachineStat.HOLD_CHANCE), 0, "Emitter has no Hold Chance");
        MachineStatAccumulator coil = MachineBaseStatCatalog.phaseCoil();
        near(coil.value(MachineStat.HOLD_CHANCE), PhaseArrayRules.COIL_HOLD_CHANCE, "Coil Hold Chance");
        near(coil.value(MachineStat.CHARGE_CAPACITY), PhaseArrayRules.COIL_CHARGE_CAPACITY, "Coil Charge Capacity");
        near(coil.value(MachineStat.STRAIN_TOLERANCE), 0, "Coil Strain Tolerance starts at 0");
    }

    private static void positionFloorsClimb() {
        int previous = 0;
        for (int position = 1; position <= PhaseArrayRules.MAX_COILS; position++) {
            int floor = PhaseArrayRules.lowestGlyphTier(position);
            check(floor >= previous && floor >= 1 && floor <= PhaseArrayRules.GLYPH_TIERS,
                    "Position " + position + " needs tier " + floor + ", below position " + (position - 1));
            previous = floor;
        }
        check(PhaseArrayRules.lowestGlyphTier(PhaseArrayRules.MAX_COILS) == PhaseArrayRules.GLYPH_TIERS,
                "The last position needs the top Glyph tier");
        check(PhaseArrayRules.lowestGlyphTier(PhaseArrayRules.MIN_COILS) == 1, "The first three positions need Tier I");
    }

    private static void near(double actual, double expected, String what) {
        check(Math.abs(actual - expected) < 1.0E-6, what + ": expected " + expected + ", got " + actual);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }
}
