package com.rngtech.rpg.unique;

import com.rngtech.content.calibration.CalibrationGearMaterial;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.machine.AlloyCrucibleMaterial;
import com.rngtech.content.machine.CrushHeadMaterial;
import com.rngtech.content.machine.FluidPumpMaterial;
import com.rngtech.content.machine.ServoMaterial;
import com.rngtech.rpg.MachinePartType;
import com.rngtech.rpg.MachineType;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.ToIntFunction;

/** The Gear a Unique can be: a machine part type with Unique support, or a Battery Cell. */
public enum UniqueHost {
    CRUSH_HEAD(MachinePartType.CRUSH_HEAD, MachineType.CRUSHER, CrushHeadMaterial.values(), CrushHeadMaterial::stage),
    HEAT_CORE(MachinePartType.HEAT_CORE, MachineType.SOLID_FUEL_BURNER, HeatCoreMaterial.values(), HeatCoreMaterial::stage),
    ALLOY_CRUCIBLE(MachinePartType.ALLOY_CRUCIBLE, MachineType.ALLOY_FURNACE, AlloyCrucibleMaterial.values(), AlloyCrucibleMaterial::stage),
    SERVO(MachinePartType.SERVO, MachineType.METAL_PRESS, ServoMaterial.values(), ServoMaterial::stage),
    FLUID_PUMP(MachinePartType.FLUID_PUMP, MachineType.MELTER, FluidPumpMaterial.values(), FluidPumpMaterial::stage),
    CONTROL_BOARD(MachinePartType.CONTROL_BOARD, MachineType.RESONANCE_CALIBRATOR, CalibrationGearMaterial.values(), CalibrationGearMaterial::stage),
    BATTERY_CELL(null, MachineType.BATTERY_CELL, normalCells(), BatteryCellMaterial::stage);

    private final MachinePartType partType;
    private final MachineType machineType;
    private final Enum<?>[] materials;
    private final ToIntFunction<Enum<?>> stageOf;
    private final int minStage;
    private final int maxStage;

    <M extends Enum<M>> UniqueHost(MachinePartType partType, MachineType machineType, M[] materials, ToIntFunction<M> stage) {
        this.partType = partType;
        this.machineType = machineType;
        this.materials = materials;
        stageOf = material -> stage.applyAsInt(materialClass(materials).cast(material));
        minStage = Arrays.stream(materials).mapToInt(stage).min().orElse(0);
        maxStage = Arrays.stream(materials).mapToInt(stage).max().orElse(0);
    }

    /** The part type, or null for a Battery Cell. */
    public MachinePartType partType() {
        return partType;
    }

    /** The machine type normal parts of this host are registered under. */
    public MachineType machineType() {
        return machineType;
    }

    public int minStage() {
        return minStage;
    }

    public int maxStage() {
        return maxStage;
    }

    /** Every normal material of this host, such as each Heat Core tier. */
    public List<Enum<?>> materials() {
        return List.of(materials);
    }

    public int stage(Enum<?> material) {
        return stageOf.applyAsInt(material);
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The normal material named {@code name}, such as {@code titanium} for a Heat Core, or null. */
    public Enum<?> material(String name) {
        return Arrays.stream(materials).filter(material -> material.name().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public static UniqueHost byName(String name) {
        return Arrays.stream(values()).filter(host -> host.serializedName().equals(name)).findFirst().orElse(null);
    }

    public static UniqueHost forPart(MachinePartType partType) {
        return Arrays.stream(values()).filter(host -> host.partType == partType).findFirst().orElse(null);
    }

    @SuppressWarnings("unchecked")
    private static <M extends Enum<M>> Class<M> materialClass(M[] materials) {
        return (Class<M>) materials.getClass().getComponentType();
    }

    private static BatteryCellMaterial[] normalCells() {
        return Arrays.stream(BatteryCellMaterial.values()).filter(material -> !material.unique()).toArray(BatteryCellMaterial[]::new);
    }
}
