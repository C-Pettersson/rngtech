package com.rngtech.content.machine;

import com.rngtech.rpg.ModifierSet;
import com.rngtech.rpg.Rarity;

import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.Locale;

public enum ServoMaterial implements StringRepresentable {
    STEEL(4, 14, 10, 15, 12, 10),
    TITANIUM(6, 18, 22, 24, 20, 16),
    TUNGSTENSTEEL(7, 20, 28, 30, 24, 20),
    NULLITE(7, 22, -50, 32, 28, 24),
    EXOTIC(8, 24, 35, 36, 30, 25);

    private final int stage;
    private final int refinementPotential;
    private final int processingSpeedPercent;
    private final int stabilityPercent;
    private final int temperatureStabilityPercent;
    private final int overheatTolerancePercent;
    private final String serializedName;

    ServoMaterial(
            int stage,
            int refinementPotential,
            int processingSpeedPercent,
            int stabilityPercent,
            int temperatureStabilityPercent,
            int overheatTolerancePercent
    ) {
        this.stage = stage;
        this.refinementPotential = refinementPotential;
        this.processingSpeedPercent = processingSpeedPercent;
        this.stabilityPercent = stabilityPercent;
        this.temperatureStabilityPercent = temperatureStabilityPercent;
        this.overheatTolerancePercent = overheatTolerancePercent;
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public int stage() {
        return stage;
    }

    public int refinementPotential() {
        return refinementPotential;
    }

    public int processingSpeedPercent() {
        return processingSpeedPercent;
    }

    public int stabilityPercent() {
        return stabilityPercent;
    }

    public int temperatureStabilityPercent() {
        return temperatureStabilityPercent;
    }

    public int overheatTolerancePercent() {
        return overheatTolerancePercent;
    }

    public ModifierSet modifierSet() {
        return new ModifierSet(Rarity.NORMAL, refinementPotential, List.of());
    }

    public String itemId() {
        return serializedName + "_servo";
    }

    public String translationKey() {
        return "rngtech.servo_material." + serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

}
