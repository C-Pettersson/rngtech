package com.rngtech.content.machine;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum CrusherChassisMaterial implements StringRepresentable {
    WOODEN(0, 0.50, 0.25, 0.70, 1.25, 0.65, 0.85),
    IRON(1, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00),
    COPPER(2, 0.85, 1.50, 1.08, 0.95, 1.08, 1.00),
    BRONZE(3, 1.10, 1.10, 0.98, 1.05, 0.95, 1.10),
    STEEL(4, 1.60, 0.90, 1.08, 0.90, 0.90, 1.08),
    ALUMINUM(5, 0.95, 1.30, 1.12, 0.92, 1.20, 1.00),
    TITANIUM(6, 1.35, 1.20, 1.05, 1.05, 1.18, 1.12),
    TUNGSTENSTEEL(7, 3.00, 1.20, 1.10, 1.10, 0.90, 1.25),
    EXOTIC(8, 75.00, 2.00, 1.15, 0.90, 1.25, 1.20);

    private final int stage;
    private final double energyCapacity;
    private final double energyTransfer;
    private final double efficiency;
    private final double energyUsage;
    private final double processingSpeed;
    private final double outputAmount;
    private final String serializedName;

    CrusherChassisMaterial(
            int stage,
            double energyCapacity,
            double energyTransfer,
            double efficiency,
            double energyUsage,
            double processingSpeed,
            double outputAmount
    ) {
        this.stage = stage;
        this.energyCapacity = energyCapacity;
        this.energyTransfer = energyTransfer;
        this.efficiency = efficiency;
        this.energyUsage = energyUsage;
        this.processingSpeed = processingSpeed;
        this.outputAmount = outputAmount;
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public int stage() {
        return stage;
    }

    public double energyCapacity() {
        return energyCapacity;
    }

    public double energyTransfer() {
        return energyTransfer;
    }

    public double efficiency() {
        return efficiency;
    }

    public double energyUsage() {
        return energyUsage;
    }

    public double processingSpeed() {
        return processingSpeed;
    }

    public double outputAmount() {
        return outputAmount;
    }

    public String blockId() {
        return serializedName + "_crusher_chassis";
    }

    public String materialId() {
        return this == EXOTIC ? "naquadah" : serializedName;
    }

    public String translationKey() {
        return "rngtech.crusher_chassis_material." + serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
