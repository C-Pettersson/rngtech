package com.rngtech.rpg.workorder;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
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
        List<WorkObjectiveEntry> eligible = entries.stream()
                .filter(entry -> WorkOrderFamilies.known(entry.family()))
                .filter(entry -> entry.filter() != WorkFilter.ITEM || !entry.options(stageCap(tier, entry), itemAvailable).isEmpty())
                .sorted(Comparator.comparing(entry -> entry.id().toString()))
                .toList();
        if (eligible.isEmpty()) { return null; }
        int total = eligible.stream().mapToInt(WorkObjectiveEntry::weight).sum();
        int pick = random.nextInt(total);
        WorkObjectiveEntry chosen = eligible.getLast();
        for (WorkObjectiveEntry entry : eligible) {
            pick -= entry.weight();
            if (pick < 0) { chosen = entry; break; }
        }
        int stage = stageCap(tier, chosen);
        WorkObjective objective = chosen.roll(tier, stage, random, itemAvailable);
        return new WorkOrder(chosen.family(), tier, objective, stage, 0, WorkOrder.BASE_BOUNTY + objective.bonus(), 0);
    }

    private static int stageCap(int tier, WorkObjectiveEntry entry) {
        return Math.min(WorkOrder.tierStage(tier), WorkOrderFamilies.topStage(entry.family()));
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
     * part's target into the next matching part. No active chip or no matching part changes nothing, and the same chips
     * come back. A fulfilled chip leaves the machine, and no other chip becomes active.
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
            if (current >= part.target() || !part.counts(report)) { continue; }
            long added = Math.min(left, part.target() - current);
            advanced = advanced.withPartProgress(index, current + added);
            left -= added;
        }
        if (advanced == active) { return new Progress(orders, null); }
        return new Progress(orders.withReplaced(orders.active(), advanced), advanced.fulfilled() ? advanced : null);
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
