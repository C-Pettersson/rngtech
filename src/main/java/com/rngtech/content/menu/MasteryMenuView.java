package com.rngtech.content.menu;

import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineMasteryHost;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.MasteryApplicability;
import com.rngtech.rpg.progression.PassiveNode;

import java.util.List;

public interface MasteryMenuView<N extends PassiveNode> extends MasteryApplicability {
    MachineMasteryHost masteryHost();

    MachineProgressionState masterySnapshot();

    /** The synced stage that gates a first Ascendancy Seal. */
    int ascendancyEntryStage();

    /** Declared stats the chosen ascendancy grants, for the Stats tab. */
    List<MasteryMenuSupport.GrantedStat> ascendancyStats();

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
