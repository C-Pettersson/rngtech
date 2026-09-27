package com.rngtech.rpg;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum MachineChassisType implements StringRepresentable {
    FURNACE,
    CRUSHER,
    WASHER,
    SOLID_FUEL_BURNER;

    private final String serializedName;

    MachineChassisType() {
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "rngtech.machine_chassis_type." + serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
