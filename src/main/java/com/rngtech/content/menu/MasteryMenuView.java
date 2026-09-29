package com.rngtech.content.menu;

import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineMasteryHost;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.MasteryApplicability;
import com.rngtech.rpg.progression.PassiveNode;

public interface MasteryMenuView<N extends PassiveNode> extends MasteryApplicability {
    MachineMasteryHost masteryHost();

    MachineProgressionState masterySnapshot();

    @Override
    MachineMasteryFamily masteryFamily();

    double masteryAttribute(MachineStat stat);

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
