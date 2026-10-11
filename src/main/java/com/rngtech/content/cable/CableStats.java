package com.rngtech.content.cable;

import java.util.Arrays;
import java.util.Locale;

/**
 * Running totals of the work cable networks do, so a change to cable tick cost can be compared in a real world.
 * {@code /rngtech cable stats} prints them per tick since the last reset. Server thread only.
 */
public final class CableStats {
    public enum Counter {
        NETWORK_REBUILDS("network rebuilds"),
        CABLES_SCANNED("cables scanned by rebuilds"),
        TARGET_QUERIES("connector target queries"),
        TARGET_LOOKUPS("connector target capability lookups"),
        ENERGY_PASSES("network energy passes"),
        ENERGY_PORTS("energy connectors visited by passes"),
        ENERGY_LOST("FE no block would take back"),
        MODULE_ATTEMPTS("item and fluid row attempts"),
        MODULE_BACKOFFS("item and fluid rows backing off"),
        ITEM_RECEIVER_CHECKS("item receiver checks"),
        FLUID_RECEIVER_CHECKS("fluid receiver checks");

        private final String label;

        Counter(String label) {
            this.label = label;
        }
    }

    private static final long[] COUNTS = new long[Counter.values().length];
    private static long resetTick;

    private CableStats() {
    }

    public static void add(Counter counter, long amount) {
        COUNTS[counter.ordinal()] += amount;
    }

    public static void increment(Counter counter) {
        COUNTS[counter.ordinal()]++;
    }

    public static long count(Counter counter) {
        return COUNTS[counter.ordinal()];
    }

    public static void reset(long tick) {
        Arrays.fill(COUNTS, 0L);
        resetTick = tick;
    }

    /** One line per counter: the total since the last reset and the average per server tick. */
    public static String report(long tick) {
        long ticks = Math.max(1L, tick - resetTick);
        StringBuilder report = new StringBuilder("Cable stats over ").append(ticks).append(" ticks:");
        for (Counter counter : Counter.values()) {
            long total = COUNTS[counter.ordinal()];
            report.append('\n')
                    .append(counter.label)
                    .append(": ")
                    .append(total)
                    .append(" (")
                    .append(String.format(Locale.ROOT, "%.1f", (double) total / ticks))
                    .append("/t)");
        }
        return report.toString();
    }
}
