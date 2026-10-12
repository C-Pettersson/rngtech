package com.rngtech.content.energy;

/**
 * The Syngas Combustor's fuel mixture, in percent of the rated fuel flow. Rich mixtures feed fuel faster than the air
 * can burn it cleanly, so FE/t rises with diminishing returns while FE per mB falls and the unburnt carbon leaves as
 * extra Carbon Exhaust. Lean mixtures are the reverse: slower, cleaner, and more fuel-efficient. The rated mixture
 * reproduces the recipe exactly.
 */
public final class CombustionMixture {
    public static final int MIN_MIXTURE = 70;
    public static final int MAX_MIXTURE = 140;
    public static final int RATED_MIXTURE = 100;
    private static final double POWER_EXPONENT = 0.75;
    private static final double EXHAUST_EXPONENT = 2.0;

    private CombustionMixture() {
    }

    public static int clamp(int percent) {
        return Math.max(MIN_MIXTURE, Math.min(MAX_MIXTURE, percent));
    }

    /** Fuel burned per tick relative to rated: the cycle's progress per tick. */
    public static double fuelRate(int mixture) {
        return clamp(mixture) / (double) RATED_MIXTURE;
    }

    /** FE/t relative to rated. */
    public static double power(int mixture) {
        return Math.pow(fuelRate(mixture), POWER_EXPONENT);
    }

    /** FE per mB of fuel relative to the recipe. */
    public static double energyPerFuel(int mixture) {
        return power(mixture) / fuelRate(mixture);
    }

    /** Carbon Exhaust per mB of fuel relative to the recipe. */
    public static double exhaustPerFuel(int mixture) {
        return Math.pow(fuelRate(mixture), EXHAUST_EXPONENT);
    }

    /** The Carbon Exhaust one cycle leaves at this mixture, rounded to whole mB. */
    public static int exhaustAmount(int recipeAmount, int mixture) {
        return (int) Math.round(recipeAmount * exhaustPerFuel(mixture));
    }
}
