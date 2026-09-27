package com.rngtech.content.calibration;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum CalibrationGearMaterial implements StringRepresentable {
    IRON(1, 4, 6, 4, 3, 0, 8),
    COPPER(2, 6, 10, 3, 2, 0, 18),
    STEEL(4, 10, 12, 8, 7, 1, 10),
    TITANIUM(6, 15, 18, 16, 12, 2, 14),
    TUNGSTENSTEEL(7, 18, 16, 12, 18, 2, 24),
    NULLITE(7, 18, 20, 24, 16, 3, 16);

    private final int stage;
    private final int refinementPotential;
    private final int qualityPercent;
    private final int precisionPercent;
    private final int catalystEfficiencyPercent;
    private final int refinementPotentialBonus;
    private final int energyTransferPercent;
    private final String serializedName;

    CalibrationGearMaterial(
            int stage,
            int refinementPotential,
            int qualityPercent,
            int precisionPercent,
            int catalystEfficiencyPercent,
            int refinementPotentialBonus,
            int energyTransferPercent
    ) {
        this.stage = stage;
        this.refinementPotential = refinementPotential;
        this.qualityPercent = qualityPercent;
        this.precisionPercent = precisionPercent;
        this.catalystEfficiencyPercent = catalystEfficiencyPercent;
        this.refinementPotentialBonus = refinementPotentialBonus;
        this.energyTransferPercent = energyTransferPercent;
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public int stage() {
        return stage;
    }

    public int refinementPotential() {
        return refinementPotential;
    }

    public int qualityPercent() {
        return qualityPercent;
    }

    public int precisionPercent() {
        return precisionPercent;
    }

    public int catalystEfficiencyPercent() {
        return catalystEfficiencyPercent;
    }

    public int refinementPotentialBonus() {
        return refinementPotentialBonus;
    }

    public int energyTransferPercent() {
        return energyTransferPercent;
    }

    public String itemId(String suffix) {
        return serializedName + "_" + suffix;
    }

    public String translationKey() {
        return "rngtech.calibration_gear_material." + serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
