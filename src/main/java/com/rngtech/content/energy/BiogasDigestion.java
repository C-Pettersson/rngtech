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

    private BiogasDigestion() {
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
