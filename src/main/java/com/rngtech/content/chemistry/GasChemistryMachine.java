package com.rngtech.content.chemistry;

import com.rngtech.rpg.MachineType;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum GasChemistryMachine implements StringRepresentable {
    COAL_GASIFIER(MachineType.COAL_GASIFIER, 5),
    SYNGAS_COMBUSTOR(MachineType.SYNGAS_COMBUSTOR, 5),
    STEAM_METHANE_REFORMER(MachineType.STEAM_METHANE_REFORMER, 6);

    private final MachineType machineType;
    private final int stage;
    private final String serializedName;

    GasChemistryMachine(MachineType machineType, int stage) {
        this.machineType = machineType;
        this.stage = stage;
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public MachineType machineType() {
        return machineType;
    }

    public int stage() {
        return stage;
    }

    public String blockId() {
        return serializedName;
    }

    public String containerTranslationKey() {
        return "container.rngtech." + serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
