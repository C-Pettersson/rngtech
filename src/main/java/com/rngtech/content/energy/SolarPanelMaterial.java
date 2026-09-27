package com.rngtech.content.energy;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum SolarPanelMaterial implements StringRepresentable {
    CRUDE("crude", 1, 4, 1, 256, 32),
    COPPER("copper", 2, 8, 2, 512, 64),
    GOLD("gold", 3, 12, 3, 512, 96),
    SPARKSTEEL("sparksteel", 5, 24, 8, 1024, 192);

    private final String id;
    private final int stage;
    private final int clearGeneration;
    private final int rainGeneration;
    private final int energyCapacity;
    private final int transferRate;

    SolarPanelMaterial(
            String id,
            int stage,
            int clearGeneration,
            int rainGeneration,
            int energyCapacity,
            int transferRate
    ) {
        this.id = id;
        this.stage = stage;
        this.clearGeneration = clearGeneration;
        this.rainGeneration = rainGeneration;
        this.energyCapacity = energyCapacity;
        this.transferRate = transferRate;
    }

    public String blockId() {
        return id + "_solar_panel";
    }

    public String translationKey() {
        return "rngtech.solar_panel_material." + id;
    }

    public int stage() {
        return stage;
    }

    public int clearGeneration() {
        return clearGeneration;
    }

    public int rainGeneration() {
        return rainGeneration;
    }

    public int energyCapacity() {
        return energyCapacity;
    }

    public int transferRate() {
        return transferRate;
    }

    @Override
    public String getSerializedName() {
        return id.toLowerCase(Locale.ROOT);
    }
}
