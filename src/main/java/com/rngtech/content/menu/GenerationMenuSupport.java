package com.rngtech.content.menu;

import com.rngtech.RNGTechConfig;

/** Sends a generator's pack energy multiplier to its menu, so the Stats tab can name it. */
public final class GenerationMenuSupport {
    private static final double PACK_MULTIPLIER_SCALE = 1000.0;

    private GenerationMenuSupport() {
    }

    /** The menu data value for a Stage {@code stage} generator's pack multiplier. */
    public static int packMultiplierData(int stage) {
        return packMultiplierData(RNGTechConfig.packGenerationMultiplier(stage));
    }

    public static int packMultiplierData(double multiplier) {
        return (int) Math.round(multiplier * PACK_MULTIPLIER_SCALE);
    }

    static double packMultiplier(int data) {
        return data <= 0 ? 1.0 : data / PACK_MULTIPLIER_SCALE;
    }
}
