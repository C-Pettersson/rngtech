package com.rngtech.content.energy;

import com.rngtech.rpg.ModifierSet;

import java.util.Locale;

/** Fuel Box slot parts that burn a fluid. The form belongs to the material and never rolls. */
public enum FuelTankMaterial {
    BRONZE("bronze_fuel_tank", 3, FluidFuelForm.LIQUID, 4_000, 1.00, 1.00, 12),
    STEEL("steel_fuel_tank", 4, FluidFuelForm.LIQUID, 8_000, 1.10, 1.15, 14),
    GAS_JET("steel_gas_jet", 4, FluidFuelForm.GAS, 4_000, 1.10, 1.15, 14);

    private final String itemId;
    private final int stage;
    private final FluidFuelForm form;
    private final int capacity;
    private final double fuelEfficiency;
    private final double stability;
    private final int refinementPotential;
    private final String serializedName;

    FuelTankMaterial(
            String itemId,
            int stage,
            FluidFuelForm form,
            int capacity,
            double fuelEfficiency,
            double stability,
            int refinementPotential
    ) {
        this.itemId = itemId;
        this.stage = stage;
        this.form = form;
        this.capacity = capacity;
        this.fuelEfficiency = fuelEfficiency;
        this.stability = stability;
        this.refinementPotential = refinementPotential;
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public String itemId() {
        return itemId;
    }

    public int stage() {
        return stage;
    }

    public FluidFuelForm form() {
        return form;
    }

    public int capacity() {
        return capacity;
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

    public String getSerializedName() {
        return serializedName;
    }

    public ModifierSet modifierSet() {
        return com.rngtech.rpg.MachineImplicitCatalog.fuelTank(this).traits().modifierSet();
    }
}
