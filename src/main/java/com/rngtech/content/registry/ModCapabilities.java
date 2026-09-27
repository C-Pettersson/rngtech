package com.rngtech.content.registry;

import com.rngtech.RNGTech;
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
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.MinersCompanionItem;
import com.rngtech.content.item.ModularToolItem;

import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredItem;

import java.lang.reflect.InvocationTargetException;

public final class ModCapabilities {
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.CRUSHER.get(),
                CrusherBlockEntity::getItemHandler
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.CRUSHER.get(),
                CrusherBlockEntity::getEnergyStorage
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.FURNACE.get(),
                FurnaceBlockEntity::getItemHandler
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.FURNACE.get(),
                FurnaceBlockEntity::getEnergyStorage
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.ALLOY_FURNACE.get(),
                AlloyFurnaceBlockEntity::getItemHandler
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.ALLOY_FURNACE.get(),
                AlloyFurnaceBlockEntity::getEnergyStorage
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.BATTERY_CHASSIS.get(),
                BatteryChassisBlockEntity::getItemHandler
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.BATTERY_CHASSIS.get(),
                BatteryChassisBlockEntity::getEnergyStorage
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.SOLID_FUEL_BURNER.get(),
                SolidFuelBurnerBlockEntity::getItemHandler
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.SOLID_FUEL_BURNER.get(),
                SolidFuelBurnerBlockEntity::getEnergyStorage
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.BIO_GENERATOR.get(),
                BioGeneratorBlockEntity::getItemHandler
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.BIO_GENERATOR.get(),
                BioGeneratorBlockEntity::getEnergyStorage
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.ALGAE_PHOTOBIOREACTOR.get(),
                AlgaePhotobioreactorBlockEntity::getItemHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.ALGAE_PHOTOBIOREACTOR.get(),
                AlgaePhotobioreactorBlockEntity::getFluidHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.WOODEN_COMPOSTER.get(),
                WoodenComposterBlockEntity::getItemHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.WOODEN_COMPOSTER.get(),
                WoodenComposterBlockEntity::getFluidHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.WOODEN_DEHUMIDIFIER.get(),
                WoodenDehumidifierBlockEntity::getItemHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.WOODEN_DEHUMIDIFIER.get(),
                WoodenDehumidifierBlockEntity::getFluidHandler
        );
        event.registerBlock(
                Capabilities.ItemHandler.BLOCK,
                (level, pos, state, blockEntity, side) -> WoodenDehumidifierBlockEntity.controllerForFrame(level, pos)
                        .map(dehumidifier -> dehumidifier.getFrameItemHandler(side))
                        .orElse(null),
                ModBlocks.WOODEN_DEHUMIDIFIER_FRAME.get()
        );
        event.registerBlock(
                Capabilities.FluidHandler.BLOCK,
                (level, pos, state, blockEntity, side) -> WoodenDehumidifierBlockEntity.controllerForFrame(level, pos)
                        .map(dehumidifier -> dehumidifier.getFrameFluidHandler(side))
                        .orElse(null),
                ModBlocks.WOODEN_DEHUMIDIFIER_FRAME.get()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.SILICA_GEL_DEHUMIDIFIER.get(),
                SilicaGelDehumidifierBlockEntity::getItemHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.SILICA_GEL_DEHUMIDIFIER.get(),
                SilicaGelDehumidifierBlockEntity::getFluidHandler
        );
        event.registerBlock(
                Capabilities.FluidHandler.BLOCK,
                (level, pos, state, blockEntity, side) -> SilicaGelDehumidifierBlockEntity.controllerForCasing(level, pos)
                        .map(dehumidifier -> dehumidifier.getCasingFluidHandler(side))
                        .orElse(null),
                ModBlocks.SILICA_GEL_COLUMN_CASING.get()
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.SOLAR_PANEL.get(),
                SolarPanelBlockEntity::getEnergyStorage
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.SOLAR_ARRAY_CONTROLLER.get(),
                SolarArrayControllerBlockEntity::getEnergyStorage
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.TOOL_BENCH.get(),
                ToolBenchBlockEntity::getEnergyStorage
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.FORESTRY_CART_STATION.get(),
                ForestryCartStationBlockEntity::getItemHandler
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.FORESTRY_CART_STATION.get(),
                ForestryCartStationBlockEntity::getEnergyStorage
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.FORESTRY_CART_STATION.get(),
                ForestryCartStationBlockEntity::getFluidHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.POTENTIAL_REACTOR.get(),
                PotentialReactorBlockEntity::getItemHandler
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.POTENTIAL_REACTOR.get(),
                PotentialReactorBlockEntity::getEnergyStorage
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.CAVITATION_GENERATOR.get(),
                CavitationGeneratorBlockEntity::getItemHandler
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.CAVITATION_GENERATOR.get(),
                CavitationGeneratorBlockEntity::getEnergyStorage
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.CAVITATION_GENERATOR.get(),
                CavitationGeneratorBlockEntity::getFluidHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.VACUUM_COLLAPSE_GENERATOR.get(),
                VacuumCollapseGeneratorBlockEntity::getItemHandler
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.VACUUM_COLLAPSE_GENERATOR.get(),
                VacuumCollapseGeneratorBlockEntity::getEnergyStorage
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.CORROSION_CELL.get(),
                CorrosionCellBlockEntity::getItemHandler
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.CORROSION_CELL.get(),
                CorrosionCellBlockEntity::getEnergyStorage
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.CORROSION_CELL.get(),
                CorrosionCellBlockEntity::getFluidHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.METAL_PRESS.get(),
                MetalPressBlockEntity::getItemHandler
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.METAL_PRESS.get(),
                MetalPressBlockEntity::getEnergyStorage
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.RESONANCE_CALIBRATOR.get(),
                ResonanceCalibratorBlockEntity::getItemHandler
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.RESONANCE_CALIBRATOR.get(),
                ResonanceCalibratorBlockEntity::getEnergyStorage
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.COMPONENT_RECYCLER.get(),
                ComponentRecyclerBlockEntity::getItemHandler
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.COMPONENT_RECYCLER.get(),
                ComponentRecyclerBlockEntity::getEnergyStorage
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.MELTER.get(),
                MelterBlockEntity::getItemHandler
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.MELTER.get(),
                MelterBlockEntity::getEnergyStorage
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.MELTER.get(),
                MelterBlockEntity::getFluidHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.BATTERY_ASSEMBLER.get(),
                BatteryAssemblerBlockEntity::getItemHandler
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.BATTERY_ASSEMBLER.get(),
                BatteryAssemblerBlockEntity::getEnergyStorage
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.BATTERY_ASSEMBLER.get(),
                BatteryAssemblerBlockEntity::getFluidHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.GAS_CHEMISTRY.get(),
                GasChemistryBlockEntity::getItemHandler
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.GAS_CHEMISTRY.get(),
                GasChemistryBlockEntity::getEnergyStorage
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.GAS_CHEMISTRY.get(),
                GasChemistryBlockEntity::getFluidHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.AMMONIA_SYNTHESIZER.get(),
                AmmoniaSynthesizerBlockEntity::getItemHandler
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.AMMONIA_SYNTHESIZER.get(),
                AmmoniaSynthesizerBlockEntity::getEnergyStorage
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.AMMONIA_SYNTHESIZER.get(),
                AmmoniaSynthesizerBlockEntity::getFluidHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.AMMONIA_FUEL_CELL.get(),
                AmmoniaFuelCellBlockEntity::getItemHandler
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.AMMONIA_FUEL_CELL.get(),
                AmmoniaFuelCellBlockEntity::getEnergyStorage
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.AMMONIA_FUEL_CELL.get(),
                AmmoniaFuelCellBlockEntity::getFluidHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.COMPRESSOR_TANK.get(),
                CompressorTankBlockEntity::getItemHandler
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.COMPRESSOR_TANK.get(),
                CompressorTankBlockEntity::getEnergyStorage
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.COMPRESSOR_TANK.get(),
                CompressorTankBlockEntity::getFluidHandler
        );
        if (RNGTech.isDebugContentEnabled()) {
            event.registerBlockEntity(
                    Capabilities.ItemHandler.BLOCK,
                    ModBlockEntities.DEBUG_CHEST.get(),
                    DebugChestBlockEntity::getItemHandler
            );
            event.registerBlockEntity(
                    Capabilities.ItemHandler.BLOCK,
                    ModBlockEntities.DEBUG_TRASHCAN.get(),
                    DebugTrashcanBlockEntity::getItemHandler
            );
            event.registerBlockEntity(
                    Capabilities.FluidHandler.BLOCK,
                    ModBlockEntities.DEBUG_TRASHCAN.get(),
                    DebugTrashcanBlockEntity::getFluidHandler
            );
            event.registerBlockEntity(
                    Capabilities.EnergyStorage.BLOCK,
                    ModBlockEntities.DEBUG_TRASHCAN.get(),
                    DebugTrashcanBlockEntity::getEnergyStorage
            );
            event.registerBlockEntity(
                    Capabilities.ItemHandler.BLOCK,
                    ModBlockEntities.DEBUG_TANK.get(),
                    DebugTankBlockEntity::getItemHandler
            );
            event.registerBlockEntity(
                    Capabilities.FluidHandler.BLOCK,
                    ModBlockEntities.DEBUG_TANK.get(),
                    DebugTankBlockEntity::getFluidHandler
            );
            event.registerBlockEntity(
                    Capabilities.EnergyStorage.BLOCK,
                    ModBlockEntities.DEBUG_BATTERY.get(),
                    DebugBatteryBlockEntity::getEnergyStorage
            );
        }
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.EXOTIC_AFFIX_FORGE.get(),
                ExoticAffixForgeBlockEntity::getEnergyStorage
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.CABLE.get(),
                CableBlockEntity::getEnergyStorage
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.CABLE.get(),
                CableBlockEntity::getItemHandler
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.CABLE.get(),
                CableBlockEntity::getFluidHandler
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.UNIVERSAL_CONNECTOR.get(),
                UniversalConnectorBlockEntity::getEnergyStorage
        );
        event.registerItem(
                Capabilities.EnergyStorage.ITEM,
                (stack, context) -> BatteryCellItem.energyStorage(stack),
                ModItems.BATTERY_CELLS.values().stream().map(DeferredItem::get).toArray(Item[]::new)
        );
        event.registerItem(
                Capabilities.EnergyStorage.ITEM,
                (stack, context) -> ModularToolItem.energyStorage(stack),
                ModItems.MODULAR_TOOLS.values().stream().map(DeferredItem::get).toArray(Item[]::new)
        );
        event.registerItem(
                Capabilities.EnergyStorage.ITEM,
                (stack, context) -> MinersCompanionItem.energyStorage(stack),
                ModItems.MINERS_COMPANION.get()
        );
        registerOptionalCompat(event, "ae2", "com.rngtech.compat.ae2.Ae2CableBridgeCompat");
        registerOptionalCompat(event, "refinedstorage", "com.rngtech.compat.refinedstorage.RefinedStorageCableBridgeCompat");
    }

    private static void registerOptionalCompat(RegisterCapabilitiesEvent event, String modId, String className) {
        if (!ModList.get().isLoaded(modId)) {
            return;
        }
        try {
            Class.forName(className).getMethod("register", RegisterCapabilitiesEvent.class).invoke(null, event);
        } catch (ClassNotFoundException
                | NoSuchMethodException
                | IllegalAccessException
                | InvocationTargetException exception) {
            RNGTech.LOGGER.warn("Failed to register optional {} cable bridge compat", modId, exception);
        }
    }

    private ModCapabilities() {
    }
}
