package com.rngtech.rpg.refinement;

import com.rngtech.content.item.MachinePartItem;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineModifierEffect;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineTraitRoller;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.MachineType;
import com.rngtech.rpg.ModifierDefinition;
import com.rngtech.rpg.ModifierEffectDefinition;
import com.rngtech.rpg.ModifierEligibilityProfile;
import com.rngtech.rpg.ModifierEligibilityProfiles;
import com.rngtech.rpg.ModifierLensTag;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.ModifierValueRange;
import com.rngtech.rpg.Rarity;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class RefinementEngine {
    private static final int MAGIC_SLOT_LIMIT = 1;
    private static final int RARE_SLOT_LIMIT = 3;
    private static final int MIN_ADD_OR_UPGRADE_COST = 1;
    private static final int MAX_ADD_OR_UPGRADE_COST = 18;
    private static final PotentialCostRange DEFAULT_ADD_OR_UPGRADE_COST =
            new PotentialCostRange(MIN_ADD_OR_UPGRADE_COST, MAX_ADD_OR_UPGRADE_COST);
    private static final PotentialCostRange AFFIX_INJECTOR_COST = new PotentialCostRange(1, 8);
    private static final PotentialCostRange MODIFIER_LENS_COST = new PotentialCostRange(2, 8);
    private static final PotentialCostRange RANDOM_UPGRADE_COST = new PotentialCostRange(2, 6);
    private static final PotentialCostRange SELECTED_UPGRADE_COST = new PotentialCostRange(6, 10);
    private static final int LENS_WEIGHT_MULTIPLIER = 3;
    private static final int ASCENSION_CATALYST_POTENTIAL_COST = 4;
    private static final int ASCENSION_CATALYST_MINIMUM_POTENTIAL = ASCENSION_CATALYST_POTENTIAL_COST + 1;

    public static RefinementResult apply(
            MachineType machineType,
            MachineTraits traits,
            RefinementOperation operation,
            RandomSource random
    ) {
        return apply(ModifierEligibilityProfiles.forMachine(machineType), traits, operation, random);
    }

    public static RefinementResult apply(
            ModifierEligibilityProfile profile,
            MachineTraits traits,
            RefinementOperation operation,
            RandomSource random
    ) {
        return apply(profile, traits, operation, 0, random);
    }

    public static RefinementResult apply(
            ModifierEligibilityProfile profile,
            MachineTraits traits,
            RefinementOperation operation,
            int componentStage,
            RandomSource random
    ) {
        return apply(profile, traits, operation, componentStage, RefinementSelection.none(), random);
    }

    public static RefinementResult apply(
            ModifierEligibilityProfile profile,
            MachineTraits traits,
            RefinementOperation operation,
            int componentStage,
            RefinementSelection selection,
            RandomSource random
    ) {
        return apply(profile, traits, operation, componentStage, selection, RefinementModifier.NONE, random);
    }

    public static RefinementResult apply(
            ModifierEligibilityProfile profile,
            MachineTraits traits,
            RefinementOperation operation,
            int componentStage,
            RefinementSelection selection,
            RefinementModifier modifier,
            RandomSource random
    ) {
        return apply(profile, traits, operation, componentStage, selection, modifier, Set.of(), random);
    }

    public static RefinementResult apply(
            ModifierEligibilityProfile profile,
            MachineTraits traits,
            RefinementOperation operation,
            int componentStage,
            RefinementSelection selection,
            RefinementModifier modifier,
            Set<ModifierLensTag> lensTags,
            RandomSource random
    ) {
        if (traits.rarity() == Rarity.UNIQUE) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.unique");
        }
        lensTags = Set.copyOf(lensTags);
        if (!lensTags.isEmpty() && !operationSupportsLens(operation)) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.lens_requires_add_or_upgrade");
        }
        if (!lensTags.isEmpty() && modifier != RefinementModifier.NONE) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.focus_conflict");
        }
        if (modifier.requiresRandomUpgrade() && operation != RefinementOperation.UPGRADE_RANDOM_MODIFIER) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.modifier_requires_affix_modifier");
        }
        if (modifier != RefinementModifier.NONE && !modifier.requiresRandomUpgrade() && !canUseModifier(operation.action())) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.modifier_requires_add_or_upgrade");
        }
        if (modifier.requiresSelectedUpgrade() && operation != RefinementOperation.UPGRADE_SELECTED_MODIFIER) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.modifier_requires_upgrade");
        }
        return switch (operation.action()) {
            case RANDOM_ADD -> addModifier(
                    profile,
                    traits,
                    random,
                    componentStage,
                    List.of(),
                    selectedEmptySlot(selection),
                    modifier,
                    lensTags,
                    costRange(operation)
            );
            case RANDOM_UPGRADE -> upgradeWeightedModifier(
                    profile,
                    traits,
                    componentStage,
                    modifier,
                    lensTags,
                    false,
                    RANDOM_UPGRADE_COST,
                    random
            );
            case SELECTED_UPGRADE -> upgradeWeightedModifier(
                    profile,
                    traits,
                    componentStage,
                    modifier,
                    lensTags,
                    true,
                    SELECTED_UPGRADE_COST,
                    random
            );
            case ASCEND_RARITY -> ascendRarity(profile, traits, componentStage, random);
            case CATALYZE_ASCENSION -> catalyzeAscension(profile, traits, componentStage, random);
            case RANDOM_REMOVE -> removeModifier(profile, traits, random);
            case TARGETED_ADD_OR_UPGRADE -> targetedAddOrUpgrade(profile, traits, operation, componentStage, selection, modifier, random);
            case FULL_REROLL -> fullReroll(profile, traits, componentStage, random);
            case FILL_OPEN_SLOTS -> fillOpenSlots(profile, traits, componentStage, random);
        };
    }

    public static int rollPotentialCost(RandomSource random) {
        int roll = random.nextInt(100);
        if (roll < 50) {
            return 1;
        }
        if (roll < 80) {
            return 2;
        }
        if (roll < 95) {
            return 3;
        }
        return 4;
    }

    public static int minAddOrUpgradePotentialCost() {
        return DEFAULT_ADD_OR_UPGRADE_COST.min();
    }

    public static int maxAddOrUpgradePotentialCost() {
        return DEFAULT_ADD_OR_UPGRADE_COST.max();
    }

    public static int minPotentialCost(RefinementOperation operation) {
        return costRange(operation).min();
    }

    public static int maxPotentialCost(RefinementOperation operation) {
        return costRange(operation).max();
    }

    public static int ascensionCatalystPotentialCost() {
        return ASCENSION_CATALYST_POTENTIAL_COST;
    }

    public static int ascensionCatalystMinimumPotential() {
        return ASCENSION_CATALYST_MINIMUM_POTENTIAL;
    }

    public static int minimumPotentialCostForTier(int tier) {
        return minPotentialCostForTier(tier, DEFAULT_ADD_OR_UPGRADE_COST);
    }

    public static boolean hasOpenAffixSlot(MachineTraits traits, ModifierSlot slot) {
        return hasOpenAffixSlot(traits, rarityAfterAdd(traits.rarity()), slot);
    }

    public static boolean hasAnyOpenAffixSlot(MachineTraits traits) {
        return hasAnyOpenAffixSlot(traits, rarityAfterAdd(traits.rarity()));
    }

    public static String noAddableAffixFailureKey(MachineTraits traits, boolean targeted) {
        return noAddableAffixFailureKey(traits, rarityAfterAdd(traits.rarity()), targeted);
    }

    private static PotentialCostRange costRange(RefinementOperation operation) {
        if (operation == null) {
            return DEFAULT_ADD_OR_UPGRADE_COST;
        }
        return switch (operation) {
            case ADD_MODIFIER -> AFFIX_INJECTOR_COST;
            case UPGRADE_RANDOM_MODIFIER -> RANDOM_UPGRADE_COST;
            case UPGRADE_SELECTED_MODIFIER -> SELECTED_UPGRADE_COST;
            case KINETIC_LENS, EFFICIENCY_LENS -> MODIFIER_LENS_COST;
            default -> DEFAULT_ADD_OR_UPGRADE_COST;
        };
    }

    public static RefinementResult reforgeTarget(ItemStack target, RandomSource random) {
        if (!RefinementTargets.canRefine(target)) {
            return RefinementResult.failure(MachineTraits.EMPTY, "rngtech.refinement.failure.invalid_target");
        }

        MachineTraits traits = RefinementTargets.storedTraits(target);
        if (traits.rarity() == Rarity.UNIQUE) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.unique");
        }

        MachineTraits rolled = MachineTraitRoller.roll(
                RefinementTargets.eligibilityProfile(target),
                RefinementTargets.modifierRollComponentStage(target),
                random
        );
        if (target.getItem() instanceof MachinePartItem part) {
            rolled = new MachineTraits(
                    rolled.rarity(),
                    part.modifierSet().refinementPotential() + rolled.refinementPotential(),
                    rolled.modifiers(),
                    rolled.behaviors()
            );
        }
        return RefinementResult.success(rolled, 0, "rngtech.refinement.success.reforge", false, false);
    }

    public static RefinementResult refineAllSameTier(
            ModifierEligibilityProfile profile,
            MachineTraits traits,
            int refinementPotentialCost,
            RandomSource random
    ) {
        if (traits.rarity() == Rarity.UNIQUE) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.unique");
        }
        if (traits.refinementPotential() < refinementPotentialCost) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.no_potential");
        }

        List<MachineModifier> modifiers = new ArrayList<>();
        int rerolled = 0;
        for (MachineModifier modifier : traits.modifiers()) {
            ModifierDefinition definition = modifier.slot().isAffix() ? definitionFor(profile, modifier) : null;
            if (definition == null) {
                modifiers.add(modifier);
                continue;
            }
            modifiers.add(definition.roll(Math.max(1, modifier.tier()), random));
            rerolled++;
        }
        if (rerolled == 0) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.no_matching_modifier");
        }
        return RefinementResult.success(
                withModifiers(traits, traits.rarity(), traits.refinementPotential() - refinementPotentialCost, modifiers),
                refinementPotentialCost,
                "rngtech.refinement.success.refine_all",
                false,
                false
        );
    }

    public static RefinementResult refineSelectedSameTier(
            ModifierEligibilityProfile profile,
            MachineTraits traits,
            RefinementSelection selection,
            int refinementPotentialCost,
            RandomSource random
    ) {
        if (traits.rarity() == Rarity.UNIQUE) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.unique");
        }
        if (selection.kind() != RefinementSelection.Kind.EXISTING_MODIFIER) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.select_modifier_or_slot");
        }
        if (traits.refinementPotential() < refinementPotentialCost) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.no_potential");
        }

        int modifierIndex = selectedAffixModifierIndex(traits, selection.affixIndex());
        if (modifierIndex < 0) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.no_matching_modifier");
        }
        MachineModifier selected = traits.modifiers().get(modifierIndex);
        ModifierDefinition definition = definitionFor(profile, selected);
        if (definition == null) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.no_matching_modifier");
        }

        List<MachineModifier> modifiers = new ArrayList<>(traits.modifiers());
        modifiers.set(modifierIndex, definition.roll(Math.max(1, selected.tier()), random));
        return RefinementResult.success(
                withModifiers(traits, traits.rarity(), traits.refinementPotential() - refinementPotentialCost, modifiers),
                refinementPotentialCost,
                "rngtech.refinement.success.refine_modifier",
                false,
                false
        );
    }

    public static RefinementResult upgradeRandomModifier(
            ModifierEligibilityProfile profile,
            MachineTraits traits,
            int componentStage,
            RandomSource random
    ) {
        return upgradeWeightedModifier(
                profile,
                traits,
                componentStage,
                RefinementModifier.NONE,
                Set.of(),
                false,
                RANDOM_UPGRADE_COST,
                random
        );
    }

    private static RefinementResult upgradeWeightedModifier(
            ModifierEligibilityProfile profile,
            MachineTraits traits,
            int componentStage,
            RefinementModifier refinementModifier,
            Set<ModifierLensTag> lensTags,
            boolean allowTuning,
            PotentialCostRange costRange,
            RandomSource random
    ) {
        if (traits.rarity() == Rarity.UNIQUE) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.unique");
        }
        List<MachineModifier> upgradeable = upgradeableAffixes(profile, traits, true, false, costRange);
        if (allowTuning && upgradeable.isEmpty()) {
            upgradeable = upgradeableAffixes(profile, traits, true, true, costRange);
        }
        if (upgradeable.isEmpty()) {
            return RefinementResult.failure(
                    traits,
                    hasUpgradeableAffix(profile, traits, false, allowTuning, costRange)
                            ? "rngtech.refinement.failure.no_potential"
                            : "rngtech.refinement.failure.no_matching_modifier"
            );
        }
        if (!lensTags.isEmpty() && upgradeable.stream().noneMatch(modifier -> matchesLensTags(profile, modifier, lensTags))) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.no_matching_lens_affix");
        }
        return upgradeModifier(
                profile,
                traits,
                weightedModifier(profile, upgradeable, lensTags, random),
                refinementModifier,
                componentStage,
                random,
                costRange,
                !lensTags.isEmpty() || consumeModifier(refinementModifier)
        );
    }

    public static RefinementResult upgradeSelectedModifier(
            ModifierEligibilityProfile profile,
            MachineTraits traits,
            int componentStage,
            RefinementSelection selection,
            RefinementModifier refinementModifier,
            RandomSource random
    ) {
        if (refinementModifier.requiresRandomUpgrade()) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.modifier_requires_affix_modifier");
        }
        if (selection.kind() != RefinementSelection.Kind.EXISTING_MODIFIER) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.select_modifier_or_slot");
        }
        MachineModifier selected = selectedAffix(traits, selection.affixIndex());
        if (selected == null) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.no_matching_modifier");
        }
        return upgradeModifier(
                profile,
                traits,
                selected,
                refinementModifier,
                componentStage,
                random,
                SELECTED_UPGRADE_COST,
                consumeModifier(refinementModifier)
        );
    }

    public static RefinementResult addSelectedModifier(
            ModifierEligibilityProfile profile,
            MachineTraits traits,
            int componentStage,
            RefinementSelection selection,
            RandomSource random
    ) {
        if (selection.kind() != RefinementSelection.Kind.EMPTY_SLOT) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.select_empty_slot");
        }
        return addModifier(
                profile,
                traits,
                random,
                componentStage,
                List.of(),
                selection.emptySlot(),
                RefinementModifier.NONE,
                Set.of(),
                AFFIX_INJECTOR_COST
        );
    }

    public static RefinementResult removeSelectedModifier(
            ModifierEligibilityProfile profile,
            MachineTraits traits,
            RefinementSelection selection,
            int refinementPotentialCost
    ) {
        if (traits.rarity() == Rarity.UNIQUE) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.unique");
        }
        if (selection.kind() != RefinementSelection.Kind.EXISTING_MODIFIER) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.select_modifier_or_slot");
        }
        if (traits.refinementPotential() < refinementPotentialCost) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.no_potential");
        }

        int modifierIndex = selectedAffixModifierIndex(traits, selection.affixIndex());
        if (modifierIndex < 0) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.no_matching_modifier");
        }
        MachineModifier selected = traits.modifiers().get(modifierIndex);
        if (isFixedDefinition(profile, selected)) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.no_removable_modifier");
        }
        List<MachineModifier> modifiers = new ArrayList<>(traits.modifiers());
        modifiers.remove(modifierIndex);
        return RefinementResult.success(
                withModifiers(traits, traits.rarity(), traits.refinementPotential() - refinementPotentialCost, modifiers),
                refinementPotentialCost,
                "rngtech.refinement.success.remove_modifier",
                false,
                false
        );
    }

    public static boolean hasRetunableAffix(ModifierEligibilityProfile profile, MachineTraits traits) {
        return traits.modifiers().stream()
                .filter(modifier -> modifier.slot().isAffix())
                .anyMatch(modifier -> definitionFor(profile, modifier) != null);
    }

    public static boolean hasSelectedRetunableAffix(
            ModifierEligibilityProfile profile,
            MachineTraits traits,
            RefinementSelection selection
    ) {
        if (selection.kind() != RefinementSelection.Kind.EXISTING_MODIFIER) {
            return false;
        }
        MachineModifier selected = selectedAffix(traits, selection.affixIndex());
        return selected != null && definitionFor(profile, selected) != null;
    }

    public static boolean hasSelectedRemovableAffix(
            ModifierEligibilityProfile profile,
            MachineTraits traits,
            RefinementSelection selection
    ) {
        if (selection.kind() != RefinementSelection.Kind.EXISTING_MODIFIER) {
            return false;
        }
        MachineModifier selected = selectedAffix(traits, selection.affixIndex());
        return selected != null && selected.slot().isAffix() && !isFixedDefinition(profile, selected);
    }

    public static boolean hasUpgradeableAffix(
            ModifierEligibilityProfile profile,
            MachineTraits traits,
            boolean requireAvailablePotential
    ) {
        return hasUpgradeableAffix(profile, traits, requireAvailablePotential, false, RANDOM_UPGRADE_COST);
    }

    private static boolean hasUpgradeableAffix(
            ModifierEligibilityProfile profile,
            MachineTraits traits,
            boolean requireAvailablePotential,
            boolean allowTuning,
            PotentialCostRange costRange
    ) {
        return !upgradeableAffixes(profile, traits, requireAvailablePotential, allowTuning, costRange).isEmpty();
    }

    public static boolean hasOpenSelectedAffixSlot(
            ModifierEligibilityProfile profile,
            MachineTraits traits,
            ModifierSlot slot,
            int componentStage
    ) {
        if (traits.refinementPotential() <= 0 || !slot.isAffix()) {
            return false;
        }
        Rarity rarity = rarityAfterAdd(traits.rarity());
        return availableSlots(
                profile,
                traits,
                rarity,
                List.of(),
                componentStage,
                traits.refinementPotential(),
                AFFIX_INJECTOR_COST)
                .contains(slot);
    }

    private static int rollAddOrUpgradePotentialCost(int tier, PotentialCostRange costRange, RandomSource random) {
        int min = minPotentialCostForTier(tier, costRange);
        return min + random.nextInt(costRange.max() - min + 1);
    }

    private static int rollSpendableAddOrUpgradePotentialCost(
            int availablePotential,
            int tier,
            PotentialCostRange costRange,
            RandomSource random
    ) {
        return Math.min(rollAddOrUpgradePotentialCost(tier, costRange, random), availablePotential);
    }

    private static int minPotentialCostForTier(int tier, PotentialCostRange costRange) {
        int cappedTier = Math.max(1, tier);
        int tierMinimum = MIN_ADD_OR_UPGRADE_COST + (cappedTier - 1) * 3;
        return Math.min(costRange.max(), Math.max(costRange.min(), tierMinimum));
    }

    public static boolean canUseModifier(RefinementAction action) {
        return action == RefinementAction.RANDOM_ADD
                || action == RefinementAction.TARGETED_ADD_OR_UPGRADE
                || action == RefinementAction.RANDOM_UPGRADE
                || action == RefinementAction.SELECTED_UPGRADE;
    }

    public static boolean operationSupportsLens(RefinementOperation operation) {
        return operation == RefinementOperation.ADD_MODIFIER
                || operation == RefinementOperation.UPGRADE_RANDOM_MODIFIER
                || operation == RefinementOperation.UPGRADE_SELECTED_MODIFIER;
    }

    private static RefinementResult addModifier(
            ModifierEligibilityProfile profile,
            MachineTraits traits,
            RandomSource random,
            int componentStage,
            List<MachineStat> targetStats,
            ModifierSlot selectedSlot,
            RefinementModifier refinementModifier,
            Set<ModifierLensTag> lensTags,
            PotentialCostRange costRange
    ) {
        if (traits.refinementPotential() <= 0) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.no_potential");
        }

        Rarity rarity = rarityAfterAdd(traits.rarity());
        List<ModifierSlot> slots = availableSlots(
                profile,
                traits,
                rarity,
                targetStats,
                componentStage,
                traits.refinementPotential(),
                costRange
        );
        if (selectedSlot != null) {
            if (!slots.contains(selectedSlot)) {
                return RefinementResult.failure(
                        traits,
                        hasAnyOpenAffixSlot(traits, rarity)
                                ? "rngtech.refinement.failure.no_open_selected_slot"
                                : "rngtech.refinement.failure.max_affixes"
                );
            }
            slots = List.of(selectedSlot);
        }
        if (slots.isEmpty()) {
            return RefinementResult.failure(
                    traits,
                    noAddableAffixFailureKey(traits, rarity, !targetStats.isEmpty())
            );
        }

        List<ModifierRollCandidate> candidates = rollModifierCandidates(
                profile,
                rarity,
                componentStage,
                traits.refinementPotential(),
                traits.modifiers(),
                targetStats,
                slots,
                costRange
        );
        if (candidates.isEmpty()) {
            return RefinementResult.failure(traits, noAddableAffixFailureKey(traits, rarity, !targetStats.isEmpty()));
        }
        if (!lensTags.isEmpty() && candidates.stream().noneMatch(candidate -> candidate.definition().matchesLensTags(lensTags))) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.no_matching_lens_affix");
        }
        ModifierRollCandidate rolledCandidate = weightedCandidate(candidates, lensTags, random);
        ModifierDefinition definition = rolledCandidate.definition();
        int tier = MachineTraitRoller.rollModifierTier(
                definition,
                profile,
                rarity,
                componentStage,
                affordableRandomTierLimit(componentStage, traits.refinementPotential(), definition, costRange),
                random
        );
        MachineModifier rolledModifier = definition.roll(tier, random);

        int cost = rollSpendableAddOrUpgradePotentialCost(
                traits.refinementPotential(),
                rolledModifier.tier(),
                costRange,
                random
        );
        List<MachineModifier> modifiers = new ArrayList<>(traits.modifiers());
        modifiers.add(rolledModifier);
        return RefinementResult.success(
                withModifiers(traits, rarity, traits.refinementPotential() - cost, modifiers),
                cost,
                "rngtech.refinement.success.add_modifier",
                consumeCatalyst(refinementModifier, random),
                !lensTags.isEmpty() || consumeModifier(refinementModifier)
        );
    }

    private static RefinementResult targetedAddOrUpgrade(
            ModifierEligibilityProfile profile,
            MachineTraits traits,
            RefinementOperation operation,
            int componentStage,
            RefinementSelection selection,
            RefinementModifier refinementModifier,
            RandomSource random
    ) {
        if (traits.refinementPotential() <= 0) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.no_potential");
        }

        if (selection.kind() == RefinementSelection.Kind.EMPTY_SLOT) {
            return addModifier(
                    profile,
                    traits,
                    random,
                    componentStage,
                    operation.targetStats(),
                    selection.emptySlot(),
                    refinementModifier,
                    Set.of(),
                    costRange(operation)
            );
        }
        if (selection.kind() == RefinementSelection.Kind.EXISTING_MODIFIER) {
            MachineModifier selected = selectedAffix(traits, selection.affixIndex());
            if (selected == null) {
                return RefinementResult.failure(traits, "rngtech.refinement.failure.no_matching_modifier");
            }
            if (operation.targetStats().stream().noneMatch(selected::matchesTargetStat)) {
                return RefinementResult.failure(traits, RefinementOperation.wrongLensMessageKey(selected.stat()));
            }
            if (targetableDefinitionFor(profile, selected) == null) {
                return RefinementResult.failure(traits, "rngtech.refinement.failure.no_matching_modifier");
            }
            return upgradeModifier(
                    profile,
                    traits,
                    selected,
                    refinementModifier,
                    componentStage,
                    random,
                    costRange(operation),
                    consumeModifier(refinementModifier)
            );
        }

        List<MachineModifier> existingTargets = traits.modifiers().stream()
                .filter(modifier -> modifier.slot().isAffix())
                .filter(modifier -> operation.targetStats().stream().anyMatch(modifier::matchesTargetStat))
                .filter(modifier -> targetableDefinitionFor(profile, modifier) != null)
                .sorted(Comparator.comparingInt(MachineModifier::tier))
                .toList();
        if (!existingTargets.isEmpty()) {
            return upgradeModifier(
                    profile,
                    traits,
                    existingTargets.get(0),
                    refinementModifier,
                    componentStage,
                    random,
                    costRange(operation),
                    consumeModifier(refinementModifier)
            );
        }

        return addModifier(
                profile,
                traits,
                random,
                componentStage,
                operation.targetStats(),
                null,
                refinementModifier,
                Set.of(),
                costRange(operation)
        );
    }

    private static RefinementResult upgradeModifier(
            ModifierEligibilityProfile profile,
            MachineTraits traits,
            MachineModifier current,
            RefinementModifier refinementModifier,
            int componentStage,
            RandomSource random,
            PotentialCostRange costRange,
            boolean consumeFocus
    ) {
        ModifierDefinition definition = definitionFor(profile, current);
        if (definition == null) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.no_matching_modifier");
        }

        int currentTier = Math.max(1, current.tier());
        int maxTier = Math.max(1, definition.maxTier());
        int maxUpgradeTier = Math.min(maxTier, MachineTraitRoller.maxUpgradeTier());
        int targetTier = currentTier >= maxUpgradeTier
                ? Math.min(currentTier, maxTier)
                : Math.min(maxTier, currentTier + 1);
        if (traits.refinementPotential() < costRange.min()) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.no_potential");
        }
        int cost = rollSpendableAddOrUpgradePotentialCost(
                traits.refinementPotential(),
                targetTier,
                costRange,
                random
        );
        cost = adjustedPotentialCost(cost, refinementModifier, random);

        List<MachineModifier> modifiers = new ArrayList<>(traits.modifiers());
        int index = modifiers.indexOf(current);
        if (index < 0) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.no_matching_modifier");
        }

        MachineModifier upgraded = upgradedModifier(profile, modifiers, index, current, definition, targetTier, refinementModifier, random);
        if (upgraded == null) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.no_matching_modifier");
        }
        modifiers.set(index, upgraded);
        if (refinementModifier == RefinementModifier.DESTABILIZE_OTHERS) {
            modifiers = destabilizeOtherAffixes(profile, modifiers, index, random);
        }
        return RefinementResult.success(
                withModifiers(traits, traits.rarity(), traits.refinementPotential() - cost, modifiers),
                cost,
                currentTier >= maxUpgradeTier
                        ? "rngtech.refinement.success.tune_modifier"
                        : "rngtech.refinement.success.upgrade_modifier",
                consumeCatalyst(refinementModifier, random),
                consumeFocus
        );
    }

    private static RefinementResult ascendRarity(
            ModifierEligibilityProfile profile,
            MachineTraits traits,
            int componentStage,
            RandomSource random
    ) {
        if (traits.rarity() != Rarity.MAGIC) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.requires_magic");
        }
        if (traits.refinementPotential() <= 0) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.no_potential");
        }

        List<MachineModifier> modifiers = new ArrayList<>(traits.modifiers());
        if (!upgradeRandomExistingAffix(profile, modifiers, random)) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.no_matching_modifier");
        }

        int targetCount = rollPotentialCost(random);
        int added = 0;
        int ascensionTierStage = nonExoticRandomTierStage(componentStage);
        for (int index = 0; index < targetCount; index++) {
            MachineTraits preview = withModifiers(traits, Rarity.RARE, 0, modifiers);
            List<ModifierSlot> slots = availableSlots(
                    profile,
                    preview,
                    Rarity.RARE,
                    List.of(),
                    ascensionTierStage,
                    traits.refinementPotential(),
                    DEFAULT_ADD_OR_UPGRADE_COST
            );
            if (slots.isEmpty()) {
                break;
            }

            MachineModifier modifier = rollModifier(
                    profile,
                    slots.get(random.nextInt(slots.size())),
                    Rarity.RARE,
                    ascensionTierStage,
                    traits.refinementPotential(),
                    modifiers,
                    List.of(),
                    random,
                    DEFAULT_ADD_OR_UPGRADE_COST
            );
            if (modifier == null) {
                break;
            }

            modifiers.add(modifier);
            added++;
        }

        if (added == 0) {
            return RefinementResult.failure(traits, noAddableAffixFailureKey(traits, Rarity.RARE, false));
        }

        return RefinementResult.success(
                withModifiers(traits, Rarity.RARE, 0, modifiers),
                traits.refinementPotential(),
                "rngtech.refinement.success.ascend_rarity"
        );
    }

    private static RefinementResult catalyzeAscension(
            ModifierEligibilityProfile profile,
            MachineTraits traits,
            int componentStage,
            RandomSource random
    ) {
        if (traits.rarity() != Rarity.MAGIC) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.requires_magic");
        }
        if (traits.refinementPotential() < ASCENSION_CATALYST_MINIMUM_POTENTIAL) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.insufficient_ascension_potential");
        }

        List<MachineModifier> modifiers = new ArrayList<>(traits.modifiers());
        MachineTraits preview = withModifiers(traits, Rarity.RARE, traits.refinementPotential(), modifiers);
        int ascensionTierStage = nonExoticRandomTierStage(componentStage);
        List<ModifierSlot> slots = availableSlots(
                profile,
                preview,
                Rarity.RARE,
                List.of(),
                ascensionTierStage,
                ASCENSION_CATALYST_POTENTIAL_COST,
                DEFAULT_ADD_OR_UPGRADE_COST
        );
        if (slots.isEmpty()) {
            return RefinementResult.failure(traits, noAddableAffixFailureKey(traits, Rarity.RARE, false));
        }

        MachineModifier modifier = rollModifier(
                profile,
                slots.get(random.nextInt(slots.size())),
                Rarity.RARE,
                ascensionTierStage,
                ASCENSION_CATALYST_POTENTIAL_COST,
                modifiers,
                List.of(),
                random,
                DEFAULT_ADD_OR_UPGRADE_COST
        );
        if (modifier == null) {
            return RefinementResult.failure(traits, noAddableAffixFailureKey(traits, Rarity.RARE, false));
        }

        modifiers.add(modifier);
        return RefinementResult.success(
                withModifiers(
                        traits,
                        Rarity.RARE,
                        traits.refinementPotential() - ASCENSION_CATALYST_POTENTIAL_COST,
                        modifiers
                ),
                ASCENSION_CATALYST_POTENTIAL_COST,
                "rngtech.refinement.success.catalyze_ascension"
        );
    }

    private static boolean upgradeRandomExistingAffix(
            ModifierEligibilityProfile profile,
            List<MachineModifier> modifiers,
            RandomSource random
    ) {
        List<Integer> indexes = new ArrayList<>();
        for (int index = 0; index < modifiers.size(); index++) {
            MachineModifier modifier = modifiers.get(index);
            if (modifier.slot().isAffix() && definitionFor(profile, modifier) != null) {
                indexes.add(index);
            }
        }
        if (indexes.isEmpty()) {
            return false;
        }

        int selectedIndex = indexes.get(random.nextInt(indexes.size()));
        MachineModifier current = modifiers.get(selectedIndex);
        ModifierDefinition definition = definitionFor(profile, current);
        if (definition == null) {
            return false;
        }

        MachineModifier upgraded = upgradedModifier(
                profile,
                modifiers,
                selectedIndex,
                current,
                definition,
                ascensionUpgradeTargetTier(current, definition),
                RefinementModifier.NONE,
                random
        );
        if (upgraded == null) {
            return false;
        }
        modifiers.set(selectedIndex, upgraded);
        return true;
    }

    private static int ascensionUpgradeTargetTier(MachineModifier current, ModifierDefinition definition) {
        int currentTier = Math.max(1, current.tier());
        int maxTier = Math.max(1, definition.maxTier());
        int maxUpgradeTier = Math.min(maxTier, MachineTraitRoller.maxUpgradeTier());
        return currentTier >= maxUpgradeTier ? Math.min(currentTier, maxTier) : currentTier + 1;
    }

    private static RefinementResult removeModifier(ModifierEligibilityProfile profile, MachineTraits traits, RandomSource random) {
        if (traits.refinementPotential() <= 0) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.no_potential");
        }

        List<MachineModifier> removable = traits.modifiers().stream()
                .filter(modifier -> modifier.slot().isAffix())
                .filter(modifier -> !isFixedDefinition(profile, modifier))
                .toList();
        if (removable.isEmpty()) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.no_removable_modifier");
        }

        MachineModifier removed = removable.get(random.nextInt(removable.size()));
        List<MachineModifier> modifiers = new ArrayList<>(traits.modifiers());
        modifiers.remove(removed);
        int cost = Math.min(rollPotentialCost(random), traits.refinementPotential());
        return RefinementResult.success(
                withModifiers(traits, traits.rarity(), traits.refinementPotential() - cost, modifiers),
                cost,
                "rngtech.refinement.success.remove_modifier"
        );
    }

    private static RefinementResult fullReroll(
            ModifierEligibilityProfile profile,
            MachineTraits traits,
            int componentStage,
            RandomSource random
    ) {
        if (traits.refinementPotential() <= 0) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.no_potential");
        }

        int prefixCount = count(traits.modifiers(), ModifierSlot.PREFIX);
        int suffixCount = count(traits.modifiers(), ModifierSlot.SUFFIX);
        if (prefixCount == 0 && suffixCount == 0) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.no_removable_modifier");
        }

        List<MachineModifier> modifiers = new ArrayList<>(traits.modifiers().stream()
                .filter(modifier -> !modifier.slot().isAffix())
                .toList());
        rollSlotModifiers(
                profile,
                ModifierSlot.PREFIX,
                prefixCount,
                traits.rarity(),
                componentStage,
                Integer.MAX_VALUE,
                random,
                modifiers
        );
        rollSlotModifiers(
                profile,
                ModifierSlot.SUFFIX,
                suffixCount,
                traits.rarity(),
                componentStage,
                Integer.MAX_VALUE,
                random,
                modifiers
        );

        if (count(modifiers, ModifierSlot.PREFIX) + count(modifiers, ModifierSlot.SUFFIX) == 0) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.no_open_affix");
        }

        int cost = Math.min(rollPotentialCost(random), traits.refinementPotential());
        return RefinementResult.success(
                withModifiers(traits, traits.rarity(), traits.refinementPotential() - cost, modifiers),
                cost,
                "rngtech.refinement.success.full_reroll"
        );
    }

    private static RefinementResult fillOpenSlots(
            ModifierEligibilityProfile profile,
            MachineTraits traits,
            int componentStage,
            RandomSource random
    ) {
        if (traits.refinementPotential() <= 0) {
            return RefinementResult.failure(traits, "rngtech.refinement.failure.no_potential");
        }

        Rarity rarity = rarityAfterAdd(traits.rarity());
        List<MachineModifier> modifiers = new ArrayList<>(traits.modifiers());
        int consumedPotential = 0;
        int added = 0;
        while (consumedPotential < traits.refinementPotential()) {
            MachineTraits preview = withModifiers(traits, rarity, traits.refinementPotential() - consumedPotential, modifiers);
            List<ModifierSlot> slots = availableSlots(
                    profile,
                    preview,
                    rarity,
                    List.of(),
                    componentStage,
                    traits.refinementPotential() - consumedPotential,
                    DEFAULT_ADD_OR_UPGRADE_COST
            );
            if (slots.isEmpty()) {
                break;
            }

            MachineModifier modifier = rollModifier(
                    profile,
                    slots.get(random.nextInt(slots.size())),
                    rarity,
                    componentStage,
                    traits.refinementPotential() - consumedPotential,
                    modifiers,
                    List.of(),
                    random,
                    DEFAULT_ADD_OR_UPGRADE_COST
            );
            if (modifier == null) {
                break;
            }

            int cost = rollSpendableAddOrUpgradePotentialCost(
                    traits.refinementPotential() - consumedPotential,
                    modifier.tier(),
                    DEFAULT_ADD_OR_UPGRADE_COST,
                    random
            );
            modifiers.add(modifier);
            consumedPotential += cost;
            added++;
        }

        if (added == 0) {
            return RefinementResult.failure(traits, noAddableAffixFailureKey(traits, rarity, false));
        }

        return RefinementResult.success(
                withModifiers(traits, rarity, traits.refinementPotential() - consumedPotential, modifiers),
                consumedPotential,
                "rngtech.refinement.success.fill_slots"
        );
    }

    private static Rarity rarityAfterAdd(Rarity rarity) {
        return rarity == Rarity.NORMAL ? Rarity.MAGIC : rarity;
    }

    private static int nonExoticRandomTierStage(int componentStage) {
        return Math.min(componentStage, 7);
    }

    private static List<ModifierSlot> availableSlots(ModifierEligibilityProfile profile, MachineTraits traits, Rarity rarity) {
        return availableSlots(profile, traits, rarity, List.of(), 0, traits.refinementPotential(), DEFAULT_ADD_OR_UPGRADE_COST);
    }

    private static List<ModifierSlot> availableSlots(
            ModifierEligibilityProfile profile,
            MachineTraits traits,
            Rarity rarity,
            List<MachineStat> targetStats
    ) {
        return availableSlots(profile, traits, rarity, targetStats, 0, traits.refinementPotential(), DEFAULT_ADD_OR_UPGRADE_COST);
    }

    private static List<ModifierSlot> availableSlots(
            ModifierEligibilityProfile profile,
            MachineTraits traits,
            Rarity rarity,
            List<MachineStat> targetStats,
            int componentStage,
            int availablePotential,
            PotentialCostRange costRange
    ) {
        List<ModifierSlot> slots = new ArrayList<>();
        if (hasOpenAffixSlot(traits, rarity, ModifierSlot.PREFIX)
                && !availableDefinitions(
                        profile,
                        ModifierSlot.PREFIX,
                        traits.modifiers(),
                        targetStats,
                        componentStage,
                        availablePotential,
                        costRange
                ).isEmpty()) {
            slots.add(ModifierSlot.PREFIX);
        }
        if (hasOpenAffixSlot(traits, rarity, ModifierSlot.SUFFIX)
                && !availableDefinitions(
                        profile,
                        ModifierSlot.SUFFIX,
                        traits.modifiers(),
                        targetStats,
                        componentStage,
                        availablePotential,
                        costRange
                ).isEmpty()) {
            slots.add(ModifierSlot.SUFFIX);
        }
        return slots;
    }

    private static String noAddableAffixFailureKey(MachineTraits traits, Rarity rarity, boolean targeted) {
        if (traits.rarity() == Rarity.UNIQUE) {
            return "rngtech.refinement.failure.unique";
        }
        if (!hasAnyOpenAffixSlot(traits, rarity)) {
            return "rngtech.refinement.failure.max_affixes";
        }
        return targeted
                ? "rngtech.refinement.failure.no_matching_modifier"
                : "rngtech.refinement.failure.no_open_affix";
    }

    private static boolean hasAnyOpenAffixSlot(MachineTraits traits, Rarity rarity) {
        return hasOpenAffixSlot(traits, rarity, ModifierSlot.PREFIX)
                || hasOpenAffixSlot(traits, rarity, ModifierSlot.SUFFIX);
    }

    private static boolean hasOpenAffixSlot(MachineTraits traits, Rarity rarity, ModifierSlot slot) {
        return slot.isAffix() && count(traits.modifiers(), slot) < affixLimit(rarity);
    }

    private static int affixLimit(Rarity rarity) {
        return switch (rarity) {
            case NORMAL, UNIQUE -> 0;
            case MAGIC -> MAGIC_SLOT_LIMIT;
            case RARE -> RARE_SLOT_LIMIT;
        };
    }

    private static MachineModifier rollModifier(
            ModifierEligibilityProfile profile,
            ModifierSlot slot,
            Rarity rarity,
            int componentStage,
            int availablePotential,
            List<MachineModifier> existingModifiers,
            List<MachineStat> targetStats,
            RandomSource random,
            PotentialCostRange costRange
    ) {
        List<ModifierDefinition> definitions =
                availableDefinitions(profile, slot, existingModifiers, targetStats, componentStage, availablePotential, costRange);
        if (definitions.isEmpty()) {
            return null;
        }

        ModifierDefinition definition = MachineTraitRoller.weightedDefinition(definitions, random);
        int tier = MachineTraitRoller.rollModifierTier(
                definition,
                profile,
                rarity,
                componentStage,
                affordableRandomTierLimit(componentStage, availablePotential, definition, costRange),
                random
        );
        return definition.roll(tier, random);
    }

    private static List<ModifierRollCandidate> rollModifierCandidates(
            ModifierEligibilityProfile profile,
            Rarity rarity,
            int componentStage,
            int availablePotential,
            List<MachineModifier> existingModifiers,
            List<MachineStat> targetStats,
            List<ModifierSlot> slots,
            PotentialCostRange costRange
    ) {
        List<ModifierRollCandidate> candidates = new ArrayList<>();
        for (ModifierSlot slot : slots) {
            for (ModifierDefinition definition : availableDefinitions(
                    profile,
                    slot,
                    existingModifiers,
                    targetStats,
                    componentStage,
                    availablePotential,
                    costRange
            )) {
                candidates.add(new ModifierRollCandidate(definition));
            }
        }
        return candidates;
    }

    private static ModifierRollCandidate weightedCandidate(
            List<ModifierRollCandidate> candidates,
            Set<ModifierLensTag> lensTags,
            RandomSource random
    ) {
        int totalWeight = candidates.stream()
                .mapToInt(candidate -> lensAdjustedWeight(candidate.definition(), lensTags))
                .sum();
        if (totalWeight <= 0) {
            return candidates.get(random.nextInt(candidates.size()));
        }
        int roll = random.nextInt(totalWeight);
        int cursor = 0;
        for (ModifierRollCandidate candidate : candidates) {
            cursor += lensAdjustedWeight(candidate.definition(), lensTags);
            if (roll < cursor) {
                return candidate;
            }
        }
        return candidates.getLast();
    }

    private static MachineModifier weightedModifier(
            ModifierEligibilityProfile profile,
            List<MachineModifier> modifiers,
            Set<ModifierLensTag> lensTags,
            RandomSource random
    ) {
        int totalWeight = modifiers.stream()
                .mapToInt(modifier -> lensAdjustedWeight(definitionFor(profile, modifier), lensTags))
                .sum();
        if (totalWeight <= 0) {
            return modifiers.get(random.nextInt(modifiers.size()));
        }
        int roll = random.nextInt(totalWeight);
        int cursor = 0;
        for (MachineModifier modifier : modifiers) {
            cursor += lensAdjustedWeight(definitionFor(profile, modifier), lensTags);
            if (roll < cursor) {
                return modifier;
            }
        }
        return modifiers.getLast();
    }

    private static int lensAdjustedWeight(ModifierDefinition definition, Set<ModifierLensTag> lensTags) {
        if (definition == null) {
            return 0;
        }
        int baseWeight = definition.rollWeight();
        return definition.matchesLensTags(lensTags) ? baseWeight * LENS_WEIGHT_MULTIPLIER : baseWeight;
    }

    private static List<ModifierDefinition> availableDefinitions(
            ModifierEligibilityProfile profile,
            ModifierSlot slot,
            List<MachineModifier> existingModifiers,
            List<MachineStat> targetStats
    ) {
        return profile.rollableDefinitions(slot).stream()
                .filter(definition -> targetStats.isEmpty()
                        || (definition.canTargetWithLens() && targetStats.contains(definition.stat())))
                .filter(definition -> existingModifiers.stream().noneMatch(definition::conflictsWith))
                .toList();
    }

    private static List<ModifierDefinition> availableDefinitions(
            ModifierEligibilityProfile profile,
            ModifierSlot slot,
            List<MachineModifier> existingModifiers,
            List<MachineStat> targetStats,
            int componentStage,
            int availablePotential,
            PotentialCostRange costRange
    ) {
        return availableDefinitions(profile, slot, existingModifiers, targetStats).stream()
                .filter(definition -> hasAffordableRandomTier(definition, componentStage, availablePotential, costRange))
                .toList();
    }

    private static void rollSlotModifiers(
            ModifierEligibilityProfile profile,
            ModifierSlot slot,
            int targetCount,
            Rarity rarity,
            int componentStage,
            int availablePotential,
            RandomSource random,
            List<MachineModifier> modifiers
    ) {
        for (int index = 0; index < targetCount; index++) {
            MachineModifier modifier = rollModifier(
                    profile,
                    slot,
                    rarity,
                    componentStage,
                    availablePotential,
                    modifiers,
                    List.of(),
                    random,
                    DEFAULT_ADD_OR_UPGRADE_COST
            );
            if (modifier == null) {
                return;
            }
            modifiers.add(modifier);
        }
    }

    private static boolean hasExistingAffix(ModifierDefinition definition, MachineModifier modifier) {
        return definition.conflictsWith(modifier);
    }

    private static boolean hasAffordableRandomTier(
            ModifierDefinition definition,
            int componentStage,
            int availablePotential,
            PotentialCostRange costRange
    ) {
        if (!definition.isTiered()) {
            return availablePotential >= costRange.min();
        }
        return affordableRandomTierLimit(componentStage, availablePotential, definition, costRange) >= 1;
    }

    private static int affordableRandomTierLimit(
            int componentStage,
            int availablePotential,
            ModifierDefinition definition,
            PotentialCostRange costRange
    ) {
        if (availablePotential < costRange.min()) {
            return 0;
        }
        return Math.min(MachineTraitRoller.maxRandomTier(componentStage), Math.max(1, definition.maxTier()));
    }

    private static boolean isFixedDefinition(ModifierEligibilityProfile profile, MachineModifier modifier) {
        return profile.definitions().stream()
                .filter(definition -> !definition.canRoll())
                .anyMatch(definition -> definition.matches(modifier));
    }

    private static ModifierDefinition definitionFor(ModifierEligibilityProfile profile, MachineModifier modifier) {
        return profile.definitions().stream()
                .filter(ModifierDefinition::canRoll)
                .filter(definition -> definition.matches(modifier))
                .findFirst()
                .orElse(null);
    }

    private static ModifierDefinition targetableDefinitionFor(
            ModifierEligibilityProfile profile,
            MachineModifier modifier
    ) {
        ModifierDefinition definition = definitionFor(profile, modifier);
        return definition != null && definition.canTargetWithLens() ? definition : null;
    }

    private static boolean matchesLensTags(
            ModifierEligibilityProfile profile,
            MachineModifier modifier,
            Set<ModifierLensTag> lensTags
    ) {
        ModifierDefinition definition = definitionFor(profile, modifier);
        return definition != null && definition.matchesLensTags(lensTags);
    }

    private static ModifierSlot selectedEmptySlot(RefinementSelection selection) {
        return selection.kind() == RefinementSelection.Kind.EMPTY_SLOT ? selection.emptySlot() : null;
    }

    private static MachineModifier selectedAffix(MachineTraits traits, int affixIndex) {
        int currentIndex = 0;
        for (MachineModifier modifier : traits.modifiers()) {
            if (!modifier.slot().isAffix()) {
                continue;
            }
            if (currentIndex == affixIndex) {
                return modifier;
            }
            currentIndex++;
        }
        return null;
    }

    private static int selectedAffixModifierIndex(MachineTraits traits, int affixIndex) {
        int currentIndex = 0;
        List<MachineModifier> modifiers = traits.modifiers();
        for (int modifierIndex = 0; modifierIndex < modifiers.size(); modifierIndex++) {
            MachineModifier modifier = modifiers.get(modifierIndex);
            if (!modifier.slot().isAffix()) {
                continue;
            }
            if (currentIndex == affixIndex) {
                return modifierIndex;
            }
            currentIndex++;
        }
        return -1;
    }

    private static List<MachineModifier> upgradeableAffixes(
            ModifierEligibilityProfile profile,
            MachineTraits traits,
            boolean requireAvailablePotential,
            boolean allowTuning,
            PotentialCostRange costRange
    ) {
        return traits.modifiers().stream()
                .filter(modifier -> modifier.slot().isAffix())
                .filter(modifier -> {
                    ModifierDefinition definition = definitionFor(profile, modifier);
                    if (definition == null) {
                        return false;
                    }
                    int currentTier = Math.max(1, modifier.tier());
                    int maxUpgradeTier = Math.min(Math.max(1, definition.maxTier()), MachineTraitRoller.maxUpgradeTier());
                    if (!allowTuning && currentTier >= maxUpgradeTier) {
                        return false;
                    }
                    return !requireAvailablePotential
                            || traits.refinementPotential() >= costRange.min();
                })
                .toList();
    }

    private static MachineModifier upgradedModifier(
            ModifierEligibilityProfile profile,
            List<MachineModifier> modifiers,
            int currentIndex,
            MachineModifier current,
            ModifierDefinition currentDefinition,
            int targetTier,
            RefinementModifier refinementModifier,
            RandomSource random
    ) {
        ModifierDefinition targetDefinition = refinementModifier == RefinementModifier.TRANSMUTE_UPGRADE
                ? transmutationDefinition(profile, modifiers, currentIndex, current, targetTier, random)
                : currentDefinition;
        if (targetDefinition == null) {
            return null;
        }
        if (refinementModifier == RefinementModifier.PRESERVE_ROLL) {
            return rollAtPercentile(targetDefinition, targetTier, percentile(current));
        }
        return targetDefinition.roll(targetTier, random);
    }

    private static ModifierDefinition transmutationDefinition(
            ModifierEligibilityProfile profile,
            List<MachineModifier> modifiers,
            int currentIndex,
            MachineModifier current,
            int targetTier,
            RandomSource random
    ) {
        List<MachineModifier> otherModifiers = new ArrayList<>(modifiers);
        otherModifiers.remove(currentIndex);
        List<ModifierDefinition> definitions = profile.rollableDefinitions(current.slot()).stream()
                .filter(ModifierDefinition::canTargetWithLens)
                .filter(definition -> !definition.matches(current))
                .filter(definition -> !definition.isTiered() || definition.maxTier() >= targetTier)
                .filter(definition -> otherModifiers.stream().noneMatch(definition::conflictsWith))
                .toList();
        if (definitions.isEmpty()) {
            return null;
        }
        return MachineTraitRoller.weightedDefinition(definitions, random);
    }

    private static List<MachineModifier> destabilizeOtherAffixes(
            ModifierEligibilityProfile profile,
            List<MachineModifier> modifiers,
            int preservedIndex,
            RandomSource random
    ) {
        List<MachineModifier> rerolled = new ArrayList<>();
        MachineModifier preserved = modifiers.get(preservedIndex);
        for (int index = 0; index < modifiers.size(); index++) {
            MachineModifier modifier = modifiers.get(index);
            if (index == preservedIndex || !modifier.slot().isAffix()) {
                rerolled.add(modifier);
                continue;
            }
            List<MachineModifier> occupiedModifiers = new ArrayList<>(rerolled);
            if (!occupiedModifiers.contains(preserved)) {
                occupiedModifiers.add(preserved);
            }
            MachineModifier replacement = rollModifierAtTier(
                    profile,
                    modifier.slot(),
                    Math.max(1, modifier.tier()),
                    occupiedModifiers,
                    random
            );
            if (replacement == null) {
                ModifierDefinition definition = definitionFor(profile, modifier);
                replacement = definition == null ? modifier : definition.roll(Math.max(1, modifier.tier()), random);
            }
            rerolled.add(replacement);
        }
        if (!rerolled.contains(preserved)) {
            rerolled.set(preservedIndex, preserved);
        }
        return rerolled;
    }

    private static MachineModifier rollModifierAtTier(
            ModifierEligibilityProfile profile,
            ModifierSlot slot,
            int tier,
            List<MachineModifier> existingModifiers,
            RandomSource random
    ) {
        List<ModifierDefinition> definitions = availableDefinitions(profile, slot, existingModifiers, List.of());
        if (definitions.isEmpty()) {
            return null;
        }
        ModifierDefinition definition = MachineTraitRoller.weightedDefinition(definitions, random);
        return definition.roll(Math.min(Math.max(1, tier), Math.max(1, definition.maxTier())), random);
    }

    private static MachineModifier rollAtPercentile(ModifierDefinition definition, int tier, double percentile) {
        List<MachineModifierEffect> effects = definition.effects().stream()
                .map(effect -> rollEffectAtPercentile(effect, tier, percentile))
                .toList();
        return MachineModifier.roll(definition.id(), definition.modGroup(), definition.slot(), definition.isTiered() ? tier : 0, effects);
    }

    private static MachineModifierEffect rollEffectAtPercentile(
            ModifierEffectDefinition definition,
            int tier,
            double percentile
    ) {
        ModifierValueRange range = definition.rangeForTier(tier);
        double value = range.min() + (range.max() - range.min()) * clamp(percentile, 0.0, 1.0);
        if (range.wholeNumber()) {
            value = Math.rint(value);
        }
        value = clamp(value, range.min(), range.max());
        return new MachineModifierEffect(definition.stat(), definition.operation(), range, value);
    }

    private static double percentile(MachineModifier modifier) {
        ModifierValueRange range = modifier.range();
        if (range.max() == range.min()) {
            return 1.0;
        }
        return (modifier.value() - range.min()) / (range.max() - range.min());
    }

    private static boolean consumeCatalyst(RefinementModifier refinementModifier, RandomSource random) {
        return refinementModifier != RefinementModifier.CONSERVE_CATALYST || random.nextInt(4) != 0;
    }

    private static int adjustedPotentialCost(int cost, RefinementModifier refinementModifier, RandomSource random) {
        if (cost > 1 && refinementModifier == RefinementModifier.REDUCE_POTENTIAL_COST && random.nextInt(4) == 0) {
            return 1;
        }
        return cost;
    }

    private static boolean consumeModifier(RefinementModifier refinementModifier) {
        return refinementModifier != RefinementModifier.NONE && refinementModifier != RefinementModifier.CORRUPTION_WARD;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static MachineTraits withModifiers(
            MachineTraits traits,
            Rarity rarity,
            int refinementPotential,
            List<MachineModifier> modifiers
    ) {
        return new MachineTraits(rarity, refinementPotential, modifiers, traits.behaviors());
    }

    private static int count(List<MachineModifier> modifiers, ModifierSlot slot) {
        return (int) modifiers.stream().filter(modifier -> modifier.slot() == slot).count();
    }

    private record PotentialCostRange(int min, int max) {
        PotentialCostRange {
            if (min < 0 || max < min) {
                throw new IllegalArgumentException("Invalid refinement potential cost range");
            }
        }
    }

    private record ModifierRollCandidate(ModifierDefinition definition) {
    }

    private RefinementEngine() {
    }
}
