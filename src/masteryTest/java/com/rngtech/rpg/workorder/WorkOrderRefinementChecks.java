package com.rngtech.rpg.workorder;

import com.rngtech.rpg.CorruptionOutcome;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineTraitRoller;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.ModifierDefinition;
import com.rngtech.rpg.Rarity;
import com.rngtech.rpg.refinement.RefinementEngine;
import com.rngtech.rpg.refinement.RefinementOperation;
import com.rngtech.rpg.refinement.RefinementResult;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Job Chips as refinement targets: Refinement Potential on listed chips, a profile per chip, the refinement engine on chip
 * conditions with Bounty kept in step, and the Volatile Catalyst's chip outcomes.
 */
public final class WorkOrderRefinementChecks {
    private static int checks;

    private WorkOrderRefinementChecks() { }

    public static int run() {
        checks = 0;
        potential();
        profiles();
        refinement();
        corruption();
        return checks;
    }

    private static void potential() {
        for (int tier = WorkOrder.MIN_TIER; tier <= WorkOrder.MAX_TIER; tier++) {
            WorkOrder order = order("crusher", WorkFilter.STAGE, tier);
            var range = MachineTraitRoller.refinementPotentialRange(WorkOrderConditions.profile(order), WorkOrder.tierStage(tier));
            for (int seed = 0; seed < 50; seed++) {
                int rp = WorkOrderConditions.roll(order, Rarity.MAGIC, RandomSource.create(seed)).traits().refinementPotential();
                require(rp >= range.min() && rp <= range.max() && rp > 0, "a Tier " + tier + " chip rolls Refinement Potential for its stage: " + rp);
            }
        }
    }

    /** A chip's profile holds only the conditions its job can use. */
    private static void profiles() {
        Set<String> stage = ids(WorkOrderConditions.profile(order("crusher", WorkFilter.STAGE, 1)).definitions());
        Set<String> any = ids(WorkOrderConditions.profile(order("crusher", WorkFilter.ANY, 1)).definitions());
        Set<String> stability = ids(WorkOrderConditions.profile(order("resonance_calibrator", WorkFilter.SETTING, 1)).definitions());
        require(stage.contains(WorkOrderConditions.DEEP_ORE) && !any.contains(WorkOrderConditions.DEEP_ORE), "Deep Ore is only offered to stage jobs");
        require(stability.contains(WorkOrderConditions.FINE_TOLERANCE) && !any.contains(WorkOrderConditions.FINE_TOLERANCE),
                "Fine Tolerance is only offered to Calibrator stability jobs");
        require(!stage.contains(WorkOrderConditions.COLD_START), "a Crusher chip is never offered a Furnace condition");
    }

    /** The refinement engine works on chip conditions, and Bounty moves with them. */
    private static void refinement() {
        WorkOrder base = order("crusher", WorkFilter.STAGE, 3);
        RandomSource random = RandomSource.create(9L);
        for (int seed = 0; seed < 200; seed++) {
            WorkOrderConditions.Rolled rolled = WorkOrderConditions.roll(base, Rarity.MAGIC, RandomSource.create(seed));
            var profile = WorkOrderConditions.profile(rolled.order());
            for (RefinementOperation operation : List.of(RefinementOperation.ADD_MODIFIER, RefinementOperation.CHAOS_CRYSTAL,
                    RefinementOperation.UPGRADE_RANDOM_MODIFIER, RefinementOperation.NULL_CRYSTAL, RefinementOperation.ASCEND_RARITY)) {
                RefinementResult result = RefinementEngine.apply(profile, rolled.traits(), operation, WorkOrder.tierStage(3), random);
                if (!result.success()) { continue; }
                for (MachineModifier modifier : result.traits().modifiers()) {
                    require(ids(profile.definitions()).contains(modifier.affixId()), operation + " only rolls conditions the chip can carry: " + modifier.affixId());
                }
                WorkOrder rebalanced = WorkOrderConditions.rebalance(rolled.order(), rolled.traits(), result.traits());
                require(rebalanced.bounty() == base.bounty() + WorkOrderConditions.bounty(result.traits())
                        && rebalanced.prestige() == base.prestige() + WorkOrderConditions.prestige(result.traits()),
                        operation + " keeps Bounty and Prestige in step with the conditions");
            }
        }
    }

    private static void corruption() {
        WorkOrder base = order("crusher", WorkFilter.STAGE, 2);
        Set<String> blessings = new HashSet<>();
        for (int seed = 0; seed < 300; seed++) {
            WorkOrderConditions.Rolled rolled = WorkOrderConditions.roll(base, Rarity.MAGIC, RandomSource.create(seed));
            for (CorruptionOutcome outcome : CorruptionOutcome.values()) {
                WorkOrderConditions.Rolled corrupted = WorkOrderConditions.corrupt(rolled.order(), rolled.traits(), outcome, RandomSource.create(seed * 7L + outcome.ordinal()));
                MachineTraits traits = corrupted.traits();
                require(traits.isCorrupted() && traits.corruption().outcome() == outcome, outcome + " leaves the chip Corrupted");
                switch (outcome) {
                    case BLESSED -> {
                        boolean reward = corrupted.order().bounty() == WorkOrder.rushAmount(rolled.order().bounty()) && traits.corruption().modifiers().isEmpty();
                        boolean prestige = corrupted.order().prestige() == rolled.order().prestige() + 1 && corrupted.order().bounty() == rolled.order().bounty();
                        boolean free = traits.corruption().modifiers().size() == 1
                                && corrupted.order().bounty() == rolled.order().bounty() + WorkOrderConditions.bounty(traits) - WorkOrderConditions.bounty(rolled.traits())
                                && WorkOrderConditions.active(traits).size() == rolled.traits().modifiers().size();
                        require(reward || prestige || free, "Blessed pays more, adds a Prestige, or adds a condition that pays with no cost: " + rolled.order().bounty() + "/" + rolled.order().prestige() + " -> " + corrupted.order().bounty() + "/" + corrupted.order().prestige() + " " + traits.corruption() + " active " + WorkOrderConditions.active(traits).size() + " vs " + rolled.traits().modifiers().size());
                        blessings.add(reward ? "reward" : prestige ? "prestige" : "free");
                    }
                    case BLIGHTED -> {
                        boolean extra = traits.corruption().modifiers().size() == 1 && WorkOrderConditions.active(traits).size() == rolled.traits().modifiers().size() + 1;
                        require(corrupted.order().bounty() == rolled.order().bounty() && WorkOrderConditions.bounty(traits) == WorkOrderConditions.bounty(rolled.traits())
                                && (extra || traits.corruption().modifiers().isEmpty()), "Blighted adds a condition that costs and pays nothing");
                        require(traits.corruption().modifiers().stream().noneMatch(modifier ->
                                rolled.traits().modifiers().stream().anyMatch(own -> own.modGroup().equals(modifier.modGroup()))), "Blighted never repeats a chip's condition");
                    }
                    case REFORGED -> require(traits.rarity() == rolled.traits().rarity()
                            && corrupted.order().bounty() == base.bounty() + WorkOrderConditions.bounty(traits), "Reforged rerolls the conditions at the chip's rarity and pays for the new ones");
                    case WARPED -> require(traits.modifiers().size() == rolled.traits().modifiers().size()
                            && corrupted.order().bounty() == rolled.order().bounty(), "Warped keeps the conditions and their tiers, so their pay stays");
                    default -> require(corrupted.order().equals(rolled.order()) && traits.modifiers().equals(rolled.traits().modifiers()), "Untouched only marks the chip");
                }
            }
        }
        require(blessings.equals(Set.of("reward", "prestige", "free")), "every Blessed outcome happens: " + blessings);
        WorkOrderConditions.Rolled rare = WorkOrderConditions.roll(base, Rarity.RARE, RandomSource.create(3L));
        WorkOrderConditions.Rolled corrupted = WorkOrderConditions.corrupt(rare.order(), rare.traits(), CorruptionOutcome.UNTOUCHED, RandomSource.create(3L));
        require(!RefinementEngine.apply(WorkOrderConditions.profile(rare.order()), corrupted.traits(), RefinementOperation.CHAOS_CRYSTAL, 6, RandomSource.create(1L)).success(),
                "a corrupted chip takes no more refinement");
    }

    private static WorkOrder order(String family, WorkFilter filter, int tier) {
        WorkObjectiveType type = switch (family) {
            case "resonance_calibrator" -> WorkObjectiveType.CALIBRATE;
            case "furnace" -> WorkObjectiveType.SMELT;
            default -> WorkObjectiveType.CRUSH;
        };
        WorkObjective objective = new WorkObjective(ResourceLocation.parse("rngtech:refine_" + family + "_" + filter.id()),
                new WorkPart(type, filter, filter == WorkFilter.ANY ? 0 : 4, "", 100));
        return new WorkOrder(family, tier, objective, WorkOrder.tierStage(tier), 0, WorkOrder.BASE_BOUNTY, 0);
    }

    private static Set<String> ids(List<ModifierDefinition> definitions) {
        Set<String> ids = new HashSet<>();
        definitions.forEach(definition -> ids.add(definition.id()));
        return ids;
    }

    private static void require(boolean condition, String label) { checks++; if (!condition) { throw new AssertionError(label); } }
}
