package com.rngtech.content.cable;

/**
 * Keeps an item or fluid row that found nothing to move from scanning its machine every tick. It retries after the
 * shorter of its tier's wait and one second, or as soon as its own network's version moves: a link, connector, setting,
 * or connector target changed on that network. Changes on other networks leave it waiting.
 */
public final class StallBackoff {
    public static final int MAX_RETRY_TICKS = 20;

    private int ticksLeft;
    private long networkVersion;

    public void start(long networkVersion, int waitTicks) {
        ticksLeft = Math.max(1, Math.min(waitTicks, MAX_RETRY_TICKS));
        this.networkVersion = networkVersion;
    }

    public boolean active() {
        return ticksLeft > 0;
    }

    /** True while the row should skip this tick; counts the tick down. */
    public boolean waiting(long networkVersion) {
        if (ticksLeft <= 0) {
            return false;
        }
        if (networkVersion != this.networkVersion) {
            ticksLeft = 0;
            return false;
        }
        ticksLeft--;
        return true;
    }

    public void clear() {
        ticksLeft = 0;
    }
}
