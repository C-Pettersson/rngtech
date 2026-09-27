package com.rngtech.rpg;

import net.minecraft.util.RandomSource;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ModifierDefinition(
        String id,
        String modGroup,
        ModifierSlot slot,
        MachineStat stat,
        ModifierOperation operation,
        List<ModifierValueRange> tierRanges,
        List<ModifierEffectDefinition> effects,
        Set<ModifierFlag> flags,
        int rollWeight,
        List<ModifierTierWeight> tierWeights,
        Set<ModifierLensTag> lensTags
) {
    public static final int DEFAULT_ROLL_WEIGHT = 100;
    public static final int DEFAULT_TIER_WEIGHT = 100;

    public ModifierDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(modGroup, "modGroup");
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(stat, "stat");
        Objects.requireNonNull(operation, "operation");
        tierRanges = List.copyOf(tierRanges);
        effects = effects.isEmpty()
                ? List.of(ModifierEffectDefinition.of(stat, operation, tierRanges))
                : List.copyOf(effects);
        flags = Set.copyOf(flags);
        rollWeight = Math.max(0, rollWeight);
        tierWeights = normalizeTierWeights(tierWeights, maxTier(tierRanges, effects));
        lensTags = lensTags == null || lensTags.isEmpty()
                ? ModifierLensTag.infer(stat, effects)
                : Set.copyOf(lensTags);
    }

    public static ModifierDefinition rollable(
            String id,
            String modGroup,
            ModifierSlot slot,
            MachineStat stat,
            ModifierOperation operation,
            List<ModifierValueRange> tierRanges
    ) {
        return new ModifierDefinition(
                id,
                modGroup,
                slot,
                stat,
                operation,
                tierRanges,
                List.of(),
                Set.of(ModifierFlag.CAN_ROLL),
                DEFAULT_ROLL_WEIGHT,
                List.of(),
                Set.of()
        );
    }

    public static ModifierDefinition rollableUntiered(
            String id,
            String modGroup,
            ModifierSlot slot,
            MachineStat stat,
            ModifierOperation operation,
            ModifierValueRange range
    ) {
        return new ModifierDefinition(
                id,
                modGroup,
                slot,
                stat,
                operation,
                List.of(range),
                List.of(),
                Set.of(ModifierFlag.CAN_ROLL, ModifierFlag.UNTIERED),
                DEFAULT_ROLL_WEIGHT,
                List.of(),
                Set.of()
        );
    }

    public static ModifierDefinition rollable(
            String id,
            String modGroup,
            ModifierSlot slot,
            List<ModifierEffectDefinition> effects
    ) {
        if (effects.isEmpty()) {
            throw new IllegalArgumentException("Compound modifier definition must have at least one effect.");
        }
        ModifierEffectDefinition primary = effects.getFirst();
        return new ModifierDefinition(
                id,
                modGroup,
                slot,
                primary.stat(),
                primary.operation(),
                primary.tierRanges(),
                effects,
                Set.of(ModifierFlag.CAN_ROLL),
                DEFAULT_ROLL_WEIGHT,
                List.of(),
                Set.of()
        );
    }

    public static ModifierDefinition rollable(
            ModifierSlot slot,
            MachineStat stat,
            ModifierOperation operation,
            List<ModifierValueRange> tierRanges
    ) {
        return rollable(stat.getSerializedName(), defaultGroup(stat, operation), slot, stat, operation, tierRanges);
    }

    public static ModifierDefinition fixed(
            ModifierSlot slot,
            MachineStat stat,
            ModifierOperation operation
    ) {
        return new ModifierDefinition(
                stat.getSerializedName(),
                defaultGroup(stat, operation),
                slot,
                stat,
                operation,
                List.of(),
                List.of(),
                Set.of(),
                DEFAULT_ROLL_WEIGHT,
                List.of(),
                Set.of()
        );
    }

    public ModifierDefinition withRollWeight(int rollWeight) {
        return new ModifierDefinition(
                id,
                modGroup,
                slot,
                stat,
                operation,
                tierRanges,
                effects,
                flags,
                rollWeight,
                tierWeights,
                lensTags
        );
    }

    public ModifierDefinition withTierWeights(List<ModifierTierWeight> tierWeights) {
        return new ModifierDefinition(
                id,
                modGroup,
                slot,
                stat,
                operation,
                tierRanges,
                effects,
                flags,
                rollWeight,
                tierWeights,
                lensTags
        );
    }

    public ModifierDefinition withLensTags(Set<ModifierLensTag> lensTags) {
        return new ModifierDefinition(
                id,
                modGroup,
                slot,
                stat,
                operation,
                tierRanges,
                effects,
                flags,
                rollWeight,
                tierWeights,
                lensTags
        );
    }

    public ModifierDefinition withoutTargetedRefinement() {
        return withFlag(ModifierFlag.NON_TARGETABLE);
    }

    public ModifierDefinition asBehaviorOnly() {
        return withFlags(ModifierFlag.BEHAVIOR_ONLY, ModifierFlag.NON_TARGETABLE);
    }

    public ModifierDefinition withFlag(ModifierFlag flag) {
        return withFlags(flag);
    }

    private ModifierDefinition withFlags(ModifierFlag... additionalFlags) {
        LinkedHashSet<ModifierFlag> mergedFlags = new LinkedHashSet<>(flags);
        mergedFlags.addAll(List.of(additionalFlags));
        return new ModifierDefinition(
                id,
                modGroup,
                slot,
                stat,
                operation,
                tierRanges,
                effects,
                mergedFlags,
                rollWeight,
                tierWeights,
                lensTags
        );
    }

    public boolean canRoll() {
        return flags.contains(ModifierFlag.CAN_ROLL);
    }

    public boolean canTargetWithLens() {
        return canRoll() && !flags.contains(ModifierFlag.NON_TARGETABLE);
    }

    public boolean matchesLensTags(Set<ModifierLensTag> targetLensTags) {
        return !targetLensTags.isEmpty() && lensTags.stream().anyMatch(targetLensTags::contains);
    }

    public boolean isBehaviorOnly() {
        return flags.contains(ModifierFlag.BEHAVIOR_ONLY);
    }

    public boolean isTiered() {
        return !flags.contains(ModifierFlag.UNTIERED);
    }

    public int maxTier() {
        return maxTier(tierRanges, effects);
    }

    public int tierWeight(int tier) {
        int normalizedTier = Math.max(1, tier);
        return tierWeights.stream()
                .filter(weight -> weight.tier() == normalizedTier)
                .mapToInt(ModifierTierWeight::weight)
                .findFirst()
                .orElse(DEFAULT_TIER_WEIGHT);
    }

    public MachineModifier roll(int tier, RandomSource random) {
        if (!canRoll()) {
            throw new IllegalStateException("Cannot roll fixed modifier definition: " + this);
        }
        int storedTier = isTiered() ? tier : 0;
        if (effects.size() == 1) {
            return MachineModifier.roll(id, modGroup, slot, stat, operation, storedTier, rangeForTier(tier), random);
        }
        return MachineModifier.roll(
                id,
                modGroup,
                slot,
                storedTier,
                effects.stream()
                        .map(effect -> effect.roll(tier, random))
                        .toList()
        );
    }

    public boolean matches(MachineModifier modifier) {
        if (modifier.slot() != slot) {
            return false;
        }
        if (modifier.hasAffixId()) {
            return modifier.affixId().equals(id) || isLegacyVariantId(modifier.affixId(), id);
        }
        if (isLegacyFlatEnergyCapacityModifier(modifier)) {
            return true;
        }
        return modifier.stat() == stat && modifier.operation() == operation;
    }

    public boolean conflictsWith(MachineModifier modifier) {
        if (!modifier.slot().isAffix()) {
            return false;
        }
        return matches(modifier)
                || (!modGroup.isBlank() && modGroup.equals(modifier.modGroup()))
                || (!modifier.hasAffixId() && modifier.stat() == stat && modifier.operation() == operation);
    }

    public ModifierValueRange rangeForTier(int tier) {
        if (tierRanges.isEmpty()) {
            throw new IllegalStateException("Rollable modifier definition has no tier ranges: " + this);
        }
        int index = Math.max(1, Math.min(tier, tierRanges.size())) - 1;
        return tierRanges.get(index);
    }

    private static String defaultGroup(MachineStat stat, ModifierOperation operation) {
        return operation.getSerializedName() + ":" + stat.getSerializedName();
    }

    private static int maxTier(List<ModifierValueRange> tierRanges, List<ModifierEffectDefinition> effects) {
        return effects.stream()
                .mapToInt(effect -> effect.tierRanges().size())
                .max()
                .orElse(tierRanges.size());
    }

    private static List<ModifierTierWeight> normalizeTierWeights(
            List<ModifierTierWeight> tierWeights,
            int maxTier
    ) {
        if (maxTier <= 0) {
            return List.of();
        }
        if (tierWeights == null || tierWeights.isEmpty()) {
            return defaultTierWeights(maxTier);
        }
        return List.copyOf(tierWeights);
    }

    private static List<ModifierTierWeight> defaultTierWeights(int maxTier) {
        return java.util.stream.IntStream.rangeClosed(1, maxTier)
                .mapToObj(tier -> new ModifierTierWeight(tier, DEFAULT_TIER_WEIGHT))
                .toList();
    }

    private static boolean isLegacyVariantId(String affixId, String definitionId) {
        for (String suffix : List.of("_tuned", "_reinforced", "_harmonic", "_focused", "_amplified")) {
            if (affixId.equals(definitionId + suffix)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLegacyFlatEnergyCapacityModifier(MachineModifier modifier) {
        return stat == MachineStat.ENERGY_CAPACITY_FLAT
                && operation == ModifierOperation.ADD
                && modifier.stat() == MachineStat.ENERGY_CAPACITY
                && modifier.operation() == ModifierOperation.ADD;
    }

    public record ModifierTierWeight(int tier, int weight) {
        public ModifierTierWeight {
            tier = Math.max(1, tier);
            weight = Math.max(0, weight);
        }
    }
}
