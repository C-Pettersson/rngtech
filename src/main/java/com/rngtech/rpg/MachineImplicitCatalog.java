package com.rngtech.rpg;

import com.rngtech.content.block.AlloyFurnaceBlock;
import com.rngtech.content.block.AmmoniaFuelCellBlock;
import com.rngtech.content.block.AmmoniaSynthesizerBlock;
import com.rngtech.content.block.BatteryAssemblerBlock;
import com.rngtech.content.block.BatteryChassisBlock;
import com.rngtech.content.block.BioGeneratorBlock;
import com.rngtech.content.block.CavitationGeneratorBlock;
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
import com.rngtech.content.calibration.CalibrationGearMaterial;
import com.rngtech.content.calibration.ResonanceCalibratorChassis;
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
import com.rngtech.content.energy.SolidFuelBurnerChassis;
import com.rngtech.content.energy.VacuumCollapsePartMaterial;
import com.rngtech.content.item.AlloyCrucibleItem;
import com.rngtech.content.item.AlloyFurnaceChassisBlockItem;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.BatteryChassisBlockItem;
import com.rngtech.content.item.CalibrationGearItem;
import com.rngtech.content.item.CathodeItem;
import com.rngtech.content.item.CavitationPartItem;
import com.rngtech.content.item.CollapseNozzleItem;
import com.rngtech.content.item.ComponentRecyclerBlockItem;
import com.rngtech.content.item.CompressorTankBlockItem;
import com.rngtech.content.item.CorrosionCellBlockItem;
import com.rngtech.content.item.CrushHeadItem;
import com.rngtech.content.item.CrusherChassisBlockItem;
import com.rngtech.content.item.DisassemblyHeadItem;
import com.rngtech.content.item.FluidPumpItem;
import com.rngtech.content.item.FurnaceChassisBlockItem;
import com.rngtech.content.item.GasChemistryPartItem;
import com.rngtech.content.item.MachineBlockItem;
import com.rngtech.content.item.MachinePartItem;
import com.rngtech.content.item.PotentialReactorBlockItem;
import com.rngtech.content.item.PotentialReactorPartItem;
import com.rngtech.content.item.ResonanceCalibratorBlockItem;
import com.rngtech.content.item.ServoItem;
import com.rngtech.content.item.SolarArrayControllerBlockItem;
import com.rngtech.content.item.SolarArrayExtenderItem;
import com.rngtech.content.item.SolarPanelBlockItem;
import com.rngtech.content.item.SolidFuelBurnerBlockItem;
import com.rngtech.content.item.SolidFuelBurnerPartItem;
import com.rngtech.content.item.VacuumCollapsePartItem;
import com.rngtech.content.machine.AlloyCrucibleMaterial;
import com.rngtech.content.machine.AlloyFurnaceChassisMaterial;
import com.rngtech.content.machine.CompressorTankMaterial;
import com.rngtech.content.machine.CrushHeadMaterial;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.machine.FluidPumpMaterial;
import com.rngtech.content.machine.FurnaceChassisMaterial;
import com.rngtech.content.machine.ServoMaterial;
import com.rngtech.content.recycling.ComponentRecyclerChassis;
import com.rngtech.content.recycling.DisassemblyHeadMaterial;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.rpg.unique.UniqueDefinition;
import com.rngtech.rpg.unique.UniqueItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class MachineImplicitCatalog {
    public record Identity(String translationKey, MachineTraits traits) {
        public static final Identity EMPTY = new Identity("", MachineTraits.EMPTY);

        public boolean isEmpty() {
            return traits.isEmpty() && translationKey.isEmpty();
        }
    }

    public static Identity identity(ItemStack stack) {
        return stack.isEmpty() ? Identity.EMPTY : identity(stack.getItem());
    }

    public static Identity identity(ItemLike itemLike) {
        return itemLike == null ? Identity.EMPTY : identity(itemLike.asItem());
    }

    public static Identity identity(Item item) {
        UniqueDefinition unique = UniqueItems.definition(item);
        if (unique != null) {
            return unique(unique);
        }
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
            return staticMachine("solar_array_controller");
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
        if (item instanceof PotentialReactorBlockItem) {
            return staticMachine("potential_reactor", MachineBehavior.OUTPUT_GUARD);
        }
        if (item instanceof MachineBlockItem machine && machine.machineType() == MachineType.VACUUM_COLLAPSE_GENERATOR) {
            return staticMachine("vacuum_collapse_generator", MachineBehavior.OUTPUT_GUARD);
        }
        if (item instanceof GasChemistryPartItem part
                && part.partType() == MachinePartType.REFORMING_CATALYST_BED) {
            return identity("reforming_catalyst_bed", Rarity.NORMAL, 12, List.of());
        }
        if (item instanceof CorrosionCellBlockItem) {
            return staticMachine("corrosion_cell", MachineBehavior.OUTPUT_GUARD);
        }
        if (item instanceof BatteryCellItem cell) {
            return batteryCell(cell.material());
        }
        if (item instanceof CrushHeadItem head) {
            return crushHead(head.material());
        }
        if (item instanceof AlloyCrucibleItem crucible) {
            return alloyCrucible(crucible.material());
        }
        if (item instanceof SolidFuelBurnerPartItem part) {
            return part.partType() == MachinePartType.HEAT_CORE ? heatCore(part.heatCoreMaterial()) : fuelBox(part.fuelBoxMaterial());
        }
        if (item instanceof MachinePartItem part
                && part.partType() == MachinePartType.BIO_CHAMBER
                && part.machineType() == MachineType.BIO_GENERATOR) {
            return identity("bio_chamber", Rarity.NORMAL, 0, List.of());
        }
        if (item instanceof PotentialReactorPartItem part) {
            return switch (part.partType()) {
                case REACTOR_CHAMBER -> reactorChamber(part.chamberMaterial());
                case RECOVERY_FILTER -> recoveryFilter(part.filterMaterial());
                case CONTAINMENT_LINING -> containmentLining(part.liningMaterial());
                default -> Identity.EMPTY;
            };
        }
        if (item instanceof VacuumCollapsePartItem part) {
            return vacuumCollapsePart(identityId(part.partType()), part.material());
        }
        if (item instanceof CollapseNozzleItem nozzle) {
            return collapseNozzle(nozzle.material());
        }
        if (item instanceof CavitationPartItem part) {
            return switch (part.partType()) {
                case CAVITATION_ROTOR -> cavitationRotor(part.rotorMaterial());
                default -> Identity.EMPTY;
            };
        }
        if (item instanceof DisassemblyHeadItem head) {
            return disassemblyHead(head.material());
        }
        if (item instanceof ServoItem servo) {
            return servo(servo.material());
        }
        if (item instanceof FluidPumpItem pump) {
            return fluidPump(pump.material());
        }
        if (item instanceof CathodeItem cathode) {
            return cathode(cathode.material());
        }
        if (item instanceof SolarArrayExtenderItem extender) {
            return identity(
                    "solar_array_extender." + extender.material().getSerializedName(),
                    Rarity.NORMAL,
                    0,
                    extender.material().modifierSet().modifiers()
            );
        }
        if (item instanceof CalibrationGearItem gear) {
            return switch (gear.partType()) {
                case RESONANCE_COIL -> resonanceCoil(gear.material());
                case CONTROL_BOARD -> controlBoard(gear.material());
                case STABILIZER_MATRIX -> stabilizerMatrix(gear.material());
                default -> Identity.EMPTY;
            };
        }
        if (item instanceof MachineBlockItem machine) {
            return switch (machine.machineType()) {
                case BIO_GENERATOR -> staticMachine("bio_generator");
                case SOLAR_ARRAY_CONTROLLER -> staticMachine("solar_array_controller");
                case CORROSION_CELL -> staticMachine("corrosion_cell", MachineBehavior.OUTPUT_GUARD);
                case ALLOY_FURNACE -> alloyFurnace(AlloyFurnaceChassisMaterial.BRONZE);
                case METAL_PRESS -> metalPress(machine.getBlock());
                case MELTER -> staticMachine("melter", MachineBehavior.OUTPUT_GUARD);
                case BATTERY_ASSEMBLER -> staticMachine("battery_assembler", MachineBehavior.OUTPUT_GUARD);
                case COAL_GASIFIER -> staticMachine("coal_gasifier", MachineBehavior.OUTPUT_GUARD);
                case SYNGAS_COMBUSTOR -> staticMachine("syngas_combustor", MachineBehavior.OUTPUT_GUARD);
                case STEAM_METHANE_REFORMER -> staticMachine("steam_methane_reformer", MachineBehavior.OUTPUT_GUARD);
                case AMMONIA_SYNTHESIZER -> staticMachine("ammonia_synthesizer", MachineBehavior.OUTPUT_GUARD);
                case AMMONIA_FUEL_CELL -> staticMachine("ammonia_fuel_cell", MachineBehavior.OUTPUT_GUARD);
                case CAVITATION_GENERATOR -> staticMachine("cavitation_generator", MachineBehavior.OUTPUT_GUARD);
                case COMPRESSOR_TANK -> compressorTank(CompressorTankMaterial.IRON);
                default -> Identity.EMPTY;
            };
        }
        return Identity.EMPTY;
    }

    public static Identity identity(Block block) {
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
            return staticMachine("bio_generator");
        }
        if (block instanceof SolarPanelBlock panel) {
            return solarPanel(panel.material());
        }
        if (block instanceof SolarArrayControllerBlock) {
            return staticMachine("solar_array_controller");
        }
        if (block instanceof ResonanceCalibratorBlock calibrator) {
            return resonanceCalibrator(calibrator.chassis());
        }
        if (block instanceof com.rngtech.content.block.ComponentRecyclerBlock recycler) {
            return componentRecycler(recycler.chassis());
        }
        if (block instanceof CompressorTankBlock tank) {
            return compressorTank(tank.material());
        }
        if (block instanceof PotentialReactorBlock) {
            return staticMachine("potential_reactor", MachineBehavior.OUTPUT_GUARD);
        }
        if (block instanceof VacuumCollapseGeneratorBlock) {
            return staticMachine("vacuum_collapse_generator", MachineBehavior.OUTPUT_GUARD);
        }
        if (block instanceof CorrosionCellBlock) {
            return staticMachine("corrosion_cell", MachineBehavior.OUTPUT_GUARD);
        }
        if (block instanceof CavitationGeneratorBlock) {
            return staticMachine("cavitation_generator", MachineBehavior.OUTPUT_GUARD);
        }
        if (block instanceof MetalPressBlock) {
            return metalPress(block);
        }
        if (block instanceof MelterBlock) {
            return staticMachine("melter", MachineBehavior.OUTPUT_GUARD);
        }
        if (block instanceof BatteryAssemblerBlock) {
            return staticMachine("battery_assembler", MachineBehavior.OUTPUT_GUARD);
        }
        if (block instanceof GasChemistryBlock gasChemistry) {
            return staticMachine(gasChemistry.machine().blockId(), MachineBehavior.OUTPUT_GUARD);
        }
        if (block instanceof AmmoniaSynthesizerBlock) {
            return staticMachine("ammonia_synthesizer", MachineBehavior.OUTPUT_GUARD);
        }
        if (block instanceof AmmoniaFuelCellBlock) {
            return staticMachine("ammonia_fuel_cell", MachineBehavior.OUTPUT_GUARD);
        }
        return Identity.EMPTY;
    }

    public static MachineTraits effectiveTraits(MachineTraits stored, ItemStack stack) {
        return effectiveTraits(stored, identity(stack).traits());
    }

    public static MachineTraits effectiveTraits(MachineTraits stored, ItemLike itemLike) {
        return effectiveTraits(stored, identity(itemLike).traits());
    }

    public static MachineTraits effectiveTraits(MachineTraits stored, Block block) {
        return effectiveTraits(stored, identity(block).traits());
    }

    public static MachineTraits effectiveTraits(MachineTraits stored, MachineTraits identity) {
        return MachineTraits.withIdentity(MachineTraits.normalizedStored(stored), identity);
    }

    public static MachineTraits storedTraits(MachineTraits traits) {
        return MachineTraits.normalizedStored(traits);
    }

    public static boolean hasBehavior(ItemStack stack, MachineBehavior behavior) {
        Identity identity = identity(stack);
        MachineTraits storedTraits = stack.get(ModDataComponents.MACHINE_TRAITS.get());
        if (storedTraits != null) {
            return effectiveTraits(storedTraits, identity.traits()).hasBehavior(behavior);
        }
        return identity.traits().hasBehavior(behavior);
    }

    public static boolean hasBehavior(Block block, MachineBehavior behavior) {
        return identity(block).traits().hasBehavior(behavior);
    }

    public static Identity crushHead(CrushHeadMaterial material) {
        return identity("crush_head." + material.getSerializedName(), Rarity.NORMAL, material.refinementPotential(), List.of());
    }

    public static Identity alloyCrucible(AlloyCrucibleMaterial material) {
        return identity("alloy_crucible." + material.getSerializedName(), Rarity.NORMAL, material.refinementPotential(), List.of());
    }

    public static Identity heatCore(HeatCoreMaterial material) {
        MachineBehavior[] behaviors = switch (material) {
            case COPPER -> new MachineBehavior[] {MachineBehavior.QUICK_FEED};
            case STEEL, SPARKSTEEL, TITANIUM, EXOTIC -> new MachineBehavior[] {MachineBehavior.THERMAL_BUFFER};
            default -> new MachineBehavior[0];
        };
        return identity("heat_core." + material.getSerializedName(), Rarity.NORMAL, material.refinementPotential(), List.of(), behaviors);
    }

    public static Identity fuelBox(FuelBoxMaterial material) {
        List<MachineBehavior> behaviors = new ArrayList<>();
        if (material.fuelGovernor()) {
            behaviors.add(MachineBehavior.FUEL_GOVERNOR);
        }
        switch (material) {
            case COPPER -> behaviors.add(MachineBehavior.QUICK_FEED);
            case BRONZE -> behaviors.add(MachineBehavior.FUEL_RESERVE);
            case STEEL -> behaviors.add(MachineBehavior.BLOCK_FEED);
            default -> {
            }
        }
        return identity(
                "fuel_box." + material.getSerializedName(),
                Rarity.NORMAL,
                material.refinementPotential(),
                List.of(),
                behaviors.toArray(MachineBehavior[]::new)
        );
    }

    public static Identity reactorChamber(ReactorChamberMaterial material) {
        MachineBehavior[] behaviors = material == ReactorChamberMaterial.STEEL
                ? new MachineBehavior[] {MachineBehavior.THERMAL_BUFFER}
                : new MachineBehavior[0];
        return identity(
                "reactor_chamber." + material.getSerializedName(),
                Rarity.NORMAL,
                material.refinementPotential(),
                List.of(),
                behaviors
        );
    }

    public static Identity recoveryFilter(RecoveryFilterMaterial material) {
        return identity(
                "recovery_filter." + material.getSerializedName(),
                Rarity.NORMAL,
                material.refinementPotential(),
                List.of(),
                MachineBehavior.RESIDUE_SCREEN
        );
    }

    public static Identity containmentLining(ContainmentLiningMaterial material) {
        MachineBehavior[] behaviors = material == ContainmentLiningMaterial.STEEL
                ? new MachineBehavior[] {MachineBehavior.THERMAL_BUFFER}
                : new MachineBehavior[0];
        return identity(
                "containment_lining." + material.getSerializedName(),
                Rarity.NORMAL,
                material.refinementPotential(),
                List.of(),
                behaviors
        );
    }

    public static Identity vacuumCollapsePart(String identityId, VacuumCollapsePartMaterial material) {
        return identity(
                identityId + "." + material.getSerializedName(),
                Rarity.NORMAL,
                material.refinementPotential(),
                List.of()
        );
    }

    public static Identity cavitationRotor(CavitationRotorMaterial material) {
        return identity(
                "cavitation_rotor." + material.getSerializedName(),
                Rarity.NORMAL,
                material.refinementPotential(),
                List.of()
        );
    }

    public static Identity collapseNozzle(CollapseNozzleMaterial material) {
        return identity(
                "collapse_nozzle." + material.getSerializedName(),
                Rarity.NORMAL,
                material.refinementPotential(),
                List.of()
        );
    }

    public static Identity disassemblyHead(DisassemblyHeadMaterial material) {
        return identity("disassembly_head." + material.getSerializedName(), Rarity.NORMAL, material.refinementPotential(), List.of());
    }

    public static Identity servo(ServoMaterial material) {
        List<MachineBehavior> behaviors = new ArrayList<>();
        if (material.stage() >= 6) {
            behaviors.add(MachineBehavior.POWER_GRACE);
        }
        if (material == ServoMaterial.NULLITE) {
            behaviors.add(MachineBehavior.AUTO_PURGE);
        }
        return identity(
                "servo." + material.getSerializedName(),
                Rarity.NORMAL,
                material.refinementPotential(),
                List.of(),
                behaviors.toArray(MachineBehavior[]::new)
        );
    }

    public static Identity cathode(CathodeMaterial material) {
        return identity("cathode." + material.getSerializedName(), Rarity.NORMAL, material.refinementPotential(), List.of());
    }

    public static Identity fluidPump(FluidPumpMaterial material) {
        return identity(
                "fluid_pump." + material.getSerializedName(),
                Rarity.NORMAL,
                material.refinementPotential(),
                List.of(),
                MachineBehavior.SIDE_FLUID_OUTPUT
        );
    }

    public static Identity resonanceCoil(CalibrationGearMaterial material) {
        return identity("resonance_coil." + material.getSerializedName(), Rarity.NORMAL, material.refinementPotential(), List.of());
    }

    public static Identity controlBoard(CalibrationGearMaterial material) {
        return identity("control_board." + material.getSerializedName(), Rarity.NORMAL, material.refinementPotential(), List.of());
    }

    public static Identity stabilizerMatrix(CalibrationGearMaterial material) {
        return identity("stabilizer_matrix." + material.getSerializedName(), Rarity.NORMAL, material.refinementPotential(), List.of());
    }

    private static Identity crusher(CrusherChassisMaterial material) {
        MachineBehavior[] behaviors = switch (material) {
            case STEEL, TITANIUM, TUNGSTENSTEEL, EXOTIC -> new MachineBehavior[] {MachineBehavior.OUTPUT_GUARD};
            default -> new MachineBehavior[0];
        };
        return identity("crusher." + material.getSerializedName(), Rarity.NORMAL, 0, List.of(), behaviors);
    }

    private static Identity furnace(FurnaceChassisMaterial material) {
        MachineBehavior[] behaviors = switch (material) {
            case BRONZE -> new MachineBehavior[] {MachineBehavior.ALLOY_BLEND};
            case STEEL -> new MachineBehavior[] {MachineBehavior.OUTPUT_GUARD};
            case TITANIUM, TUNGSTENSTEEL, EXOTIC -> new MachineBehavior[] {MachineBehavior.THERMAL_BUFFER, MachineBehavior.OUTPUT_GUARD};
            default -> new MachineBehavior[0];
        };
        return identity("furnace." + material.getSerializedName(), Rarity.NORMAL, 0, List.of(), behaviors);
    }

    private static Identity alloyFurnace(AlloyFurnaceChassisMaterial material) {
        MachineBehavior[] behaviors = switch (material) {
            case STEEL, TITANIUM -> new MachineBehavior[] {MachineBehavior.OUTPUT_GUARD};
            default -> new MachineBehavior[0];
        };
        return identity("alloy_furnace." + material.getSerializedName(), Rarity.NORMAL, 0, List.of(), behaviors);
    }

    private static Identity batteryChassis(BatteryChassisMaterial material) {
        MachineBehavior[] behaviors = switch (material) {
            case COPPER -> new MachineBehavior[] {MachineBehavior.BURST_RELEASE};
            case STEEL -> new MachineBehavior[] {MachineBehavior.CELL_LEAKAGE_DAMPING};
            case GOLD -> new MachineBehavior[] {MachineBehavior.BURST_RELEASE};
            case SPARKSTEEL, EXOTIC -> new MachineBehavior[] {MachineBehavior.CHARGE_BALANCER, MachineBehavior.BURST_RELEASE};
            case ARCLITE, NULLITE -> new MachineBehavior[] {MachineBehavior.CHARGE_BALANCER, MachineBehavior.BURST_RELEASE};
            case AETHERGOLD -> new MachineBehavior[] {
                    MachineBehavior.CHARGE_BALANCER,
                    MachineBehavior.BURST_RELEASE,
                    MachineBehavior.CELL_LEAKAGE_DAMPING
            };
            default -> new MachineBehavior[0];
        };
        return identity("battery_chassis." + material.getSerializedName(), Rarity.NORMAL, 0, List.of(), behaviors);
    }

    private static Identity batteryCell(BatteryCellMaterial material) {
        return identity("battery_cell." + material.getSerializedName(), Rarity.NORMAL, 0, List.of());
    }

    /** A Unique's identity: Unique rarity, no Refinement Potential, and its catalog behaviors. */
    public static Identity unique(UniqueDefinition definition) {
        return new Identity(definition.translationKey(), new MachineTraits(Rarity.UNIQUE, 0, List.of(), definition.behaviors()));
    }

    private static Identity solidFuelBurner(SolidFuelBurnerChassis chassis) {
        MachineBehavior[] behaviors = switch (chassis) {
            case COPPER -> new MachineBehavior[] {MachineBehavior.QUICK_FEED};
            case ALLOY -> new MachineBehavior[] {MachineBehavior.FUEL_RESERVE};
            default -> new MachineBehavior[0];
        };
        return identity("solid_fuel_burner." + chassis.getSerializedName(), Rarity.NORMAL, 0, List.of(), behaviors);
    }

    private static Identity solarPanel(com.rngtech.content.energy.SolarPanelMaterial material) {
        return identity("solar_panel." + material.getSerializedName(), Rarity.NORMAL, 0, List.of());
    }

    private static Identity resonanceCalibrator(ResonanceCalibratorChassis chassis) {
        return identity(
                "resonance_calibrator." + chassis.getSerializedName(),
                Rarity.NORMAL,
                0,
                List.of(),
                chassis.behaviors()
        );
    }

    private static Identity componentRecycler(ComponentRecyclerChassis chassis) {
        return identity("component_recycler." + chassis.getSerializedName(), Rarity.NORMAL, 0, List.of(), MachineBehavior.OUTPUT_GUARD);
    }

    private static Identity compressorTank(CompressorTankMaterial material) {
        return identity("compressor_tank." + material.getSerializedName(), Rarity.NORMAL, 0, List.of());
    }

    private static Identity staticMachine(String identityId, MachineBehavior... behaviors) {
        return identity(identityId, Rarity.NORMAL, 0, List.of(), behaviors);
    }

    private static String identityId(MachinePartType partType) {
        return switch (partType) {
            case VOID_CHAMBER -> "void_chamber";
            case COLLAPSE_NOZZLE -> "collapse_nozzle";
            case DIMENSIONAL_STABILIZER -> "dimensional_stabilizer";
            case REFORMING_CATALYST_BED -> "reforming_catalyst_bed";
            default -> "vacuum_collapse_part";
        };
    }

    private static Identity metalPress(Block block) {
        if (block instanceof MetalPressBlock press && press.isCrude()) {
            return staticMachine("crude_metal_press", MachineBehavior.OUTPUT_GUARD);
        }
        return staticMachine("metal_press", MachineBehavior.OUTPUT_GUARD);
    }

    private static Identity identity(
            String id,
            Rarity rarity,
            int refinementPotential,
            List<MachineModifier> modifiers,
            MachineBehavior... behaviors
    ) {
        return new Identity(
                "rngtech.identity." + id,
                new MachineTraits(rarity, refinementPotential, modifiers, Arrays.asList(behaviors))
        );
    }

    private MachineImplicitCatalog() {
    }
}
