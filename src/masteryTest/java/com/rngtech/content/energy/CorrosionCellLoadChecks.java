package com.rngtech.content.energy;

/** Corrosion Cell load: rated load keeps the recipe exactly, and every lighter load is a visible FE/t-for-fuel trade. */
public final class CorrosionCellLoadChecks {
    private static final int[] RECIPE_TICKS = {1, 137, 160, 320};
    private static final double RECIPE_ENERGY = 64_000.0;
    private static int checks;

    private CorrosionCellLoadChecks() {
    }

    public static int run() {
        checks = 0;
        ratedLoadKeepsTheRecipe();
        ratedCycleMatchesTheRecipeTickForTick();
        lighterLoadsTradeFePerTickForFePerFuel();
        everyLoadDeliversItsCurveOverACycle();
        changingLoadMidCycleAppliesToTheRest();
        loadSnapsToTheGrid();
        wikiTableValues();
        return checks;
    }

    private static void ratedLoadKeepsTheRecipe() {
        require(CorrosionCellLoad.RATED_LOAD == CorrosionCellLoad.MAX_LOAD, "rated load is the highest load, so FE/t never exceeds the recipe");
        near(CorrosionCellLoad.fePerFuel(CorrosionCellLoad.RATED_LOAD), 1.0, "rated load gets the recipe FE per fuel");
        near(CorrosionCellLoad.fePerTick(CorrosionCellLoad.RATED_LOAD), 1.0, "rated load makes the recipe FE/t");
        near(CorrosionCellLoad.cycleLength(CorrosionCellLoad.RATED_LOAD), 1.0, "rated load takes the recipe ticks");
        near(CorrosionCellLoad.volts(CorrosionCellLoad.RATED_LOAD), CorrosionCellLoad.RATED_VOLTS, "rated load reads the rated voltage");
    }

    /** The block entity's tick loop at rated load: exactly one recipe tick per tick, with the recipe's FE. */
    private static void ratedCycleMatchesTheRecipeTickForTick() {
        for (int ticks : RECIPE_TICKS) {
            double[] result = simulate(ticks, CorrosionCellLoad.RATED_LOAD, -1, CorrosionCellLoad.RATED_LOAD);
            require(result[0] == ticks, "a " + ticks + "-tick recipe at rated load took " + result[0] + " ticks");
            near(result[1], RECIPE_ENERGY, "a " + ticks + "-tick recipe at rated load makes the recipe FE");
            require(CorrosionCellLoad.displayTicks(ticks, CorrosionCellLoad.RATED_LOAD) == ticks, "the screen shows the recipe ticks at rated load");
            require(CorrosionCellLoad.dissolve(0.0, ticks, CorrosionCellLoad.RATED_LOAD) == Math.min(1.0, ticks), "rated load dissolves one tick per tick");
        }
    }

    private static void lighterLoadsTradeFePerTickForFePerFuel() {
        double previousPerTick = 0.0;
        double previousPerFuel = Double.MAX_VALUE;
        for (int load = CorrosionCellLoad.MIN_LOAD; load <= CorrosionCellLoad.MAX_LOAD; load += CorrosionCellLoad.STEP) {
            double perTick = CorrosionCellLoad.fePerTick(load);
            double perFuel = CorrosionCellLoad.fePerFuel(load);
            require(perTick > previousPerTick, "load " + load + "% raises FE/t");
            require(perFuel < previousPerFuel, "load " + load + "% lowers FE per fuel");
            require(perTick <= 1.0 + 1.0e-12, "load " + load + "% stays at or below the recipe FE/t");
            require(perFuel >= 1.0 - 1.0e-12, "load " + load + "% gets at least the recipe FE per fuel");
            previousPerTick = perTick;
            previousPerFuel = perFuel;
        }
    }

    private static void everyLoadDeliversItsCurveOverACycle() {
        for (int ticks : RECIPE_TICKS) {
            for (int load = CorrosionCellLoad.MIN_LOAD; load <= CorrosionCellLoad.MAX_LOAD; load += CorrosionCellLoad.STEP) {
                double[] result = simulate(ticks, load, -1, load);
                require(result[0] == CorrosionCellLoad.displayTicks(ticks, load),
                        "a " + ticks + "-tick recipe at " + load + "% took " + result[0] + " ticks, the screen says "
                                + CorrosionCellLoad.displayTicks(ticks, load));
                near(result[1], RECIPE_ENERGY * CorrosionCellLoad.fePerFuel(load), "a " + ticks + "-tick recipe at " + load + "% FE per fuel");
            }
        }
    }

    private static void changingLoadMidCycleAppliesToTheRest() {
        double[] result = simulate(320, CorrosionCellLoad.RATED_LOAD, 160, 50);
        require(result[0] == 160 + 320, "half the fuel at rated then half at 50% takes 160 + 320 ticks, took " + result[0]);
        near(result[1], RECIPE_ENERGY * (0.5 + 0.5 * CorrosionCellLoad.fePerFuel(50)), "each half of the fuel pays at its own load");
    }

    private static void loadSnapsToTheGrid() {
        require(CorrosionCellLoad.snap(0) == CorrosionCellLoad.MIN_LOAD, "loads below the minimum clamp up");
        require(CorrosionCellLoad.snap(250) == CorrosionCellLoad.MAX_LOAD, "loads above rated clamp down");
        require(CorrosionCellLoad.snap(72) == 70 && CorrosionCellLoad.snap(73) == 75, "loads snap to 5% steps");
        require(CorrosionCellLoad.current(10) == CorrosionCellLoad.current(CorrosionCellLoad.MIN_LOAD), "current never drops below the minimum load");
    }

    /** The values the wiki table shows: {load, FE/t, FE per fuel, cycle length}. */
    private static void wikiTableValues() {
        double[][] rows = {
                {40, 0.46, 1.15, 2.5},
                {50, 0.5625, 1.125, 2.0},
                {60, 0.66, 1.10, 1.0 / 0.6},
                {70, 0.7525, 1.075, 1.0 / 0.7},
                {80, 0.84, 1.05, 1.25},
                {90, 0.9225, 1.025, 1.0 / 0.9},
                {100, 1.0, 1.0, 1.0},
        };
        for (double[] row : rows) {
            int load = (int) row[0];
            near(CorrosionCellLoad.fePerTick(load), row[1], load + "% FE/t");
            near(CorrosionCellLoad.fePerFuel(load), row[2], load + "% FE per fuel");
            near(CorrosionCellLoad.cycleLength(load), row[3], load + "% cycle length");
        }
        near(CorrosionCellLoad.volts(40), 1.61, "40% cell voltage");
    }

    /**
     * CorrosionCellBlockEntity#generateActiveRecipe with room in the buffer: returns {ticks, FE}. The load switches to
     * {@code laterLoad} once {@code switchAt} ticks have passed, or never when it is negative.
     */
    private static double[] simulate(int ratedTicks, int load, int switchAt, int laterLoad) {
        double progress = 0.0;
        double energy = 0.0;
        int ticks = 0;
        while (!CorrosionCellLoad.finished(progress, ratedTicks)) {
            int current = switchAt >= 0 && ticks >= switchAt ? laterLoad : load;
            double step = CorrosionCellLoad.dissolve(progress, ratedTicks, current);
            energy += RECIPE_ENERGY / ratedTicks * CorrosionCellLoad.fePerFuel(current) * step;
            progress += step;
            ticks++;
            require(ticks < ratedTicks * 4, "a cycle never ends at " + current + "%");
        }
        return new double[] {ticks, energy};
    }

    private static void near(double actual, double expected, String message) {
        require(Math.abs(actual - expected) <= 1.0e-6 * Math.max(1.0, Math.abs(expected)), message + ": expected " + expected + ", got " + actual);
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
