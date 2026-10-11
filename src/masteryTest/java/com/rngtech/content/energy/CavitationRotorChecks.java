package com.rngtech.content.energy;

import java.util.Random;

/** Cavitation rotor speed: the rated throttle keeps the recipe balance, and every other throttle is a visible trade. */
public final class CavitationRotorChecks {
    private static final int ROLL_SAMPLES = 20_000;
    private static int checks;

    private CavitationRotorChecks() {
    }

    public static int run() {
        checks = 0;
        ratedThrottleKeepsTheRecipe();
        nothingBelowOnset();
        throttleTradesAreMonotonic();
        spoolUpReachesTheTargetAndCoastsDown();
        throttleAndResonanceShareOneGrid();
        resonanceStepsAreVisibleImprovements();
        resonanceRollsStayOnTheGridAndRiseWithPrecision();
        wearScaleStretchesRotorLife();
        ratedIsTheMostFePerBucket();
        heatStatsHaveVisibleJobs();
        return checks;
    }

    /** Heat Core and Servo heat stats each change one visible number; an unrolled core keeps its stage cooling. */
    private static void heatStatsHaveVisibleJobs() {
        near(CavitationHeat.cooling(0, 1.0, 0.0), 1.0, "no Heat Core cools 1 per tick");
        near(CavitationHeat.cooling(6, 1.0, 0.0), 4.0, "an unrolled Titanium Heat Core keeps its stage cooling");
        near(CavitationHeat.cooling(6, 1.2, 0.0), 4.6, "rolled Heat Transfer speeds the core's cooling");
        near(CavitationHeat.cooling(6, 1.0, 2.0), 6.0, "Strain Recovery adds flat cooling");
        near(CavitationHeat.strainCap(1.0), 1000.0, "base strain cap");
        near(CavitationHeat.strainCap(1.25), 1250.0, "Overheat Tolerance raises the cap");
        near(CavitationHeat.strainCap(0.5), 1000.0, "the cap never falls below base");
        near(CavitationHeat.maxPenalty(0.0), 0.5, "without Max Temperature, full strain costs half the FE");
        require(CavitationHeat.maxPenalty(1200.0) < CavitationHeat.maxPenalty(200.0), "a hotter-rated core loses less FE at the cap");
        near(CavitationHeat.penalty(1250.0, 1250.0, 0.4), 0.4, "the cap costs the max penalty");
        near(CavitationHeat.penalty(500.0, 1000.0, 0.5), 0.25, "half strain costs half the max penalty");
        require(CavitationHeat.waterPerCycle(1000, 1.0) == 1000, "no Bonus Output draws the recipe's Water");
        require(CavitationHeat.waterPerCycle(1000, 1.55) == 646, "Bonus Output gets the same FE from less Water");
        require(CavitationHeat.waterPerCycle(1000, 0.1) == 2000, "a Bonus Output penalty never more than doubles the Water");
    }

    /** Water flow follows rotor speed, so every throttle away from rated gets less FE out of each bucket. */
    private static void ratedIsTheMostFePerBucket() {
        require(CavitationRotorDynamics.flow(CavitationRotorDynamics.RATED_SPEED) == 1.0, "rated speed draws the recipe's Water exactly");
        for (int throttle = CavitationRotorDynamics.MIN_THROTTLE; throttle <= CavitationRotorDynamics.MAX_THROTTLE; throttle++) {
            double speed = CavitationRotorDynamics.targetSpeed(throttle);
            double fePerBucket = CavitationRotorDynamics.generation(speed) / CavitationRotorDynamics.flow(speed);
            require(throttle == CavitationRotorDynamics.RATED_THROTTLE ? Math.abs(fePerBucket - 1.0) < 1e-9 : fePerBucket < 1.0,
                    "throttle " + throttle + "% gets " + fePerBucket + "x the rated FE per bucket");
        }
    }

    private static void ratedThrottleKeepsTheRecipe() {
        double rated = CavitationRotorDynamics.targetSpeed(CavitationRotorDynamics.RATED_THROTTLE);
        near(CavitationRotorDynamics.generation(rated), 1.0, "rated speed makes the recipe FE");
        near(CavitationRotorDynamics.wear(rated), 1.0, "rated speed wears the rotor at the recipe rate");
        near(CavitationRotorDynamics.heat(rated), 1.0, "rated speed heats at the recipe rate");
    }

    private static void nothingBelowOnset() {
        require(CavitationRotorDynamics.generation(CavitationRotorDynamics.ONSET_SPEED - 0.01) == 0.0, "no FE below cavitation onset");
        require(!CavitationRotorDynamics.cavitating(CavitationRotorDynamics.ONSET_SPEED - 0.01), "no cavitation below onset");
        require(CavitationRotorDynamics.targetSpeed(0) > CavitationRotorDynamics.ONSET_SPEED, "the lowest throttle still cavitates");
        near(CavitationRotorDynamics.generation(CavitationRotorDynamics.targetSpeed(CavitationRotorDynamics.MAX_THROTTLE)),
                CavitationRotorDynamics.MAX_GENERATION, "full throttle reaches the supercavitation cap");
    }

    private static void throttleTradesAreMonotonic() {
        double previousFe = 0.0;
        double previousWear = 0.0;
        for (int throttle = CavitationRotorDynamics.MIN_THROTTLE; throttle <= CavitationRotorDynamics.MAX_THROTTLE; throttle++) {
            double speed = CavitationRotorDynamics.targetSpeed(throttle);
            double fe = CavitationRotorDynamics.generation(speed);
            double wear = CavitationRotorDynamics.wear(speed);
            require(fe > previousFe && wear > previousWear, "throttle " + throttle + "% raises both FE and wear");
            if (throttle > CavitationRotorDynamics.RATED_THROTTLE) {
                require(fe / wear < 1.0 && CavitationRotorDynamics.heat(speed) > fe, "supercavitation costs more wear and heat than it adds FE");
            }
            previousFe = fe;
            previousWear = wear;
        }
    }

    private static void spoolUpReachesTheTargetAndCoastsDown() {
        double target = CavitationRotorDynamics.targetSpeed(CavitationRotorDynamics.RATED_THROTTLE);
        double speed = 0.0;
        int ticks = 0;
        while (speed < target && ticks < 1000) {
            speed = CavitationRotorDynamics.step(speed, target, true, 1.0);
            ticks++;
        }
        require(speed == target, "spool-up lands exactly on the target");
        require(ticks >= 70 && ticks <= 71, "the rated spool-up takes about 70 ticks, took " + ticks);
        require(CavitationRotorDynamics.step(1.0, target, true, 1.0) < 1.0, "lowering the throttle brakes the rotor");
        require(CavitationRotorDynamics.step(target, target, false, 1.0) < target, "an idle rotor coasts down");
        require(CavitationRotorDynamics.step(0.0, target, false, 1.0) == 0.0, "a stopped rotor stays stopped");
    }

    /** A throttle set to a rotor's resonance percent must hit it exactly, from any starting speed and spool rate. */
    private static void throttleAndResonanceShareOneGrid() {
        double minimum = 1.0 - CavitationRotorDynamics.RESONANCE_DEPTH;
        double[] starts = {0.0, 0.333, 0.95, 1.0};
        double[] spoolRates = {0.5, 1.0, 1.43, 4.0};
        for (int resonance = CavitationRotorDynamics.MIN_RESONANCE; resonance <= CavitationRotorDynamics.MAX_RESONANCE; resonance++) {
            double target = CavitationRotorDynamics.targetSpeed(resonance);
            for (double start : starts) {
                for (double rate : spoolRates) {
                    double speed = start;
                    for (int tick = 0; tick < 400 && speed != target; tick++) {
                        speed = CavitationRotorDynamics.step(speed, target, true, rate);
                    }
                    require(speed == CavitationRotorDynamics.speedOf(resonance), "throttle " + resonance + "% settles exactly on resonance " + resonance + "%");
                    require(CavitationRotorDynamics.resonance(speed, resonance) == minimum,
                            "throttle on resonance " + resonance + "% gives the full bonus, got " + CavitationRotorDynamics.resonance(speed, resonance));
                }
            }
            require(CavitationRotorDynamics.rpmOf(resonance) == CavitationRotorDynamics.rpm(target), "gauge and rotor agree on RPM at " + resonance + "%");
        }
    }

    private static void resonanceStepsAreVisibleImprovements() {
        int resonance = 75;
        near(CavitationRotorDynamics.resonance(CavitationRotorDynamics.speedOf(resonance), resonance), 0.33, "on resonance");
        near(round2(CavitationRotorDynamics.resonance(CavitationRotorDynamics.speedOf(resonance + 1), resonance)), 0.40, "one step off");
        near(round2(CavitationRotorDynamics.resonance(CavitationRotorDynamics.speedOf(resonance - 2), resonance)), 0.57, "two steps off");
        near(round2(CavitationRotorDynamics.resonance(CavitationRotorDynamics.speedOf(resonance + 3), resonance)), 0.75, "three steps off");
        require(CavitationRotorDynamics.resonance(CavitationRotorDynamics.speedOf(resonance + 5), resonance) > 0.95, "five steps off is nearly no bonus");
        require(CavitationRotorDynamics.resonance(0.7, 0) == 1.0, "a rotor without a resonance gets no bonus");
        double previous = 1.0;
        for (int offset = 6; offset >= 0; offset--) {
            double factor = CavitationRotorDynamics.resonance(CavitationRotorDynamics.speedOf(resonance + offset), resonance);
            require(factor < previous, "each step toward resonance lowers wear, at " + offset + " off");
            previous = factor;
        }
    }

    private static void resonanceRollsStayOnTheGridAndRiseWithPrecision() {
        double previousMean = 0.0;
        for (double precision : new double[] {0.0, 0.2, 0.5, 0.8}) {
            Random random = new Random(42);
            long sum = 0;
            for (int sample = 0; sample < ROLL_SAMPLES; sample++) {
                int roll = CavitationRotorDynamics.rollResonance(random::nextDouble, precision);
                require(roll >= CavitationRotorDynamics.MIN_RESONANCE && roll <= CavitationRotorDynamics.MAX_RESONANCE,
                        "resonance roll " + roll + " stays on the 55-90% grid");
                sum += roll;
            }
            double mean = sum / (double) ROLL_SAMPLES;
            require(mean > previousMean, "a more precise hub raises the average resonance: " + mean + " at precision " + precision);
            previousMean = mean;
        }
        require(previousMean > 79.0, "a stability-80 hub averages about 80% resonance, got " + previousMean);
    }

    /** Steel off resonance at rated: recipe wear 20 per cycle, scaled, gives 300 cycles of 240 ticks, one hour. */
    private static void wearScaleStretchesRotorLife() {
        double perCycle = 20 * CavitationRotorDynamics.WEAR_SCALE;
        int cycles = (int) Math.ceil(1000 / perCycle - 1e-9);
        require(cycles == 300, "a Steel rotor lasts 300 rated cycles, got " + cycles);
        double tuned = perCycle * CavitationRotorDynamics.resonance(0.7, 70);
        double tunedHours = 1000 / tuned * 240 / 20 / 3600;
        require(tunedHours > 2.9 && tunedHours < 3.1, "a tuned Steel rotor lasts about three hours, got " + tunedHours);
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static void near(double actual, double expected, String message) {
        require(Math.abs(actual - expected) < 0.0001, message + ": expected " + expected + " but was " + actual);
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
