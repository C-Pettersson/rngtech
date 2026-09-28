package com.rngtech.rpg.progression;

import com.rngtech.rpg.MachineStatAccumulator;


public final class CrusherPassiveTree {
    public static final PassiveTree<MegaPassiveNode> TREE = MegaPassiveTree.TREE;

    public static void applyStats(MachineStatAccumulator stats, MachineProgressionState progression) {
        MegaPassiveTree.applyStats(stats, progression, MachineMasteryFamily.CRUSHER);
    }

    public static boolean blocksBatteryCell(MachineProgressionState progression) {
        return MegaPassiveTree.has(progression, "BLOCK_BATTERY");
    }

    public static boolean requiresMatchingCrushHeadStage(MachineProgressionState progression) {
        return MegaPassiveTree.has(progression, "MATCHING_HEAD");
    }

    public static boolean enablesDenseParallel(MachineProgressionState progression) {
        return MegaPassiveTree.has(progression, "DENSE_PARALLEL");
    }

    public static boolean mutesMachineSound(MachineProgressionState progression) {
        return MegaPassiveTree.has(progression, "MUTE_MACHINE_SOUND");
    }

    public static int componentStageSupport(MachineProgressionState progression) {
        return MegaPassiveTree.passive(progression, PassiveStatType.COMPONENT_STAGE_SUPPORT);
    }

    private CrusherPassiveTree() {
    }
}
