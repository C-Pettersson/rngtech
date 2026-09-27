package com.rngtech.rpg;

import net.minecraft.util.RandomSource;

public record MachineStatRange(MachineStat stat, double min, double max, boolean wholeNumber) {
    public MachineStatRange(MachineStat stat, double min, double max) {
        this(stat, min, max, false);
    }

    public MachineStatRange {
        if (max < min) {
            throw new IllegalArgumentException("Machine stat range max must be greater than or equal to min.");
        }
        if (wholeNumber && Math.ceil(min) > Math.floor(max)) {
            throw new IllegalArgumentException("Whole-number machine stat range must include at least one whole number.");
        }
    }

    public double roll(RandomSource random) {
        if (wholeNumber) {
            int minValue = (int) Math.ceil(min);
            int maxValue = (int) Math.floor(max);
            return minValue + random.nextInt(maxValue - minValue + 1);
        }
        if (min == max) {
            return min;
        }
        return min + (random.nextDouble() * (max - min));
    }
}
