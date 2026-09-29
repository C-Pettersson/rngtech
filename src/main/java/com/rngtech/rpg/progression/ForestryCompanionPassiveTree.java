package com.rngtech.rpg.progression;

import com.rngtech.rpg.MachineStatAccumulator;


public final class ForestryCompanionPassiveTree {
    public static final PassiveTreeDefinition<ForestryCompanionPassiveNode> DEFINITION =
            ForestryCompanionPassiveTreeDefinition.create();
    public static final PassiveTree<MegaPassiveNode> TREE = MegaPassiveTree.TREE;

    public static void applyStats(MachineStatAccumulator stats, MachineProgressionState progression) {
        MegaPassiveTree.applyStats(stats, progression, MachineMasteryFamily.FORESTRY);
    }

    public static boolean mutesMachineSound(MachineProgressionState progression) {
        return MegaPassiveTree.has(progression, "MUTE_MACHINE_SOUND");
    }

    public static boolean enablesMagnetMode(MachineProgressionState progression) {
        return MegaPassiveTree.has(progression, "MAGNET_MODE");
    }

    public static boolean processesLeavesWithoutShears(MachineProgressionState progression) {
        return MegaPassiveTree.has(progression, "SERRATED_LEAF_PROTOCOL");
    }

    public static boolean enablesManualThrottle(MachineProgressionState progression) {
        return MegaPassiveTree.has(progression, "MANUAL_THROTTLE");
    }

    public static boolean enablesCoastingClutch(MachineProgressionState progression) {
        return MegaPassiveTree.has(progression, "COASTING_CLUTCH");
    }

    public static boolean routesMagnetSaplings(MachineProgressionState progression) {
        return MegaPassiveTree.has(progression, "SEEDLING_MAGNET");
    }

    public static int managedCellBonus(MachineProgressionState progression) {
        return MegaPassiveTree.passive(progression, PassiveStatType.MANAGED_CELLS);
    }

    private ForestryCompanionPassiveTree() {
    }
}
