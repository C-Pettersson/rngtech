package com.rngtech.content.blockentity;

import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.MachineType;

import net.neoforged.neoforge.items.ItemStackHandler;

public interface RefinableMachine {
    MachineType refinementMachineType();

    MachineTraits machineTraits();

    void setMachineTraits(MachineTraits traits);

    default int refinementComponentStage() {
        return 0;
    }

    default int refinementModifierRollComponentStage() {
        return refinementComponentStage();
    }

    ItemStackHandler getRefinementInventory();
}
