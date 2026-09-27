package com.rngtech.content.machine;

import com.rngtech.rpg.ModifierSet;
import com.rngtech.rpg.Rarity;

import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.Locale;

public enum FluidPumpMaterial implements StringRepresentable {
    OSMIUM(5, 16, 125),
    TITANIUM(6, 18, 250),
    TUNGSTENSTEEL(7, 20, 500),
    EXOTIC(8, 24, 1000);

    private final int stage;
    private final int refinementPotential;
    private final int transferRate;
    private final String serializedName;

    FluidPumpMaterial(int stage, int refinementPotential, int transferRate) {
        this.stage = stage;
        this.refinementPotential = refinementPotential;
        this.transferRate = transferRate;
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public int stage() {
        return stage;
    }

    public int transferRate() {
        return transferRate;
    }

    public int refinementPotential() {
        return refinementPotential;
    }

    public ModifierSet modifierSet() {
        return new ModifierSet(Rarity.NORMAL, refinementPotential, List.of());
    }

    public String itemId() {
        return serializedName + "_fluid_pump";
    }

    public String materialId() {
        return this == EXOTIC ? "naquadah" : serializedName;
    }

    public String translationKey() {
        return "rngtech.fluid_pump_material." + serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
