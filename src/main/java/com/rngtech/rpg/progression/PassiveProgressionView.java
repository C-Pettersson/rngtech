package com.rngtech.rpg.progression;

public interface PassiveProgressionView {
    boolean hasNode(int index);

    int level();

    int unspentPoints();
}
