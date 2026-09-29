package com.rngtech.rpg.progression;

public interface PassiveProgressionView {
    default String startNodeId() { return ""; }

    boolean hasNode(int index);

    int level();

    int unspentPoints();
}
