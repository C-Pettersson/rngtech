package com.rngtech.rpg.progression;

import java.util.List;

public interface MachineMasteryHost {
    MachineMasteryFamily masteryFamily();
    MachineProgressionState machineProgression();
    void setMachineProgression(MachineProgressionState state);

    default MachineProgressionState masteryState() { return machineProgression().forFamily(masteryFamily()); }
    default boolean masteryGearAllows(MachineProgressionState state) { return true; }
    default void masteryChanged() { }

    default void grantMasteryXp(long amount, int quarters) {
        MachineProgressionState previous = masteryState();
        MachineProgressionState next = MegaPassiveTree.follow(previous.withAddedScaledXp(amount, quarters), this::masteryGearAllows);
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
}
