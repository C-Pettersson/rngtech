package com.rngtech.content.blockentity;

/** Algae culture and aeration: rated aeration keeps the recipe balance, and every other setting is a visible trade. */
public final class AlgaeCultureChecks {
    private static final int CYCLE_TICKS = 1200;
    private static int checks;

    private AlgaeCultureChecks() {
    }

    public static int run() {
        checks = 0;
        ratedAerationKeepsTheRecipe();
        aerationTradesSpeedForCarbon();
        aFreshCultureTakesAboutOneCycleToSettle();
        aFullCultureHarvestsAtTheRecipeRate();
        starvingCulturesDieBackToTheSeed();
        return checks;
    }

    private static void ratedAerationKeepsTheRecipe() {
        int rated = AlgaeCultureDynamics.RATED_AERATION;
        require(AlgaeCultureDynamics.growth(rated) == 1.0, "rated aeration grows at the recipe speed");
        require(AlgaeCultureDynamics.carbonPerBiomass(rated) == 1.0, "rated aeration uses the recipe's carbon");
        for (int amount : new int[] {80, 100, 500}) {
            require(AlgaeCultureDynamics.carbonAmount(amount, rated) == amount, "rated aeration drains " + amount + " mB per biomass");
        }
        require(AlgaeCultureDynamics.clampAeration(10) == AlgaeCultureDynamics.MIN_AERATION, "aeration clamps to its minimum");
        require(AlgaeCultureDynamics.clampAeration(150) == AlgaeCultureDynamics.MAX_AERATION, "aeration clamps to its maximum");
    }

    /** More gas always grows faster and always costs more gas per biomass, with diminishing returns. */
    private static void aerationTradesSpeedForCarbon() {
        double lastGrowth = 0.0;
        double lastCarbon = 0.0;
        double lastGain = Double.MAX_VALUE;
        for (int aeration = AlgaeCultureDynamics.MIN_AERATION; aeration <= AlgaeCultureDynamics.MAX_AERATION; aeration++) {
            double growth = AlgaeCultureDynamics.growth(aeration);
            double carbon = AlgaeCultureDynamics.carbonPerBiomass(aeration);
            require(growth > lastGrowth, "aeration " + aeration + "% grows faster than one step less");
            require(carbon > lastCarbon, "aeration " + aeration + "% costs more carbon per biomass than one step less");
            if (lastGrowth > 0.0) {
                double gain = growth - lastGrowth;
                require(gain < lastGain, "aeration " + aeration + "% gains less speed than the step before");
                lastGain = gain;
            }
            lastGrowth = growth;
            lastCarbon = carbon;
        }
        double fullGrowth = AlgaeCultureDynamics.growth(AlgaeCultureDynamics.MAX_AERATION);
        double fullCarbon = AlgaeCultureDynamics.carbonPerBiomass(AlgaeCultureDynamics.MAX_AERATION);
        require(fullGrowth > 1.08 && fullGrowth < 1.15, "full aeration grows about 11% faster, was " + fullGrowth);
        require(fullCarbon > 1.25 && fullCarbon < 1.32, "full aeration costs about 29% more carbon, was " + fullCarbon);
        double leanGrowth = AlgaeCultureDynamics.growth(AlgaeCultureDynamics.MIN_AERATION);
        double leanCarbon = AlgaeCultureDynamics.carbonPerBiomass(AlgaeCultureDynamics.MIN_AERATION);
        require(leanGrowth > 0.77 && leanGrowth < 0.83, "lean aeration grows about 20% slower, was " + leanGrowth);
        require(leanCarbon > 0.68 && leanCarbon < 0.75, "lean aeration saves about 29% carbon, was " + leanCarbon);
    }

    private static void aFreshCultureTakesAboutOneCycleToSettle() {
        double density = 0.0;
        int ticks = 0;
        while (density < 1.0 && ticks < CYCLE_TICKS * 5) {
            density = AlgaeCultureDynamics.grow(density, AlgaeCultureDynamics.RATED_AERATION, CYCLE_TICKS);
            ticks++;
        }
        require(ticks > CYCLE_TICKS * 0.8 && ticks < CYCLE_TICKS * 1.5, "a fresh culture settles in about one cycle, took " + ticks);
        int rich = 0;
        density = 0.0;
        while (density < 1.0) {
            density = AlgaeCultureDynamics.grow(density, AlgaeCultureDynamics.MAX_AERATION, CYCLE_TICKS);
            rich++;
        }
        require(rich < ticks, "rich aeration settles a culture sooner");
    }

    private static void aFullCultureHarvestsAtTheRecipeRate() {
        double progress = 0.0;
        for (int tick = 0; tick < CYCLE_TICKS; tick++) {
            progress += AlgaeCultureDynamics.progressPerTick(1.0, AlgaeCultureDynamics.RATED_AERATION);
        }
        require(progress == CYCLE_TICKS, "a full culture at rated aeration harvests once per recipe cycle");
        require(AlgaeCultureDynamics.progressPerTick(0.5, AlgaeCultureDynamics.RATED_AERATION) == 0.5, "a half-dense culture grows at half speed");
    }

    private static void starvingCulturesDieBackToTheSeed() {
        double density = 1.0;
        for (int tick = 0; tick < 6000; tick++) {
            density = AlgaeCultureDynamics.dieBack(density);
        }
        double expected = AlgaeCultureDynamics.SEED_DENSITY + (1.0 - AlgaeCultureDynamics.SEED_DENSITY) / 2.0;
        require(Math.abs(density - expected) < 0.001, "a starving culture loses half its density above the seed in five minutes, was " + density);
        for (int tick = 0; tick < 200_000; tick++) {
            density = AlgaeCultureDynamics.dieBack(density);
        }
        require(density >= AlgaeCultureDynamics.SEED_DENSITY, "a starving culture never dies below the seed");
        require(AlgaeCultureDynamics.dieBack(0.0) == 0.0, "an empty reactor stays empty");
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
