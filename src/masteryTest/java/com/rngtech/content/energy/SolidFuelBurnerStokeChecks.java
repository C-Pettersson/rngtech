package com.rngtech.content.energy;

/** Solid Fuel Burner stoke: rated stoke is the unstoked burner, and turning it down trades FE/t for FE per fuel item. */
public final class SolidFuelBurnerStokeChecks {
    private static final int[] RATINGS = {1, 24, 40, 64, 96, 128, 157};
    private static int checks;

    private SolidFuelBurnerStokeChecks() {
    }

    public static int run() {
        checks = 0;
        ratedStokeIsTheUnstokedBurner();
        stokeNeverExceedsTheHeatCore();
        lowerStokeTradesRateForFuel();
        return checks;
    }

    private static void ratedStokeIsTheUnstokedBurner() {
        int rated = SolidFuelBurnerStoke.RATED_STOKE;
        require(rated == SolidFuelBurnerStoke.MAX_STOKE, "rated stoke is the top of the range");
        require(SolidFuelBurnerStoke.fuelEconomy(rated) == 1.0, "rated stoke gets each fuel item's full FE and no more");
        for (int rating : RATINGS) {
            require(SolidFuelBurnerStoke.generation(rating, rated) == rating, "rated stoke runs a " + rating + " FE/t core at " + rating + " FE/t");
        }
        require(SolidFuelBurnerStoke.clamp(0) == SolidFuelBurnerStoke.MIN_STOKE, "stoke clamps to its minimum");
        require(SolidFuelBurnerStoke.clamp(250) == SolidFuelBurnerStoke.MAX_STOKE, "stoke clamps to its maximum");
    }

    /** Coal fails on rate: no stoke runs faster than the Heat Core, and a running burner never stalls at 0 FE/t. */
    private static void stokeNeverExceedsTheHeatCore() {
        for (int rating : RATINGS) {
            for (int stoke = 0; stoke <= 120; stoke++) {
                int generation = SolidFuelBurnerStoke.generation(rating, stoke);
                require(generation <= rating, stoke + "% stoke stays within the " + rating + " FE/t rating");
                require(generation >= 1, stoke + "% stoke still makes FE from a " + rating + " FE/t core");
            }
        }
        require(SolidFuelBurnerStoke.generation(0, 50) == 0, "no Heat Core output means no FE at any stoke");
    }

    /** Each step down costs FE/t and buys FE per item, reaching a quarter of the rate for 20% more FE at the minimum. */
    private static void lowerStokeTradesRateForFuel() {
        double lastEconomy = Double.MAX_VALUE;
        int lastGeneration = 0;
        for (int stoke = SolidFuelBurnerStoke.MIN_STOKE; stoke <= SolidFuelBurnerStoke.MAX_STOKE; stoke++) {
            double economy = SolidFuelBurnerStoke.fuelEconomy(stoke);
            int generation = SolidFuelBurnerStoke.generation(96, stoke);
            require(economy < lastEconomy, stoke + "% stoke gets less FE per item than one step lower");
            require(economy >= 1.0, stoke + "% stoke never loses FE per item");
            require(generation >= lastGeneration, stoke + "% stoke runs at least as fast as one step lower");
            lastEconomy = economy;
            lastGeneration = generation;
        }
        require(Math.abs(SolidFuelBurnerStoke.fuelEconomy(SolidFuelBurnerStoke.MIN_STOKE) - 1.2) < 1.0E-9, "the minimum stoke gets 20% more FE per item");
        require(SolidFuelBurnerStoke.generation(96, SolidFuelBurnerStoke.MIN_STOKE) == 24, "the minimum stoke runs at a quarter of the rating");
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
