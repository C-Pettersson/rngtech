package com.rngtech.content.registry;

import com.rngtech.RNGTech;
import com.rngtech.content.cable.EnergyConnectorTier;
import com.rngtech.content.cable.FluidConnectorTier;
import com.rngtech.content.cable.ItemConnectorTier;
import com.rngtech.content.calibration.CalibrationFamily;
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
import com.rngtech.content.energy.SolarArrayExtenderMaterial;
import com.rngtech.content.energy.SolarPanelMaterial;
import com.rngtech.content.energy.SolidFuelBurnerChassis;
import com.rngtech.content.energy.VacuumCollapsePartMaterial;
import com.rngtech.content.machine.AlloyBlendMaterial;
import com.rngtech.content.machine.AlloyCrucibleMaterial;
import com.rngtech.content.machine.AlloyFurnaceChassisMaterial;
import com.rngtech.content.machine.CompressorTankMaterial;
import com.rngtech.content.machine.CrushHeadMaterial;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.machine.FluidPumpMaterial;
import com.rngtech.content.machine.FurnaceChassisMaterial;
import com.rngtech.content.machine.ServoMaterial;
import com.rngtech.content.material.MaterialCatalog;
import com.rngtech.content.material.MaterialEnablement;
import com.rngtech.content.material.MaterialItemDefinition;
import com.rngtech.content.material.OreCatalog;
import com.rngtech.content.material.OreDefinition;
import com.rngtech.content.material.OreHost;
import com.rngtech.content.recycling.ComponentRecyclerChassis;
import com.rngtech.content.recycling.DisassemblyHeadMaterial;
import com.rngtech.content.tool.PruningShearsMaterial;
import com.rngtech.content.tool.ToolHeadFamily;
import com.rngtech.content.tool.ToolHeadMaterial;
import com.rngtech.content.tool.ToolRodMaterial;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RNGTech.MOD_ID);
    private static final Set<String> PINNED_MATERIAL_ITEMS = Set.of("crushed_iron", "crushed_gold", "crushed_copper");
    private static final Set<String> HIDDEN_PUBLIC_ALPHA_ITEMS = Set.of(
            "invar_blend",
            "sparksteel_blend"
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.rngtech"))
                    .withTabsBefore(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                    .icon(() -> ModItems.IRON_CRUSH_HEAD.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.FURNACE_MACHINE_CHASSIS.get());
                        output.accept(ModItems.WASHER_MACHINE_CHASSIS.get());
                        output.accept(ModItems.SOLID_FUEL_BURNER_MACHINE_CHASSIS.get());
                        output.accept(ModItems.AFFIX_FORGE.get());
                        output.accept(ModItems.EXOTIC_AFFIX_FORGE.get());
                        acceptDebug(output, ModItems.DEBUG_REROLLER);
                        acceptDebug(output, ModItems.DEBUG_CHEST);
                        acceptDebug(output, ModItems.DEBUG_TRASHCAN);
                        acceptDebug(output, ModItems.DEBUG_TANK);
                        acceptDebug(output, ModItems.DEBUG_BATTERY);
                        output.accept(ModItems.FURNACE.get());
                        output.accept(ModItems.CABLE.get());
                        output.accept(ModItems.UNIVERSAL_CONNECTOR.get());
                        output.accept(ModItems.AE2_NETWORK_CONNECTOR.get());
                        output.accept(ModItems.REFINED_STORAGE_NETWORK_CONNECTOR.get());
                        for (EnergyConnectorTier tier : EnergyConnectorTier.values()) {
                            if (!tier.enabled()) {
                                continue;
                            }
                            output.accept(ModItems.energyConnector(tier).get());
                        }
                        for (FluidConnectorTier tier : FluidConnectorTier.values()) {
                            if (!tier.enabled()) {
                                continue;
                            }
                            output.accept(ModItems.fluidConnector(tier).get());
                        }
                        for (ItemConnectorTier tier : ItemConnectorTier.values()) {
                            if (!tier.enabled()) {
                                continue;
                            }
                            output.accept(ModItems.itemConnector(tier).get());
                        }
                        output.accept(ModItems.WRENCH.get());
                        output.accept(ModItems.CONFIGURATOR.get());
                        output.accept(ModItems.MASTERY_REFUND.get());
                        ModItems.ASCENDANCY_SEALS.forEach(seal -> output.accept(seal.get()));
                        output.accept(ModItems.PRIMED_SEAL_CORE.get());
                        output.accept(ModItems.LUBRICATED_SEAL_CORE.get());
                        output.accept(ModItems.ADVANCED_ITEM_FILTER.get());
                        output.accept(ModItems.MINERS_COMPANION.get());
                        output.accept(ModItems.MINERS_COMPANION_MAGNET.get());
                        output.accept(ModItems.MINING_LAMP.get());
                        output.accept(ModItems.PURGE_BUCKET.get());
                        output.accept(ModItems.FORMING_HAMMER.get());
                        output.accept(ModItems.TOOL_BENCH.get());
                        output.accept(ModItems.FORESTRY_CART_STATION.get());
                        output.accept(ModItems.FORESTRY_CART.get());
                        output.accept(ModItems.TINY_ANVIL.get());
                        output.accept(ModItems.DIAMOND_TIP.get());
                        output.accept(ModItems.AFFIX_LENS_ARRAY.get());
                        output.accept(ModItems.AFFIX_MODIFIER_SOCKET.get());
                        output.accept(ModItems.AFFIX_RESONANCE_MATRIX.get());
                        for (ToolHeadFamily family : ToolHeadFamily.values()) {
                            output.accept(ModItems.modularTool(family).get());
                            for (ToolHeadMaterial material : ToolHeadMaterial.values()) {
                                output.accept(ModItems.toolHead(family, material).get());
                            }
                        }
                        for (ToolRodMaterial material : ToolRodMaterial.values()) {
                            output.accept(ModItems.toolRod(material).get());
                        }
                        for (PruningShearsMaterial material : PruningShearsMaterial.values()) {
                            output.accept(ModItems.pruningShears(material).get());
                        }
                        output.accept(ModItems.CRUDE_METAL_PRESS.get());
                        output.accept(ModItems.METAL_PRESS.get());
                        output.accept(ModItems.MELTER.get());
                        output.accept(ModItems.BATTERY_ASSEMBLER.get());
                        output.accept(ModItems.COAL_GASIFIER.get());
                        output.accept(ModItems.SYNGAS_COMBUSTOR.get());
                        output.accept(ModItems.STEAM_METHANE_REFORMER.get());
                        for (CompressorTankMaterial material : CompressorTankMaterial.values()) {
                            output.accept(ModItems.compressorTank(material).get());
                        }
                        output.accept(ModItems.LUBRICANT_BUCKET.get());
                        output.accept(ModItems.ELECTROLYTE_SOLUTION_BUCKET.get());
                        output.accept(ModItems.CARBON_EXHAUST_BUCKET.get());
                        output.accept(ModItems.SYNGAS_BUCKET.get());
                        output.accept(ModItems.METHANE_BUCKET.get());
                        output.accept(ModItems.CARBON_MONOXIDE_BUCKET.get());
                        output.accept(ModItems.NITROGEN_BUCKET.get());
                        output.accept(ModItems.HYDROGEN_BUCKET.get());
                        output.accept(ModItems.AMMONIA_BUCKET.get());
                        for (FurnaceChassisMaterial material : FurnaceChassisMaterial.values()) {
                            if (material != FurnaceChassisMaterial.PRIMITIVE) {
                                output.accept(ModItems.furnaceChassis(material).get());
                            }
                        }
                        for (AlloyFurnaceChassisMaterial material : AlloyFurnaceChassisMaterial.values()) {
                            output.accept(ModItems.alloyFurnaceChassis(material).get());
                        }
                        for (BatteryCellMaterial material : BatteryCellMaterial.values()) {
                            output.accept(ModItems.batteryCell(material).get());
                        }
                        ModItems.UNIQUE_PARTS.values().forEach(unique -> output.accept(unique.get()));
                        for (BatteryChassisMaterial material : BatteryChassisMaterial.values()) {
                            output.accept(ModItems.batteryChassis(material).get());
                        }
                        for (CrusherChassisMaterial material : CrusherChassisMaterial.values()) {
                            output.accept(ModItems.crusherChassis(material).get());
                        }
                        for (SolidFuelBurnerChassis chassis : SolidFuelBurnerChassis.values()) {
                            output.accept(ModItems.solidFuelBurner(chassis).get());
                        }
                        output.accept(ModItems.BIO_GENERATOR.get());
                        output.accept(ModItems.ALGAE_PHOTOBIOREACTOR.get());
                        output.accept(ModItems.WOODEN_COMPOSTER.get());
                        output.accept(ModItems.WATERPROOFED_PLANKS.get());
                        output.accept(ModItems.WOODEN_DEHUMIDIFIER.get());
                        output.accept(ModItems.WOODEN_DEHUMIDIFIER_FRAME.get());
                        output.accept(ModItems.SILICA_GEL_DEHUMIDIFIER.get());
                        output.accept(ModItems.SILICA_GEL_COLUMN_CASING.get());
                        output.accept(ModItems.SILICA_GEL_BEADS.get());
                        output.accept(ModItems.SATURATED_SILICA_GEL_BEADS.get());
                        output.accept(ModItems.SUPERCHARGED_SILICA_GEL_BEADS.get());
                        output.accept(ModItems.SATURATED_SUPERCHARGED_SILICA_GEL_BEADS.get());
                        for (SolarPanelMaterial material : SolarPanelMaterial.values()) {
                            output.accept(ModItems.solarPanel(material).get());
                        }
                        output.accept(ModItems.SOLAR_ARRAY_CONTROLLER.get());
                        for (SolarArrayExtenderMaterial material : SolarArrayExtenderMaterial.values()) {
                            output.accept(ModItems.solarArrayExtender(material).get());
                        }
                        output.accept(ModItems.BIO_CHAMBER.get());
                        output.accept(ModItems.COMPOST_FEEDSTOCK.get());
                        output.accept(ModItems.COMPOSTED_BIOMASS.get());
                        output.accept(ModItems.RICH_BIOMASS.get());
                        output.accept(ModItems.ALGAE_BIOMASS.get());
                        output.accept(ModItems.DENSE_ALGAE_BIOMASS.get());
                        for (ResonanceCalibratorChassis chassis : ResonanceCalibratorChassis.values()) {
                            output.accept(ModItems.resonanceCalibrator(chassis).get());
                        }
                        for (ComponentRecyclerChassis chassis : ComponentRecyclerChassis.values()) {
                            output.accept(ModItems.componentRecycler(chassis).get());
                        }
                        output.accept(ModItems.HAND_CRANK.get());
                        output.accept(ModItems.POTENTIAL_REACTOR.get());
                        output.accept(ModItems.CORROSION_CELL.get());
                        output.accept(ModItems.CAVITATION_GENERATOR.get());
                        output.accept(ModItems.VACUUM_COLLAPSE_GENERATOR.get());
                        output.accept(ModItems.CORROSION_RESIDUE.get());
                        for (CathodeMaterial material : CathodeMaterial.values()) {
                            output.accept(ModItems.anode(material).get());
                        }
                        output.accept(ModItems.GASIFICATION_RESIDUE.get());
                        output.accept(ModItems.VOID_CATALYST.get());
                        output.accept(ModItems.COLLAPSE_RESIDUE.get());
                        for (CrushHeadMaterial material : CrushHeadMaterial.values()) {
                            output.accept(ModItems.crushHead(material).get());
                        }
                        for (HeatCoreMaterial material : HeatCoreMaterial.values()) {
                            output.accept(ModItems.heatCore(material).get());
                        }
                        for (FuelBoxMaterial material : FuelBoxMaterial.values()) {
                            output.accept(ModItems.fuelBox(material).get());
                        }
                        for (ReactorChamberMaterial material : ReactorChamberMaterial.values()) {
                            output.accept(ModItems.reactorChamber(material).get());
                        }
                        for (RecoveryFilterMaterial material : RecoveryFilterMaterial.values()) {
                            output.accept(ModItems.recoveryFilter(material).get());
                        }
                        for (ContainmentLiningMaterial material : ContainmentLiningMaterial.values()) {
                            output.accept(ModItems.containmentLining(material).get());
                        }
                        for (CavitationRotorMaterial material : CavitationRotorMaterial.values()) {
                            output.accept(ModItems.cavitationRotor(material).get());
                        }
                        for (CollapseNozzleMaterial material : CollapseNozzleMaterial.values()) {
                            output.accept(ModItems.collapseNozzle(material).get());
                        }
                        output.accept(ModItems.PITTED_CAVITATION_ROTOR.get());
                        output.accept(ModItems.REFORMING_CATALYST_BED.get());
                        for (VacuumCollapsePartMaterial material : VacuumCollapsePartMaterial.values()) {
                            output.accept(ModItems.voidChamber(material).get());
                            output.accept(ModItems.dimensionalStabilizer(material).get());
                        }
                        for (ServoMaterial material : ServoMaterial.values()) {
                            output.accept(ModItems.servo(material).get());
                        }
                        for (FluidPumpMaterial material : FluidPumpMaterial.values()) {
                            output.accept(ModItems.fluidPump(material).get());
                        }
                        for (CathodeMaterial material : CathodeMaterial.values()) {
                            output.accept(ModItems.cathode(material).get());
                        }
                        for (AlloyCrucibleMaterial material : AlloyCrucibleMaterial.values()) {
                            output.accept(ModItems.alloyCrucible(material).get());
                        }
                        for (CalibrationGearMaterial material : CalibrationGearMaterial.values()) {
                            output.accept(ModItems.resonanceCoil(material).get());
                            output.accept(ModItems.controlBoard(material).get());
                            output.accept(ModItems.stabilizerMatrix(material).get());
                        }
                        for (DisassemblyHeadMaterial material : DisassemblyHeadMaterial.values()) {
                            output.accept(ModItems.disassemblyHead(material).get());
                        }
                        for (CalibrationFamily family : CalibrationFamily.values()) {
                            output.accept(ModItems.calibrationPattern(family).get());
                            output.accept(ModItems.calibratedComponent(family).get());
                        }
                        output.accept(ModItems.CALIBRATED_DIAMOND_CRYSTAL.get());
                        output.accept(ModItems.PLATE_MOLD.get());
                        output.accept(ModItems.CASING_MOLD.get());
                        output.accept(ModItems.GEAR_MOLD.get());
                        output.accept(ModItems.CIRCUIT_MOLD.get());
                        output.accept(ModItems.CONNECTOR_MOLD.get());
                        output.accept(ModItems.MALFORMED_INGOT.get());
                        output.accept(ModItems.AFFIX_INJECTOR.get());
                        output.accept(ModItems.AFFIX_MODIFIER.get());
                        output.accept(ModItems.AFFIX_UPGRADE.get());
                        output.accept(ModItems.ASCENSION_MATRIX.get());
                        output.accept(ModItems.ASCENSION_CATALYST.get());
                        output.accept(ModItems.NULLIFIER_COIL.get());
                        output.accept(ModItems.KINETIC_MODIFIER_LENS.get());
                        output.accept(ModItems.EFFICIENCY_MODIFIER_LENS.get());
                        output.accept(ModItems.POWER_MODIFIER_LENS.get());
                        output.accept(ModItems.SPEED_MODIFIER_LENS.get());
                        output.accept(ModItems.YIELD_MODIFIER_LENS.get());
                        output.accept(ModItems.STABILITY_MODIFIER_LENS.get());
                        output.accept(ModItems.CONTROL_MODIFIER_LENS.get());
                        output.accept(ModItems.CHAOS_CRYSTAL.get());
                        output.accept(ModItems.EXPANSION_CRYSTAL.get());
                        output.accept(ModItems.CONSERVATION_CRYSTAL.get());
                        output.accept(ModItems.FRUGALITY_CRYSTAL.get());
                        output.accept(ModItems.TRANSMUTATION_CRYSTAL.get());
                        output.accept(ModItems.RESONANCE_CRYSTAL.get());
                        output.accept(ModItems.DESTABILIZATION_CRYSTAL.get());
                        output.accept(ModItems.STABILIZATION_CRYSTAL.get());
                        output.accept(ModItems.NULL_CRYSTAL.get());
                        output.accept(ModItems.EXOTIC_AFFIX_CATALYST.get());
                        acceptMaterial(output, ModItems.CRUSHED_IRON);
                        acceptMaterial(output, ModItems.CRUSHED_GOLD);
                        acceptMaterial(output, ModItems.CRUSHED_COPPER);
                        for (AlloyBlendMaterial material : AlloyBlendMaterial.values()) {
                            if (!HIDDEN_PUBLIC_ALPHA_ITEMS.contains(material.itemId())
                                    && MaterialEnablement.isEnabled(material.materialId())) {
                                output.accept(ModItems.alloyBlend(material).get());
                            }
                        }
                        for (MaterialItemDefinition definition : ModItems.materialItemDefinitions()) {
                            if (!PINNED_MATERIAL_ITEMS.contains(definition.itemId())) {
                                acceptMaterial(output, definition);
                            }
                        }
                        for (OreDefinition ore : OreCatalog.ores()) {
                            for (OreHost host : ore.hosts()) {
                                if (host != OreHost.STONE) {
                                    acceptOre(output, ore, host);
                                }
                            }
                        }
                    })
                    .build()
    );

    private static void acceptMaterial(CreativeModeTab.Output output, DeferredItem<? extends Item> item) {
        String itemId = item.getId().getPath();
        if (MaterialCatalog.hasItemDefinition(itemId) && !MaterialEnablement.isEnabled(MaterialCatalog.itemDefinition(itemId))) {
            return;
        }
        output.accept(item.get());
    }

    private static void acceptMaterial(CreativeModeTab.Output output, MaterialItemDefinition definition) {
        if (MaterialEnablement.isEnabled(definition)) {
            output.accept(ModItems.materialItem(definition.itemId()).get());
        }
    }

    private static void acceptOre(CreativeModeTab.Output output, OreDefinition ore, OreHost host) {
        if (MaterialEnablement.isEnabled(ore.materialId())) {
            output.accept(ModItems.materialItem(ore.blockId(host)).get());
        }
    }

    private static void acceptDebug(CreativeModeTab.Output output, DeferredItem<? extends Item> item) {
        if (RNGTech.isDebugContentEnabled()) {
            output.accept(item.get());
        }
    }

    public static void register(IEventBus bus) {
        CREATIVE_TABS.register(bus);
    }

    private ModCreativeTabs() {
    }
}
