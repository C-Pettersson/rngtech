package com.rngtech.content.energy;

/** Bio Generator digestion: the rated retention keeps the old fuel balance, and every other retention is a visible trade. */
public final class BiogasDigestionChecks {
    private static final double[] RATED_GENERATIONS = {0.8, 1.0, 7.2, 8.0, 8.4, 9.6, 12.0, 16.5};
    private static int checks;

    private BiogasDigestionChecks() {
    }

    public static void run() {
        checks = 0;
        ratedRetentionKeepsTheFuelBalance();
        ratedRetentionKeepsTheBurnerFormula();
        retentionTradesAreMonotonic();
        noRetentionBeatsRatedOnBoth();
        digestionTimeFollowsRetention();
        curveEndpoints();
        retentionIsClamped();
        holderStartsBeforeItFills();
        takeOffGasIsSixtyFortyMethane();
        membraneRecoveryFollowsCalibration();
        takeOffNeverBeatsTheMelter();
        takeOffLoopsReturnLessThanTheyUse();
        System.out.println("Biogas digestion: " + checks + " checks passed");
    }

    private static void ratedRetentionKeepsTheFuelBalance() {
        require(BiogasDigestion.gasYield(BiogasDigestion.RATED_RETENTION_DAYS) == 1.0, "rated retention gives the listed FE per item exactly");
        require(BiogasDigestion.digestionRate(BiogasDigestion.RATED_RETENTION_DAYS) == 1.0, "rated retention digests at the listed FE/t exactly");
        for (double rated : RATED_GENERATIONS) {
            require(BiogasDigestion.engineRate(rated, BiogasDigestion.RATED_RETENTION_DAYS) == Math.max(1, (int) Math.ceil(rated)),
                    "rated retention keeps the rounded-up generation for " + rated + " FE/t");
        }
    }

    /**
     * The generator before the digester burned an item for {@code ceil(ceil(FE / 8) * duration)} ticks at its rate. At
     * rated retention the digester and engine move the same FE per item at the same FE/t.
     */
    private static void ratedRetentionKeepsTheBurnerFormula() {
        int[] fuels = {40, 80, 120, 200, 240, 400, 546, 1600, 2400, 3200};
        double[] scales = {1.0, 1.15, 1.37, 0.9};
        for (int fuel : fuels) {
            for (double scale : scales) {
                for (double rated : RATED_GENERATIONS) {
                    int rate = Math.max(1, (int) Math.ceil(rated));
                    int ticks = Math.max(1, (int) Math.ceil(fuel * scale / 8.0));
                    double feed = ticks * (double) rate;
                    double gas = feed * BiogasDigestion.gasYield(BiogasDigestion.RATED_RETENTION_DAYS);
                    int engine = BiogasDigestion.engineRate(rated, BiogasDigestion.RATED_RETENTION_DAYS);
                    require(gas == feed && engine == rate && Math.ceil(gas / engine) == ticks,
                            "rated retention matches the old burn of " + fuel + " FE at " + rated + " FE/t");
                }
            }
        }
    }

    private static void retentionTradesAreMonotonic() {
        for (int days = BiogasDigestion.MIN_RETENTION_DAYS; days < BiogasDigestion.MAX_RETENTION_DAYS; days++) {
            require(BiogasDigestion.gasYield(days) < BiogasDigestion.gasYield(days + 1), "longer retention gets more gas per item at " + days + " days");
            require(BiogasDigestion.digestionRate(days) > BiogasDigestion.digestionRate(days + 1), "shorter retention digests faster at " + days + " days");
        }
    }

    /** Every setting off rated wins one side and loses the other, so no setting is a free upgrade. */
    private static void noRetentionBeatsRatedOnBoth() {
        for (int days = BiogasDigestion.MIN_RETENTION_DAYS; days <= BiogasDigestion.MAX_RETENTION_DAYS; days++) {
            double rate = BiogasDigestion.digestionRate(days);
            double yield = BiogasDigestion.gasYield(days);
            if (days < BiogasDigestion.RATED_RETENTION_DAYS) {
                require(rate > 1.0 && yield < 1.0, days + " days trades FE per item for FE/t");
            } else if (days > BiogasDigestion.RATED_RETENTION_DAYS) {
                require(rate < 1.0 && yield > 1.0, days + " days trades FE/t for FE per item");
            }
        }
    }

    /** Gas per item over gas per tick is the digestion time, which scales with the retention like a real digester's. */
    private static void digestionTimeFollowsRetention() {
        for (int days = BiogasDigestion.MIN_RETENTION_DAYS; days <= BiogasDigestion.MAX_RETENTION_DAYS; days++) {
            double time = BiogasDigestion.gasYield(days) / BiogasDigestion.digestionRate(days);
            near(time, days / (double) BiogasDigestion.RATED_RETENTION_DAYS, "an item digests over " + days + " days of retention");
        }
    }

    private static void curveEndpoints() {
        near(BiogasDigestion.digestedShare(BiogasDigestion.RATED_RETENTION_DAYS), 0.75, "rated retention recovers three quarters of the feed's biogas");
        near(BiogasDigestion.digestionRate(BiogasDigestion.MIN_RETENTION_DAYS), 1.6, "the shortest retention digests 1.6x faster");
        near(BiogasDigestion.gasYield(BiogasDigestion.MIN_RETENTION_DAYS), 0.8, "the shortest retention keeps 80% of the gas per item");
        near(BiogasDigestion.gasYield(BiogasDigestion.MAX_RETENTION_DAYS), 8.0 / 7.0, "the longest retention gets 8/7 of the gas per item");
        near(BiogasDigestion.digestionRate(BiogasDigestion.MAX_RETENTION_DAYS), 4.0 / 7.0, "the longest retention digests at 4/7 of the pace");
        require(BiogasDigestion.engineRate(8.0, BiogasDigestion.MIN_RETENTION_DAYS) == 13, "an unrolled generator runs 13 FE/t at 10 days");
        require(BiogasDigestion.engineRate(8.0, BiogasDigestion.MAX_RETENTION_DAYS) == 5, "an unrolled generator runs 5 FE/t at 40 days");
    }

    private static void retentionIsClamped() {
        require(BiogasDigestion.clampRetention(1) == BiogasDigestion.MIN_RETENTION_DAYS, "retention clamps to the minimum");
        require(BiogasDigestion.clampRetention(99) == BiogasDigestion.MAX_RETENTION_DAYS, "retention clamps to the maximum");
        require(BiogasDigestion.gasYield(5) == BiogasDigestion.gasYield(BiogasDigestion.MIN_RETENTION_DAYS), "the curve clamps out-of-range days");
    }

    private static void holderStartsBeforeItFills() {
        require(BiogasDigestion.ENGINE_START_GAS > 0 && BiogasDigestion.ENGINE_START_GAS < BiogasDigestion.HOLDER_CAPACITY,
                "the engine starts on part of the holder");
    }

    private static void takeOffGasIsSixtyFortyMethane() {
        double methane = BiogasDigestion.METHANE_PER_FE;
        double carbon = BiogasDigestion.CARBON_EXHAUST_PER_FE;
        near(methane / (methane + carbon), 0.6, "biogas is 60% Methane by volume");
        near(methane, 0.05, "the take-off holds 1 mB Methane per 20 FE of gas");
    }

    private static void membraneRecoveryFollowsCalibration() {
        near(BiogasDigestion.membraneRecovery(0), 0.85, "a stability 0 membrane recovers 85%");
        near(BiogasDigestion.membraneRecovery(100), 0.98, "a perfect membrane recovers 98%");
        require(BiogasDigestion.membraneRecovery(-5) == BiogasDigestion.membraneRecovery(0)
                && BiogasDigestion.membraneRecovery(150) == BiogasDigestion.membraneRecovery(100), "stability clamps to 0-100");
        for (int stability = 1; stability <= 100; stability++) {
            require(BiogasDigestion.membraneRecovery(stability) > BiogasDigestion.membraneRecovery(stability - 1),
                    "better calibration always recovers more");
        }
        require(BiogasDigestion.membraneRecovery(0) > BiogasDigestion.PUMP_RECOVERY, "any membrane beats a bare pump");
        require(BiogasDigestion.membraneRecovery(100) < 1.0, "some Methane always slips");
    }

    /**
     * The Melter turns one Algae Biomass and one Organic Reagent (946 FE as Bio Generator fuel) into 1,000 mB Methane.
     * The same items through the best take-off must give far less, so algae and reagent still go to the Melter.
     */
    private static void takeOffNeverBeatsTheMelter() {
        double takeOff = (400 + 546) * BiogasDigestion.METHANE_PER_FE * BiogasDigestion.membraneRecovery(100);
        require(takeOff * 10.0 < 1000.0, "the Melter gets over ten times the Methane per item: take-off gives " + takeOff + " mB");
    }

    /**
     * Neither by-product feeds back into more than it came from: a reform's 500 mB Carbon Monoxide grows one 400 FE Algae
     * Biomass, and Algae grows on 100 mB Carbon Exhaust.
     */
    private static void takeOffLoopsReturnLessThanTheyUse() {
        double reformReturn = 400 * BiogasDigestion.METHANE_PER_FE * BiogasDigestion.membraneRecovery(100) / 1000.0;
        require(reformReturn < 0.05, "reformed Methane returns " + reformReturn + " of itself through algae");
        double carbonReturn = BiogasDigestion.CARBON_EXHAUST_PER_FE / 100.0 * 400;
        require(carbonReturn < 0.25, "captured CO2 returns " + carbonReturn + " of its gas through algae");
    }

    private static void near(double actual, double expected, String message) {
        require(Math.abs(actual - expected) < 1.0E-9, message + ": expected " + expected + ", got " + actual);
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new IllegalStateException("Biogas digestion check failed: " + message);
        }
    }
}
