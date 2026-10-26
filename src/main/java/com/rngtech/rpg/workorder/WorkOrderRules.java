package com.rngtech.rpg.workorder;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.random.RandomGenerator;

/** Rolling, loading, counting, and paying out Work Orders, apart from any machine or world. */
public final class WorkOrderRules {
    private WorkOrderRules() { }

    /** The result of reported work: the machine's next chips, and the chip it fulfilled, or null. */
    public record Progress(MachineWorkOrders orders, LoadedWorkOrder fulfilled) { }

    /** Rolls a random job of a random tier, each tier equally likely, the way the Dispatch Terminal fills a listing. */
    public static WorkOrder roll(Collection<WorkObjectiveEntry> entries, RandomGenerator random, Predicate<String> itemAvailable) {
        return roll(entries, WorkOrder.MIN_TIER + random.nextInt(WorkOrder.MAX_TIER - WorkOrder.MIN_TIER + 1), random, itemAvailable);
    }

    public static WorkOrder roll(Collection<WorkObjectiveEntry> entries, RandomGenerator random) {
        return roll(entries, random, item -> true);
    }

    public static WorkOrder roll(Collection<WorkObjectiveEntry> entries, int tier, RandomGenerator random) {
        return roll(entries, tier, random, item -> true);
    }

    /**
     * Rolls a random job of {@code tier}: an entry by weight among those a chassis at the tier's stage can run, its parts and
     * count, the tier's stage requirement capped at the family's top stage, base Bounty plus the count's band bonus, and no
     * Mastery level requirement. {@code itemAvailable} says whether an item option has a loaded recipe. Returns null when no
     * entry can roll.
     */
    public static WorkOrder roll(Collection<WorkObjectiveEntry> entries, int tier, RandomGenerator random, Predicate<String> itemAvailable) {
        return roll(entries, true, List.of(), tier, random, itemAvailable);
    }

    /**
     * Rolls a listing the way the Dispatch Terminal does: at {@code tier}, or with 0 at a tier picked evenly among the tiers
     * that have something to draw, one draw by weight from the random objectives (only while {@code randomJobs} is on) and
     * the pooled templates that fit the tier. Objective and template weights share one scale. {@code objectives} is every
     * loaded objective, which templates name even with random jobs off. Returns null when nothing can roll.
     */
    public static WorkOrder roll(Collection<WorkObjectiveEntry> objectives, boolean randomJobs, Collection<WorkOrderTemplate> templates, int tier,
            RandomGenerator random, Predicate<String> itemAvailable) {
        Map<ResourceLocation, WorkObjectiveEntry> byId = new HashMap<>();
        objectives.forEach(entry -> byId.put(entry.id(), entry));
        List<WorkObjectiveEntry> entries = randomJobs ? objectives.stream().sorted(Comparator.comparing(entry -> entry.id().toString())).toList() : List.of();
        List<WorkOrderTemplate> pooled = templates.stream().filter(WorkOrderTemplate::pooled)
                .sorted(Comparator.comparing(template -> template.id().toString())).toList();
        int rolledTier = tier;
        if (tier == 0) {
            List<Integer> tiers = new ArrayList<>();
            for (int candidate = WorkOrder.MIN_TIER; candidate <= WorkOrder.MAX_TIER; candidate++) {
                if (!candidates(entries, pooled, byId, candidate, itemAvailable).isEmpty()) { tiers.add(candidate); }
            }
            if (tiers.isEmpty()) { return null; }
            rolledTier = tiers.get(random.nextInt(tiers.size()));
        }
        List<Candidate> candidates = candidates(entries, pooled, byId, rolledTier, itemAvailable);
        if (candidates.isEmpty()) { return null; }
        int pick = random.nextInt(candidates.stream().mapToInt(Candidate::weight).sum());
        Candidate chosen = candidates.getLast();
        for (Candidate candidate : candidates) {
            pick -= candidate.weight();
            if (pick < 0) { chosen = candidate; break; }
        }
        if (chosen.template() != null) {
            return chosen.template().roll(rolledTier, byId, random, itemAvailable);
        }
        WorkObjectiveEntry entry = chosen.entry();
        WorkObjective objective = entry.roll(rolledTier, stageCap(rolledTier, entry), random, itemAvailable);
        return new WorkOrder(entry.family(), rolledTier, objective, WorkOrderFamilies.requiredStage(entry.family(), rolledTier), 0,
                WorkOrder.BASE_BOUNTY + objective.bonus(), 0);
    }

    /** One thing a listing can draw: a random objective or a pooled template, with its weight. */
    private record Candidate(WorkObjectiveEntry entry, WorkOrderTemplate template, int weight) { }

    private static List<Candidate> candidates(List<WorkObjectiveEntry> entries, List<WorkOrderTemplate> pooled,
            Map<ResourceLocation, WorkObjectiveEntry> byId, int tier, Predicate<String> itemAvailable) {
        List<Candidate> candidates = new ArrayList<>();
        for (WorkObjectiveEntry entry : entries) {
            if (WorkOrderFamilies.known(entry.family())
                    && (!entry.filter().named() || !entry.options(stageCap(tier, entry), itemAvailable).isEmpty())) {
                candidates.add(new Candidate(entry, null, entry.weight()));
            }
        }
        for (WorkOrderTemplate template : pooled) {
            if (template.canRoll(tier, byId, itemAvailable)) {
                candidates.add(new Candidate(null, template, template.weight()));
            }
        }
        return candidates;
    }

    private static int stageCap(int tier, WorkObjectiveEntry entry) {
        return WorkOrderFamilies.stageCap(entry.family(), tier);
    }

    /**
     * Why a machine of {@code family} at {@code stage} and {@code masteryLevel} cannot load {@code order}, as a message
     * key, or null when it can. A blank chip, one with no job, never loads.
     */
    public static String loadFailure(MachineWorkOrders orders, String family, int stage, int masteryLevel, WorkOrder order) {
        if (order == null) { return "blank"; }
        if (!order.family().equals(family)) { return "family"; }
        if (orders.full()) { return "full"; }
        if (stage < order.minStage()) { return "stage"; }
        if (order.minMasteryLevel() > 0 && masteryLevel < order.minMasteryLevel()) { return "level"; }
        return null;
    }

    /**
     * Counts {@code report} toward the active chip: into the first unfinished part it matches, and any amount past that
     * part's target into the next matching part. Work of the part's verb that misses a streak's minimum, or a stability job's
     * minimum under Fine Tolerance, sets that part back to 0. No active chip or no matching part changes nothing, and the same chips
     * come back. A fulfilled chip stays in its bay, inactive, and no other chip becomes active.
     */
    public static Progress progress(MachineWorkOrders orders, WorkReport report) {
        LoadedWorkOrder active = orders.activeOrder();
        if (active == null || report.amount() <= 0) {
            return new Progress(orders, null);
        }
        LoadedWorkOrder advanced = active;
        long left = report.amount();
        List<WorkPart> parts = active.order().objective().parts();
        for (int index = 0; index < parts.size() && left > 0; index++) {
            WorkPart part = parts.get(index);
            long current = advanced.progress().get(index);
            boolean counts = WorkOrderConditions.counts(part, report, active.traits());
            if (!counts && current > 0 && current < part.target() && report.type().id().equals(part.type())
                    && WorkOrderConditions.breaks(part, active.traits())) {
                advanced = advanced.withPartProgress(index, 0);
                continue;
            }
            if (current >= part.target() || !counts) { continue; }
            long added = Math.min(left, part.target() - current);
            advanced = advanced.withPartProgress(index, current + added);
            left -= added;
        }
        if (advanced == active) { return new Progress(orders, null); }
        return new Progress(orders.withReplaced(orders.active(), advanced), advanced.fulfilled() ? advanced : null);
    }

    /**
     * Breaks the active chip's streaks after a failure on the machine: each unfinished streak part goes back to 0. The chip
     * keeps its best streak. Returns the same chips when no streak was running.
     */
    public static MachineWorkOrders breakStreaks(MachineWorkOrders orders) {
        LoadedWorkOrder active = orders.activeOrder();
        if (active == null) { return orders; }
        LoadedWorkOrder next = active;
        List<WorkPart> parts = active.order().objective().parts();
        for (int index = 0; index < parts.size(); index++) {
            WorkPart part = parts.get(index);
            long current = active.progress().get(index);
            if (WorkFilter.STREAK.id().equals(part.filter()) && current > 0 && current < part.target()) {
                next = next.withPartProgress(index, 0);
            }
        }
        return next.equals(active) ? orders : orders.withReplaced(orders.active(), next);
    }

    /**
     * Checks the active chip's stat goals after a completed job: each one's progress becomes the machine's current value,
     * in hundredths and capped at the target, so it can fall as well as rise until the goal is met. A chip whose every part
     * is met is done. Returns the same chips when nothing changed.
     */
    public static MachineWorkOrders reachStats(MachineWorkOrders orders, ToDoubleFunction<String> statValue) {
        LoadedWorkOrder active = orders.activeOrder();
        if (active == null) { return orders; }
        LoadedWorkOrder next = active;
        List<WorkPart> parts = active.order().objective().parts();
        for (int index = 0; index < parts.size(); index++) {
            WorkPart part = parts.get(index);
            if (!part.statGoal() || active.progress().get(index) >= part.target()) { continue; }
            long value = Math.max(0, Math.round(statValue.applyAsDouble(part.item()) * WorkPart.STAT_SCALE));
            next = next.withPartProgress(index, Math.min(part.target(), value));
        }
        return next.equals(active) ? orders : orders.withReplaced(orders.active(), next);
    }

    /**
     * A reward's amount scaled by Bounty: {@code base} times Bounty, rounded down, plus one more with a chance equal to the
     * rest. 100% pays the base amount; 150% of 3 pays 4, and 5 half the time. A reward always pays at least one.
     */
    public static int scaledAmount(int base, int bounty, RandomGenerator random) {
        long scaled = (long) Math.max(1, base) * Math.max(0, bounty);
        int amount = (int) (scaled / 100) + (random.nextInt(100) < scaled % 100 ? 1 : 0);
        return Math.max(1, amount);
    }
}
