package com.rngtech.content.machine;

import com.rngtech.rpg.ModifierSet;
import com.rngtech.rpg.Rarity;

import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.Locale;

public enum AlloyCrucibleMaterial implements StringRepresentable {
    BRONZE(3, 3, 1.05, 1.00, 1.05, 1.05, 12),
    STEEL(4, 3, 1.12, 1.08, 1.12, 1.10, 14),
    TITANIUM(6, 4, 1.20, 1.15, 1.22, 1.18, 18);

    private final int stage;
    private final int inputSlots;
    private final double stability;
    private final double temperatureStability;
    private final double heatTransfer;
    private final double processingSpeed;
    private final int refinementPotential;

    AlloyCrucibleMaterial(
            int stage,
            int inputSlots,
            double stability,
            double temperatureStability,
            double heatTransfer,
            double processingSpeed,
            int refinementPotential
    ) {
        this.stage = stage;
        this.inputSlots = inputSlots;
        this.stability = stability;
        this.temperatureStability = temperatureStability;
        this.heatTransfer = heatTransfer;
        this.processingSpeed = processingSpeed;
        this.refinementPotential = refinementPotential;
    }

    public int stage() {
        return stage;
    }

    public double stability() {
        return stability;
    }

    public double temperatureStability() {
        return temperatureStability;
    }

    public double heatTransfer() {
        return heatTransfer;
    }

    public double processingSpeed() {
        return processingSpeed;
    }

    public int inputSlots() {
        return inputSlots;
    }

    public int refinementPotential() {
        return refinementPotential;
    }

    public String itemId() {
        return getSerializedName() + "_alloy_crucible";
    }

    public String translationKey() {
        return "rngtech.alloy_crucible." + getSerializedName();
    }

    public ModifierSet modifierSet() {
        return new ModifierSet(Rarity.NORMAL, refinementPotential, List.of());
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
