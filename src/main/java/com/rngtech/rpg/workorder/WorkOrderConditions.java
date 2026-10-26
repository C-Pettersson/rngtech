package com.rngtech.rpg.workorder;

import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.ModifierDefinition;
import com.rngtech.rpg.ModifierEligibilityProfile;
import com.rngtech.rpg.ModifierOperation;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.ModifierValueRange;
import com.rngtech.rpg.Rarity;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A Job Chip's conditions: affixes from a {@code work_order} profile per family, rolled by rarity when a job is listed.
 * Prefixes are pressure conditions, costs on the machine's stats while the chip is active. Suffixes are rule conditions
 * on the job: one resets progress on a machine event, and one raises a stage job's minimum. Every condition adds Bounty,
 * and the rule conditions add a Prestige too. Draft numbers, to tune with the processing balance simulation.
 */
public final class WorkOrderConditions {
    public static final String STARVED_FEED = "starved_feed";
    public static final String POWER_SURCHARGE = "power_surcharge";
    public static final String SHORT_CREW = "short_crew";
    public static final String COLD_START = "cold_start";
    public static final String HAND_FEEDING = "hand_feeding";
    public static final String BRITTLE_LINING = "brittle_lining";
    public static final String JAMMED_LINE = "jammed_line";
    public static final String DEEP_ORE = "deep_ore";
    public static final String UNSTEADY_HANDS = "unsteady_hands";
    public static final String FINE_TOLERANCE = "fine_tolerance";

    /** Draft rarity weights for a listed job: Normal, Magic, Rare. */
    private static final int[] RARITY_WEIGHTS = {50, 35, 15};
    private static final Set<String> ALL = Set.of();

    /**
     * One condition: its affix definition, the families that can read it (none listed means every family), the filter a
     * job needs for it to roll (null for any job), whether it is a pressure condition applied to the machine's stats, and
     * its Bounty for each tier and Prestige.
     */
    public record Condition(ModifierDefinition definition, Set<String> families, WorkFilter needs, boolean pressure,
            int[] bounty, int prestige) {
        public String id() {
            return definition.id();
        }

        boolean fits(String family, WorkObjective objective) {
            return (families.isEmpty() || families.contains(family))
                    && (needs == null || objective.parts().stream().anyMatch(part -> needs.id().equals(part.filter())));
        }

        /** The Bounty this condition adds at {@code tier}; an untiered condition reads tier 0 as its one tier. */
        public int bounty(int tier) {
            return bounty[Math.clamp(tier, 1, bounty.length) - 1];
        }
    }

    private static final List<Condition> CONDITIONS = List.of(
            new Condition(tiered(STARVED_FEED, ModifierSlot.PREFIX, MachineStat.PROCESSING_SPEED, ModifierOperation.LESS,
                    range(0.80, 0.85), range(0.75, 0.80), range(0.70, 0.75)), ALL, null, true, new int[] {15, 22, 30}, 0),
            new Condition(tiered(POWER_SURCHARGE, ModifierSlot.PREFIX, MachineStat.ENERGY_USAGE, ModifierOperation.INCREASED_PERCENT,
                    whole(25, 33), whole(34, 41), whole(42, 50)), Set.of("crusher", "furnace", "alloy_furnace", "metal_press", "melter",
                    "resonance_calibrator", "component_assembler"), null, true, new int[] {10, 17, 25}, 0),
            new Condition(fixed(SHORT_CREW, ModifierSlot.PREFIX, MachineStat.BATCH_SIZE, ModifierOperation.ADD, -1),
                    Set.of("crusher", "furnace", "alloy_furnace", "metal_press", "melter", "resonance_calibrator"), null, true, new int[] {30}, 0),
            new Condition(tiered(COLD_START, ModifierSlot.PREFIX, MachineStat.TEMPERATURE_STABILITY, ModifierOperation.DECREASED_PERCENT,
                    whole(10, 13), whole(14, 16), whole(17, 20)), Set.of("furnace", "alloy_furnace", "metal_press"), null, true, new int[] {15, 20, 25}, 0),
            new Condition(fixed(HAND_FEEDING, ModifierSlot.SUFFIX, MachineStat.INSTANT_PROCESS_CHANCE, ModifierOperation.LESS, 0),
                    Set.of("crusher"), null, true, new int[] {20}, 0),
            new Condition(rule(BRITTLE_LINING), Set.of("furnace", "alloy_furnace", "metal_press"), null, false, new int[] {35}, 1),
            new Condition(rule(JAMMED_LINE), Set.of("crusher"), null, false, new int[] {30}, 1),
            new Condition(rule(DEEP_ORE), ALL, WorkFilter.STAGE, false, new int[] {25}, 1),
            new Condition(tiered(UNSTEADY_HANDS, ModifierSlot.PREFIX, MachineStat.STABILITY, ModifierOperation.DECREASED_PERCENT,
                    whole(10, 13), whole(14, 16), whole(17, 20)), Set.of("resonance_calibrator", "metal_press"), null, true, new int[] {15, 20, 25}, 0),
            new Condition(rule(FINE_TOLERANCE), Set.of("resonance_calibrator"), WorkFilter.SETTING, false, new int[] {35}, 1));

    private WorkOrderConditions() {
    }

    public static List<Condition> all() {
        return CONDITIONS;
    }

    public static Condition get(String id) {
        return CONDITIONS.stream().filter(condition -> condition.id().equals(id)).findFirst().orElse(null);
    }

    /** The conditions {@code family} can read, as an eligibility profile for rolling and, later, refinement. */
    public static ModifierEligibilityProfile profile(String family) {
        return new ModifierEligibilityProfile("work_order_" + family, Set.of(),
                CONDITIONS.stream().filter(condition -> condition.families().isEmpty() || condition.families().contains(family))
                        .map(Condition::definition).toList());
    }

    /** A job's rarity, by the draft weights. */
    public static Rarity rollRarity(RandomSource random) {
        int pick = random.nextInt(RARITY_WEIGHTS[0] + RARITY_WEIGHTS[1] + RARITY_WEIGHTS[2]);
        return pick < RARITY_WEIGHTS[0] ? Rarity.NORMAL : pick < RARITY_WEIGHTS[0] + RARITY_WEIGHTS[1] ? Rarity.MAGIC : Rarity.RARE;
    }

    /** A rolled job: the order with its conditions' Bounty and Prestige added, and the chip's traits. */
    public record Rolled(WorkOrder order, MachineTraits traits) { }

    /**
     * Rolls {@code order}'s conditions at {@code rarity}, or at a rolled rarity when it is null: none for Normal, one or
     * two for Magic, three or four for Rare, at most one prefix and one suffix on Magic and three of each on Rare, each
     * from a different group and readable by the order's family. Fewer roll when the family has fewer to offer.
     */
    public static Rolled roll(WorkOrder order, Rarity rarity, RandomSource random) {
        Rarity rolledRarity = rarity == null ? rollRarity(random) : rarity;
        int wanted = switch (rolledRarity) {
            case MAGIC -> 1 + random.nextInt(2);
            case RARE -> 3 + random.nextInt(2);
            default -> 0;
        };
        int slotLimit = rolledRarity == Rarity.RARE ? 3 : 1;
        List<Condition> pool = new ArrayList<>(CONDITIONS.stream().filter(condition -> condition.fits(order.family(), order.objective())).toList());
        List<MachineModifier> modifiers = new ArrayList<>();
        Set<String> groups = new HashSet<>();
        for (int count = 0; count < wanted; count++) {
            List<Condition> open = pool.stream()
                    .filter(condition -> !groups.contains(condition.definition().modGroup()))
                    .filter(condition -> modifiers.stream().filter(modifier -> modifier.slot() == condition.definition().slot()).count() < slotLimit)
                    .toList();
            if (open.isEmpty()) { break; }
            Condition picked = open.get(random.nextInt(open.size()));
            int tier = picked.definition().isTiered() ? 1 + random.nextInt(picked.definition().tierRanges().size()) : 1;
            modifiers.add(picked.definition().roll(tier, random));
            groups.add(picked.definition().modGroup());
        }
        int bounty = order.bounty();
        int prestige = order.prestige();
        for (MachineModifier modifier : modifiers) {
            Condition condition = get(modifier.affixId());
            bounty += condition.bounty(modifier.tier());
            prestige += condition.prestige();
        }
        MachineTraits traits = new MachineTraits(rolledRarity, 0, modifiers, List.of());
        return new Rolled(new WorkOrder(order.family(), order.tier(), order.objective(), order.minStage(), order.minMasteryLevel(), bounty, prestige,
                order.rewards(), order.template(), order.name(), order.rewardTable()), traits);
    }

    /** Whether {@code traits} carry the condition {@code id}. */
    public static boolean has(MachineTraits traits, String id) {
        return traits != null && traits.modifiers().stream().anyMatch(modifier -> modifier.affixId().equals(id));
    }

    /** Whether {@code report} counts toward {@code part} under {@code traits}: Deep Ore raises a stage part's minimum by one. */
    public static boolean counts(WorkPart part, WorkReport report, MachineTraits traits) {
        if (has(traits, DEEP_ORE) && WorkFilter.STAGE.id().equals(part.filter())) {
            return new WorkPart(part.type(), part.filter(), part.minimum() + 1, part.item(), part.target()).counts(report);
        }
        return part.counts(report);
    }

    /**
     * Whether work that misses {@code part} sets it back to 0: a streak's work below its minimum, and a stability job's
     * work below its minimum under Fine Tolerance.
     */
    public static boolean breaks(WorkPart part, MachineTraits traits) {
        return WorkFilter.STREAK.id().equals(part.filter()) || (has(traits, FINE_TOLERANCE) && WorkFilter.SETTING.id().equals(part.filter()));
    }

    /** The minimum a stage part needs under {@code traits}, for the job's Shift list. */
    public static int minimum(WorkPart part, MachineTraits traits) {
        return part.minimum() + (has(traits, DEEP_ORE) && WorkFilter.STAGE.id().equals(part.filter()) ? 1 : 0);
    }

    /**
     * Applies the active chip's pressure conditions to a machine's stats, after its Gear, tree, and ascendancy effects,
     * labelled as Work Order costs in a stat breakdown.
     */
    public static void apply(MachineStatAccumulator stats, LoadedWorkOrder active) {
        if (active == null || active.fulfilled()) { return; }
        for (MachineModifier modifier : active.traits().modifiers()) {
            Condition condition = get(modifier.affixId());
            if (condition == null || !condition.pressure()) { continue; }
            stats.apply(Component.translatable("rngtech.stat.breakdown.source.work_order",
                            Component.translatable("rngtech.work_order.condition." + condition.id())),
                    new MachineModifier(ModifierSlot.IMPLICIT, modifier.stat(), modifier.operation(), modifier.value()));
        }
    }

    private static ModifierDefinition tiered(String id, ModifierSlot slot, MachineStat stat, ModifierOperation operation, ModifierValueRange... tiers) {
        return ModifierDefinition.rollable(id, id, slot, stat, operation, List.of(tiers));
    }

    private static ModifierDefinition fixed(String id, ModifierSlot slot, MachineStat stat, ModifierOperation operation, double value) {
        return ModifierDefinition.rollableUntiered(id, id, slot, stat, operation, ModifierValueRange.fixed(value));
    }

    /** A rule condition changes no stat; the machine or the counting reads it by id. */
    private static ModifierDefinition rule(String id) {
        return fixed(id, ModifierSlot.SUFFIX, MachineStat.PROCESSING_SPEED, ModifierOperation.ADD, 0);
    }

    private static ModifierValueRange range(double min, double max) {
        return new ModifierValueRange(min, max, false);
    }

    private static ModifierValueRange whole(double min, double max) {
        return new ModifierValueRange(min, max, true);
    }
}
