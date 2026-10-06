package com.rngtech.content.calibration;

import com.rngtech.rpg.MachineBehavior;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum ResonanceCalibratorChassis implements StringRepresentable {
    IRON(1, 1, 2200, 96, 1.00, 1.00, 1.00, 1.00, 0, 0, 0),
    COPPER(2, 1, 2200, 160, 1.18, 1.00, 1.08, 0.92, 4, -4, 0),
    STEEL(4, 1, 3600, 128, 0.90, 1.08, 0.94, 1.14, 10, 4, 0, MachineBehavior.OUTPUT_GUARD),
    TITANIUM(6, 1, 4600, 176, 1.28, 1.12, 0.90, 1.18, 12, 14, 1, MachineBehavior.OUTPUT_GUARD),
    LEAD(4, 2, 5600, 96, 0.72, 0.92, 1.20, 0.90, 2, -6, 0),
    TUNGSTENSTEEL(7, 3, 9000, 280, 0.82, 0.96, 1.16, 1.08, 8, 2, 0, MachineBehavior.OUTPUT_GUARD),
    NULLITE(7, 1, 6200, 224, 1.65, 1.18, 0.86, 1.20, 16, 22, 2, MachineBehavior.OUTPUT_GUARD);

    private final int stage;
    private final int lanes;
    private final int energyCapacity;
    private final int energyTransfer;
    private final double processingSpeed;
    private final double efficiency;
    private final double energyUsage;
    private final double stability;
    private final int qualityPercent;
    private final int precisionPercent;
    private final int refinementPotentialBonus;
    private final MachineBehavior[] behaviors;
    private final String serializedName;

    ResonanceCalibratorChassis(
            int stage,
            int lanes,
            int energyCapacity,
            int energyTransfer,
            double processingSpeed,
            double efficiency,
            double energyUsage,
            double stability,
            int qualityPercent,
            int precisionPercent,
            int refinementPotentialBonus,
            MachineBehavior... behaviors
    ) {
        this.stage = stage;
        this.lanes = lanes;
        this.energyCapacity = energyCapacity;
        this.energyTransfer = energyTransfer;
        this.processingSpeed = processingSpeed;
        this.efficiency = efficiency;
        this.energyUsage = energyUsage;
        this.stability = stability;
        this.qualityPercent = qualityPercent;
        this.precisionPercent = precisionPercent;
        this.refinementPotentialBonus = refinementPotentialBonus;
        this.behaviors = behaviors;
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public int stage() {
        return stage;
    }

    public int lanes() {
        return lanes;
    }

    public int energyCapacity() {
        return energyCapacity;
    }

    public int energyTransfer() {
        return energyTransfer;
    }

    public double processingSpeed() {
        return processingSpeed;
    }

    public double efficiency() {
        return efficiency;
    }

    public double energyUsage() {
        return energyUsage;
    }

    public double stability() {
        return stability;
    }

    public int qualityPercent() {
        return qualityPercent;
    }

    public int precisionPercent() {
        return precisionPercent;
    }

    public int refinementPotentialBonus() {
        return refinementPotentialBonus;
    }

    public MachineBehavior[] behaviors() {
        return behaviors;
    }

    public String blockId() {
        return serializedName + "_resonance_calibrator_chassis";
    }

    public String translationKey() {
        return "rngtech.resonance_calibrator_chassis." + serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
