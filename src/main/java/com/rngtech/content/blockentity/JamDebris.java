package com.rngtech.content.blockentity;

/** When an under-level Crusher jam leaves Jam Debris, the Volatile Catalyst's Crusher ingredient. */
public final class JamDebris {
    public static final int MIN_PROCESSING_LEVEL = 5;

    private JamDebris() {
    }

    public static boolean dropsFrom(int requiredProcessingLevel) {
        return requiredProcessingLevel >= MIN_PROCESSING_LEVEL;
    }
}
