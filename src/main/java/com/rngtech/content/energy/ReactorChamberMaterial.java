package com.rngtech.content.energy;

import com.rngtech.rpg.ModifierSet;

import java.util.Locale;

public enum ReactorChamberMaterial {
    IRON(1, 0.10, 0.90, 8),
    COPPER(2, 0.20, 0.95, 10),
    BRONZE(3, 0.35, 1.00, 12),
    STEEL(4, 0.50, 1.10, 14);

    private final int stage;
    private final double energyGenerationBonus;
    private final double stability;
    private final int refinementPotential;
    private final String serializedName;

    ReactorChamberMaterial(int stage, double energyGenerationBonus, double stability, int refinementPotential) {
        this.stage = stage;
        this.energyGenerationBonus = energyGenerationBonus;
        this.stability = stability;
        this.refinementPotential = refinementPotential;
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public int stage() {
        return stage;
    }

    public double energyGenerationBonus() {
        return energyGenerationBonus;
    }

    public double stability() {
        return stability;
    }

    public int refinementPotential() {
        return refinementPotential;
    }

    public String itemId() {
        return serializedName + "_reactor_chamber";
    }

    public String translationKey() {
        return "rngtech.reactor_chamber_material." + serializedName;
    }

    public String getSerializedName() {
        return serializedName;
    }

    public ModifierSet modifierSet() {
        return com.rngtech.rpg.MachineImplicitCatalog.reactorChamber(this).traits().modifierSet();
    }
}
