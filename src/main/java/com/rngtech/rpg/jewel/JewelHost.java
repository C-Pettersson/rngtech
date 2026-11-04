package com.rngtech.rpg.jewel;

/** A machine that holds jewels: its Mastery tree's Jewel Sockets and the sockets on its body. */
public interface JewelHost {
    MachineJewels jewels();

    void setJewels(MachineJewels jewels);

    /** Body sockets on the machine itself; none until the body-socket affix or a Gold body gives it some. */
    default int bodySocketCount() {
        return 0;
    }
}
