package com.rngtech.content.cable;

import com.rngtech.RNGTech;

import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum EnergyConnectorTier implements StringRepresentable {
    CRUDE("crude_energy_connector", 1, 64, false),
    BASIC("basic_energy_connector", 1, 128, false),
    COPPER("copper_energy_connector", 2, 512, false),
    GOLD("gold_energy_connector", 3, 2_048, false),
    SPARKSTEEL("sparksteel_energy_connector", 5, 8_192, false),
    ARCLITE("arclite_energy_connector", 6, 32_768, false),
    EXOTIC("exotic_energy_connector", 8, 1_000_000, false),
    DEBUG("debug_energy_connector", 0, 50_000_000, true);

    private final String itemId;
    private final int stage;
    private final int transferRate;
    private final boolean debugOnly;
    private final String serializedName;

    EnergyConnectorTier(String itemId, int stage, int transferRate, boolean debugOnly) {
        this.itemId = itemId;
        this.stage = stage;
        this.transferRate = transferRate;
        this.debugOnly = debugOnly;
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public String itemId() {
        return itemId;
    }

    public int stage() {
        return stage;
    }

    public int transferRate() {
        return transferRate;
    }

    public boolean debugOnly() {
        return debugOnly;
    }

    public boolean enabled() {
        return !debugOnly || RNGTech.isDebugContentEnabled();
    }

    public String translationKey() {
        return "rngtech.energy_connector_tier." + serializedName;
    }

    public static Optional<EnergyConnectorTier> bySerializedName(String name) {
        return Arrays.stream(values())
                .filter(tier -> tier.serializedName.equals(name))
                .findFirst();
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
