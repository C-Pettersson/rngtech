package com.rngtech.content.blockentity;

/**
 * Algae Photobioreactor culture and aeration. The culture density (0 to 1) grows logistically while the reactor has
 * light, water and carbon gas, and growth runs at density times the aeration factor, so a fresh or starved culture
 * needs time to recover. Aeration follows Monod saturation: more gas speeds growth with diminishing returns, so carbon
 * per Algae Biomass climbs above rated and falls below it. The rated aeration reproduces the recipe exactly.
 */
public final class AlgaeCultureDynamics {
    public static final int MIN_AERATION = 40;
    public static final int MAX_AERATION = 100;
    public static final int RATED_AERATION = 70;
    /** The density a reactor seeds itself with from its water, and the floor a starving culture dies back to. */
    public static final double SEED_DENSITY = 0.05;
    /** Densities this close to full count as full, so a settled culture harvests at exactly the recipe rate. */
    public static final double FULL_DENSITY = 0.995;
    /** Logistic growth per processing cycle at rated aeration: about one cycle from seed to full. */
    private static final double GROWTH_PER_CYCLE = 6.0;
    private static final double HALF_SATURATION = 0.35;
    /** A starving culture loses half its density above the seed every five minutes. */
    private static final double DIE_BACK_HALF_LIFE_TICKS = 6000.0;

    private AlgaeCultureDynamics() {
    }

    public static int clampAeration(int percent) {
        return Math.max(MIN_AERATION, Math.min(MAX_AERATION, percent));
    }

    /** Growth speed relative to rated aeration. */
    public static double growth(int aeration) {
        return monod(clampAeration(aeration)) / monod(RATED_AERATION);
    }

    /** Carbon gas per Algae Biomass relative to the recipe: gas fed over growth gained. */
    public static double carbonPerBiomass(int aeration) {
        int clamped = clampAeration(aeration);
        return clamped / (double) RATED_AERATION / growth(clamped);
    }

    /** The carbon one harvest drains at this aeration, never below 1 mB. */
    public static int carbonAmount(int recipeAmount, int aeration) {
        return Math.max(1, (int) Math.round(recipeAmount * carbonPerBiomass(aeration)));
    }

    /** Harvest progress one tick adds, in processing ticks. */
    public static double progressPerTick(double density, int aeration) {
        return effectiveDensity(density) * growth(aeration);
    }

    /** One tick of logistic growth for a culture whose full cycle takes {@code processingTicks}. */
    public static double grow(double density, int aeration, int processingTicks) {
        double current = Math.max(SEED_DENSITY, Math.min(1.0, density));
        if (current >= FULL_DENSITY) {
            return 1.0;
        }
        double rate = GROWTH_PER_CYCLE * growth(aeration) / Math.max(1, processingTicks);
        double next = current + rate * current * (1.0 - current);
        return next >= FULL_DENSITY ? 1.0 : next;
    }

    /** One tick of die-back while the culture has no light, water or carbon gas. */
    public static double dieBack(double density) {
        if (density <= SEED_DENSITY) {
            return Math.max(0.0, density);
        }
        double keep = Math.pow(0.5, 1.0 / DIE_BACK_HALF_LIFE_TICKS);
        return SEED_DENSITY + (density - SEED_DENSITY) * keep;
    }

    private static double effectiveDensity(double density) {
        return density >= FULL_DENSITY ? 1.0 : Math.max(0.0, Math.min(1.0, density));
    }

    private static double monod(int aeration) {
        double fraction = aeration / 100.0;
        return fraction / (fraction + HALF_SATURATION);
    }
}
