package com.rngtech.rpg;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Bonus output banks for recently processed inputs, most recent first. With a memory of one, a new input replaces the
 * bank, as a single bonus bar always has; a larger memory keeps each input's progress until it is the oldest of too many.
 */
public final class BonusBanks<K> {
    /** Whole items a bank can hold beyond its fraction, for salvage that did not fit the output. */
    public static final double MAX_PROGRESS = 64.0;

    private final BiPredicate<K, K> same;
    private final List<Bank<K>> banks = new ArrayList<>();
    private int memory = 1;

    public BonusBanks(BiPredicate<K, K> same) {
        this.same = same;
    }

    /** One remembered input: its fractional progress and eligible cycles toward a Super Output cadence. */
    public static final class Bank<K> {
        private final K key;
        private double progress;
        private int cycles;

        public Bank(K key, double progress, int cycles) {
            this.key = key;
            this.progress = clamp(progress);
            this.cycles = Math.max(0, cycles);
        }

        public K key() { return key; }
        public double progress() { return progress; }
        public int cycles() { return cycles; }
    }

    public int memory() {
        return memory;
    }

    public void setMemory(int memory) {
        this.memory = Math.max(1, memory);
        trim();
    }

    /** Makes {@code key} the current input, restoring its bank when remembered. */
    public void select(K key) {
        int index = indexOf(key);
        if (index == 0) {
            return;
        }
        Bank<K> bank = index > 0 ? banks.remove(index) : new Bank<>(key, 0.0, 0);
        banks.addFirst(bank);
        trim();
    }

    /** Empties the current input's bank and cadence count, keeping the input current. */
    public void resetCurrent() {
        if (!banks.isEmpty()) {
            banks.getFirst().progress = 0.0;
            banks.getFirst().cycles = 0;
        }
    }

    public void clear() {
        banks.clear();
    }

    public K currentKey() {
        return banks.isEmpty() ? null : banks.getFirst().key;
    }

    public double currentProgress() {
        return banks.isEmpty() ? 0.0 : banks.getFirst().progress;
    }

    public void setCurrentProgress(double progress) {
        if (!banks.isEmpty()) {
            banks.getFirst().progress = clamp(progress);
        }
    }

    /** Progress for {@code key}, or none when it is not remembered. */
    public double progressFor(K key) {
        int index = indexOf(key);
        return index < 0 ? 0.0 : banks.get(index).progress;
    }

    /** Adds whole or partial items to the current bank. */
    public void bank(double amount) {
        if (!banks.isEmpty() && amount > 0.0) {
            banks.getFirst().progress = clamp(banks.getFirst().progress + amount);
        }
    }

    /**
     * Counts one eligible cycle on the current input. Returns true on every {@code cadence}th cycle, which then starts
     * over; a cadence below one counts nothing.
     */
    public boolean countCycle(int cadence) {
        if (cadence < 1 || banks.isEmpty()) {
            return false;
        }
        Bank<K> bank = banks.getFirst();
        bank.cycles++;
        if (bank.cycles < cadence) {
            return false;
        }
        bank.cycles = 0;
        return true;
    }

    /** Whether selecting {@code key} would push out a bank that still holds progress. */
    public boolean wouldEvictProgress(K key) {
        if (indexOf(key) >= 0 || banks.size() < memory) {
            return false;
        }
        return banks.getLast().progress > 0.0;
    }

    public List<Bank<K>> banks() {
        return List.copyOf(banks);
    }

    /** Restores saved banks, most recent first, within the current memory. */
    public void restore(List<Bank<K>> saved) {
        banks.clear();
        for (Bank<K> bank : saved) {
            if (bank.key != null && indexOf(bank.key) < 0) {
                banks.add(bank);
            }
        }
        trim();
    }

    private int indexOf(K key) {
        for (int index = 0; index < banks.size(); index++) {
            if (same.test(banks.get(index).key, key)) {
                return index;
            }
        }
        return -1;
    }

    private void trim() {
        while (banks.size() > memory) {
            banks.removeLast();
        }
    }

    private static double clamp(double progress) {
        return Math.max(0.0, Math.min(MAX_PROGRESS, progress));
    }
}
