package com.rngtech.rpg.progression;

public enum PassiveNodeKind {
    STARTER(10),
    TRAVEL(8),
    NODE(10),
    NOTABLE(14),
    KEYSTONE(18),
    /** An allocatable socket on the shared tree that holds one jewel and grants nothing of its own. */
    JEWEL_SOCKET(12);

    private final int size;

    PassiveNodeKind(int size) {
        this.size = size;
    }

    public int size() {
        return size;
    }
}
