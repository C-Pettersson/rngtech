package com.rngtech.content.energy;

import net.minecraft.util.Mth;

/**
 * The Solid Fuel Burner's stoke: how hard the fire is driven. Rated stoke (100%) runs at the Heat Core's FE/t and gets
 * each fuel item's full FE, exactly as an unstoked burner. Turning it down is a visible trade: FE/t falls in proportion,
 * and the slower, cooler burn gets up to {@link #MAX_FUEL_BONUS} more FE from each item at the minimum stoke. Stoke never
 * raises FE/t above the Heat Core's rating.
 */
public final class SolidFuelBurnerStoke {
    public static final int MIN_STOKE = 25;
    public static final int MAX_STOKE = 100;
    public static final int RATED_STOKE = MAX_STOKE;
    public static final double MAX_FUEL_BONUS = 0.20;
    /** Quick Feed holds the set stoke up to this cell fill, then eases toward the minimum at a full cell. */
    public static final double QUICK_FEED_EASE_START = 0.5;

    private SolidFuelBurnerStoke() {
    }

    public static int clamp(int stoke) {
        return Mth.clamp(stoke, MIN_STOKE, MAX_STOKE);
    }

    /** FE/t at this stoke from a Heat Core rated for {@code rated} FE/t; a running burner always makes at least 1 FE/t. */
    public static int generation(int rated, int stoke) {
        if (rated <= 0) {
            return 0;
        }
        return Math.max(1, (int) Math.round(rated * clamp(stoke) / (double) MAX_STOKE));
    }

    /**
     * Quick Feed's running stoke: the set stoke while the cell is at most half full, then a straight slide to the minimum
     * at a full cell. The set stoke stays the ceiling, so the fire never runs harder than the gauge.
     */
    public static int quickFeed(int setStoke, double cellFill) {
        double fill = Mth.clamp(cellFill, 0.0, 1.0);
        if (fill <= QUICK_FEED_EASE_START) {
            return clamp(setStoke);
        }
        double eased = MAX_STOKE - (MAX_STOKE - MIN_STOKE) * (fill - QUICK_FEED_EASE_START) / (1.0 - QUICK_FEED_EASE_START);
        return clamp(Math.min(setStoke, (int) Math.round(eased)));
    }

    /** FE per fuel item against rated stoke: 1.0 at 100%, rising linearly to 1.2 at the minimum. */
    public static double fuelEconomy(int stoke) {
        return 1.0 + MAX_FUEL_BONUS * (MAX_STOKE - clamp(stoke)) / (double) (MAX_STOKE - MIN_STOKE);
    }
}
