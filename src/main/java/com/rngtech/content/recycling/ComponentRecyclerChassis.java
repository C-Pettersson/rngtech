package com.rngtech.content.recycling;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum ComponentRecyclerChassis implements StringRepresentable {
    CRUDE(2, 0, 0, 1.0, 1.0, 1.0, 1.0, true, "crude_recycler"),
    IRON(1, 1800, 64, 0.92, 0.95, 1.08, 0.94),
    COPPER(2, 2200, 96, 1.08, 1.00, 1.04, 0.98),
    BRONZE(3, 3000, 96, 1.00, 1.06, 1.00, 1.04),
    STEEL(4, 4200, 128, 1.05, 1.12, 0.94, 1.12),
    ALUMINUM(5, 5200, 160, 1.20, 1.14, 0.92, 1.10),
    TITANIUM(6, 6600, 192, 1.32, 1.18, 0.88, 1.16),
    TUNGSTENSTEEL(7, 8400, 256, 1.18, 1.24, 0.86, 1.22),
    EXOTIC(8, 11000, 320, 1.45, 1.30, 0.82, 1.28);

    private final int stage;
    private final int energyCapacity;
    private final int energyTransfer;
    private final double processingSpeed;
    private final double efficiency;
    private final double energyUsage;
    private final double stability;
    private final boolean manual;
    private final String blockId;
    private final String serializedName;

    ComponentRecyclerChassis(
            int stage,
            int energyCapacity,
            int energyTransfer,
            double processingSpeed,
            double efficiency,
            double energyUsage,
            double stability
    ) {
        this(stage, energyCapacity, energyTransfer, processingSpeed, efficiency, energyUsage, stability, false, null);
    }

    ComponentRecyclerChassis(
            int stage,
            int energyCapacity,
            int energyTransfer,
            double processingSpeed,
            double efficiency,
            double energyUsage,
            double stability,
            boolean manual,
            String blockId
    ) {
        this.stage = stage;
        this.energyCapacity = energyCapacity;
        this.energyTransfer = energyTransfer;
        this.processingSpeed = processingSpeed;
        this.efficiency = efficiency;
        this.energyUsage = energyUsage;
        this.stability = stability;
        this.manual = manual;
        serializedName = name().toLowerCase(Locale.ROOT);
        this.blockId = blockId == null ? serializedName + "_component_recycler" : blockId;
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

    public boolean manual() {
        return manual;
    }

    public String blockId() {
        return blockId;
    }

    public String translationKey() {
        return "rngtech.component_recycler_chassis." + serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
