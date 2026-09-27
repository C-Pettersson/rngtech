package com.rngtech.rpg.progression;

import com.rngtech.rpg.MachineStatAccumulator;

import java.util.List;

public final class CrusherPassiveTree {
    public static final PassiveTree<CrusherPassiveNode> TREE = new PassiveTree<>(List.of(CrusherPassiveNode.values()));

    public static void applyStats(MachineStatAccumulator stats, MachineProgressionState progression) {
        TREE.applyStats(stats, progression);
    }

    public static boolean blocksBatteryCell(MachineProgressionState progression) {
        return CrusherPassiveNode.CELL_BYPASS.isUnlocked(progression);
    }

    public static boolean requiresMatchingCrushHeadStage(MachineProgressionState progression) {
        return CrusherPassiveNode.PRECISION_JAW_MOUNT.isUnlocked(progression);
    }

    public static boolean enablesDenseParallel(MachineProgressionState progression) {
        return CrusherPassiveNode.DENSE_BATCHING.isUnlocked(progression);
    }

    public static boolean mutesMachineSound(MachineProgressionState progression) {
        return TREE.hasUnlockedFlag(PassiveNodeFlag.MUTE_MACHINE_SOUND, progression);
    }

    public static int componentStageSupport(MachineProgressionState progression) {
        return TREE.statTotal(PassiveStatType.COMPONENT_STAGE_SUPPORT, progression);
    }

    private CrusherPassiveTree() {
    }
}
