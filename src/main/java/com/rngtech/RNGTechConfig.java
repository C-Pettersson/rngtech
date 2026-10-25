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
import java.util.List;
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
            .comment("Share of Bonus Output (Output Amount above 1x) a crusher keeps while it has no Battery Cell installed.")
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

    public static final ModConfigSpec.DoubleValue CALIBRATION_ENERGY_MULTIPLIER = BUILDER
            .comment("Multiplier applied to Resonance Calibrator recipe FE costs.")
            .defineInRange("calibration.energyMultiplier", 1.0D, 0.01D, 100.0D);

    public static final ModConfigSpec.DoubleValue CALIBRATION_TIME_MULTIPLIER = BUILDER
            .comment("Multiplier applied to Resonance Calibrator recipe processing time.")
            .defineInRange("calibration.timeMultiplier", 1.0D, 0.01D, 100.0D);

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

    public static final ModConfigSpec.BooleanValue SOUL_TRANSFER_ALWAYS_SUCCEEDS = BUILDER
            .comment("Whether every Soul Transfer absorbs a machine's Mastery. When false, the stability of the Calibrated Logic Component it was crafted with is the success chance.")
            .define("soulTransfer.alwaysSucceeds", false);

    public static final ModConfigSpec.IntValue SOUL_TRANSFER_FAILURE_XP_LOSS = BUILDER
            .comment("Percent of the machine's Mastery XP lost when a Soul Transfer fails. Late levels need far more XP than early ones, so this costs high-level machines few levels unless it is near 100.")
            .defineInRange("soulTransfer.failureXpLossPercent", 0, 0, 100);

    public static final ModConfigSpec.IntValue SOUL_TRANSFER_FAILURE_LEVEL_LOSS = BUILDER
            .comment("Mastery levels lost when a Soul Transfer fails, after any XP loss. Refunds the latest nodes the tree and installed Gear can spare; never drops below the level the build still needs.")
            .defineInRange("soulTransfer.failureLevelLoss", 0, 0, 99);

    public static final ModConfigSpec.BooleanValue DORMANT_SITES_ENABLED = BUILDER
            .comment("Whether RNGTech generates Dormant Machine sites in new chunks. Placed and already generated machines keep working.")
            .define("dormantMachines.enabled", true);

    public static final ModConfigSpec.EnumValue<DormantFailedAbsorb> DORMANT_FAILED_ABSORB = BUILDER
            .comment("What a failed Soul Transfer absorb does to a Dormant Machine. The Soul Transfer is always used up. NOTHING keeps the soul, and the soulTransfer failure loss settings still apply to it; HUSK loses the soul; EXPLODE loses it in a TNT-strength explosion that leaves the husk.")
            .defineEnum("dormantMachines.failedAbsorb", DormantFailedAbsorb.NOTHING);

    public static final ModConfigSpec.BooleanValue VOLATILE_CATALYST_RECIPES_ENABLED = BUILDER
            .comment("Whether the default Volatile Catalyst recipe loads. Disable it to award catalysts through quests, loot, or custom recipes instead.")
            .define("corruption.volatileCatalystRecipesEnabled", true);

    public static final ModConfigSpec.BooleanValue WORK_ORDER_RECIPES_ENABLED = BUILDER
            .comment("Whether the default Work Order recipes load: the Dispatch Terminal and Requisitions. Disable it to award them through quests, loot, or custom recipes instead.")
            .define("workOrders.recipesEnabled", true);

    public static final ModConfigSpec.BooleanValue WORK_ORDER_RANDOM_ENABLED = BUILDER
            .comment("Whether the Dispatch Terminal and /rngtech work_order give random roll random jobs from the objectives. Disable it to list only the job templates a datapack puts in the terminal's pool.")
            .define("workOrders.randomOrdersEnabled", true);

    public static final ModConfigSpec.EnumValue<RestrictedToolMode> RESTRICTED_TOOLS_MODE = BUILDER
            .comment("What happens to tools in the rngtech:restricted_tools item tag (stone through netherite pickaxes, axes, and shovels by default; wooden tools reach no ore and stay free). NORMAL leaves them alone; WEAKENED slows their mining by restrictedTools.weakenedSpeedMultiplier; USELESS makes them mine, harvest, strip, and flatten like an empty hand.")
            .defineEnum("restrictedTools.mode", RestrictedToolMode.NORMAL);

    public static final ModConfigSpec.DoubleValue RESTRICTED_TOOLS_WEAKENED_SPEED_MULTIPLIER = BUILDER
            .comment("Mining speed multiplier for restricted tools while restrictedTools.mode is WEAKENED.")
            .defineInRange("restrictedTools.weakenedSpeedMultiplier", 0.25D, 0.01D, 1.0D);

    public static final ModConfigSpec.BooleanValue VANILLA_TOOL_RECIPES_ENABLED = BUILDER
            .comment("Whether the vanilla crafting and smithing recipes for stone through netherite pickaxes, axes, and shovels load. Wooden tools, hoes, and swords keep their recipes.")
            .define("restrictedTools.vanillaRecipesEnabled", true);
    private static final UniqueLootConfig UNIQUE_LOOT_CONFIG = defineUniqueLoot();
    /** Gates every default Unique loot table; pack sources such as challenge tables and quests are unaffected. */
    public static final ModConfigSpec.BooleanValue UNIQUE_LOOT_ENABLED = UNIQUE_LOOT_CONFIG.enabled();
    public static final Map<String, ModConfigSpec.BooleanValue> UNIQUE_LOOT = UNIQUE_LOOT_CONFIG.uniques();

    // Phase Array. The operations themselves are rngtech:phase_array recipes; these are the line's own numbers.
    public static final ModConfigSpec.DoubleValue PHASE_COIL_HOLD_CHANCE = BUILDER
            .comment("Phase Array: percent chance a Coil holds the pulse, before part affixes.")
            .defineInRange("phaseArray.coilHoldChance", 92.0D, 0.0D, 100.0D);

    public static final ModConfigSpec.DoubleValue PHASE_COIL_OVERHOLD_CHANCE = BUILDER
            .comment("Phase Array: percent chance a surplus Coil's hold triggers Overhold, before part affixes.")
            .defineInRange("phaseArray.coilOverholdChance", 8.0D, 0.0D, 100.0D);

    public static final ModConfigSpec.IntValue PHASE_PARTIAL_WINDOW = BUILDER
            .comment("Phase Array: Coils short of an operation's count that still count as Partial, before the Emitter's affixes.")
            .defineInRange("phaseArray.partialWindow", 2, 0, 8);

    public static final ModConfigSpec.DoubleValue PHASE_FRACTURE_PER_STRAIN = BUILDER
            .comment("Phase Array: percent Fracture chance per point of Strain above the line's Tolerance.")
            .defineInRange("phaseArray.fracturePerStrainPercent", 1.0D, 0.0D, 100.0D);

    public static final ModConfigSpec.DoubleValue PHASE_QUALITY_BASE = BUILDER
            .comment("Phase Array: a pulse's FE multiplies by this for every qualityDivisor points of the item's Quality.")
            .defineInRange("phaseArray.qualityBase", 1.5D, 1.0D, 10.0D);

    public static final ModConfigSpec.DoubleValue PHASE_QUALITY_DIVISOR = BUILDER
            .comment("Phase Array: Quality points per step of qualityBase. Quality is the sum of the item's affix tiers.")
            .defineInRange("phaseArray.qualityDivisor", 3.5D, 0.1D, 100.0D);

    public static final ModConfigSpec.DoubleValue PHASE_STAGE_FACTOR_0_TO_2 = BUILDER
            .comment("Phase Array: FE factor for a Stage 0 to 2 item.")
            .defineInRange("phaseArray.stageFactor.stage0to2", 0.1D, 0.0001D, 100.0D);

    public static final ModConfigSpec.DoubleValue PHASE_STAGE_FACTOR_3_TO_4 = BUILDER
            .comment("Phase Array: FE factor for a Stage 3 or 4 item.")
            .defineInRange("phaseArray.stageFactor.stage3to4", 0.25D, 0.0001D, 100.0D);

    public static final ModConfigSpec.DoubleValue PHASE_STAGE_FACTOR_5_TO_6 = BUILDER
            .comment("Phase Array: FE factor for a Stage 5 or 6 item.")
            .defineInRange("phaseArray.stageFactor.stage5to6", 0.5D, 0.0001D, 100.0D);

    public static final ModConfigSpec.DoubleValue PHASE_STAGE_FACTOR_7 = BUILDER
            .comment("Phase Array: FE factor for a Stage 7 item.")
            .defineInRange("phaseArray.stageFactor.stage7", 1.0D, 0.0001D, 100.0D);

    public static final ModConfigSpec.DoubleValue PHASE_STAGE_FACTOR_8 = BUILDER
            .comment("Phase Array: FE factor for a Stage 8 item.")
            .defineInRange("phaseArray.stageFactor.stage8", 1.5D, 0.0001D, 100.0D);

    public static final ModConfigSpec.LongValue PHASE_EMITTER_CHARGE_CAPACITY = BUILDER
            .comment("Phase Array: FE a Phase Emitter adds to the line's charge store, before affixes.")
            .defineInRange("phaseArray.emitterChargeCapacity", 16_000_000L, 1L, Long.MAX_VALUE);

    public static final ModConfigSpec.IntValue PHASE_EMITTER_CHARGE_RATE = BUILDER
            .comment("Phase Array: FE per tick a Phase Emitter adds to the line's Charge Rate, before affixes.")
            .defineInRange("phaseArray.emitterChargeRate", 8_000, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.LongValue PHASE_COIL_CHARGE_CAPACITY = BUILDER
            .comment("Phase Array: FE each Phase Coil adds to the line's charge store, before affixes.")
            .defineInRange("phaseArray.coilChargeCapacity", 4_000_000L, 0L, Long.MAX_VALUE);

    public static final ModConfigSpec.IntValue PHASE_COIL_CHARGE_RATE = BUILDER
            .comment("Phase Array: FE per tick each Phase Coil adds to the line's Charge Rate, before affixes.")
            .defineInRange("phaseArray.coilChargeRate", 2_000, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> PHASE_GLYPH_FLOORS = BUILDER
            .comment("Phase Array: the lowest Glyph tier (1 to 5) each Coil position needs, nine entries from the position next to the Emitter outward.")
            .defineList("phaseArray.glyphFloors", List.of(1, 1, 1, 2, 2, 3, 3, 4, 5), element -> element instanceof Integer tier && tier >= 1 && tier <= 5);

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
                .comment("How far pickaxes that are not Modular tools reach on ore hardness, by their vanilla tier. Items in the rngtech:ore_reach data map use their listed level instead.")
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

    public enum DormantFailedAbsorb {
        NOTHING,
        HUSK,
        EXPLODE
    }

    public enum RestrictedToolMode {
        NORMAL,
        WEAKENED,
        USELESS
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
