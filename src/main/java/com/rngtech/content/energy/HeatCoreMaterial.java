package com.rngtech.content.energy;

import com.rngtech.rpg.ModifierSet;

import java.util.Locale;

public enum HeatCoreMaterial {
    IRON(1, 24, 0.85, 1, 0.85, 1.08, 200, 0.85, 8),
    COPPER(2, 40, 0.80, 2, 0.75, 1.16, 400, 0.90, 10),
    BRONZE(3, 64, 1.00, 3, 1.00, 1.24, 600, 0.95, 12),
    STEEL(4, 96, 1.15, 4, 1.20, 1.32, 800, 1.08, 14),
    ALUMINUM(5, 48, 0.70, 2, 0.60, 2.20, 250, 0.86, 16),
    SPARKSTEEL(5, 64, 1.25, 5, 1.28, 1.40, 1000, 1.12, 16),
    TITANIUM(6, 80, 1.35, 6, 1.35, 1.48, 1200, 1.18, 18),
    EXOTIC(8, 128, 1.60, 8, 1.60, 1.64, 1600, 1.30, 24);

    private final int stage;
    private final int energyGeneration;
    private final double fuelEfficiency;
    private final int maxFuelTier;
    private final double heatIsolation;
    private final double heatTransfer;
    private final int maxTemperatureBonus;
    private final double temperatureStability;
    private final int refinementPotential;
    private final String serializedName;

    HeatCoreMaterial(
            int stage,
            int energyGeneration,
            double fuelEfficiency,
            int maxFuelTier,
            double heatIsolation,
            double heatTransfer,
            int maxTemperatureBonus,
            double temperatureStability,
            int refinementPotential
    ) {
        this.stage = stage;
        this.energyGeneration = energyGeneration;
        this.fuelEfficiency = fuelEfficiency;
        this.maxFuelTier = maxFuelTier;
        this.heatIsolation = heatIsolation;
        this.heatTransfer = heatTransfer;
        this.maxTemperatureBonus = maxTemperatureBonus;
        this.temperatureStability = temperatureStability;
        this.refinementPotential = refinementPotential;
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public int stage() {
        return stage;
    }

    public int energyGeneration() {
        return energyGeneration;
    }

    public double fuelEfficiency() {
        return fuelEfficiency;
    }

    public int maxFuelTier() {
        return maxFuelTier;
    }

    public double heatIsolation() {
        return heatIsolation;
    }

    public double heatTransfer() {
        return heatTransfer;
    }

    public int maxTemperatureBonus() {
        return maxTemperatureBonus;
    }

    public double temperatureStability() {
        return temperatureStability;
    }

    public int refinementPotential() {
        return refinementPotential;
    }

    public String itemId() {
        return serializedName + "_heat_core";
    }

    public String translationKey() {
        return "rngtech.heat_core_material." + serializedName;
    }

    public String getSerializedName() {
        return serializedName;
    }

    public ModifierSet modifierSet() {
        return com.rngtech.rpg.MachineImplicitCatalog.heatCore(this).traits().modifierSet();
    }
}
