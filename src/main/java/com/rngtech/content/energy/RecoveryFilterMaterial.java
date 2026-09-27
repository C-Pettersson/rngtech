package com.rngtech.content.energy;

import com.rngtech.rpg.ModifierSet;

import java.util.Locale;

public enum RecoveryFilterMaterial {
    IRON(1, 1.00, 1.00, 6),
    COPPER(2, 1.05, 1.00, 8),
    BRONZE(3, 1.10, 1.05, 10),
    STEEL(4, 1.15, 1.10, 12);

    private final int stage;
    private final double efficiency;
    private final double processingSpeed;
    private final int refinementPotential;
    private final String serializedName;

    RecoveryFilterMaterial(int stage, double efficiency, double processingSpeed, int refinementPotential) {
        this.stage = stage;
        this.efficiency = efficiency;
        this.processingSpeed = processingSpeed;
        this.refinementPotential = refinementPotential;
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public int stage() {
        return stage;
    }

    public double efficiency() {
        return efficiency;
    }

    public double processingSpeed() {
        return processingSpeed;
    }

    public int refinementPotential() {
        return refinementPotential;
    }

    public String itemId() {
        return serializedName + "_recovery_filter";
    }

    public String translationKey() {
        return "rngtech.recovery_filter_material." + serializedName;
    }

    public String getSerializedName() {
        return serializedName;
    }

    public ModifierSet modifierSet() {
        return com.rngtech.rpg.MachineImplicitCatalog.recoveryFilter(this).traits().modifierSet();
    }
}
