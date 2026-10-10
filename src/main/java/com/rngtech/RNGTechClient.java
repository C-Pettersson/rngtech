package com.rngtech;

import com.rngtech.client.CableTintClient;
import com.rngtech.client.MinersCompanionLampClient;
import com.rngtech.client.configurator.ConfiguratorClient;
import com.rngtech.client.renderer.BatteryChassisRenderer;
import com.rngtech.client.renderer.ForestryCartRenderer;
import com.rngtech.client.renderer.ModularToolItemRenderer;
import com.rngtech.client.renderer.SolarArrayControllerRenderer;
import com.rngtech.client.screen.AdvancedItemFilterScreen;
import com.rngtech.client.screen.AffixForgeScreen;
import com.rngtech.client.screen.AlgaePhotobioreactorScreen;
import com.rngtech.client.screen.AlloyFurnaceScreen;
import com.rngtech.client.screen.AmmoniaFuelCellScreen;
import com.rngtech.client.screen.AmmoniaSynthesizerScreen;
import com.rngtech.client.screen.BatteryAssemblerScreen;
import com.rngtech.client.screen.BatteryChassisScreen;
import com.rngtech.client.screen.BioGeneratorScreen;
import com.rngtech.client.screen.CableConnectorScreen;
import com.rngtech.client.screen.CavitationGeneratorScreen;
import com.rngtech.client.screen.ComponentRecyclerScreen;
import com.rngtech.client.screen.CompressorTankScreen;
import com.rngtech.client.screen.ConfiguratorActionScreen;
import com.rngtech.client.screen.ConfiguratorAdvancedScreen;
import com.rngtech.client.screen.CorrosionCellScreen;
import com.rngtech.client.screen.CrusherScreen;
import com.rngtech.client.screen.DebugBatteryScreen;
import com.rngtech.client.screen.DebugChestScreen;
import com.rngtech.client.screen.DebugRerollerScreen;
import com.rngtech.client.screen.DebugTankScreen;
import com.rngtech.client.screen.ExoticAffixForgeScreen;
import com.rngtech.client.screen.ForestryCartScreen;
import com.rngtech.client.screen.ForestryCartStationScreen;
import com.rngtech.client.screen.FurnaceScreen;
import com.rngtech.client.screen.GasChemistryScreen;
import com.rngtech.client.screen.MelterScreen;
import com.rngtech.client.screen.MetalPressScreen;
import com.rngtech.client.screen.MinersCompanionScreen;
import com.rngtech.client.screen.PotentialReactorScreen;
import com.rngtech.client.screen.ResonanceCalibratorScreen;
import com.rngtech.client.screen.SilicaGelDehumidifierScreen;
import com.rngtech.client.screen.SolarArrayControllerScreen;
import com.rngtech.client.screen.SolidFuelBurnerScreen;
import com.rngtech.client.screen.ToolBenchScreen;
import com.rngtech.client.screen.UniqueHostReaders;
import com.rngtech.client.screen.UniversalConnectorScreen;
import com.rngtech.client.screen.VacuumCollapseGeneratorScreen;
import com.rngtech.client.screen.WoodenComposterScreen;
import com.rngtech.client.screen.WoodenDehumidifierScreen;
import com.rngtech.client.sound.MachineSoundClient;
import com.rngtech.client.wrench.WrenchOverlayClient;
import com.rngtech.content.item.UniqueTooltip;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModEntityTypes;
import com.rngtech.content.registry.ModFluids;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.registry.ModMenus;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = RNGTech.MOD_ID, dist = Dist.CLIENT)
public final class RNGTechClient {
    private static final ResourceLocation LUBRICANT_STILL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_still");
    private static final ResourceLocation LUBRICANT_FLOWING_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_flow");
    private static final ResourceLocation CARBON_EXHAUST_STILL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_still");
    private static final ResourceLocation CARBON_EXHAUST_FLOWING_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_flow");

    public RNGTechClient(IEventBus modBus, ModContainer container) {
        modBus.addListener(this::registerScreens);
        modBus.addListener(this::registerRenderers);
        modBus.addListener(this::registerClientExtensions);
        modBus.addListener(this::registerKeyMappings);
        modBus.addListener(CableTintClient::registerBlockColors);
        modBus.addListener(CableTintClient::registerItemColors);
        NeoForge.EVENT_BUS.addListener(WrenchOverlayClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(WrenchOverlayClient::onUseInput);
        NeoForge.EVENT_BUS.addListener(WrenchOverlayClient::onRenderLevel);
        NeoForge.EVENT_BUS.addListener(ConfiguratorClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(MachineSoundClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(MinersCompanionLampClient::onClientTick);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        UniqueTooltip.setOpenHost(UniqueHostReaders::current);
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.AFFIX_FORGE.get(), AffixForgeScreen::new);
        event.register(ModMenus.EXOTIC_AFFIX_FORGE.get(), ExoticAffixForgeScreen::new);
        event.register(ModMenus.DEBUG_REROLLER.get(), DebugRerollerScreen::new);
        event.register(ModMenus.DEBUG_CHEST.get(), DebugChestScreen::new);
        event.register(ModMenus.DEBUG_TANK.get(), DebugTankScreen::new);
        event.register(ModMenus.DEBUG_BATTERY.get(), DebugBatteryScreen::new);
        event.register(ModMenus.TOOL_BENCH.get(), ToolBenchScreen::new);
        event.register(ModMenus.CABLE_CONNECTOR.get(), CableConnectorScreen::new);
        event.register(ModMenus.UNIVERSAL_CONNECTOR.get(), UniversalConnectorScreen::new);
        event.register(ModMenus.CONFIGURATOR_ACTION.get(), ConfiguratorActionScreen::new);
        event.register(ModMenus.CONFIGURATOR_ADVANCED.get(), ConfiguratorAdvancedScreen::new);
        event.register(ModMenus.ADVANCED_ITEM_FILTER.get(), AdvancedItemFilterScreen::new);
        event.register(ModMenus.MINERS_COMPANION.get(), MinersCompanionScreen::new);
        event.register(ModMenus.BATTERY_CHASSIS.get(), BatteryChassisScreen::new);
        event.register(ModMenus.SOLID_FUEL_BURNER.get(), SolidFuelBurnerScreen::new);
        event.register(ModMenus.BIO_GENERATOR.get(), BioGeneratorScreen::new);
        event.register(ModMenus.ALGAE_PHOTOBIOREACTOR.get(), AlgaePhotobioreactorScreen::new);
        event.register(ModMenus.WOODEN_COMPOSTER.get(), WoodenComposterScreen::new);
        event.register(ModMenus.WOODEN_DEHUMIDIFIER.get(), WoodenDehumidifierScreen::new);
        event.register(ModMenus.SILICA_GEL_DEHUMIDIFIER.get(), SilicaGelDehumidifierScreen::new);
        event.register(ModMenus.SOLAR_ARRAY_CONTROLLER.get(), SolarArrayControllerScreen::new);
        event.register(ModMenus.POTENTIAL_REACTOR.get(), PotentialReactorScreen::new);
        event.register(ModMenus.CORROSION_CELL.get(), CorrosionCellScreen::new);
        event.register(ModMenus.CAVITATION_GENERATOR.get(), CavitationGeneratorScreen::new);
        event.register(ModMenus.VACUUM_COLLAPSE_GENERATOR.get(), VacuumCollapseGeneratorScreen::new);
        event.register(ModMenus.CRUSHER.get(), CrusherScreen::new);
        event.register(ModMenus.FURNACE.get(), FurnaceScreen::new);
        event.register(ModMenus.ALLOY_FURNACE.get(), AlloyFurnaceScreen::new);
        event.register(ModMenus.METAL_PRESS.get(), MetalPressScreen::new);
        event.register(ModMenus.RESONANCE_CALIBRATOR.get(), ResonanceCalibratorScreen::new);
        event.register(ModMenus.COMPONENT_RECYCLER.get(), ComponentRecyclerScreen::new);
        event.register(ModMenus.MELTER.get(), MelterScreen::new);
        event.register(ModMenus.BATTERY_ASSEMBLER.get(), BatteryAssemblerScreen::new);
        event.register(ModMenus.GAS_CHEMISTRY.get(), GasChemistryScreen::new);
        event.register(ModMenus.AMMONIA_SYNTHESIZER.get(), AmmoniaSynthesizerScreen::new);
        event.register(ModMenus.AMMONIA_FUEL_CELL.get(), AmmoniaFuelCellScreen::new);
        event.register(ModMenus.COMPRESSOR_TANK.get(), CompressorTankScreen::new);
        event.register(ModMenus.FORESTRY_CART_STATION.get(), ForestryCartStationScreen::new);
        event.register(ModMenus.FORESTRY_CART.get(), ForestryCartScreen::new);
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.BATTERY_CHASSIS.get(), BatteryChassisRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SOLAR_ARRAY_CONTROLLER.get(), SolarArrayControllerRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.FORESTRY_CART.get(), ForestryCartRenderer::new);
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        WrenchOverlayClient.registerKeyMappings(event);
        ConfiguratorClient.registerKeyMappings(event);
    }

    private void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer = new ModularToolItemRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        }, ModItems.MODULAR_TOOLS.values().stream().map(holder -> holder.get()).toArray(Item[]::new));

        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return LUBRICANT_STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return LUBRICANT_FLOWING_TEXTURE;
            }

            @Override
            public int getTintColor() {
                return 0xFFD6C658;
            }
        }, ModFluids.LUBRICANT_TYPE.get());
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return LUBRICANT_STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return LUBRICANT_FLOWING_TEXTURE;
            }

            @Override
            public int getTintColor() {
                return 0xFF63D4C4;
            }
        }, ModFluids.ELECTROLYTE_SOLUTION_TYPE.get());
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return CARBON_EXHAUST_STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return CARBON_EXHAUST_FLOWING_TEXTURE;
            }

            @Override
            public int getTintColor() {
                return 0xFF6A7778;
            }
        }, ModFluids.CARBON_EXHAUST_TYPE.get());
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return LUBRICANT_STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return LUBRICANT_FLOWING_TEXTURE;
            }

            @Override
            public int getTintColor() {
                return 0xFFB69D54;
            }
        }, ModFluids.SYNGAS_TYPE.get());
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return LUBRICANT_STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return LUBRICANT_FLOWING_TEXTURE;
            }

            @Override
            public int getTintColor() {
                return 0xFFAFCB8F;
            }
        }, ModFluids.METHANE_TYPE.get());
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return LUBRICANT_STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return LUBRICANT_FLOWING_TEXTURE;
            }

            @Override
            public int getTintColor() {
                return 0xFF8B8D90;
            }
        }, ModFluids.CARBON_MONOXIDE_TYPE.get());
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return LUBRICANT_STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return LUBRICANT_FLOWING_TEXTURE;
            }

            @Override
            public int getTintColor() {
                return 0xFF8BA4C9;
            }
        }, ModFluids.NITROGEN_TYPE.get());
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return LUBRICANT_STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return LUBRICANT_FLOWING_TEXTURE;
            }

            @Override
            public int getTintColor() {
                return 0xFFE3D2A2;
            }
        }, ModFluids.HYDROGEN_TYPE.get());
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return LUBRICANT_STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return LUBRICANT_FLOWING_TEXTURE;
            }

            @Override
            public int getTintColor() {
                return 0xFFBFD48B;
            }
        }, ModFluids.AMMONIA_TYPE.get());
    }
}
