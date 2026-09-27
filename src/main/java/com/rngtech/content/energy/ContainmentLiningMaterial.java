package com.rngtech.content.energy;

import com.rngtech.rpg.ModifierSet;

import java.util.Locale;

public enum ContainmentLiningMaterial {
    IRON(1, 0.95, 6),
    COPPER(2, 1.00, 8),
    BRONZE(3, 1.10, 10),
    STEEL(4, 1.20, 12);

    private final int stage;
    private final double stability;
    private final int refinementPotential;
    private final String serializedName;

    ContainmentLiningMaterial(int stage, double stability, int refinementPotential) {
        this.stage = stage;
        this.stability = stability;
        this.refinementPotential = refinementPotential;
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public int stage() {
        return stage;
    }

    public double stability() {
        return stability;
    }

    public int refinementPotential() {
        return refinementPotential;
    }

    public String itemId() {
        return serializedName + "_containment_lining";
    }

    public String translationKey() {
        return "rngtech.containment_lining_material." + serializedName;
    }

    public String getSerializedName() {
        return serializedName;
    }

    public ModifierSet modifierSet() {
        return com.rngtech.rpg.MachineImplicitCatalog.containmentLining(this).traits().modifierSet();
    }
}
