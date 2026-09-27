package com.rngtech.rpg;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class MachineTraitRoller {
    private static final RollTuning DEFAULT_ROLL_TUNING = new RollTuning(0, 0);
    private static final RollTuning MODULAR_TOOL_ROLL_TUNING = new RollTuning(2, 5);
    private static final int MAX_STANDARD_RANDOM_TIER = 6;
    private static final int MAX_EXOTIC_RANDOM_TIER = 7;
    private static final int MAX_UPGRADE_TIER = 6;

    public static MachineTraits roll(MachineType type, RandomSource random) {
        return roll(ModifierEligibilityProfiles.forMachine(type), random);
    }

    public static MachineTraits roll(ModifierEligibilityProfile profile, RandomSource random) {
        return roll(profile, 0, random);
    }

    public static MachineTraits roll(ModifierEligibilityProfile profile, int componentStage, RandomSource random) {
        RollTuning tuning = tuningFor(profile);
        Rarity rarity = rollRarity(componentStage, random);
        List<MachineModifier> modifiers = new ArrayList<>();
        List<MachineBehavior> behaviors = new ArrayList<>();
        rollTraits(profile, rarity, componentStage, tuning, random, modifiers, behaviors);
        return new MachineTraits(
                rarity,
                rollRefinementPotential(componentStage, tuning, random),
                modifiers,
                behaviors
        );
    }

    private static Rarity rollRarity(int componentStage, RandomSource random) {
        int roll = random.nextInt(100);
        if (componentStage <= 0) {
            if (roll < 35) {
                return Rarity.NORMAL;
            }
            if (roll < 85) {
                return Rarity.MAGIC;
            }
            return Rarity.RARE;
        }
        if (componentStage <= 4) {
            if (roll < 70) {
                return Rarity.MAGIC;
            }
            return Rarity.RARE;
        }
        if (roll < 50) {
            return Rarity.NORMAL;
        }
        if (roll < 90) {
            return Rarity.MAGIC;
        }
        return Rarity.RARE;
    }

    private static int rollRefinementPotential(int componentStage, RollTuning tuning, RandomSource random) {
        return refinementPotentialRange(componentStage, tuning).roll(random);
    }

    public static RefinementPotentialRange refinementPotentialRange(int componentStage) {
        return refinementPotentialRange(componentStage, DEFAULT_ROLL_TUNING);
    }

    public static RefinementPotentialRange refinementPotentialRange(ModifierEligibilityProfile profile, int componentStage) {
        return refinementPotentialRange(componentStage, tuningFor(profile));
    }

    private static RefinementPotentialRange refinementPotentialRange(int componentStage, RollTuning tuning) {
        if (componentStage <= 0) {
            return new RefinementPotentialRange(1, 3).offset(tuning.refinementPotentialBonus());
        }
        if (componentStage <= 2) {
            return new RefinementPotentialRange(6, 10).offset(tuning.refinementPotentialBonus());
        }
        if (componentStage <= 4) {
            return new RefinementPotentialRange(8, 14).offset(tuning.refinementPotentialBonus());
        }
        if (componentStage <= 6) {
            return new RefinementPotentialRange(14, 22).offset(tuning.refinementPotentialBonus());
        }
        return new RefinementPotentialRange(22, 32).offset(tuning.refinementPotentialBonus());
    }

    private static void rollTraits(
            ModifierEligibilityProfile profile,
            Rarity rarity,
            int componentStage,
            RollTuning tuning,
            RandomSource random,
            List<MachineModifier> modifiers,
            List<MachineBehavior> behaviors
    ) {
        int prefixCount = switch (rarity) {
            case NORMAL, UNIQUE -> 0;
            case MAGIC -> 1;
            case RARE -> 3;
        };
        int suffixCount = switch (rarity) {
            case NORMAL, UNIQUE -> 0;
            case MAGIC -> 1;
            case RARE -> 3;
        };
        if (prefixCount == 0 && suffixCount == 0) {
            return;
        }

        rollSlotModifiers(profile, ModifierSlot.PREFIX, prefixCount, componentStage, tuning, random, modifiers);
        rollSuffixTraits(profile, suffixCount, componentStage, tuning, random, modifiers, behaviors);
    }

    private static void rollSlotModifiers(
            ModifierEligibilityProfile profile,
            ModifierSlot slot,
            int targetCount,
            int componentStage,
            RollTuning tuning,
            RandomSource random,
            List<MachineModifier> modifiers
    ) {
        for (int index = 0; index < targetCount; index++) {
            List<ModifierDefinition> pool = availableDefinitions(profile, slot, occupiedGroups(modifiers, List.of()));
            if (pool.isEmpty()) {
                return;
            }
            ModifierDefinition definition = weightedDefinition(pool, random);
            int tier = rollModifierTier(definition, componentStage, tuning, Integer.MAX_VALUE, random);
            modifiers.add(definition.roll(tier, random));
        }
    }

    private static void rollSuffixTraits(
            ModifierEligibilityProfile profile,
            int targetCount,
            int componentStage,
            RollTuning tuning,
            RandomSource random,
            List<MachineModifier> modifiers,
            List<MachineBehavior> behaviors
    ) {
        for (int index = 0; index < targetCount; index++) {
            Set<String> occupiedGroups = occupiedGroups(modifiers, behaviors);
            List<ModifierDefinition> modifierPool = availableDefinitions(profile, ModifierSlot.SUFFIX, occupiedGroups);
            List<MachineBehavior> behaviorPool = availableBehaviors(profile, occupiedGroups);
            if (modifierPool.isEmpty() && behaviorPool.isEmpty()) {
                return;
            }
            WeightedSuffixSelection selection = weightedSuffixSelection(modifierPool, behaviorPool, random);
            if (selection == null) {
                return;
            }
            if (selection.definition() != null) {
                ModifierDefinition definition = selection.definition();
                int tier = rollModifierTier(definition, componentStage, tuning, Integer.MAX_VALUE, random);
                modifiers.add(definition.roll(tier, random));
                continue;
            }
            behaviors.add(selection.behavior());
        }
    }

    private static List<ModifierDefinition> availableDefinitions(
            ModifierEligibilityProfile profile,
            ModifierSlot slot,
            Set<String> occupiedGroups
    ) {
        return profile.rollableDefinitions(slot).stream()
                .filter(definition -> !occupiedGroups.contains(definition.modGroup()))
                .toList();
    }

    private static List<MachineBehavior> availableBehaviors(
            ModifierEligibilityProfile profile,
            Set<String> occupiedGroups
    ) {
        return profile.rollableBehaviors().stream()
                .filter(behavior -> !occupiedGroups.contains(ModifierEligibilityProfiles.behaviorModGroup(behavior)))
                .toList();
    }

    private static Set<String> occupiedGroups(List<MachineModifier> modifiers, List<MachineBehavior> behaviors) {
        LinkedHashSet<String> groups = new LinkedHashSet<>();
        modifiers.stream()
                .map(MachineModifier::modGroup)
                .filter(group -> !group.isBlank())
                .forEach(groups::add);
        behaviors.stream()
                .map(ModifierEligibilityProfiles::behaviorModGroup)
                .filter(group -> !group.isBlank())
                .forEach(groups::add);
        return groups;
    }

    public static List<MachineStat> statsFor(MachineType type) {
        return ModifierEligibilityProfiles.forMachine(type).definitions().stream()
                .filter(ModifierDefinition::canRoll)
                .map(ModifierDefinition::stat)
                .distinct()
                .toList();
    }

    public static ModifierSlot legalSlotFor(MachineStat stat) {
        return switch (stat) {
            case ENERGY_CAPACITY,
                    ENERGY_CAPACITY_FLAT,
                    DURABILITY,
                    SELF_REPAIR,
                    BATTERY_SUPPORT,
                    LUCK,
                    TREE_FELL_LIMIT,
                    VEIN_MINE_LIMIT,
                    BLOCK_FILTER_SLOTS,
                    CONTROL,
                    HEAT_TRANSFER,
                    HEAT_ISOLATION,
                    WARMUP_TIME,
                    COOLING_RATE,
                    BATTERY_SLOTS,
                    EFFICIENCY,
                    FUEL_EFFICIENCY,
                    POTATO_POWER,
                    CARROT_POWER,
                    BREAD_POWER,
                    SAPLING_POWER,
                    SEED_POWER -> ModifierSlot.PREFIX;
            case INPUT_SLOTS,
                    OUTPUT_SLOTS,
                    ADDON_SLOTS,
                    OUTPUT_AMOUNT,
                    SUPER_OUTPUT_CHANCE,
                    PROCESSING_SPEED,
                    INSTANT_PROCESS_CHANCE,
                    MINING_SPEED,
                    ENERGY_USAGE,
                    FE_USAGE,
                    FE_TRANSFER,
                    VEIN_MINE_FE_USAGE,
                    ORE_BURST_SPEED,
                    ORE_BURST_DURATION,
                    ORE_BURST_FE_USAGE,
                    ORE_BURST_COOLDOWN,
                    ENERGY_GENERATION,
                    ENERGY_TRANSFER,
                    PEAK_SOLAR_GENERATION,
                    FLUID_TRANSFER,
                    TEMPERATURE_STABILITY,
                    OVERHEAT_TOLERANCE,
                    STABILITY -> ModifierSlot.SUFFIX;
            default -> ModifierSlot.SUFFIX;
        };
    }

    public static ModifierOperation operationFor(MachineStat stat) {
        if (stat == MachineStat.ENERGY_CAPACITY_FLAT
                || stat == MachineStat.INPUT_SLOTS
                || stat == MachineStat.BATTERY_SLOTS
                || stat == MachineStat.SELF_REPAIR) {
            return ModifierOperation.ADD;
        }
        if (stat == MachineStat.BLOCK_FILTER_SLOTS) {
            return ModifierOperation.ADD;
        }
        return stat == MachineStat.ENERGY_USAGE
                || stat == MachineStat.FE_USAGE
                || stat == MachineStat.ORE_BURST_FE_USAGE
                || stat == MachineStat.IDLE_LOSS
                || stat == MachineStat.WARMUP_TIME
                || stat == MachineStat.COOLING_RATE
                ? ModifierOperation.DECREASED_PERCENT
                : ModifierOperation.INCREASED_PERCENT;
    }

    public static ModifierOperation operationFor(
            MachineType type,
            MachineStat stat,
            List<MachineModifier> existingModifiers,
            RandomSource random
    ) {
        List<ModifierOperation> profileOperations = ModifierEligibilityProfiles.forMachine(type).definitions().stream()
                .filter(ModifierDefinition::canRoll)
                .filter(definition -> definition.stat() == stat)
                .map(ModifierDefinition::operation)
                .distinct()
                .toList();
        if (profileOperations.isEmpty()) {
            return operationFor(stat);
        }
        if (profileOperations.size() == 1) {
            return profileOperations.get(0);
        }

        List<ModifierOperation> missingOperations = profileOperations.stream()
                .filter(operation -> !hasAffixOperation(existingModifiers, stat, operation))
                .toList();
        if (!missingOperations.isEmpty()) {
            return missingOperations.get(random.nextInt(missingOperations.size()));
        }
        return profileOperations.get(random.nextInt(profileOperations.size()));
    }

    public static int rollModifierTier(Rarity rarity, RandomSource random) {
        return rollModifierTier(0, DEFAULT_ROLL_TUNING, MAX_STANDARD_RANDOM_TIER, random);
    }

    public static int rollModifierTier(Rarity rarity, int componentStage, RandomSource random) {
        return rollModifierTier(componentStage, DEFAULT_ROLL_TUNING, maxRandomTier(componentStage), random);
    }

    public static int rollModifierTier(
            ModifierEligibilityProfile profile,
            Rarity rarity,
            int componentStage,
            RandomSource random
    ) {
        return rollModifierTier(componentStage, tuningFor(profile), maxRandomTier(componentStage), random);
    }

    public static int rollModifierTier(
            ModifierDefinition definition,
            ModifierEligibilityProfile profile,
            Rarity rarity,
            int componentStage,
            RandomSource random
    ) {
        return rollModifierTier(definition, componentStage, tuningFor(profile), Integer.MAX_VALUE, random);
    }

    public static int rollModifierTier(
            ModifierDefinition definition,
            ModifierEligibilityProfile profile,
            Rarity rarity,
            int componentStage,
            int maxAffordableTier,
            RandomSource random
    ) {
        return rollModifierTier(definition, componentStage, tuningFor(profile), maxAffordableTier, random);
    }

    public static ModifierDefinition weightedDefinition(List<ModifierDefinition> definitions, RandomSource random) {
        if (definitions.isEmpty()) {
            throw new IllegalArgumentException("Cannot select from an empty modifier definition pool.");
        }
        int totalWeight = definitions.stream().mapToInt(ModifierDefinition::rollWeight).sum();
        if (totalWeight <= 0) {
            return definitions.get(random.nextInt(definitions.size()));
        }
        int roll = random.nextInt(totalWeight);
        int cursor = 0;
        for (ModifierDefinition definition : definitions) {
            cursor += definition.rollWeight();
            if (roll < cursor) {
                return definition;
            }
        }
        return definitions.getLast();
    }

    private static int rollModifierTier(
            int componentStage,
            RollTuning tuning,
            int maxTier,
            RandomSource random
    ) {
        List<WeightedTier> weightedTiers = new ArrayList<>();
        for (int tier = 1; tier <= maxTier; tier++) {
            int globalWeight = globalTierWeight(tier);
            if (globalWeight <= 0) {
                continue;
            }
            int stageWeight = stageTierWeight(tier, componentStage, tuning);
            if (stageWeight <= 0) {
                continue;
            }
            weightedTiers.add(new WeightedTier(tier, Math.max(1, globalWeight * stageWeight)));
        }
        return selectWeightedTier(weightedTiers, random);
    }

    private static int overflowTierChance(int overflow) {
        if (overflow <= 1) {
            return 50;
        }
        if (overflow == 2) {
            return 15;
        }
        return 5;
    }

    private static int overflowTierChance(int overflow, RollTuning tuning) {
        return Math.min(100, overflowTierChance(overflow) + tuning.overflowTierChanceBonus());
    }

    public static int naturalMaxTier(int componentStage) {
        if (componentStage <= 0) {
            return 1;
        }
        if (componentStage <= 2) {
            return 2;
        }
        if (componentStage <= 6) {
            return 3;
        }
        return 4;
    }

    public static int maxRandomTier(int componentStage) {
        return componentStage >= 8 ? MAX_EXOTIC_RANDOM_TIER : MAX_STANDARD_RANDOM_TIER;
    }

    public static int maxUpgradeTier() {
        return MAX_UPGRADE_TIER;
    }

    public static ModifierValueRange rangeFor(MachineStat stat, int tier) {
        return ModifierEligibilityProfiles.percentRangeForTier(tier);
    }

    public static ModifierValueRange rangeFor(MachineType type, MachineStat stat, ModifierOperation operation, int tier) {
        if ((stat == MachineStat.ENERGY_CAPACITY || stat == MachineStat.ENERGY_CAPACITY_FLAT)
                && operation == ModifierOperation.ADD) {
            return type == MachineType.BATTERY_CELL
                    ? ModifierEligibilityProfiles.batteryCellEnergyCapacityAddRangeForTier(tier)
                    : ModifierEligibilityProfiles.machineEnergyCapacityAddRangeForTier(tier);
        }
        if (stat == MachineStat.INPUT_SLOTS
                && operation == ModifierOperation.ADD) {
            return ModifierEligibilityProfiles.inputSlotsAddRangeForTier(tier);
        }
        if (stat == MachineStat.BATTERY_SLOTS
                && operation == ModifierOperation.ADD) {
            return ModifierEligibilityProfiles.batteryChassisBatterySlotsAddRangeForTier(tier);
        }
        if (stat == MachineStat.BLOCK_FILTER_SLOTS
                && operation == ModifierOperation.ADD) {
            return ModifierEligibilityProfiles.minersCompanionFilterSlotsAddRangeForTier(tier);
        }
        return rangeFor(stat, tier);
    }

    private static boolean hasAffixOperation(
            List<MachineModifier> modifiers,
            MachineStat stat,
            ModifierOperation operation
    ) {
        return modifiers.stream()
                .filter(modifier -> modifier.slot().isAffix())
                .anyMatch(modifier -> modifier.stat() == stat && modifier.operation() == operation);
    }

    private static int randomBetween(RandomSource random, int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    private static int rollModifierTier(
            ModifierDefinition definition,
            int componentStage,
            RollTuning tuning,
            int maxAffordableTier,
            RandomSource random
    ) {
        if (!definition.isTiered()) {
            return 1;
        }

        int maxRandomTier = Math.min(maxRandomTier(componentStage), Math.max(1, definition.maxTier()));
        int maxTier = Math.min(maxRandomTier, Math.max(1, maxAffordableTier));
        List<WeightedTier> weightedTiers = new ArrayList<>();
        for (int tier = 1; tier <= maxTier; tier++) {
            int globalWeight = globalTierWeight(tier);
            if (globalWeight <= 0) {
                continue;
            }
            int tierWeight = definition.tierWeight(tier);
            if (tierWeight <= 0) {
                continue;
            }
            int stageWeight = stageTierWeight(tier, componentStage, tuning);
            if (stageWeight <= 0) {
                continue;
            }
            weightedTiers.add(new WeightedTier(tier, Math.max(1, tierWeight * globalWeight * stageWeight)));
        }

        return selectWeightedTier(weightedTiers, random);
    }

    private static int selectWeightedTier(List<WeightedTier> weightedTiers, RandomSource random) {
        if (weightedTiers.isEmpty()) {
            return 1;
        }

        int totalWeight = weightedTiers.stream().mapToInt(WeightedTier::weight).sum();
        int roll = random.nextInt(totalWeight);
        int cursor = 0;
        for (WeightedTier tier : weightedTiers) {
            cursor += tier.weight();
            if (roll < cursor) {
                return tier.tier();
            }
        }
        return weightedTiers.getLast().tier();
    }

    private static int globalTierWeight(int tier) {
        return switch (tier) {
            case 1 -> 450;
            case 2 -> 270;
            case 3 -> 150;
            case 4 -> 80;
            case 5 -> 35;
            case 6 -> 12;
            case 7 -> 3;
            default -> 0;
        };
    }

    private static int stageTierWeight(int tier, int componentStage, RollTuning tuning) {
        int overflow = tier - naturalMaxTier(componentStage);
        if (overflow <= 0) {
            return 100;
        }
        if (componentStage >= 3 && componentStage <= 4 && overflow == 1) {
            return 100;
        }
        return overflowTierChance(overflow, tuning);
    }

    private static WeightedSuffixSelection weightedSuffixSelection(
            List<ModifierDefinition> definitions,
            List<MachineBehavior> behaviors,
            RandomSource random
    ) {
        int totalWeight = definitions.stream().mapToInt(ModifierDefinition::rollWeight).sum()
                + behaviors.stream().mapToInt(ModifierEligibilityProfiles::behaviorRollWeight).sum();
        if (totalWeight <= 0) {
            return null;
        }
        int roll = random.nextInt(totalWeight);
        int cursor = 0;
        for (ModifierDefinition definition : definitions) {
            cursor += definition.rollWeight();
            if (roll < cursor) {
                return new WeightedSuffixSelection(definition, null);
            }
        }
        for (MachineBehavior behavior : behaviors) {
            cursor += ModifierEligibilityProfiles.behaviorRollWeight(behavior);
            if (roll < cursor) {
                return new WeightedSuffixSelection(null, behavior);
            }
        }
        return null;
    }

    private static RollTuning tuningFor(ModifierEligibilityProfile profile) {
        return switch (profile.id()) {
            case "tool_head", "pick_head", "tool_rod", "modular_tool" -> MODULAR_TOOL_ROLL_TUNING;
            default -> DEFAULT_ROLL_TUNING;
        };
    }

    public record RefinementPotentialRange(int min, int max) {
        public RefinementPotentialRange {
            if (max < min) {
                throw new IllegalArgumentException("Refinement Potential range max must be greater than or equal to min.");
            }
        }

        public RefinementPotentialRange offset(int amount) {
            return new RefinementPotentialRange(min + amount, max + amount);
        }

        public int roll(RandomSource random) {
            return randomBetween(random, min, max);
        }
    }

    private record RollTuning(
            int refinementPotentialBonus,
            int overflowTierChanceBonus
    ) {
        private RollTuning {
            if (overflowTierChanceBonus < 0 || overflowTierChanceBonus > 100) {
                throw new IllegalArgumentException("Overflow tier chance bonus must be between 0 and 100.");
            }
        }
    }

    private record WeightedTier(int tier, int weight) {
    }

    private record WeightedSuffixSelection(ModifierDefinition definition, MachineBehavior behavior) {
    }

    private MachineTraitRoller() {
    }
}
