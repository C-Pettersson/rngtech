package com.rngtech.content.energy;

/**
 * Corrosion Cell load: the share of the cell's rated current the player draws, as in a metal-air cell. A fuel holds a
 * fixed charge (Faraday's law), so the current sets how fast it dissolves, and the cell's polarization curve sets the
 * voltage and with it the FE that charge is worth. Rated load is the highest and reproduces the recipe exactly; a lighter
 * load runs at a higher voltage, trading FE/t for FE per fuel.
 */
public final class CorrosionCellLoad {
    public static final int MIN_LOAD = 40;
    public static final int MAX_LOAD = 100;
    public static final int RATED_LOAD = MAX_LOAD;
    public static final int STEP = 5;
    /** Voltage gained per unit of current below rated: the slope of the cell's polarization curve. */
    public static final double POLARIZATION = 0.25;
    /** Cell voltage at rated load, a display value in the range of real aluminium-air cells. */
    public static final double RATED_VOLTS = 1.40;
    private static final double FINISH_TOLERANCE = 1.0e-6;

    private CorrosionCellLoad() {
    }

    public static int clamp(int load) {
        return Math.max(MIN_LOAD, Math.min(MAX_LOAD, load));
    }

    /** The nearest selectable load, on the {@link #STEP} grid. */
    public static int snap(int load) {
        return clamp(Math.round(load / (float) STEP) * STEP);
    }

    /** Current as a fraction of rated. */
    public static double current(int load) {
        return clamp(load) / 100.0;
    }

    /** FE each fuel is worth relative to its recipe: the cell voltage relative to rated. */
    public static double fePerFuel(int load) {
        return 1.0 + POLARIZATION * (1.0 - current(load));
    }

    /** FE/t relative to the recipe: current times voltage. */
    public static double fePerTick(int load) {
        return current(load) * fePerFuel(load);
    }

    /** Cycle length relative to the recipe: the fuel's charge divided by the current. */
    public static double cycleLength(int load) {
        return 1.0 / current(load);
    }

    public static double volts(int load) {
        return RATED_VOLTS * fePerFuel(load);
    }

    /**
     * Rated ticks of fuel one tick at {@code load} dissolves from a cycle {@code ratedTicks} long, never past its end. At
     * rated load this is exactly one tick, so a cycle takes the recipe's ticks.
     */
    public static double dissolve(double progress, int ratedTicks, int load) {
        return Math.max(0.0, Math.min(current(load), ratedTicks - progress));
    }

    public static boolean finished(double progress, int ratedTicks) {
        return progress >= ratedTicks - FINISH_TOLERANCE;
    }

    /** Whole ticks a cycle of {@code ratedTicks} takes at {@code load}, for display. */
    public static int displayTicks(int ratedTicks, int load) {
        return ratedTicks <= 0 ? 0 : (int) Math.ceil(ratedTicks * cycleLength(load) - FINISH_TOLERANCE);
    }
}
