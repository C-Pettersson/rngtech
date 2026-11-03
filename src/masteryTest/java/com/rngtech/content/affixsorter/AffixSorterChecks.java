package com.rngtech.content.affixsorter;

import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.ModifierOperation;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.ModifierValueRange;
import com.rngtech.rpg.Rarity;

import java.util.List;
import java.util.Optional;

/** Affix Sorter rule lists, trait conditions, and the per-item energy curve. */
public final class AffixSorterChecks {
    private static int checks;

    private AffixSorterChecks() {
    }

    public static int run() {
        checks = 0;
        energyGrowsAsRulesToTheOneAndAHalf();
        identifierCostsScaleWithStage();
        traitConditionsMatch();
        ruleListEditsKeepOrder();
        capacityDisablesFromTheBottom();
        return checks;
    }

    private static void energyGrowsAsRulesToTheOneAndAHalf() {
        long one = AffixSorterCosts.sorterFePerItem(1);
        require(one == AffixSorterCosts.DEFAULT_SORTER_BASE_FE, "one rule costs E0, was " + one);
        require(AffixSorterCosts.sorterFePerItem(0) == one, "an empty list costs as much as one rule");
        require(Math.abs(AffixSorterCosts.sorterFePerItem(10) / (double) one - Math.pow(10, 1.5)) < 0.01, "ten rules cost 10^1.5 times one");
        require(Math.abs(AffixSorterCosts.sorterFePerItem(20) / (double) one - 89.44) < 0.01, "twenty rules cost about 89 times one");
        for (int rules = 1; rules < 32; rules++) {
            long cost = AffixSorterCosts.sorterFePerItem(rules);
            long next = AffixSorterCosts.sorterFePerItem(rules + 1);
            require(next - cost > AffixSorterCosts.sorterFePerItem(rules) - AffixSorterCosts.sorterFePerItem(rules - 1) - 1,
                    "each added rule costs at least as much as the one before (" + rules + ")");
        }
    }

    private static void identifierCostsScaleWithStage() {
        require(AffixSorterCosts.identifierFePerItem(8) == AffixSorterCosts.DEFAULT_IDENTIFIER_FE_PER_ITEM, "a Stage 8 item pays the full FE");
        require(AffixSorterCosts.identifierFluidPerItem(8) == AffixSorterCosts.DEFAULT_IDENTIFIER_FLUID_PER_ITEM, "a Stage 8 item pays the full fluid");
        for (int stage = 0; stage < 8; stage++) {
            require(AffixSorterCosts.identifierFePerItem(stage) < AffixSorterCosts.identifierFePerItem(stage + 1), "FE rises with stage " + stage);
            require(AffixSorterCosts.identifierFluidPerItem(stage) <= AffixSorterCosts.identifierFluidPerItem(stage + 1), "fluid rises with stage " + stage);
            require(AffixSorterCosts.identifierFluidPerItem(stage) >= 1, "every item costs some fluid");
        }
    }

    private static void traitConditionsMatch() {
        MachineTraits rare = new MachineTraits(Rarity.RARE, 12, List.of(
                affix("rngtech:speed", ModifierSlot.PREFIX, 6),
                affix("rngtech:energy", ModifierSlot.SUFFIX, 2)
        ));
        SorterRule base = SorterRule.DEFAULT;
        require(SorterMatcher.matchesTraits(base, rare), "an empty rule matches anything");
        require(SorterMatcher.matchesTraits(base.withRarities(List.of(Rarity.RARE, Rarity.UNIQUE)), rare), "rarity in the list matches");
        require(!SorterMatcher.matchesTraits(base.withRarities(List.of(Rarity.MAGIC)), rare), "rarity outside the list fails");

        SorterRule speedSix = base.withAffixes(List.of(new SorterRule.AffixCondition("rngtech:speed", 6)), 1);
        require(SorterMatcher.matchesTraits(speedSix, rare), "an affix at its minimum tier matches");
        SorterRule speedSeven = base.withAffixes(List.of(new SorterRule.AffixCondition("rngtech:speed", 7)), 1);
        require(!SorterMatcher.matchesTraits(speedSeven, rare), "an affix below the minimum tier fails");

        SorterRule twoOfThree = base.withAffixes(List.of(
                new SorterRule.AffixCondition("rngtech:speed", 1),
                new SorterRule.AffixCondition("rngtech:energy", 3),
                new SorterRule.AffixCondition("rngtech:missing", 0)
        ), 2);
        require(!SorterMatcher.matchesTraits(twoOfThree, rare), "one of three met fails at least two");
        SorterRule oneOfThree = twoOfThree.withAffixes(twoOfThree.affixes(), 1);
        require(SorterMatcher.matchesTraits(oneOfThree, rare), "one of three met passes at least one");
        require(twoOfThree.withAffixes(twoOfThree.affixes(), 9).minAffixMatches() == 3, "the match count clamps to the affix count");

        require(SorterMatcher.matchesTraits(base.withThresholds(8, 12, 0), rare), "Quality 8 and RP 12 meet their minimums");
        require(!SorterMatcher.matchesTraits(base.withThresholds(9, 0, 0), rare), "Quality 9 fails on tiers 6 and 2");
        require(!SorterMatcher.matchesTraits(base.withThresholds(0, 13, 0), rare), "RP 13 fails on 12 RP");
    }

    private static void ruleListEditsKeepOrder() {
        SorterRule pass = SorterRule.DEFAULT;
        SorterRule reject = SorterRule.DEFAULT.withRoute(SorterRule.Route.REJECT);
        SorterRules rules = SorterRules.EMPTY.withAdded(pass).withAdded(reject);
        require(rules.fallback() == SorterRule.Route.REJECT, "items no rule matches are rejected by default");
        SorterRules moved = rules.withMoved(1, -1);
        require(moved.rules().get(0).route() == SorterRule.Route.REJECT, "moving a rule up swaps it with the one above");
        require(moved.withMoved(0, -1).equals(moved), "the top rule cannot move further up");
        require(moved.withRemoved(0).rules().size() == 1, "removing a rule shortens the list");
        SorterRules full = SorterRules.EMPTY;
        for (int index = 0; index < SorterRules.MAX_RULES + 4; index++) {
            full = full.withAdded(pass);
        }
        require(full.rules().size() == SorterRules.MAX_RULES, "the list stores at most " + SorterRules.MAX_RULES + " rules");
        SorterRule tagged = pass.withItems(List.of(), Optional.empty());
        require(tagged.items().isEmpty() && tagged.tag().isEmpty(), "a rule with no items or tag matches any item");
    }

    private static void capacityDisablesFromTheBottom() {
        SorterRules rules = SorterRules.EMPTY;
        for (int index = 0; index < 10; index++) {
            rules = rules.withAdded(SorterRule.DEFAULT.withPulse(index % 2 == 0));
        }
        SorterRules limited = rules.limitedTo(8);
        require(limited.enabledCount() == 8, "a Rule Card write keeps capacity, was " + limited.enabledCount());
        require(limited.rules().get(7).enabled() && !limited.rules().get(8).enabled() && !limited.rules().get(9).enabled(),
                "rules past capacity arrive disabled from the bottom");
        require(limited.rules().get(8).pulse(), "disabling a rule keeps its other settings");
    }

    private static MachineModifier affix(String id, ModifierSlot slot, int tier) {
        return new MachineModifier(id, id, slot, MachineStat.PROCESSING_SPEED, ModifierOperation.INCREASED_PERCENT, tier,
                ModifierValueRange.fixed(0.1), 0.1, List.of());
    }

    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError(label);
        }
    }
}
