package com.rngtech.content.registry;

import com.rngtech.RNGTech;
import com.rngtech.content.block.AffixForgeBlock;
import com.rngtech.content.block.AlgaePhotobioreactorBlock;
import com.rngtech.content.block.AlloyFurnaceBlock;
import com.rngtech.content.block.AmmoniaFuelCellBlock;
import com.rngtech.content.block.AmmoniaSynthesizerBlock;
import com.rngtech.content.block.BatteryAssemblerBlock;
import com.rngtech.content.block.BatteryChassisBlock;
import com.rngtech.content.block.BioGeneratorBlock;
import com.rngtech.content.block.CableBlock;
import com.rngtech.content.block.CavitationGeneratorBlock;
import com.rngtech.content.block.ComponentRecyclerBlock;
import com.rngtech.content.block.CompressorTankBlock;
import com.rngtech.content.block.CorrosionCellBlock;
import com.rngtech.content.block.CrusherBlock;
import com.rngtech.content.block.DebugBatteryBlock;
import com.rngtech.content.block.DebugChestBlock;
import com.rngtech.content.block.DebugRerollerBlock;
import com.rngtech.content.block.DebugTankBlock;
import com.rngtech.content.block.DebugTrashcanBlock;
import com.rngtech.content.block.ExoticAffixForgeBlock;
import com.rngtech.content.block.ForestryCartStationBlock;
import com.rngtech.content.block.FurnaceBlock;
import com.rngtech.content.block.GasChemistryBlock;
import com.rngtech.content.block.HandCrankBlock;
import com.rngtech.content.block.MelterBlock;
import com.rngtech.content.block.MetalPressBlock;
import com.rngtech.content.block.PotentialReactorBlock;
import com.rngtech.content.block.ResonanceCalibratorBlock;
import com.rngtech.content.block.SilicaGelColumnCasingBlock;
import com.rngtech.content.block.SilicaGelDehumidifierBlock;
import com.rngtech.content.block.SolarArrayControllerBlock;
import com.rngtech.content.block.SolarPanelBlock;
import com.rngtech.content.block.SolidFuelBurnerBlock;
import com.rngtech.content.block.ToolBenchBlock;
import com.rngtech.content.block.UniversalConnectorBlock;
import com.rngtech.content.block.VacuumCollapseGeneratorBlock;
import com.rngtech.content.block.WoodenComposterBlock;
import com.rngtech.content.block.WoodenDehumidifierBlock;
import com.rngtech.content.block.WoodenDehumidifierFrameBlock;
import com.rngtech.content.calibration.ResonanceCalibratorChassis;
import com.rngtech.content.chemistry.GasChemistryMachine;
import com.rngtech.content.energy.BatteryChassisMaterial;
import com.rngtech.content.energy.SolarPanelMaterial;
import com.rngtech.content.energy.SolidFuelBurnerChassis;
import com.rngtech.content.machine.AlloyFurnaceChassisMaterial;
import com.rngtech.content.machine.CompressorTankMaterial;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.machine.FurnaceChassisMaterial;
import com.rngtech.content.material.OreCatalog;
import com.rngtech.content.material.OreDefinition;
import com.rngtech.content.material.OreHost;
import com.rngtech.content.recycling.ComponentRecyclerChassis;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RNGTech.MOD_ID);

    public static final DeferredBlock<AffixForgeBlock> AFFIX_FORGE = BLOCKS.registerBlock(
            "affix_forge",
            AffixForgeBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
    );

    public static final DeferredBlock<ExoticAffixForgeBlock> EXOTIC_AFFIX_FORGE = BLOCKS.registerBlock(
            "exotic_affix_forge",
            ExoticAffixForgeBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(6.0F, 9.0F)
                    .sound(SoundType.METAL)
    );

    public static final DeferredBlock<DebugRerollerBlock> DEBUG_REROLLER = BLOCKS.registerBlock(
            "debug_reroller",
            DebugRerollerBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F)
                    .sound(SoundType.METAL)
    );

    public static final DeferredBlock<DebugChestBlock> DEBUG_CHEST = BLOCKS.registerBlock(
            "debug_chest",
            DebugChestBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F)
                    .sound(SoundType.METAL)
    );

    public static final DeferredBlock<DebugTrashcanBlock> DEBUG_TRASHCAN = BLOCKS.registerBlock(
            "debug_trashcan",
            DebugTrashcanBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F)
                    .sound(SoundType.METAL)
    );

    public static final DeferredBlock<DebugTankBlock> DEBUG_TANK = BLOCKS.registerBlock(
            "debug_tank",
            DebugTankBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F)
                    .sound(SoundType.METAL)
    );

    public static final DeferredBlock<DebugBatteryBlock> DEBUG_BATTERY = BLOCKS.registerBlock(
            "debug_battery",
            DebugBatteryBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F)
                    .sound(SoundType.METAL)
    );

    public static final DeferredBlock<ToolBenchBlock> TOOL_BENCH = BLOCKS.registerBlock(
            "tool_bench",
            ToolBenchBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.5F)
                    .sound(SoundType.METAL)
    );

    public static final DeferredBlock<ForestryCartStationBlock> FORESTRY_CART_STATION = BLOCKS.registerBlock(
            "forestry_cart_station",
            ForestryCartStationBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5F)
                    .sound(SoundType.WOOD)
    );

    public static final Map<CrusherChassisMaterial, DeferredBlock<CrusherBlock>> CRUSHER_CHASSIS =
            registerCrusherChassisBlocks();

    public static final DeferredBlock<FurnaceBlock> FURNACE = BLOCKS.registerBlock(
            "furnace",
            properties -> new FurnaceBlock(FurnaceChassisMaterial.PRIMITIVE, properties),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F)
                    .sound(SoundType.METAL)
    );
    public static final Map<FurnaceChassisMaterial, DeferredBlock<FurnaceBlock>> FURNACE_CHASSIS =
            registerFurnaceChassisBlocks();
    public static final Map<AlloyFurnaceChassisMaterial, DeferredBlock<AlloyFurnaceBlock>> ALLOY_FURNACE_CHASSIS =
            registerAlloyFurnaceChassisBlocks();

    public static final Map<BatteryChassisMaterial, DeferredBlock<BatteryChassisBlock>> BATTERY_CHASSIS =
            registerBatteryChassisBlocks();
    public static final Map<SolidFuelBurnerChassis, DeferredBlock<SolidFuelBurnerBlock>> SOLID_FUEL_BURNERS =
            registerSolidFuelBurnerBlocks();
    public static final DeferredBlock<BioGeneratorBlock> BIO_GENERATOR = BLOCKS.registerBlock(
            "bio_generator",
            BioGeneratorBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(2.5F)
                    .sound(SoundType.WOOD)
    );
    public static final DeferredBlock<AlgaePhotobioreactorBlock> ALGAE_PHOTOBIOREACTOR = BLOCKS.registerBlock(
            "algae_photobioreactor",
            AlgaePhotobioreactorBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(2.5F)
                    .sound(SoundType.GLASS)
    );
    public static final DeferredBlock<WoodenComposterBlock> WOODEN_COMPOSTER = BLOCKS.registerBlock(
            "wooden_composter",
            WoodenComposterBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(1.5F)
                    .sound(SoundType.WOOD)
    );
    public static final DeferredBlock<WoodenDehumidifierBlock> WOODEN_DEHUMIDIFIER = BLOCKS.registerBlock(
            "wooden_dehumidifier",
            WoodenDehumidifierBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(1.8F)
                    .sound(SoundType.WOOD)
    );
    public static final DeferredBlock<WoodenDehumidifierFrameBlock> WOODEN_DEHUMIDIFIER_FRAME = BLOCKS.registerBlock(
            "wooden_dehumidifier_frame",
            WoodenDehumidifierFrameBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(1.5F)
                    .sound(SoundType.WOOD)
    );
    public static final DeferredBlock<SilicaGelDehumidifierBlock> SILICA_GEL_DEHUMIDIFIER = BLOCKS.registerBlock(
            "silica_gel_dehumidifier",
            SilicaGelDehumidifierBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F)
                    .sound(SoundType.METAL)
    );
    public static final DeferredBlock<SilicaGelColumnCasingBlock> SILICA_GEL_COLUMN_CASING = BLOCKS.registerBlock(
            "silica_gel_column_casing",
            SilicaGelColumnCasingBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.QUARTZ)
                    .strength(2.5F)
                    .sound(SoundType.GLASS)
    );
    public static final Map<SolarPanelMaterial, DeferredBlock<SolarPanelBlock>> SOLAR_PANELS =
            registerSolarPanelBlocks();
    public static final DeferredBlock<SolarArrayControllerBlock> SOLAR_ARRAY_CONTROLLER = BLOCKS.registerBlock(
            "solar_array_controller",
            SolarArrayControllerBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F)
                    .sound(SoundType.METAL)
    );
    public static final Map<ResonanceCalibratorChassis, DeferredBlock<ResonanceCalibratorBlock>> RESONANCE_CALIBRATORS =
            registerResonanceCalibratorBlocks();
    public static final Map<ComponentRecyclerChassis, DeferredBlock<ComponentRecyclerBlock>> COMPONENT_RECYCLERS =
            registerComponentRecyclerBlocks();
    public static final DeferredBlock<HandCrankBlock> HAND_CRANK = BLOCKS.registerBlock(
            "hand_crank",
            HandCrankBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(0.8F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
    );
    public static final DeferredBlock<PotentialReactorBlock> POTENTIAL_REACTOR = BLOCKS.registerBlock(
            "potential_reactor",
            PotentialReactorBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F)
                    .sound(SoundType.METAL)
    );
    public static final DeferredBlock<CavitationGeneratorBlock> CAVITATION_GENERATOR = BLOCKS.registerBlock(
            "cavitation_generator",
            CavitationGeneratorBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F)
                    .sound(SoundType.METAL)
    );
    public static final DeferredBlock<VacuumCollapseGeneratorBlock> VACUUM_COLLAPSE_GENERATOR = BLOCKS.registerBlock(
            "vacuum_collapse_generator",
            VacuumCollapseGeneratorBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(4.5F)
                    .sound(SoundType.METAL)
    );
    public static final DeferredBlock<CorrosionCellBlock> CORROSION_CELL = BLOCKS.registerBlock(
            "corrosion_cell",
            CorrosionCellBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F)
                    .sound(SoundType.METAL)
    );
    public static final DeferredBlock<MetalPressBlock> CRUDE_METAL_PRESS = BLOCKS.registerBlock(
            "crude_metal_press",
            properties -> new MetalPressBlock(true, 3, properties),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F)
                    .sound(SoundType.METAL)
    );

    public static final DeferredBlock<MetalPressBlock> METAL_PRESS = BLOCKS.registerBlock(
            "metal_press",
            properties -> new MetalPressBlock(false, 4, properties),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F)
                    .sound(SoundType.METAL)
    );

    public static final DeferredBlock<MelterBlock> MELTER = BLOCKS.registerBlock(
            "melter",
            MelterBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F)
                    .sound(SoundType.METAL)
    );
    public static final DeferredBlock<BatteryAssemblerBlock> BATTERY_ASSEMBLER = BLOCKS.registerBlock(
            "battery_assembler",
            BatteryAssemblerBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F)
                    .sound(SoundType.METAL)
    );
    public static final DeferredBlock<GasChemistryBlock> COAL_GASIFIER = BLOCKS.registerBlock(
            "coal_gasifier",
            properties -> new GasChemistryBlock(GasChemistryMachine.COAL_GASIFIER, properties),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F)
                    .sound(SoundType.METAL)
    );
    public static final DeferredBlock<GasChemistryBlock> SYNGAS_COMBUSTOR = BLOCKS.registerBlock(
            "syngas_combustor",
            properties -> new GasChemistryBlock(GasChemistryMachine.SYNGAS_COMBUSTOR, properties),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F)
                    .sound(SoundType.METAL)
    );
    public static final DeferredBlock<GasChemistryBlock> STEAM_METHANE_REFORMER = BLOCKS.registerBlock(
            "steam_methane_reformer",
            properties -> new GasChemistryBlock(GasChemistryMachine.STEAM_METHANE_REFORMER, properties),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F)
                    .sound(SoundType.METAL)
    );
    public static final DeferredBlock<AmmoniaSynthesizerBlock> AMMONIA_SYNTHESIZER = BLOCKS.registerBlock(
            "ammonia_synthesizer",
            AmmoniaSynthesizerBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F)
                    .sound(SoundType.METAL)
    );
    public static final DeferredBlock<AmmoniaFuelCellBlock> AMMONIA_FUEL_CELL = BLOCKS.registerBlock(
            "ammonia_fuel_cell",
            AmmoniaFuelCellBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F)
                    .sound(SoundType.METAL)
    );
    public static final Map<CompressorTankMaterial, DeferredBlock<CompressorTankBlock>> COMPRESSOR_TANKS =
            registerCompressorTankBlocks();

    public static final DeferredBlock<LiquidBlock> LUBRICANT_BLOCK = BLOCKS.registerBlock(
            "lubricant",
            properties -> new LiquidBlock(ModFluids.LUBRICANT_SOURCE.get(), properties),
            BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()
    );
    public static final DeferredBlock<LiquidBlock> ELECTROLYTE_SOLUTION_BLOCK = BLOCKS.registerBlock(
            "electrolyte_solution",
            properties -> new LiquidBlock(ModFluids.ELECTROLYTE_SOLUTION_SOURCE.get(), properties),
            BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()
    );
    public static final DeferredBlock<LiquidBlock> CARBON_EXHAUST_BLOCK = BLOCKS.registerBlock(
            "carbon_exhaust",
            properties -> new LiquidBlock(ModFluids.CARBON_EXHAUST_SOURCE.get(), properties),
            BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()
    );
    public static final DeferredBlock<LiquidBlock> SYNGAS_BLOCK = BLOCKS.registerBlock(
            "syngas",
            properties -> new LiquidBlock(ModFluids.SYNGAS_SOURCE.get(), properties),
            BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()
    );
    public static final DeferredBlock<LiquidBlock> METHANE_BLOCK = BLOCKS.registerBlock(
            "methane",
            properties -> new LiquidBlock(ModFluids.METHANE_SOURCE.get(), properties),
            BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()
    );
    public static final DeferredBlock<LiquidBlock> CARBON_MONOXIDE_BLOCK = BLOCKS.registerBlock(
            "carbon_monoxide",
            properties -> new LiquidBlock(ModFluids.CARBON_MONOXIDE_SOURCE.get(), properties),
            BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()
    );
    public static final DeferredBlock<LiquidBlock> NITROGEN_BLOCK = BLOCKS.registerBlock(
            "nitrogen",
            properties -> new LiquidBlock(ModFluids.NITROGEN_SOURCE.get(), properties),
            BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()
    );
    public static final DeferredBlock<LiquidBlock> HYDROGEN_BLOCK = BLOCKS.registerBlock(
            "hydrogen",
            properties -> new LiquidBlock(ModFluids.HYDROGEN_SOURCE.get(), properties),
            BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()
    );
    public static final DeferredBlock<LiquidBlock> AMMONIA_BLOCK = BLOCKS.registerBlock(
            "ammonia",
            properties -> new LiquidBlock(ModFluids.AMMONIA_SOURCE.get(), properties),
            BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()
    );
    public static final DeferredBlock<CableBlock> CABLE = BLOCKS.registerBlock(
            "cable",
            CableBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(0.3F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
    );
    public static final DeferredBlock<UniversalConnectorBlock> UNIVERSAL_CONNECTOR = BLOCKS.registerBlock(
            "universal_connector",
            UniversalConnectorBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
    );
    public static final Map<String, DeferredBlock<Block>> MATERIAL_ORES = registerMaterialOreBlocks();

    public static DeferredBlock<BatteryChassisBlock> batteryChassis(BatteryChassisMaterial material) {
        DeferredBlock<BatteryChassisBlock> block = BATTERY_CHASSIS.get(material);
        if (block == null) {
            throw new IllegalArgumentException("Unknown battery chassis material: " + material);
        }
        return block;
    }

    public static DeferredBlock<CrusherBlock> crusherChassis(CrusherChassisMaterial material) {
        DeferredBlock<CrusherBlock> block = CRUSHER_CHASSIS.get(material);
        if (block == null) {
            throw new IllegalArgumentException("Unknown crusher chassis material: " + material);
        }
        return block;
    }

    public static DeferredBlock<FurnaceBlock> furnaceChassis(FurnaceChassisMaterial material) {
        if (material == FurnaceChassisMaterial.PRIMITIVE) {
            return FURNACE;
        }
        DeferredBlock<FurnaceBlock> block = FURNACE_CHASSIS.get(material);
        if (block == null) {
            throw new IllegalArgumentException("Unknown furnace chassis material: " + material);
        }
        return block;
    }

    public static DeferredBlock<AlloyFurnaceBlock> alloyFurnaceChassis(AlloyFurnaceChassisMaterial material) {
        DeferredBlock<AlloyFurnaceBlock> block = ALLOY_FURNACE_CHASSIS.get(material);
        if (block == null) {
            throw new IllegalArgumentException("Unknown alloy furnace chassis material: " + material);
        }
        return block;
    }

    public static DeferredBlock<SolidFuelBurnerBlock> solidFuelBurner(SolidFuelBurnerChassis chassis) {
        DeferredBlock<SolidFuelBurnerBlock> block = SOLID_FUEL_BURNERS.get(chassis);
        if (block == null) {
            throw new IllegalArgumentException("Unknown solid fuel burner chassis: " + chassis);
        }
        return block;
    }

    public static DeferredBlock<SolarPanelBlock> solarPanel(SolarPanelMaterial material) {
        DeferredBlock<SolarPanelBlock> block = SOLAR_PANELS.get(material);
        if (block == null) {
            throw new IllegalArgumentException("Unknown solar panel material: " + material);
        }
        return block;
    }

    public static DeferredBlock<ResonanceCalibratorBlock> resonanceCalibrator(ResonanceCalibratorChassis chassis) {
        DeferredBlock<ResonanceCalibratorBlock> block = RESONANCE_CALIBRATORS.get(chassis);
        if (block == null) {
            throw new IllegalArgumentException("Unknown resonance calibrator chassis: " + chassis);
        }
        return block;
    }

    public static DeferredBlock<ComponentRecyclerBlock> componentRecycler(ComponentRecyclerChassis chassis) {
        DeferredBlock<ComponentRecyclerBlock> block = COMPONENT_RECYCLERS.get(chassis);
        if (block == null) {
            throw new IllegalArgumentException("Unknown component recycler chassis: " + chassis);
        }
        return block;
    }

    public static DeferredBlock<CompressorTankBlock> compressorTank(CompressorTankMaterial material) {
        DeferredBlock<CompressorTankBlock> block = COMPRESSOR_TANKS.get(material);
        if (block == null) {
            throw new IllegalArgumentException("Unknown compressor tank material: " + material);
        }
        return block;
    }

    public static DeferredBlock<Block> materialOre(String blockId) {
        DeferredBlock<Block> block = MATERIAL_ORES.get(blockId);
        if (block == null) {
            throw new IllegalArgumentException("Unknown material ore block: " + blockId);
        }
        return block;
    }

    private static Map<BatteryChassisMaterial, DeferredBlock<BatteryChassisBlock>> registerBatteryChassisBlocks() {
        Map<BatteryChassisMaterial, DeferredBlock<BatteryChassisBlock>> blocks = new EnumMap<>(BatteryChassisMaterial.class);
        for (BatteryChassisMaterial material : BatteryChassisMaterial.values()) {
            DeferredBlock<BatteryChassisBlock> block = BLOCKS.registerBlock(
                    material.blockId(),
                    properties -> new BatteryChassisBlock(material, properties),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.5F)
                            .sound(SoundType.METAL)
            );
            blocks.put(material, block);
        }
        return Collections.unmodifiableMap(blocks);
    }

    private static Map<CrusherChassisMaterial, DeferredBlock<CrusherBlock>> registerCrusherChassisBlocks() {
        Map<CrusherChassisMaterial, DeferredBlock<CrusherBlock>> blocks = new EnumMap<>(CrusherChassisMaterial.class);
        for (CrusherChassisMaterial material : CrusherChassisMaterial.values()) {
            DeferredBlock<CrusherBlock> block = BLOCKS.registerBlock(
                    material.blockId(),
                    properties -> new CrusherBlock(material, properties),
                    crusherChassisProperties(material)
            );
            blocks.put(material, block);
        }
        return Collections.unmodifiableMap(blocks);
    }

    private static BlockBehaviour.Properties crusherChassisProperties(CrusherChassisMaterial material) {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                .mapColor(material == CrusherChassisMaterial.WOODEN ? MapColor.WOOD : MapColor.METAL)
                .strength(material == CrusherChassisMaterial.WOODEN ? 1.5F : 3.5F)
                .sound(material == CrusherChassisMaterial.WOODEN ? SoundType.WOOD : SoundType.METAL);
        return properties;
    }

    private static Map<FurnaceChassisMaterial, DeferredBlock<FurnaceBlock>> registerFurnaceChassisBlocks() {
        Map<FurnaceChassisMaterial, DeferredBlock<FurnaceBlock>> blocks = new EnumMap<>(FurnaceChassisMaterial.class);
        for (FurnaceChassisMaterial material : FurnaceChassisMaterial.values()) {
            if (material == FurnaceChassisMaterial.PRIMITIVE) {
                continue;
            }
            DeferredBlock<FurnaceBlock> block = BLOCKS.registerBlock(
                    material.blockId(),
                    properties -> new FurnaceBlock(material, properties),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.5F)
                            .sound(SoundType.METAL)
            );
            blocks.put(material, block);
        }
        return Collections.unmodifiableMap(blocks);
    }

    private static Map<AlloyFurnaceChassisMaterial, DeferredBlock<AlloyFurnaceBlock>> registerAlloyFurnaceChassisBlocks() {
        Map<AlloyFurnaceChassisMaterial, DeferredBlock<AlloyFurnaceBlock>> blocks =
                new EnumMap<>(AlloyFurnaceChassisMaterial.class);
        for (AlloyFurnaceChassisMaterial material : AlloyFurnaceChassisMaterial.values()) {
            DeferredBlock<AlloyFurnaceBlock> block = BLOCKS.registerBlock(
                    material.blockId(),
                    properties -> new AlloyFurnaceBlock(material, properties),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.5F)
                            .sound(SoundType.METAL)
            );
            blocks.put(material, block);
        }
        return Collections.unmodifiableMap(blocks);
    }

    private static Map<SolidFuelBurnerChassis, DeferredBlock<SolidFuelBurnerBlock>> registerSolidFuelBurnerBlocks() {
        Map<SolidFuelBurnerChassis, DeferredBlock<SolidFuelBurnerBlock>> blocks =
                new EnumMap<>(SolidFuelBurnerChassis.class);
        for (SolidFuelBurnerChassis chassis : SolidFuelBurnerChassis.values()) {
            DeferredBlock<SolidFuelBurnerBlock> block = BLOCKS.registerBlock(
                    chassis.blockId(),
                    properties -> new SolidFuelBurnerBlock(chassis, properties),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0F)
                            .sound(SoundType.METAL)
            );
            blocks.put(chassis, block);
        }
        return Collections.unmodifiableMap(blocks);
    }

    private static Map<CompressorTankMaterial, DeferredBlock<CompressorTankBlock>> registerCompressorTankBlocks() {
        Map<CompressorTankMaterial, DeferredBlock<CompressorTankBlock>> blocks =
                new EnumMap<>(CompressorTankMaterial.class);
        for (CompressorTankMaterial material : CompressorTankMaterial.values()) {
            DeferredBlock<CompressorTankBlock> block = BLOCKS.registerBlock(
                    material.blockId(),
                    properties -> new CompressorTankBlock(material, properties),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0F)
                            .sound(SoundType.METAL)
            );
            blocks.put(material, block);
        }
        return Collections.unmodifiableMap(blocks);
    }

    private static Map<SolarPanelMaterial, DeferredBlock<SolarPanelBlock>> registerSolarPanelBlocks() {
        Map<SolarPanelMaterial, DeferredBlock<SolarPanelBlock>> blocks = new EnumMap<>(SolarPanelMaterial.class);
        for (SolarPanelMaterial material : SolarPanelMaterial.values()) {
            DeferredBlock<SolarPanelBlock> block = BLOCKS.registerBlock(
                    material.blockId(),
                    properties -> new SolarPanelBlock(material, properties),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0F)
                            .sound(SoundType.GLASS)
            );
            blocks.put(material, block);
        }
        return Collections.unmodifiableMap(blocks);
    }

    private static Map<ResonanceCalibratorChassis, DeferredBlock<ResonanceCalibratorBlock>> registerResonanceCalibratorBlocks() {
        Map<ResonanceCalibratorChassis, DeferredBlock<ResonanceCalibratorBlock>> blocks =
                new EnumMap<>(ResonanceCalibratorChassis.class);
        for (ResonanceCalibratorChassis chassis : ResonanceCalibratorChassis.values()) {
            DeferredBlock<ResonanceCalibratorBlock> block = BLOCKS.registerBlock(
                    chassis.blockId(),
                    properties -> new ResonanceCalibratorBlock(chassis, properties),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.5F)
                            .sound(SoundType.METAL)
            );
            blocks.put(chassis, block);
        }
        return Collections.unmodifiableMap(blocks);
    }

    private static Map<ComponentRecyclerChassis, DeferredBlock<ComponentRecyclerBlock>> registerComponentRecyclerBlocks() {
        Map<ComponentRecyclerChassis, DeferredBlock<ComponentRecyclerBlock>> blocks =
                new EnumMap<>(ComponentRecyclerChassis.class);
        for (ComponentRecyclerChassis chassis : ComponentRecyclerChassis.values()) {
            DeferredBlock<ComponentRecyclerBlock> block = BLOCKS.registerBlock(
                    chassis.blockId(),
                    properties -> new ComponentRecyclerBlock(chassis, properties),
                    componentRecyclerProperties(chassis)
            );
            blocks.put(chassis, block);
        }
        return Collections.unmodifiableMap(blocks);
    }

    private static BlockBehaviour.Properties componentRecyclerProperties(ComponentRecyclerChassis chassis) {
        if (chassis.manual()) {
            return BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.0F)
                    .sound(SoundType.STONE);
        }
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5F)
                .sound(SoundType.METAL);
    }

    private static Map<String, DeferredBlock<Block>> registerMaterialOreBlocks() {
        Map<String, DeferredBlock<Block>> blocks = new LinkedHashMap<>();
        for (OreDefinition ore : OreCatalog.ores()) {
            for (OreHost host : ore.hosts()) {
                String blockId = ore.blockId(host);
                DeferredBlock<Block> block = BLOCKS.registerBlock(
                        blockId,
                        Block::new,
                        oreProperties(ore, host)
                );
                blocks.put(blockId, block);
            }
        }
        return Collections.unmodifiableMap(blocks);
    }

    private static BlockBehaviour.Properties oreProperties(OreDefinition ore, OreHost host) {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.ofFullCopy(switch (host) {
            case DEEPSLATE -> Blocks.DEEPSLATE_IRON_ORE;
            case END -> Blocks.END_STONE;
            case NETHER -> Blocks.NETHER_QUARTZ_ORE;
            case STONE -> Blocks.IRON_ORE;
        });
        float strength = switch (host) {
            case DEEPSLATE -> 4.5F;
            case END -> 3.0F;
            case NETHER -> 3.0F;
            case STONE -> 3.0F;
        };
        float hardnessBonus = Math.max(0, ore.hardnessLevel() - 2) * 0.25F;
        return properties.strength(strength + hardnessBonus, strength + hardnessBonus);
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }

    private ModBlocks() {
    }
}
