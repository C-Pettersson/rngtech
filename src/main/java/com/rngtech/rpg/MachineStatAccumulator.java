package com.rngtech.rpg;

import com.rngtech.RNGTechConfig;
import com.rngtech.content.calibration.ResonanceCalibratorChassis;
import com.rngtech.content.energy.BatteryChassisMaterial;
import com.rngtech.content.energy.SolarPanelMaterial;
import com.rngtech.content.energy.SolidFuelBurnerChassis;
import com.rngtech.content.machine.AlloyFurnaceChassisMaterial;
import com.rngtech.content.machine.CompressorTankMaterial;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.machine.FurnaceChassisMaterial;
import com.rngtech.content.recycling.ComponentRecyclerChassis;

import net.minecraft.util.RandomSource;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class MachineStatAccumulator {
    public static final double FURNACE_BASE_MAX_TEMPERATURE = 600.0;
    public static final double PRIMITIVE_FURNACE_MAX_TEMPERATURE = 800.0;

    private final Map<MachineStat, Double> baseValues = new EnumMap<>(MachineStat.class);
    private final Map<MachineStat, Double> additiveValues = new EnumMap<>(MachineStat.class);
    private final Map<MachineStat, Double> increasedPercentValues = new EnumMap<>(MachineStat.class);
    private final Map<MachineStat, Double> moreValues = new EnumMap<>(MachineStat.class);
    private double flatEnergyGenerationBonus;
    private final Map<MachineStat, Double> absoluteValues = new EnumMap<>(MachineStat.class);
    private final Map<MachineStat, Double> absoluteCeilings = new EnumMap<>(MachineStat.class);

    public void setAbsolute(MachineStat stat, double value) {
        absoluteValues.merge(accumulationStat(stat), value, Math::min);
    }

    public void capAbsolute(MachineStat stat, double value) {
        absoluteCeilings.merge(accumulationStat(stat), value, Math::min);
    }

    public static MachineStatAccumulator fromRanges(List<MachineStatRange> ranges, RandomSource random) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        stats.rollBaseValues(ranges, random);
        return stats;
    }

    public static MachineStatAccumulator componentBase(Map<MachineStat, Double> values) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        stats.baseValues.putAll(values);
        return stats;
    }

    public static MachineStatAccumulator crusherBase(CrusherChassisMaterial chassis) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        stats.baseValues.put(MachineStat.INPUT_SLOTS, (double) RNGTechConfig.CRUSHER_BASE_INPUT_SLOTS.get());
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.PROCESSING_LEVEL, 0.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, RNGTechConfig.CRUSHER_ENERGY_CAPACITY.get() * chassis.energyCapacity());
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, RNGTechConfig.CRUSHER_MAX_ENERGY_INPUT.get() * chassis.energyTransfer());
        stats.baseValues.put(MachineStat.EFFICIENCY, chassis.efficiency());
        stats.baseValues.put(MachineStat.ENERGY_USAGE, chassis.energyUsage());
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, chassis.processingSpeed());
        stats.baseValues.put(MachineStat.OUTPUT_AMOUNT, chassis.outputAmount());
        stats.baseValues.put(
                MachineStat.PARALLEL_JOBS,
                switch (chassis) {
                    case TUNGSTENSTEEL -> 4.0;
                    case EXOTIC -> 9.0;
                    default -> 1.0;
                }
        );
        return stats;
    }

    public static MachineStatAccumulator solidFuelBurnerBase(int transferRate, double stability) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 0.0);
        stats.baseValues.put(MachineStat.ENERGY_GENERATION, 0.0);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.STABILITY, stability);
        stats.baseValues.put(MachineStat.FUEL_EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.FUEL_DURATION, 1.0);
        stats.baseValues.put(MachineStat.HEAT_ISOLATION, 1.0);
        return stats;
    }

    public static MachineStatAccumulator solidFuelBurnerBase(SolidFuelBurnerChassis chassis) {
        return solidFuelBurnerBase(chassis.transferRate(), chassis.stability());
    }

    public static MachineStatAccumulator bioGeneratorBase(int energyCapacity, int transferRate) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) energyCapacity);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.ENERGY_GENERATION, 1.0);
        stats.baseValues.put(MachineStat.FUEL_EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.POTATO_POWER, 1.0);
        stats.baseValues.put(MachineStat.CARROT_POWER, 1.0);
        stats.baseValues.put(MachineStat.BREAD_POWER, 1.0);
        stats.baseValues.put(MachineStat.SAPLING_POWER, 1.0);
        stats.baseValues.put(MachineStat.SEED_POWER, 1.0);
        stats.baseValues.put(MachineStat.PLANT_POWER, 1.0);
        stats.baseValues.put(MachineStat.ORGANIC_REAGENT_POWER, 1.0);
        stats.baseValues.put(MachineStat.COMPOSTED_BIOMASS_POWER, 1.0);
        stats.baseValues.put(MachineStat.ALGAE_POWER, 1.0);
        stats.baseValues.put(MachineStat.RICH_BIOMASS_POWER, 1.0);
        stats.baseValues.put(MachineStat.FUEL_DURATION, 1.0);
        return stats;
    }

    public static MachineStatAccumulator bioOrganicProcessorBase() {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 2.0);
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.FUEL_EFFICIENCY, 1.0);
        return stats;
    }

    public static MachineStatAccumulator solarPanelBase(SolarPanelMaterial material) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) material.energyCapacity());
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) material.transferRate());
        stats.baseValues.put(MachineStat.ENERGY_GENERATION, (double) material.clearGeneration());
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.PEAK_SOLAR_GENERATION, 1.0);
        return stats;
    }

    public static MachineStatAccumulator solarArrayControllerBase(int energyCapacity, int transferRate) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) energyCapacity);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.ENERGY_GENERATION, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.STABILITY, 1.0);
        stats.baseValues.put(MachineStat.BATTERY_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.SOLAR_PANEL_LIMIT, 1.0);
        stats.baseValues.put(MachineStat.MOONLIGHT_CONVERSION, 0.0);
        stats.baseValues.put(MachineStat.WEATHER_RECOVERY, 0.0);
        stats.baseValues.put(MachineStat.SOLAR_PANEL_SYNCHRONIZATION, 0.0);
        stats.baseValues.put(MachineStat.SOLAR_PANEL_ARBITRATION, 0.0);
        stats.baseValues.put(MachineStat.OVERFLOW_SHUNTING, 0.0);
        stats.baseValues.put(MachineStat.CLEAR_SKY_AMPLIFICATION, 0.0);
        stats.baseValues.put(MachineStat.LUNAR_INVERSION, 0.0);
        return stats;
    }

    public static MachineStatAccumulator potentialReactorBase(int energyCapacity, int transferRate) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.PROCESSING_LEVEL, 0.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) energyCapacity);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.ENERGY_GENERATION, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, 1.0);
        stats.baseValues.put(MachineStat.STABILITY, 1.0);
        return stats;
    }

    public static MachineStatAccumulator corrosionCellBase(int energyCapacity, int transferRate) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 2.0);
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) energyCapacity);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.FLUID_TRANSFER, 0.0);
        stats.baseValues.put(MachineStat.ENERGY_GENERATION, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, 1.0);
        stats.baseValues.put(MachineStat.STABILITY, 1.0);
        return stats;
    }

    public static MachineStatAccumulator ammoniaSynthesizerBase(int energyCapacity, int transferRate) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 0.0);
        stats.baseValues.put(MachineStat.PROCESSING_LEVEL, 0.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) energyCapacity);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.ENERGY_USAGE, 1.0);
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.STABILITY, 1.0);
        stats.baseValues.put(MachineStat.FLUID_TRANSFER, 1000.0);
        return stats;
    }

    public static MachineStatAccumulator ammoniaFuelCellBase(int energyCapacity, int transferRate) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.PROCESSING_LEVEL, 0.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) energyCapacity);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.ENERGY_GENERATION, 1.0);
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.STABILITY, 1.0);
        stats.baseValues.put(MachineStat.FLUID_TRANSFER, 1000.0);
        return stats;
    }

    public static MachineStatAccumulator generatorProcessingBase(long energyCapacity, int transferRate) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) energyCapacity);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.ENERGY_GENERATION, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, 1.0);
        stats.baseValues.put(MachineStat.STABILITY, 1.0);
        return stats;
    }

    public static MachineStatAccumulator vacuumCollapseGeneratorBase(long energyCapacity, int transferRate) {
        MachineStatAccumulator stats = generatorProcessingBase(energyCapacity, transferRate);
        stats.baseValues.put(MachineStat.PROCESSING_LEVEL, 0.0);
        return stats;
    }

    public static MachineStatAccumulator componentRecyclerBase(ComponentRecyclerChassis chassis) {
        MachineStatAccumulator stats = componentRecyclerBase(chassis.energyCapacity(), chassis.energyTransfer());
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, chassis.processingSpeed());
        stats.baseValues.put(MachineStat.ENERGY_USAGE, chassis.energyUsage());
        stats.baseValues.put(MachineStat.EFFICIENCY, chassis.efficiency());
        stats.baseValues.put(MachineStat.STABILITY, chassis.stability());
        if (chassis.manual()) {
            stats.baseValues.put(MachineStat.PROCESSING_LEVEL, (double) chassis.stage());
            stats.baseValues.put(MachineStat.ENERGY_CAPACITY, 0.0);
            stats.baseValues.put(MachineStat.ENERGY_TRANSFER, 0.0);
        }
        return stats;
    }

    public static MachineStatAccumulator componentRecyclerBase(int energyCapacity, int transferRate) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 3.0);
        stats.baseValues.put(MachineStat.PROCESSING_LEVEL, 0.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) energyCapacity);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.ENERGY_USAGE, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, 1.0);
        stats.baseValues.put(MachineStat.STABILITY, 1.0);
        return stats;
    }

    public static MachineStatAccumulator metalPressBase(int energyCapacity, int transferRate) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        applyBaseFurnaceValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) energyCapacity);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_USAGE, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.STABILITY, 1.0);
        stats.baseValues.put(MachineStat.MAX_TEMPERATURE, 0.0);
        stats.baseValues.put(MachineStat.TEMPERATURE_STABILITY, 1.0);
        stats.baseValues.put(MachineStat.OVERHEAT_TOLERANCE, 1.0);
        return stats;
    }

    public static MachineStatAccumulator alloyFurnaceBase(AlloyFurnaceChassisMaterial chassis) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        applyBaseFurnaceValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 0.0);
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) chassis.energyCapacity());
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) chassis.energyTransfer());
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, chassis.processingSpeed());
        stats.baseValues.put(MachineStat.ENERGY_USAGE, chassis.energyUsage());
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.HEAT_TRANSFER, chassis.heatTransfer());
        stats.baseValues.put(MachineStat.MAX_TEMPERATURE, 300.0);
        stats.baseValues.put(MachineStat.TEMPERATURE_STABILITY, 1.0);
        stats.baseValues.put(MachineStat.STABILITY, chassis.stability());
        return stats;
    }

    public static MachineStatAccumulator melterBase(int energyCapacity, int transferRate, int fluidTransferRate) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        applyBaseFurnaceValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 2.0);
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 0.0);
        stats.baseValues.put(MachineStat.PROCESSING_LEVEL, 0.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) energyCapacity);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.FLUID_TRANSFER, (double) fluidTransferRate);
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_USAGE, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.MAX_TEMPERATURE, 0.0);
        stats.baseValues.put(MachineStat.TEMPERATURE_STABILITY, 1.0);
        return stats;
    }

    public static MachineStatAccumulator batteryAssemblerBase(int energyCapacity, int transferRate, int fluidTransferRate) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 4.0);
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 4.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) energyCapacity);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.FLUID_TRANSFER, (double) fluidTransferRate);
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_USAGE, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.STABILITY, 1.0);
        return stats;
    }

    public static MachineStatAccumulator gasChemistryProcessorBase(int energyCapacity, int transferRate, int fluidTransferRate) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        applyBaseFurnaceValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.PROCESSING_LEVEL, 0.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) energyCapacity);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.FLUID_TRANSFER, (double) fluidTransferRate);
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_USAGE, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.MAX_TEMPERATURE, 0.0);
        stats.baseValues.put(MachineStat.TEMPERATURE_STABILITY, 1.0);
        stats.baseValues.put(MachineStat.STABILITY, 1.0);
        return stats;
    }

    public static MachineStatAccumulator gasCombustorBase(int energyCapacity, int transferRate, int fluidTransferRate) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 0.0);
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 0.0);
        stats.baseValues.put(MachineStat.PROCESSING_LEVEL, 0.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) energyCapacity);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.FLUID_TRANSFER, (double) fluidTransferRate);
        stats.baseValues.put(MachineStat.ENERGY_GENERATION, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, 1.0);
        stats.baseValues.put(MachineStat.STABILITY, 1.0);
        return stats;
    }

    public static MachineStatAccumulator fluidHeatGeneratorBase(int energyCapacity, int transferRate) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        applyBaseFurnaceValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 0.0);
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 0.0);
        stats.baseValues.put(MachineStat.PROCESSING_LEVEL, 0.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) energyCapacity);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.ENERGY_GENERATION, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, 1.0);
        stats.baseValues.put(MachineStat.MAX_TEMPERATURE, 0.0);
        stats.baseValues.put(MachineStat.TEMPERATURE_STABILITY, 1.0);
        stats.baseValues.put(MachineStat.STABILITY, 1.0);
        stats.baseValues.put(MachineStat.FLUID_TRANSFER, 1000.0);
        return stats;
    }

    public static MachineStatAccumulator compressorTankBase(CompressorTankMaterial material) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 0.0);
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 0.0);
        stats.baseValues.put(MachineStat.ADDON_SLOTS, 5.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) material.energyCapacity());
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) material.energyTransfer());
        stats.baseValues.put(MachineStat.ENERGY_USAGE, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, 1.0);
        stats.baseValues.put(MachineStat.STABILITY, 1.0);
        stats.baseValues.put(MachineStat.FLUID_TRANSFER, (double) material.baseRate());
        stats.baseValues.put(
                MachineStat.FLUID_CAPACITY,
                (material.looseBuckets() + material.compressedPhysicalBuckets() * material.compressionRatio())
                        * 1000.0
        );
        stats.baseValues.put(MachineStat.COMPRESSION_RATIO, (double) material.compressionRatio());
        return stats;
    }

    public static MachineStatAccumulator resonanceCalibratorBase(int energyCapacity, int transferRate) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 4.0);
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) energyCapacity);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_USAGE, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.STABILITY, 1.0);
        stats.baseValues.put(MachineStat.CALIBRATION_QUALITY, 1.0);
        stats.baseValues.put(MachineStat.CALIBRATION_PRECISION, 1.0);
        stats.baseValues.put(MachineStat.CATALYST_EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.REFINEMENT_POTENTIAL_BONUS, 0.0);
        return stats;
    }

    public static MachineStatAccumulator resonanceCalibratorBase(ResonanceCalibratorChassis chassis) {
        MachineStatAccumulator stats = resonanceCalibratorBase(chassis.energyCapacity(), chassis.energyTransfer());
        stats.baseValues.put(MachineStat.PARALLEL_JOBS, (double) chassis.lanes());
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, chassis.processingSpeed());
        stats.baseValues.put(MachineStat.EFFICIENCY, chassis.efficiency());
        stats.baseValues.put(MachineStat.ENERGY_USAGE, chassis.energyUsage());
        stats.baseValues.put(MachineStat.STABILITY, chassis.stability());
        stats.baseValues.put(MachineStat.CALIBRATION_QUALITY, 1.0 + chassis.qualityPercent() / 100.0);
        stats.baseValues.put(MachineStat.CALIBRATION_PRECISION, 1.0 + chassis.precisionPercent() / 100.0);
        stats.baseValues.put(MachineStat.REFINEMENT_POTENTIAL_BONUS, (double) chassis.refinementPotentialBonus());
        return stats;
    }

    public static MachineStatAccumulator furnaceBase() {
        return furnaceBase(FurnaceChassisMaterial.PRIMITIVE);
    }

    public static MachineStatAccumulator furnaceBase(FurnaceChassisMaterial chassis) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        applyBaseFurnaceValues(stats);
        stats.baseValues.put(
                MachineStat.INPUT_SLOTS,
                (double) Math.max(RNGTechConfig.FURNACE_BASE_INPUT_SLOTS.get(), chassis.processingSlots())
        );
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, (double) chassis.processingSlots());
        stats.baseValues.put(MachineStat.ADDON_SLOTS, (double) chassis.heatCoreSlots());
        stats.baseValues.put(MachineStat.HEAT_TRANSFER, chassis.heatTransfer());
        stats.baseValues.put(
                MachineStat.PROCESSING_SPEED,
                chassis == FurnaceChassisMaterial.BRONZE ? 1.0 : chassis.processingSpeed()
        );
        stats.baseValues.put(MachineStat.EFFICIENCY, chassis.electric() ? 1.0 : chassis.efficiency());
        stats.baseValues.put(MachineStat.FUEL_EFFICIENCY, chassis.fuelEfficiency());
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, chassis.electric() ? (double) chassis.energyCapacity() : 0.0);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, chassis.electric() ? (double) chassis.energyTransfer() : 0.0);
        stats.baseValues.put(MachineStat.ENERGY_USAGE, chassis.energyUsage());
        if (chassis == FurnaceChassisMaterial.PRIMITIVE) {
            stats.baseValues.put(MachineStat.MAX_TEMPERATURE, PRIMITIVE_FURNACE_MAX_TEMPERATURE);
        }
        return stats;
    }

    public static MachineStatAccumulator batteryBase() {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        return stats;
    }

    public static MachineStatAccumulator batteryChassisBase(BatteryChassisMaterial material) {
        return batteryChassisBase(
                material.slots(),
                material.transferRate(),
                material.burstTransfer(),
                material.burstDuration(),
                material.efficiency(),
                material.stability(),
                material.idleLossPercentPerMinute(),
                material.globalModifierStrength()
        );
    }

    public static MachineStatAccumulator batteryCellBase() {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, 0.0);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.IDLE_LOSS, 0.0);
        return stats;
    }

    public static MachineStatAccumulator minersCompanionBase() {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.BLOCK_FILTER_SLOTS, 3.0);
        stats.baseValues.put(MachineStat.ENERGY_USAGE, 1.0);
        return stats;
    }

    public static MachineStatAccumulator forestryCompanionBase() {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_USAGE, 1.0);
        return stats;
    }

    public static MachineStatAccumulator batteryChassisBase(
            int slots,
            int transferRate,
            double burstTransfer,
            int burstDuration,
            double efficiency,
            double stability,
            double idleLossPercentPerMinute,
            double globalModifierStrength
    ) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.BATTERY_SLOTS, (double) slots);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.BURST_TRANSFER, burstTransfer);
        stats.baseValues.put(MachineStat.BURST_DURATION, (double) burstDuration);
        stats.baseValues.put(MachineStat.EFFICIENCY, efficiency);
        stats.baseValues.put(MachineStat.STABILITY, stability);
        stats.baseValues.put(MachineStat.IDLE_LOSS, idleLossPercentPerMinute);
        stats.baseValues.put(MachineStat.GLOBAL_MODIFIER_STRENGTH, globalModifierStrength);
        return stats;
    }

    private static void applyBaseMachineValues(MachineStatAccumulator stats) {
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.ADDON_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.OUTPUT_AMOUNT, 1.0);
        stats.baseValues.put(MachineStat.SUPER_OUTPUT_CHANCE, 0.0);
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, 1.0);
        stats.baseValues.put(MachineStat.INSTANT_PROCESS_CHANCE, 0.0);
        stats.baseValues.put(MachineStat.PROCESSING_LEVEL, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_USAGE, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_GENERATION, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.PARALLEL_JOBS, 1.0);
        stats.baseValues.put(MachineStat.BUFFER_SIZE, 1.0);
        stats.baseValues.put(MachineStat.STABILITY, 1.0);
        stats.baseValues.put(MachineStat.UPGRADE_LIMIT, 1.0);
        stats.baseValues.put(MachineStat.REFINEMENT_POTENTIAL, 0.0);
        stats.baseValues.put(MachineStat.BURST_TRANSFER, 1.0);
        stats.baseValues.put(MachineStat.BURST_DURATION, 0.0);
        stats.baseValues.put(MachineStat.IDLE_LOSS, 0.0);
        stats.baseValues.put(MachineStat.BATTERY_SLOTS, 0.0);
        stats.baseValues.put(MachineStat.GLOBAL_MODIFIER_STRENGTH, 1.0);
        stats.baseValues.put(MachineStat.FLUID_CAPACITY, 0.0);
        stats.baseValues.put(MachineStat.COMPRESSION_RATIO, 1.0);
        stats.baseValues.put(MachineStat.FLUID_TRANSFER, 1.0);
        stats.baseValues.put(MachineStat.CALIBRATION_QUALITY, 1.0);
        stats.baseValues.put(MachineStat.CALIBRATION_PRECISION, 1.0);
        stats.baseValues.put(MachineStat.CATALYST_EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.REFINEMENT_POTENTIAL_BONUS, 0.0);
        stats.baseValues.put(MachineStat.OUTPUT_GUARD_GRACE, 0.0);
        stats.baseValues.put(MachineStat.NO_BATTERY_OUTPUT_RETENTION, 0.0);
        stats.baseValues.put(MachineStat.HIGH_HARDNESS_ENERGY_MITIGATION, 0.0);
        stats.baseValues.put(MachineStat.CRUSHER_INPUT_FILTER, 0.0);
        stats.baseValues.put(MachineStat.CRUSHER_SALVAGE_CHANCE, 0.0);
        stats.baseValues.put(MachineStat.BLOCK_FILTER_SLOTS, 0.0);
        stats.baseValues.put(MachineStat.CONTROL, 0.0);
        stats.baseValues.put(MachineStat.DRIVE, 0.0);
        stats.baseValues.put(MachineStat.RESERVE, 0.0);
    }

    private static void applyBaseFurnaceValues(MachineStatAccumulator stats) {
        stats.baseValues.put(MachineStat.HEAT_TRANSFER, 1.0);
        stats.baseValues.put(MachineStat.HEAT_ISOLATION, 1.0);
        stats.baseValues.put(MachineStat.MAX_TEMPERATURE, FURNACE_BASE_MAX_TEMPERATURE);
        stats.baseValues.put(MachineStat.TEMPERATURE_STABILITY, 1.0);
        stats.baseValues.put(MachineStat.FUEL_EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.WARMUP_TIME, 1.0);
        stats.baseValues.put(MachineStat.COOLING_RATE, 1.0);
        stats.baseValues.put(MachineStat.OVERHEAT_TOLERANCE, 1.0);
    }

    public void apply(ModifierSet set) {
        for (MachineModifier modifier : set.modifiers()) {
            apply(modifier);
        }
        add(MachineStat.REFINEMENT_POTENTIAL, set.refinementPotential());
    }

    public void apply(MachineTraits traits) {
        apply(traits.modifierSet());
    }

    public void apply(MachineModifier modifier) {
        for (MachineModifierEffect effect : modifier.effects()) {
            apply(effect);
            if (isFlatEnergyGenerationAffix(modifier, effect)) {
                addFlatEnergyGenerationBonus(effect.value());
            }
        }
    }

    private void apply(MachineModifierEffect effect) {
        switch (effect.operation()) {
            case ADD -> add(effect.stat(), effect.value());
            case INCREASED_PERCENT -> addIncreasedPercent(effect.stat(), effect.value());
            case DECREASED_PERCENT -> addIncreasedPercent(effect.stat(), -effect.value());
            case MORE -> addMore(effect.stat(), effect.value());
            case LESS -> addMore(effect.stat(), effect.value());
        }
    }

    public double value(MachineStat stat) {
        MachineStat resolvedStat = accumulationStat(stat);
        double base = baseValues.getOrDefault(resolvedStat, 0.0);
        double added = additiveValues.getOrDefault(resolvedStat, 0.0);
        double increased = increasedPercentValues.getOrDefault(resolvedStat, 0.0);
        double more = moreValues.getOrDefault(resolvedStat, 1.0);
        double ordinary = (base + added) * Math.max(0.0, 1.0 + increased / 100.0) * more;
        return Math.min(absoluteValues.getOrDefault(resolvedStat, ordinary),
                absoluteCeilings.getOrDefault(resolvedStat, Double.POSITIVE_INFINITY));
    }

    public double generatedEnergyTotal(double baseEnergy, int processingTicks) {
        double statScale = percentAndMore(MachineStat.ENERGY_GENERATION);
        double nonFlatAdditive = additiveValues.getOrDefault(MachineStat.ENERGY_GENERATION, 0.0)
                - flatEnergyGenerationBonus;
        double generationMultiplier = Math.max(
                0.1,
                (baseValues.getOrDefault(MachineStat.ENERGY_GENERATION, 0.0) + nonFlatAdditive) * statScale
        );
        double flatGeneration = flatEnergyGenerationBonus * Math.max(1, processingTicks) * statScale;
        return baseEnergy * generationMultiplier + flatGeneration;
    }

    double valueWithoutFlatEnergyGenerationBonus(MachineStat stat) {
        if (stat != MachineStat.ENERGY_GENERATION) {
            return value(stat);
        }
        double nonFlatAdditive = additiveValues.getOrDefault(stat, 0.0) - flatEnergyGenerationBonus;
        return (baseValues.getOrDefault(stat, 0.0) + nonFlatAdditive) * percentAndMore(stat);
    }

    public double effectiveEnergyGenerationMultiplier() {
        return valueWithoutFlatEnergyGenerationBonus(MachineStat.ENERGY_GENERATION);
    }

    public double effectiveFlatEnergyGenerationBonus() {
        return flatEnergyGenerationBonus * percentAndMore(MachineStat.ENERGY_GENERATION);
    }

    void addFlatEnergyGenerationBonus(double value) {
        flatEnergyGenerationBonus += value;
    }

    public double baseValue(MachineStat stat) {
        return baseValues.getOrDefault(accumulationStat(stat), 0.0);
    }

    public int intValue(MachineStat stat) {
        return Math.max(0, (int) Math.floor(value(stat)));
    }

    public int adjustedProcessingTicks(int baseTicks) {
        double speed = Math.max(0.1, value(MachineStat.PROCESSING_SPEED));
        return Math.max(1, (int) Math.ceil(baseTicks / speed));
    }

    public int adjustedHeatProcessingTicks(int baseTicks) {
        double heatTransfer = Math.max(0.1, value(MachineStat.HEAT_TRANSFER));
        return Math.max(1, (int) Math.ceil(adjustedProcessingTicks(baseTicks) / heatTransfer));
    }

    public int adjustedEnergyCost(int baseEnergyCost) {
        double usage = Math.max(0.1, value(MachineStat.ENERGY_USAGE));
        return Math.max(1, (int) Math.ceil(baseEnergyCost * usage));
    }

    public int adjustedEnergyCostForProgress(int baseEnergyCost, int adjustedTicks, int progress) {
        return distributedEnergyCostForProgress(adjustedEnergyCost(baseEnergyCost), adjustedTicks, progress);
    }

    private static int distributedEnergyCostForProgress(int totalCost, int adjustedTicks, int progress) {
        int safeTicks = Math.max(1, adjustedTicks);
        int currentProgress = Math.max(0, Math.min(progress, safeTicks - 1));
        return cumulativeProcessingEnergyCost(totalCost, safeTicks, currentProgress + 1)
                - cumulativeProcessingEnergyCost(totalCost, safeTicks, currentProgress);
    }

    private static int cumulativeProcessingEnergyCost(int totalCost, int adjustedTicks, int progress) {
        return (int) Math.ceil(totalCost * (double) progress / adjustedTicks);
    }

    public int adjustedFuelTicks(int baseFuelTicks) {
        double fuelEfficiency = Math.max(0.1, value(MachineStat.FUEL_EFFICIENCY));
        double efficiency = Math.max(0.1, value(MachineStat.EFFICIENCY));
        return Math.max(1, (int) Math.ceil(baseFuelTicks * fuelEfficiency * efficiency));
    }

    public void rollBaseValues(List<MachineStatRange> ranges, RandomSource random) {
        for (MachineStatRange range : ranges) {
            baseValues.put(range.stat(), range.roll(random));
        }
    }

    private void add(MachineStat stat, double value) {
        additiveValues.merge(accumulationStat(stat), value, Double::sum);
    }

    private void addIncreasedPercent(MachineStat stat, double value) {
        increasedPercentValues.merge(accumulationStat(stat), value, Double::sum);
    }

    private void addMore(MachineStat stat, double value) {
        moreValues.merge(accumulationStat(stat), value, (current, next) -> current * next);
    }

    private double percentAndMore(MachineStat stat) {
        MachineStat resolvedStat = accumulationStat(stat);
        double increased = increasedPercentValues.getOrDefault(resolvedStat, 0.0);
        double more = moreValues.getOrDefault(resolvedStat, 1.0);
        return (1.0 + increased / 100.0) * more;
    }

    private static MachineStat accumulationStat(MachineStat stat) {
        return stat == MachineStat.ENERGY_CAPACITY_FLAT ? MachineStat.ENERGY_CAPACITY : stat;
    }

    private static boolean isFlatEnergyGenerationAffix(MachineModifier modifier, MachineModifierEffect effect) {
        return effect.stat() == MachineStat.ENERGY_GENERATION
                && effect.operation() == ModifierOperation.ADD
                && (ModifierEligibilityProfiles.ENERGY_GENERATION_FLAT_AFFIX_ID.equals(modifier.affixId())
                        || ModifierEligibilityProfiles.ENERGY_GENERATION_FLAT_AFFIX_ID.equals(modifier.modGroup()));
    }
}
