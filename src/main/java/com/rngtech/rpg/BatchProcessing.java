package com.rngtech.rpg;

import java.util.function.IntPredicate;

/** Shared batch rules: how many items a lane works in one cycle, and how much longer that cycle takes. */
public final class BatchProcessing {
    public static final int MAX_BATCH_SIZE = 16;
    public static final double BASE_BATCH_OVERHEAD = 25.0;
    public static final double MIN_BATCH_OVERHEAD = 5.0;

    /** The machine's Batch Size stat, clamped to the supported range. Batching opt-outs scale their payoff with it. */
    public static int statBatchSize(MachineStatAccumulator stats) {
        return Math.max(1, Math.min(MAX_BATCH_SIZE, stats.intValue(MachineStat.BATCH_SIZE)));
    }

    /** The largest batch a cycle may lock; 1 while a batching opt-out such as Refiner's Oath is allocated. */
    public static int batchSize(MachineStatAccumulator stats, boolean batchingOff) {
        return batchingOff ? 1 : statBatchSize(stats);
    }

    /** Batch Overhead as a fraction of the base cycle time per extra item. */
    public static double overhead(MachineStatAccumulator stats) {
        return Math.max(MIN_BATCH_OVERHEAD, stats.value(MachineStat.BATCH_OVERHEAD)) / 100.0;
    }

    public static double timeMultiplier(MachineStatAccumulator stats, int batch) {
        return 1.0 + overhead(stats) * Math.max(0, batch - 1);
    }

    /** Cycle ticks for a batch: every item past the first adds Batch Overhead of the single-item time. */
    public static int batchTicks(int ticks, MachineStatAccumulator stats, int batch) {
        long scaled = (long) Math.ceil(Math.max(1, ticks) * timeMultiplier(stats, batch));
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, scaled));
    }

    /** The largest batch from {@code limit} down to 1 that {@code fits}, or 0 when even one item does not. */
    public static int largestFitting(int limit, IntPredicate fits) {
        for (int batch = Math.min(limit, MAX_BATCH_SIZE); batch > 0; batch--) {
            if (fits.test(batch)) {
                return batch;
            }
        }
        return 0;
    }

    private BatchProcessing() {
    }
}
