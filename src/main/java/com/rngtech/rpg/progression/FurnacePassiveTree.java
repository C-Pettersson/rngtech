package com.rngtech.rpg.progression;

import com.rngtech.rpg.MachineStatAccumulator;

import java.util.List;

public final class FurnacePassiveTree {
    public static final PassiveTree<FurnacePassiveNode> TREE = new PassiveTree<>(List.of(FurnacePassiveNode.values()));

    public static void applyStats(MachineStatAccumulator stats, MachineProgressionState progression) {
        TREE.applyStats(stats, progression);
    }

    public static boolean mutesMachineSound(MachineProgressionState progression) {
        return TREE.hasUnlockedFlag(PassiveNodeFlag.MUTE_MACHINE_SOUND, progression);
    }

    public static boolean usesQuenchProtocol(MachineProgressionState progression) {
        return FurnacePassiveNode.QUENCH_PROTOCOL.isUnlocked(progression);
    }

    public static boolean usesClosedLoopRecuperator(MachineProgressionState progression) {
        return FurnacePassiveNode.CLOSED_LOOP_RECUPERATOR.isUnlocked(progression);
    }

    private FurnacePassiveTree() {
    }
}
