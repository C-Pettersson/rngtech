package com.rngtech.content.registry;

import com.rngtech.RNGTech;
import com.rngtech.content.menu.AdvancedItemFilterMenu;
import com.rngtech.content.menu.AffixForgeMenu;
import com.rngtech.content.menu.AlgaePhotobioreactorMenu;
import com.rngtech.content.menu.AlloyFurnaceMenu;
import com.rngtech.content.menu.AmmoniaFuelCellMenu;
import com.rngtech.content.menu.AmmoniaSynthesizerMenu;
import com.rngtech.content.menu.BatteryAssemblerMenu;
import com.rngtech.content.menu.BatteryChassisMenu;
import com.rngtech.content.menu.BioGeneratorMenu;
import com.rngtech.content.menu.CableConnectorMenu;
import com.rngtech.content.menu.CavitationGeneratorMenu;
import com.rngtech.content.menu.ComponentRecyclerMenu;
import com.rngtech.content.menu.CompressorTankMenu;
import com.rngtech.content.menu.ConfiguratorActionMenu;
import com.rngtech.content.menu.ConfiguratorAdvancedMenu;
import com.rngtech.content.menu.CorrosionCellMenu;
import com.rngtech.content.menu.CrusherMenu;
import com.rngtech.content.menu.DebugBatteryMenu;
import com.rngtech.content.menu.DebugChestMenu;
import com.rngtech.content.menu.DebugRerollerMenu;
import com.rngtech.content.menu.DebugTankMenu;
import com.rngtech.content.menu.ExoticAffixForgeMenu;
import com.rngtech.content.menu.ForestryCartMenu;
import com.rngtech.content.menu.ForestryCartStationMenu;
import com.rngtech.content.menu.FurnaceMenu;
import com.rngtech.content.menu.GasChemistryMenu;
import com.rngtech.content.menu.MelterMenu;
import com.rngtech.content.menu.MetalPressMenu;
import com.rngtech.content.menu.MinersCompanionMenu;
import com.rngtech.content.menu.PotentialReactorMenu;
import com.rngtech.content.menu.ResonanceCalibratorMenu;
import com.rngtech.content.menu.SilicaGelDehumidifierMenu;
import com.rngtech.content.menu.SolarArrayControllerMenu;
import com.rngtech.content.menu.SolidFuelBurnerMenu;
import com.rngtech.content.menu.ToolBenchMenu;
import com.rngtech.content.menu.UniversalConnectorMenu;
import com.rngtech.content.menu.VacuumCollapseGeneratorMenu;
import com.rngtech.content.menu.WoodenComposterMenu;
import com.rngtech.content.menu.WoodenDehumidifierMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    private static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, RNGTech.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<AffixForgeMenu>> AFFIX_FORGE =
            MENU_TYPES.register("affix_forge", () -> IMenuTypeExtension.create(AffixForgeMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ExoticAffixForgeMenu>> EXOTIC_AFFIX_FORGE =
            MENU_TYPES.register("exotic_affix_forge", () -> IMenuTypeExtension.create(ExoticAffixForgeMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<DebugRerollerMenu>> DEBUG_REROLLER =
            MENU_TYPES.register("debug_reroller", () -> IMenuTypeExtension.create(DebugRerollerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<DebugChestMenu>> DEBUG_CHEST =
            MENU_TYPES.register("debug_chest", () -> IMenuTypeExtension.create(DebugChestMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<DebugTankMenu>> DEBUG_TANK =
            MENU_TYPES.register("debug_tank", () -> IMenuTypeExtension.create(DebugTankMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<DebugBatteryMenu>> DEBUG_BATTERY =
            MENU_TYPES.register("debug_battery", () -> IMenuTypeExtension.create(DebugBatteryMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ToolBenchMenu>> TOOL_BENCH =
            MENU_TYPES.register("tool_bench", () -> IMenuTypeExtension.create(ToolBenchMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ForestryCartStationMenu>> FORESTRY_CART_STATION =
            MENU_TYPES.register("forestry_cart_station", () -> IMenuTypeExtension.create(ForestryCartStationMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ForestryCartMenu>> FORESTRY_CART =
            MENU_TYPES.register("forestry_cart", () -> IMenuTypeExtension.create(ForestryCartMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CableConnectorMenu>> CABLE_CONNECTOR =
            MENU_TYPES.register("cable_connector", () -> IMenuTypeExtension.create(CableConnectorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<UniversalConnectorMenu>> UNIVERSAL_CONNECTOR =
            MENU_TYPES.register("universal_connector", () -> IMenuTypeExtension.create(UniversalConnectorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguratorActionMenu>> CONFIGURATOR_ACTION =
            MENU_TYPES.register("configurator_action", () -> IMenuTypeExtension.create(ConfiguratorActionMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguratorAdvancedMenu>> CONFIGURATOR_ADVANCED =
            MENU_TYPES.register("configurator_advanced", () -> IMenuTypeExtension.create(ConfiguratorAdvancedMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<AdvancedItemFilterMenu>> ADVANCED_ITEM_FILTER =
            MENU_TYPES.register("advanced_item_filter", () -> IMenuTypeExtension.create(AdvancedItemFilterMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MinersCompanionMenu>> MINERS_COMPANION =
            MENU_TYPES.register("miners_companion", () -> IMenuTypeExtension.create(MinersCompanionMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CrusherMenu>> CRUSHER =
            MENU_TYPES.register("crusher", () -> IMenuTypeExtension.create(CrusherMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FurnaceMenu>> FURNACE =
            MENU_TYPES.register("furnace", () -> IMenuTypeExtension.create(FurnaceMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<AlloyFurnaceMenu>> ALLOY_FURNACE =
            MENU_TYPES.register("alloy_furnace", () -> IMenuTypeExtension.create(AlloyFurnaceMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<BatteryChassisMenu>> BATTERY_CHASSIS =
            MENU_TYPES.register("battery_chassis", () -> IMenuTypeExtension.create(BatteryChassisMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SolidFuelBurnerMenu>> SOLID_FUEL_BURNER =
            MENU_TYPES.register("solid_fuel_burner_chassis", () -> IMenuTypeExtension.create(SolidFuelBurnerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<BioGeneratorMenu>> BIO_GENERATOR =
            MENU_TYPES.register("bio_generator", () -> IMenuTypeExtension.create(BioGeneratorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<AlgaePhotobioreactorMenu>> ALGAE_PHOTOBIOREACTOR =
            MENU_TYPES.register("algae_photobioreactor", () -> IMenuTypeExtension.create(AlgaePhotobioreactorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<WoodenComposterMenu>> WOODEN_COMPOSTER =
            MENU_TYPES.register("wooden_composter", () -> IMenuTypeExtension.create(WoodenComposterMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<WoodenDehumidifierMenu>> WOODEN_DEHUMIDIFIER =
            MENU_TYPES.register("wooden_dehumidifier", () -> IMenuTypeExtension.create(WoodenDehumidifierMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SilicaGelDehumidifierMenu>> SILICA_GEL_DEHUMIDIFIER =
            MENU_TYPES.register("silica_gel_dehumidifier", () -> IMenuTypeExtension.create(SilicaGelDehumidifierMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SolarArrayControllerMenu>> SOLAR_ARRAY_CONTROLLER =
            MENU_TYPES.register("solar_array_controller", () -> IMenuTypeExtension.create(SolarArrayControllerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<PotentialReactorMenu>> POTENTIAL_REACTOR =
            MENU_TYPES.register("potential_reactor", () -> IMenuTypeExtension.create(PotentialReactorMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<CavitationGeneratorMenu>> CAVITATION_GENERATOR =
            MENU_TYPES.register("cavitation_generator", () -> IMenuTypeExtension.create(CavitationGeneratorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<VacuumCollapseGeneratorMenu>> VACUUM_COLLAPSE_GENERATOR =
            MENU_TYPES.register("vacuum_collapse_generator", () -> IMenuTypeExtension.create(VacuumCollapseGeneratorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CorrosionCellMenu>> CORROSION_CELL =
            MENU_TYPES.register("corrosion_cell", () -> IMenuTypeExtension.create(CorrosionCellMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MetalPressMenu>> METAL_PRESS =
            MENU_TYPES.register("metal_press", () -> IMenuTypeExtension.create(MetalPressMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ResonanceCalibratorMenu>> RESONANCE_CALIBRATOR =
            MENU_TYPES.register("resonance_calibrator", () -> IMenuTypeExtension.create(ResonanceCalibratorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ComponentRecyclerMenu>> COMPONENT_RECYCLER =
            MENU_TYPES.register("component_recycler", () -> IMenuTypeExtension.create(ComponentRecyclerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MelterMenu>> MELTER =
            MENU_TYPES.register("melter", () -> IMenuTypeExtension.create(MelterMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<BatteryAssemblerMenu>> BATTERY_ASSEMBLER =
            MENU_TYPES.register("battery_assembler", () -> IMenuTypeExtension.create(BatteryAssemblerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GasChemistryMenu>> GAS_CHEMISTRY =
            MENU_TYPES.register("gas_chemistry", () -> IMenuTypeExtension.create(GasChemistryMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<AmmoniaSynthesizerMenu>> AMMONIA_SYNTHESIZER =
            MENU_TYPES.register("ammonia_synthesizer", () -> IMenuTypeExtension.create(AmmoniaSynthesizerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<AmmoniaFuelCellMenu>> AMMONIA_FUEL_CELL =
            MENU_TYPES.register("ammonia_fuel_cell", () -> IMenuTypeExtension.create(AmmoniaFuelCellMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<CompressorTankMenu>> COMPRESSOR_TANK =
            MENU_TYPES.register("compressor_tank", () -> IMenuTypeExtension.create(CompressorTankMenu::new));

    public static void register(IEventBus bus) {
        MENU_TYPES.register(bus);
    }

    private ModMenus() {
    }
}
