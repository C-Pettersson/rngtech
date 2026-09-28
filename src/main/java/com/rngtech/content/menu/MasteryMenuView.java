package com.rngtech.content.menu;

import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineMasteryHost;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.PassiveNode;

public interface MasteryMenuView<N extends PassiveNode> {
    MachineMasteryHost masteryHost();

    MachineProgressionState masterySnapshot();

    MachineMasteryFamily masteryFamily();

    double masteryAttribute(MachineStat stat);

    default boolean masterySupports(MachineStat stat) { return masteryFamily().supports(stat); }

    default boolean masterySupportsAbsolute(MachineStat stat) { return masterySupports(stat) && masteryFamily().supportsAbsolute(stat); }

    default boolean masterySupportsBehavior(String behavior) { return masteryFamily().supportsBehavior(behavior); }

    long machineXp();

    int machineLevel();

    int machineXpInLevel();

    int machineXpToNextLevel();

    float machineXpProgress();

    int unspentPassivePoints();

    boolean hasPassiveNode(PassiveNode node);

    boolean canUnlockPassiveNode(N node);

    boolean hasUnlockedPassiveConnection(PassiveNode node);
}
