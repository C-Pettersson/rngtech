package com.rngtech.rpg.progression;

import com.rngtech.rpg.MachineStatAccumulator;


public final class FurnacePassiveTree {
    public static final PassiveTree<MegaPassiveNode> TREE = MegaPassiveTree.TREE;

    public static void applyStats(MachineStatAccumulator stats, MachineProgressionState progression) {
        MegaPassiveTree.applyStats(stats, progression, MachineMasteryFamily.FURNACE);
    }

    public static boolean mutesMachineSound(MachineProgressionState progression) {
        return MegaPassiveTree.has(progression, "MUTE_MACHINE_SOUND");
    }

    public static boolean usesQuenchProtocol(MachineProgressionState progression) {
        return MegaPassiveTree.has(progression, "QUENCH_PROTOCOL");
    }

    public static boolean usesClosedLoopRecuperator(MachineProgressionState progression) {
        return MegaPassiveTree.has(progression, "CLOSED_LOOP_RECUPERATOR");
    }

    private FurnacePassiveTree() {
    }
}
