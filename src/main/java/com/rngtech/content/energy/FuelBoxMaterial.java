package com.rngtech.content.energy;

import com.rngtech.rpg.ModifierSet;

import java.util.Locale;

public enum FuelBoxMaterial {
    IRON(1, 1, true, false, false, 0.95, 0.85, 8),
    COPPER(2, 1, true, false, true, 0.90, 0.90, 10),
    BRONZE(3, 2, true, false, true, 1.00, 1.00, 12),
    STEEL(4, 2, true, true, true, 1.10, 1.15, 14);

    private final int stage;
    private final int fuelSlots;
    private final boolean acceptsItemFuels;
    private final boolean acceptsBlockFuels;
    private final boolean fuelGovernor;
    private final double fuelEfficiency;
    private final double stability;
    private final int refinementPotential;
    private final String serializedName;

    FuelBoxMaterial(
            int stage,
            int fuelSlots,
            boolean acceptsItemFuels,
            boolean acceptsBlockFuels,
            boolean fuelGovernor,
            double fuelEfficiency,
            double stability,
            int refinementPotential
    ) {
        this.stage = stage;
        this.fuelSlots = fuelSlots;
        this.acceptsItemFuels = acceptsItemFuels;
        this.acceptsBlockFuels = acceptsBlockFuels;
        this.fuelGovernor = fuelGovernor;
        this.fuelEfficiency = fuelEfficiency;
        this.stability = stability;
        this.refinementPotential = refinementPotential;
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public int stage() {
        return stage;
    }

    public int fuelSlots() {
        return fuelSlots;
    }

    public boolean acceptsItemFuels() {
        return acceptsItemFuels;
    }

    public boolean acceptsBlockFuels() {
        return acceptsBlockFuels;
    }

    public boolean fuelGovernor() {
        return fuelGovernor;
    }

    public double fuelEfficiency() {
        return fuelEfficiency;
    }

    public double stability() {
        return stability;
    }

    public int refinementPotential() {
        return refinementPotential;
    }

    public String itemId() {
        return serializedName + "_fuel_box";
    }

    public String translationKey() {
        return "rngtech.fuel_box_material." + serializedName;
    }

    public String getSerializedName() {
        return serializedName;
    }

    public ModifierSet modifierSet() {
        return com.rngtech.rpg.MachineImplicitCatalog.fuelBox(this).traits().modifierSet();
    }
}
