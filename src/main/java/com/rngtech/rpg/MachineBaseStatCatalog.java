package com.rngtech.rpg;

import com.rngtech.content.block.AlloyFurnaceBlock;
import com.rngtech.content.block.AmmoniaFuelCellBlock;
import com.rngtech.content.block.AmmoniaSynthesizerBlock;
import com.rngtech.content.block.BatteryAssemblerBlock;
import com.rngtech.content.block.BatteryChassisBlock;
import com.rngtech.content.block.BioGeneratorBlock;
import com.rngtech.content.block.CavitationGeneratorBlock;
import com.rngtech.content.block.ComponentRecyclerBlock;
import com.rngtech.content.block.CompressorTankBlock;
import com.rngtech.content.block.CorrosionCellBlock;
import com.rngtech.content.block.CrusherBlock;
import com.rngtech.content.block.FurnaceBlock;
import com.rngtech.content.block.GasChemistryBlock;
import com.rngtech.content.block.MelterBlock;
import com.rngtech.content.block.MetalPressBlock;
import com.rngtech.content.block.PotentialReactorBlock;
import com.rngtech.content.block.ResonanceCalibratorBlock;
import com.rngtech.content.block.SolarArrayControllerBlock;
import com.rngtech.content.block.SolarPanelBlock;
import com.rngtech.content.block.SolidFuelBurnerBlock;
import com.rngtech.content.block.VacuumCollapseGeneratorBlock;
import com.rngtech.content.calibration.ResonanceCalibratorChassis;
import com.rngtech.content.energy.BatteryChassisMaterial;
import com.rngtech.content.energy.SolarPanelMaterial;
import com.rngtech.content.energy.SolidFuelBurnerChassis;
import com.rngtech.content.item.AlloyFurnaceChassisBlockItem;
import com.rngtech.content.item.BatteryChassisBlockItem;
import com.rngtech.content.item.ComponentRecyclerBlockItem;
import com.rngtech.content.item.CompressorTankBlockItem;
import com.rngtech.content.item.CorrosionCellBlockItem;
import com.rngtech.content.item.CrusherChassisBlockItem;
import com.rngtech.content.item.ForestryCartItem;
import com.rngtech.content.item.FurnaceChassisBlockItem;
import com.rngtech.content.item.MachineBlockItem;
import com.rngtech.content.item.PotentialReactorBlockItem;
import com.rngtech.content.item.ResonanceCalibratorBlockItem;
import com.rngtech.content.item.SolarArrayControllerBlockItem;
import com.rngtech.content.item.SolarPanelBlockItem;
import com.rngtech.content.item.SolidFuelBurnerBlockItem;
import com.rngtech.content.machine.AlloyFurnaceChassisMaterial;
import com.rngtech.content.machine.CompressorTankMaterial;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.machine.FurnaceChassisMaterial;
import com.rngtech.content.recycling.ComponentRecyclerChassis;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;

public final class MachineBaseStatCatalog {
    public static final int BIO_GENERATOR_ENERGY_CAPACITY = 400;
    public static final int BIO_GENERATOR_ENERGY_TRANSFER = 80;
    public static final int SOLAR_ARRAY_CONTROLLER_ENERGY_CAPACITY = 512;
    public static final int SOLAR_ARRAY_CONTROLLER_ENERGY_TRANSFER = 128;
    public static final int POTENTIAL_REACTOR_ENERGY_CAPACITY = 16000;
    public static final int POTENTIAL_REACTOR_ENERGY_TRANSFER = 96;
    public static final int CORROSION_CELL_ENERGY_CAPACITY = 8000;
    public static final int CORROSION_CELL_ENERGY_TRANSFER = 128;
    public static final long VACUUM_COLLAPSE_ENERGY_CAPACITY = 20_000L;
    public static final int VACUUM_COLLAPSE_ENERGY_TRANSFER = 8192;
    public static final int CAVITATION_GENERATOR_ENERGY_CAPACITY = 4000;
    public static final int CAVITATION_GENERATOR_ENERGY_TRANSFER = 160;
    public static final int METAL_PRESS_ENERGY_CAPACITY = 1000;
    public static final int METAL_PRESS_ENERGY_TRANSFER = 128;
    public static final int MELTER_ENERGY_CAPACITY = 2400;
    public static final int MELTER_ENERGY_TRANSFER = 160;
    public static final int MELTER_FLUID_TRANSFER = 0;
    public static final int BATTERY_ASSEMBLER_ENERGY_CAPACITY = 1600;
    public static final int BATTERY_ASSEMBLER_ENERGY_TRANSFER = 128;
    public static final int BATTERY_ASSEMBLER_FLUID_TRANSFER = 250;
    public static final int COAL_GASIFIER_ENERGY_CAPACITY = 6000;
    public static final int COAL_GASIFIER_ENERGY_TRANSFER = 192;
    public static final int GAS_CHEMISTRY_FLUID_TRANSFER = 1000;
    public static final int SYNGAS_COMBUSTOR_ENERGY_CAPACITY = 12000;
    public static final int SYNGAS_COMBUSTOR_ENERGY_TRANSFER = 384;
    public static final int STEAM_METHANE_REFORMER_ENERGY_CAPACITY = 12000;
    public static final int STEAM_METHANE_REFORMER_ENERGY_TRANSFER = 256;
    public static final int AMMONIA_SYNTHESIZER_ENERGY_CAPACITY = 12000;
    public static final int AMMONIA_SYNTHESIZER_ENERGY_TRANSFER = 256;
    public static final int AMMONIA_FUEL_CELL_ENERGY_CAPACITY = 16000;
    public static final int AMMONIA_FUEL_CELL_ENERGY_TRANSFER = 512;

    public static MachineStatAccumulator forStack(ItemStack stack) {
        return stack.isEmpty() ? null : forItem(stack.getItem());
    }

    public static MachineStatAccumulator forItem(Item item) {
        if (item instanceof CrusherChassisBlockItem chassis) {
            return crusher(chassis.material());
        }
        if (item instanceof FurnaceChassisBlockItem chassis) {
            return furnace(chassis.material());
        }
        if (item instanceof AlloyFurnaceChassisBlockItem chassis) {
            return alloyFurnace(chassis.material());
        }
        if (item instanceof BatteryChassisBlockItem chassis) {
            return batteryChassis(chassis.material());
        }
        if (item instanceof SolidFuelBurnerBlockItem burner) {
            return solidFuelBurner(burner.chassis());
        }
        if (item instanceof SolarPanelBlockItem panel) {
            return solarPanel(panel.material());
        }
        if (item instanceof SolarArrayControllerBlockItem) {
            return solarArrayController();
        }
        if (item instanceof ResonanceCalibratorBlockItem calibrator) {
            return resonanceCalibrator(calibrator.chassis());
        }
        if (item instanceof ComponentRecyclerBlockItem recycler) {
            return componentRecycler(recycler.chassis());
        }
        if (item instanceof CompressorTankBlockItem tank) {
            return compressorTank(tank.material());
        }
        if (item instanceof ForestryCartItem) {
            return forestryCompanion();
        }
        if (item instanceof PotentialReactorBlockItem) {
            return potentialReactor();
        }
        if (item instanceof CorrosionCellBlockItem) {
            return corrosionCell();
        }
        if (item instanceof MachineBlockItem machine) {
            return switch (machine.machineType()) {
                case BIO_GENERATOR -> bioGenerator();
                case SOLAR_ARRAY_CONTROLLER -> solarArrayController();
                case CORROSION_CELL -> corrosionCell();
                case VACUUM_COLLAPSE_GENERATOR -> vacuumCollapseGenerator();
                case ALLOY_FURNACE -> alloyFurnace(AlloyFurnaceChassisMaterial.BRONZE);
                case METAL_PRESS -> metalPress();
                case MELTER -> melter();
                case BATTERY_ASSEMBLER -> batteryAssembler();
                case COAL_GASIFIER -> coalGasifier();
                case SYNGAS_COMBUSTOR -> syngasCombustor();
                case STEAM_METHANE_REFORMER -> steamMethaneReformer();
                case AMMONIA_SYNTHESIZER -> ammoniaSynthesizer();
                case AMMONIA_FUEL_CELL -> ammoniaFuelCell();
                case CAVITATION_GENERATOR -> cavitationGenerator();
                case COMPRESSOR_TANK -> compressorTank(CompressorTankMaterial.IRON);
                default -> null;
            };
        }
        return null;
    }

    public static MachineStatAccumulator forBlock(Block block) {
        if (block instanceof CrusherBlock crusher) {
            return crusher(crusher.material());
        }
        if (block instanceof FurnaceBlock furnace) {
            return furnace(furnace.material());
        }
        if (block instanceof AlloyFurnaceBlock furnace) {
            return alloyFurnace(furnace.material());
        }
        if (block instanceof BatteryChassisBlock chassis) {
            return batteryChassis(chassis.material());
        }
        if (block instanceof SolidFuelBurnerBlock burner) {
            return solidFuelBurner(burner.chassis());
        }
        if (block instanceof BioGeneratorBlock) {
            return bioGenerator();
        }
        if (block instanceof SolarPanelBlock panel) {
            return solarPanel(panel.material());
        }
        if (block instanceof SolarArrayControllerBlock) {
            return solarArrayController();
        }
        if (block instanceof ResonanceCalibratorBlock calibrator) {
            return resonanceCalibrator(calibrator.chassis());
        }
        if (block instanceof ComponentRecyclerBlock recycler) {
            return componentRecycler(recycler.chassis());
        }
        if (block instanceof CompressorTankBlock tank) {
            return compressorTank(tank.material());
        }
        if (block instanceof PotentialReactorBlock) {
            return potentialReactor();
        }
        if (block instanceof CorrosionCellBlock) {
            return corrosionCell();
        }
        if (block instanceof CavitationGeneratorBlock) {
            return cavitationGenerator();
        }
        if (block instanceof VacuumCollapseGeneratorBlock) {
            return vacuumCollapseGenerator();
        }
        if (block instanceof MetalPressBlock) {
            return metalPress();
        }
        if (block instanceof MelterBlock) {
            return melter();
        }
        if (block instanceof BatteryAssemblerBlock) {
            return batteryAssembler();
        }
        if (block instanceof GasChemistryBlock gasChemistry) {
            return switch (gasChemistry.machine()) {
                case COAL_GASIFIER -> coalGasifier();
                case SYNGAS_COMBUSTOR -> syngasCombustor();
                case STEAM_METHANE_REFORMER -> steamMethaneReformer();
            };
        }
        if (block instanceof AmmoniaSynthesizerBlock) {
            return ammoniaSynthesizer();
        }
        if (block instanceof AmmoniaFuelCellBlock) {
            return ammoniaFuelCell();
        }
        return null;
    }

    public static MachineStatAccumulator crusher(CrusherChassisMaterial material) {
        return MachineStatAccumulator.crusherBase(material);
    }

    public static MachineStatAccumulator furnace(FurnaceChassisMaterial material) {
        return MachineStatAccumulator.furnaceBase(material);
    }

    public static MachineStatAccumulator alloyFurnace(AlloyFurnaceChassisMaterial material) {
        return MachineStatAccumulator.alloyFurnaceBase(material);
    }

    public static MachineStatAccumulator batteryChassis(BatteryChassisMaterial material) {
        return MachineStatAccumulator.batteryChassisBase(material);
    }

    public static MachineStatAccumulator solidFuelBurner(SolidFuelBurnerChassis chassis) {
        return MachineStatAccumulator.solidFuelBurnerBase(chassis);
    }

    public static MachineStatAccumulator bioGenerator() {
        return MachineStatAccumulator.bioGeneratorBase(BIO_GENERATOR_ENERGY_CAPACITY, BIO_GENERATOR_ENERGY_TRANSFER);
    }

    public static MachineStatAccumulator solarPanel(SolarPanelMaterial material) {
        return MachineStatAccumulator.solarPanelBase(material);
    }

    public static MachineStatAccumulator solarArrayController() {
        return MachineStatAccumulator.solarArrayControllerBase(
                SOLAR_ARRAY_CONTROLLER_ENERGY_CAPACITY,
                SOLAR_ARRAY_CONTROLLER_ENERGY_TRANSFER
        );
    }

    public static MachineStatAccumulator potentialReactor() {
        return MachineStatAccumulator.potentialReactorBase(
                POTENTIAL_REACTOR_ENERGY_CAPACITY,
                POTENTIAL_REACTOR_ENERGY_TRANSFER
        );
    }

    public static MachineStatAccumulator corrosionCell() {
        return MachineStatAccumulator.corrosionCellBase(
                CORROSION_CELL_ENERGY_CAPACITY,
                CORROSION_CELL_ENERGY_TRANSFER
        );
    }

    public static MachineStatAccumulator vacuumCollapseGenerator() {
        return MachineStatAccumulator.vacuumCollapseGeneratorBase(
                VACUUM_COLLAPSE_ENERGY_CAPACITY,
                VACUUM_COLLAPSE_ENERGY_TRANSFER
        );
    }

    public static MachineStatAccumulator componentRecycler(ComponentRecyclerChassis chassis) {
        return MachineStatAccumulator.componentRecyclerBase(chassis);
    }

    public static MachineStatAccumulator metalPress() {
        return MachineStatAccumulator.metalPressBase(METAL_PRESS_ENERGY_CAPACITY, METAL_PRESS_ENERGY_TRANSFER);
    }

    public static MachineStatAccumulator melter() {
        return MachineStatAccumulator.melterBase(MELTER_ENERGY_CAPACITY, MELTER_ENERGY_TRANSFER, MELTER_FLUID_TRANSFER);
    }

    public static MachineStatAccumulator batteryAssembler() {
        return MachineStatAccumulator.batteryAssemblerBase(
                BATTERY_ASSEMBLER_ENERGY_CAPACITY,
                BATTERY_ASSEMBLER_ENERGY_TRANSFER,
                BATTERY_ASSEMBLER_FLUID_TRANSFER
        );
    }

    public static MachineStatAccumulator coalGasifier() {
        return MachineStatAccumulator.gasChemistryProcessorBase(
                COAL_GASIFIER_ENERGY_CAPACITY,
                COAL_GASIFIER_ENERGY_TRANSFER,
                GAS_CHEMISTRY_FLUID_TRANSFER
        );
    }

    public static MachineStatAccumulator syngasCombustor() {
        return MachineStatAccumulator.gasCombustorBase(
                SYNGAS_COMBUSTOR_ENERGY_CAPACITY,
                SYNGAS_COMBUSTOR_ENERGY_TRANSFER,
                GAS_CHEMISTRY_FLUID_TRANSFER
        );
    }

    public static MachineStatAccumulator steamMethaneReformer() {
        return MachineStatAccumulator.gasChemistryProcessorBase(
                STEAM_METHANE_REFORMER_ENERGY_CAPACITY,
                STEAM_METHANE_REFORMER_ENERGY_TRANSFER,
                GAS_CHEMISTRY_FLUID_TRANSFER
        );
    }

    public static MachineStatAccumulator ammoniaSynthesizer() {
        return MachineStatAccumulator.ammoniaSynthesizerBase(
                AMMONIA_SYNTHESIZER_ENERGY_CAPACITY,
                AMMONIA_SYNTHESIZER_ENERGY_TRANSFER
        );
    }

    public static MachineStatAccumulator ammoniaFuelCell() {
        return MachineStatAccumulator.ammoniaFuelCellBase(
                AMMONIA_FUEL_CELL_ENERGY_CAPACITY,
                AMMONIA_FUEL_CELL_ENERGY_TRANSFER
        );
    }

    public static MachineStatAccumulator cavitationGenerator() {
        return MachineStatAccumulator.fluidHeatGeneratorBase(
                CAVITATION_GENERATOR_ENERGY_CAPACITY,
                CAVITATION_GENERATOR_ENERGY_TRANSFER
        );
    }

    public static MachineStatAccumulator compressorTank(CompressorTankMaterial material) {
        return MachineStatAccumulator.compressorTankBase(material);
    }

    public static MachineStatAccumulator forestryCompanion() {
        return MachineStatAccumulator.forestryCompanionBase();
    }

    public static MachineStatAccumulator resonanceCalibrator(ResonanceCalibratorChassis chassis) {
        return MachineStatAccumulator.resonanceCalibratorBase(chassis);
    }

    public static List<MachineStat> summaryStats(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof CrusherChassisBlockItem) {
            return crusherSummaryStats();
        }
        if (item instanceof FurnaceChassisBlockItem) {
            return List.of(
                    MachineStat.HEAT_TRANSFER,
                    MachineStat.WARMUP_TIME,
                    MachineStat.COOLING_RATE,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.ENERGY_USAGE,
                    MachineStat.ENERGY_CAPACITY,
                    MachineStat.INPUT_SLOTS
            );
        }
        if (item instanceof AlloyFurnaceChassisBlockItem) {
            return List.of(
                    MachineStat.ENERGY_CAPACITY,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.ENERGY_USAGE,
                    MachineStat.HEAT_TRANSFER,
                    MachineStat.STABILITY
            );
        }
        if (item instanceof BatteryChassisBlockItem) {
            return List.of(
                    MachineStat.BATTERY_SLOTS,
                    MachineStat.ENERGY_TRANSFER,
                    MachineStat.BURST_TRANSFER,
                    MachineStat.BURST_DURATION,
                    MachineStat.EFFICIENCY,
                    MachineStat.STABILITY
            );
        }
        if (item instanceof SolidFuelBurnerBlockItem) {
            return List.of(MachineStat.STABILITY);
        }
        if (item instanceof SolarPanelBlockItem) {
            return List.of(
                    MachineStat.ENERGY_GENERATION,
                    MachineStat.ENERGY_CAPACITY,
                    MachineStat.EFFICIENCY
            );
        }
        if (item instanceof ForestryCartItem) {
            return List.of(MachineStat.PROCESSING_SPEED, MachineStat.ENERGY_USAGE);
        }
        if (item instanceof SolarArrayControllerBlockItem) {
            return List.of(
                    MachineStat.ENERGY_GENERATION,
                    MachineStat.ENERGY_CAPACITY,
                    MachineStat.ENERGY_TRANSFER,
                    MachineStat.EFFICIENCY,
                    MachineStat.STABILITY,
                    MachineStat.SOLAR_PANEL_LIMIT,
                    MachineStat.MOONLIGHT_CONVERSION,
                    MachineStat.WEATHER_RECOVERY
            );
        }
        if (item instanceof PotentialReactorBlockItem) {
            return List.of(
                    MachineStat.ENERGY_CAPACITY,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.ENERGY_GENERATION,
                    MachineStat.STABILITY
            );
        }
        if (item instanceof CorrosionCellBlockItem) {
            return List.of(
                    MachineStat.ENERGY_CAPACITY,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.ENERGY_GENERATION,
                    MachineStat.EFFICIENCY,
                    MachineStat.STABILITY
            );
        }
        if (item instanceof ComponentRecyclerBlockItem) {
            return List.of(
                    MachineStat.ENERGY_CAPACITY,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.ENERGY_USAGE,
                    MachineStat.STABILITY
            );
        }
        if (item instanceof CompressorTankBlockItem tank) {
            return tank.material().supportsCompression() ? compressorTankSummaryStats() : fluidTankSummaryStats();
        }
        if (item instanceof ResonanceCalibratorBlockItem) {
            return List.of(
                    MachineStat.ENERGY_CAPACITY,
                    MachineStat.BATCH_SIZE,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.ENERGY_USAGE,
                    MachineStat.CALIBRATION_QUALITY
            );
        }
        if (item instanceof MachineBlockItem machine) {
            return switch (machine.machineType()) {
                case BIO_GENERATOR -> List.of(
                        MachineStat.ENERGY_CAPACITY,
                        MachineStat.ENERGY_GENERATION,
                        MachineStat.FUEL_EFFICIENCY
                );
                case SOLAR_ARRAY_CONTROLLER -> List.of(
                        MachineStat.ENERGY_CAPACITY,
                        MachineStat.ENERGY_TRANSFER,
                        MachineStat.ENERGY_GENERATION,
                        MachineStat.EFFICIENCY,
                        MachineStat.STABILITY,
                        MachineStat.SOLAR_PANEL_LIMIT,
                        MachineStat.MOONLIGHT_CONVERSION,
                        MachineStat.WEATHER_RECOVERY
                );
                case CORROSION_CELL -> List.of(
                        MachineStat.ENERGY_CAPACITY,
                        MachineStat.PROCESSING_SPEED,
                        MachineStat.ENERGY_GENERATION,
                        MachineStat.EFFICIENCY,
                        MachineStat.STABILITY
                );
                case ALLOY_FURNACE -> List.of(
                        MachineStat.ENERGY_CAPACITY,
                        MachineStat.PROCESSING_SPEED,
                        MachineStat.ENERGY_USAGE,
                        MachineStat.HEAT_TRANSFER,
                        MachineStat.WARMUP_TIME,
                        MachineStat.COOLING_RATE,
                        MachineStat.STABILITY
                );
                case METAL_PRESS -> List.of(
                        MachineStat.ENERGY_CAPACITY,
                        MachineStat.PROCESSING_SPEED,
                        MachineStat.ENERGY_USAGE,
                        MachineStat.HEAT_TRANSFER,
                        MachineStat.WARMUP_TIME,
                        MachineStat.COOLING_RATE,
                        MachineStat.STABILITY
                );
                case MELTER -> List.of(
                        MachineStat.ENERGY_CAPACITY,
                        MachineStat.PROCESSING_SPEED,
                        MachineStat.ENERGY_USAGE,
                        MachineStat.HEAT_TRANSFER,
                        MachineStat.FLUID_TRANSFER
                );
                case BATTERY_ASSEMBLER -> List.of(
                        MachineStat.ENERGY_CAPACITY,
                        MachineStat.PROCESSING_SPEED,
                        MachineStat.ENERGY_USAGE,
                        MachineStat.FLUID_TRANSFER,
                        MachineStat.INPUT_SLOTS,
                        MachineStat.OUTPUT_SLOTS
                );
                case COAL_GASIFIER, STEAM_METHANE_REFORMER -> List.of(
                        MachineStat.ENERGY_CAPACITY,
                        MachineStat.PROCESSING_SPEED,
                        MachineStat.ENERGY_USAGE,
                        MachineStat.HEAT_TRANSFER,
                        MachineStat.FLUID_TRANSFER
                );
                case SYNGAS_COMBUSTOR -> List.of(
                        MachineStat.ENERGY_CAPACITY,
                        MachineStat.ENERGY_GENERATION,
                        MachineStat.EFFICIENCY,
                        MachineStat.PROCESSING_SPEED,
                        MachineStat.FLUID_TRANSFER
                );
                case CAVITATION_GENERATOR -> List.of(
                        MachineStat.ENERGY_CAPACITY,
                        MachineStat.ENERGY_TRANSFER,
                        MachineStat.ENERGY_GENERATION,
                        MachineStat.EFFICIENCY,
                        MachineStat.PROCESSING_SPEED,
                        MachineStat.STABILITY,
                        MachineStat.OUTPUT_AMOUNT,
                        MachineStat.FLUID_TRANSFER
                );
                case COMPRESSOR_TANK -> compressorTankSummaryStats();
                default -> List.of();
            };
        }
        return List.of();
    }

    private static List<MachineStat> compressorTankSummaryStats() {
        return List.of(
                MachineStat.FLUID_CAPACITY,
                MachineStat.COMPRESSION_RATIO,
                MachineStat.FLUID_TRANSFER,
                MachineStat.PROCESSING_SPEED,
                MachineStat.ENERGY_USAGE,
                MachineStat.ENERGY_CAPACITY
        );
    }

    private static List<MachineStat> fluidTankSummaryStats() {
        return List.of(MachineStat.FLUID_CAPACITY);
    }

    private static List<MachineStat> crusherSummaryStats() {
        return List.of(
                MachineStat.ENERGY_CAPACITY,
                MachineStat.ENERGY_USAGE,
                MachineStat.PROCESSING_SPEED,
                MachineStat.OUTPUT_AMOUNT,
                MachineStat.BATCH_SIZE
        );
    }

    private MachineBaseStatCatalog() {
    }
}
