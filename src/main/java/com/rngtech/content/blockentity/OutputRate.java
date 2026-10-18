package com.rngtech.content.blockentity;

/** Expected output per minute, in tenths so a container data slot can sync it. */
public final class OutputRate {
    private static final double TENTHS_PER_MINUTE = 1200.0D * 10.0D;

    /** Tenths of items or mB per minute for {@code perCycle} output every {@code ticks}; 0 when nothing runs. */
    public static int perMinuteTenths(double perCycle, int ticks) {
        if (perCycle <= 0.0D || ticks <= 0) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, Math.round(perCycle * TENTHS_PER_MINUTE / ticks));
    }

    private OutputRate() {
    }
}
