package com.rngtech.content.affixsorter;

import com.rngtech.content.item.CraftedTraitOutputs;
import com.rngtech.content.itemfilter.ItemFilterMatcher;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.refinement.RefinementEngine;
import com.rngtech.rpg.refinement.RefinementTargets;
import com.rngtech.rpg.unique.UniqueDefinition;
import com.rngtech.rpg.unique.UniqueItems;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.OptionalInt;

/** Runs an Affix Sorter's rules against one identified item. */
public final class SorterMatcher {
    /** The route an item takes, the index of the rule that decided it ({@code -1} for the fallback), and its pulse. */
    public record Decision(SorterRule.Route route, int ruleIndex, boolean pulse) {
    }

    public static Decision decide(SorterRules rules, ItemStack stack) {
        MachineTraits traits = RefinementTargets.storedTraits(stack);
        List<SorterRule> list = rules.rules();
        for (int index = 0; index < list.size(); index++) {
            SorterRule rule = list.get(index);
            if (rule.enabled() && matches(rule, stack, traits)) {
                return new Decision(rule.route(), index, rule.pulse());
            }
        }
        return new Decision(rules.fallback(), -1, false);
    }

    /** Whether an item can be sorted at all: it must carry rolled traits, so unidentified items are refused. */
    public static boolean canSort(ItemStack stack) {
        return !stack.isEmpty()
                && stack.has(ModDataComponents.MACHINE_TRAITS.get())
                && !CraftedTraitOutputs.isUnidentified(stack);
    }

    public static boolean matches(SorterRule rule, ItemStack stack, MachineTraits traits) {
        return matchesItem(rule, stack)
                && matchesStage(rule, stack)
                && matchesTraits(rule, traits)
                && matchesUniqueQuality(rule, stack, traits)
                && rule.corrupted().matches(RefinementTargets.isCorrupted(stack))
                && rule.mirrored().matches(RefinementTargets.isMirrored(stack));
    }

    /** The conditions that read only rolled traits: rarity, affixes, Quality, and Refinement Potential. */
    public static boolean matchesTraits(SorterRule rule, MachineTraits traits) {
        return (rule.rarities().isEmpty() || rule.rarities().contains(traits.rarity()))
                && matchesAffixes(rule, traits)
                && RefinementEngine.phaseQuality(traits) >= rule.minQuality()
                && traits.refinementPotential() >= rule.minRefinementPotential();
    }

    private static boolean matchesItem(SorterRule rule, ItemStack stack) {
        if (rule.items().isEmpty() && rule.tag().isEmpty()) {
            return true;
        }
        if (rule.items().stream().anyMatch(stack::is)) {
            return true;
        }
        return rule.tag().map(tag -> stack.is(TagKey.create(Registries.ITEM, tag))).orElse(false);
    }

    private static boolean matchesStage(SorterRule rule, ItemStack stack) {
        if (rule.minStage() == SorterRule.MIN_STAGE && rule.maxStage() == SorterRule.MAX_STAGE) {
            return true;
        }
        OptionalInt stage = ItemFilterMatcher.componentStage(stack);
        return stage.isPresent() && stage.getAsInt() >= rule.minStage() && stage.getAsInt() <= rule.maxStage();
    }

    private static boolean matchesAffixes(SorterRule rule, MachineTraits traits) {
        if (rule.affixes().isEmpty()) {
            return true;
        }
        int met = 0;
        for (SorterRule.AffixCondition condition : rule.affixes()) {
            if (hasAffix(traits, condition)) {
                met++;
            }
        }
        return met >= rule.minAffixMatches();
    }

    private static boolean hasAffix(MachineTraits traits, SorterRule.AffixCondition condition) {
        for (MachineModifier modifier : traits.modifiers()) {
            if (modifier.slot().isAffix()
                    && modifier.affixId().equals(condition.affixId())
                    && Math.max(1, modifier.tier()) >= condition.minTier()) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesUniqueQuality(SorterRule rule, ItemStack stack, MachineTraits traits) {
        if (rule.minUniqueQuality() <= 0) {
            return true;
        }
        UniqueDefinition unique = UniqueItems.definition(stack);
        return unique != null && Math.round(unique.quality(traits.modifiers()) * 100.0D) >= rule.minUniqueQuality();
    }

    private SorterMatcher() {
    }
}
