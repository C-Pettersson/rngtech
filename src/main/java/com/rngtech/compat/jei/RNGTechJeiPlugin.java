package com.rngtech.compat.jei;

import com.rngtech.RNGTech;
import com.rngtech.client.screen.AlgaePhotobioreactorScreen;
import com.rngtech.client.screen.AlloyFurnaceScreen;
import com.rngtech.client.screen.AmmoniaFuelCellScreen;
import com.rngtech.client.screen.AmmoniaSynthesizerScreen;
import com.rngtech.client.screen.BatteryAssemblerScreen;
import com.rngtech.client.screen.BioGeneratorScreen;
import com.rngtech.client.screen.CavitationGeneratorScreen;
import com.rngtech.client.screen.ComponentRecyclerScreen;
import com.rngtech.client.screen.CorrosionCellScreen;
import com.rngtech.client.screen.CrusherScreen;
import com.rngtech.client.screen.ExoticAffixForgeScreen;
import com.rngtech.client.screen.FurnaceScreen;
import com.rngtech.client.screen.GasChemistryScreen;
import com.rngtech.client.screen.MelterScreen;
import com.rngtech.client.screen.MetalPressScreen;
import com.rngtech.client.screen.MinersCompanionScreen;
import com.rngtech.client.screen.PotentialReactorScreen;
import com.rngtech.client.screen.ResonanceCalibratorScreen;
import com.rngtech.client.screen.SilicaGelDehumidifierScreen;
import com.rngtech.client.screen.SolidFuelBurnerScreen;
import com.rngtech.client.screen.VacuumCollapseGeneratorScreen;
import com.rngtech.client.screen.WoodenComposterScreen;
import com.rngtech.client.screen.WoodenDehumidifierScreen;
import com.rngtech.content.calibration.ResonanceCalibratorChassis;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.energy.SolidFuelBurnerChassis;
import com.rngtech.content.gear.GearMachineSpec;
import com.rngtech.content.gear.GearSlotCatalog;
import com.rngtech.content.machine.AlloyFurnaceChassisMaterial;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.machine.FurnaceChassisMaterial;
import com.rngtech.content.menu.AffixForgeMenu;
import com.rngtech.content.menu.AlgaePhotobioreactorMenu;
import com.rngtech.content.menu.AlloyFurnaceMenu;
import com.rngtech.content.menu.AmmoniaFuelCellMenu;
import com.rngtech.content.menu.AmmoniaSynthesizerMenu;
import com.rngtech.content.menu.BatteryAssemblerMenu;
import com.rngtech.content.menu.BioGeneratorMenu;
import com.rngtech.content.menu.CavitationGeneratorMenu;
import com.rngtech.content.menu.ComponentRecyclerMenu;
import com.rngtech.content.menu.CorrosionCellMenu;
import com.rngtech.content.menu.CrusherMenu;
import com.rngtech.content.menu.ExoticAffixForgeMenu;
import com.rngtech.content.menu.FurnaceMenu;
import com.rngtech.content.menu.GasChemistryMenu;
import com.rngtech.content.menu.MelterMenu;
import com.rngtech.content.menu.MetalPressMenu;
import com.rngtech.content.menu.MinersCompanionMenu;
import com.rngtech.content.menu.PotentialReactorMenu;
import com.rngtech.content.menu.ResonanceCalibratorMenu;
import com.rngtech.content.menu.SilicaGelDehumidifierMenu;
import com.rngtech.content.menu.SolidFuelBurnerMenu;
import com.rngtech.content.menu.VacuumCollapseGeneratorMenu;
import com.rngtech.content.menu.WoodenComposterMenu;
import com.rngtech.content.menu.WoodenDehumidifierMenu;
import com.rngtech.content.recipe.AlgaeGrowthRecipe;
import com.rngtech.content.recipe.AlloyFurnaceRecipe;
import com.rngtech.content.recipe.AmmoniaPowerCycleRecipe;
import com.rngtech.content.recipe.AmmoniaSynthesisRecipe;
import com.rngtech.content.recipe.BatteryAssemblyRecipe;
import com.rngtech.content.recipe.CalibrationRecipe;
import com.rngtech.content.recipe.CavitationRecipe;
import com.rngtech.content.recipe.CoalGasificationRecipe;
import com.rngtech.content.recipe.ComponentRecyclingRecipe;
import com.rngtech.content.recipe.CorrosionCellRecipe;
import com.rngtech.content.recipe.CrusherRecipe;
import com.rngtech.content.recipe.DesiccantAbsorptionRecipe;
import com.rngtech.content.recipe.ExoticAffixForgeRecipe;
import com.rngtech.content.recipe.FurnaceRecipe;
import com.rngtech.content.recipe.GasCombustionRecipe;
import com.rngtech.content.recipe.GasReformingRecipe;
import com.rngtech.content.recipe.MelterRecipe;
import com.rngtech.content.recipe.MetalPressRecipe;
import com.rngtech.content.recipe.MinersCompanionRecoveryRecipe;
import com.rngtech.content.recipe.PotentialReactorRecipe;
import com.rngtech.content.recipe.VacuumCollapseRecipe;
import com.rngtech.content.recipe.WoodenDehumidifierConversionRecipe;
import com.rngtech.content.recycling.ComponentRecyclerChassis;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.registry.ModMenus;
import com.rngtech.content.registry.ModRecipes;
import com.rngtech.rpg.CorruptionOutcome;
import com.rngtech.rpg.corruption.CorruptionCatalog;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import mezz.jei.api.runtime.IRecipesGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

@JeiPlugin
public final class RNGTechJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_UID = RNGTech.id("jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new MachineGearRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new AffixForgeRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new ExoticAffixForgeRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new SolidFuelBurnerRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new BioGeneratorRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new WoodenComposterRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new OreWorldgenRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new CrusherRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new FurnaceRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new AlloyFurnaceRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new PotentialReactorRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new MinersCompanionRecoveryRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new CorrosionCellRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new ComponentRecyclingRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new MetalPressRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new MelterRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new BatteryAssemblyRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new CoalGasificationRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new GasCombustionRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new GasReformingRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new AmmoniaSynthesisRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new AmmoniaPowerCycleRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new CavitationRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new VacuumCollapseRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new AlgaeGrowthRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new WoodenDehumidifierConversionRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new DesiccantAbsorptionRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new ResonanceCalibrationRecipeCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        registration.getCraftingCategory().addExtension(
                com.rngtech.content.recipe.CalibratedShapedRecipe.class,
                new CalibratedCraftingCategoryExtension()
        );
    }

    /** The default outcome weights as loaded, then how the Stabilization Crystal changes them. */
    private static List<Component> volatileCatalystInfo() {
        List<Component> lines = new java.util.ArrayList<>();
        lines.add(Component.translatable("rngtech.jei.volatile_catalyst.info"));
        Map<CorruptionOutcome, Integer> weights = CorruptionCatalog.active().weights(false);
        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        for (CorruptionOutcome outcome : CorruptionOutcome.values()) {
            lines.add(Component.translatable(
                    "rngtech.jei.volatile_catalyst.outcome",
                    Component.translatable(outcome.translationKey()),
                    total <= 0 ? 0 : Math.round(weights.getOrDefault(outcome, 0) * 100.0F / total),
                    com.rngtech.rpg.corruption.CorruptionText.description(outcome)
            ));
        }
        lines.add(Component.translatable("rngtech.jei.volatile_catalyst.stabilization"));
        return lines;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(JeiRecipeTypes.MACHINE_GEAR, MachineGearJeiRecipe.recipes());
        registration.addRecipes(JeiRecipeTypes.AFFIX_FORGE, AffixForgeJeiRecipe.recipes());
        registration.addRecipes(JeiRecipeTypes.SOLID_FUEL_BURNING, SolidFuelBurnerJeiFuel.recipes());
        registration.addRecipes(JeiRecipeTypes.BIO_GENERATOR, BioGeneratorJeiFuel.recipes());
        registration.addRecipes(JeiRecipeTypes.WOODEN_COMPOSTER, WoodenComposterJeiRecipe.recipes());
        registration.addRecipes(JeiRecipeTypes.ORE_WORLDGEN, OreWorldgenJeiRecipe.recipes());
        registration.addIngredientInfo(
                ModItems.AFFIX_FORGE.get(),
                Component.translatable("rngtech.jei.affix_forge.info")
        );
        registration.addIngredientInfo(
                ModItems.EXOTIC_AFFIX_FORGE.get(),
                Component.translatable("rngtech.jei.exotic_affix_forge.info")
        );
        registration.addItemStackInfo(
                uniqueItemStacks(),
                Component.translatable("rngtech.jei.unique.drop_find_only")
        );
        registration.addItemStackInfo(
                ModItems.ASCENDANCY_SEALS.stream().map(seal -> new ItemStack(seal.get())).toList(),
                Component.translatable("rngtech.jei.ascendancy_seal.info")
        );
        registration.addIngredientInfo(ModItems.VOLATILE_CATALYST.get(), volatileCatalystInfo().toArray(Component[]::new));
        registration.addIngredientInfo(
                ModItems.JAM_DEBRIS.get(),
                Component.translatable(
                        "rngtech.jei.jam_debris.info",
                        com.rngtech.content.blockentity.JamDebris.MIN_PROCESSING_LEVEL
                )
        );

        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        List<RecipeHolder<CalibrationRecipe>> recipes =
                level.getRecipeManager().getAllRecipesFor(ModRecipes.CALIBRATION_TYPE.get());
        registration.addRecipes(JeiRecipeTypes.CALIBRATION, recipes);
        List<RecipeHolder<CrusherRecipe>> crusherRecipes =
                level.getRecipeManager().getAllRecipesFor(ModRecipes.CRUSHER_TYPE.get());
        registration.addRecipes(JeiRecipeTypes.CRUSHER, crusherRecipes);
        List<RecipeHolder<FurnaceRecipe>> furnaceRecipes =
                level.getRecipeManager().getAllRecipesFor(ModRecipes.FURNACE_TYPE.get());
        registration.addRecipes(JeiRecipeTypes.FURNACE, furnaceRecipes);
        List<RecipeHolder<AlloyFurnaceRecipe>> alloyFurnaceRecipes =
                level.getRecipeManager().getAllRecipesFor(ModRecipes.ALLOY_FURNACE_TYPE.get());
        registration.addRecipes(JeiRecipeTypes.ALLOY_FURNACE, alloyFurnaceRecipes);
        List<RecipeHolder<PotentialReactorRecipe>> potentialReactorRecipes =
                level.getRecipeManager().getAllRecipesFor(ModRecipes.POTENTIAL_REACTOR_TYPE.get());
        registration.addRecipes(JeiRecipeTypes.POTENTIAL_REACTOR, potentialReactorRecipes);
        List<RecipeHolder<MinersCompanionRecoveryRecipe>> minersCompanionRecoveryRecipes =
                level.getRecipeManager().getAllRecipesFor(ModRecipes.MINERS_COMPANION_RECOVERY_TYPE.get());
        registration.addRecipes(JeiRecipeTypes.MINERS_COMPANION_RECOVERY, minersCompanionRecoveryRecipes);
        List<RecipeHolder<ExoticAffixForgeRecipe>> exoticAffixForgeRecipes =
                level.getRecipeManager().getAllRecipesFor(ModRecipes.EXOTIC_AFFIX_FORGE_TYPE.get());
        registration.addRecipes(JeiRecipeTypes.EXOTIC_AFFIX_FORGE, exoticAffixForgeRecipes);
        List<RecipeHolder<CorrosionCellRecipe>> corrosionCellRecipes =
                level.getRecipeManager().getAllRecipesFor(ModRecipes.CORROSION_CELL_TYPE.get());
        registration.addRecipes(JeiRecipeTypes.CORROSION_CELL, corrosionCellRecipes);
        List<RecipeHolder<ComponentRecyclingRecipe>> componentRecyclingRecipes =
                level.getRecipeManager().getAllRecipesFor(ModRecipes.COMPONENT_RECYCLING_TYPE.get());
        registration.addRecipes(JeiRecipeTypes.COMPONENT_RECYCLING, componentRecyclingRecipes);
        List<RecipeHolder<MetalPressRecipe>> metalPressRecipes =
                level.getRecipeManager().getAllRecipesFor(ModRecipes.METAL_PRESS_TYPE.get());
        registration.addRecipes(JeiRecipeTypes.METAL_PRESS, metalPressRecipes);
        List<RecipeHolder<MelterRecipe>> melterRecipes =
                level.getRecipeManager().getAllRecipesFor(ModRecipes.MELTER_TYPE.get());
        registration.addRecipes(JeiRecipeTypes.MELTER, melterRecipes);
        List<RecipeHolder<BatteryAssemblyRecipe>> batteryAssemblyRecipes =
                level.getRecipeManager().getAllRecipesFor(ModRecipes.BATTERY_ASSEMBLY_TYPE.get());
        registration.addRecipes(JeiRecipeTypes.BATTERY_ASSEMBLY, batteryAssemblyRecipes);
        List<RecipeHolder<CoalGasificationRecipe>> coalGasificationRecipes =
                level.getRecipeManager().getAllRecipesFor(ModRecipes.COAL_GASIFICATION_TYPE.get());
        registration.addRecipes(JeiRecipeTypes.COAL_GASIFICATION, coalGasificationRecipes);
        List<RecipeHolder<GasCombustionRecipe>> gasCombustionRecipes =
                level.getRecipeManager().getAllRecipesFor(ModRecipes.GAS_COMBUSTION_TYPE.get());
        registration.addRecipes(JeiRecipeTypes.GAS_COMBUSTION, gasCombustionRecipes);
        List<RecipeHolder<GasReformingRecipe>> gasReformingRecipes =
                level.getRecipeManager().getAllRecipesFor(ModRecipes.GAS_REFORMING_TYPE.get());
        registration.addRecipes(JeiRecipeTypes.GAS_REFORMING, gasReformingRecipes);
        List<RecipeHolder<AmmoniaSynthesisRecipe>> ammoniaSynthesisRecipes =
                level.getRecipeManager().getAllRecipesFor(ModRecipes.AMMONIA_SYNTHESIS_TYPE.get());
        registration.addRecipes(JeiRecipeTypes.AMMONIA_SYNTHESIS, ammoniaSynthesisRecipes);
        List<RecipeHolder<AmmoniaPowerCycleRecipe>> ammoniaPowerCycleRecipes =
                level.getRecipeManager().getAllRecipesFor(ModRecipes.AMMONIA_POWER_CYCLE_TYPE.get());
        registration.addRecipes(JeiRecipeTypes.AMMONIA_POWER_CYCLE, ammoniaPowerCycleRecipes);
        List<RecipeHolder<CavitationRecipe>> cavitationRecipes =
                level.getRecipeManager().getAllRecipesFor(ModRecipes.CAVITATION_TYPE.get());
        registration.addRecipes(JeiRecipeTypes.CAVITATION, cavitationRecipes);
        List<RecipeHolder<VacuumCollapseRecipe>> vacuumCollapseRecipes =
                level.getRecipeManager().getAllRecipesFor(ModRecipes.VACUUM_COLLAPSE_TYPE.get());
        registration.addRecipes(JeiRecipeTypes.VACUUM_COLLAPSE, vacuumCollapseRecipes);
        List<RecipeHolder<AlgaeGrowthRecipe>> algaeGrowthRecipes =
                level.getRecipeManager().getAllRecipesFor(ModRecipes.ALGAE_GROWTH_TYPE.get());
        registration.addRecipes(JeiRecipeTypes.ALGAE_GROWTH, algaeGrowthRecipes);
        List<RecipeHolder<WoodenDehumidifierConversionRecipe>> woodenDehumidifierRecipes =
                level.getRecipeManager().getAllRecipesFor(ModRecipes.WOODEN_DEHUMIDIFIER_CONVERSION_TYPE.get());
        registration.addRecipes(JeiRecipeTypes.WOODEN_DEHUMIDIFIER_CONVERSION, woodenDehumidifierRecipes);
        List<RecipeHolder<DesiccantAbsorptionRecipe>> desiccantAbsorptionRecipes =
                level.getRecipeManager().getAllRecipesFor(ModRecipes.DESICCANT_ABSORPTION_TYPE.get());
        registration.addRecipes(JeiRecipeTypes.DESICCANT_ABSORPTION, desiccantAbsorptionRecipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        Set<Item> machineGearCatalysts = new HashSet<>();
        for (GearMachineSpec spec : GearSlotCatalog.all()) {
            for (ItemStack stack : spec.catalystCopies()) {
                Item item = stack.getItem();
                if (machineGearCatalysts.add(item)) {
                    registration.addRecipeCatalyst(item, JeiRecipeTypes.MACHINE_GEAR);
                }
            }
        }
        registration.addRecipeCatalyst(ModItems.AFFIX_FORGE.get(), JeiRecipeTypes.AFFIX_FORGE);
        registration.addRecipeCatalyst(ModItems.EXOTIC_AFFIX_FORGE.get(), JeiRecipeTypes.EXOTIC_AFFIX_FORGE);
        for (SolidFuelBurnerChassis chassis : SolidFuelBurnerChassis.values()) {
            registration.addRecipeCatalyst(ModItems.solidFuelBurner(chassis).get(), JeiRecipeTypes.SOLID_FUEL_BURNING);
        }
        registration.addRecipeCatalyst(ModItems.BIO_GENERATOR.get(), JeiRecipeTypes.BIO_GENERATOR);
        registration.addRecipeCatalyst(ModItems.WOODEN_COMPOSTER.get(), JeiRecipeTypes.WOODEN_COMPOSTER);
        for (CrusherChassisMaterial material : CrusherChassisMaterial.values()) {
            registration.addRecipeCatalyst(ModItems.crusherChassis(material).get(), JeiRecipeTypes.CRUSHER);
        }
        for (FurnaceChassisMaterial material : FurnaceChassisMaterial.values()) {
            registration.addRecipeCatalyst(ModItems.furnaceChassis(material).get(), JeiRecipeTypes.FURNACE);
        }
        for (AlloyFurnaceChassisMaterial material : AlloyFurnaceChassisMaterial.values()) {
            registration.addRecipeCatalyst(ModItems.alloyFurnaceChassis(material).get(), JeiRecipeTypes.ALLOY_FURNACE);
        }
        registration.addRecipeCatalyst(ModItems.POTENTIAL_REACTOR.get(), JeiRecipeTypes.POTENTIAL_REACTOR);
        registration.addRecipeCatalyst(ModItems.MINERS_COMPANION.get(), JeiRecipeTypes.MINERS_COMPANION_RECOVERY);
        registration.addRecipeCatalyst(ModItems.CORROSION_CELL.get(), JeiRecipeTypes.CORROSION_CELL);
        for (ComponentRecyclerChassis chassis : ComponentRecyclerChassis.values()) {
            registration.addRecipeCatalyst(ModItems.componentRecycler(chassis).get(), JeiRecipeTypes.COMPONENT_RECYCLING);
        }
        registration.addRecipeCatalyst(ModItems.CRUDE_METAL_PRESS.get(), JeiRecipeTypes.METAL_PRESS);
        registration.addRecipeCatalyst(ModItems.METAL_PRESS.get(), JeiRecipeTypes.METAL_PRESS);
        registration.addRecipeCatalyst(ModItems.MELTER.get(), JeiRecipeTypes.MELTER);
        registration.addRecipeCatalyst(ModItems.BATTERY_ASSEMBLER.get(), JeiRecipeTypes.BATTERY_ASSEMBLY);
        registration.addRecipeCatalyst(ModItems.COAL_GASIFIER.get(), JeiRecipeTypes.COAL_GASIFICATION);
        registration.addRecipeCatalyst(ModItems.SYNGAS_COMBUSTOR.get(), JeiRecipeTypes.GAS_COMBUSTION);
        registration.addRecipeCatalyst(ModItems.STEAM_METHANE_REFORMER.get(), JeiRecipeTypes.GAS_REFORMING);
        registration.addRecipeCatalyst(ModItems.AMMONIA_SYNTHESIZER.get(), JeiRecipeTypes.AMMONIA_SYNTHESIS);
        registration.addRecipeCatalyst(ModItems.AMMONIA_FUEL_CELL.get(), JeiRecipeTypes.AMMONIA_POWER_CYCLE);
        registration.addRecipeCatalyst(ModItems.CAVITATION_GENERATOR.get(), JeiRecipeTypes.CAVITATION);
        registration.addRecipeCatalyst(ModItems.VACUUM_COLLAPSE_GENERATOR.get(), JeiRecipeTypes.VACUUM_COLLAPSE);
        registration.addRecipeCatalyst(ModItems.ALGAE_PHOTOBIOREACTOR.get(), JeiRecipeTypes.ALGAE_GROWTH);
        registration.addRecipeCatalyst(ModItems.WOODEN_DEHUMIDIFIER.get(), JeiRecipeTypes.WOODEN_DEHUMIDIFIER_CONVERSION);
        registration.addRecipeCatalyst(ModItems.SILICA_GEL_DEHUMIDIFIER.get(), JeiRecipeTypes.DESICCANT_ABSORPTION);
        for (ResonanceCalibratorChassis chassis : ResonanceCalibratorChassis.values()) {
            registration.addRecipeCatalyst(ModItems.resonanceCalibrator(chassis).get(), JeiRecipeTypes.CALIBRATION);
        }
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registerProcessRecipeLineClickArea(
                registration,
                ExoticAffixForgeScreen.class,
                screen -> screen.getMenu().selectedTab() == ExoticAffixForgeMenu.TAB_PROCESSING,
                96,
                105,
                112,
                8,
                JeiRecipeTypes.EXOTIC_AFFIX_FORGE
        );
        registerProcessRecipeLineClickArea(
                registration,
                SolidFuelBurnerScreen.class,
                screen -> screen.getMenu().selectedTab() == SolidFuelBurnerMenu.TAB_PROCESSING,
                137,
                48,
                69,
                7,
                JeiRecipeTypes.SOLID_FUEL_BURNING
        );
        registerProcessRecipeLineClickArea(
                registration,
                BioGeneratorScreen.class,
                screen -> screen.getMenu().selectedTab() == BioGeneratorMenu.TAB_PROCESSING,
                126,
                48,
                80,
                7,
                JeiRecipeTypes.BIO_GENERATOR
        );
        registerProcessRecipeLineClickArea(
                registration,
                WoodenComposterScreen.class,
                screen -> true,
                8,
                91,
                162,
                8,
                JeiRecipeTypes.WOODEN_COMPOSTER
        );
        registerProcessRecipeLineClickArea(
                registration,
                CrusherScreen.class,
                screen -> screen.getMenu().selectedTab() == CrusherMenu.TAB_PROCESSING,
                86,
                51,
                69,
                8,
                JeiRecipeTypes.CRUSHER
        );
        registerProcessRecipeLineClickArea(
                registration,
                FurnaceScreen.class,
                screen -> screen.getMenu().selectedTab() == FurnaceMenu.TAB_PROCESSING,
                86,
                43,
                69,
                30,
                JeiRecipeTypes.FURNACE
        );
        registerProcessRecipeLineClickArea(
                registration,
                AlloyFurnaceScreen.class,
                screen -> screen.getMenu().selectedTab() == AlloyFurnaceMenu.TAB_PROCESSING,
                98,
                51,
                88,
                8,
                JeiRecipeTypes.ALLOY_FURNACE
        );
        registerProcessRecipeLineClickArea(
                registration,
                PotentialReactorScreen.class,
                screen -> screen.getMenu().selectedTab() == PotentialReactorMenu.TAB_PROCESSING,
                137,
                48,
                69,
                7,
                JeiRecipeTypes.POTENTIAL_REACTOR
        );
        registerProcessRecipeLineClickArea(
                registration,
                MinersCompanionScreen.class,
                screen -> screen.getMenu().selectedTab() == MinersCompanionMenu.TAB_PROCESS,
                40,
                87,
                84,
                8,
                JeiRecipeTypes.MINERS_COMPANION_RECOVERY
        );
        registerProcessRecipeLineClickArea(
                registration,
                CorrosionCellScreen.class,
                screen -> screen.getMenu().selectedTab() == CorrosionCellMenu.TAB_PROCESSING,
                137,
                48,
                69,
                7,
                JeiRecipeTypes.CORROSION_CELL
        );
        registerProcessRecipeLineClickArea(
                registration,
                ComponentRecyclerScreen.class,
                screen -> screen.getMenu().selectedTab() == ComponentRecyclerMenu.TAB_PROCESSING,
                84,
                50,
                76,
                7,
                JeiRecipeTypes.COMPONENT_RECYCLING
        );
        registerProcessRecipeLineClickArea(
                registration,
                MetalPressScreen.class,
                screen -> screen.getMenu().selectedTab() == MetalPressMenu.TAB_PROCESSING,
                86,
                43,
                69,
                16,
                JeiRecipeTypes.METAL_PRESS
        );
        registerProcessRecipeLineClickArea(
                registration,
                MelterScreen.class,
                screen -> screen.getMenu().selectedTab() == MelterMenu.TAB_PROCESSING,
                104,
                51,
                58,
                8,
                JeiRecipeTypes.MELTER
        );
        registerProcessRecipeLineClickArea(
                registration,
                ResonanceCalibratorScreen.class,
                screen -> screen.getMenu().selectedTab() == ResonanceCalibratorMenu.TAB_PROCESSING,
                92,
                74,
                76,
                8,
                JeiRecipeTypes.CALIBRATION
        );
        registerProcessRecipeLineClickArea(
                registration,
                BatteryAssemblerScreen.class,
                screen -> screen.getMenu().selectedTab() == BatteryAssemblerMenu.TAB_PROCESSING,
                146,
                50,
                34,
                8,
                JeiRecipeTypes.BATTERY_ASSEMBLY
        );
        registerGasChemistryRecipeClickAreas(registration);
        registerProcessRecipeLineClickArea(
                registration,
                AmmoniaSynthesizerScreen.class,
                screen -> screen.getMenu().selectedTab() == AmmoniaSynthesizerMenu.TAB_PROCESSING,
                92,
                54,
                48,
                8,
                JeiRecipeTypes.AMMONIA_SYNTHESIS
        );
        registerProcessRecipeLineClickArea(
                registration,
                CavitationGeneratorScreen.class,
                screen -> screen.getMenu().selectedTab() == CavitationGeneratorMenu.TAB_PROCESSING,
                84,
                49,
                72,
                7,
                JeiRecipeTypes.CAVITATION
        );
        registerProcessRecipeLineClickArea(
                registration,
                VacuumCollapseGeneratorScreen.class,
                screen -> screen.getMenu().selectedTab() == VacuumCollapseGeneratorMenu.TAB_PROCESSING,
                90,
                48,
                66,
                7,
                JeiRecipeTypes.VACUUM_COLLAPSE
        );
        registerProcessRecipeLineClickArea(
                registration,
                AlgaePhotobioreactorScreen.class,
                screen -> screen.getMenu().selectedTab() == AlgaePhotobioreactorMenu.TAB_PROCESSING,
                98,
                53,
                58,
                8,
                JeiRecipeTypes.ALGAE_GROWTH
        );
        registerProcessRecipeLineClickArea(
                registration,
                AmmoniaFuelCellScreen.class,
                screen -> screen.getMenu().selectedTab() == AmmoniaFuelCellMenu.TAB_PROCESSING,
                88,
                54,
                38,
                8,
                JeiRecipeTypes.AMMONIA_POWER_CYCLE
        );
        registerProcessRecipeLineClickArea(
                registration,
                WoodenDehumidifierScreen.class,
                screen -> true,
                88,
                56,
                66,
                8,
                JeiRecipeTypes.WOODEN_DEHUMIDIFIER_CONVERSION
        );
        registerProcessRecipeLineClickArea(
                registration,
                SilicaGelDehumidifierScreen.class,
                screen -> true,
                72,
                33,
                58,
                56,
                JeiRecipeTypes.DESICCANT_ABSORPTION
        );
    }

    private static void registerGasChemistryRecipeClickAreas(IGuiHandlerRegistration registration) {
        IGuiClickableArea coalGasificationArea = recipeLineClickArea(96, 54, 38, 8, JeiRecipeTypes.COAL_GASIFICATION);
        IGuiClickableArea gasCombustionArea = recipeLineClickArea(92, 54, 48, 8, JeiRecipeTypes.GAS_COMBUSTION);
        IGuiClickableArea gasReformingArea = recipeLineClickArea(92, 54, 48, 8, JeiRecipeTypes.GAS_REFORMING);
        registration.addGuiContainerHandler(
                GasChemistryScreen.class,
                new IGuiContainerHandler<GasChemistryScreen>() {
                    @Override
                    public Collection<IGuiClickableArea> getGuiClickableAreas(
                            GasChemistryScreen screen,
                            double guiMouseX,
                            double guiMouseY
                    ) {
                        if (screen.getMenu().selectedTab() != GasChemistryMenu.TAB_PROCESSING) {
                            return List.of();
                        }
                        return switch (screen.getMenu().machine()) {
                            case COAL_GASIFIER -> List.of(coalGasificationArea);
                            case SYNGAS_COMBUSTOR -> List.of(gasCombustionArea);
                            case STEAM_METHANE_REFORMER -> List.of(gasReformingArea);
                        };
                    }
                }
        );
    }

    private static <T extends AbstractContainerScreen<?>> void registerProcessRecipeLineClickArea(
            IGuiHandlerRegistration registration,
            Class<T> screenClass,
            Predicate<T> isProcessTab,
            int x,
            int y,
            int width,
            int height,
            mezz.jei.api.recipe.RecipeType<?> recipeType
    ) {
        IGuiClickableArea clickArea = recipeLineClickArea(x, y, width, height, recipeType);
        registration.addGuiContainerHandler(
                screenClass,
                new IGuiContainerHandler<T>() {
                    @Override
                    public Collection<IGuiClickableArea> getGuiClickableAreas(
                            T screen,
                            double guiMouseX,
                            double guiMouseY
                    ) {
                        if (!isProcessTab.test(screen)) {
                            return List.of();
                        }
                        return List.of(clickArea);
                    }
                }
        );
    }

    private static IGuiClickableArea recipeLineClickArea(
            int x,
            int y,
            int width,
            int height,
            mezz.jei.api.recipe.RecipeType<?> recipeType
    ) {
        return new IGuiClickableArea() {
            private final Rect2i area = new Rect2i(x, y, width, height);

            @Override
            public Rect2i getArea() {
                return area;
            }

            @Override
            public boolean isTooltipEnabled() {
                return false;
            }

            @Override
            public void onClick(IFocusFactory focusFactory, IRecipesGui recipesGui) {
                recipesGui.showTypes(List.of(recipeType));
            }
        };
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(
                AffixForgeMenu.class,
                ModMenus.AFFIX_FORGE.get(),
                JeiRecipeTypes.AFFIX_FORGE,
                0,
                2,
                6,
                36
        );
        registration.addRecipeTransferHandler(
                ExoticAffixForgeMenu.class,
                ModMenus.EXOTIC_AFFIX_FORGE.get(),
                JeiRecipeTypes.EXOTIC_AFFIX_FORGE,
                0,
                2,
                4,
                36
        );
        registration.addRecipeTransferHandler(
                BioGeneratorMenu.class,
                ModMenus.BIO_GENERATOR.get(),
                JeiRecipeTypes.BIO_GENERATOR,
                0,
                1,
                5,
                36
        );
        registration.addRecipeTransferHandler(
                WoodenComposterMenu.class,
                ModMenus.WOODEN_COMPOSTER.get(),
                JeiRecipeTypes.WOODEN_COMPOSTER,
                0,
                1,
                30,
                36
        );
        registration.addRecipeTransferHandler(
                WoodenDehumidifierMenu.class,
                ModMenus.WOODEN_DEHUMIDIFIER.get(),
                JeiRecipeTypes.WOODEN_DEHUMIDIFIER_CONVERSION,
                0,
                1,
                3,
                36
        );
        registration.addRecipeTransferHandler(
                SilicaGelDehumidifierMenu.class,
                ModMenus.SILICA_GEL_DEHUMIDIFIER.get(),
                JeiRecipeTypes.DESICCANT_ABSORPTION,
                0,
                3,
                6,
                36
        );
        registration.addRecipeTransferHandler(
                CrusherMenu.class,
                ModMenus.CRUSHER.get(),
                JeiRecipeTypes.CRUSHER,
                0,
                1,
                7,
                36
        );
        registration.addRecipeTransferHandler(
                CorrosionCellMenu.class,
                ModMenus.CORROSION_CELL.get(),
                JeiRecipeTypes.CORROSION_CELL,
                0,
                2,
                8,
                36
        );
        registration.addRecipeTransferHandler(
                FurnaceMenu.class,
                ModMenus.FURNACE.get(),
                JeiRecipeTypes.FURNACE,
                0,
                1,
                16,
                36
        );
        registration.addRecipeTransferHandler(
                AlloyFurnaceMenu.class,
                ModMenus.ALLOY_FURNACE.get(),
                JeiRecipeTypes.ALLOY_FURNACE,
                0,
                4,
                11,
                36
        );
        registration.addRecipeTransferHandler(
                PotentialReactorMenu.class,
                ModMenus.POTENTIAL_REACTOR.get(),
                JeiRecipeTypes.POTENTIAL_REACTOR,
                0,
                1,
                7,
                36
        );
        registration.addRecipeTransferHandler(
                ComponentRecyclerMenu.class,
                ModMenus.COMPONENT_RECYCLER.get(),
                JeiRecipeTypes.COMPONENT_RECYCLING,
                0,
                1,
                9,
                36
        );
        registration.addRecipeTransferHandler(new ResonanceCalibrationRecipeTransferInfo());
        registration.addRecipeTransferHandler(new MetalPressRecipeTransferInfo());
    }

    private static List<ItemStack> uniqueItemStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        for (BatteryCellMaterial material : BatteryCellMaterial.values()) {
            if (material.unique()) {
                stacks.add(ModItems.batteryCell(material).get().getDefaultInstance());
            }
        }
        return stacks;
    }
}
