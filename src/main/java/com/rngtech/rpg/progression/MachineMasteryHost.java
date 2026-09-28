package com.rngtech.rpg.progression;

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
        MachineProgressionState state = masteryState();
        if (!MegaPassiveTree.TREE.canUnlock(node, state)) { return false; }
        MachineProgressionState next = state.withUnlockedNode(node.index());
        if (!masteryGearAllows(next)) { return false; }
        applyMasteryState(next);
        return true;
    }
}
