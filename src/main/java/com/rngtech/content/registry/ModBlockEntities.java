package com.rngtech.content.registry;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.AffixForgeBlockEntity;
import com.rngtech.content.blockentity.AlgaePhotobioreactorBlockEntity;
import com.rngtech.content.blockentity.AlloyFurnaceBlockEntity;
import com.rngtech.content.blockentity.AmmoniaFuelCellBlockEntity;
import com.rngtech.content.blockentity.AmmoniaSynthesizerBlockEntity;
import com.rngtech.content.blockentity.BatteryAssemblerBlockEntity;
import com.rngtech.content.blockentity.BatteryChassisBlockEntity;
import com.rngtech.content.blockentity.BioGeneratorBlockEntity;
import com.rngtech.content.blockentity.CableBlockEntity;
import com.rngtech.content.blockentity.CavitationGeneratorBlockEntity;
import com.rngtech.content.blockentity.ComponentRecyclerBlockEntity;
import com.rngtech.content.blockentity.CompressorTankBlockEntity;
import com.rngtech.content.blockentity.CorrosionCellBlockEntity;
import com.rngtech.content.blockentity.CrusherBlockEntity;
import com.rngtech.content.blockentity.DebugBatteryBlockEntity;
import com.rngtech.content.blockentity.DebugChestBlockEntity;
import com.rngtech.content.blockentity.DebugRerollerBlockEntity;
import com.rngtech.content.blockentity.DebugTankBlockEntity;
import com.rngtech.content.blockentity.DebugTrashcanBlockEntity;
import com.rngtech.content.blockentity.ExoticAffixForgeBlockEntity;
import com.rngtech.content.blockentity.ForestryCartStationBlockEntity;
import com.rngtech.content.blockentity.FurnaceBlockEntity;
import com.rngtech.content.blockentity.GasChemistryBlockEntity;
import com.rngtech.content.blockentity.MelterBlockEntity;
import com.rngtech.content.blockentity.MetalPressBlockEntity;
import com.rngtech.content.blockentity.PotentialReactorBlockEntity;
import com.rngtech.content.blockentity.ResonanceCalibratorBlockEntity;
import com.rngtech.content.blockentity.SilicaGelDehumidifierBlockEntity;
import com.rngtech.content.blockentity.SolarArrayControllerBlockEntity;
import com.rngtech.content.blockentity.SolarPanelBlockEntity;
import com.rngtech.content.blockentity.SolidFuelBurnerBlockEntity;
import com.rngtech.content.blockentity.ToolBenchBlockEntity;
import com.rngtech.content.blockentity.UniversalConnectorBlockEntity;
import com.rngtech.content.blockentity.VacuumCollapseGeneratorBlockEntity;
import com.rngtech.content.blockentity.WoodenComposterBlockEntity;
import com.rngtech.content.blockentity.WoodenDehumidifierBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RNGTech.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AffixForgeBlockEntity>> AFFIX_FORGE =
            BLOCK_ENTITY_TYPES.register(
                    "affix_forge",
                    () -> BlockEntityType.Builder.of(AffixForgeBlockEntity::new, ModBlocks.AFFIX_FORGE.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ExoticAffixForgeBlockEntity>> EXOTIC_AFFIX_FORGE =
            BLOCK_ENTITY_TYPES.register(
                    "exotic_affix_forge",
                    () -> BlockEntityType.Builder.of(ExoticAffixForgeBlockEntity::new, ModBlocks.EXOTIC_AFFIX_FORGE.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DebugRerollerBlockEntity>> DEBUG_REROLLER =
            BLOCK_ENTITY_TYPES.register(
                    "debug_reroller",
                    () -> BlockEntityType.Builder.of(DebugRerollerBlockEntity::new, ModBlocks.DEBUG_REROLLER.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DebugChestBlockEntity>> DEBUG_CHEST =
            BLOCK_ENTITY_TYPES.register(
                    "debug_chest",
                    () -> BlockEntityType.Builder.of(DebugChestBlockEntity::new, ModBlocks.DEBUG_CHEST.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DebugTrashcanBlockEntity>> DEBUG_TRASHCAN =
            BLOCK_ENTITY_TYPES.register(
                    "debug_trashcan",
                    () -> BlockEntityType.Builder.of(DebugTrashcanBlockEntity::new, ModBlocks.DEBUG_TRASHCAN.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DebugTankBlockEntity>> DEBUG_TANK =
            BLOCK_ENTITY_TYPES.register(
                    "debug_tank",
                    () -> BlockEntityType.Builder.of(DebugTankBlockEntity::new, ModBlocks.DEBUG_TANK.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DebugBatteryBlockEntity>> DEBUG_BATTERY =
            BLOCK_ENTITY_TYPES.register(
                    "debug_battery",
                    () -> BlockEntityType.Builder.of(DebugBatteryBlockEntity::new, ModBlocks.DEBUG_BATTERY.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ToolBenchBlockEntity>> TOOL_BENCH =
            BLOCK_ENTITY_TYPES.register(
                    "tool_bench",
                    () -> BlockEntityType.Builder.of(ToolBenchBlockEntity::new, ModBlocks.TOOL_BENCH.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ForestryCartStationBlockEntity>> FORESTRY_CART_STATION =
            BLOCK_ENTITY_TYPES.register(
                    "forestry_cart_station",
                    () -> BlockEntityType.Builder.of(ForestryCartStationBlockEntity::new, ModBlocks.FORESTRY_CART_STATION.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrusherBlockEntity>> CRUSHER =
            BLOCK_ENTITY_TYPES.register(
                    "crusher",
                    () -> BlockEntityType.Builder.of(CrusherBlockEntity::new, crusherBlocks()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FurnaceBlockEntity>> FURNACE =
            BLOCK_ENTITY_TYPES.register(
                    "furnace",
                    () -> BlockEntityType.Builder.of(FurnaceBlockEntity::new, furnaceBlocks()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlloyFurnaceBlockEntity>> ALLOY_FURNACE =
            BLOCK_ENTITY_TYPES.register(
                    "alloy_furnace",
                    () -> BlockEntityType.Builder.of(
                            AlloyFurnaceBlockEntity::new,
                            ModBlocks.ALLOY_FURNACE_CHASSIS.values().stream().map(DeferredBlock::get).toArray(Block[]::new)
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatteryChassisBlockEntity>> BATTERY_CHASSIS =
            BLOCK_ENTITY_TYPES.register(
                    "battery_chassis",
                    () -> BlockEntityType.Builder.of(
                            BatteryChassisBlockEntity::new,
                            ModBlocks.BATTERY_CHASSIS.values().stream().map(DeferredBlock::get).toArray(Block[]::new)
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolidFuelBurnerBlockEntity>> SOLID_FUEL_BURNER =
            BLOCK_ENTITY_TYPES.register(
                    "solid_fuel_burner_chassis",
                    () -> BlockEntityType.Builder.of(
                            SolidFuelBurnerBlockEntity::new,
                            ModBlocks.SOLID_FUEL_BURNERS.values().stream().map(DeferredBlock::get).toArray(Block[]::new)
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BioGeneratorBlockEntity>> BIO_GENERATOR =
            BLOCK_ENTITY_TYPES.register(
                    "bio_generator",
                    () -> BlockEntityType.Builder.of(BioGeneratorBlockEntity::new, ModBlocks.BIO_GENERATOR.get()).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlgaePhotobioreactorBlockEntity>> ALGAE_PHOTOBIOREACTOR =
            BLOCK_ENTITY_TYPES.register(
                    "algae_photobioreactor",
                    () -> BlockEntityType.Builder.of(
                            AlgaePhotobioreactorBlockEntity::new,
                            ModBlocks.ALGAE_PHOTOBIOREACTOR.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WoodenComposterBlockEntity>> WOODEN_COMPOSTER =
            BLOCK_ENTITY_TYPES.register(
                    "wooden_composter",
                    () -> BlockEntityType.Builder.of(WoodenComposterBlockEntity::new, ModBlocks.WOODEN_COMPOSTER.get()).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WoodenDehumidifierBlockEntity>> WOODEN_DEHUMIDIFIER =
            BLOCK_ENTITY_TYPES.register(
                    "wooden_dehumidifier",
                    () -> BlockEntityType.Builder.of(
                            WoodenDehumidifierBlockEntity::new,
                            ModBlocks.WOODEN_DEHUMIDIFIER.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SilicaGelDehumidifierBlockEntity>> SILICA_GEL_DEHUMIDIFIER =
            BLOCK_ENTITY_TYPES.register(
                    "silica_gel_dehumidifier",
                    () -> BlockEntityType.Builder.of(
                            SilicaGelDehumidifierBlockEntity::new,
                            ModBlocks.SILICA_GEL_DEHUMIDIFIER.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL =
            BLOCK_ENTITY_TYPES.register(
                    "solar_panel",
                    () -> BlockEntityType.Builder.of(
                            SolarPanelBlockEntity::new,
                            ModBlocks.SOLAR_PANELS.values().stream().map(DeferredBlock::get).toArray(Block[]::new)
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarArrayControllerBlockEntity>> SOLAR_ARRAY_CONTROLLER =
            BLOCK_ENTITY_TYPES.register(
                    "solar_array_controller",
                    () -> BlockEntityType.Builder.of(
                            SolarArrayControllerBlockEntity::new,
                            ModBlocks.SOLAR_ARRAY_CONTROLLER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PotentialReactorBlockEntity>> POTENTIAL_REACTOR =
            BLOCK_ENTITY_TYPES.register(
                    "potential_reactor",
                    () -> BlockEntityType.Builder.of(PotentialReactorBlockEntity::new, ModBlocks.POTENTIAL_REACTOR.get()).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CavitationGeneratorBlockEntity>> CAVITATION_GENERATOR =
            BLOCK_ENTITY_TYPES.register(
                    "cavitation_generator",
                    () -> BlockEntityType.Builder.of(
                            CavitationGeneratorBlockEntity::new,
                            ModBlocks.CAVITATION_GENERATOR.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VacuumCollapseGeneratorBlockEntity>> VACUUM_COLLAPSE_GENERATOR =
            BLOCK_ENTITY_TYPES.register(
                    "vacuum_collapse_generator",
                    () -> BlockEntityType.Builder.of(
                            VacuumCollapseGeneratorBlockEntity::new,
                            ModBlocks.VACUUM_COLLAPSE_GENERATOR.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CorrosionCellBlockEntity>> CORROSION_CELL =
            BLOCK_ENTITY_TYPES.register(
                    "corrosion_cell",
                    () -> BlockEntityType.Builder.of(CorrosionCellBlockEntity::new, ModBlocks.CORROSION_CELL.get()).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MetalPressBlockEntity>> METAL_PRESS =
            BLOCK_ENTITY_TYPES.register(
                    "metal_press",
                    () -> BlockEntityType.Builder.of(
                            MetalPressBlockEntity::new,
                            ModBlocks.CRUDE_METAL_PRESS.get(),
                            ModBlocks.METAL_PRESS.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResonanceCalibratorBlockEntity>> RESONANCE_CALIBRATOR =
            BLOCK_ENTITY_TYPES.register(
                    "resonance_calibrator",
                    () -> BlockEntityType.Builder.of(
                            ResonanceCalibratorBlockEntity::new,
                            ModBlocks.RESONANCE_CALIBRATORS.values().stream().map(DeferredBlock::get).toArray(Block[]::new)
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ComponentRecyclerBlockEntity>> COMPONENT_RECYCLER =
            BLOCK_ENTITY_TYPES.register(
                    "component_recycler",
                    () -> BlockEntityType.Builder.of(
                            ComponentRecyclerBlockEntity::new,
                            ModBlocks.COMPONENT_RECYCLERS.values().stream().map(DeferredBlock::get).toArray(Block[]::new)
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MelterBlockEntity>> MELTER =
            BLOCK_ENTITY_TYPES.register(
                    "melter",
                    () -> BlockEntityType.Builder.of(MelterBlockEntity::new, ModBlocks.MELTER.get()).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatteryAssemblerBlockEntity>> BATTERY_ASSEMBLER =
            BLOCK_ENTITY_TYPES.register(
                    "battery_assembler",
                    () -> BlockEntityType.Builder.of(
                            BatteryAssemblerBlockEntity::new,
                            ModBlocks.BATTERY_ASSEMBLER.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GasChemistryBlockEntity>> GAS_CHEMISTRY =
            BLOCK_ENTITY_TYPES.register(
                    "gas_chemistry",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new GasChemistryBlockEntity(
                                    pos,
                                    state,
                                    ((com.rngtech.content.block.GasChemistryBlock) state.getBlock()).machine()
                            ),
                            ModBlocks.COAL_GASIFIER.get(),
                            ModBlocks.SYNGAS_COMBUSTOR.get(),
                            ModBlocks.STEAM_METHANE_REFORMER.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AmmoniaSynthesizerBlockEntity>> AMMONIA_SYNTHESIZER =
            BLOCK_ENTITY_TYPES.register(
                    "ammonia_synthesizer",
                    () -> BlockEntityType.Builder.of(AmmoniaSynthesizerBlockEntity::new, ModBlocks.AMMONIA_SYNTHESIZER.get()).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AmmoniaFuelCellBlockEntity>> AMMONIA_FUEL_CELL =
            BLOCK_ENTITY_TYPES.register(
                    "ammonia_fuel_cell",
                    () -> BlockEntityType.Builder.of(AmmoniaFuelCellBlockEntity::new, ModBlocks.AMMONIA_FUEL_CELL.get()).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CompressorTankBlockEntity>> COMPRESSOR_TANK =
            BLOCK_ENTITY_TYPES.register(
                    "compressor_tank",
                    () -> BlockEntityType.Builder.of(
                            CompressorTankBlockEntity::new,
                            ModBlocks.COMPRESSOR_TANKS.values().stream().map(DeferredBlock::get).toArray(Block[]::new)
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CableBlockEntity>> CABLE =
            BLOCK_ENTITY_TYPES.register(
                    "cable",
                    () -> BlockEntityType.Builder.of(
                            CableBlockEntity::new,
                            ModBlocks.CABLE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<UniversalConnectorBlockEntity>> UNIVERSAL_CONNECTOR =
            BLOCK_ENTITY_TYPES.register(
                    "universal_connector",
                    () -> BlockEntityType.Builder.of(
                            UniversalConnectorBlockEntity::new,
                            ModBlocks.UNIVERSAL_CONNECTOR.get()
                    ).build(null)
            );

    public static void register(IEventBus bus) {
        BLOCK_ENTITY_TYPES.register(bus);
    }

    private static Block[] crusherBlocks() {
        List<Block> blocks = new ArrayList<>();
        ModBlocks.CRUSHER_CHASSIS.values().stream().map(DeferredBlock::get).forEach(blocks::add);
        return blocks.toArray(Block[]::new);
    }

    private static Block[] furnaceBlocks() {
        List<Block> blocks = new ArrayList<>();
        blocks.add(ModBlocks.FURNACE.get());
        ModBlocks.FURNACE_CHASSIS.values().stream().map(DeferredBlock::get).forEach(blocks::add);
        return blocks.toArray(Block[]::new);
    }

    private ModBlockEntities() {
    }
}
