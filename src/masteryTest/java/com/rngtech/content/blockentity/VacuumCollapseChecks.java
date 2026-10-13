package com.rngtech.content.blockentity;

/** Vacuum Collapse depth and curve: rated depth keeps the recipe, the curve keeps every FE, and depth is a visible trade. */
public final class VacuumCollapseChecks {
    private static final double EPSILON = 1.0E-9;
    private static int checks;

    private VacuumCollapseChecks() {
    }

    public static int run() {
        checks = 0;
        ratedDepthKeepsTheRecipe();
        everyCurveGeneratesExactlyTheCollapsesEnergy();
        theCurveClimbsToItsPeakAtTheEnd();
        deeperCollapsesTradeABurstForCatalysts();
        fractionAboveFollowsTheCurve();
        return checks;
    }

    private static void ratedDepthKeepsTheRecipe() {
        int rated = VacuumCollapseDynamics.RATED_DEPTH;
        require(VacuumCollapseDynamics.energy(rated) == 1.0, "rated depth draws the recipe's FE");
        require(VacuumCollapseDynamics.duration(rated) == 1.0, "rated depth runs the recipe's cycle");
        require(VacuumCollapseDynamics.instability(rated) == 1.0, "rated depth keeps the recipe's instability");
        require(VacuumCollapseDynamics.clampDepth(10) == VacuumCollapseDynamics.MIN_DEPTH, "depth clamps to its minimum");
        require(VacuumCollapseDynamics.clampDepth(400) == VacuumCollapseDynamics.MAX_DEPTH, "depth clamps to its maximum");
    }

    /** The curve's running share starts at 0, never falls, and ends at exactly 1, so no FE is lost or invented. */
    private static void everyCurveGeneratesExactlyTheCollapsesEnergy() {
        for (int depth = VacuumCollapseDynamics.MIN_DEPTH; depth <= VacuumCollapseDynamics.MAX_DEPTH; depth++) {
            require(VacuumCollapseDynamics.cumulative(depth, 0.0) == 0.0, depth + "% depth starts at nothing generated");
            require(Math.abs(VacuumCollapseDynamics.cumulative(depth, 1.0) - 1.0) < EPSILON, depth + "% depth ends with everything generated");
            double last = 0.0;
            double integral = 0.0;
            int steps = 2000;
            for (int step = 1; step <= steps; step++) {
                double share = VacuumCollapseDynamics.cumulative(depth, step / (double) steps);
                require(share >= last, depth + "% depth never takes FE back");
                last = share;
                integral += VacuumCollapseDynamics.rate(depth, (step - 0.5) / steps) / steps;
            }
            require(Math.abs(integral - 1.0) < 1.0E-4, depth + "% depth averages exactly its average rate, was " + integral);
        }
    }

    private static void theCurveClimbsToItsPeakAtTheEnd() {
        for (int depth = VacuumCollapseDynamics.MIN_DEPTH; depth <= VacuumCollapseDynamics.MAX_DEPTH; depth++) {
            require(VacuumCollapseDynamics.startRate(depth) < 1.0, depth + "% depth starts below its average");
            require(VacuumCollapseDynamics.peakRate(depth) > 1.0, depth + "% depth peaks above its average");
            require(Math.abs(VacuumCollapseDynamics.rate(depth, 1.0) - VacuumCollapseDynamics.peakRate(depth)) < EPSILON,
                    depth + "% depth reaches its peak at the end");
            double last = 0.0;
            for (int step = 0; step <= 100; step++) {
                double rate = VacuumCollapseDynamics.rate(depth, step / 100.0);
                require(rate >= last - EPSILON, depth + "% depth output never drops during a collapse");
                last = rate;
            }
        }
        require(Math.abs(VacuumCollapseDynamics.peakRate(VacuumCollapseDynamics.RATED_DEPTH) - 2.0) < EPSILON, "a rated collapse peaks at twice its average");
    }

    /** Deeper: more FE per catalyst with diminishing returns, but a harder peak, a longer cycle, and more instability. */
    private static void deeperCollapsesTradeABurstForCatalysts() {
        double lastEnergy = 0.0;
        double lastGain = Double.MAX_VALUE;
        for (int depth = VacuumCollapseDynamics.MIN_DEPTH + 1; depth <= VacuumCollapseDynamics.MAX_DEPTH; depth++) {
            double energy = VacuumCollapseDynamics.energy(depth);
            double previous = VacuumCollapseDynamics.energy(depth - 1);
            require(energy > previous, depth + "% depth draws more FE per catalyst than one step shallower");
            double gain = energy - previous;
            require(gain < lastGain, depth + "% depth gains less FE per catalyst than the step before");
            lastGain = gain;
            require(VacuumCollapseDynamics.peakRate(depth) > VacuumCollapseDynamics.peakRate(depth - 1), depth + "% depth peaks harder");
            require(VacuumCollapseDynamics.duration(depth) > VacuumCollapseDynamics.duration(depth - 1), depth + "% depth runs longer");
            require(VacuumCollapseDynamics.instability(depth) > VacuumCollapseDynamics.instability(depth - 1), depth + "% depth is less stable");
            lastEnergy = energy;
        }
        require(lastEnergy < 1.3, "full depth draws less than 1.3x FE per catalyst, was " + lastEnergy);
        double shallow = VacuumCollapseDynamics.energy(VacuumCollapseDynamics.MIN_DEPTH);
        require(shallow > 0.6 && shallow < 0.7, "the shallowest depth draws about two thirds of the FE, was " + shallow);
        double fullPerTick = lastEnergy / VacuumCollapseDynamics.duration(VacuumCollapseDynamics.MAX_DEPTH);
        double shallowPerTick = shallow / VacuumCollapseDynamics.duration(VacuumCollapseDynamics.MIN_DEPTH);
        require(fullPerTick < 1.1 && shallowPerTick > 0.9, "depth barely moves the average FE/t");
    }

    private static void fractionAboveFollowsTheCurve() {
        int rated = VacuumCollapseDynamics.RATED_DEPTH;
        require(VacuumCollapseDynamics.fractionAbove(rated, 1000.0, 2000.0) == 0.0, "a connector at the peak takes everything");
        double underAverage = VacuumCollapseDynamics.fractionAbove(rated, 1000.0, 1000.0);
        require(underAverage > 0.0 && underAverage < 0.5, "a connector at the average loses only part of the burst, was " + underAverage);
        require(VacuumCollapseDynamics.fractionAbove(VacuumCollapseDynamics.MAX_DEPTH, 1000.0, 1500.0)
                > VacuumCollapseDynamics.fractionAbove(VacuumCollapseDynamics.MIN_DEPTH, 1000.0, 1500.0),
                "the same connector loses more of a deep collapse than a shallow one");
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
