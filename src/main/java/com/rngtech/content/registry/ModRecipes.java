package com.rngtech.content.registry;

import com.rngtech.RNGTech;
import com.rngtech.content.recipe.AlgaeGrowthRecipe;
import com.rngtech.content.recipe.AlloyFurnaceRecipe;
import com.rngtech.content.recipe.AmmoniaPowerCycleRecipe;
import com.rngtech.content.recipe.AmmoniaSynthesisRecipe;
import com.rngtech.content.recipe.BatteryAssemblyRecipe;
import com.rngtech.content.recipe.CalibratedShapedRecipe;
import com.rngtech.content.recipe.CalibrationRecipe;
import com.rngtech.content.recipe.CarbonExhaustBucketRecipe;
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
import com.rngtech.content.recipe.ToolDamageShapedRecipe;
import com.rngtech.content.recipe.ToolDamageShapelessRecipe;
import com.rngtech.content.recipe.TraitShapedRecipe;
import com.rngtech.content.recipe.UnidentifiedTraitIdentifyRecipe;
import com.rngtech.content.recipe.UnidentifiedTraitIngredient;
import com.rngtech.content.recipe.VacuumCollapseRecipe;
import com.rngtech.content.recipe.WoodenDehumidifierConversionRecipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModRecipes {
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, RNGTech.MOD_ID);
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, RNGTech.MOD_ID);
    private static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, RNGTech.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CrusherRecipe>> CRUSHER_SERIALIZER =
            RECIPE_SERIALIZERS.register("crusher", CrusherRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<CrusherRecipe>> CRUSHER_TYPE =
            RECIPE_TYPES.register("crusher", () -> RecipeType.simple(RNGTech.id("crusher")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FurnaceRecipe>> FURNACE_SERIALIZER =
            RECIPE_SERIALIZERS.register("furnace", FurnaceRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<FurnaceRecipe>> FURNACE_TYPE =
            RECIPE_TYPES.register("furnace", () -> RecipeType.simple(RNGTech.id("furnace")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AlloyFurnaceRecipe>> ALLOY_FURNACE_SERIALIZER =
            RECIPE_SERIALIZERS.register("alloy_furnace", AlloyFurnaceRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<AlloyFurnaceRecipe>> ALLOY_FURNACE_TYPE =
            RECIPE_TYPES.register("alloy_furnace", () -> RecipeType.simple(RNGTech.id("alloy_furnace")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PotentialReactorRecipe>> POTENTIAL_REACTOR_SERIALIZER =
            RECIPE_SERIALIZERS.register("potential_reactor", PotentialReactorRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<PotentialReactorRecipe>> POTENTIAL_REACTOR_TYPE =
            RECIPE_TYPES.register("potential_reactor", () -> RecipeType.simple(RNGTech.id("potential_reactor")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MinersCompanionRecoveryRecipe>>
            MINERS_COMPANION_RECOVERY_SERIALIZER =
                    RECIPE_SERIALIZERS.register("miners_companion_recovery", MinersCompanionRecoveryRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<MinersCompanionRecoveryRecipe>> MINERS_COMPANION_RECOVERY_TYPE =
            RECIPE_TYPES.register("miners_companion_recovery", () -> RecipeType.simple(RNGTech.id("miners_companion_recovery")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ExoticAffixForgeRecipe>> EXOTIC_AFFIX_FORGE_SERIALIZER =
            RECIPE_SERIALIZERS.register("exotic_affix_forge", ExoticAffixForgeRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<ExoticAffixForgeRecipe>> EXOTIC_AFFIX_FORGE_TYPE =
            RECIPE_TYPES.register("exotic_affix_forge", () -> RecipeType.simple(RNGTech.id("exotic_affix_forge")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CavitationRecipe>> CAVITATION_SERIALIZER =
            RECIPE_SERIALIZERS.register("cavitation", CavitationRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<CavitationRecipe>> CAVITATION_TYPE =
            RECIPE_TYPES.register("cavitation", () -> RecipeType.simple(RNGTech.id("cavitation")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<VacuumCollapseRecipe>> VACUUM_COLLAPSE_SERIALIZER =
            RECIPE_SERIALIZERS.register("vacuum_collapse", VacuumCollapseRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<VacuumCollapseRecipe>> VACUUM_COLLAPSE_TYPE =
            RECIPE_TYPES.register("vacuum_collapse", () -> RecipeType.simple(RNGTech.id("vacuum_collapse")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CorrosionCellRecipe>> CORROSION_CELL_SERIALIZER =
            RECIPE_SERIALIZERS.register("corrosion_cell", CorrosionCellRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<CorrosionCellRecipe>> CORROSION_CELL_TYPE =
            RECIPE_TYPES.register("corrosion_cell", () -> RecipeType.simple(RNGTech.id("corrosion_cell")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ComponentRecyclingRecipe>> COMPONENT_RECYCLING_SERIALIZER =
            RECIPE_SERIALIZERS.register("component_recycling", ComponentRecyclingRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<ComponentRecyclingRecipe>> COMPONENT_RECYCLING_TYPE =
            RECIPE_TYPES.register("component_recycling", () -> RecipeType.simple(RNGTech.id("component_recycling")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MetalPressRecipe>> METAL_PRESS_SERIALIZER =
            RECIPE_SERIALIZERS.register("metal_press", MetalPressRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<MetalPressRecipe>> METAL_PRESS_TYPE =
            RECIPE_TYPES.register("metal_press", () -> RecipeType.simple(RNGTech.id("metal_press")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MelterRecipe>> MELTER_SERIALIZER =
            RECIPE_SERIALIZERS.register("melter", MelterRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<MelterRecipe>> MELTER_TYPE =
            RECIPE_TYPES.register("melter", () -> RecipeType.simple(RNGTech.id("melter")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BatteryAssemblyRecipe>> BATTERY_ASSEMBLY_SERIALIZER =
            RECIPE_SERIALIZERS.register("battery_assembly", BatteryAssemblyRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<BatteryAssemblyRecipe>> BATTERY_ASSEMBLY_TYPE =
            RECIPE_TYPES.register("battery_assembly", () -> RecipeType.simple(RNGTech.id("battery_assembly")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CoalGasificationRecipe>> COAL_GASIFICATION_SERIALIZER =
            RECIPE_SERIALIZERS.register("coal_gasification", CoalGasificationRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<CoalGasificationRecipe>> COAL_GASIFICATION_TYPE =
            RECIPE_TYPES.register("coal_gasification", () -> RecipeType.simple(RNGTech.id("coal_gasification")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<GasCombustionRecipe>> GAS_COMBUSTION_SERIALIZER =
            RECIPE_SERIALIZERS.register("gas_combustion", GasCombustionRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<GasCombustionRecipe>> GAS_COMBUSTION_TYPE =
            RECIPE_TYPES.register("gas_combustion", () -> RecipeType.simple(RNGTech.id("gas_combustion")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<GasReformingRecipe>> GAS_REFORMING_SERIALIZER =
            RECIPE_SERIALIZERS.register("gas_reforming", GasReformingRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<GasReformingRecipe>> GAS_REFORMING_TYPE =
            RECIPE_TYPES.register("gas_reforming", () -> RecipeType.simple(RNGTech.id("gas_reforming")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AmmoniaSynthesisRecipe>> AMMONIA_SYNTHESIS_SERIALIZER =
            RECIPE_SERIALIZERS.register("ammonia_synthesis", AmmoniaSynthesisRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<AmmoniaSynthesisRecipe>> AMMONIA_SYNTHESIS_TYPE =
            RECIPE_TYPES.register("ammonia_synthesis", () -> RecipeType.simple(RNGTech.id("ammonia_synthesis")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AmmoniaPowerCycleRecipe>> AMMONIA_POWER_CYCLE_SERIALIZER =
            RECIPE_SERIALIZERS.register("ammonia_power_cycle", AmmoniaPowerCycleRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<AmmoniaPowerCycleRecipe>> AMMONIA_POWER_CYCLE_TYPE =
            RECIPE_TYPES.register("ammonia_power_cycle", () -> RecipeType.simple(RNGTech.id("ammonia_power_cycle")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AlgaeGrowthRecipe>> ALGAE_GROWTH_SERIALIZER =
            RECIPE_SERIALIZERS.register("algae_growth", AlgaeGrowthRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<AlgaeGrowthRecipe>> ALGAE_GROWTH_TYPE =
            RECIPE_TYPES.register("algae_growth", () -> RecipeType.simple(RNGTech.id("algae_growth")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<WoodenDehumidifierConversionRecipe>>
            WOODEN_DEHUMIDIFIER_CONVERSION_SERIALIZER =
                    RECIPE_SERIALIZERS.register("wooden_dehumidifier_conversion", WoodenDehumidifierConversionRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<WoodenDehumidifierConversionRecipe>>
            WOODEN_DEHUMIDIFIER_CONVERSION_TYPE =
                    RECIPE_TYPES.register(
                            "wooden_dehumidifier_conversion",
                            () -> RecipeType.simple(RNGTech.id("wooden_dehumidifier_conversion"))
                    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DesiccantAbsorptionRecipe>>
            DESICCANT_ABSORPTION_SERIALIZER =
                    RECIPE_SERIALIZERS.register("desiccant_absorption", DesiccantAbsorptionRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<DesiccantAbsorptionRecipe>> DESICCANT_ABSORPTION_TYPE =
            RECIPE_TYPES.register("desiccant_absorption", () -> RecipeType.simple(RNGTech.id("desiccant_absorption")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CalibrationRecipe>> CALIBRATION_SERIALIZER =
            RECIPE_SERIALIZERS.register("calibration", CalibrationRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<CalibrationRecipe>> CALIBRATION_TYPE =
            RECIPE_TYPES.register("calibration", () -> RecipeType.simple(RNGTech.id("calibration")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TraitShapedRecipe>> TRAIT_SHAPED_SERIALIZER =
            RECIPE_SERIALIZERS.register("trait_shaped", TraitShapedRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<UnidentifiedTraitIdentifyRecipe>>
            UNIDENTIFIED_TRAIT_IDENTIFY_SERIALIZER =
                    RECIPE_SERIALIZERS.register("identify_traits", UnidentifiedTraitIdentifyRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ToolDamageShapedRecipe>> TOOL_DAMAGE_SHAPED_SERIALIZER =
            RECIPE_SERIALIZERS.register("tool_damage_shaped", ToolDamageShapedRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ToolDamageShapelessRecipe>> TOOL_DAMAGE_SHAPELESS_SERIALIZER =
            RECIPE_SERIALIZERS.register("tool_damage_shapeless", ToolDamageShapelessRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CalibratedShapedRecipe>> CALIBRATED_SHAPED_SERIALIZER =
            RECIPE_SERIALIZERS.register("calibrated_shaped", CalibratedShapedRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CarbonExhaustBucketRecipe>> CARBON_EXHAUST_BUCKET_SERIALIZER =
            RECIPE_SERIALIZERS.register("carbon_exhaust_bucket", CarbonExhaustBucketRecipe.Serializer::new);
    public static final DeferredHolder<IngredientType<?>, IngredientType<UnidentifiedTraitIngredient>>
            UNIDENTIFIED_TRAIT_INGREDIENT_TYPE =
                    INGREDIENT_TYPES.register(
                            "unidentified_traits",
                            () -> new IngredientType<>(
                                    UnidentifiedTraitIngredient.CODEC,
                                    UnidentifiedTraitIngredient.STREAM_CODEC
                            )
                    );

    public static void register(IEventBus bus) {
        RECIPE_SERIALIZERS.register(bus);
        RECIPE_TYPES.register(bus);
        INGREDIENT_TYPES.register(bus);
    }

    private ModRecipes() {
    }
}
