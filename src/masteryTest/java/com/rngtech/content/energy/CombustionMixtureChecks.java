package com.rngtech.content.energy;

/** Syngas Combustor fuel mixture: the rated mixture keeps the recipe, and every other setting is a visible trade. */
public final class CombustionMixtureChecks {
    /** Base FE/t of the Stage 5 Syngas recipe and the Stage 6 Ammonia Fuel Cell. */
    private static final double SYNGAS_FE_PER_TICK = 240.0;
    private static final double AMMONIA_FE_PER_TICK = 600.0;
    private static int checks;

    private CombustionMixtureChecks() {
    }

    public static int run() {
        checks = 0;
        ratedMixtureKeepsTheRecipe();
        richTradesFuelAndExhaustForPower();
        richestMixtureStaysBelowTheNextStage();
        return checks;
    }

    private static void ratedMixtureKeepsTheRecipe() {
        int rated = CombustionMixture.RATED_MIXTURE;
        require(CombustionMixture.fuelRate(rated) == 1.0, "the rated mixture burns fuel at the recipe rate");
        require(CombustionMixture.power(rated) == 1.0, "the rated mixture makes the recipe's FE/t");
        require(CombustionMixture.energyPerFuel(rated) == 1.0, "the rated mixture makes the recipe's FE per mB");
        require(CombustionMixture.exhaustPerFuel(rated) == 1.0, "the rated mixture leaves the recipe's exhaust");
        for (int amount : new int[] {125, 500}) {
            require(CombustionMixture.exhaustAmount(amount, rated) == amount, "the rated mixture leaves " + amount + " mB of exhaust");
        }
        require(CombustionMixture.clamp(10) == CombustionMixture.MIN_MIXTURE, "the mixture clamps to its leanest");
        require(CombustionMixture.clamp(500) == CombustionMixture.MAX_MIXTURE, "the mixture clamps to its richest");
    }

    /** A richer mixture always burns faster and makes more FE/t, but less FE and more exhaust per mB of fuel. */
    private static void richTradesFuelAndExhaustForPower() {
        double lastFuel = 0.0;
        double lastPower = 0.0;
        double lastEnergy = Double.MAX_VALUE;
        double lastExhaust = 0.0;
        double lastGain = Double.MAX_VALUE;
        for (int mixture = CombustionMixture.MIN_MIXTURE; mixture <= CombustionMixture.MAX_MIXTURE; mixture++) {
            double fuel = CombustionMixture.fuelRate(mixture);
            double power = CombustionMixture.power(mixture);
            double energy = CombustionMixture.energyPerFuel(mixture);
            double exhaust = CombustionMixture.exhaustPerFuel(mixture);
            require(fuel > lastFuel, "mixture " + mixture + "% burns fuel faster than one step leaner");
            require(power > lastPower, "mixture " + mixture + "% makes more FE/t than one step leaner");
            require(energy < lastEnergy, "mixture " + mixture + "% makes less FE per mB than one step leaner");
            require(exhaust > lastExhaust, "mixture " + mixture + "% leaves more exhaust per mB than one step leaner");
            require(power < fuel || mixture <= CombustionMixture.RATED_MIXTURE, "a rich mixture gains less FE/t than fuel");
            require(power > fuel || mixture >= CombustionMixture.RATED_MIXTURE, "a lean mixture loses less FE/t than fuel");
            if (lastPower > 0.0) {
                double gain = power - lastPower;
                require(gain < lastGain, "mixture " + mixture + "% gains less FE/t than the step before");
                lastGain = gain;
            }
            lastFuel = fuel;
            lastPower = power;
            lastEnergy = energy;
            lastExhaust = exhaust;
        }
    }

    private static void richestMixtureStaysBelowTheNextStage() {
        double richest = SYNGAS_FE_PER_TICK * CombustionMixture.power(CombustionMixture.MAX_MIXTURE);
        require(richest < AMMONIA_FE_PER_TICK * 0.6, "the richest Syngas mixture stays well below the Ammonia Fuel Cell, was " + richest);
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
