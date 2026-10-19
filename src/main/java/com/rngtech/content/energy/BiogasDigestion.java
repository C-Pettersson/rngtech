package com.rngtech.content.energy;

import net.minecraft.util.Mth;

/**
 * Anaerobic digestion in the Bio Generator. Feed digests with first-order kinetics, so the share of its gas a digester
 * recovers in retention time {@code t} is {@code k t / (1 + k t)}. The rated retention recovers each fuel's listed FE at
 * the listed FE/t; shorter retention trades FE per item for FE/t, and longer retention trades FE/t for FE per item.
 */
public final class BiogasDigestion {
    public static final int MIN_RETENTION_DAYS = 10;
    public static final int RATED_RETENTION_DAYS = 20;
    public static final int MAX_RETENTION_DAYS = 40;
    /** {@code k t} at rated retention: the rated feed gives up three quarters of its ultimate gas. */
    public static final double RATED_DIGESTION = 3.0;
    /** Biogas the gas holder stores, in FE. */
    public static final int HOLDER_CAPACITY = 1600;
    /** Biogas the engine needs in the holder before it starts, in FE. */
    public static final int ENGINE_START_GAS = 400;
    /** Methane in the biogas, in mB per FE of gas: 60% of the gas by volume. */
    public static final double METHANE_PER_FE = 1.0 / 20.0;
    /** CO2 in the biogas, in mB of Carbon Exhaust per FE of gas: the other 40%. */
    public static final double CARBON_EXHAUST_PER_FE = 1.0 / 30.0;
    /** Share of the Methane a Fluid Pump recovers with no membrane; the rest slips out with the vented CO2. */
    public static final double PUMP_RECOVERY = 0.70;
    /** Methane a Biogas Membrane recovers at stability 0, and the extra it gains at stability 100. */
    public static final double MEMBRANE_BASE_RECOVERY = 0.85;
    public static final double MEMBRANE_RECOVERY_SPAN = 0.13;
    /** Each take-off output tank before Fluid Capacity, in mB. */
    public static final int TAKE_OFF_TANK_CAPACITY = 1000;

    private BiogasDigestion() {
    }

    /** Methane a membrane recovers, from the stability of the calibrated component it was assembled from. */
    public static double membraneRecovery(int stability) {
        return MEMBRANE_BASE_RECOVERY + MEMBRANE_RECOVERY_SPAN * Mth.clamp(stability, 0, 100) / 100.0;
    }

    public static int clampRetention(int days) {
        return Mth.clamp(days, MIN_RETENTION_DAYS, MAX_RETENTION_DAYS);
    }

    /** Gas per item against the rated retention: 1 at rated, below 1 when shorter, above 1 when longer. */
    public static double gasYield(int days) {
        return ratio(days) * digestionRate(days);
    }

    /** Gas per tick against the rated retention: the same feed digests in proportionally less time. */
    public static double digestionRate(int days) {
        return (1.0 + RATED_DIGESTION) / (1.0 + RATED_DIGESTION * ratio(days));
    }

    /** Share of the feed's ultimate gas the digester recovers, for display. */
    public static double digestedShare(int days) {
        double kt = RATED_DIGESTION * ratio(days);
        return kt / (1.0 + kt);
    }

    /** Engine FE/t at this retention, rounded up like the rated generation; the rated retention keeps it unchanged. */
    public static int engineRate(double ratedGeneration, int days) {
        return Math.max(1, Mth.ceil(ratedGeneration * digestionRate(days)));
    }

    private static double ratio(int days) {
        return clampRetention(days) / (double) RATED_RETENTION_DAYS;
    }
}
