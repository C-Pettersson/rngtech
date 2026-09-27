package com.rngtech.rpg;

import net.minecraft.util.RandomSource;

import java.util.List;
import java.util.Objects;

public record ModifierEffectDefinition(
        MachineStat stat,
        ModifierOperation operation,
        List<ModifierValueRange> tierRanges
) {
    public ModifierEffectDefinition {
        Objects.requireNonNull(stat, "stat");
        Objects.requireNonNull(operation, "operation");
        tierRanges = List.copyOf(tierRanges);
    }

    public static ModifierEffectDefinition of(
            MachineStat stat,
            ModifierOperation operation,
            List<ModifierValueRange> tierRanges
    ) {
        return new ModifierEffectDefinition(stat, operation, tierRanges);
    }

    public MachineModifierEffect roll(int tier, RandomSource random) {
        ModifierValueRange range = rangeForTier(tier);
        return MachineModifierEffect.roll(stat, operation, range, random);
    }

    public ModifierValueRange rangeForTier(int tier) {
        if (tierRanges.isEmpty()) {
            throw new IllegalStateException("Rollable modifier effect has no tier ranges: " + this);
        }
        int index = Math.max(1, Math.min(tier, tierRanges.size())) - 1;
        return tierRanges.get(index);
    }
}
