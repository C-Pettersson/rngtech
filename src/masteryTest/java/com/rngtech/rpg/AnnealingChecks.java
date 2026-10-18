package com.rngtech.rpg;

import net.minecraft.util.RandomSource;

/** Annealed items roll Refinement Potential twice and keep the higher, without changing anything else they roll. */
public final class AnnealingChecks {
    private static final int SAMPLES = 4000;
    private static int checks;

    private AnnealingChecks() {
    }

    public static int run() {
        checks = 0;
        luckyRefinementPotentialChangesOnlyRp();
        luckyRefinementPotentialMatchesBestOfTwo();
        return checks;
    }

    private static void luckyRefinementPotentialChangesOnlyRp() {
        ModifierEligibilityProfile profile = ModifierEligibilityProfiles.forBatteryCell(false);
        for (long seed = 0; seed < 200; seed++) {
            MachineTraits plain = MachineTraitRoller.roll(profile, 6, RandomSource.create(seed), false);
            MachineTraits lucky = MachineTraitRoller.roll(profile, 6, RandomSource.create(seed), true);
            require(plain.rarity() == lucky.rarity() && plain.modifiers().equals(lucky.modifiers()),
                    "annealing changes only RP (seed " + seed + ")");
            require(lucky.refinementPotential() >= plain.refinementPotential(), "annealed RP never rolls lower (seed " + seed + ")");
            require(lucky.refinementPotential() >= 14 && lucky.refinementPotential() <= 22, "annealed RP stays in the Stage 5-6 range");
        }
    }

    private static void luckyRefinementPotentialMatchesBestOfTwo() {
        ModifierEligibilityProfile profile = ModifierEligibilityProfiles.forBatteryCell(false);
        RandomSource random = RandomSource.create(42L);
        double total = 0;
        for (int sample = 0; sample < SAMPLES; sample++) {
            total += MachineTraitRoller.roll(profile, 6, random, true).refinementPotential();
        }
        double mean = total / SAMPLES;
        // Best of two uniform rolls from 14 to 22 averages about 19.5, against 18 for one roll.
        require(Math.abs(mean - 19.48) < 0.25, "annealed Stage 5-6 RP averages about 19.5, was " + mean);
    }

    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError(label);
        }
    }
}
