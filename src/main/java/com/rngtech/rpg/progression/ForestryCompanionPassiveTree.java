package com.rngtech.rpg.progression;

import com.rngtech.rpg.MachineStatAccumulator;

import java.util.List;

public final class ForestryCompanionPassiveTree {
    public static final PassiveTreeDefinition<ForestryCompanionPassiveNode> DEFINITION =
            ForestryCompanionPassiveTreeDefinition.create();
    public static final PassiveTree<ForestryCompanionPassiveNode> TREE =
            new PassiveTree<>(List.of(ForestryCompanionPassiveNode.values()));

    public static void applyStats(MachineStatAccumulator stats, MachineProgressionState progression) {
        TREE.applyStats(stats, progression);
    }

    public static boolean mutesMachineSound(MachineProgressionState progression) {
        return TREE.hasUnlockedFlag(PassiveNodeFlag.MUTE_MACHINE_SOUND, progression);
    }

    public static boolean enablesMagnetMode(MachineProgressionState progression) {
        return ForestryCompanionPassiveNode.MAGNET_MODE.isUnlocked(progression);
    }

    public static boolean processesLeavesWithoutShears(MachineProgressionState progression) {
        return ForestryCompanionPassiveNode.SERRATED_LEAF_PROTOCOL.isUnlocked(progression);
    }

    public static boolean enablesManualThrottle(MachineProgressionState progression) {
        return ForestryCompanionPassiveNode.MANUAL_THROTTLE.isUnlocked(progression);
    }

    public static boolean enablesCoastingClutch(MachineProgressionState progression) {
        return ForestryCompanionPassiveNode.COASTING_CLUTCH.isUnlocked(progression);
    }

    public static boolean routesMagnetSaplings(MachineProgressionState progression) {
        return ForestryCompanionPassiveNode.SEEDLING_MAGNET.isUnlocked(progression);
    }

    public static int managedCellBonus(MachineProgressionState progression) {
        return TREE.statTotal(PassiveStatType.MANAGED_CELLS, progression);
    }

    private ForestryCompanionPassiveTree() {
    }
}
