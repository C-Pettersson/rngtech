package com.rngtech.compat.jei;

import com.rngtech.RNGTech;
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

import mezz.jei.api.recipe.RecipeType;
import net.minecraft.world.item.crafting.RecipeHolder;

final class JeiRecipeTypes {
    static final RecipeType<MachineGearJeiRecipe> MACHINE_GEAR =
            RecipeType.create(RNGTech.MOD_ID, "machine_gear", MachineGearJeiRecipe.class);
    static final RecipeType<AffixForgeJeiRecipe> AFFIX_FORGE =
            RecipeType.create(RNGTech.MOD_ID, "affix_forge", AffixForgeJeiRecipe.class);
    static final RecipeType<SolidFuelBurnerJeiFuel> SOLID_FUEL_BURNING =
            RecipeType.create(RNGTech.MOD_ID, "solid_fuel_burning", SolidFuelBurnerJeiFuel.class);
    static final RecipeType<BioGeneratorJeiFuel> BIO_GENERATOR =
            RecipeType.create(RNGTech.MOD_ID, "bio_generator", BioGeneratorJeiFuel.class);
    static final RecipeType<WoodenComposterJeiRecipe> WOODEN_COMPOSTER =
            RecipeType.create(RNGTech.MOD_ID, "wooden_composter", WoodenComposterJeiRecipe.class);
    static final RecipeType<OreWorldgenJeiRecipe> ORE_WORLDGEN =
            RecipeType.create(RNGTech.MOD_ID, "ore_worldgen", OreWorldgenJeiRecipe.class);
    static final RecipeType<RecipeHolder<CrusherRecipe>> CRUSHER =
            RecipeType.createRecipeHolderType(RNGTech.id("crusher"));
    static final RecipeType<RecipeHolder<FurnaceRecipe>> FURNACE =
            RecipeType.createRecipeHolderType(RNGTech.id("furnace"));
    static final RecipeType<RecipeHolder<AlloyFurnaceRecipe>> ALLOY_FURNACE =
            RecipeType.createRecipeHolderType(RNGTech.id("alloy_furnace"));
    static final RecipeType<RecipeHolder<PotentialReactorRecipe>> POTENTIAL_REACTOR =
            RecipeType.createRecipeHolderType(RNGTech.id("potential_reactor"));
    static final RecipeType<RecipeHolder<MinersCompanionRecoveryRecipe>> MINERS_COMPANION_RECOVERY =
            RecipeType.createRecipeHolderType(RNGTech.id("miners_companion_recovery"));
    static final RecipeType<RecipeHolder<ExoticAffixForgeRecipe>> EXOTIC_AFFIX_FORGE =
            RecipeType.createRecipeHolderType(RNGTech.id("exotic_affix_forge"));
    static final RecipeType<RecipeHolder<CorrosionCellRecipe>> CORROSION_CELL =
            RecipeType.createRecipeHolderType(RNGTech.id("corrosion_cell"));
    static final RecipeType<RecipeHolder<ComponentRecyclingRecipe>> COMPONENT_RECYCLING =
            RecipeType.createRecipeHolderType(RNGTech.id("component_recycling"));
    static final RecipeType<RecipeHolder<MetalPressRecipe>> METAL_PRESS =
            RecipeType.createRecipeHolderType(RNGTech.id("metal_press"));
    static final RecipeType<RecipeHolder<MelterRecipe>> MELTER =
            RecipeType.createRecipeHolderType(RNGTech.id("melter"));
    static final RecipeType<RecipeHolder<BatteryAssemblyRecipe>> BATTERY_ASSEMBLY =
            RecipeType.createRecipeHolderType(RNGTech.id("battery_assembly"));
    static final RecipeType<RecipeHolder<CoalGasificationRecipe>> COAL_GASIFICATION =
            RecipeType.createRecipeHolderType(RNGTech.id("coal_gasification"));
    static final RecipeType<RecipeHolder<GasCombustionRecipe>> GAS_COMBUSTION =
            RecipeType.createRecipeHolderType(RNGTech.id("gas_combustion"));
    static final RecipeType<RecipeHolder<GasReformingRecipe>> GAS_REFORMING =
            RecipeType.createRecipeHolderType(RNGTech.id("gas_reforming"));
    static final RecipeType<RecipeHolder<AmmoniaSynthesisRecipe>> AMMONIA_SYNTHESIS =
            RecipeType.createRecipeHolderType(RNGTech.id("ammonia_synthesis"));
    static final RecipeType<RecipeHolder<AmmoniaPowerCycleRecipe>> AMMONIA_POWER_CYCLE =
            RecipeType.createRecipeHolderType(RNGTech.id("ammonia_power_cycle"));
    static final RecipeType<RecipeHolder<CavitationRecipe>> CAVITATION =
            RecipeType.createRecipeHolderType(RNGTech.id("cavitation"));
    static final RecipeType<RecipeHolder<VacuumCollapseRecipe>> VACUUM_COLLAPSE =
            RecipeType.createRecipeHolderType(RNGTech.id("vacuum_collapse"));
    static final RecipeType<RecipeHolder<AlgaeGrowthRecipe>> ALGAE_GROWTH =
            RecipeType.createRecipeHolderType(RNGTech.id("algae_growth"));
    static final RecipeType<RecipeHolder<WoodenDehumidifierConversionRecipe>> WOODEN_DEHUMIDIFIER_CONVERSION =
            RecipeType.createRecipeHolderType(RNGTech.id("wooden_dehumidifier_conversion"));
    static final RecipeType<RecipeHolder<DesiccantAbsorptionRecipe>> DESICCANT_ABSORPTION =
            RecipeType.createRecipeHolderType(RNGTech.id("desiccant_absorption"));
    static final RecipeType<RecipeHolder<CalibrationRecipe>> CALIBRATION =
            RecipeType.createRecipeHolderType(RNGTech.id("calibration"));

    private JeiRecipeTypes() {
    }
}
