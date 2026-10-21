package com.rngtech.rpg.refinement;

import com.rngtech.rpg.PhaseArrayRules;

import net.minecraft.util.RandomSource;

/**
 * One Phase Array pulse: the Fracture check, then the Coils in order from the Emitter until the first slip, then the
 * ladder against the operation's Coil count. {@code PhaseArraySimulation} mirrors {@link #resolve} and {@link #odds};
 * keep them in step.
 */
public final class PhasePulse {
    public enum Outcome {
        SUCCESS,
        OVERHOLD,
        PARTIAL,
        MISS,
        FRACTURE;

        public String translationKey() {
            return "rngtech.phase_array.outcome." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** {@code held} Coils held before the first slip; {@code slippedPosition} is 0 when none slipped. */
    public record Result(Outcome outcome, int held, int slippedPosition) {
    }

    /** Fractions that sum to 1. */
    public record Odds(double success, double overhold, double partial, double miss, double fracture) {
        public static final Odds NONE = new Odds(0, 0, 0, 1, 0);
    }

    private PhasePulse() {
    }

    /** PRD, The Hold Sequence: 1% per point of Strain above the line's Tolerance. */
    public static double fractureChance(PhaseLineStats line, int strain) {
        int exposed = Math.max(0, strain - line.strainTolerance());
        return Math.min(1.0, exposed * PhaseArrayRules.FRACTURE_PER_STRAIN);
    }

    public static Result resolve(PhaseLineStats line, int requiredCoils, int strain, RandomSource random) {
        if (random.nextDouble() < fractureChance(line, strain)) {
            return new Result(Outcome.FRACTURE, 0, 0);
        }
        int coils = line.usableCoils();
        int held = 0;
        int slipped = 0;
        for (int position = 1; position <= coils; position++) {
            double hold = line.holdChance().get(position - 1) / 100.0;
            double second = line.secondHold().get(position - 1) / 100.0;
            boolean holds = random.nextDouble() < hold || (second > 0 && random.nextDouble() < second);
            if (!holds) {
                slipped = position;
                break;
            }
            held++;
        }
        if (held == coils && coils > requiredCoils) {
            for (int position = requiredCoils + 1; position <= coils; position++) {
                if (random.nextDouble() < line.overholdFraction(position)) {
                    return new Result(Outcome.OVERHOLD, held, 0);
                }
            }
            return new Result(Outcome.SUCCESS, held, 0);
        }
        if (held >= requiredCoils) {
            return new Result(Outcome.SUCCESS, held, slipped);
        }
        if (held >= requiredCoils - line.partialWindow()) {
            return new Result(Outcome.PARTIAL, held, slipped);
        }
        return new Result(Outcome.MISS, held, slipped);
    }

    /** The same ladder in closed form, for the preview. */
    public static Odds odds(PhaseLineStats line, int requiredCoils, int strain) {
        int coils = line.usableCoils();
        if (coils < requiredCoils) {
            return Odds.NONE;
        }
        double fracture = fractureChance(line, strain);
        double success = 0;
        double overhold = 0;
        double partial = 0;
        double reach = 1.0;
        for (int held = 0; held <= coils; held++) {
            double chance = held == coils ? reach : reach * (1 - line.effectiveHold(held + 1));
            if (held == coils && coils > requiredCoils) {
                double noOverhold = 1.0;
                for (int position = requiredCoils + 1; position <= coils; position++) {
                    noOverhold *= 1 - line.overholdFraction(position);
                }
                overhold += chance * (1 - noOverhold);
                success += chance * noOverhold;
            } else if (held >= requiredCoils) {
                success += chance;
            } else if (held >= requiredCoils - line.partialWindow()) {
                partial += chance;
            }
            if (held < coils) {
                reach *= line.effectiveHold(held + 1);
            }
        }
        double live = 1 - fracture;
        double miss = Math.max(0, 1 - success - overhold - partial);
        return new Odds(live * success, live * overhold, live * partial, live * miss, fracture);
    }
}
