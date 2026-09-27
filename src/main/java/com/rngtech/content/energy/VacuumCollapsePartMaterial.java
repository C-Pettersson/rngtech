package com.rngtech.content.energy;

import com.rngtech.rpg.ModifierSet;

import java.util.Locale;

public enum VacuumCollapsePartMaterial {
    TUNGSTENSTEEL(7, 1.00, 1.00, 18),
    NULLITE(8, 1.25, 1.15, 22),
    EXOTIC(8, 1.60, 1.30, 26);

    private final int stage;
    private final double generation;
    private final double stability;
    private final int refinementPotential;
    private final String serializedName;

    VacuumCollapsePartMaterial(int stage, double generation, double stability, int refinementPotential) {
        this.stage = stage;
        this.generation = generation;
        this.stability = stability;
        this.refinementPotential = refinementPotential;
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public int stage() {
        return stage;
    }

    public double generation() {
        return generation;
    }

    public double stability() {
        return stability;
    }

    public int refinementPotential() {
        return refinementPotential;
    }

    public String itemId(String suffix) {
        return serializedName + "_" + suffix;
    }

    public String getSerializedName() {
        return serializedName;
    }

    public ModifierSet modifierSet(String identityId) {
        return com.rngtech.rpg.MachineImplicitCatalog.vacuumCollapsePart(identityId, this).traits().modifierSet();
    }
}
