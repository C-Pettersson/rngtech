package com.rngtech.content.machine;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum AlloyFurnaceChassisMaterial implements StringRepresentable {
    BRONZE(3, 1100, 96, 1.06, 1.05, 1.02, 1.05),
    STEEL(4, 1500, 128, 1.12, 1.10, 0.96, 1.12),
    TITANIUM(6, 2200, 192, 1.22, 1.22, 0.88, 1.18);

    private final int stage;
    private final int energyCapacity;
    private final int energyTransfer;
    private final double heatTransfer;
    private final double processingSpeed;
    private final double energyUsage;
    private final double stability;
    private final String serializedName;

    AlloyFurnaceChassisMaterial(
            int stage,
            int energyCapacity,
            int energyTransfer,
            double heatTransfer,
            double processingSpeed,
            double energyUsage,
            double stability
    ) {
        this.stage = stage;
        this.energyCapacity = energyCapacity;
        this.energyTransfer = energyTransfer;
        this.heatTransfer = heatTransfer;
        this.processingSpeed = processingSpeed;
        this.energyUsage = energyUsage;
        this.stability = stability;
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public int stage() {
        return stage;
    }

    public int energyCapacity() {
        return energyCapacity;
    }

    public int energyTransfer() {
        return energyTransfer;
    }

    public double heatTransfer() {
        return heatTransfer;
    }

    public double processingSpeed() {
        return processingSpeed;
    }

    public double energyUsage() {
        return energyUsage;
    }

    public double stability() {
        return stability;
    }

    public String blockId() {
        return serializedName + "_alloy_furnace_chassis";
    }

    public String materialId() {
        return serializedName;
    }

    public String translationKey() {
        return "rngtech.alloy_furnace_chassis." + serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
