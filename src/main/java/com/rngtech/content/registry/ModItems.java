package com.rngtech.content.registry;

import com.rngtech.RNGTech;
import com.rngtech.content.block.MetalPressBlock;
import com.rngtech.content.cable.EnergyConnectorTier;
import com.rngtech.content.cable.FluidConnectorTier;
import com.rngtech.content.cable.ItemConnectorTier;
import com.rngtech.content.cable.NetworkBridgeType;
import com.rngtech.content.calibration.CalibrationFamily;
import com.rngtech.content.calibration.CalibrationGearMaterial;
import com.rngtech.content.calibration.ResonanceCalibratorChassis;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.energy.BatteryChassisMaterial;
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
import com.rngtech.content.item.AdvancedItemFilterItem;
import com.rngtech.content.item.AlloyCrucibleItem;
import com.rngtech.content.item.AlloyFurnaceChassisBlockItem;
import com.rngtech.content.item.AmmoniaMachineBlockItem;
import com.rngtech.content.item.AmmoniaPartItem;
import com.rngtech.content.item.AscendancySealItem;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.BatteryChassisBlockItem;
import com.rngtech.content.item.BioChamberItem;
import com.rngtech.content.item.BioGeneratorBlockItem;
import com.rngtech.content.item.CableItem;
import com.rngtech.content.item.CalibratedComponentItem;
import com.rngtech.content.item.CalibrationGearItem;
import com.rngtech.content.item.CalibrationPatternItem;
import com.rngtech.content.item.CavitationPartItem;
import com.rngtech.content.item.CollapseNozzleItem;
import com.rngtech.content.item.ComponentRecyclerBlockItem;
import com.rngtech.content.item.CompressorTankBlockItem;
import com.rngtech.content.item.ConfiguratorItem;
import com.rngtech.content.item.CorrosionCellBlockItem;
import com.rngtech.content.item.CrushHeadItem;
import com.rngtech.content.item.CrusherChassisBlockItem;
import com.rngtech.content.item.DisassemblyHeadItem;
import com.rngtech.content.item.EnergyConnectorItem;
import com.rngtech.content.item.FluidConnectorItem;
import com.rngtech.content.item.FluidPumpItem;
import com.rngtech.content.item.ForestryCartItem;
import com.rngtech.content.item.FurnaceChassisBlockItem;
import com.rngtech.content.item.GasChemistryPartItem;
import com.rngtech.content.item.ItemConnectorItem;
import com.rngtech.content.item.MachineBlockItem;
import com.rngtech.content.item.MachineChassisItem;
import com.rngtech.content.item.MalformedIngotItem;
import com.rngtech.content.item.MaterialItem;
import com.rngtech.content.item.MinersCompanionItem;
import com.rngtech.content.item.ModularToolItem;
import com.rngtech.content.item.NetworkConnectorItem;
import com.rngtech.content.item.OreBlockItem;
import com.rngtech.content.item.PotentialReactorBlockItem;
import com.rngtech.content.item.PotentialReactorPartItem;
import com.rngtech.content.item.PruningShearsItem;
import com.rngtech.content.item.PurgeBucketItem;
import com.rngtech.content.item.RefinementConsumableItem;
import com.rngtech.content.item.RefinementLensItem;
import com.rngtech.content.item.RefinementModifierItem;
import com.rngtech.content.item.ResonanceCalibratorBlockItem;
import com.rngtech.content.item.ServoItem;
import com.rngtech.content.item.SolarArrayControllerBlockItem;
import com.rngtech.content.item.SolarArrayExtenderItem;
import com.rngtech.content.item.SolarPanelBlockItem;
import com.rngtech.content.item.SolidFuelBurnerBlockItem;
import com.rngtech.content.item.SolidFuelBurnerPartItem;
import com.rngtech.content.item.ToolHeadItem;
import com.rngtech.content.item.ToolRodItem;
import com.rngtech.content.item.UniversalConnectorItem;
import com.rngtech.content.item.VacuumCollapsePartItem;
import com.rngtech.content.item.WrenchItem;
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
import com.rngtech.content.material.MaterialItemDefinition;
import com.rngtech.content.material.OreCatalog;
import com.rngtech.content.material.OreCatalog.OreVariant;
import com.rngtech.content.material.OreDefinition;
import com.rngtech.content.material.OreHost;
import com.rngtech.content.recycling.ComponentRecyclerChassis;
import com.rngtech.content.recycling.DisassemblyHeadMaterial;
import com.rngtech.content.tool.PruningShearsMaterial;
import com.rngtech.content.tool.ToolHeadFamily;
import com.rngtech.content.tool.ToolHeadMaterial;
import com.rngtech.content.tool.ToolRodMaterial;
import com.rngtech.rpg.MachineChassisType;
import com.rngtech.rpg.MachinePartType;
import com.rngtech.rpg.MachineType;
import com.rngtech.rpg.ModifierLensTag;
import com.rngtech.rpg.refinement.RefinementModifier;
import com.rngtech.rpg.refinement.RefinementOperation;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ModItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RNGTech.MOD_ID);

    public static final DeferredItem<MachineChassisItem> FURNACE_MACHINE_CHASSIS = ITEMS.register(
            "furnace_machine_chassis",
            () -> new MachineChassisItem(MachineChassisType.FURNACE, new Item.Properties())
    );
    public static final DeferredItem<MachineChassisItem> WASHER_MACHINE_CHASSIS = ITEMS.register(
            "washer_machine_chassis",
            () -> new MachineChassisItem(MachineChassisType.WASHER, new Item.Properties())
    );
    public static final DeferredItem<MachineChassisItem> SOLID_FUEL_BURNER_MACHINE_CHASSIS = ITEMS.register(
            "solid_fuel_burner_machine_chassis",
            () -> new MachineChassisItem(MachineChassisType.SOLID_FUEL_BURNER, new Item.Properties())
    );

    public static final DeferredItem<FurnaceChassisBlockItem> FURNACE = ITEMS.register(
            "furnace",
            () -> new FurnaceChassisBlockItem(
                    ModBlocks.FURNACE.get(),
                    FurnaceChassisMaterial.PRIMITIVE,
                    new Item.Properties().stacksTo(1)
            )
    );
    public static final DeferredItem<BlockItem> AFFIX_FORGE = ITEMS.register(
            "affix_forge",
            () -> new BlockItem(ModBlocks.AFFIX_FORGE.get(), new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<BlockItem> EXOTIC_AFFIX_FORGE = ITEMS.register(
            "exotic_affix_forge",
            () -> new BlockItem(ModBlocks.EXOTIC_AFFIX_FORGE.get(), new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<BlockItem> DEBUG_REROLLER = ITEMS.register(
            "debug_reroller",
            () -> new BlockItem(ModBlocks.DEBUG_REROLLER.get(), new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<BlockItem> DEBUG_CHEST = ITEMS.register(
            "debug_chest",
            () -> new BlockItem(ModBlocks.DEBUG_CHEST.get(), new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<BlockItem> DEBUG_TRASHCAN = ITEMS.register(
            "debug_trashcan",
            () -> new BlockItem(ModBlocks.DEBUG_TRASHCAN.get(), new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<BlockItem> DEBUG_TANK = ITEMS.register(
            "debug_tank",
            () -> new BlockItem(ModBlocks.DEBUG_TANK.get(), new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<BlockItem> DEBUG_BATTERY = ITEMS.register(
            "debug_battery",
            () -> new BlockItem(ModBlocks.DEBUG_BATTERY.get(), new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<BlockItem> TOOL_BENCH = ITEMS.register(
            "tool_bench",
            () -> new BlockItem(ModBlocks.TOOL_BENCH.get(), new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<BlockItem> FORESTRY_CART_STATION = ITEMS.register(
            "forestry_cart_station",
            () -> new BlockItem(ModBlocks.FORESTRY_CART_STATION.get(), new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<ForestryCartItem> FORESTRY_CART = ITEMS.register(
            "forestry_cart",
            () -> new ForestryCartItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> TINY_ANVIL = ITEMS.register(
            "tiny_anvil",
            () -> new Item(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> DIAMOND_TIP = ITEMS.register(
            "diamond_tip",
            () -> new Item(new Item.Properties())
    );
    public static final DeferredItem<Item> AFFIX_LENS_ARRAY = ITEMS.register(
            "affix_lens_array",
            () -> new Item(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> AFFIX_MODIFIER_SOCKET = ITEMS.register(
            "affix_modifier_socket",
            () -> new Item(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> AFFIX_RESONANCE_MATRIX = ITEMS.register(
            "affix_resonance_matrix",
            () -> new Item(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<CableItem> CABLE = ITEMS.register(
            "cable",
            () -> new CableItem(ModBlocks.CABLE.get(), new Item.Properties())
    );
    public static final DeferredItem<UniversalConnectorItem> UNIVERSAL_CONNECTOR = ITEMS.register(
            "universal_connector",
            () -> new UniversalConnectorItem(ModBlocks.UNIVERSAL_CONNECTOR.get(), new Item.Properties())
    );
    public static final DeferredItem<NetworkConnectorItem> AE2_NETWORK_CONNECTOR = ITEMS.register(
            "ae2_network_connector",
            () -> new NetworkConnectorItem(NetworkBridgeType.AE2, new Item.Properties())
    );
    public static final DeferredItem<NetworkConnectorItem> REFINED_STORAGE_NETWORK_CONNECTOR = ITEMS.register(
            "refined_storage_network_connector",
            () -> new NetworkConnectorItem(NetworkBridgeType.REFINED_STORAGE, new Item.Properties())
    );
    public static final DeferredItem<WrenchItem> WRENCH = ITEMS.register(
            "wrench",
            () -> new WrenchItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> MASTERY_REFUND = ITEMS.registerSimpleItem("mastery_refund");
    public static final DeferredItem<AscendancySealItem> ASCENDANCY_SEAL_1 = registerAscendancySeal(1, Rarity.UNCOMMON);
    public static final DeferredItem<AscendancySealItem> ASCENDANCY_SEAL_2 = registerAscendancySeal(2, Rarity.RARE);
    public static final DeferredItem<AscendancySealItem> ASCENDANCY_SEAL_3 = registerAscendancySeal(3, Rarity.EPIC);
    public static final List<DeferredItem<AscendancySealItem>> ASCENDANCY_SEALS = List.of(ASCENDANCY_SEAL_1, ASCENDANCY_SEAL_2, ASCENDANCY_SEAL_3);
    public static final DeferredItem<Item> PRIMED_SEAL_CORE = ITEMS.registerSimpleItem("primed_seal_core");
    public static final DeferredItem<Item> LUBRICATED_SEAL_CORE = ITEMS.registerSimpleItem("lubricated_seal_core");

    public static final DeferredItem<ConfiguratorItem> CONFIGURATOR = ITEMS.register(
            "configurator",
            () -> new ConfiguratorItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<AdvancedItemFilterItem> ADVANCED_ITEM_FILTER = ITEMS.register(
            "advanced_item_filter",
            () -> new AdvancedItemFilterItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<MinersCompanionItem> MINERS_COMPANION = ITEMS.register(
            "miners_companion",
            () -> new MinersCompanionItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> MINERS_COMPANION_MAGNET = ITEMS.register(
            "miners_companion_magnet",
            () -> new Item(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> MINING_LAMP = ITEMS.register(
            "mining_lamp",
            () -> new Item(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<PurgeBucketItem> PURGE_BUCKET = ITEMS.register(
            "purge_bucket",
            () -> new PurgeBucketItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> FORMING_HAMMER = ITEMS.register(
            "forming_hammer",
            () -> new Item(new Item.Properties().durability(30))
    );
    public static final DeferredItem<PotentialReactorBlockItem> POTENTIAL_REACTOR = ITEMS.register(
            "potential_reactor",
            () -> new PotentialReactorBlockItem(ModBlocks.POTENTIAL_REACTOR.get(), new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<MachineBlockItem> CAVITATION_GENERATOR = ITEMS.register(
            "cavitation_generator",
            () -> new MachineBlockItem(ModBlocks.CAVITATION_GENERATOR.get(), MachineType.CAVITATION_GENERATOR, new Item.Properties().stacksTo(1)) {
                @Override
                protected int componentStage() {
                    return 5;
                }
            }
    );
    public static final DeferredItem<CorrosionCellBlockItem> CORROSION_CELL = ITEMS.register(
            "corrosion_cell",
            () -> new CorrosionCellBlockItem(ModBlocks.CORROSION_CELL.get(), new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<MachineBlockItem> VACUUM_COLLAPSE_GENERATOR = ITEMS.register(
            "vacuum_collapse_generator",
            () -> new MachineBlockItem(
                    ModBlocks.VACUUM_COLLAPSE_GENERATOR.get(),
                    MachineType.VACUUM_COLLAPSE_GENERATOR,
                    new Item.Properties().stacksTo(1)
            ) {
                @Override
                protected int componentStage() {
                    return 7;
                }
            }
    );
    public static final DeferredItem<Item> CORROSION_RESIDUE = ITEMS.register(
            "corrosion_residue",
            () -> new Item(new Item.Properties())
    );
    public static final DeferredItem<MachineBlockItem> CRUDE_METAL_PRESS = ITEMS.register(
            "crude_metal_press",
            () -> new MachineBlockItem(ModBlocks.CRUDE_METAL_PRESS.get(), MachineType.METAL_PRESS, new Item.Properties().stacksTo(1)) {
                @Override
                protected int componentStage() {
                    return 3;
                }

                @Override
                protected int modifierRollComponentStage() {
                    return MetalPressBlock.MODIFIER_ROLL_COMPONENT_STAGE;
                }
            }
    );
    public static final DeferredItem<MachineBlockItem> METAL_PRESS = ITEMS.register(
            "metal_press",
            () -> new MachineBlockItem(ModBlocks.METAL_PRESS.get(), MachineType.METAL_PRESS, new Item.Properties().stacksTo(1)) {
                @Override
                protected int componentStage() {
                    return 4;
                }

                @Override
                protected int modifierRollComponentStage() {
                    return MetalPressBlock.MODIFIER_ROLL_COMPONENT_STAGE;
                }
            }
    );
    public static final DeferredItem<MachineBlockItem> MELTER = ITEMS.register(
            "melter",
            () -> new MachineBlockItem(ModBlocks.MELTER.get(), MachineType.MELTER, new Item.Properties().stacksTo(1)) {
                @Override
                protected int componentStage() {
                    return 6;
                }
            }
    );
    public static final DeferredItem<MachineBlockItem> BATTERY_ASSEMBLER = ITEMS.register(
            "battery_assembler",
            () -> new MachineBlockItem(
                    ModBlocks.BATTERY_ASSEMBLER.get(),
                    MachineType.BATTERY_ASSEMBLER,
                    new Item.Properties().stacksTo(1)
            ) {
                @Override
                protected int componentStage() {
                    return 2;
                }
            }
    );
    public static final DeferredItem<MachineBlockItem> COAL_GASIFIER = ITEMS.register(
            "coal_gasifier",
            () -> new MachineBlockItem(ModBlocks.COAL_GASIFIER.get(), MachineType.COAL_GASIFIER, new Item.Properties().stacksTo(1)) {
                @Override
                protected int componentStage() {
                    return 5;
                }
            }
    );
    public static final DeferredItem<MachineBlockItem> SYNGAS_COMBUSTOR = ITEMS.register(
            "syngas_combustor",
            () -> new MachineBlockItem(ModBlocks.SYNGAS_COMBUSTOR.get(), MachineType.SYNGAS_COMBUSTOR, new Item.Properties().stacksTo(1)) {
                @Override
                protected int componentStage() {
                    return 5;
                }
            }
    );
    public static final DeferredItem<MachineBlockItem> STEAM_METHANE_REFORMER = ITEMS.register(
            "steam_methane_reformer",
            () -> new MachineBlockItem(
                    ModBlocks.STEAM_METHANE_REFORMER.get(),
                    MachineType.STEAM_METHANE_REFORMER,
                    new Item.Properties().stacksTo(1)
            ) {
                @Override
                protected int componentStage() {
                    return 6;
                }
            }
    );
    public static final DeferredItem<Item> GASIFICATION_RESIDUE = ITEMS.register(
            "gasification_residue",
            () -> new Item(new Item.Properties())
    );
    public static final DeferredItem<AmmoniaMachineBlockItem> AMMONIA_SYNTHESIZER = ITEMS.register(
            "ammonia_synthesizer",
            () -> new AmmoniaMachineBlockItem(
                    ModBlocks.AMMONIA_SYNTHESIZER.get(),
                    MachineType.AMMONIA_SYNTHESIZER,
                    new Item.Properties().stacksTo(1)
            )
    );
    public static final DeferredItem<AmmoniaMachineBlockItem> AMMONIA_FUEL_CELL = ITEMS.register(
            "ammonia_fuel_cell",
            () -> new AmmoniaMachineBlockItem(
                    ModBlocks.AMMONIA_FUEL_CELL.get(),
                    MachineType.AMMONIA_FUEL_CELL,
                    new Item.Properties().stacksTo(1)
            )
    );
    public static final DeferredItem<AmmoniaPartItem> AMMONIA_CATALYST_BED = ITEMS.register(
            "ammonia_catalyst_bed",
            () -> new AmmoniaPartItem(
                    MachinePartType.AMMONIA_CATALYST_BED,
                    MachineType.AMMONIA_SYNTHESIZER,
                    6,
                    new Item.Properties().stacksTo(1)
            )
    );
    public static final DeferredItem<AmmoniaPartItem> FUEL_CELL_MEMBRANE = ITEMS.register(
            "fuel_cell_membrane",
            () -> new AmmoniaPartItem(
                    MachinePartType.FUEL_CELL_MEMBRANE,
                    MachineType.AMMONIA_FUEL_CELL,
                    6,
                    new Item.Properties().stacksTo(1)
            )
    );
    public static final DeferredItem<GasChemistryPartItem> REFORMING_CATALYST_BED = ITEMS.register(
            "reforming_catalyst_bed",
            () -> new GasChemistryPartItem(
                    MachinePartType.REFORMING_CATALYST_BED,
                    MachineType.STEAM_METHANE_REFORMER,
                    6,
                    new Item.Properties().stacksTo(1)
            )
    );
    public static final DeferredItem<BucketItem> LUBRICANT_BUCKET = ITEMS.register(
            "lubricant_bucket",
            () -> new BucketItem(
                    ModFluids.LUBRICANT_SOURCE.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)
            )
    );
    public static final DeferredItem<BucketItem> ELECTROLYTE_SOLUTION_BUCKET = ITEMS.register(
            "electrolyte_solution_bucket",
            () -> new BucketItem(
                    ModFluids.ELECTROLYTE_SOLUTION_SOURCE.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)
            )
    );
    public static final DeferredItem<BucketItem> CARBON_EXHAUST_BUCKET = ITEMS.register(
            "carbon_exhaust_bucket",
            () -> new BucketItem(
                    ModFluids.CARBON_EXHAUST_SOURCE.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)
            )
    );
    public static final DeferredItem<BucketItem> SYNGAS_BUCKET = ITEMS.register(
            "syngas_bucket",
            () -> new BucketItem(
                    ModFluids.SYNGAS_SOURCE.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)
            )
    );
    public static final DeferredItem<BucketItem> METHANE_BUCKET = ITEMS.register(
            "methane_bucket",
            () -> new BucketItem(
                    ModFluids.METHANE_SOURCE.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)
            )
    );
    public static final DeferredItem<BucketItem> CARBON_MONOXIDE_BUCKET = ITEMS.register(
            "carbon_monoxide_bucket",
            () -> new BucketItem(
                    ModFluids.CARBON_MONOXIDE_SOURCE.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)
            )
    );
    public static final DeferredItem<BucketItem> NITROGEN_BUCKET = ITEMS.register(
            "nitrogen_bucket",
            () -> new BucketItem(
                    ModFluids.NITROGEN_SOURCE.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)
            )
    );
    public static final DeferredItem<BucketItem> HYDROGEN_BUCKET = ITEMS.register(
            "hydrogen_bucket",
            () -> new BucketItem(
                    ModFluids.HYDROGEN_SOURCE.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)
            )
    );
    public static final DeferredItem<BucketItem> AMMONIA_BUCKET = ITEMS.register(
            "ammonia_bucket",
            () -> new BucketItem(
                    ModFluids.AMMONIA_SOURCE.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)
            )
    );
    public static final DeferredItem<Item> PLATE_MOLD = ITEMS.register(
            "plate_mold",
            () -> new Item(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> CASING_MOLD = ITEMS.register(
            "casing_mold",
            () -> new Item(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> GEAR_MOLD = ITEMS.register(
            "gear_mold",
            () -> new Item(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> CIRCUIT_MOLD = ITEMS.register(
            "circuit_mold",
            () -> new Item(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> CONNECTOR_MOLD = ITEMS.register(
            "connector_mold",
            () -> new Item(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<MalformedIngotItem> MALFORMED_INGOT = ITEMS.register(
            "malformed_ingot",
            () -> new MalformedIngotItem(new Item.Properties())
    );
    public static final Map<EnergyConnectorTier, DeferredItem<EnergyConnectorItem>> ENERGY_CONNECTORS =
            registerEnergyConnectors();
    public static final Map<FluidConnectorTier, DeferredItem<FluidConnectorItem>> FLUID_CONNECTORS =
            registerFluidConnectors();
    public static final Map<ItemConnectorTier, DeferredItem<ItemConnectorItem>> ITEM_CONNECTORS =
            registerItemConnectors();
    public static final Map<BatteryCellMaterial, DeferredItem<BatteryCellItem>> BATTERY_CELLS = registerBatteryCells();
    public static final Map<ToolHeadFamily, Map<ToolHeadMaterial, DeferredItem<ToolHeadItem>>> TOOL_HEADS =
            registerToolHeads();
    public static final Map<ToolRodMaterial, DeferredItem<ToolRodItem>> TOOL_RODS = registerToolRods();
    public static final Map<ToolHeadFamily, DeferredItem<ModularToolItem>> MODULAR_TOOLS = registerModularTools();
    public static final Map<PruningShearsMaterial, DeferredItem<PruningShearsItem>> PRUNING_SHEARS =
            registerPruningShears();
    public static final Map<BatteryChassisMaterial, DeferredItem<BatteryChassisBlockItem>> BATTERY_CHASSIS =
            registerBatteryChassisItems();
    public static final Map<CrusherChassisMaterial, DeferredItem<CrusherChassisBlockItem>> CRUSHER_CHASSIS =
            registerCrusherChassisItems();
    public static final Map<FurnaceChassisMaterial, DeferredItem<FurnaceChassisBlockItem>> FURNACE_CHASSIS =
            registerFurnaceChassisItems();
    public static final Map<AlloyFurnaceChassisMaterial, DeferredItem<AlloyFurnaceChassisBlockItem>> ALLOY_FURNACE_CHASSIS =
            registerAlloyFurnaceChassisItems();
    public static final Map<SolidFuelBurnerChassis, DeferredItem<SolidFuelBurnerBlockItem>> SOLID_FUEL_BURNERS =
            registerSolidFuelBurnerItems();
    public static final DeferredItem<BioGeneratorBlockItem> BIO_GENERATOR = ITEMS.register(
            "bio_generator",
            () -> new BioGeneratorBlockItem(ModBlocks.BIO_GENERATOR.get(), new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<BlockItem> ALGAE_PHOTOBIOREACTOR = ITEMS.register(
            "algae_photobioreactor",
            () -> new BlockItem(ModBlocks.ALGAE_PHOTOBIOREACTOR.get(), new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<BlockItem> WOODEN_COMPOSTER = ITEMS.register(
            "wooden_composter",
            () -> new BlockItem(ModBlocks.WOODEN_COMPOSTER.get(), new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> WATERPROOFED_PLANKS = ITEMS.register(
            "waterproofed_planks",
            () -> new Item(new Item.Properties())
    );
    public static final DeferredItem<BlockItem> WOODEN_DEHUMIDIFIER = ITEMS.register(
            "wooden_dehumidifier",
            () -> new BlockItem(ModBlocks.WOODEN_DEHUMIDIFIER.get(), new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<BlockItem> WOODEN_DEHUMIDIFIER_FRAME = ITEMS.register(
            "wooden_dehumidifier_frame",
            () -> new BlockItem(ModBlocks.WOODEN_DEHUMIDIFIER_FRAME.get(), new Item.Properties())
    );
    public static final DeferredItem<BlockItem> SILICA_GEL_DEHUMIDIFIER = ITEMS.register(
            "silica_gel_dehumidifier",
            () -> new BlockItem(ModBlocks.SILICA_GEL_DEHUMIDIFIER.get(), new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<BlockItem> SILICA_GEL_COLUMN_CASING = ITEMS.register(
            "silica_gel_column_casing",
            () -> new BlockItem(ModBlocks.SILICA_GEL_COLUMN_CASING.get(), new Item.Properties())
    );
    public static final DeferredItem<Item> SILICA_GEL_BEADS = ITEMS.register(
            "silica_gel_beads",
            () -> new Item(new Item.Properties())
    );
    public static final DeferredItem<Item> SATURATED_SILICA_GEL_BEADS = ITEMS.register(
            "saturated_silica_gel_beads",
            () -> new Item(new Item.Properties())
    );
    public static final DeferredItem<Item> SUPERCHARGED_SILICA_GEL_BEADS = ITEMS.register(
            "supercharged_silica_gel_beads",
            () -> new Item(new Item.Properties())
    );
    public static final DeferredItem<Item> SATURATED_SUPERCHARGED_SILICA_GEL_BEADS = ITEMS.register(
            "saturated_supercharged_silica_gel_beads",
            () -> new Item(new Item.Properties())
    );
    public static final Map<SolarPanelMaterial, DeferredItem<SolarPanelBlockItem>> SOLAR_PANELS =
            registerSolarPanelItems();
    public static final DeferredItem<SolarArrayControllerBlockItem> SOLAR_ARRAY_CONTROLLER = ITEMS.register(
            "solar_array_controller",
            () -> new SolarArrayControllerBlockItem(
                    ModBlocks.SOLAR_ARRAY_CONTROLLER.get(),
                    new Item.Properties().stacksTo(1)
            )
    );
    public static final Map<SolarArrayExtenderMaterial, DeferredItem<SolarArrayExtenderItem>> SOLAR_ARRAY_EXTENDERS =
            registerSolarArrayExtenders();
    public static final DeferredItem<BioChamberItem> BIO_CHAMBER = ITEMS.register(
            "bio_chamber",
            () -> new BioChamberItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> COMPOST_FEEDSTOCK = ITEMS.register(
            "compost_feedstock",
            () -> new Item(new Item.Properties())
    );
    public static final DeferredItem<Item> COMPOSTED_BIOMASS = ITEMS.register(
            "composted_biomass",
            () -> new Item(new Item.Properties())
    );
    public static final DeferredItem<Item> RICH_BIOMASS = ITEMS.register(
            "rich_biomass",
            () -> new Item(new Item.Properties())
    );
    public static final DeferredItem<Item> ALGAE_BIOMASS = ITEMS.register(
            "algae_biomass",
            () -> new Item(new Item.Properties())
    );
    public static final DeferredItem<Item> DENSE_ALGAE_BIOMASS = ITEMS.register(
            "dense_algae_biomass",
            () -> new Item(new Item.Properties())
    );
    public static final DeferredItem<Item> VOID_CATALYST = ITEMS.register(
            "void_catalyst",
            () -> new Item(new Item.Properties())
    );
    public static final DeferredItem<Item> COLLAPSE_RESIDUE = ITEMS.register(
            "collapse_residue",
            () -> new Item(new Item.Properties())
    );
    public static final Map<ResonanceCalibratorChassis, DeferredItem<ResonanceCalibratorBlockItem>> RESONANCE_CALIBRATORS =
            registerResonanceCalibratorItems();
    public static final Map<ComponentRecyclerChassis, DeferredItem<ComponentRecyclerBlockItem>> COMPONENT_RECYCLERS =
            registerComponentRecyclerItems();
    public static final DeferredItem<BlockItem> HAND_CRANK = ITEMS.register(
            "hand_crank",
            () -> new BlockItem(ModBlocks.HAND_CRANK.get(), new Item.Properties())
    );
    public static final Map<CompressorTankMaterial, DeferredItem<CompressorTankBlockItem>> COMPRESSOR_TANKS =
            registerCompressorTankItems();
    public static final Map<DisassemblyHeadMaterial, DeferredItem<DisassemblyHeadItem>> DISASSEMBLY_HEADS =
            registerDisassemblyHeads();
    public static final Map<HeatCoreMaterial, DeferredItem<SolidFuelBurnerPartItem>> HEAT_CORES =
            registerHeatCores();
    public static final Map<FuelBoxMaterial, DeferredItem<SolidFuelBurnerPartItem>> FUEL_BOXES = registerFuelBoxes();
    public static final Map<ReactorChamberMaterial, DeferredItem<PotentialReactorPartItem>> REACTOR_CHAMBERS =
            registerReactorChambers();
    public static final Map<RecoveryFilterMaterial, DeferredItem<PotentialReactorPartItem>> RECOVERY_FILTERS =
            registerRecoveryFilters();
    public static final Map<ContainmentLiningMaterial, DeferredItem<PotentialReactorPartItem>> CONTAINMENT_LININGS =
            registerContainmentLinings();
    public static final Map<CavitationRotorMaterial, DeferredItem<CavitationPartItem>> CAVITATION_ROTORS =
            registerCavitationRotors();
    public static final Map<CollapseNozzleMaterial, DeferredItem<CollapseNozzleItem>> COLLAPSE_NOZZLES =
            registerCollapseNozzles();
    public static final DeferredItem<Item> PITTED_CAVITATION_ROTOR = ITEMS.register(
            "pitted_cavitation_rotor",
            () -> new Item(new Item.Properties())
    );
    public static final Map<VacuumCollapsePartMaterial, DeferredItem<VacuumCollapsePartItem>> VOID_CHAMBERS =
            registerVacuumCollapseParts("void_chamber", MachinePartType.VOID_CHAMBER);
    public static final Map<VacuumCollapsePartMaterial, DeferredItem<VacuumCollapsePartItem>> DIMENSIONAL_STABILIZERS =
            registerVacuumCollapseParts("dimensional_stabilizer", MachinePartType.DIMENSIONAL_STABILIZER);
    public static final Map<ServoMaterial, DeferredItem<ServoItem>> SERVOS = registerServos();
    public static final DeferredItem<ServoItem> STEEL_SERVO = servo(ServoMaterial.STEEL);
    public static final Map<FluidPumpMaterial, DeferredItem<FluidPumpItem>> FLUID_PUMPS = registerFluidPumps();
    public static final Map<AlloyCrucibleMaterial, DeferredItem<AlloyCrucibleItem>> ALLOY_CRUCIBLES =
            registerAlloyCrucibles();
    public static final Map<CalibrationGearMaterial, DeferredItem<CalibrationGearItem>> RESONANCE_COILS =
            registerCalibrationGear("resonance_coil", MachinePartType.RESONANCE_COIL);
    public static final Map<CalibrationGearMaterial, DeferredItem<CalibrationGearItem>> CONTROL_BOARDS =
            registerCalibrationGear("control_board", MachinePartType.CONTROL_BOARD);
    public static final Map<CalibrationGearMaterial, DeferredItem<CalibrationGearItem>> STABILIZER_MATRICES =
            registerCalibrationGear("stabilizer_matrix", MachinePartType.STABILIZER_MATRIX);
    public static final Map<CalibrationFamily, DeferredItem<CalibrationPatternItem>> CALIBRATION_PATTERNS =
            registerCalibrationPatterns();
    public static final Map<CalibrationFamily, DeferredItem<CalibratedComponentItem>> CALIBRATED_COMPONENTS =
            registerCalibratedComponents();
    public static final DeferredItem<CalibratedComponentItem> CALIBRATED_DIAMOND_CRYSTAL = ITEMS.register(
            "calibrated_diamond_crystal",
            () -> new CalibratedComponentItem(CalibrationFamily.LOGIC, new Item.Properties())
    );

    public static final Map<CrushHeadMaterial, DeferredItem<CrushHeadItem>> CRUSH_HEADS = registerCrushHeads();
    public static final DeferredItem<CrushHeadItem> FLINT_CRUSH_HEAD = crushHead(CrushHeadMaterial.FLINT);
    public static final DeferredItem<CrushHeadItem> IRON_CRUSH_HEAD = crushHead(CrushHeadMaterial.IRON);

    public static final DeferredItem<RefinementConsumableItem> AFFIX_INJECTOR = ITEMS.register(
            "affix_injector",
            () -> new RefinementConsumableItem(RefinementOperation.ADD_MODIFIER, new Item.Properties())
    );
    public static final DeferredItem<RefinementConsumableItem> AFFIX_MODIFIER = ITEMS.register(
            "affix_modifier",
            () -> new RefinementConsumableItem(RefinementOperation.UPGRADE_RANDOM_MODIFIER, new Item.Properties())
    );
    public static final DeferredItem<RefinementConsumableItem> AFFIX_UPGRADE = ITEMS.register(
            "affix_upgrade",
            () -> new RefinementConsumableItem(RefinementOperation.UPGRADE_SELECTED_MODIFIER, new Item.Properties())
    );
    public static final DeferredItem<RefinementConsumableItem> ASCENSION_MATRIX = ITEMS.register(
            "ascension_matrix",
            () -> new RefinementConsumableItem(RefinementOperation.ASCEND_RARITY, new Item.Properties())
    );
    public static final DeferredItem<RefinementConsumableItem> ASCENSION_CATALYST = ITEMS.register(
            "ascension_catalyst",
            () -> new RefinementConsumableItem(RefinementOperation.ASCENSION_CATALYST, new Item.Properties())
    );
    public static final DeferredItem<RefinementConsumableItem> NULLIFIER_COIL = ITEMS.register(
            "nullifier_coil",
            () -> new RefinementConsumableItem(RefinementOperation.REMOVE_MODIFIER, new Item.Properties())
    );
    public static final DeferredItem<RefinementLensItem> KINETIC_MODIFIER_LENS = ITEMS.register(
            "kinetic_modifier_lens",
            () -> new RefinementLensItem(Set.of(ModifierLensTag.KINETIC), new Item.Properties())
    );
    public static final DeferredItem<RefinementLensItem> EFFICIENCY_MODIFIER_LENS = ITEMS.register(
            "efficiency_modifier_lens",
            () -> new RefinementLensItem(Set.of(ModifierLensTag.EFFICIENCY), new Item.Properties())
    );
    public static final DeferredItem<RefinementLensItem> POWER_MODIFIER_LENS = ITEMS.register(
            "power_modifier_lens",
            () -> new RefinementLensItem(Set.of(ModifierLensTag.POWER), new Item.Properties())
    );
    public static final DeferredItem<RefinementLensItem> SPEED_MODIFIER_LENS = ITEMS.register(
            "speed_modifier_lens",
            () -> new RefinementLensItem(Set.of(ModifierLensTag.SPEED), new Item.Properties())
    );
    public static final DeferredItem<RefinementLensItem> YIELD_MODIFIER_LENS = ITEMS.register(
            "yield_modifier_lens",
            () -> new RefinementLensItem(Set.of(ModifierLensTag.YIELD), new Item.Properties())
    );
    public static final DeferredItem<RefinementLensItem> STABILITY_MODIFIER_LENS = ITEMS.register(
            "stability_modifier_lens",
            () -> new RefinementLensItem(Set.of(ModifierLensTag.STABILITY), new Item.Properties())
    );
    public static final DeferredItem<RefinementLensItem> CONTROL_MODIFIER_LENS = ITEMS.register(
            "control_modifier_lens",
            () -> new RefinementLensItem(Set.of(ModifierLensTag.CONTROL), new Item.Properties())
    );
    public static final DeferredItem<RefinementConsumableItem> CHAOS_CRYSTAL = ITEMS.register(
            "chaos_crystal",
            () -> new RefinementConsumableItem(RefinementOperation.CHAOS_CRYSTAL, new Item.Properties())
    );
    public static final DeferredItem<RefinementConsumableItem> EXPANSION_CRYSTAL = ITEMS.register(
            "expansion_crystal",
            () -> new RefinementConsumableItem(RefinementOperation.EXPANSION_CRYSTAL, new Item.Properties())
    );
    public static final DeferredItem<RefinementModifierItem> CONSERVATION_CRYSTAL = ITEMS.register(
            "conservation_crystal",
            () -> new RefinementModifierItem(RefinementModifier.CONSERVE_CATALYST, new Item.Properties())
    );
    public static final DeferredItem<RefinementModifierItem> FRUGALITY_CRYSTAL = ITEMS.register(
            "frugality_crystal",
            () -> new RefinementModifierItem(RefinementModifier.REDUCE_POTENTIAL_COST, new Item.Properties())
    );
    public static final DeferredItem<RefinementModifierItem> TRANSMUTATION_CRYSTAL = ITEMS.register(
            "transmutation_crystal",
            () -> new RefinementModifierItem(RefinementModifier.TRANSMUTE_UPGRADE, new Item.Properties())
    );
    public static final DeferredItem<RefinementModifierItem> RESONANCE_CRYSTAL = ITEMS.register(
            "resonance_crystal",
            () -> new RefinementModifierItem(RefinementModifier.PRESERVE_ROLL, new Item.Properties())
    );
    public static final DeferredItem<RefinementModifierItem> DESTABILIZATION_CRYSTAL = ITEMS.register(
            "destabilization_crystal",
            () -> new RefinementModifierItem(RefinementModifier.DESTABILIZE_OTHERS, new Item.Properties())
    );
    public static final DeferredItem<RefinementModifierItem> STABILIZATION_CRYSTAL = ITEMS.register(
            "stabilization_crystal",
            () -> new RefinementModifierItem(RefinementModifier.CORRUPTION_WARD, new Item.Properties())
    );
    public static final DeferredItem<RefinementConsumableItem> NULL_CRYSTAL = ITEMS.register(
            "null_crystal",
            () -> new RefinementConsumableItem(RefinementOperation.NULL_CRYSTAL, new Item.Properties())
    );
    public static final DeferredItem<Item> EXOTIC_AFFIX_CATALYST = ITEMS.register(
            "exotic_affix_catalyst",
            () -> new Item(new Item.Properties())
    );

    public static final Map<String, DeferredItem<Item>> MATERIAL_ITEMS = registerMaterialItems();
    public static final Map<AlloyBlendMaterial, DeferredItem<Item>> ALLOY_BLENDS = registerAlloyBlends();

    public static final DeferredItem<Item> CRUSHED_IRON = materialItem("crushed_iron");
    public static final DeferredItem<Item> CRUSHED_GOLD = materialItem("crushed_gold");
    public static final DeferredItem<Item> CRUSHED_COPPER = materialItem("crushed_copper");

    public static List<DeferredItem<RefinementLensItem>> modifierLenses() {
        return List.of(
                KINETIC_MODIFIER_LENS,
                EFFICIENCY_MODIFIER_LENS,
                POWER_MODIFIER_LENS,
                SPEED_MODIFIER_LENS,
                YIELD_MODIFIER_LENS,
                STABILITY_MODIFIER_LENS,
                CONTROL_MODIFIER_LENS
        );
    }

    /** The Seal of a tier from 1 to {@value com.rngtech.rpg.progression.AscendancyCatalog#MAX_TIERS}. */
    public static DeferredItem<AscendancySealItem> ascendancySeal(int tier) {
        return ASCENDANCY_SEALS.get(tier - 1);
    }

    private static DeferredItem<AscendancySealItem> registerAscendancySeal(int tier, Rarity rarity) {
        return ITEMS.register("ascendancy_seal_" + tier, () -> new AscendancySealItem(tier, new Item.Properties().stacksTo(16).rarity(rarity)));
    }

    public static DeferredItem<Item> materialItem(String itemId) {
        DeferredItem<Item> item = MATERIAL_ITEMS.get(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Unknown material item: " + itemId);
        }
        return item;
    }

    public static DeferredItem<EnergyConnectorItem> energyConnector(EnergyConnectorTier tier) {
        DeferredItem<EnergyConnectorItem> item = ENERGY_CONNECTORS.get(tier);
        if (item == null) {
            throw new IllegalArgumentException("Unknown energy connector tier: " + tier);
        }
        return item;
    }

    public static DeferredItem<FluidConnectorItem> fluidConnector(FluidConnectorTier tier) {
        DeferredItem<FluidConnectorItem> item = FLUID_CONNECTORS.get(tier);
        if (item == null) {
            throw new IllegalArgumentException("Unknown fluid connector tier: " + tier);
        }
        return item;
    }

    public static DeferredItem<ItemConnectorItem> itemConnector(ItemConnectorTier tier) {
        DeferredItem<ItemConnectorItem> item = ITEM_CONNECTORS.get(tier);
        if (item == null) {
            throw new IllegalArgumentException("Unknown item connector tier: " + tier);
        }
        return item;
    }

    public static DeferredItem<BatteryCellItem> batteryCell(BatteryCellMaterial material) {
        DeferredItem<BatteryCellItem> item = BATTERY_CELLS.get(material);
        if (item == null) {
            throw new IllegalArgumentException("Unknown battery cell material: " + material);
        }
        return item;
    }

    public static DeferredItem<ToolHeadItem> toolHead(ToolHeadFamily family, ToolHeadMaterial material) {
        Map<ToolHeadMaterial, DeferredItem<ToolHeadItem>> familyItems = TOOL_HEADS.get(family);
        if (familyItems == null || !familyItems.containsKey(material)) {
            throw new IllegalArgumentException("Unknown tool head: " + family + " / " + material);
        }
        return familyItems.get(material);
    }

    public static DeferredItem<ToolRodItem> toolRod(ToolRodMaterial material) {
        DeferredItem<ToolRodItem> item = TOOL_RODS.get(material);
        if (item == null) {
            throw new IllegalArgumentException("Unknown tool rod material: " + material);
        }
        return item;
    }

    public static DeferredItem<ModularToolItem> modularTool(ToolHeadFamily family) {
        DeferredItem<ModularToolItem> item = MODULAR_TOOLS.get(family);
        if (item == null) {
            throw new IllegalArgumentException("Unknown modular tool family: " + family);
        }
        return item;
    }

    public static DeferredItem<PruningShearsItem> pruningShears(PruningShearsMaterial material) {
        DeferredItem<PruningShearsItem> item = PRUNING_SHEARS.get(material);
        if (item == null) {
            throw new IllegalArgumentException("Unknown pruning shears material: " + material);
        }
        return item;
    }

    public static DeferredItem<BatteryChassisBlockItem> batteryChassis(BatteryChassisMaterial material) {
        DeferredItem<BatteryChassisBlockItem> item = BATTERY_CHASSIS.get(material);
        if (item == null) {
            throw new IllegalArgumentException("Unknown battery chassis material: " + material);
        }
        return item;
    }

    public static DeferredItem<CrusherChassisBlockItem> crusherChassis(CrusherChassisMaterial material) {
        DeferredItem<CrusherChassisBlockItem> item = CRUSHER_CHASSIS.get(material);
        if (item == null) {
            throw new IllegalArgumentException("Unknown crusher chassis material: " + material);
        }
        return item;
    }

    public static DeferredItem<FurnaceChassisBlockItem> furnaceChassis(FurnaceChassisMaterial material) {
        if (material == FurnaceChassisMaterial.PRIMITIVE) {
            return FURNACE;
        }
        DeferredItem<FurnaceChassisBlockItem> item = FURNACE_CHASSIS.get(material);
        if (item == null) {
            throw new IllegalArgumentException("Unknown furnace chassis material: " + material);
        }
        return item;
    }

    public static DeferredItem<AlloyFurnaceChassisBlockItem> alloyFurnaceChassis(AlloyFurnaceChassisMaterial material) {
        DeferredItem<AlloyFurnaceChassisBlockItem> item = ALLOY_FURNACE_CHASSIS.get(material);
        if (item == null) {
            throw new IllegalArgumentException("Unknown alloy furnace chassis material: " + material);
        }
        return item;
    }

    public static DeferredItem<CrushHeadItem> crushHead(CrushHeadMaterial material) {
        DeferredItem<CrushHeadItem> item = CRUSH_HEADS.get(material);
        if (item == null) {
            throw new IllegalArgumentException("Unknown crush head material: " + material);
        }
        return item;
    }

    public static DeferredItem<SolidFuelBurnerBlockItem> solidFuelBurner(SolidFuelBurnerChassis chassis) {
        DeferredItem<SolidFuelBurnerBlockItem> item = SOLID_FUEL_BURNERS.get(chassis);
        if (item == null) {
            throw new IllegalArgumentException("Unknown solid fuel burner chassis: " + chassis);
        }
        return item;
    }

    public static DeferredItem<SolarPanelBlockItem> solarPanel(SolarPanelMaterial material) {
        DeferredItem<SolarPanelBlockItem> item = SOLAR_PANELS.get(material);
        if (item == null) {
            throw new IllegalArgumentException("Unknown solar panel material: " + material);
        }
        return item;
    }

    public static DeferredItem<SolarArrayExtenderItem> solarArrayExtender(SolarArrayExtenderMaterial material) {
        DeferredItem<SolarArrayExtenderItem> item = SOLAR_ARRAY_EXTENDERS.get(material);
        if (item == null) {
            throw new IllegalArgumentException("Unknown solar array extender material: " + material);
        }
        return item;
    }

    public static DeferredItem<ResonanceCalibratorBlockItem> resonanceCalibrator(ResonanceCalibratorChassis chassis) {
        DeferredItem<ResonanceCalibratorBlockItem> item = RESONANCE_CALIBRATORS.get(chassis);
        if (item == null) {
            throw new IllegalArgumentException("Unknown resonance calibrator chassis: " + chassis);
        }
        return item;
    }

    public static DeferredItem<ComponentRecyclerBlockItem> componentRecycler(ComponentRecyclerChassis chassis) {
        DeferredItem<ComponentRecyclerBlockItem> item = COMPONENT_RECYCLERS.get(chassis);
        if (item == null) {
            throw new IllegalArgumentException("Unknown component recycler chassis: " + chassis);
        }
        return item;
    }

    public static DeferredItem<CompressorTankBlockItem> compressorTank(CompressorTankMaterial material) {
        DeferredItem<CompressorTankBlockItem> item = COMPRESSOR_TANKS.get(material);
        if (item == null) {
            throw new IllegalArgumentException("Unknown compressor tank material: " + material);
        }
        return item;
    }

    public static DeferredItem<DisassemblyHeadItem> disassemblyHead(DisassemblyHeadMaterial material) {
        DeferredItem<DisassemblyHeadItem> item = DISASSEMBLY_HEADS.get(material);
        if (item == null) {
            throw new IllegalArgumentException("Unknown disassembly head material: " + material);
        }
        return item;
    }

    public static DeferredItem<SolidFuelBurnerPartItem> heatCore(HeatCoreMaterial material) {
        DeferredItem<SolidFuelBurnerPartItem> item = HEAT_CORES.get(material);
        if (item == null) {
            throw new IllegalArgumentException("Unknown heat core material: " + material);
        }
        return item;
    }

    public static DeferredItem<SolidFuelBurnerPartItem> fuelBox(FuelBoxMaterial material) {
        DeferredItem<SolidFuelBurnerPartItem> item = FUEL_BOXES.get(material);
        if (item == null) {
            throw new IllegalArgumentException("Unknown fuel box material: " + material);
        }
        return item;
    }

    public static DeferredItem<PotentialReactorPartItem> reactorChamber(ReactorChamberMaterial material) {
        DeferredItem<PotentialReactorPartItem> item = REACTOR_CHAMBERS.get(material);
        if (item == null) {
            throw new IllegalArgumentException("Unknown reactor chamber material: " + material);
        }
        return item;
    }

    public static DeferredItem<PotentialReactorPartItem> recoveryFilter(RecoveryFilterMaterial material) {
        DeferredItem<PotentialReactorPartItem> item = RECOVERY_FILTERS.get(material);
        if (item == null) {
            throw new IllegalArgumentException("Unknown recovery filter material: " + material);
        }
        return item;
    }

    public static DeferredItem<PotentialReactorPartItem> containmentLining(ContainmentLiningMaterial material) {
        DeferredItem<PotentialReactorPartItem> item = CONTAINMENT_LININGS.get(material);
        if (item == null) {
            throw new IllegalArgumentException("Unknown containment lining material: " + material);
        }
        return item;
    }

    public static DeferredItem<CavitationPartItem> cavitationRotor(CavitationRotorMaterial material) {
        DeferredItem<CavitationPartItem> item = CAVITATION_ROTORS.get(material);
        if (item == null) {
            throw new IllegalArgumentException("Unknown cavitation rotor material: " + material);
        }
        return item;
    }

    public static DeferredItem<CollapseNozzleItem> collapseNozzle(CollapseNozzleMaterial material) {
        DeferredItem<CollapseNozzleItem> item = COLLAPSE_NOZZLES.get(material);
        if (item == null) {
            throw new IllegalArgumentException("Unknown collapse nozzle material: " + material);
        }
        return item;
    }

    public static DeferredItem<VacuumCollapsePartItem> voidChamber(VacuumCollapsePartMaterial material) {
        DeferredItem<VacuumCollapsePartItem> item = VOID_CHAMBERS.get(material);
        if (item == null) {
            throw new IllegalArgumentException("Unknown void chamber material: " + material);
        }
        return item;
    }

    public static DeferredItem<VacuumCollapsePartItem> dimensionalStabilizer(VacuumCollapsePartMaterial material) {
        DeferredItem<VacuumCollapsePartItem> item = DIMENSIONAL_STABILIZERS.get(material);
        if (item == null) {
            throw new IllegalArgumentException("Unknown dimensional stabilizer material: " + material);
        }
        return item;
    }

    public static DeferredItem<ServoItem> servo(ServoMaterial material) {
        DeferredItem<ServoItem> item = SERVOS.get(material);
        if (item == null) {
            throw new IllegalArgumentException("Unknown servo material: " + material);
        }
        return item;
    }

    public static DeferredItem<FluidPumpItem> fluidPump(FluidPumpMaterial material) {
        DeferredItem<FluidPumpItem> item = FLUID_PUMPS.get(material);
        if (item == null) {
            throw new IllegalArgumentException("Unknown fluid pump material: " + material);
        }
        return item;
    }

    public static DeferredItem<AlloyCrucibleItem> alloyCrucible(AlloyCrucibleMaterial material) {
        DeferredItem<AlloyCrucibleItem> item = ALLOY_CRUCIBLES.get(material);
        if (item == null) {
            throw new IllegalArgumentException("Unknown alloy crucible material: " + material);
        }
        return item;
    }

    public static DeferredItem<Item> alloyBlend(AlloyBlendMaterial material) {
        DeferredItem<Item> item = ALLOY_BLENDS.get(material);
        if (item == null) {
            throw new IllegalArgumentException("Unknown alloy blend material: " + material);
        }
        return item;
    }

    public static DeferredItem<CalibrationGearItem> resonanceCoil(CalibrationGearMaterial material) {
        DeferredItem<CalibrationGearItem> item = RESONANCE_COILS.get(material);
        if (item == null) {
            throw new IllegalArgumentException("Unknown resonance coil material: " + material);
        }
        return item;
    }

    public static DeferredItem<CalibrationGearItem> controlBoard(CalibrationGearMaterial material) {
        DeferredItem<CalibrationGearItem> item = CONTROL_BOARDS.get(material);
        if (item == null) {
            throw new IllegalArgumentException("Unknown control board material: " + material);
        }
        return item;
    }

    public static DeferredItem<CalibrationGearItem> stabilizerMatrix(CalibrationGearMaterial material) {
        DeferredItem<CalibrationGearItem> item = STABILIZER_MATRICES.get(material);
        if (item == null) {
            throw new IllegalArgumentException("Unknown stabilizer matrix material: " + material);
        }
        return item;
    }

    public static DeferredItem<CalibrationPatternItem> calibrationPattern(CalibrationFamily family) {
        DeferredItem<CalibrationPatternItem> item = CALIBRATION_PATTERNS.get(family);
        if (item == null) {
            throw new IllegalArgumentException("Unknown calibration pattern family: " + family);
        }
        return item;
    }

    public static DeferredItem<CalibratedComponentItem> calibratedComponent(CalibrationFamily family) {
        DeferredItem<CalibratedComponentItem> item = CALIBRATED_COMPONENTS.get(family);
        if (item == null) {
            throw new IllegalArgumentException("Unknown calibrated component family: " + family);
        }
        return item;
    }

    public static List<MaterialItemDefinition> materialItemDefinitions() {
        return MaterialCatalog.registeredItems();
    }

    private static Map<String, DeferredItem<Item>> registerMaterialItems() {
        Map<String, DeferredItem<Item>> items = new LinkedHashMap<>();
        for (MaterialItemDefinition definition : MaterialCatalog.registeredItems()) {
            DeferredItem<Item> item = OreCatalog.hasBlockItem(definition.itemId())
                    ? registerOreBlockItem(definition.itemId())
                    : ITEMS.register(
                            definition.itemId(),
                            () -> new MaterialItem(definition, new Item.Properties())
                    );
            items.put(definition.itemId(), item);
        }
        for (OreDefinition ore : OreCatalog.ores()) {
            for (OreHost host : ore.hosts()) {
                String itemId = ore.blockId(host);
                if (!items.containsKey(itemId)) {
                    DeferredItem<Item> item = registerOreBlockItem(itemId);
                    items.put(itemId, item);
                }
            }
        }
        return Collections.unmodifiableMap(items);
    }

    private static DeferredItem<Item> registerOreBlockItem(String itemId) {
        OreVariant variant = OreCatalog.variantByBlockId(itemId);
        return ITEMS.register(
                itemId,
                () -> new OreBlockItem(
                        ModBlocks.materialOre(itemId).get(),
                        variant.definition(),
                        variant.host(),
                        new Item.Properties()
                )
        );
    }

    private static Map<EnergyConnectorTier, DeferredItem<EnergyConnectorItem>> registerEnergyConnectors() {
        Map<EnergyConnectorTier, DeferredItem<EnergyConnectorItem>> items = new EnumMap<>(EnergyConnectorTier.class);
        for (EnergyConnectorTier tier : EnergyConnectorTier.values()) {
            DeferredItem<EnergyConnectorItem> item = ITEMS.register(
                    tier.itemId(),
                    () -> new EnergyConnectorItem(tier, new Item.Properties())
            );
            items.put(tier, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<FluidConnectorTier, DeferredItem<FluidConnectorItem>> registerFluidConnectors() {
        Map<FluidConnectorTier, DeferredItem<FluidConnectorItem>> items = new EnumMap<>(FluidConnectorTier.class);
        for (FluidConnectorTier tier : FluidConnectorTier.values()) {
            DeferredItem<FluidConnectorItem> item = ITEMS.register(
                    tier.itemId(),
                    () -> new FluidConnectorItem(tier, new Item.Properties())
            );
            items.put(tier, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<ItemConnectorTier, DeferredItem<ItemConnectorItem>> registerItemConnectors() {
        Map<ItemConnectorTier, DeferredItem<ItemConnectorItem>> items = new EnumMap<>(ItemConnectorTier.class);
        for (ItemConnectorTier tier : ItemConnectorTier.values()) {
            DeferredItem<ItemConnectorItem> item = ITEMS.register(
                    tier.itemId(),
                    () -> new ItemConnectorItem(tier, new Item.Properties())
            );
            items.put(tier, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<BatteryCellMaterial, DeferredItem<BatteryCellItem>> registerBatteryCells() {
        Map<BatteryCellMaterial, DeferredItem<BatteryCellItem>> items = new EnumMap<>(BatteryCellMaterial.class);
        for (BatteryCellMaterial material : BatteryCellMaterial.values()) {
            DeferredItem<BatteryCellItem> item = ITEMS.register(
                    material.itemId(),
                    () -> new BatteryCellItem(material, new Item.Properties().stacksTo(1))
            );
            items.put(material, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<ToolHeadFamily, Map<ToolHeadMaterial, DeferredItem<ToolHeadItem>>> registerToolHeads() {
        Map<ToolHeadFamily, Map<ToolHeadMaterial, DeferredItem<ToolHeadItem>>> families =
                new EnumMap<>(ToolHeadFamily.class);
        for (ToolHeadFamily family : ToolHeadFamily.values()) {
            Map<ToolHeadMaterial, DeferredItem<ToolHeadItem>> items = new EnumMap<>(ToolHeadMaterial.class);
            for (ToolHeadMaterial material : ToolHeadMaterial.values()) {
                DeferredItem<ToolHeadItem> item = ITEMS.register(
                        family.itemId(material),
                        () -> new ToolHeadItem(family, material, new Item.Properties().stacksTo(1))
                );
                items.put(material, item);
            }
            families.put(family, Collections.unmodifiableMap(items));
        }
        return Collections.unmodifiableMap(families);
    }

    private static Map<ToolRodMaterial, DeferredItem<ToolRodItem>> registerToolRods() {
        Map<ToolRodMaterial, DeferredItem<ToolRodItem>> items = new EnumMap<>(ToolRodMaterial.class);
        for (ToolRodMaterial material : ToolRodMaterial.values()) {
            DeferredItem<ToolRodItem> item = ITEMS.register(
                    material.itemId(),
                    () -> new ToolRodItem(material, new Item.Properties().stacksTo(1))
            );
            items.put(material, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<ToolHeadFamily, DeferredItem<ModularToolItem>> registerModularTools() {
        Map<ToolHeadFamily, DeferredItem<ModularToolItem>> items = new EnumMap<>(ToolHeadFamily.class);
        for (ToolHeadFamily family : ToolHeadFamily.values()) {
            DeferredItem<ModularToolItem> item = ITEMS.register(
                    family.assembledItemId(),
                    () -> new ModularToolItem(family, new Item.Properties().stacksTo(1).durability(1).setNoRepair())
            );
            items.put(family, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<PruningShearsMaterial, DeferredItem<PruningShearsItem>> registerPruningShears() {
        Map<PruningShearsMaterial, DeferredItem<PruningShearsItem>> items = new EnumMap<>(PruningShearsMaterial.class);
        for (PruningShearsMaterial material : PruningShearsMaterial.values()) {
            DeferredItem<PruningShearsItem> item = ITEMS.register(
                    material.itemId(),
                    () -> new PruningShearsItem(material, new Item.Properties().durability(material.durability()))
            );
            items.put(material, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<BatteryChassisMaterial, DeferredItem<BatteryChassisBlockItem>> registerBatteryChassisItems() {
        Map<BatteryChassisMaterial, DeferredItem<BatteryChassisBlockItem>> items = new EnumMap<>(BatteryChassisMaterial.class);
        for (BatteryChassisMaterial material : BatteryChassisMaterial.values()) {
            DeferredItem<BatteryChassisBlockItem> item = ITEMS.register(
                    material.blockId(),
                    () -> new BatteryChassisBlockItem(
                            ModBlocks.batteryChassis(material).get(),
                            material,
                            new Item.Properties().stacksTo(1)
                    )
            );
            items.put(material, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<CrusherChassisMaterial, DeferredItem<CrusherChassisBlockItem>> registerCrusherChassisItems() {
        Map<CrusherChassisMaterial, DeferredItem<CrusherChassisBlockItem>> items = new EnumMap<>(CrusherChassisMaterial.class);
        for (CrusherChassisMaterial material : CrusherChassisMaterial.values()) {
            DeferredItem<CrusherChassisBlockItem> item = ITEMS.register(
                    material.blockId(),
                    () -> new CrusherChassisBlockItem(
                            ModBlocks.crusherChassis(material).get(),
                            material,
                            new Item.Properties().stacksTo(1)
                    )
            );
            items.put(material, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<FurnaceChassisMaterial, DeferredItem<FurnaceChassisBlockItem>> registerFurnaceChassisItems() {
        Map<FurnaceChassisMaterial, DeferredItem<FurnaceChassisBlockItem>> items =
                new EnumMap<>(FurnaceChassisMaterial.class);
        for (FurnaceChassisMaterial material : FurnaceChassisMaterial.values()) {
            if (material == FurnaceChassisMaterial.PRIMITIVE) {
                continue;
            }
            DeferredItem<FurnaceChassisBlockItem> item = ITEMS.register(
                    material.blockId(),
                    () -> new FurnaceChassisBlockItem(
                            ModBlocks.furnaceChassis(material).get(),
                            material,
                            new Item.Properties().stacksTo(1)
                    )
            );
            items.put(material, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<AlloyFurnaceChassisMaterial, DeferredItem<AlloyFurnaceChassisBlockItem>> registerAlloyFurnaceChassisItems() {
        Map<AlloyFurnaceChassisMaterial, DeferredItem<AlloyFurnaceChassisBlockItem>> items =
                new EnumMap<>(AlloyFurnaceChassisMaterial.class);
        for (AlloyFurnaceChassisMaterial material : AlloyFurnaceChassisMaterial.values()) {
            DeferredItem<AlloyFurnaceChassisBlockItem> item = ITEMS.register(
                    material.blockId(),
                    () -> new AlloyFurnaceChassisBlockItem(
                            ModBlocks.alloyFurnaceChassis(material).get(),
                            material,
                            new Item.Properties().stacksTo(1)
                    )
            );
            items.put(material, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<CrushHeadMaterial, DeferredItem<CrushHeadItem>> registerCrushHeads() {
        Map<CrushHeadMaterial, DeferredItem<CrushHeadItem>> items = new EnumMap<>(CrushHeadMaterial.class);
        for (CrushHeadMaterial material : CrushHeadMaterial.values()) {
            DeferredItem<CrushHeadItem> item = ITEMS.register(
                    material.itemId(),
                    () -> new CrushHeadItem(material, new Item.Properties().stacksTo(1))
            );
            items.put(material, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<SolidFuelBurnerChassis, DeferredItem<SolidFuelBurnerBlockItem>> registerSolidFuelBurnerItems() {
        Map<SolidFuelBurnerChassis, DeferredItem<SolidFuelBurnerBlockItem>> items =
                new EnumMap<>(SolidFuelBurnerChassis.class);
        for (SolidFuelBurnerChassis chassis : SolidFuelBurnerChassis.values()) {
            DeferredItem<SolidFuelBurnerBlockItem> item = ITEMS.register(
                    chassis.blockId(),
                    () -> new SolidFuelBurnerBlockItem(
                            ModBlocks.solidFuelBurner(chassis).get(),
                            chassis,
                            new Item.Properties().stacksTo(1)
                    )
            );
            items.put(chassis, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<SolarPanelMaterial, DeferredItem<SolarPanelBlockItem>> registerSolarPanelItems() {
        Map<SolarPanelMaterial, DeferredItem<SolarPanelBlockItem>> items = new EnumMap<>(SolarPanelMaterial.class);
        for (SolarPanelMaterial material : SolarPanelMaterial.values()) {
            DeferredItem<SolarPanelBlockItem> item = ITEMS.register(
                    material.blockId(),
                    () -> new SolarPanelBlockItem(
                            ModBlocks.solarPanel(material).get(),
                            material,
                            new Item.Properties().stacksTo(1)
                    )
            );
            items.put(material, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<SolarArrayExtenderMaterial, DeferredItem<SolarArrayExtenderItem>> registerSolarArrayExtenders() {
        Map<SolarArrayExtenderMaterial, DeferredItem<SolarArrayExtenderItem>> items =
                new EnumMap<>(SolarArrayExtenderMaterial.class);
        for (SolarArrayExtenderMaterial material : SolarArrayExtenderMaterial.values()) {
            DeferredItem<SolarArrayExtenderItem> item = ITEMS.register(
                    material.itemId(),
                    () -> new SolarArrayExtenderItem(material, new Item.Properties().stacksTo(1))
            );
            items.put(material, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<ResonanceCalibratorChassis, DeferredItem<ResonanceCalibratorBlockItem>> registerResonanceCalibratorItems() {
        Map<ResonanceCalibratorChassis, DeferredItem<ResonanceCalibratorBlockItem>> items =
                new EnumMap<>(ResonanceCalibratorChassis.class);
        for (ResonanceCalibratorChassis chassis : ResonanceCalibratorChassis.values()) {
            DeferredItem<ResonanceCalibratorBlockItem> item = ITEMS.register(
                    chassis.blockId(),
                    () -> new ResonanceCalibratorBlockItem(
                            ModBlocks.resonanceCalibrator(chassis).get(),
                            chassis,
                            new Item.Properties().stacksTo(1)
                    )
            );
            items.put(chassis, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<ComponentRecyclerChassis, DeferredItem<ComponentRecyclerBlockItem>> registerComponentRecyclerItems() {
        Map<ComponentRecyclerChassis, DeferredItem<ComponentRecyclerBlockItem>> items =
                new EnumMap<>(ComponentRecyclerChassis.class);
        for (ComponentRecyclerChassis chassis : ComponentRecyclerChassis.values()) {
            DeferredItem<ComponentRecyclerBlockItem> item = ITEMS.register(
                    chassis.blockId(),
                    () -> new ComponentRecyclerBlockItem(
                            ModBlocks.componentRecycler(chassis).get(),
                            chassis,
                            new Item.Properties().stacksTo(1)
                    )
            );
            items.put(chassis, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<CompressorTankMaterial, DeferredItem<CompressorTankBlockItem>> registerCompressorTankItems() {
        Map<CompressorTankMaterial, DeferredItem<CompressorTankBlockItem>> items =
                new EnumMap<>(CompressorTankMaterial.class);
        for (CompressorTankMaterial material : CompressorTankMaterial.values()) {
            DeferredItem<CompressorTankBlockItem> item = ITEMS.register(
                    material.blockId(),
                    () -> new CompressorTankBlockItem(
                            ModBlocks.compressorTank(material).get(),
                            material,
                            new Item.Properties().stacksTo(1)
                    )
            );
            items.put(material, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<DisassemblyHeadMaterial, DeferredItem<DisassemblyHeadItem>> registerDisassemblyHeads() {
        Map<DisassemblyHeadMaterial, DeferredItem<DisassemblyHeadItem>> items =
                new EnumMap<>(DisassemblyHeadMaterial.class);
        for (DisassemblyHeadMaterial material : DisassemblyHeadMaterial.values()) {
            DeferredItem<DisassemblyHeadItem> item = ITEMS.register(
                    material.itemId(),
                    () -> new DisassemblyHeadItem(material, new Item.Properties().stacksTo(1))
            );
            items.put(material, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<HeatCoreMaterial, DeferredItem<SolidFuelBurnerPartItem>> registerHeatCores() {
        Map<HeatCoreMaterial, DeferredItem<SolidFuelBurnerPartItem>> items =
                new EnumMap<>(HeatCoreMaterial.class);
        for (HeatCoreMaterial material : HeatCoreMaterial.values()) {
            DeferredItem<SolidFuelBurnerPartItem> item = ITEMS.register(
                    material.itemId(),
                    () -> new SolidFuelBurnerPartItem(material, new Item.Properties().stacksTo(1))
            );
            items.put(material, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<FuelBoxMaterial, DeferredItem<SolidFuelBurnerPartItem>> registerFuelBoxes() {
        Map<FuelBoxMaterial, DeferredItem<SolidFuelBurnerPartItem>> items = new EnumMap<>(FuelBoxMaterial.class);
        for (FuelBoxMaterial material : FuelBoxMaterial.values()) {
            DeferredItem<SolidFuelBurnerPartItem> item = ITEMS.register(
                    material.itemId(),
                    () -> new SolidFuelBurnerPartItem(material, new Item.Properties().stacksTo(1))
            );
            items.put(material, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<ReactorChamberMaterial, DeferredItem<PotentialReactorPartItem>> registerReactorChambers() {
        Map<ReactorChamberMaterial, DeferredItem<PotentialReactorPartItem>> items =
                new EnumMap<>(ReactorChamberMaterial.class);
        for (ReactorChamberMaterial material : ReactorChamberMaterial.values()) {
            DeferredItem<PotentialReactorPartItem> item = ITEMS.register(
                    material.itemId(),
                    () -> new PotentialReactorPartItem(material, new Item.Properties().stacksTo(1))
            );
            items.put(material, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<RecoveryFilterMaterial, DeferredItem<PotentialReactorPartItem>> registerRecoveryFilters() {
        Map<RecoveryFilterMaterial, DeferredItem<PotentialReactorPartItem>> items =
                new EnumMap<>(RecoveryFilterMaterial.class);
        for (RecoveryFilterMaterial material : RecoveryFilterMaterial.values()) {
            DeferredItem<PotentialReactorPartItem> item = ITEMS.register(
                    material.itemId(),
                    () -> new PotentialReactorPartItem(material, new Item.Properties().stacksTo(1))
            );
            items.put(material, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<CavitationRotorMaterial, DeferredItem<CavitationPartItem>> registerCavitationRotors() {
        Map<CavitationRotorMaterial, DeferredItem<CavitationPartItem>> items =
                new EnumMap<>(CavitationRotorMaterial.class);
        for (CavitationRotorMaterial material : CavitationRotorMaterial.values()) {
            DeferredItem<CavitationPartItem> item = ITEMS.register(
                    material.itemId(),
                    () -> new CavitationPartItem(material, new Item.Properties().stacksTo(1))
            );
            items.put(material, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<CollapseNozzleMaterial, DeferredItem<CollapseNozzleItem>> registerCollapseNozzles() {
        Map<CollapseNozzleMaterial, DeferredItem<CollapseNozzleItem>> items =
                new EnumMap<>(CollapseNozzleMaterial.class);
        for (CollapseNozzleMaterial material : CollapseNozzleMaterial.values()) {
            DeferredItem<CollapseNozzleItem> item = ITEMS.register(
                    material.itemId(),
                    () -> new CollapseNozzleItem(material, new Item.Properties().stacksTo(1))
            );
            items.put(material, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<ContainmentLiningMaterial, DeferredItem<PotentialReactorPartItem>> registerContainmentLinings() {
        Map<ContainmentLiningMaterial, DeferredItem<PotentialReactorPartItem>> items =
                new EnumMap<>(ContainmentLiningMaterial.class);
        for (ContainmentLiningMaterial material : ContainmentLiningMaterial.values()) {
            DeferredItem<PotentialReactorPartItem> item = ITEMS.register(
                    material.itemId(),
                    () -> new PotentialReactorPartItem(material, new Item.Properties().stacksTo(1))
            );
            items.put(material, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<VacuumCollapsePartMaterial, DeferredItem<VacuumCollapsePartItem>> registerVacuumCollapseParts(
            String suffix,
            MachinePartType partType
    ) {
        Map<VacuumCollapsePartMaterial, DeferredItem<VacuumCollapsePartItem>> items =
                new EnumMap<>(VacuumCollapsePartMaterial.class);
        for (VacuumCollapsePartMaterial material : VacuumCollapsePartMaterial.values()) {
            DeferredItem<VacuumCollapsePartItem> item = ITEMS.register(
                    material.itemId(suffix),
                    () -> new VacuumCollapsePartItem(partType, material, new Item.Properties().stacksTo(1))
            );
            items.put(material, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<ServoMaterial, DeferredItem<ServoItem>> registerServos() {
        Map<ServoMaterial, DeferredItem<ServoItem>> items = new EnumMap<>(ServoMaterial.class);
        for (ServoMaterial material : ServoMaterial.values()) {
            DeferredItem<ServoItem> item = ITEMS.register(
                    material.itemId(),
                    () -> new ServoItem(material, new Item.Properties().stacksTo(1))
            );
            items.put(material, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<FluidPumpMaterial, DeferredItem<FluidPumpItem>> registerFluidPumps() {
        Map<FluidPumpMaterial, DeferredItem<FluidPumpItem>> items = new EnumMap<>(FluidPumpMaterial.class);
        for (FluidPumpMaterial material : FluidPumpMaterial.values()) {
            DeferredItem<FluidPumpItem> item = ITEMS.register(
                    material.itemId(),
                    () -> new FluidPumpItem(material, new Item.Properties().stacksTo(1))
            );
            items.put(material, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<AlloyCrucibleMaterial, DeferredItem<AlloyCrucibleItem>> registerAlloyCrucibles() {
        Map<AlloyCrucibleMaterial, DeferredItem<AlloyCrucibleItem>> items =
                new EnumMap<>(AlloyCrucibleMaterial.class);
        for (AlloyCrucibleMaterial material : AlloyCrucibleMaterial.values()) {
            DeferredItem<AlloyCrucibleItem> item = ITEMS.register(
                    material.itemId(),
                    () -> new AlloyCrucibleItem(material, new Item.Properties().stacksTo(1))
            );
            items.put(material, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<AlloyBlendMaterial, DeferredItem<Item>> registerAlloyBlends() {
        Map<AlloyBlendMaterial, DeferredItem<Item>> items = new EnumMap<>(AlloyBlendMaterial.class);
        for (AlloyBlendMaterial material : AlloyBlendMaterial.values()) {
            DeferredItem<Item> item = ITEMS.register(material.itemId(), () -> new Item(new Item.Properties()));
            items.put(material, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<CalibrationGearMaterial, DeferredItem<CalibrationGearItem>> registerCalibrationGear(
            String suffix,
            MachinePartType partType
    ) {
        Map<CalibrationGearMaterial, DeferredItem<CalibrationGearItem>> items =
                new EnumMap<>(CalibrationGearMaterial.class);
        for (CalibrationGearMaterial material : CalibrationGearMaterial.values()) {
            DeferredItem<CalibrationGearItem> item = ITEMS.register(
                    material.itemId(suffix),
                    () -> new CalibrationGearItem(partType, material, new Item.Properties().stacksTo(1))
            );
            items.put(material, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<CalibrationFamily, DeferredItem<CalibrationPatternItem>> registerCalibrationPatterns() {
        Map<CalibrationFamily, DeferredItem<CalibrationPatternItem>> items = new EnumMap<>(CalibrationFamily.class);
        for (CalibrationFamily family : CalibrationFamily.values()) {
            DeferredItem<CalibrationPatternItem> item = ITEMS.register(
                    family.patternItemId(),
                    () -> new CalibrationPatternItem(family, new Item.Properties().stacksTo(1))
            );
            items.put(family, item);
        }
        return Collections.unmodifiableMap(items);
    }

    private static Map<CalibrationFamily, DeferredItem<CalibratedComponentItem>> registerCalibratedComponents() {
        Map<CalibrationFamily, DeferredItem<CalibratedComponentItem>> items = new EnumMap<>(CalibrationFamily.class);
        for (CalibrationFamily family : CalibrationFamily.values()) {
            DeferredItem<CalibratedComponentItem> item = ITEMS.register(
                    family.componentItemId(),
                    () -> new CalibratedComponentItem(family, new Item.Properties())
            );
            items.put(family, item);
        }
        return Collections.unmodifiableMap(items);
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    private ModItems() {
    }
}
