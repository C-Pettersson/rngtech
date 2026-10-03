package com.rngtech.rpg;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fractions of extra output banked per output kind and paid out as whole items. Progress past a whole item waits until
 * the output has room, so nothing is lost to a full slot.
 */
public final class OutputLedger<K> {
    public static final int MAX_ENTRIES = 16;
    public static final double MAX_PROGRESS = 64.0;
    private static final double EPSILON = 1.0E-9;

    private final Map<K, Double> progress = new LinkedHashMap<>(MAX_ENTRIES, 0.75F, true);

    public void add(K key, double amount) {
        if (key == null || amount <= 0.0) {
            return;
        }
        progress.merge(key, amount, (current, added) -> Math.min(MAX_PROGRESS, current + added));
        while (progress.size() > MAX_ENTRIES) {
            progress.remove(progress.keySet().iterator().next());
        }
    }

    public double progress(K key) {
        return progress.getOrDefault(key, 0.0);
    }

    /** Whole items ready to pay for {@code key}. */
    public int payable(K key) {
        return (int) Math.floor(progress(key) + EPSILON);
    }

    public void pay(K key, int items) {
        double remaining = progress(key) - Math.max(0, items);
        if (remaining <= EPSILON) {
            progress.remove(key);
        } else {
            progress.put(key, remaining);
        }
    }

    public Map<K, Double> entries() {
        return Map.copyOf(progress);
    }

    public void restore(Map<K, Double> saved) {
        progress.clear();
        saved.forEach(this::add);
    }
}
