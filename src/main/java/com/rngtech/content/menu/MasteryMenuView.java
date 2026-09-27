package com.rngtech.content.menu;

import com.rngtech.rpg.progression.PassiveNode;

public interface MasteryMenuView<N extends PassiveNode> {
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
