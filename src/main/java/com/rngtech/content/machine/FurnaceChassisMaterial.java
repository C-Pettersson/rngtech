package com.rngtech.content.machine;

import com.rngtech.rpg.MachineType;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum FurnaceChassisMaterial implements StringRepresentable {
    PRIMITIVE("furnace", 0, false, 0, 0, 0.75, 0.54, 0.85, 1.0, 1.0, 1, 1),
    IRON("iron_furnace_chassis", 1, true, 400, 48, 1.00, 1.00, 1.00, 1.15, 1.0, 1, 1),
    COPPER("copper_furnace_chassis", 2, true, 500, 72, 1.08, 1.05, 1.02, 1.05, 1.0, 1, 1),
    BRONZE("bronze_furnace_chassis", 3, true, 700, 64, 1.15, 1.08, 1.06, 1.00, 1.0, 1, 1),
    STEEL("steel_furnace_chassis", 4, true, 900, 96, 1.20, 1.12, 1.10, 0.92, 1.0, 1, 1),
    LEAD("lead_furnace_chassis", 4, true, 1100, 128, 1.05, 0.50, 0.95, 1.20, 1.0, 4, 4),
    ALUMINUM("aluminum_furnace_chassis", 5, true, 800, 128, 1.30, 1.18, 1.08, 0.90, 1.0, 1, 1),
    TITANIUM("titanium_furnace_chassis", 6, true, 1200, 144, 1.40, 1.22, 1.14, 0.86, 1.0, 1, 1),
    TUNGSTENSTEEL("tungstensteel_furnace_chassis", 7, true, 1600, 112, 1.55, 1.15, 1.22, 0.82, 1.0, 1, 1),
    EXOTIC("exotic_furnace_chassis", 8, true, 2000, 192, 1.75, 1.30, 1.25, 0.78, 1.0, 1, 1);

    private final String blockId;
    private final int stage;
    private final boolean electric;
    private final int energyCapacity;
    private final int energyTransfer;
    private final double heatTransfer;
    private final double processingSpeed;
    private final double efficiency;
    private final double energyUsage;
    private final double fuelEfficiency;
    private final int processingSlots;
    private final int heatCoreSlots;
    private final String serializedName;

    FurnaceChassisMaterial(
            String blockId,
            int stage,
            boolean electric,
            int energyCapacity,
            int energyTransfer,
            double heatTransfer,
            double processingSpeed,
            double efficiency,
            double energyUsage,
            double fuelEfficiency,
            int processingSlots,
            int heatCoreSlots
    ) {
        this.blockId = blockId;
        this.stage = stage;
        this.electric = electric;
        this.energyCapacity = energyCapacity;
        this.energyTransfer = energyTransfer;
        this.heatTransfer = heatTransfer;
        this.processingSpeed = processingSpeed;
        this.efficiency = efficiency;
        this.energyUsage = energyUsage;
        this.fuelEfficiency = fuelEfficiency;
        this.processingSlots = processingSlots;
        this.heatCoreSlots = heatCoreSlots;
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public String blockId() {
        return blockId;
    }

    public int stage() {
        return stage;
    }

    public boolean electric() {
        return electric;
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

    public double efficiency() {
        return efficiency;
    }

    public double energyUsage() {
        return energyUsage;
    }

    public double fuelEfficiency() {
        return fuelEfficiency;
    }

    public int processingSlots() {
        return processingSlots;
    }

    public int heatCoreSlots() {
        return heatCoreSlots;
    }

    public boolean usesHeatCores() {
        return electric && heatCoreSlots > 0;
    }

    public MachineType machineType() {
        return electric ? MachineType.ELECTRIC_FURNACE : MachineType.FURNACE;
    }

    public String materialId() {
        return this == EXOTIC ? "naquadah" : serializedName;
    }

    public String translationKey() {
        return "rngtech.furnace_chassis_material." + serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
