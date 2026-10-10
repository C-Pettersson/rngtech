package com.rngtech.content.cable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps each cable network's snapshot under every cable position it covers, until something changes it. Callers
 * report changes as events, and this class decides what they invalidate:
 *
 * <ul>
 *     <li>{@link #cableChanged}: a cable was placed, broken, loaded, unloaded, or changed its block state (links or
 *     connector faces). Its own network and every network touching it are dropped, so merges and splits rebuild.</li>
 *     <li>{@link #connectorChanged}: a connector, its settings, a link switch, a dye, or a connector's target changed
 *     on a cable. Only that cable's network is dropped.</li>
 * </ul>
 *
 * <p>A snapshot also expires after a safety lifetime, so a change no event reports cannot leave routing stale for
 * long. Rebuilding a network that overlaps an older snapshot drops the older one.</p>
 */
public final class NetworkIndex<S> {
    private static final Direction[] DIRECTIONS = Direction.values();

    private final Map<BlockPos, Entry<S>> byPosition = new HashMap<>();
    private long epoch;

    /** Compared by identity: two builds of the same network are different entries. */
    private static final class Entry<S> {
        private final S snapshot;
        private final List<BlockPos> positions;
        private final long builtTick;

        private Entry(S snapshot, List<BlockPos> positions, long builtTick) {
            this.snapshot = snapshot;
            this.positions = positions;
            this.builtTick = builtTick;
        }

        private S snapshot() {
            return snapshot;
        }

        private List<BlockPos> positions() {
            return positions;
        }

        private long builtTick() {
            return builtTick;
        }
    }

    /** The cached snapshot covering {@code pos}, or null when there is none or it outlived {@code lifetime}. */
    public S get(BlockPos pos, long tick, long lifetime) {
        Entry<S> entry = byPosition.get(pos);
        if (entry == null) {
            return null;
        }
        if (expired(entry, tick, lifetime)) {
            remove(entry);
            return null;
        }
        return entry.snapshot();
    }

    /** Stores a freshly built snapshot under every position it covers; an empty network is not stored. */
    public void put(S snapshot, List<BlockPos> positions, long tick) {
        if (positions.isEmpty()) {
            return;
        }
        Entry<S> entry = new Entry<>(snapshot, List.copyOf(positions), tick);
        for (BlockPos pos : entry.positions()) {
            Entry<S> previous = byPosition.get(pos);
            if (previous != null && previous != entry) {
                remove(previous);
            }
        }
        for (BlockPos pos : entry.positions()) {
            byPosition.put(pos, entry);
        }
    }

    public void cableChanged(BlockPos pos) {
        invalidate(pos);
        for (Direction direction : DIRECTIONS) {
            invalidate(pos.relative(direction));
        }
    }

    public void connectorChanged(BlockPos pos) {
        invalidate(pos);
    }

    /** Counts every reported change, so stalled rows can tell that something on some network changed. */
    public long epoch() {
        return epoch;
    }

    /** Drops every snapshot that outlived {@code lifetime}. */
    public void prune(long tick, long lifetime) {
        byPosition.values().removeIf(entry -> expired(entry, tick, lifetime));
    }

    public int size() {
        return byPosition.size();
    }

    private void invalidate(BlockPos pos) {
        epoch++;
        Entry<S> entry = byPosition.get(pos);
        if (entry != null) {
            remove(entry);
        }
    }

    private void remove(Entry<S> entry) {
        for (BlockPos pos : entry.positions()) {
            byPosition.remove(pos, entry);
        }
    }

    private static boolean expired(Entry<?> entry, long tick, long lifetime) {
        return tick < entry.builtTick() || tick - entry.builtTick() >= lifetime;
    }
}
