package com.rngtech.util;

/**
 * Counts an amount moved during the current game tick and remembers the previous tick's total.
 *
 * <p>Transfer caps are per tick, not per call: several callers in one tick share the same budget.</p>
 */
public final class TickTransferCounter {
    private long tick = Long.MIN_VALUE;
    private int current;
    private int previous;

    public int remaining(long gameTime, int cap) {
        roll(gameTime);
        return Math.max(0, cap - current);
    }

    public void add(long gameTime, int amount) {
        if (amount <= 0) {
            return;
        }
        roll(gameTime);
        current = saturatedAdd(current, amount);
    }

    public int lastTick(long gameTime) {
        roll(gameTime);
        return previous;
    }

    private void roll(long gameTime) {
        if (tick == gameTime) {
            return;
        }
        previous = tick == gameTime - 1 ? current : 0;
        tick = gameTime;
        current = 0;
    }

    private static int saturatedAdd(int first, int second) {
        long sum = (long) first + second;
        return sum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum;
    }
}
