package com.rngtech.content.gear;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.AffixForgeBlockEntity;
import com.rngtech.content.blockentity.AlgaePhotobioreactorBlockEntity;
import com.rngtech.content.blockentity.AlloyFurnaceBlockEntity;
import com.rngtech.content.blockentity.AmmoniaFuelCellBlockEntity;
import com.rngtech.content.blockentity.AmmoniaSynthesizerBlockEntity;
import com.rngtech.content.blockentity.BatteryAssemblerBlockEntity;
import com.rngtech.content.blockentity.BioGeneratorBlockEntity;
import com.rngtech.content.blockentity.CavitationGeneratorBlockEntity;
import com.rngtech.content.blockentity.ComponentRecyclerBlockEntity;
import com.rngtech.content.blockentity.CompressorTankBlockEntity;
import com.rngtech.content.blockentity.CorrosionCellBlockEntity;
import com.rngtech.content.blockentity.CrusherBlockEntity;
import com.rngtech.content.blockentity.ForestryCartStationBlockEntity;
import com.rngtech.content.blockentity.FurnaceBlockEntity;
import com.rngtech.content.blockentity.GasChemistryBlockEntity;
import com.rngtech.content.blockentity.MelterBlockEntity;
import com.rngtech.content.blockentity.MetalPressBlockEntity;
import com.rngtech.content.blockentity.PotentialReactorBlockEntity;
import com.rngtech.content.blockentity.ResonanceCalibratorBlockEntity;
import com.rngtech.content.blockentity.SolarArrayControllerBlockEntity;
import com.rngtech.content.blockentity.SolidFuelBurnerBlockEntity;
import com.rngtech.content.blockentity.ToolBenchBlockEntity;
import com.rngtech.content.blockentity.VacuumCollapseGeneratorBlockEntity;
import com.rngtech.content.cable.EnergyConnectorTier;
import com.rngtech.content.cable.FluidConnectorTier;
import com.rngtech.content.cable.ItemConnectorTier;
import com.rngtech.content.calibration.CalibrationFamily;
import com.rngtech.content.calibration.CalibrationGearMaterial;
import com.rngtech.content.calibration.ResonanceCalibratorChassis;
import com.rngtech.content.chemistry.GasChemistryMachine;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.energy.BatteryChassisMaterial;
import com.rngtech.content.energy.CathodeMaterial;
import com.rngtech.content.energy.CavitationRotorMaterial;
import com.rngtech.content.energy.CollapseNozzleMaterial;
import com.rngtech.content.energy.ContainmentLiningMaterial;
import com.rngtech.content.energy.FuelBoxMaterial;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.energy.ReactorChamberMaterial;
import com.rngtech.content.energy.RecoveryFilterMaterial;
import com.rngtech.content.energy.SolarArrayExtenderMaterial;
import com.rngtech.content.energy.SolidFuelBurnerChassis;
import com.rngtech.content.energy.VacuumCollapsePartMaterial;
import com.rngtech.content.entity.ForestryCartEntity;
import com.rngtech.content.item.MachinePartItem;
import com.rngtech.content.machine.AlloyCrucibleMaterial;
import com.rngtech.content.machine.AlloyFurnaceChassisMaterial;
import com.rngtech.content.machine.CompressorTankMaterial;
import com.rngtech.content.machine.CrushHeadMaterial;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.machine.FluidPumpMaterial;
import com.rngtech.content.machine.FurnaceChassisMaterial;
import com.rngtech.content.machine.ServoMaterial;
import com.rngtech.content.menu.MinersCompanionMenu;
import com.rngtech.content.recycling.ComponentRecyclerChassis;
import com.rngtech.content.recycling.DisassemblyHeadMaterial;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.tool.PruningShearsMaterial;
import com.rngtech.content.tool.ToolHeadFamily;
import com.rngtech.rpg.MachineType;
import com.rngtech.rpg.ModifierEligibilityProfiles;
import com.rngtech.rpg.unique.UniqueDefinition;
import com.rngtech.rpg.unique.UniqueHost;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class GearSlotCatalog {
    public static List<GearMachineSpec> all() {
        List<GearMachineSpec> specs = new ArrayList<>();
        addCrusher(specs);
        addFurnace(specs);
        addAlloyFurnace(specs);
        addBatteryChassis(specs);
        addSolidFuelBurners(specs);
        addSingleMachines(specs);
        addMinersCompanion(specs);
        addGasChemistry(specs);
        addComponentRecycler(specs);
        addResonanceCalibrator(specs);
        addCompressorTanks(specs);
        return List.copyOf(specs);
    }

    public static Optional<GearMachineSpec> forMachineStack(ItemStack stack) {
        return all().stream().filter(spec -> spec.matchesMachineStack(stack)).findFirst();
    }

    public static Optional<GearSlotSpec> slotFor(ItemStack machineStack, GearSlotArea area, int slotIndex) {
        return forMachineStack(machineStack).flatMap(spec -> spec.slot(area, slotIndex));
    }

    private static void addCrusher(List<GearMachineSpec> specs) {
        for (CrusherChassisMaterial material : CrusherChassisMaterial.values()) {
            specs.add(spec(
                    material.blockId(),
                    item(ModItems.crusherChassis(material).get()),
                    slot(
                            "rngtech.gear.crush_head",
                            true,
                            GearSlotArea.MACHINE,
                            CrusherBlockEntity.SLOT_CRUSH_HEAD,
                            crushHeads(head -> head.stage() <= material.stage()),
                            maxStage(material.stage())
                    ),
                    slot(
                            "rngtech.gear.battery_cell",
                            false,
                            GearSlotArea.MACHINE,
                            CrusherBlockEntity.SLOT_FUEL,
                            batteryCells(materialFilter -> true),
                            anyStage()
                    )
            ));
        }
    }

    private static void addFurnace(List<GearMachineSpec> specs) {
        for (FurnaceChassisMaterial material : FurnaceChassisMaterial.values()) {
            List<GearSlotSpec> slots = new ArrayList<>();
            if (material.electric()) {
                slots.add(slot(
                        "rngtech.gear.battery_cell",
                        false,
                        GearSlotArea.MACHINE,
                        FurnaceBlockEntity.SLOT_FUEL,
                        batteryCells(cell -> true),
                        anyStage()
                ));
            }
            if (material.usesHeatCores()) {
                slots.add(slot(
                        "rngtech.gear.heat_core",
                        true,
                        GearSlotArea.GEAR,
                        0,
                        material.heatCoreSlots(),
                        heatCores(core -> core.stage() <= material.stage()),
                        maxStage(material.stage())
                ));
            } else {
                slots.add(slot(
                        "rngtech.gear.component",
                        false,
                        GearSlotArea.GEAR,
                        0,
                        1,
                        machineParts(MachineType.FURNACE),
                        Component.translatable("rngtech.gear_guide.future_component_slot").withStyle(ChatFormatting.GRAY)
                ));
            }
            specs.add(spec(material.blockId(), item(ModItems.furnaceChassis(material).get()), slots));
        }
    }

    private static void addAlloyFurnace(List<GearMachineSpec> specs) {
        for (AlloyFurnaceChassisMaterial material : AlloyFurnaceChassisMaterial.values()) {
            specs.add(spec(
                    material.blockId(),
                    item(ModItems.alloyFurnaceChassis(material).get()),
                    slot(
                            "rngtech.gear.heat_core",
                            true,
                            GearSlotArea.GEAR,
                            AlloyFurnaceBlockEntity.SLOT_HEAT_CORE,
                            heatCores(core -> core.stage() <= material.stage()),
                            maxStage(material.stage())
                    ),
                    slot(
                            "rngtech.gear.crucible",
                            true,
                            GearSlotArea.GEAR,
                            AlloyFurnaceBlockEntity.SLOT_CRUCIBLE,
                            alloyCrucibles(crucible -> crucible.stage() <= material.stage()),
                            maxStage(material.stage())
                    ),
                    slot(
                            "rngtech.gear.battery_cell",
                            false,
                            GearSlotArea.GEAR,
                            AlloyFurnaceBlockEntity.SLOT_BATTERY_CELL,
                            batteryCells(cell -> true),
                            anyStage()
                    ),
                    slot(
                            "rngtech.gear.servo",
                            false,
                            GearSlotArea.GEAR,
                            AlloyFurnaceBlockEntity.SLOT_SERVO,
                            servos(servo -> servo.stage() <= material.stage()),
                            maxStage(material.stage())
                    )
            ));
        }
    }

    private static void addBatteryChassis(List<GearMachineSpec> specs) {
        int additionalSlots = ModifierEligibilityProfiles.maxBatteryChassisAdditionalSlots();
        for (BatteryChassisMaterial material : BatteryChassisMaterial.values()) {
            specs.add(spec(
                    material.blockId(),
                    item(ModItems.batteryChassis(material).get()),
                    slot(
                            "rngtech.gear.battery_cells",
                            true,
                            GearSlotArea.CELL,
                            0,
                            material.slots() + additionalSlots,
                            batteryCells(cell -> true),
                            Component.translatable("rngtech.gear_guide.base_slots", material.slots()).withStyle(ChatFormatting.GRAY),
                            Component.translatable("rngtech.gear_guide.additional_slots", additionalSlots).withStyle(ChatFormatting.GRAY)
                    )
            ));
        }
    }

    private static void addSolidFuelBurners(List<GearMachineSpec> specs) {
        for (SolidFuelBurnerChassis chassis : SolidFuelBurnerChassis.values()) {
            int maxPartStage = chassis.maxPartStage();
            int maxCellStage = Math.min(4, maxPartStage);
            specs.add(spec(
                    chassis.blockId(),
                    item(ModItems.solidFuelBurner(chassis).get()),
                    slot(
                            "rngtech.gear.heat_core",
                            true,
                            GearSlotArea.GEAR,
                            SolidFuelBurnerBlockEntity.SLOT_HEAT_CORE,
                            heatCores(core -> core.stage() <= maxPartStage),
                            maxStage(maxPartStage)
                    ),
                    slot(
                            "rngtech.gear.battery_cell",
                            true,
                            GearSlotArea.GEAR,
                            SolidFuelBurnerBlockEntity.SLOT_BATTERY_CELL,
                            batteryCells(cell -> cell.stage() <= maxCellStage),
                            stageRange(0, maxCellStage)
                    ),
                    slot(
                            "rngtech.gear.fuel_box",
                            true,
                            GearSlotArea.GEAR,
                            SolidFuelBurnerBlockEntity.SLOT_FUEL_BOX,
                            fuelBoxes(box -> box.stage() <= maxPartStage),
                            maxStage(maxPartStage)
                    )
            ));
        }
    }

    private static void addSingleMachines(List<GearMachineSpec> specs) {
        specs.add(spec(
                "forestry_cart",
                item(ModItems.FORESTRY_CART.get()),
                slot(
                        "rngtech.gear.battery_cell",
                        false,
                        GearSlotArea.GEAR,
                        ForestryCartEntity.SLOT_BATTERY_CELL,
                        batteryCells(cell -> true),
                        anyStage()
                ),
                slot(
                        "rngtech.gear.modular_tool",
                        true,
                        GearSlotArea.GEAR,
                        ForestryCartEntity.SLOT_TOOL,
                        modularTools(family -> family == ToolHeadFamily.AXE || family == ToolHeadFamily.TREEFELLER),
                        anyRegistered(),
                        Component.translatable("rngtech.gear_guide.forestry_cart_tool").withStyle(ChatFormatting.GRAY)
                ),
                slot(
                        "rngtech.gear.shears",
                        false,
                        GearSlotArea.GEAR,
                        ForestryCartEntity.SLOT_SHEARS,
                        forestryCartShears(),
                        anyRegistered()
                ),
                slot(
                        "rngtech.gear.fluid_pump",
                        false,
                        GearSlotArea.GEAR,
                        ForestryCartEntity.SLOT_PUMP,
                        fluidPumps(pump -> true),
                        anyRegistered(),
                        Component.translatable("rngtech.gear_guide.forestry_cart_pump").withStyle(ChatFormatting.GRAY)
                )
        ));
        specs.add(spec(
                "forestry_cart_station",
                item(ModItems.FORESTRY_CART_STATION.get()),
                slot(
                        "rngtech.gear.battery_cell",
                        false,
                        GearSlotArea.GEAR,
                        ForestryCartStationBlockEntity.SLOT_STATION_BATTERY_CELL,
                        batteryCells(cell -> true),
                        anyStage()
                ),
                slot(
                        "rngtech.gear.energy_connector",
                        true,
                        GearSlotArea.GEAR,
                        ForestryCartStationBlockEntity.SLOT_ENERGY_CONNECTOR,
                        energyConnectors(tier -> true),
                        anyStage()
                ),
                slot(
                        "rngtech.gear.item_connector",
                        true,
                        GearSlotArea.GEAR,
                        ForestryCartStationBlockEntity.SLOT_ITEM_CONNECTOR,
                        itemConnectors(tier -> true),
                        anyStage()
                ),
                slot(
                        "rngtech.gear.fluid_connector",
                        true,
                        GearSlotArea.GEAR,
                        ForestryCartStationBlockEntity.SLOT_FLUID_CONNECTOR,
                        fluidConnectors(tier -> true),
                        anyStage()
                )
        ));
        specs.add(spec(
                "bio_generator",
                item(ModItems.BIO_GENERATOR.get()),
                slot(
                        "rngtech.gear.battery_cell",
                        false,
                        GearSlotArea.GEAR,
                        BioGeneratorBlockEntity.SLOT_BATTERY_CELL,
                        batteryCells(cell -> cell.stage() <= 3),
                        stageRange(0, 3)
                ),
                slot(
                        "rngtech.gear.bio_chamber",
                        true,
                        GearSlotArea.GEAR,
                        BioGeneratorBlockEntity.SLOT_BIO_CHAMBER,
                        List.of(item(ModItems.BIO_CHAMBER.get())),
                        exactComponent()
                )
        ));
        specs.add(spec(
                "algae_photobioreactor",
                item(ModItems.ALGAE_PHOTOBIOREACTOR.get()),
                slot(
                        "rngtech.gear.bio_chamber",
                        true,
                        GearSlotArea.GEAR,
                        AlgaePhotobioreactorBlockEntity.SLOT_BIO_CHAMBER,
                        List.of(item(ModItems.BIO_CHAMBER.get())),
                        exactComponent()
                )
        ));
        specs.add(spec(
                "solar_array_controller",
                item(ModItems.SOLAR_ARRAY_CONTROLLER.get()),
                slot(
                        "rngtech.gear.battery_cell",
                        false,
                        GearSlotArea.GEAR,
                        SolarArrayControllerBlockEntity.SLOT_BATTERY_CELL,
                        batteryCells(cell -> true),
                        anyStage()
                ),
                slot(
                        "rngtech.gear.solar_array_extender",
                        false,
                        GearSlotArea.GEAR,
                        SolarArrayControllerBlockEntity.SLOT_SOLAR_ARRAY_EXTENDER,
                        solarArrayExtenders(material -> true),
                        anyRegistered()
                )
        ));
        specs.add(spec(
                "potential_reactor",
                item(ModItems.POTENTIAL_REACTOR.get()),
                slot(
                        "rngtech.gear.reactor_chamber",
                        true,
                        GearSlotArea.GEAR,
                        PotentialReactorBlockEntity.SLOT_REACTOR_CHAMBER,
                        reactorChambers(material -> true),
                        anyRegistered()
                ),
                slot(
                        "rngtech.gear.recovery_filter",
                        false,
                        GearSlotArea.GEAR,
                        PotentialReactorBlockEntity.SLOT_RECOVERY_FILTER,
                        recoveryFilters(material -> true),
                        anyRegistered()
                ),
                slot(
                        "rngtech.gear.containment_lining",
                        false,
                        GearSlotArea.GEAR,
                        PotentialReactorBlockEntity.SLOT_CONTAINMENT_LINING,
                        containmentLinings(material -> true),
                        anyRegistered()
                )
        ));
        specs.add(spec(
                "corrosion_cell",
                item(ModItems.CORROSION_CELL.get()),
                slot(
                        "rngtech.gear.battery_cell",
                        false,
                        GearSlotArea.GEAR,
                        CorrosionCellBlockEntity.SLOT_BATTERY_CELL,
                        batteryCells(cell -> true),
                        anyStage()
                ),
                slot(
                        "rngtech.gear.fluid_pump",
                        false,
                        GearSlotArea.GEAR,
                        CorrosionCellBlockEntity.SLOT_FLUID_PUMP,
                        fluidPumps(pump -> true),
                        anyRegistered()
                ),
                slot(
                        "rngtech.gear.cathode",
                        false,
                        GearSlotArea.GEAR,
                        CorrosionCellBlockEntity.SLOT_CATHODE,
                        cathodes(cathode -> true),
                        stageRange(5, 8)
                )
        ));
        specs.add(spec(
                "cavitation_generator",
                item(ModItems.CAVITATION_GENERATOR.get()),
                slot(
                        "rngtech.gear.rotor",
                        true,
                        GearSlotArea.GEAR,
                        CavitationGeneratorBlockEntity.SLOT_ROTOR,
                        cavitationRotors(material -> true),
                        anyRegistered()
                ),
                slot(
                        "rngtech.gear.nozzle",
                        true,
                        GearSlotArea.GEAR,
                        CavitationGeneratorBlockEntity.SLOT_NOZZLE,
                        collapseNozzles(CollapseNozzleMaterial::cavitationCompatible),
                        anyRegistered()
                ),
                slot(
                        "rngtech.gear.heat_core",
                        false,
                        GearSlotArea.GEAR,
                        CavitationGeneratorBlockEntity.SLOT_HEAT_CORE,
                        heatCores(core -> core.stage() <= 8),
                        maxStage(8)
                ),
                slot(
                        "rngtech.gear.battery_cell",
                        false,
                        GearSlotArea.GEAR,
                        CavitationGeneratorBlockEntity.SLOT_BATTERY_CELL,
                        batteryCells(cell -> true),
                        anyStage()
                ),
                slot(
                        "rngtech.gear.servo",
                        false,
                        GearSlotArea.GEAR,
                        CavitationGeneratorBlockEntity.SLOT_SERVO,
                        servos(servo -> servo.stage() >= 6),
                        minStage(6)
                )
        ));
        specs.add(spec(
                "ammonia_synthesizer",
                item(ModItems.AMMONIA_SYNTHESIZER.get()),
                slot(
                        "rngtech.gear.catalyst_bed",
                        true,
                        GearSlotArea.GEAR,
                        AmmoniaSynthesizerBlockEntity.SLOT_CATALYST_BED,
                        List.of(item(ModItems.AMMONIA_CATALYST_BED.get())),
                        exactComponent()
                )
        ));
        specs.add(spec(
                "ammonia_fuel_cell",
                item(ModItems.AMMONIA_FUEL_CELL.get()),
                slot(
                        "rngtech.gear.fuel_cell_membrane",
                        true,
                        GearSlotArea.GEAR,
                        AmmoniaFuelCellBlockEntity.SLOT_MEMBRANE,
                        List.of(item(ModItems.FUEL_CELL_MEMBRANE.get())),
                        exactComponent()
                ),
                slot(
                        "rngtech.gear.battery_cell",
                        false,
                        GearSlotArea.GEAR,
                        AmmoniaFuelCellBlockEntity.SLOT_BATTERY_CELL,
                        batteryCells(cell -> cell.stage() >= 6),
                        minStage(6)
                )
        ));
        specs.add(spec(
                "vacuum_collapse_generator",
                item(ModItems.VACUUM_COLLAPSE_GENERATOR.get()),
                slot(
                        "rngtech.gear.void_chamber",
                        true,
                        GearSlotArea.GEAR,
                        VacuumCollapseGeneratorBlockEntity.SLOT_VOID_CHAMBER,
                        vacuumParts(ModItems.VOID_CHAMBERS, material -> true),
                        anyRegistered()
                ),
                slot(
                        "rngtech.gear.collapse_nozzle",
                        true,
                        GearSlotArea.GEAR,
                        VacuumCollapseGeneratorBlockEntity.SLOT_COLLAPSE_NOZZLE,
                        collapseNozzles(CollapseNozzleMaterial::vacuumCollapseCompatible),
                        anyRegistered()
                ),
                slot(
                        "rngtech.gear.dimensional_stabilizer",
                        true,
                        GearSlotArea.GEAR,
                        VacuumCollapseGeneratorBlockEntity.SLOT_DIMENSIONAL_STABILIZER,
                        vacuumParts(ModItems.DIMENSIONAL_STABILIZERS, material -> true),
                        anyRegistered()
                )
        ));
        specs.add(spec(
                "crude_metal_press",
                item(ModItems.CRUDE_METAL_PRESS.get()),
                metalPressSlots(false)
        ));
        specs.add(spec(
                "metal_press",
                item(ModItems.METAL_PRESS.get()),
                metalPressSlots(true)
        ));
        specs.add(spec(
                "melter",
                item(ModItems.MELTER.get()),
                slot(
                        "rngtech.gear.heat_core",
                        true,
                        GearSlotArea.GEAR,
                        MelterBlockEntity.SLOT_HEAT_CORE,
                        heatCores(core -> core.stage() <= 6),
                        maxStage(6)
                ),
                slot(
                        "rngtech.gear.crush_head",
                        true,
                        GearSlotArea.GEAR,
                        MelterBlockEntity.SLOT_CRUSH_HEAD,
                        crushHeads(head -> true),
                        anyRegistered()
                ),
                slot(
                        "rngtech.gear.battery_cell",
                        false,
                        GearSlotArea.GEAR,
                        MelterBlockEntity.SLOT_BATTERY_CELL,
                        batteryCells(cell -> true),
                        anyStage()
                ),
                slot(
                        "rngtech.gear.servo",
                        false,
                        GearSlotArea.GEAR,
                        MelterBlockEntity.SLOT_SERVO,
                        servos(servo -> true),
                        anyRegistered()
                ),
                slot(
                        "rngtech.gear.fluid_pump",
                        true,
                        GearSlotArea.GEAR,
                        MelterBlockEntity.SLOT_FLUID_PUMP,
                        fluidPumps(pump -> true),
                        anyRegistered()
                )
        ));
        specs.add(spec(
                "battery_assembler",
                item(ModItems.BATTERY_ASSEMBLER.get()),
                slot(
                        "rngtech.gear.battery_cell",
                        false,
                        GearSlotArea.GEAR,
                        BatteryAssemblerBlockEntity.SLOT_BATTERY_CELL,
                        batteryCells(cell -> true),
                        anyStage()
                )
        ));
        specs.add(spec(
                "affix_forge",
                item(ModItems.AFFIX_FORGE.get()),
                slot(
                        "rngtech.gear.affix_lens_array",
                        false,
                        GearSlotArea.MACHINE,
                        AffixForgeBlockEntity.SLOT_LENS_ARRAY,
                        List.of(item(ModItems.AFFIX_LENS_ARRAY.get())),
                        Component.translatable("rngtech.gear_guide.affix_lens_array").withStyle(ChatFormatting.GRAY)
                ),
                slot(
                        "rngtech.gear.affix_modifier_socket",
                        false,
                        GearSlotArea.MACHINE,
                        AffixForgeBlockEntity.SLOT_MODIFIER_SOCKET,
                        List.of(item(ModItems.AFFIX_MODIFIER_SOCKET.get())),
                        Component.translatable("rngtech.gear_guide.affix_modifier_socket").withStyle(ChatFormatting.GRAY)
                ),
                slot(
                        "rngtech.gear.affix_resonance_matrix",
                        false,
                        GearSlotArea.MACHINE,
                        AffixForgeBlockEntity.SLOT_RESONANCE_MATRIX,
                        List.of(item(ModItems.AFFIX_RESONANCE_MATRIX.get())),
                        Component.translatable("rngtech.gear_guide.affix_resonance_matrix").withStyle(ChatFormatting.GRAY)
                )
        ));
        specs.add(spec(
                "tool_bench",
                item(ModItems.TOOL_BENCH.get()),
                slot(
                        "rngtech.tool_bench.anvil",
                        false,
                        GearSlotArea.MACHINE,
                        ToolBenchBlockEntity.SLOT_TINY_ANVIL,
                        List.of(item(ModItems.TINY_ANVIL.get())),
                        Component.translatable("rngtech.gear_guide.tiny_anvil_use").withStyle(ChatFormatting.GRAY)
                ),
                slot(
                        "rngtech.gear.energy_connector",
                        false,
                        GearSlotArea.MACHINE,
                        ToolBenchBlockEntity.SLOT_ENERGY_CONNECTOR,
                        energyConnectors(tier -> true),
                        Component.translatable("rngtech.gear_guide.tool_bench_energy_connector").withStyle(ChatFormatting.GRAY)
                )
        ));
    }

    private static void addMinersCompanion(List<GearMachineSpec> specs) {
        specs.add(spec(
                "miners_companion",
                item(ModItems.MINERS_COMPANION.get()),
                slot(
                        "rngtech.gear.crush_head",
                        true,
                        GearSlotArea.GEAR,
                        MinersCompanionMenu.SLOT_CRUSH_HEAD,
                        crushHeads(head -> true),
                        anyStage()
                ),
                slot(
                        "rngtech.gear.battery_cell",
                        true,
                        GearSlotArea.GEAR,
                        MinersCompanionMenu.SLOT_BATTERY_CELL,
                        batteryCells(cell -> true),
                        anyStage()
                ),
                slot(
                        "rngtech.gear.recovery_filter",
                        false,
                        GearSlotArea.GEAR,
                        MinersCompanionMenu.SLOT_RECOVERY_FILTER,
                        recoveryFilters(filter -> true),
                        anyRegistered()
                ),
                slot(
                        "rngtech.gear.magnet",
                        false,
                        GearSlotArea.GEAR,
                        MinersCompanionMenu.SLOT_MAGNET,
                        List.of(item(ModItems.MINERS_COMPANION_MAGNET.get())),
                        exactComponent()
                ),
                slot(
                        "rngtech.gear.mining_lamp",
                        false,
                        GearSlotArea.GEAR,
                        MinersCompanionMenu.SLOT_MINING_LAMP,
                        List.of(item(ModItems.MINING_LAMP.get())),
                        exactComponent()
                )
        ));
    }

    private static void addGasChemistry(List<GearMachineSpec> specs) {
        specs.add(gasSpec(GasChemistryMachine.COAL_GASIFIER, item(ModItems.COAL_GASIFIER.get()), 5, true, true, false));
        specs.add(gasSpec(GasChemistryMachine.SYNGAS_COMBUSTOR, item(ModItems.SYNGAS_COMBUSTOR.get()), 5, false, true, false));
        specs.add(gasSpec(GasChemistryMachine.STEAM_METHANE_REFORMER, item(ModItems.STEAM_METHANE_REFORMER.get()), 6, true, true, true));
    }

    private static GearMachineSpec gasSpec(
            GasChemistryMachine machine,
            ItemStack catalyst,
            int minimumStage,
            boolean heatCore,
            boolean servo,
            boolean catalystBed
    ) {
        List<GearSlotSpec> slots = new ArrayList<>();
        if (heatCore) {
            slots.add(slot(
                    "rngtech.gear.heat_core",
                    true,
                    GearSlotArea.GEAR,
                    GasChemistryBlockEntity.SLOT_HEAT_CORE,
                    heatCores(core -> core.stage() >= minimumStage),
                    minStage(minimumStage)
            ));
        }
        slots.add(slot(
                "rngtech.gear.battery_cell",
                false,
                GearSlotArea.GEAR,
                GasChemistryBlockEntity.SLOT_BATTERY_CELL,
                batteryCells(cell -> cell.stage() >= machine.stage()),
                minStage(machine.stage())
        ));
        if (servo) {
            slots.add(slot(
                    "rngtech.gear.servo",
                    false,
                    GearSlotArea.GEAR,
                    GasChemistryBlockEntity.SLOT_SERVO,
                    servos(material -> material.stage() >= 4),
                    minStage(4)
            ));
        }
        if (catalystBed) {
            slots.add(slot(
                    "rngtech.gear.catalyst_bed",
                    true,
                    GearSlotArea.GEAR,
                    GasChemistryBlockEntity.SLOT_CATALYST_BED,
                    List.of(item(ModItems.REFORMING_CATALYST_BED.get())),
                    exactComponent()
            ));
        }
        return spec(machine.blockId(), catalyst, slots);
    }

    private static void addComponentRecycler(List<GearMachineSpec> specs) {
        for (ComponentRecyclerChassis chassis : ComponentRecyclerChassis.values()) {
            if (chassis.manual()) {
                continue;
            }
            specs.add(spec(
                    chassis.blockId(),
                    item(ModItems.componentRecycler(chassis).get()),
                    slot(
                            "rngtech.gear.battery_cell",
                            false,
                            GearSlotArea.GEAR,
                            ComponentRecyclerBlockEntity.SLOT_BATTERY_CELL,
                            batteryCells(cell -> true),
                            anyStage()
                    ),
                    slot(
                            "rngtech.gear.disassembly_head",
                            true,
                            GearSlotArea.GEAR,
                            ComponentRecyclerBlockEntity.SLOT_DISASSEMBLY_HEAD,
                            disassemblyHeads(head -> head.stage() <= chassis.stage()),
                            maxStage(chassis.stage())
                    ),
                    slot(
                            "rngtech.gear.recovery_filter",
                            false,
                            GearSlotArea.GEAR,
                            ComponentRecyclerBlockEntity.SLOT_RECOVERY_FILTER,
                            recoveryFilters(filter -> filter.stage() <= chassis.stage()),
                            maxStage(chassis.stage())
                    )
            ));
        }
    }

    private static void addResonanceCalibrator(List<GearMachineSpec> specs) {
        for (ResonanceCalibratorChassis chassis : ResonanceCalibratorChassis.values()) {
            specs.add(spec(
                    chassis.blockId(),
                    item(ModItems.resonanceCalibrator(chassis).get()),
                    slot(
                            "rngtech.gear.battery_cell",
                            false,
                            GearSlotArea.GEAR,
                            ResonanceCalibratorBlockEntity.SLOT_BATTERY_CELL,
                            batteryCells(cell -> true),
                            anyStage()
                    ),
                    slot(
                            "rngtech.gear.resonance_coil",
                            true,
                            GearSlotArea.GEAR,
                            ResonanceCalibratorBlockEntity.SLOT_RESONANCE_COIL,
                            resonanceCoils(material -> true),
                            anyRegistered()
                    ),
                    slot(
                            "rngtech.gear.control_board",
                            true,
                            GearSlotArea.GEAR,
                            ResonanceCalibratorBlockEntity.SLOT_CONTROL_BOARD,
                            controlBoards(material -> true),
                            anyRegistered()
                    ),
                    slot(
                            "rngtech.gear.stabilizer_matrix",
                            false,
                            GearSlotArea.GEAR,
                            ResonanceCalibratorBlockEntity.SLOT_STABILIZER_MATRIX,
                            stabilizerMatrices(material -> true),
                            anyRegistered()
                    ),
                    slot(
                            "rngtech.calibration.pattern",
                            true,
                            GearSlotArea.GEAR,
                            ResonanceCalibratorBlockEntity.SLOT_PATTERN_STORAGE,
                            ResonanceCalibratorBlockEntity.PATTERN_SLOT_COUNT,
                            calibrationPatterns(),
                            anyRegistered()
                    )
            ));
        }
    }

    private static void addCompressorTanks(List<GearMachineSpec> specs) {
        for (CompressorTankMaterial material : CompressorTankMaterial.values()) {
            if (!material.supportsCompression()) {
                continue;
            }
            specs.add(spec(
                    material.blockId(),
                    item(ModItems.compressorTank(material).get()),
                    slot(
                            "rngtech.gear.battery_cell",
                            true,
                            GearSlotArea.GEAR,
                            CompressorTankBlockEntity.SLOT_BATTERY_CELL,
                            batteryCells(cell -> cell.stage() <= material.stage()),
                            maxStage(material.stage())
                    ),
                    slot(
                            "rngtech.gear.servo",
                            true,
                            GearSlotArea.GEAR,
                            CompressorTankBlockEntity.SLOT_SERVO_0,
                            4,
                            servos(servo -> servo.stage() <= material.stage()),
                            maxStage(material.stage()),
                            Component.translatable("rngtech.gear_guide.at_least_one").withStyle(ChatFormatting.GRAY)
                    )
            ));
        }
    }

    private static List<GearSlotSpec> metalPressSlots(boolean steelPress) {
        return List.of(
                slot(
                        "rngtech.gear.heat_core",
                        true,
                        GearSlotArea.GEAR,
                        MetalPressBlockEntity.SLOT_HEAT_CORE,
                        heatCores(core -> core.stage() <= 6),
                        maxStage(6)
                ),
                slot(
                        "rngtech.gear.servo",
                        steelPress,
                        GearSlotArea.GEAR,
                        MetalPressBlockEntity.SLOT_SERVO,
                        servos(servo -> true),
                        anyRegistered()
                ),
                slot(
                        "rngtech.gear.mold",
                        true,
                        GearSlotArea.GEAR,
                        MetalPressBlockEntity.SLOT_MOLD,
                        MetalPressBlockEntity.MOLD_SLOT_COUNT,
                        metalPressMolds(),
                        anyRegistered()
                ),
                slot(
                        "rngtech.gear.battery_cell",
                        false,
                        GearSlotArea.GEAR,
                        MetalPressBlockEntity.SLOT_BATTERY_CELL,
                        batteryCells(cell -> true),
                        anyStage()
                )
        );
    }

    private static GearMachineSpec spec(String path, ItemStack catalyst, GearSlotSpec... slots) {
        return spec(path, catalyst, Arrays.asList(slots));
    }

    private static GearMachineSpec spec(String path, ItemStack catalyst, List<GearSlotSpec> slots) {
        return new GearMachineSpec(RNGTech.id("machine_gear/" + path), List.of(catalyst), slots);
    }

    private static GearSlotSpec slot(
            String labelKey,
            boolean required,
            GearSlotArea area,
            int slot,
            List<ItemStack> validStacks,
            Component... notes
    ) {
        return slot(labelKey, required, area, slot, 1, validStacks, notes);
    }

    private static GearSlotSpec slot(
            String labelKey,
            boolean required,
            GearSlotArea area,
            int firstSlot,
            int slotCount,
            List<ItemStack> validStacks,
            Component... notes
    ) {
        return new GearSlotSpec(area, firstSlot, slotCount, labelKey, required, validStacks, List.of(notes));
    }

    private static ItemStack item(Item item) {
        return new ItemStack(item);
    }

    private static List<ItemStack> batteryCells(Predicate<BatteryCellMaterial> filter) {
        return Arrays.stream(BatteryCellMaterial.values())
                .filter(filter)
                .map(material -> item(ModItems.batteryCell(material).get()))
                .toList();
    }

    private static List<ItemStack> modularTools(Predicate<ToolHeadFamily> filter) {
        return Arrays.stream(ToolHeadFamily.values())
                .filter(filter)
                .map(family -> item(ModItems.modularTool(family).get()))
                .toList();
    }

    private static List<ItemStack> forestryCartShears() {
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(item(Items.SHEARS));
        for (PruningShearsMaterial material : PruningShearsMaterial.values()) {
            stacks.add(item(ModItems.pruningShears(material).get()));
        }
        return List.copyOf(stacks);
    }

    private static List<ItemStack> crushHeads(Predicate<CrushHeadMaterial> filter) {
        return withUniques(Arrays.stream(CrushHeadMaterial.values())
                .filter(filter)
                .map(material -> item(ModItems.crushHead(material).get()))
                .toList(), UniqueHost.CRUSH_HEAD, filter);
    }

    private static List<ItemStack> heatCores(Predicate<HeatCoreMaterial> filter) {
        return withUniques(Arrays.stream(HeatCoreMaterial.values())
                .filter(filter)
                .map(material -> item(ModItems.heatCore(material).get()))
                .toList(), UniqueHost.HEAT_CORE, filter);
    }

    private static List<ItemStack> fuelBoxes(Predicate<FuelBoxMaterial> filter) {
        return Arrays.stream(FuelBoxMaterial.values())
                .filter(filter)
                .map(material -> item(ModItems.fuelBox(material).get()))
                .toList();
    }

    private static List<ItemStack> reactorChambers(Predicate<ReactorChamberMaterial> filter) {
        return Arrays.stream(ReactorChamberMaterial.values())
                .filter(filter)
                .map(material -> item(ModItems.reactorChamber(material).get()))
                .toList();
    }

    private static List<ItemStack> recoveryFilters(Predicate<RecoveryFilterMaterial> filter) {
        return Arrays.stream(RecoveryFilterMaterial.values())
                .filter(filter)
                .map(material -> item(ModItems.recoveryFilter(material).get()))
                .toList();
    }

    private static List<ItemStack> containmentLinings(Predicate<ContainmentLiningMaterial> filter) {
        return Arrays.stream(ContainmentLiningMaterial.values())
                .filter(filter)
                .map(material -> item(ModItems.containmentLining(material).get()))
                .toList();
    }

    private static List<ItemStack> cavitationRotors(Predicate<CavitationRotorMaterial> filter) {
        return Arrays.stream(CavitationRotorMaterial.values())
                .filter(filter)
                .map(material -> item(ModItems.cavitationRotor(material).get()))
                .toList();
    }

    private static List<ItemStack> collapseNozzles(Predicate<CollapseNozzleMaterial> filter) {
        return Arrays.stream(CollapseNozzleMaterial.values())
                .filter(filter)
                .map(material -> item(ModItems.collapseNozzle(material).get()))
                .toList();
    }

    private static List<ItemStack> vacuumParts(
            java.util.Map<VacuumCollapsePartMaterial, ? extends java.util.function.Supplier<? extends Item>> items,
            Predicate<VacuumCollapsePartMaterial> filter
    ) {
        return Arrays.stream(VacuumCollapsePartMaterial.values())
                .filter(filter)
                .map(material -> item(items.get(material).get()))
                .toList();
    }

    private static List<ItemStack> servos(Predicate<ServoMaterial> filter) {
        return withUniques(Arrays.stream(ServoMaterial.values())
                .filter(filter)
                .map(material -> item(ModItems.servo(material).get()))
                .toList(), UniqueHost.SERVO, filter);
    }

    private static List<ItemStack> cathodes(Predicate<CathodeMaterial> filter) {
        return Arrays.stream(CathodeMaterial.values())
                .filter(filter)
                .map(material -> item(ModItems.cathode(material).get()))
                .toList();
    }

    private static List<ItemStack> fluidPumps(Predicate<FluidPumpMaterial> filter) {
        return withUniques(Arrays.stream(FluidPumpMaterial.values())
                .filter(filter)
                .map(material -> item(ModItems.fluidPump(material).get()))
                .toList(), UniqueHost.FLUID_PUMP, filter);
    }

    private static List<ItemStack> alloyCrucibles(Predicate<AlloyCrucibleMaterial> filter) {
        return withUniques(Arrays.stream(AlloyCrucibleMaterial.values())
                .filter(filter)
                .map(material -> item(ModItems.alloyCrucible(material).get()))
                .toList(), UniqueHost.ALLOY_CRUCIBLE, filter);
    }

    private static List<ItemStack> disassemblyHeads(Predicate<DisassemblyHeadMaterial> filter) {
        return Arrays.stream(DisassemblyHeadMaterial.values())
                .filter(filter)
                .map(material -> item(ModItems.disassemblyHead(material).get()))
                .toList();
    }

    private static List<ItemStack> energyConnectors(Predicate<EnergyConnectorTier> filter) {
        return Arrays.stream(EnergyConnectorTier.values())
                .filter(filter)
                .map(tier -> item(ModItems.energyConnector(tier).get()))
                .toList();
    }

    private static List<ItemStack> itemConnectors(Predicate<ItemConnectorTier> filter) {
        return Arrays.stream(ItemConnectorTier.values())
                .filter(filter)
                .map(tier -> item(ModItems.itemConnector(tier).get()))
                .toList();
    }

    private static List<ItemStack> fluidConnectors(Predicate<FluidConnectorTier> filter) {
        return Arrays.stream(FluidConnectorTier.values())
                .filter(filter)
                .map(tier -> item(ModItems.fluidConnector(tier).get()))
                .toList();
    }

    private static List<ItemStack> solarArrayExtenders(Predicate<SolarArrayExtenderMaterial> filter) {
        return Arrays.stream(SolarArrayExtenderMaterial.values())
                .filter(filter)
                .map(material -> item(ModItems.solarArrayExtender(material).get()))
                .toList();
    }

    private static List<ItemStack> resonanceCoils(Predicate<CalibrationGearMaterial> filter) {
        return Arrays.stream(CalibrationGearMaterial.values())
                .filter(filter)
                .map(material -> item(ModItems.resonanceCoil(material).get()))
                .toList();
    }

    private static List<ItemStack> controlBoards(Predicate<CalibrationGearMaterial> filter) {
        return withUniques(Arrays.stream(CalibrationGearMaterial.values())
                .filter(filter)
                .map(material -> item(ModItems.controlBoard(material).get()))
                .toList(), UniqueHost.CONTROL_BOARD, filter);
    }

    private static List<ItemStack> stabilizerMatrices(Predicate<CalibrationGearMaterial> filter) {
        return Arrays.stream(CalibrationGearMaterial.values())
                .filter(filter)
                .map(material -> item(ModItems.stabilizerMatrix(material).get()))
                .toList();
    }

    private static List<ItemStack> calibrationPatterns() {
        return Arrays.stream(CalibrationFamily.values())
                .map(family -> item(ModItems.calibrationPattern(family).get()))
                .toList();
    }

    private static List<ItemStack> machineParts(MachineType machineType) {
        return BuiltInRegistries.ITEM.stream()
                .filter(item -> item instanceof MachinePartItem part && part.machineType() == machineType)
                .map(GearSlotCatalog::item)
                .toList();
    }

    private static List<ItemStack> metalPressMolds() {
        return List.of(
                item(ModItems.PLATE_MOLD.get()),
                item(ModItems.CASING_MOLD.get()),
                item(ModItems.GEAR_MOLD.get()),
                item(ModItems.CIRCUIT_MOLD.get()),
                item(ModItems.CONNECTOR_MOLD.get())
        );
    }

    /** Adds Unique parts of {@code host} whose slot stage passes {@code filter}, judged on a normal material of that stage. */
    @SuppressWarnings("unchecked")
    private static <M> List<ItemStack> withUniques(List<ItemStack> normal, UniqueHost host, Predicate<M> filter) {
        List<ItemStack> stacks = new ArrayList<>(normal);
        for (var unique : ModItems.UNIQUE_PARTS.values()) {
            UniqueDefinition definition = unique.get().definition();
            if (definition.host() == host && host.materials().stream()
                    .filter(material -> host.stage(material) == definition.slotStage())
                    .anyMatch(material -> filter.test((M) material))) {
                stacks.add(item(unique.get()));
            }
        }
        return List.copyOf(stacks);
    }

    private static Component anyStage() {
        return Component.translatable("rngtech.gear_guide.any_stage").withStyle(ChatFormatting.GRAY);
    }

    private static Component anyRegistered() {
        return Component.translatable("rngtech.gear_guide.any_registered").withStyle(ChatFormatting.GRAY);
    }

    private static Component exactComponent() {
        return Component.translatable("rngtech.gear_guide.exact_component").withStyle(ChatFormatting.GRAY);
    }

    private static Component minStage(int minimum) {
        return Component.translatable("rngtech.gear_guide.min_stage", minimum).withStyle(ChatFormatting.GRAY);
    }

    private static Component maxStage(int maximum) {
        return Component.translatable("rngtech.gear_guide.max_stage", maximum).withStyle(ChatFormatting.GRAY);
    }

    private static Component stageRange(int minimum, int maximum) {
        return Component.translatable("rngtech.gear_guide.stage_range", minimum, maximum).withStyle(ChatFormatting.GRAY);
    }

    private GearSlotCatalog() {
    }
}
