package com.rngtech.content.cable;

import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum EnergyDistributionMode implements StringRepresentable {
    ROUND_ROBIN,
    EVEN,
    FIRST_AVAILABLE;

    private final String serializedName;

    EnergyDistributionMode() {
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public EnergyDistributionMode next() {
        EnergyDistributionMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public String translationKey() {
        return "rngtech.energy_distribution." + serializedName;
    }

    public String descriptionKey() {
        return "rngtech.energy_distribution." + serializedName + ".description";
    }

    public static Optional<EnergyDistributionMode> bySerializedName(String name) {
        return Arrays.stream(values())
                .filter(mode -> mode.serializedName.equals(name))
                .findFirst();
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
