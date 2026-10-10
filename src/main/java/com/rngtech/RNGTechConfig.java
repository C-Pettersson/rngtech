package com.rngtech;

import com.rngtech.content.material.MaterialCatalog;
import com.rngtech.content.material.MaterialFamily;
import com.rngtech.content.material.OreCatalog;
import com.rngtech.content.material.OreDefinition;
import com.rngtech.rpg.unique.UniqueCatalog;
import com.rngtech.rpg.unique.UniqueDefinition;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RNGTechConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue CRUSHER_BASE_INPUT_SLOTS = BUILDER
            .comment("The base number of crusher input slots before modifiers are applied.")
            .defineInRange("crusher.baseInputSlots", 1, 1, 2);

    public static final ModConfigSpec.IntValue CRUSHER_ENERGY_CAPACITY = BUILDER
            .comment("FE stored by a crusher's small internal working buffer before an installed Battery Cell is counted.")
            .defineInRange("crusher.internalEnergyCapacity", 200, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue CRUSHER_MAX_ENERGY_INPUT = BUILDER
            .comment("Maximum FE accepted per tick by a crusher.")
            .defineInRange("crusher.maxEnergyInput", 500, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue CRUSHER_NO_BATTERY_CELL_OUTPUT_MULTIPLIER = BUILDER
            .comment("Output amount multiplier applied while a crusher has no Battery Cell installed.")
            .defineInRange("crusher.noBatteryCellOutputMultiplier", 0.75D, 0.01D, 1.0D);

    public static final ModConfigSpec.IntValue CRUSHER_HIGH_HARDNESS_ENERGY_THRESHOLD = BUILDER
            .comment("Crusher recipes at or above this required processing level use the high-hardness energy multiplier.")
            .defineInRange("crusher.highHardnessEnergyThreshold", 7, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue CRUSHER_HIGH_HARDNESS_ENERGY_MULTIPLIER = BUILDER
            .comment("Energy multiplier applied to Crusher recipes at or above the high-hardness threshold.")
            .defineInRange("crusher.highHardnessEnergyMultiplier", 2.0D, 0.01D, 100.0D);

    public static final ModConfigSpec.DoubleValue CRUSHER_UNDER_LEVEL_PENALTY_MULTIPLIER_PER_LEVEL = BUILDER
            .comment("Additional Crusher processing-time and total-FE multiplier applied per missing processing level when the installed Crush Head is below the recipe requirement.")
            .defineInRange("crusher.underLevelPenaltyMultiplierPerLevel", 1.0D, 0.0D, 100.0D);

    public static final ModConfigSpec.IntValue CRUSHER_UNDER_LEVEL_JAM_CHANCE_PER_LEVEL = BUILDER
            .comment("Crusher jam chance per missing processing level, in thousandths, rolled once when an under-level processing cycle starts.")
            .defineInRange("crusher.underLevelJamChancePerLevel", 50, 0, 1000);

    public static final ModConfigSpec.IntValue CRUSHER_UNDER_LEVEL_JAM_TICKS_PER_LEVEL = BUILDER
            .comment("Crusher jam duration in ticks per missing processing level when an under-level processing cycle jams.")
            .defineInRange("crusher.underLevelJamTicksPerLevel", 40, 0, 20 * 60);

    public static final ModConfigSpec.IntValue FURNACE_BASE_INPUT_SLOTS = BUILDER
            .comment("The base number of furnace input slots before modifiers are applied.")
            .defineInRange("furnace.baseInputSlots", 1, 1, 2);

    public static final ModConfigSpec.DoubleValue FURNACE_NO_BATTERY_CELL_PROCESSING_SPEED_MULTIPLIER = BUILDER
            .comment("Processing speed multiplier applied to electric furnace chassis without an installed Battery Cell.")
            .defineInRange("furnace.noBatteryCellProcessingSpeedMultiplier", 0.75D, 0.01D, 1.0D);

    public static final ModConfigSpec.IntValue FURNACE_HIGH_HEAT_ENERGY_THRESHOLD = BUILDER
            .comment("Furnace recipes at or above this minimum temperature use the high-heat energy multiplier.")
            .defineInRange("furnace.highHeatEnergyThreshold", 1750, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue FURNACE_HIGH_HEAT_ENERGY_MULTIPLIER = BUILDER
            .comment("Energy multiplier applied to Furnace recipes at or above the high-heat threshold.")
            .defineInRange("furnace.highHeatEnergyMultiplier", 2.0D, 0.01D, 100.0D);

    public static final ModConfigSpec.DoubleValue CALIBRATION_ENERGY_MULTIPLIER = BUILDER
            .comment("Multiplier applied to Resonance Calibrator recipe FE costs.")
            .defineInRange("calibration.energyMultiplier", 1.0D, 0.01D, 100.0D);

    public static final ModConfigSpec.DoubleValue CALIBRATION_TIME_MULTIPLIER = BUILDER
            .comment("Multiplier applied to Resonance Calibrator recipe processing time.")
            .defineInRange("calibration.timeMultiplier", 1.0D, 0.01D, 100.0D);

    public static final ModConfigSpec.DoubleValue EXOTIC_AFFIX_FORGE_ENERGY_MULTIPLIER = BUILDER
            .comment("Multiplier applied to Exotic Affix Forge recipe FE costs after per-target history scaling.")
            .defineInRange("exoticAffixForge.energyMultiplier", 1.0D, 0.01D, 100.0D);

    public static final ModConfigSpec.DoubleValue EXOTIC_AFFIX_FORGE_TIME_MULTIPLIER = BUILDER
            .comment("Multiplier applied to Exotic Affix Forge recipe processing time.")
            .defineInRange("exoticAffixForge.timeMultiplier", 1.0D, 0.01D, 100.0D);

    public static final ModConfigSpec.BooleanValue SOLAR_GENERATES_IN_OVERWORLD = BUILDER
            .comment("Whether Solar Panels and Solar Array Controllers generate FE in the Overworld.")
            .define("solar.dimensions.overworld", true);

    public static final ModConfigSpec.BooleanValue SOLAR_GENERATES_IN_NETHER = BUILDER
            .comment("Whether Solar Panels and Solar Array Controllers generate FE in the Nether.")
            .define("solar.dimensions.nether", false);

    public static final ModConfigSpec.BooleanValue SOLAR_GENERATES_IN_END = BUILDER
            .comment("Whether Solar Panels and Solar Array Controllers generate FE in the End.")
            .define("solar.dimensions.end", false);

    public static final ModConfigSpec.BooleanValue SOLAR_GENERATES_IN_OTHER_DIMENSIONS = BUILDER
            .comment("Whether Solar Panels and Solar Array Controllers generate FE in dimensions other than the Overworld, Nether, and End.")
            .define("solar.dimensions.other", false);

    public static final ModConfigSpec.IntValue CALIBRATION_STABILITY_BONUS = BUILDER
            .comment("Flat stability adjustment applied to all calibrated outputs. Use negative values to make calibration more punishing.")
            .defineInRange("calibration.stabilityBonus", 0, -100, 100);

    public static final ModConfigSpec.IntValue CALIBRATION_REFINEMENT_POTENTIAL_BONUS = BUILDER
            .comment("Flat Refinement Potential adjustment applied to all calibrated outputs.")
            .defineInRange("calibration.refinementPotentialBonus", 0, -64, 64);

    public static final ModConfigSpec.BooleanValue ASCENDANCY_SEAL_RECIPES_ENABLED = BUILDER
            .comment("Whether the default Ascendancy Seal and Seal Core recipes load. Disable them to award Seals through loot, quests, or custom recipes instead.")
            .define("ascendancy.sealRecipesEnabled", true);

    private static final UniqueLootConfig UNIQUE_LOOT_CONFIG = defineUniqueLoot();
    /** Gates every default Unique loot table; pack sources such as challenge tables and quests are unaffected. */
    public static final ModConfigSpec.BooleanValue UNIQUE_LOOT_ENABLED = UNIQUE_LOOT_CONFIG.enabled();
    public static final Map<String, ModConfigSpec.BooleanValue> UNIQUE_LOOT = UNIQUE_LOOT_CONFIG.uniques();

    public static final Map<String, ModConfigSpec.BooleanValue> MATERIALS = defineMaterials();
    private static final OreWorldgenConfig ORE_WORLDGEN_CONFIG = defineOreWorldgen();
    public static final ModConfigSpec.BooleanValue ORE_WORLDGEN_ENABLED = ORE_WORLDGEN_CONFIG.enabled();
    public static final ModConfigSpec.EnumValue<VanillaOreBridgePolicy> ORE_VANILLA_BRIDGE_POLICY = ORE_WORLDGEN_CONFIG.bridgePolicy();
    public static final Map<String, ModConfigSpec.BooleanValue> ORE_WORLDGEN = ORE_WORLDGEN_CONFIG.materials();

    public static final ModConfigSpec SPEC = BUILDER.build();

    private static Map<String, ModConfigSpec.BooleanValue> defineMaterials() {
        Map<String, ModConfigSpec.BooleanValue> values = new LinkedHashMap<>();
        BUILDER.push("materials");
        for (MaterialFamily material : MaterialCatalog.materialFamilies()) {
            values.put(material.id(), BUILDER
                    .comment(material.defaultReason())
                    .define(material.id(), material.defaultEnabled()));
        }
        BUILDER.pop();
        return Collections.unmodifiableMap(values);
    }

    private static UniqueLootConfig defineUniqueLoot() {
        BUILDER.push("uniques");
        BUILDER.push("loot");
        ModConfigSpec.BooleanValue enabled = BUILDER
                .comment("Whether the default Unique loot tables, such as Nether fortress chests, can drop Uniques.")
                .define("enabled", true);
        Map<String, ModConfigSpec.BooleanValue> values = new LinkedHashMap<>();
        for (UniqueDefinition unique : UniqueCatalog.all()) {
            values.put(unique.id(), BUILDER
                    .comment("Whether the default loot table can drop " + unique.id() + ".")
                    .define(unique.id() + ".enabled", true));
        }
        BUILDER.pop();
        BUILDER.pop();
        return new UniqueLootConfig(enabled, Collections.unmodifiableMap(values));
    }

    /** Whether the default loot tables may drop {@code uniqueId}. */
    public static boolean uniqueLootEnabled(String uniqueId) {
        ModConfigSpec.BooleanValue unique = UNIQUE_LOOT.get(uniqueId);
        return UNIQUE_LOOT_ENABLED.get() && (unique == null || unique.get());
    }

    private static OreWorldgenConfig defineOreWorldgen() {
        BUILDER.push("worldgen");
        BUILDER.push("ores");
        ModConfigSpec.BooleanValue enabled = BUILDER
                .comment("Whether RNGTech biome modifiers place enabled RNGTech ores in new chunks.")
                .define("enabled", true);
        ModConfigSpec.EnumValue<VanillaOreBridgePolicy> bridgePolicy = BUILDER
                .comment("How ordinary vanilla pickaxes interact with RNGTech ore hardness gates.")
                .defineEnum("vanillaToolBridgePolicy", VanillaOreBridgePolicy.EARLY_BRIDGE);
        Map<String, ModConfigSpec.BooleanValue> values = new LinkedHashMap<>();
        for (OreDefinition ore : OreCatalog.ores()) {
            values.put(ore.materialId(), BUILDER
                    .comment("Whether RNGTech default worldgen may place " + ore.materialDisplayName() + " ores.")
                    .define(ore.materialId() + ".enabled", ore.hasDefaultGeneration()));
        }
        BUILDER.pop();
        BUILDER.pop();
        return new OreWorldgenConfig(enabled, bridgePolicy, Collections.unmodifiableMap(values));
    }

    public enum VanillaOreBridgePolicy {
        BOOTSTRAP_ONLY,
        EARLY_BRIDGE,
        VANILLA_TIER_MAPPING,
        MODULAR_ONLY
    }

    private record UniqueLootConfig(ModConfigSpec.BooleanValue enabled, Map<String, ModConfigSpec.BooleanValue> uniques) {
    }

    private record OreWorldgenConfig(
            ModConfigSpec.BooleanValue enabled,
            ModConfigSpec.EnumValue<VanillaOreBridgePolicy> bridgePolicy,
            Map<String, ModConfigSpec.BooleanValue> materials
    ) {
    }

    private RNGTechConfig() {
    }
}
