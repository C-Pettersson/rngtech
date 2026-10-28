package com.rngtech.rpg.progression;

import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;

import java.util.List;

public interface MachineMasteryHost {
    MachineMasteryFamily masteryFamily();
    MachineProgressionState machineProgression();
    void setMachineProgression(MachineProgressionState state);
    /** The stage that gates a first Ascendancy Seal: the chassis stage, or an equivalent for hosts without one. */
    int ascendancyEntryStage();

    default MachineProgressionState masteryState() { return machineProgression().forFamily(masteryFamily()); }

    /** 1 plus the machine's increased Mastery XP gain; the stat has no base, so only its increased bucket counts. */
    static double xpGain(MachineStatAccumulator stats) {
        return Math.max(0.0, 1.0 + stats.increasedPercent(MachineStat.MASTERY_XP_GAIN) / 100.0);
    }
    default boolean masteryGearAllows(MachineProgressionState state) { return true; }
    /** The Mastery XP multiplier from Studious rolls on the machine and its Gear. */
    default double masteryXpGain() { return 1.0; }
    default void masteryChanged() { }

    default void grantMasteryXp(long amount, int quarters) {
        MachineProgressionState previous = masteryState();
        MachineProgressionState next = MegaPassiveTree.follow(previous.withAddedScaledXp(amount, quarters, masteryXpGain()), this::masteryGearAllows);
        setMachineProgression(next);
        if (!previous.allocatedNodes().equals(next.allocatedNodes())) { masteryChanged(); }
    }

    default void applyMasteryState(MachineProgressionState state) {
        setMachineProgression(MegaPassiveTree.follow(state.forFamily(masteryFamily()), this::masteryGearAllows));
        masteryChanged();
    }

    default boolean allocateMastery(MegaPassiveNode node) {
        return node != null && allocateMasteryPath(List.of(node));
    }

    /** Allocates every node in order, or nothing when any step lacks points, a connection, or legal Gear. */
    default boolean allocateMasteryPath(List<MegaPassiveNode> path) {
        if (path.isEmpty()) { return false; }
        MachineProgressionState next = masteryState();
        for (MegaPassiveNode node : path) {
            if (!MegaPassiveTree.TREE.canUnlock(node, next)) { return false; }
            next = next.withUnlockedNode(node.index());
            if (!masteryGearAllows(next)) { return false; }
        }
        applyMasteryState(next);
        return true;
    }

    /** Allocates ascendancy nodes in order until one lacks a point, a connection, or legal Gear, keeping the ones before it. */
    default int allocateAscendancyNodes(List<String> order) {
        MachineProgressionState next = masteryState();
        int allocated = 0;
        for (String id : order) {
            MachineProgressionState candidate = next.withAscendancyNode(id);
            if (candidate.ascendancyNodes().size() == next.ascendancyNodes().size() || !masteryGearAllows(candidate)) { break; }
            next = candidate;
            allocated++;
        }
        if (allocated > 0) { applyMasteryState(next); }
        return allocated;
    }
}
