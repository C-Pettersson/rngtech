package com.rngtech.content.energy;

import java.util.function.DoubleSupplier;

/**
 * Cavitation Generator rotor speed. Speed is a fraction of {@link #MAX_RPM}; the throttle and a rotor's resonance are
 * whole percents on the same grid, converted by {@link #speedOf}, so a throttle set to the resonance matches it exactly.
 * Below onset nothing cavitates, the rated throttle reproduces the recipe exactly, and supercavitation trades rotor
 * wear and heat strain for extra FE. Resonance only lowers wear.
 */
public final class CavitationRotorDynamics {
    public static final int MAX_RPM = 3000;
    public static final int MIN_THROTTLE = 40;
    public static final int MAX_THROTTLE = 100;
    public static final int RATED_THROTTLE = 70;
    public static final int MIN_RESONANCE = 55;
    public static final int MAX_RESONANCE = 90;
    public static final double ONSET_SPEED = 0.30;
    public static final double RATED_SPEED = speedOf(RATED_THROTTLE);
    public static final double MAX_GENERATION = 1.35;
    /** Rotor wear per recipe wear point; recipes keep their authored wear while rotors last hours. */
    public static final double WEAR_SCALE = 1.0 / 6.0;
    public static final double RESONANCE_DEPTH = 0.67;
    private static final double RESONANCE_WIDTH = 0.03;
    private static final double SPOOL_UP_PER_TICK = 0.01;
    private static final double COAST_DOWN_PER_TICK = 0.005;
    private static final double BRAKE_PER_TICK = 0.02;

    private CavitationRotorDynamics() {
    }

    /** The one conversion from a throttle or resonance percent to rotor speed. */
    public static double speedOf(int percent) {
        return percent / 100.0;
    }

    public static int clampThrottle(int percent) {
        return Math.max(MIN_THROTTLE, Math.min(MAX_THROTTLE, percent));
    }

    public static double targetSpeed(int percent) {
        return speedOf(clampThrottle(percent));
    }

    public static boolean cavitating(double speed) {
        return speed >= ONSET_SPEED;
    }

    public static double generation(double speed) {
        if (!cavitating(speed)) {
            return 0.0;
        }
        if (speed <= RATED_SPEED) {
            return Math.min(1.0, (speed - ONSET_SPEED) / (RATED_SPEED - ONSET_SPEED));
        }
        return 1.0 + (Math.min(1.0, speed) - RATED_SPEED) / (1.0 - RATED_SPEED) * (MAX_GENERATION - 1.0);
    }

    /** Water drawn through the rotor relative to rated: flow is proportional to speed, so rated is the most FE per bucket. */
    public static double flow(double speed) {
        return Math.max(0.0, speed) / RATED_SPEED;
    }

    public static double wear(double speed) {
        return Math.pow(speed / RATED_SPEED, 3);
    }

    /** Steep on purpose: Heat Core stage and Temperature Stability decide the highest throttle a build can sustain. */
    public static double heat(double speed) {
        return Math.pow(speed / RATED_SPEED, 8);
    }

    /** Wear multiplier from running near the rotor's resonance percent; 1 when the rotor has none. */
    public static double resonance(double speed, int resonancePercent) {
        if (resonancePercent <= 0) {
            return 1.0;
        }
        double offset = (speed - speedOf(resonancePercent)) / RESONANCE_WIDTH;
        return 1.0 - RESONANCE_DEPTH * Math.exp(-offset * offset);
    }

    /**
     * A whole resonance percent in [{@link #MIN_RESONANCE}, {@link #MAX_RESONANCE}]. Higher hub {@code precision}
     * (calibration stability / 100) skews the roll toward the top of the range.
     */
    public static int rollResonance(DoubleSupplier uniform, double precision) {
        double skew = 1.0 / (1.0 + 2.0 * Math.max(0.0, Math.min(1.0, precision)));
        double position = Math.pow(uniform.getAsDouble(), skew);
        return (int) Math.round(MIN_RESONANCE + (MAX_RESONANCE - MIN_RESONANCE) * position);
    }

    /** One tick of spool-up toward the target while driven, braking above it, or coasting down when idle. */
    public static double step(double speed, double target, boolean driven, double spoolMultiplier) {
        if (!driven) {
            return Math.max(0.0, speed - COAST_DOWN_PER_TICK);
        }
        if (speed < target) {
            return Math.min(target, speed + SPOOL_UP_PER_TICK * Math.max(0.1, spoolMultiplier));
        }
        return Math.max(target, speed - BRAKE_PER_TICK);
    }

    public static int rpm(double speed) {
        return (int) Math.round(Math.max(0.0, speed) * MAX_RPM);
    }

    public static int rpmOf(int percent) {
        return percent * MAX_RPM / 100;
    }
}
