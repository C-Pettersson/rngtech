package com.rngtech.rpg;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ModifierEligibilityProfiles {
    public static final String BATTERY_CHASSIS_CHARGED_STORAGE_AFFIX_ID = "charged_storage";
    public static final String BATTERY_CHASSIS_BALANCE_MODE_AFFIX_ID = "balance_mode";
    public static final String CRUSHER_FRAME_AFFIX_ID = "crusher_frame";
    public static final String CRUSHER_THROUGHPUT_AFFIX_ID = "crusher_throughput";
    public static final String ENERGY_GENERATION_FLAT_AFFIX_ID = "energy_generation_flat";
    public static final String FLUID_CAPACITY_FLAT_AFFIX_ID = "fluid_capacity_flat";
    private static final int PROCESSING_SPECIFIC_SPEED_WEIGHT = 75;
    private static final int HIGH_IMPACT_CHANCE_WEIGHT = 35;
    private static final int OVERCLOCKED_WEIGHT = 25;
    private static final int BULK_SPEED_WEIGHT = 25;
    private static final int CRUSHER_PREFIX_WEIGHT = 65;
    private static final int CRUSH_HEAD_PREFIX_WEIGHT = 65;
    private static final int BALANCE_MODE_WEIGHT = 45;
    private static final int CHARGED_STORAGE_WEIGHT = 35;
    private static final int ADDITIONAL_BATTERY_SLOTS_WEIGHT = 60;
    private static final int SOLAR_SPECIAL_WEIGHT = 60;
    private static final int TOOL_VEIN_MINER_WEIGHT = 18;
    private static final int MINERS_COMPANION_FILTER_SLOTS_WEIGHT = 60;

    private static final List<ModifierValueRange> PERCENT_TIER_RANGES = List.of(
            new ModifierValueRange(2, 5, true),
            new ModifierValueRange(4, 10, true),
            new ModifierValueRange(11, 30, true),
            new ModifierValueRange(40, 55, true),
            new ModifierValueRange(60, 75, true),
            new ModifierValueRange(80, 100, true),
            new ModifierValueRange(110, 140, true)
    );
    private static final List<ModifierValueRange> CHANCE_TIER_RANGES = List.of(
            new ModifierValueRange(1, 2, true),
            new ModifierValueRange(2, 3, true),
            new ModifierValueRange(3, 4, true),
            new ModifierValueRange(4, 5, true),
            new ModifierValueRange(5, 7, true),
            new ModifierValueRange(7, 9, true),
            new ModifierValueRange(10, 12, true)
    );
    private static final List<ModifierValueRange> CRUSHER_OUTPUT_GUARD_GRACE_RANGES = List.of(
            ModifierValueRange.fixed(100),
            ModifierValueRange.fixed(200),
            ModifierValueRange.fixed(400),
            ModifierValueRange.fixed(800)
    );
    private static final List<ModifierValueRange> CRUSHER_NO_BATTERY_RETENTION_RANGES = List.of(
            ModifierValueRange.fixed(25),
            ModifierValueRange.fixed(50),
            ModifierValueRange.fixed(75),
            ModifierValueRange.fixed(100)
    );
    private static final List<ModifierValueRange> CRUSHER_INPUT_FILTER_RANGES = List.of(
            ModifierValueRange.fixed(1),
            ModifierValueRange.fixed(2),
            ModifierValueRange.fixed(3),
            ModifierValueRange.fixed(4)
    );
    private static final List<ModifierValueRange> CRUSHER_HIGH_HARDNESS_MITIGATION_RANGES = List.of(
            ModifierValueRange.fixed(15),
            ModifierValueRange.fixed(30),
            ModifierValueRange.fixed(45),
            ModifierValueRange.fixed(60)
    );
    private static final List<ModifierValueRange> CRUSHER_PARALLEL_JOB_RANGES = List.of(
            ModifierValueRange.fixed(1),
            ModifierValueRange.fixed(2),
            ModifierValueRange.fixed(3),
            ModifierValueRange.fixed(5)
    );
    private static final List<ModifierValueRange> PROCESSING_SPECIFIC_SPEED_RANGES = List.of(
            new ModifierValueRange(4, 4, true),
            new ModifierValueRange(6, 6, true),
            new ModifierValueRange(8, 8, true),
            new ModifierValueRange(10, 10, true),
            new ModifierValueRange(13, 13, true),
            new ModifierValueRange(16, 16, true),
            new ModifierValueRange(20, 20, true)
    );
    private static final List<ModifierValueRange> OVERCLOCK_SPEED_RANGES = List.of(
            new ModifierValueRange(8, 10, true),
            new ModifierValueRange(12, 15, true),
            new ModifierValueRange(18, 22, true),
            new ModifierValueRange(25, 30, true),
            new ModifierValueRange(35, 40, true),
            new ModifierValueRange(45, 55, true),
            new ModifierValueRange(65, 80, true)
    );
    private static final List<ModifierValueRange> OVERCLOCK_ENERGY_USAGE_RANGES = List.of(
            new ModifierValueRange(4, 6, true),
            new ModifierValueRange(7, 10, true),
            new ModifierValueRange(12, 15, true),
            new ModifierValueRange(16, 20, true),
            new ModifierValueRange(21, 26, true),
            new ModifierValueRange(28, 35, true),
            new ModifierValueRange(40, 50, true)
    );
    private static final String PROCESSING_SPEED_GROUP = "processing_speed";
    private static final List<ModifierValueRange> MACHINE_ENERGY_CAPACITY_ADD_RANGES = List.of(
            new ModifierValueRange(1000, 3000, true),
            new ModifierValueRange(3000, 8000, true),
            new ModifierValueRange(9000, 20000, true),
            new ModifierValueRange(30000, 50000, true),
            new ModifierValueRange(75000, 100000, true),
            new ModifierValueRange(150000, 250000, true),
            new ModifierValueRange(350000, 500000, true)
    );
    private static final List<ModifierValueRange> BATTERY_CELL_ENERGY_CAPACITY_ADD_RANGES = List.of(
            new ModifierValueRange(1000, 3000, true),
            new ModifierValueRange(4000, 12000, true),
            new ModifierValueRange(16000, 50000, true),
            new ModifierValueRange(75000, 100000, true),
            new ModifierValueRange(150000, 250000, true),
            new ModifierValueRange(350000, 600000, true),
            new ModifierValueRange(800000, 1000000, true)
    );
    private static final List<ModifierValueRange> REFINEMENT_POTENTIAL_BONUS_ADD_RANGES = List.of(
            new ModifierValueRange(1, 1, true),
            new ModifierValueRange(1, 2, true),
            new ModifierValueRange(2, 3, true),
            new ModifierValueRange(3, 4, true)
    );
    private static final List<ModifierValueRange> TOOL_BATTERY_SUPPORT_ADD_RANGES = List.of(
            new ModifierValueRange(1, 1, true),
            new ModifierValueRange(1, 1, true),
            new ModifierValueRange(1, 2, true),
            new ModifierValueRange(2, 2, true)
    );
    private static final List<ModifierValueRange> TOOL_LUCK_ADD_RANGES = List.of(
            new ModifierValueRange(1, 1, true),
            new ModifierValueRange(1, 2, true),
            new ModifierValueRange(2, 3, true),
            new ModifierValueRange(3, 4, true)
    );
    private static final List<ModifierValueRange> TOOL_TREE_FELL_LIMIT_ADD_RANGES = List.of(
            new ModifierValueRange(4, 8, true),
            new ModifierValueRange(8, 16, true),
            new ModifierValueRange(16, 32, true),
            new ModifierValueRange(32, 48, true)
    );
    private static final List<ModifierValueRange> TOOL_VEIN_MINE_LIMIT_RANGES = List.of(
            ModifierValueRange.fixed(3),
            ModifierValueRange.fixed(5),
            ModifierValueRange.fixed(8),
            ModifierValueRange.fixed(12),
            ModifierValueRange.fixed(18),
            ModifierValueRange.fixed(26),
            ModifierValueRange.fixed(40)
    );
    private static final List<ModifierValueRange> TOOL_VEIN_MINE_FE_USAGE_RANGES = List.of(
            new ModifierValueRange(24, 32, true),
            new ModifierValueRange(20, 24, true),
            new ModifierValueRange(16, 20, true),
            new ModifierValueRange(12, 16, true),
            new ModifierValueRange(8, 12, true),
            new ModifierValueRange(6, 8, true),
            new ModifierValueRange(4, 6, true)
    );
    private static final List<ModifierValueRange> TOOL_ATTACK_SPEED_ADD_RANGES = List.of(
            new ModifierValueRange(0.05, 0.10, false),
            new ModifierValueRange(0.10, 0.15, false),
            new ModifierValueRange(0.15, 0.25, false),
            new ModifierValueRange(0.25, 0.35, false),
            new ModifierValueRange(0.35, 0.45, false),
            new ModifierValueRange(0.45, 0.60, false),
            new ModifierValueRange(0.65, 0.80, false)
    );
    private static final List<ModifierValueRange> DURABILITY_ADD_RANGES = List.of(
            new ModifierValueRange(32, 64, true),
            new ModifierValueRange(65, 128, true),
            new ModifierValueRange(129, 256, true),
            new ModifierValueRange(257, 512, true),
            new ModifierValueRange(513, 768, true),
            new ModifierValueRange(769, 1024, true),
            new ModifierValueRange(1025, 1536, true)
    );
    private static final List<ModifierValueRange> TOOL_SELF_REPAIR_ADD_RANGES = List.of(
            ModifierValueRange.fixed(3),
            ModifierValueRange.fixed(4),
            ModifierValueRange.fixed(6),
            ModifierValueRange.fixed(8)
    );
    private static final List<ModifierValueRange> SOLAR_PANEL_LIMIT_ADD_RANGES = List.of(
            ModifierValueRange.fixed(1),
            ModifierValueRange.fixed(1),
            ModifierValueRange.fixed(2),
            ModifierValueRange.fixed(2)
    );
    private static final List<ModifierValueRange> MOONLIGHT_CONVERSION_RANGES = List.of(
            new ModifierValueRange(10, 20, true),
            new ModifierValueRange(20, 30, true),
            new ModifierValueRange(30, 40, true),
            new ModifierValueRange(50, 55, true)
    );
    private static final List<ModifierValueRange> WEATHER_RECOVERY_RANGES = List.of(
            new ModifierValueRange(10, 15, true),
            new ModifierValueRange(16, 25, true),
            new ModifierValueRange(26, 40, true),
            new ModifierValueRange(45, 60, true)
    );
    private static final List<ModifierValueRange> SOLAR_UTILITY_PERCENT_RANGES = List.of(
            new ModifierValueRange(5, 10, true),
            new ModifierValueRange(11, 20, true),
            new ModifierValueRange(21, 35, true),
            new ModifierValueRange(40, 50, true)
    );
    private static final List<ModifierValueRange> PEAK_SOLAR_GENERATION_RANGES = List.of(
            new ModifierValueRange(50, 60, true),
            new ModifierValueRange(61, 75, true),
            new ModifierValueRange(76, 90, true),
            new ModifierValueRange(91, 100, true)
    );
    private static final List<ModifierValueRange> SOLAR_BOOLEAN_RANGES = List.of(
            ModifierValueRange.fixed(1),
            ModifierValueRange.fixed(1),
            ModifierValueRange.fixed(1),
            ModifierValueRange.fixed(1)
    );
    private static final List<ModifierValueRange> RUNTIME_PERCENT_RANGES = List.of(
            new ModifierValueRange(4, 6, true),
            new ModifierValueRange(7, 10, true),
            new ModifierValueRange(11, 16, true),
            new ModifierValueRange(18, 25, true)
    );
    private static final List<ModifierValueRange> RUNTIME_INPUT_SLOT_RANGES = List.of(
            ModifierValueRange.fixed(1),
            ModifierValueRange.fixed(1),
            ModifierValueRange.fixed(2),
            ModifierValueRange.fixed(2)
    );
    private static final List<ModifierValueRange> INPUT_SLOTS_ADD_RANGES = List.of(
            ModifierValueRange.fixed(1),
            ModifierValueRange.fixed(1),
            ModifierValueRange.fixed(1),
            ModifierValueRange.fixed(2),
            ModifierValueRange.fixed(2),
            ModifierValueRange.fixed(2),
            ModifierValueRange.fixed(3)
    );
    private static final List<ModifierValueRange> BATTERY_CHASSIS_BATTERY_SLOTS_ADD_RANGES = List.of(
            ModifierValueRange.fixed(1),
            ModifierValueRange.fixed(1),
            ModifierValueRange.fixed(2),
            ModifierValueRange.fixed(2),
            ModifierValueRange.fixed(2),
            ModifierValueRange.fixed(3),
            ModifierValueRange.fixed(4)
    );
    private static final List<ModifierValueRange> MINERS_COMPANION_FILTER_SLOTS_ADD_RANGES = List.of(
            ModifierValueRange.fixed(1),
            ModifierValueRange.fixed(1),
            ModifierValueRange.fixed(2),
            ModifierValueRange.fixed(3),
            ModifierValueRange.fixed(4),
            ModifierValueRange.fixed(5),
            ModifierValueRange.fixed(7)
    );
    private static final List<ModifierValueRange> MAX_TEMPERATURE_ADD_RANGES = List.of(
            new ModifierValueRange(50, 100, true),
            new ModifierValueRange(100, 200, true),
            new ModifierValueRange(250, 400, true),
            new ModifierValueRange(500, 800, true)
    );
    /** Millibuckets of tank capacity a Fluid Pump adds to the machine it is installed in. */
    private static final List<ModifierValueRange> FLUID_CAPACITY_ADD_RANGES = List.of(
            new ModifierValueRange(250, 500, true),
            new ModifierValueRange(500, 1000, true),
            new ModifierValueRange(1000, 2000, true),
            new ModifierValueRange(2000, 3000, true),
            new ModifierValueRange(3000, 4500, true),
            new ModifierValueRange(4500, 6500, true),
            new ModifierValueRange(6500, 9000, true)
    );
    private static final List<ModifierValueRange> ENERGY_GENERATION_ADD_RANGES = List.of(
            new ModifierValueRange(5, 10, true),
            new ModifierValueRange(10, 20, true),
            new ModifierValueRange(25, 40, true),
            new ModifierValueRange(50, 80, true),
            new ModifierValueRange(100, 150, true),
            new ModifierValueRange(180, 260, true),
            new ModifierValueRange(320, 480, true)
    );
    private static final List<MachineBehavior> PROCESSING_MACHINE_BEHAVIORS = List.of(MachineBehavior.BULK_SPEED);
    private static final List<MachineBehavior> POWER_HEAT_CONTROL_BEHAVIORS =
            List.of(MachineBehavior.BULK_SPEED, MachineBehavior.POWER_GRACE);
    private static final List<MachineBehavior> POWER_GRACE_BEHAVIOR = List.of(MachineBehavior.POWER_GRACE);

    private static final ModifierDefinition MACHINE_ENERGY_CAPACITY_ADD = ModifierDefinition.rollable(
            "reserve_bank",
            "energy_capacity_flat",
            ModifierSlot.PREFIX,
            MachineStat.ENERGY_CAPACITY_FLAT,
            ModifierOperation.ADD,
            MACHINE_ENERGY_CAPACITY_ADD_RANGES
    );
    private static final ModifierDefinition BATTERY_CELL_ENERGY_CAPACITY_ADD = ModifierDefinition.rollable(
            "dense_cell",
            "energy_capacity_flat",
            ModifierSlot.PREFIX,
            MachineStat.ENERGY_CAPACITY_FLAT,
            ModifierOperation.ADD,
            BATTERY_CELL_ENERGY_CAPACITY_ADD_RANGES
    );
    private static final ModifierDefinition ENERGY_CAPACITY = percent(ModifierSlot.PREFIX, MachineStat.ENERGY_CAPACITY);
    private static final ModifierDefinition BATTERY_CHASSIS_CHARGED_STORAGE = ModifierDefinition.rollableUntiered(
            BATTERY_CHASSIS_CHARGED_STORAGE_AFFIX_ID,
            "charged_storage",
            ModifierSlot.PREFIX,
            MachineStat.ENERGY_CAPACITY,
            ModifierOperation.MORE,
            ModifierValueRange.fixed(2.0)
    ).withRollWeight(CHARGED_STORAGE_WEIGHT);
    private static final ModifierDefinition BATTERY_CHASSIS_BALANCE_MODE = ModifierDefinition.rollableUntiered(
            BATTERY_CHASSIS_BALANCE_MODE_AFFIX_ID,
            MachineBehavior.CHARGE_BALANCER.getSerializedName(),
            ModifierSlot.PREFIX,
            MachineStat.BATTERY_SLOTS,
            ModifierOperation.ADD,
            ModifierValueRange.fixed(0)
    ).withRollWeight(BALANCE_MODE_WEIGHT);
    private static final ModifierDefinition BATTERY_CHASSIS_BATTERY_SLOTS = ModifierDefinition.rollable(
            "additional_battery_slots",
            "battery_slots",
            ModifierSlot.PREFIX,
            MachineStat.BATTERY_SLOTS,
            ModifierOperation.ADD,
            BATTERY_CHASSIS_BATTERY_SLOTS_ADD_RANGES
    ).withRollWeight(ADDITIONAL_BATTERY_SLOTS_WEIGHT);
    private static final ModifierDefinition MINERS_COMPANION_FILTER_SLOTS = ModifierDefinition.rollable(
            "miners_companion_filter_slots",
            "miners_companion_filter_slots",
            ModifierSlot.PREFIX,
            MachineStat.BLOCK_FILTER_SLOTS,
            ModifierOperation.ADD,
            MINERS_COMPANION_FILTER_SLOTS_ADD_RANGES
    ).withRollWeight(MINERS_COMPANION_FILTER_SLOTS_WEIGHT);
    private static final ModifierDefinition PROCESSING_SPEED = ModifierDefinition.rollable(
            "processing_speed",
            PROCESSING_SPEED_GROUP,
            ModifierSlot.SUFFIX,
            MachineStat.PROCESSING_SPEED,
            ModifierOperation.INCREASED_PERCENT,
            PERCENT_TIER_RANGES
    );
    private static final ModifierDefinition EFFICIENCY = percent(ModifierSlot.PREFIX, MachineStat.EFFICIENCY);
    private static final ModifierDefinition ENERGY_USAGE = percent(ModifierSlot.SUFFIX, MachineStat.ENERGY_USAGE);
    private static final ModifierDefinition OUTPUT_AMOUNT = percent(ModifierSlot.SUFFIX, MachineStat.OUTPUT_AMOUNT);
    private static final ModifierDefinition HEAT_TRANSFER = percent(ModifierSlot.PREFIX, MachineStat.HEAT_TRANSFER);
    private static final ModifierDefinition MAX_TEMPERATURE = percent(ModifierSlot.PREFIX, MachineStat.MAX_TEMPERATURE);
    private static final ModifierDefinition MAX_TEMPERATURE_ADD = ModifierDefinition.rollable(
            "max_temperature_flat",
            "max_temperature_flat",
            ModifierSlot.PREFIX,
            MachineStat.MAX_TEMPERATURE,
            ModifierOperation.ADD,
            MAX_TEMPERATURE_ADD_RANGES
    );
    private static final ModifierDefinition HEAT_ISOLATION = percent(ModifierSlot.PREFIX, MachineStat.HEAT_ISOLATION);
    private static final ModifierDefinition WARMUP_TIME = reducedPercent(ModifierSlot.PREFIX, MachineStat.WARMUP_TIME);
    private static final ModifierDefinition COOLING_RATE = reducedPercent(ModifierSlot.PREFIX, MachineStat.COOLING_RATE);
    private static final ModifierDefinition FUEL_SLOTS = ModifierDefinition.rollable(
            ModifierSlot.SUFFIX,
            MachineStat.INPUT_SLOTS,
            ModifierOperation.ADD,
            INPUT_SLOTS_ADD_RANGES
    );
    private static final ModifierDefinition ENERGY_GENERATION = percent(ModifierSlot.SUFFIX, MachineStat.ENERGY_GENERATION);
    private static final ModifierDefinition ENERGY_GENERATION_ADD = ModifierDefinition.rollable(
            ENERGY_GENERATION_FLAT_AFFIX_ID,
            ENERGY_GENERATION_FLAT_AFFIX_ID,
            ModifierSlot.PREFIX,
            MachineStat.ENERGY_GENERATION,
            ModifierOperation.ADD,
            ENERGY_GENERATION_ADD_RANGES
    );
    private static final ModifierDefinition ENERGY_TRANSFER = percent(ModifierSlot.SUFFIX, MachineStat.ENERGY_TRANSFER);
    private static final ModifierDefinition FLUID_TRANSFER = percent(ModifierSlot.SUFFIX, MachineStat.FLUID_TRANSFER);
    private static final ModifierDefinition FLUID_CAPACITY = percent(ModifierSlot.SUFFIX, MachineStat.FLUID_CAPACITY);
    private static final ModifierDefinition FLUID_CAPACITY_ADD = ModifierDefinition.rollable(
            FLUID_CAPACITY_FLAT_AFFIX_ID,
            FLUID_CAPACITY_FLAT_AFFIX_ID,
            ModifierSlot.PREFIX,
            MachineStat.FLUID_CAPACITY,
            ModifierOperation.ADD,
            FLUID_CAPACITY_ADD_RANGES
    );
    private static final ModifierDefinition FUEL_EFFICIENCY = percent(ModifierSlot.PREFIX, MachineStat.FUEL_EFFICIENCY);
    private static final ModifierDefinition STABILITY = percent(ModifierSlot.SUFFIX, MachineStat.STABILITY);
    private static final ModifierDefinition TEMPERATURE_STABILITY =
            percent(ModifierSlot.SUFFIX, MachineStat.TEMPERATURE_STABILITY);
    private static final ModifierDefinition OVERHEAT_TOLERANCE =
            percent(ModifierSlot.SUFFIX, MachineStat.OVERHEAT_TOLERANCE);
    private static final ModifierDefinition BURST_TRANSFER = percent(ModifierSlot.SUFFIX, MachineStat.BURST_TRANSFER);
    private static final ModifierDefinition BURST_DURATION = percent(ModifierSlot.SUFFIX, MachineStat.BURST_DURATION);
    private static final ModifierDefinition IDLE_LOSS = percent(ModifierSlot.SUFFIX, MachineStat.IDLE_LOSS);
    private static final ModifierDefinition GLOBAL_MODIFIER_STRENGTH =
            percent(ModifierSlot.SUFFIX, MachineStat.GLOBAL_MODIFIER_STRENGTH);
    private static final ModifierDefinition CALIBRATION_QUALITY =
            percent(ModifierSlot.PREFIX, MachineStat.CALIBRATION_QUALITY);
    private static final ModifierDefinition CALIBRATION_PRECISION =
            percent(ModifierSlot.PREFIX, MachineStat.CALIBRATION_PRECISION);
    private static final ModifierDefinition CATALYST_EFFICIENCY =
            percent(ModifierSlot.SUFFIX, MachineStat.CATALYST_EFFICIENCY);
    private static final ModifierDefinition REFINEMENT_POTENTIAL_BONUS = ModifierDefinition.rollable(
            ModifierSlot.SUFFIX,
            MachineStat.REFINEMENT_POTENTIAL_BONUS,
            ModifierOperation.ADD,
            REFINEMENT_POTENTIAL_BONUS_ADD_RANGES
    );
    private static final ModifierDefinition DURABILITY_PERCENT = percent(ModifierSlot.PREFIX, MachineStat.DURABILITY);
    private static final ModifierDefinition DURABILITY_ADD = ModifierDefinition.rollable(
            "durability_flat",
            "durability_flat",
            ModifierSlot.PREFIX,
            MachineStat.DURABILITY,
            ModifierOperation.ADD,
            DURABILITY_ADD_RANGES
    );
    private static final ModifierDefinition TOOL_SELF_REPAIR = ModifierDefinition.rollable(
            "self_repair",
            "self_repair",
            ModifierSlot.PREFIX,
            MachineStat.SELF_REPAIR,
            ModifierOperation.ADD,
            TOOL_SELF_REPAIR_ADD_RANGES
    );
    private static final ModifierDefinition TOOL_MINING_SPEED = percent(ModifierSlot.SUFFIX, MachineStat.MINING_SPEED);
    private static final ModifierDefinition TOOL_ATTACK_SPEED = ModifierDefinition.rollable(
            ModifierSlot.SUFFIX,
            MachineStat.ATTACK_SPEED,
            ModifierOperation.ADD,
            TOOL_ATTACK_SPEED_ADD_RANGES
    );
    private static final ModifierDefinition TOOL_FE_USAGE = percent(ModifierSlot.SUFFIX, MachineStat.FE_USAGE);
    private static final ModifierDefinition TOOL_FE_TRANSFER = percent(ModifierSlot.SUFFIX, MachineStat.FE_TRANSFER);
    private static final ModifierDefinition TOOL_STABILITY = percent(ModifierSlot.SUFFIX, MachineStat.STABILITY);
    private static final ModifierDefinition TOOL_CONTROL = percent(ModifierSlot.PREFIX, MachineStat.CONTROL);
    private static final ModifierDefinition TOOL_ORE_BURST_SPEED =
            percent(ModifierSlot.SUFFIX, MachineStat.ORE_BURST_SPEED);
    private static final ModifierDefinition TOOL_BATTERY_SUPPORT = ModifierDefinition.rollable(
            ModifierSlot.PREFIX,
            MachineStat.BATTERY_SUPPORT,
            ModifierOperation.ADD,
            TOOL_BATTERY_SUPPORT_ADD_RANGES
    );
    private static final ModifierDefinition TOOL_LUCK = ModifierDefinition.rollable(
            ModifierSlot.PREFIX,
            MachineStat.LUCK,
            ModifierOperation.ADD,
            TOOL_LUCK_ADD_RANGES
    );
    private static final ModifierDefinition TOOL_TREE_FELL_LIMIT = ModifierDefinition.rollable(
            ModifierSlot.PREFIX,
            MachineStat.TREE_FELL_LIMIT,
            ModifierOperation.ADD,
            TOOL_TREE_FELL_LIMIT_ADD_RANGES
    );
    private static final ModifierDefinition TOOL_VEIN_MINER = ModifierDefinition.rollable(
            "vein_miner",
            "vein_miner",
            ModifierSlot.PREFIX,
            List.of(
                    ModifierEffectDefinition.of(
                            MachineStat.VEIN_MINE_LIMIT,
                            ModifierOperation.ADD,
                            TOOL_VEIN_MINE_LIMIT_RANGES
                    ),
                    ModifierEffectDefinition.of(
                            MachineStat.VEIN_MINE_FE_USAGE,
                            ModifierOperation.ADD,
                            TOOL_VEIN_MINE_FE_USAGE_RANGES
                    )
            )
    ).withRollWeight(TOOL_VEIN_MINER_WEIGHT);
    private static final ModifierDefinition POTATO_POWER = percent(ModifierSlot.PREFIX, MachineStat.POTATO_POWER);
    private static final ModifierDefinition CARROT_POWER = percent(ModifierSlot.PREFIX, MachineStat.CARROT_POWER);
    private static final ModifierDefinition BREAD_POWER = percent(ModifierSlot.PREFIX, MachineStat.BREAD_POWER);
    private static final ModifierDefinition SAPLING_POWER = percent(ModifierSlot.PREFIX, MachineStat.SAPLING_POWER);
    private static final ModifierDefinition SEED_POWER = percent(ModifierSlot.PREFIX, MachineStat.SEED_POWER);
    private static final ModifierDefinition PLANT_POWER = percent(ModifierSlot.SUFFIX, MachineStat.PLANT_POWER);
    private static final ModifierDefinition ORGANIC_REAGENT_POWER =
            percent(ModifierSlot.SUFFIX, MachineStat.ORGANIC_REAGENT_POWER);
    private static final ModifierDefinition COMPOSTED_BIOMASS_POWER =
            percent(ModifierSlot.SUFFIX, MachineStat.COMPOSTED_BIOMASS_POWER);
    private static final ModifierDefinition ALGAE_POWER = percent(ModifierSlot.SUFFIX, MachineStat.ALGAE_POWER);
    private static final ModifierDefinition RICH_BIOMASS_POWER =
            percent(ModifierSlot.SUFFIX, MachineStat.RICH_BIOMASS_POWER);
    private static final ModifierDefinition FUEL_DURATION = percent(ModifierSlot.SUFFIX, MachineStat.FUEL_DURATION);
    private static final ModifierDefinition SOLAR_PANEL_LIMIT = ModifierDefinition.rollable(
            "array_expansion",
            "solar_array_size",
            ModifierSlot.SUFFIX,
            MachineStat.SOLAR_PANEL_LIMIT,
            ModifierOperation.ADD,
            SOLAR_PANEL_LIMIT_ADD_RANGES
    ).withRollWeight(SOLAR_SPECIAL_WEIGHT);
    private static final ModifierDefinition MOONLIGHT_CONVERSION = ModifierDefinition.rollable(
            "moonlit_conversion",
            "solar_low_light",
            ModifierSlot.SUFFIX,
            MachineStat.MOONLIGHT_CONVERSION,
            ModifierOperation.ADD,
            MOONLIGHT_CONVERSION_RANGES
    ).withRollWeight(SOLAR_SPECIAL_WEIGHT);
    private static final ModifierDefinition CLOUD_PIERCER = ModifierDefinition.rollable(
            "cloud_piercer",
            "solar_weather",
            ModifierSlot.SUFFIX,
            MachineStat.WEATHER_RECOVERY,
            ModifierOperation.ADD,
            WEATHER_RECOVERY_RANGES
    ).withRollWeight(SOLAR_SPECIAL_WEIGHT);
    private static final ModifierDefinition PANEL_SYNCHRONIZER = ModifierDefinition.rollable(
            "panel_synchronizer",
            "solar_panel_sync",
            ModifierSlot.PREFIX,
            MachineStat.SOLAR_PANEL_SYNCHRONIZATION,
            ModifierOperation.ADD,
            SOLAR_UTILITY_PERCENT_RANGES
    ).withRollWeight(SOLAR_SPECIAL_WEIGHT);
    private static final ModifierDefinition PANEL_ARBITRATION = ModifierDefinition.rollable(
            "panel_arbitration",
            "solar_panel_selection",
            ModifierSlot.PREFIX,
            MachineStat.SOLAR_PANEL_ARBITRATION,
            ModifierOperation.ADD,
            SOLAR_BOOLEAN_RANGES
    ).withRollWeight(SOLAR_SPECIAL_WEIGHT);
    private static final ModifierDefinition OVERFLOW_SHUNTING = ModifierDefinition.rollable(
            "overflow_shunting",
            "solar_overflow",
            ModifierSlot.PREFIX,
            MachineStat.OVERFLOW_SHUNTING,
            ModifierOperation.ADD,
            SOLAR_UTILITY_PERCENT_RANGES
    ).withRollWeight(SOLAR_SPECIAL_WEIGHT);
    private static final ModifierDefinition CLEAR_SKY_AMPLIFIER = ModifierDefinition.rollable(
            "clear_sky_amplifier",
            "solar_clear_sky",
            ModifierSlot.SUFFIX,
            MachineStat.CLEAR_SKY_AMPLIFICATION,
            ModifierOperation.ADD,
            SOLAR_UTILITY_PERCENT_RANGES
    ).withRollWeight(SOLAR_SPECIAL_WEIGHT);
    private static final ModifierDefinition LUNAR_INVERTER = ModifierDefinition.rollable(
            "lunar_inverter",
            "solar_low_light",
            ModifierSlot.SUFFIX,
            MachineStat.LUNAR_INVERSION,
            ModifierOperation.ADD,
            List.of(
                    new ModifierValueRange(30, 40, true),
                    new ModifierValueRange(41, 55, true),
                    new ModifierValueRange(56, 70, true),
                    new ModifierValueRange(71, 85, true)
            )
    ).withRollWeight(SOLAR_SPECIAL_WEIGHT);
    private static final ModifierDefinition PEAK_SOLAR = ModifierDefinition.rollable(
            "peak_solar",
            "solar_peak_generation",
            ModifierSlot.SUFFIX,
            MachineStat.PEAK_SOLAR_GENERATION,
            ModifierOperation.INCREASED_PERCENT,
            PEAK_SOLAR_GENERATION_RANGES
    ).withRollWeight(SOLAR_SPECIAL_WEIGHT);
    private static final ModifierDefinition INSTANT_PROCESS = ModifierDefinition.rollable(
            "instant_process",
            "instant_process",
            ModifierSlot.SUFFIX,
            MachineStat.INSTANT_PROCESS_CHANCE,
            ModifierOperation.ADD,
            CHANCE_TIER_RANGES
    ).withRollWeight(HIGH_IMPACT_CHANCE_WEIGHT);
    private static final ModifierDefinition SUPER_OUTPUT = ModifierDefinition.rollable(
            "super_output",
            "super_output",
            ModifierSlot.SUFFIX,
            MachineStat.SUPER_OUTPUT_CHANCE,
            ModifierOperation.ADD,
            CHANCE_TIER_RANGES
    ).withRollWeight(HIGH_IMPACT_CHANCE_WEIGHT);
    private static final ModifierDefinition OVERCLOCKED = ModifierDefinition.rollable(
            "overclocked",
            PROCESSING_SPEED_GROUP,
            ModifierSlot.SUFFIX,
            List.of(
                    ModifierEffectDefinition.of(
                            MachineStat.PROCESSING_SPEED,
                            ModifierOperation.INCREASED_PERCENT,
                            OVERCLOCK_SPEED_RANGES
                    ),
                    ModifierEffectDefinition.of(
                            MachineStat.ENERGY_USAGE,
                            ModifierOperation.INCREASED_PERCENT,
                            OVERCLOCK_ENERGY_USAGE_RANGES
                    )
            )
    ).withRollWeight(OVERCLOCKED_WEIGHT);
    private static final ModifierDefinition CRUSHER_FRAME = ModifierDefinition.rollable(
            CRUSHER_FRAME_AFFIX_ID,
            MachineBehavior.OUTPUT_GUARD.getSerializedName(),
            ModifierSlot.PREFIX,
            MachineStat.OUTPUT_GUARD_GRACE,
            ModifierOperation.ADD,
            CRUSHER_OUTPUT_GUARD_GRACE_RANGES
    ).withRollWeight(CRUSHER_PREFIX_WEIGHT);
    private static final ModifierDefinition CRUSHER_KINETICS = ModifierDefinition.rollable(
            "crusher_kinetics",
            "energy_capacity_flat",
            ModifierSlot.PREFIX,
            MachineStat.ENERGY_CAPACITY_FLAT,
            ModifierOperation.ADD,
            MACHINE_ENERGY_CAPACITY_ADD_RANGES
    ).withRollWeight(CRUSHER_PREFIX_WEIGHT);
    private static final ModifierDefinition CRUSHER_JAWS = ModifierDefinition.rollable(
            "crusher_jaws",
            "increased_percent:output_amount",
            ModifierSlot.PREFIX,
            MachineStat.OUTPUT_AMOUNT,
            ModifierOperation.INCREASED_PERCENT,
            PERCENT_TIER_RANGES
    ).withRollWeight(CRUSHER_PREFIX_WEIGHT);
    private static final ModifierDefinition CRUSHER_ORE_HANDLING = ModifierDefinition.rollable(
            "crusher_ore_handling",
            "super_output",
            ModifierSlot.PREFIX,
            MachineStat.SUPER_OUTPUT_CHANCE,
            ModifierOperation.ADD,
            CHANCE_TIER_RANGES
    ).withRollWeight(CRUSHER_PREFIX_WEIGHT);
    private static final ModifierDefinition CRUSHER_BATTERY_LINK = ModifierDefinition.rollable(
            "crusher_battery_link",
            "crusher_battery_link",
            ModifierSlot.PREFIX,
            MachineStat.NO_BATTERY_OUTPUT_RETENTION,
            ModifierOperation.ADD,
            CRUSHER_NO_BATTERY_RETENTION_RANGES
    ).withRollWeight(CRUSHER_PREFIX_WEIGHT);
    private static final ModifierDefinition CRUSHER_FEED_CONTROL = ModifierDefinition.rollable(
            "crusher_feed_control",
            "crusher_feed_control",
            ModifierSlot.PREFIX,
            MachineStat.CRUSHER_INPUT_FILTER,
            ModifierOperation.ADD,
            CRUSHER_INPUT_FILTER_RANGES
    ).withRollWeight(CRUSHER_PREFIX_WEIGHT);
    private static final ModifierDefinition CRUSHER_COMPRESSION = ModifierDefinition.rollable(
            "crusher_compression",
            "crusher_high_hardness_energy",
            ModifierSlot.PREFIX,
            MachineStat.HIGH_HARDNESS_ENERGY_MITIGATION,
            ModifierOperation.ADD,
            CRUSHER_HIGH_HARDNESS_MITIGATION_RANGES
    ).withRollWeight(CRUSHER_PREFIX_WEIGHT);
    private static final ModifierDefinition CRUSHER_VIBRATION = ModifierDefinition.rollable(
            "crusher_vibration",
            "decreased_percent:energy_usage",
            ModifierSlot.PREFIX,
            MachineStat.ENERGY_USAGE,
            ModifierOperation.DECREASED_PERCENT,
            PERCENT_TIER_RANGES
    ).withRollWeight(CRUSHER_PREFIX_WEIGHT);
    private static final ModifierDefinition CRUSHER_THROUGHPUT = ModifierDefinition.rollable(
            CRUSHER_THROUGHPUT_AFFIX_ID,
            "dense_parallel",
            ModifierSlot.PREFIX,
            MachineStat.BATCH_SIZE,
            ModifierOperation.ADD,
            CRUSHER_PARALLEL_JOB_RANGES
    ).withRollWeight(CRUSHER_PREFIX_WEIGHT);
    private static final ModifierDefinition CRUSHER_SALVAGE = ModifierDefinition.rollable(
            "crusher_salvage",
            "crusher_salvage",
            ModifierSlot.PREFIX,
            MachineStat.CRUSHER_SALVAGE_CHANCE,
            ModifierOperation.ADD,
            CHANCE_TIER_RANGES
    ).withRollWeight(CRUSHER_PREFIX_WEIGHT);
    private static final ModifierDefinition CRUSH_HEAD_PULVERIZING = ModifierDefinition.rollable(
            "crush_head_pulverizing",
            "increased_percent:output_amount",
            ModifierSlot.PREFIX,
            MachineStat.OUTPUT_AMOUNT,
            ModifierOperation.INCREASED_PERCENT,
            PERCENT_TIER_RANGES
    ).withRollWeight(CRUSH_HEAD_PREFIX_WEIGHT);
    private static final ModifierDefinition CRUSH_HEAD_JAGGED = ModifierDefinition.rollable(
            "crush_head_jagged",
            "crusher_salvage",
            ModifierSlot.PREFIX,
            MachineStat.CRUSHER_SALVAGE_CHANCE,
            ModifierOperation.ADD,
            CHANCE_TIER_RANGES
    ).withRollWeight(CRUSH_HEAD_PREFIX_WEIGHT);
    private static final ModifierDefinition CRUSH_HEAD_KINETIC = ModifierDefinition.rollable(
            "crush_head_kinetic",
            PROCESSING_SPEED_GROUP,
            ModifierSlot.PREFIX,
            MachineStat.PROCESSING_SPEED,
            ModifierOperation.INCREASED_PERCENT,
            PERCENT_TIER_RANGES
    ).withRollWeight(CRUSH_HEAD_PREFIX_WEIGHT);
    private static final ModifierDefinition CRUSH_HEAD_SCUFFED = ModifierDefinition.rollableUntiered(
            "crush_head_scuffed",
            "no_battery_output_retention",
            ModifierSlot.PREFIX,
            MachineStat.NO_BATTERY_OUTPUT_RETENTION,
            ModifierOperation.ADD,
            ModifierValueRange.fixed(6)
    ).withRollWeight(CRUSH_HEAD_PREFIX_WEIGHT);
    private static final ModifierDefinition CRUSH_HEAD_DUST_GROOVE = ModifierDefinition.rollableUntiered(
            "crush_head_dust_groove",
            "crusher_input_filter",
            ModifierSlot.PREFIX,
            MachineStat.CRUSHER_INPUT_FILTER,
            ModifierOperation.ADD,
            ModifierValueRange.fixed(1)
    ).withRollWeight(CRUSH_HEAD_PREFIX_WEIGHT);
    private static final ModifierDefinition BATTERY_CHARGE_BACKPLANE = runtime(
            "charge_backplane",
            "charge_acceptance",
            ModifierSlot.PREFIX,
            MachineStat.ENERGY_TRANSFER,
            ModifierOperation.INCREASED_PERCENT,
            PERCENT_TIER_RANGES,
            80
    );
    private static final ModifierDefinition BATTERY_LOW_CHARGE_RETENTION = runtime(
            "low_charge_retention",
            "low_charge_retention",
            ModifierSlot.SUFFIX,
            MachineStat.IDLE_LOSS,
            ModifierOperation.DECREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            55
    );
    private static final ModifierDefinition BATTERY_BROWNOUT_CUSHION = runtimeUntiered(
            "brownout_cushion",
            "power_grace",
            ModifierSlot.SUFFIX,
            MachineStat.ENERGY_TRANSFER,
            ModifierOperation.INCREASED_PERCENT,
            10,
            55
    );
    private static final ModifierDefinition BATTERY_LEAK_QUENCHER = runtime(
            "leak_quencher",
            "leak_quencher",
            ModifierSlot.SUFFIX,
            MachineStat.IDLE_LOSS,
            ModifierOperation.DECREASED_PERCENT,
            PERCENT_TIER_RANGES,
            80
    );
    private static final ModifierDefinition BIO_DIGESTATE_PRIME = runtime(
            "digestate_prime",
            "fuel_ramp_time",
            ModifierSlot.SUFFIX,
            MachineStat.FUEL_EFFICIENCY,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            65
    );
    private static final ModifierDefinition BIO_COMPOST_MEMORY = runtime(
            "compost_memory",
            "biomass_streak_output",
            ModifierSlot.SUFFIX,
            MachineStat.ENERGY_GENERATION,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            55
    );
    private static final ModifierDefinition RECYCLER_PATTERN_MEMORY = runtime(
            "pattern_memory",
            "recycling_repeat_speed",
            ModifierSlot.PREFIX,
            MachineStat.PROCESSING_SPEED,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            65
    );
    private static final ModifierDefinition RECYCLER_CLEAN_BREAK = runtime(
            "clean_break",
            "recycling_remainder_chance",
            ModifierSlot.PREFIX,
            MachineStat.OUTPUT_AMOUNT,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            60
    );
    private static final ModifierDefinition TANK_RECAPTURE_SLEEVE = runtime(
            "recapture_sleeve",
            "compression_fe_recovery",
            ModifierSlot.PREFIX,
            MachineStat.ENERGY_USAGE,
            ModifierOperation.DECREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            55
    );
    private static final ModifierDefinition TANK_PRESSURE_SPLITTER = runtimeUntiered(
            "pressure_splitter",
            "dual_reservoir_throughput",
            ModifierSlot.PREFIX,
            MachineStat.FLUID_TRANSFER,
            ModifierOperation.INCREASED_PERCENT,
            15,
            60
    );
    private static final ModifierDefinition LINING_EMERGENCY_SCRAM = runtime(
            "emergency_scram",
            "reactor_scram_threshold",
            ModifierSlot.PREFIX,
            MachineStat.STABILITY,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            45
    );
    private static final ModifierDefinition LINING_NEUTRON_BAFFLE = runtime(
            "neutron_baffle",
            "stage_gap_stability_loss",
            ModifierSlot.PREFIX,
            MachineStat.STABILITY,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            65
    );
    private static final ModifierDefinition LINING_RESIDUE_QUARANTINE = runtime(
            "residue_quarantine",
            "residue_block_grace",
            ModifierSlot.PREFIX,
            MachineStat.STABILITY,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            55
    );
    private static final ModifierDefinition LINING_PRESSURE_CURTAIN = runtime(
            "pressure_curtain",
            "redstone_pause_stability",
            ModifierSlot.PREFIX,
            MachineStat.STABILITY,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            60
    );
    private static final ModifierDefinition LINING_QUIET_CORE = runtime(
            "quiet_core",
            "generation_variance",
            ModifierSlot.SUFFIX,
            MachineStat.STABILITY,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            70
    );
    private static final ModifierDefinition LINING_SEALED_DUMP = runtime(
            "sealed_dump",
            "residue_transfer_window",
            ModifierSlot.SUFFIX,
            MachineStat.STABILITY,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            55
    );
    private static final ModifierDefinition LINING_CONTAINMENT_WINDOW = runtime(
            "containment_window",
            "safe_progress_window",
            ModifierSlot.SUFFIX,
            MachineStat.STABILITY,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            60
    );
    private static final ModifierDefinition BOARD_BRANCH_PREDICTOR = runtime(
            "branch_predictor",
            "calibration_low_roll_floor",
            ModifierSlot.PREFIX,
            MachineStat.CALIBRATION_PRECISION,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            65
    );
    private static final ModifierDefinition BOARD_LENS_ROUTER = runtime(
            "lens_router",
            "targeted_lens_discount",
            ModifierSlot.PREFIX,
            MachineStat.REFINEMENT_POTENTIAL_BONUS,
            ModifierOperation.ADD,
            REFINEMENT_POTENTIAL_BONUS_ADD_RANGES,
            55
    );
    private static final ModifierDefinition BOARD_ERROR_MAP = runtime(
            "error_map",
            "low_rp_output_bonus",
            ModifierSlot.PREFIX,
            MachineStat.REFINEMENT_POTENTIAL_BONUS,
            ModifierOperation.ADD,
            REFINEMENT_POTENTIAL_BONUS_ADD_RANGES,
            60
    );
    private static final ModifierDefinition BOARD_INTERRUPT_MASK = runtime(
            "interrupt_mask",
            "catalyst_preserve_chance",
            ModifierSlot.SUFFIX,
            MachineStat.STABILITY,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            45
    );
    private static final ModifierDefinition BOARD_CLOCK_DIVIDER = runtimeUntiered(
            "clock_divider",
            "precision_over_speed",
            ModifierSlot.SUFFIX,
            MachineStat.CALIBRATION_PRECISION,
            ModifierOperation.INCREASED_PERCENT,
            12,
            35
    );
    private static final ModifierDefinition CRUSH_HEAD_SHOCK_TOOTH = runtime(
            "shock_tooth",
            "high_hardness_energy_mitigation",
            ModifierSlot.PREFIX,
            MachineStat.HIGH_HARDNESS_ENERGY_MITIGATION,
            ModifierOperation.ADD,
            CRUSHER_HIGH_HARDNESS_MITIGATION_RANGES,
            65
    );
    private static final ModifierDefinition DISASSEMBLY_FASTENER_FINDER = runtime(
            "fastener_finder",
            "recycling_secondary_chance",
            ModifierSlot.PREFIX,
            MachineStat.OUTPUT_AMOUNT,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            70
    );
    private static final ModifierDefinition DISASSEMBLY_GENTLE_PRY = runtime(
            "gentle_pry",
            "recycling_damage_tolerance",
            ModifierSlot.PREFIX,
            MachineStat.STABILITY,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            65
    );
    private static final ModifierDefinition DISASSEMBLY_TRACE_CUTTER = runtime(
            "trace_cutter",
            "recycling_data_strip_speed",
            ModifierSlot.PREFIX,
            MachineStat.PROCESSING_SPEED,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            60
    );
    private static final ModifierDefinition PUMP_SELF_PRIMING = runtime(
            "self_priming",
            "pump_start_volume",
            ModifierSlot.PREFIX,
            MachineStat.FLUID_TRANSFER,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            70
    );
    private static final ModifierDefinition PUMP_CHECK_VALVE = runtimeUntiered(
            "check_valve",
            "wrong_fluid_backflow_guard",
            ModifierSlot.PREFIX,
            MachineStat.FLUID_TRANSFER,
            ModifierOperation.INCREASED_PERCENT,
            8,
            40
    );
    private static final ModifierDefinition PUMP_PULSE = runtime(
            "pulse_pump",
            "pump_burst_transfer",
            ModifierSlot.PREFIX,
            MachineStat.FLUID_TRANSFER,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            65
    );
    private static final ModifierDefinition PUMP_SEAL_FLUSH = runtime(
            "seal_flush",
            "pump_purge_efficiency",
            ModifierSlot.PREFIX,
            MachineStat.FLUID_TRANSFER,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            50
    );
    private static final ModifierDefinition PUMP_HIGH_HEAD = runtime(
            "high_head",
            "high_head",
            ModifierSlot.SUFFIX,
            MachineStat.FLUID_TRANSFER,
            ModifierOperation.INCREASED_PERCENT,
            PERCENT_TIER_RANGES,
            100
    );
    private static final ModifierDefinition PUMP_VAPOR_LOCK_BREAKER = runtime(
            "vapor_lock_breaker",
            "pump_blocked_recovery",
            ModifierSlot.SUFFIX,
            MachineStat.FLUID_TRANSFER,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            60
    );
    private static final ModifierDefinition PUMP_METERED_DRIP = runtime(
            "metered_drip",
            "container_fill_rounding",
            ModifierSlot.SUFFIX,
            MachineStat.FLUID_TRANSFER,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            55
    );
    private static final ModifierDefinition FUEL_BOX_COAL_SILO = runtime(
            "coal_silo",
            "compact_fuel_lanes",
            ModifierSlot.PREFIX,
            MachineStat.INPUT_SLOTS,
            ModifierOperation.ADD,
            RUNTIME_INPUT_SLOT_RANGES,
            70
    );
    private static final ModifierDefinition FUEL_BOX_DRAFT_COLLAR = runtime(
            "draft_collar",
            "ignition_ramp_speed",
            ModifierSlot.PREFIX,
            MachineStat.FUEL_EFFICIENCY,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            65
    );
    private static final ModifierDefinition FUEL_BOX_ASH_TRAP = runtime(
            "ash_trap",
            "dirty_fuel_stability",
            ModifierSlot.PREFIX,
            MachineStat.STABILITY,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            60
    );
    private static final ModifierDefinition FUEL_BOX_EMBER_METER = runtime(
            "ember_meter",
            "fuel_slice_size",
            ModifierSlot.SUFFIX,
            MachineStat.FUEL_EFFICIENCY,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            55
    );
    private static final ModifierDefinition FUEL_BOX_CINDER_RETURN = runtime(
            "cinder_return",
            "fuel_remainder_chance",
            ModifierSlot.SUFFIX,
            MachineStat.STABILITY,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            45
    );
    private static final ModifierDefinition FUEL_BOX_QUEUE_SEQUENCER = runtime(
            "queue_sequencer",
            "fuel_queue_swap_speed",
            ModifierSlot.SUFFIX,
            MachineStat.INPUT_SLOTS,
            ModifierOperation.ADD,
            RUNTIME_INPUT_SLOT_RANGES,
            60
    );
    private static final ModifierDefinition HEAT_CORE_HEAT_SOAK = runtime(
            "heat_soak",
            "warmth_retention_time",
            ModifierSlot.SUFFIX,
            MachineStat.COOLING_RATE,
            ModifierOperation.DECREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            70
    );
    private static final ModifierDefinition REACTOR_SAMPLE_LOOP = runtime(
            "sample_loop",
            "reactor_recipe_lock_speed",
            ModifierSlot.PREFIX,
            MachineStat.PROCESSING_SPEED,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            65
    );
    private static final ModifierDefinition REACTOR_MODERATOR_TEETH = runtime(
            "moderator_teeth",
            "reactor_stability_floor",
            ModifierSlot.PREFIX,
            MachineStat.STABILITY,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            70
    );
    private static final ModifierDefinition REACTOR_FLUX_LENS = runtime(
            "flux_lens",
            "high_value_salvage_gain",
            ModifierSlot.PREFIX,
            MachineStat.ENERGY_GENERATION,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            55
    );
    private static final ModifierDefinition REACTOR_EXHAUST_PORT = runtime(
            "exhaust_port",
            "reactor_output_drain_priority",
            ModifierSlot.SUFFIX,
            MachineStat.PROCESSING_SPEED,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            60
    );
    private static final ModifierDefinition FILTER_FINE_MESH = runtime(
            "fine_mesh",
            "residue_fine_output_chance",
            ModifierSlot.PREFIX,
            MachineStat.OUTPUT_AMOUNT,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            70
    );
    private static final ModifierDefinition FILTER_MAGNETIC_CATCH = runtime(
            "magnetic_catch",
            "metal_residue_bonus",
            ModifierSlot.PREFIX,
            MachineStat.OUTPUT_AMOUNT,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            65
    );
    private static final ModifierDefinition FILTER_SIEVE_MEMORY = runtime(
            "sieve_memory",
            "filter_repeat_bonus",
            ModifierSlot.PREFIX,
            MachineStat.PROCESSING_SPEED,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            55
    );
    private static final ModifierDefinition FILTER_FAST_BACKFLUSH = runtime(
            "fast_backflush",
            "filter_clear_time",
            ModifierSlot.SUFFIX,
            MachineStat.PROCESSING_SPEED,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            70
    );
    private static final ModifierDefinition FILTER_CATALYST_SWEEP = runtime(
            "catalyst_sweep",
            "catalyst_trace_recovery",
            ModifierSlot.SUFFIX,
            MachineStat.OUTPUT_AMOUNT,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            40
    );
    private static final ModifierDefinition FILTER_LOW_DRAG = runtime(
            "low_drag",
            "low_drag",
            ModifierSlot.SUFFIX,
            MachineStat.PROCESSING_SPEED,
            ModifierOperation.INCREASED_PERCENT,
            PERCENT_TIER_RANGES,
            80
    );
    private static final ModifierDefinition COIL_HARMONIC_LOCK = runtime(
            "harmonic_lock",
            "calibration_roll_spread",
            ModifierSlot.PREFIX,
            MachineStat.CALIBRATION_QUALITY,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            70
    );
    private static final ModifierDefinition COIL_PHASE_TAP = runtime(
            "phase_tap",
            "pattern_reuse_discount",
            ModifierSlot.PREFIX,
            MachineStat.ENERGY_USAGE,
            ModifierOperation.DECREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            55
    );
    private static final ModifierDefinition COIL_RETURN_WINDING = runtime(
            "return_winding",
            "failed_calibration_fe_return",
            ModifierSlot.PREFIX,
            MachineStat.ENERGY_TRANSFER,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            45
    );
    private static final ModifierDefinition SOLAR_HORIZON_CATCHER = runtime(
            "horizon_catcher",
            "low_sun_angle_generation",
            ModifierSlot.SUFFIX,
            MachineStat.PEAK_SOLAR_GENERATION,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            60
    );
    private static final ModifierDefinition SOLID_FUEL_EMBER_BANK = runtime(
            "ember_bank",
            "burn_tail_generation",
            ModifierSlot.SUFFIX,
            MachineStat.FUEL_EFFICIENCY,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            65
    );
    private static final ModifierDefinition MATRIX_QUENCH_GATE = runtime(
            "quench_gate",
            "calibration_failure_damping",
            ModifierSlot.PREFIX,
            MachineStat.STABILITY,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            55
    );
    private static final ModifierDefinition MATRIX_CATALYST_CRADLE = runtime(
            "catalyst_cradle",
            "catalyst_preserve_chance",
            ModifierSlot.PREFIX,
            MachineStat.CATALYST_EFFICIENCY,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            45
    );
    private static final ModifierDefinition MATRIX_DEADBAND_TUNER = runtime(
            "deadband_tuner",
            "stability_floor",
            ModifierSlot.PREFIX,
            MachineStat.CALIBRATION_PRECISION,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            65
    );
    private static final ModifierDefinition MATRIX_POTENTIAL_CLAMP = runtime(
            "potential_clamp",
            "low_rp_output_bonus",
            ModifierSlot.SUFFIX,
            MachineStat.REFINEMENT_POTENTIAL_BONUS,
            ModifierOperation.ADD,
            REFINEMENT_POTENTIAL_BONUS_ADD_RANGES,
            60
    );
    private static final ModifierDefinition TOOL_HEAD_MOMENTUM_EDGE = runtime(
            "momentum_edge",
            "repeat_block_speed",
            ModifierSlot.SUFFIX,
            MachineStat.MINING_SPEED,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            65
    );
    private static final ModifierDefinition TOOL_ROD_SHOCK_GRIP = runtime(
            "shock_grip",
            "burst_strain_reduction",
            ModifierSlot.SUFFIX,
            MachineStat.CONTROL,
            ModifierOperation.INCREASED_PERCENT,
            RUNTIME_PERCENT_RANGES,
            65
    );

    private static final Set<String> BEHAVIOR_ONLY_AFFIX_IDS = Set.of();
    private static final Set<String> DESCRIBED_AFFIX_IDS = Set.of(
            CRUSHER_SALVAGE.id(),
            CRUSH_HEAD_JAGGED.id(),
            CRUSH_HEAD_SCUFFED.id(),
            CRUSH_HEAD_DUST_GROOVE.id(),
            BATTERY_CHARGE_BACKPLANE.id(),
            BATTERY_LOW_CHARGE_RETENTION.id(),
            BATTERY_BROWNOUT_CUSHION.id(),
            BATTERY_LEAK_QUENCHER.id(),
            BIO_DIGESTATE_PRIME.id(),
            BIO_COMPOST_MEMORY.id(),
            RECYCLER_PATTERN_MEMORY.id(),
            RECYCLER_CLEAN_BREAK.id(),
            TANK_RECAPTURE_SLEEVE.id(),
            TANK_PRESSURE_SPLITTER.id(),
            LINING_EMERGENCY_SCRAM.id(),
            LINING_NEUTRON_BAFFLE.id(),
            LINING_RESIDUE_QUARANTINE.id(),
            LINING_PRESSURE_CURTAIN.id(),
            LINING_QUIET_CORE.id(),
            LINING_SEALED_DUMP.id(),
            LINING_CONTAINMENT_WINDOW.id(),
            BOARD_BRANCH_PREDICTOR.id(),
            BOARD_LENS_ROUTER.id(),
            BOARD_ERROR_MAP.id(),
            BOARD_INTERRUPT_MASK.id(),
            BOARD_CLOCK_DIVIDER.id(),
            CRUSH_HEAD_SHOCK_TOOTH.id(),
            DISASSEMBLY_FASTENER_FINDER.id(),
            DISASSEMBLY_GENTLE_PRY.id(),
            DISASSEMBLY_TRACE_CUTTER.id(),
            PUMP_SELF_PRIMING.id(),
            PUMP_CHECK_VALVE.id(),
            PUMP_PULSE.id(),
            PUMP_SEAL_FLUSH.id(),
            PUMP_HIGH_HEAD.id(),
            PUMP_VAPOR_LOCK_BREAKER.id(),
            PUMP_METERED_DRIP.id(),
            FUEL_BOX_COAL_SILO.id(),
            FUEL_BOX_DRAFT_COLLAR.id(),
            FUEL_BOX_ASH_TRAP.id(),
            FUEL_BOX_EMBER_METER.id(),
            FUEL_BOX_CINDER_RETURN.id(),
            FUEL_BOX_QUEUE_SEQUENCER.id(),
            HEAT_CORE_HEAT_SOAK.id(),
            REACTOR_SAMPLE_LOOP.id(),
            REACTOR_MODERATOR_TEETH.id(),
            REACTOR_FLUX_LENS.id(),
            REACTOR_EXHAUST_PORT.id(),
            FILTER_FINE_MESH.id(),
            FILTER_MAGNETIC_CATCH.id(),
            FILTER_SIEVE_MEMORY.id(),
            FILTER_FAST_BACKFLUSH.id(),
            FILTER_CATALYST_SWEEP.id(),
            FILTER_LOW_DRAG.id(),
            COIL_HARMONIC_LOCK.id(),
            COIL_PHASE_TAP.id(),
            COIL_RETURN_WINDING.id(),
            SOLAR_HORIZON_CATCHER.id(),
            SOLID_FUEL_EMBER_BANK.id(),
            MATRIX_QUENCH_GATE.id(),
            MATRIX_CATALYST_CRADLE.id(),
            MATRIX_DEADBAND_TUNER.id(),
            MATRIX_POTENTIAL_CLAMP.id(),
            TOOL_HEAD_MOMENTUM_EDGE.id(),
            TOOL_VEIN_MINER.id(),
            TOOL_ROD_SHOCK_GRIP.id()
    );

    private static final Map<ModifierCapability, List<ModifierDefinition>> MODIFIERS_BY_CAPABILITY =
            createModifiersByCapability();

    public static final ModifierEligibilityProfile CRUSHER = profile(
            "crusher",
            Set.of(
                    ModifierCapability.HAS_FE_STORAGE,
                    ModifierCapability.HAS_FE_INPUT,
                    ModifierCapability.HAS_INPUT_SLOT,
                    ModifierCapability.HAS_OUTPUT_SLOT,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_CRUSHER_SPECIALS
            ),
            crusherAffixes(),
            PROCESSING_MACHINE_BEHAVIORS
    );
    public static final ModifierEligibilityProfile MINERS_COMPANION = profile(
            "miners_companion",
            Set.of(ModifierCapability.HAS_MINERS_COMPANION),
            affixes(
                    List.of(MINERS_COMPANION_FILTER_SLOTS),
                    MachineStat.ENERGY_USAGE
            )
    );
    public static final ModifierEligibilityProfile FORESTRY_COMPANION = profile(
            "forestry_companion",
            Set.of(
                    ModifierCapability.HAS_FE_INPUT,
                    ModifierCapability.HAS_PROCESSING
            ),
            affixes(
                    List.of(),
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.ENERGY_USAGE
            )
    );
    public static final ModifierEligibilityProfile FURNACE = profile(
            "furnace",
            Set.of(
                    ModifierCapability.HAS_INPUT_SLOT,
                    ModifierCapability.HAS_OUTPUT_SLOT,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_FUEL_SLOT,
                    ModifierCapability.HAS_HEAT
            ),
            processingAffixes(
                    "smelting",
                    true,
                    MachineStat.HEAT_TRANSFER,
                    MachineStat.MAX_TEMPERATURE,
                    MachineStat.HEAT_ISOLATION,
                    MachineStat.WARMUP_TIME,
                    MachineStat.COOLING_RATE,
                    MachineStat.EFFICIENCY,
                    MachineStat.FUEL_EFFICIENCY,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.TEMPERATURE_STABILITY,
                    MachineStat.OVERHEAT_TOLERANCE,
                    MachineStat.INPUT_SLOTS
            )
    );
    public static final ModifierEligibilityProfile ELECTRIC_FURNACE = profile(
            "electric_furnace",
            Set.of(
                    ModifierCapability.HAS_FE_STORAGE,
                    ModifierCapability.HAS_FE_INPUT,
                    ModifierCapability.HAS_INPUT_SLOT,
                    ModifierCapability.HAS_OUTPUT_SLOT,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_HEAT
            ),
            poweredProcessingAffixes(
                    "smelting",
                    true,
                    MACHINE_ENERGY_CAPACITY_ADD,
                    MachineStat.ENERGY_CAPACITY,
                    MachineStat.ENERGY_USAGE,
                    MachineStat.HEAT_TRANSFER,
                    MachineStat.MAX_TEMPERATURE,
                    MachineStat.HEAT_ISOLATION,
                    MachineStat.WARMUP_TIME,
                    MachineStat.COOLING_RATE,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.TEMPERATURE_STABILITY,
                    MachineStat.OVERHEAT_TOLERANCE,
                    MachineStat.INPUT_SLOTS
            ),
            POWER_HEAT_CONTROL_BEHAVIORS
    );
    public static final ModifierEligibilityProfile SOLID_FUEL_BURNER = profile(
            "solid_fuel_burner",
            Set.of(
                    ModifierCapability.HAS_FE_OUTPUT,
                    ModifierCapability.HAS_FE_GENERATION,
                    ModifierCapability.HAS_FUEL_SLOT,
                    ModifierCapability.HAS_HEAT,
                    ModifierCapability.HAS_STABILITY
            ),
            affixes(
                    List.of(SOLID_FUEL_EMBER_BANK),
                    MachineStat.EFFICIENCY,
                    MachineStat.FUEL_EFFICIENCY,
                    MachineStat.INPUT_SLOTS,
                    MachineStat.ENERGY_GENERATION,
                    MachineStat.FUEL_DURATION,
                    MachineStat.HEAT_ISOLATION,
                    MachineStat.STABILITY
            )
    );
    public static final ModifierEligibilityProfile BIO_GENERATOR = profile(
            "bio_generator",
            Set.of(
                    ModifierCapability.HAS_FE_STORAGE,
                    ModifierCapability.HAS_FE_OUTPUT,
                    ModifierCapability.HAS_FE_GENERATION,
                    ModifierCapability.HAS_FUEL_SLOT,
                    ModifierCapability.HAS_INPUT_SLOT
            ),
            affixes(
                    List.of(MACHINE_ENERGY_CAPACITY_ADD, BIO_DIGESTATE_PRIME, BIO_COMPOST_MEMORY),
                    MachineStat.ENERGY_CAPACITY,
                    MachineStat.EFFICIENCY,
                    MachineStat.FUEL_EFFICIENCY,
                    MachineStat.ENERGY_GENERATION,
                    MachineStat.ENERGY_TRANSFER
            )
    );
    public static final ModifierEligibilityProfile SOLAR_PANEL = profile(
            "solar_panel",
            Set.of(
                    ModifierCapability.HAS_FE_STORAGE,
                    ModifierCapability.HAS_FE_OUTPUT,
                    ModifierCapability.HAS_FE_GENERATION,
                    ModifierCapability.HAS_SOLAR_PANEL
            ),
            solarPanelAffixes()
    );
    public static final ModifierEligibilityProfile SOLAR_ARRAY_CONTROLLER = profile(
            "solar_array_controller",
            Set.of(
                    ModifierCapability.HAS_FE_STORAGE,
                    ModifierCapability.HAS_FE_OUTPUT,
                    ModifierCapability.HAS_FE_GENERATION,
                    ModifierCapability.HAS_BATTERY_CELLS,
                    ModifierCapability.HAS_STABILITY,
                    ModifierCapability.HAS_SOLAR_ARRAY
            ),
            solarArrayControllerAffixes()
    );
    public static final ModifierEligibilityProfile POTENTIAL_REACTOR = profile(
            "potential_reactor",
            Set.of(
                    ModifierCapability.HAS_FE_STORAGE,
                    ModifierCapability.HAS_FE_OUTPUT,
                    ModifierCapability.HAS_FE_GENERATION,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_STABILITY
            ),
            processingStatAffixes(
                    "reacting",
                    MACHINE_ENERGY_CAPACITY_ADD,
                    MachineStat.ENERGY_CAPACITY,
                    MachineStat.ENERGY_GENERATION,
                    MachineStat.ENERGY_TRANSFER,
                    MachineStat.EFFICIENCY,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.STABILITY
            ),
            PROCESSING_MACHINE_BEHAVIORS
    );
    public static final ModifierEligibilityProfile CORROSION_CELL = profile(
            "corrosion_cell",
            Set.of(
                    ModifierCapability.HAS_FE_STORAGE,
                    ModifierCapability.HAS_FE_OUTPUT,
                    ModifierCapability.HAS_FE_GENERATION,
                    ModifierCapability.HAS_INPUT_SLOT,
                    ModifierCapability.HAS_OUTPUT_SLOT,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_FLUID_STORAGE,
                    ModifierCapability.HAS_FLUID_INPUT,
                    ModifierCapability.HAS_STABILITY
            ),
            processingStatAffixes(
                    "corroding",
                    MACHINE_ENERGY_CAPACITY_ADD,
                    MachineStat.ENERGY_CAPACITY,
                    MachineStat.ENERGY_GENERATION,
                    MachineStat.ENERGY_TRANSFER,
                    MachineStat.FLUID_TRANSFER,
                    MachineStat.EFFICIENCY,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.STABILITY
            ),
            PROCESSING_MACHINE_BEHAVIORS
    );
    public static final ModifierEligibilityProfile VACUUM_COLLAPSE_GENERATOR = profile(
            "vacuum_collapse_generator",
            Set.of(
                    ModifierCapability.HAS_FE_STORAGE,
                    ModifierCapability.HAS_FE_OUTPUT,
                    ModifierCapability.HAS_FE_GENERATION,
                    ModifierCapability.HAS_INPUT_SLOT,
                    ModifierCapability.HAS_OUTPUT_SLOT,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_STABILITY
            ),
            processingStatAffixes(
                    "collapsing",
                    MACHINE_ENERGY_CAPACITY_ADD,
                    MachineStat.ENERGY_CAPACITY,
                    MachineStat.ENERGY_GENERATION,
                    MachineStat.ENERGY_TRANSFER,
                    MachineStat.EFFICIENCY,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.STABILITY
            ),
            PROCESSING_MACHINE_BEHAVIORS
    );
    public static final ModifierEligibilityProfile COMPONENT_RECYCLER = profile(
            "component_recycler",
            Set.of(
                    ModifierCapability.HAS_FE_STORAGE,
                    ModifierCapability.HAS_FE_INPUT,
                    ModifierCapability.HAS_INPUT_SLOT,
                    ModifierCapability.HAS_OUTPUT_SLOT,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_STABILITY
            ),
            poweredProcessingAffixes(
                    "recycling",
                    false,
                    List.of(MACHINE_ENERGY_CAPACITY_ADD, RECYCLER_PATTERN_MEMORY, RECYCLER_CLEAN_BREAK),
                    MachineStat.ENERGY_CAPACITY,
                    MachineStat.ENERGY_TRANSFER,
                    MachineStat.ENERGY_USAGE,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.STABILITY
            ),
            PROCESSING_MACHINE_BEHAVIORS
    );
    public static final ModifierEligibilityProfile METAL_PRESS = profile(
            "metal_press",
            Set.of(
                    ModifierCapability.HAS_FE_STORAGE,
                    ModifierCapability.HAS_FE_INPUT,
                    ModifierCapability.HAS_INPUT_SLOT,
                    ModifierCapability.HAS_OUTPUT_SLOT,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_HEAT,
                    ModifierCapability.HAS_STABILITY
            ),
            poweredProcessingAffixes(
                    "pressing",
                    true,
                    MACHINE_ENERGY_CAPACITY_ADD,
                    MachineStat.ENERGY_CAPACITY,
                    MachineStat.ENERGY_TRANSFER,
                    MachineStat.ENERGY_USAGE,
                    MachineStat.HEAT_TRANSFER,
                    MachineStat.MAX_TEMPERATURE,
                    MachineStat.WARMUP_TIME,
                    MachineStat.COOLING_RATE,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.STABILITY,
                    MachineStat.TEMPERATURE_STABILITY,
                    MachineStat.OVERHEAT_TOLERANCE
            ),
            POWER_HEAT_CONTROL_BEHAVIORS
    );
    public static final ModifierEligibilityProfile MELTER = profile(
            "melter",
            Set.of(
                    ModifierCapability.HAS_FE_STORAGE,
                    ModifierCapability.HAS_FE_INPUT,
                    ModifierCapability.HAS_INPUT_SLOT,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_HEAT,
                    ModifierCapability.HAS_FLUID_STORAGE,
                    ModifierCapability.HAS_FLUID_INPUT,
                    ModifierCapability.HAS_FLUID_OUTPUT
            ),
            poweredProcessingAffixes(
                    "melting",
                    false,
                    MACHINE_ENERGY_CAPACITY_ADD,
                    MachineStat.ENERGY_CAPACITY,
                    MachineStat.ENERGY_TRANSFER,
                    MachineStat.ENERGY_USAGE,
                    MachineStat.HEAT_TRANSFER,
                    MachineStat.MAX_TEMPERATURE,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.FLUID_TRANSFER
            ),
            PROCESSING_MACHINE_BEHAVIORS
    );
    public static final ModifierEligibilityProfile BATTERY_ASSEMBLER = profile(
            "battery_assembler",
            Set.of(
                    ModifierCapability.HAS_FE_STORAGE,
                    ModifierCapability.HAS_FE_INPUT,
                    ModifierCapability.HAS_INPUT_SLOT,
                    ModifierCapability.HAS_OUTPUT_SLOT,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_FLUID_STORAGE,
                    ModifierCapability.HAS_FLUID_INPUT,
                    ModifierCapability.HAS_STABILITY
            ),
            poweredProcessingAffixes(
                    "assembling",
                    true,
                    MACHINE_ENERGY_CAPACITY_ADD,
                    MachineStat.ENERGY_CAPACITY,
                    MachineStat.ENERGY_TRANSFER,
                    MachineStat.ENERGY_USAGE,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.FLUID_TRANSFER,
                    MachineStat.STABILITY
            ),
            PROCESSING_MACHINE_BEHAVIORS
    );
    public static final ModifierEligibilityProfile COAL_GASIFIER = profile(
            "coal_gasifier",
            Set.of(
                    ModifierCapability.HAS_FE_STORAGE,
                    ModifierCapability.HAS_FE_INPUT,
                    ModifierCapability.HAS_INPUT_SLOT,
                    ModifierCapability.HAS_OUTPUT_SLOT,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_HEAT,
                    ModifierCapability.HAS_STABILITY,
                    ModifierCapability.HAS_FLUID_STORAGE,
                    ModifierCapability.HAS_FLUID_INPUT,
                    ModifierCapability.HAS_FLUID_OUTPUT
            ),
            poweredProcessingAffixes(
                    "gasifying",
                    true,
                    MACHINE_ENERGY_CAPACITY_ADD,
                    MachineStat.ENERGY_CAPACITY,
                    MachineStat.ENERGY_TRANSFER,
                    MachineStat.ENERGY_USAGE,
                    MachineStat.HEAT_TRANSFER,
                    MachineStat.MAX_TEMPERATURE,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.STABILITY,
                    MachineStat.FLUID_TRANSFER
            ),
            PROCESSING_MACHINE_BEHAVIORS
    );
    public static final ModifierEligibilityProfile SYNGAS_COMBUSTOR = profile(
            "syngas_combustor",
            Set.of(
                    ModifierCapability.HAS_FE_STORAGE,
                    ModifierCapability.HAS_FE_OUTPUT,
                    ModifierCapability.HAS_FE_GENERATION,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_STABILITY,
                    ModifierCapability.HAS_FLUID_STORAGE,
                    ModifierCapability.HAS_FLUID_INPUT,
                    ModifierCapability.HAS_FLUID_OUTPUT
            ),
            processingStatAffixes(
                    "combusting",
                    MACHINE_ENERGY_CAPACITY_ADD,
                    MachineStat.ENERGY_CAPACITY,
                    MachineStat.ENERGY_TRANSFER,
                    MachineStat.ENERGY_GENERATION,
                    MachineStat.EFFICIENCY,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.STABILITY,
                    MachineStat.FLUID_TRANSFER
            ),
            PROCESSING_MACHINE_BEHAVIORS
    );
    public static final ModifierEligibilityProfile STEAM_METHANE_REFORMER = profile(
            "steam_methane_reformer",
            Set.of(
                    ModifierCapability.HAS_FE_STORAGE,
                    ModifierCapability.HAS_FE_INPUT,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_HEAT,
                    ModifierCapability.HAS_STABILITY,
                    ModifierCapability.HAS_FLUID_STORAGE,
                    ModifierCapability.HAS_FLUID_INPUT,
                    ModifierCapability.HAS_FLUID_OUTPUT
            ),
            poweredProcessingAffixes(
                    "reforming",
                    false,
                    MACHINE_ENERGY_CAPACITY_ADD,
                    MachineStat.ENERGY_CAPACITY,
                    MachineStat.ENERGY_TRANSFER,
                    MachineStat.ENERGY_USAGE,
                    MachineStat.HEAT_TRANSFER,
                    MachineStat.MAX_TEMPERATURE,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.EFFICIENCY,
                    MachineStat.STABILITY,
                    MachineStat.FLUID_TRANSFER
            ),
            PROCESSING_MACHINE_BEHAVIORS
    );
    public static final ModifierEligibilityProfile CAVITATION_GENERATOR = profile(
            "cavitation_generator",
            Set.of(
                    ModifierCapability.HAS_FE_STORAGE,
                    ModifierCapability.HAS_FE_OUTPUT,
                    ModifierCapability.HAS_FE_GENERATION,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_HEAT,
                    ModifierCapability.HAS_STABILITY,
                    ModifierCapability.HAS_FLUID_STORAGE,
                    ModifierCapability.HAS_FLUID_INPUT,
                    ModifierCapability.HAS_FLUID_OUTPUT,
                    ModifierCapability.HAS_OUTPUT_SLOT
            ),
            processingStatAffixes(
                    "cavitating",
                    MACHINE_ENERGY_CAPACITY_ADD,
                    MachineStat.ENERGY_CAPACITY,
                    MachineStat.ENERGY_TRANSFER,
                    MachineStat.ENERGY_GENERATION,
                    MachineStat.EFFICIENCY,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.STABILITY,
                    MachineStat.TEMPERATURE_STABILITY,
                    MachineStat.OUTPUT_AMOUNT,
                    MachineStat.FLUID_TRANSFER
            ),
            PROCESSING_MACHINE_BEHAVIORS
    );
    public static final ModifierEligibilityProfile AMMONIA_SYNTHESIZER = profile(
            "ammonia_synthesizer",
            Set.of(
                    ModifierCapability.HAS_FE_STORAGE,
                    ModifierCapability.HAS_FE_INPUT,
                    ModifierCapability.HAS_INPUT_SLOT,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_STABILITY,
                    ModifierCapability.HAS_FLUID_STORAGE,
                    ModifierCapability.HAS_FLUID_INPUT,
                    ModifierCapability.HAS_FLUID_OUTPUT
            ),
            poweredProcessingAffixes(
                    "synthesizing",
                    false,
                    MACHINE_ENERGY_CAPACITY_ADD,
                    MachineStat.ENERGY_CAPACITY,
                    MachineStat.ENERGY_TRANSFER,
                    MachineStat.ENERGY_USAGE,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.EFFICIENCY,
                    MachineStat.STABILITY,
                    MachineStat.FLUID_TRANSFER
            ),
            PROCESSING_MACHINE_BEHAVIORS
    );
    public static final ModifierEligibilityProfile AMMONIA_FUEL_CELL = profile(
            "ammonia_fuel_cell",
            Set.of(
                    ModifierCapability.HAS_FE_STORAGE,
                    ModifierCapability.HAS_FE_OUTPUT,
                    ModifierCapability.HAS_FE_GENERATION,
                    ModifierCapability.HAS_INPUT_SLOT,
                    ModifierCapability.HAS_OUTPUT_SLOT,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_STABILITY,
                    ModifierCapability.HAS_FLUID_STORAGE,
                    ModifierCapability.HAS_FLUID_INPUT
            ),
            processingStatAffixes(
                    "ammonia",
                    MACHINE_ENERGY_CAPACITY_ADD,
                    MachineStat.ENERGY_CAPACITY,
                    MachineStat.ENERGY_TRANSFER,
                    MachineStat.ENERGY_GENERATION,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.EFFICIENCY,
                    MachineStat.STABILITY,
                    MachineStat.FLUID_TRANSFER
            ),
            PROCESSING_MACHINE_BEHAVIORS
    );
    public static final ModifierEligibilityProfile COMPRESSOR_TANK = profile(
            "compressor_tank",
            Set.of(
                    ModifierCapability.HAS_FE_STORAGE,
                    ModifierCapability.HAS_FE_INPUT,
                    ModifierCapability.HAS_FLUID_STORAGE,
                    ModifierCapability.HAS_FLUID_INPUT,
                    ModifierCapability.HAS_FLUID_OUTPUT,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_STABILITY
            ),
            affixes(
                    List.of(MACHINE_ENERGY_CAPACITY_ADD, TANK_RECAPTURE_SLEEVE, TANK_PRESSURE_SPLITTER),
                    MachineStat.ENERGY_CAPACITY,
                    MachineStat.ENERGY_TRANSFER,
                    MachineStat.ENERGY_USAGE,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.FLUID_TRANSFER,
                    MachineStat.STABILITY
            )
    );
    public static final ModifierEligibilityProfile FLUID_TANK = profile(
            "fluid_tank",
            Set.of(
                    ModifierCapability.HAS_FLUID_STORAGE,
                    ModifierCapability.HAS_FLUID_INPUT,
                    ModifierCapability.HAS_FLUID_OUTPUT
            ),
            List.of()
    );
    public static final ModifierEligibilityProfile ALLOY_FURNACE = profile(
            "alloy_furnace",
            Set.of(
                    ModifierCapability.HAS_FE_STORAGE,
                    ModifierCapability.HAS_FE_INPUT,
                    ModifierCapability.HAS_INPUT_SLOT,
                    ModifierCapability.HAS_OUTPUT_SLOT,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_HEAT,
                    ModifierCapability.HAS_STABILITY
            ),
            poweredProcessingAffixes(
                    "alloying",
                    true,
                    MACHINE_ENERGY_CAPACITY_ADD,
                    MachineStat.ENERGY_CAPACITY,
                    MachineStat.ENERGY_USAGE,
                    MachineStat.HEAT_TRANSFER,
                    MachineStat.MAX_TEMPERATURE,
                    MachineStat.HEAT_ISOLATION,
                    MachineStat.WARMUP_TIME,
                    MachineStat.COOLING_RATE,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.STABILITY,
                    MachineStat.TEMPERATURE_STABILITY,
                    MachineStat.OVERHEAT_TOLERANCE
            ),
            POWER_HEAT_CONTROL_BEHAVIORS
    );
    public static final ModifierEligibilityProfile RESONANCE_CALIBRATOR = profile(
            "resonance_calibrator",
            Set.of(
                    ModifierCapability.HAS_FE_STORAGE,
                    ModifierCapability.HAS_FE_INPUT,
                    ModifierCapability.HAS_INPUT_SLOT,
                    ModifierCapability.HAS_OUTPUT_SLOT,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_STABILITY,
                    ModifierCapability.HAS_CALIBRATION,
                    ModifierCapability.HAS_CATALYST_CONTROL
            ),
            poweredProcessingAffixes(
                    "calibrating",
                    true,
                    MACHINE_ENERGY_CAPACITY_ADD,
                    MachineStat.ENERGY_CAPACITY,
                    MachineStat.ENERGY_TRANSFER,
                    MachineStat.ENERGY_USAGE,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.STABILITY,
                    MachineStat.CALIBRATION_QUALITY,
                    MachineStat.CALIBRATION_PRECISION,
                    MachineStat.CATALYST_EFFICIENCY,
                    MachineStat.REFINEMENT_POTENTIAL_BONUS
            ),
            PROCESSING_MACHINE_BEHAVIORS
    );
    public static final ModifierEligibilityProfile TOOL_HEAD = profile(
            "tool_head",
            Set.of(ModifierCapability.HAS_DURABILITY, ModifierCapability.HAS_FIELD_TOOL),
            affixes(
                    List.of(TOOL_HEAD_MOMENTUM_EDGE),
                    MachineStat.DURABILITY,
                    MachineStat.SELF_REPAIR,
                    MachineStat.MINING_SPEED,
                    MachineStat.ATTACK_SPEED,
                    MachineStat.STABILITY,
                    MachineStat.LUCK,
                    MachineStat.TREE_FELL_LIMIT,
                    MachineStat.ORE_BURST_SPEED
            )
    );
    public static final ModifierEligibilityProfile PICK_HEAD = profile(
            "pick_head",
            Set.of(ModifierCapability.HAS_DURABILITY, ModifierCapability.HAS_FIELD_TOOL),
            affixes(
                    List.of(TOOL_HEAD_MOMENTUM_EDGE, TOOL_VEIN_MINER),
                    MachineStat.DURABILITY,
                    MachineStat.SELF_REPAIR,
                    MachineStat.MINING_SPEED,
                    MachineStat.ATTACK_SPEED,
                    MachineStat.STABILITY,
                    MachineStat.LUCK,
                    MachineStat.ORE_BURST_SPEED
            )
    );
    public static final ModifierEligibilityProfile TOOL_ROD = profile(
            "tool_rod",
            Set.of(ModifierCapability.HAS_DURABILITY, ModifierCapability.HAS_FIELD_TOOL),
            affixes(
                    List.of(TOOL_ROD_SHOCK_GRIP),
                    MachineStat.DURABILITY,
                    MachineStat.SELF_REPAIR,
                    MachineStat.MINING_SPEED,
                    MachineStat.ATTACK_SPEED,
                    MachineStat.FE_USAGE,
                    MachineStat.FE_TRANSFER,
                    MachineStat.STABILITY,
                    MachineStat.CONTROL,
                    MachineStat.BATTERY_SUPPORT
            )
    );
    public static final ModifierEligibilityProfile MODULAR_TOOL = profile(
            "modular_tool",
            Set.of(ModifierCapability.HAS_DURABILITY, ModifierCapability.HAS_FIELD_TOOL),
            affixes(
                    MachineStat.DURABILITY,
                    MachineStat.SELF_REPAIR,
                    MachineStat.MINING_SPEED,
                    MachineStat.ATTACK_SPEED,
                    MachineStat.FE_USAGE,
                    MachineStat.STABILITY,
                    MachineStat.BATTERY_SUPPORT,
                    MachineStat.LUCK
            )
    );
    public static final ModifierEligibilityProfile BATTERY_CELL = profile(
            "battery_cell",
            Set.of(
                    ModifierCapability.HAS_FE_STORAGE,
                    ModifierCapability.HAS_FE_INPUT,
                    ModifierCapability.HAS_FE_OUTPUT,
                    ModifierCapability.HAS_IDLE_LOSS
            ),
            affixes(
                    List.of(
                            BATTERY_CELL_ENERGY_CAPACITY_ADD,
                            BATTERY_CHARGE_BACKPLANE,
                            BATTERY_LOW_CHARGE_RETENTION,
                            BATTERY_BROWNOUT_CUSHION,
                            BATTERY_LEAK_QUENCHER
                    ),
                    MachineStat.ENERGY_CAPACITY,
                    MachineStat.EFFICIENCY,
                    MachineStat.ENERGY_TRANSFER,
                    MachineStat.IDLE_LOSS
            )
    );
    public static final ModifierEligibilityProfile UNIQUE_BATTERY_CELL = profile(
            "unique_battery_cell",
            Set.of(
                    ModifierCapability.HAS_FE_STORAGE,
                    ModifierCapability.HAS_FE_INPUT,
                    ModifierCapability.HAS_FE_OUTPUT,
                    ModifierCapability.HAS_IDLE_LOSS
            ),
            List.of()
    );
    public static final ModifierEligibilityProfile BATTERY_CHASSIS = profile(
            "battery_chassis",
            Set.of(
                    ModifierCapability.HAS_FE_STORAGE,
                    ModifierCapability.HAS_FE_INPUT,
                    ModifierCapability.HAS_FE_OUTPUT,
                    ModifierCapability.HAS_BATTERY_CELLS,
                    ModifierCapability.HAS_BURST_TRANSFER,
                    ModifierCapability.HAS_IDLE_LOSS,
                    ModifierCapability.HAS_GLOBAL_MODIFIER_EFFECTS,
                    ModifierCapability.HAS_STABILITY
            ),
            batteryChassisAffixes()
    );
    public static final ModifierEligibilityProfile CRUSH_HEAD = profile(
            "crush_head",
            Set.of(
                    ModifierCapability.HAS_GEAR_STATS,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_OUTPUT_SLOT,
                    ModifierCapability.HAS_CRUSHER_SPECIALS
            ),
            crushHeadAffixes()
    );
    public static final ModifierEligibilityProfile ALLOY_CRUCIBLE = profile(
            "alloy_crucible",
            Set.of(
                    ModifierCapability.HAS_GEAR_STATS,
                    ModifierCapability.HAS_INPUT_SLOT,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_OUTPUT_SLOT,
                    ModifierCapability.HAS_HEAT,
                    ModifierCapability.HAS_STABILITY
            ),
            processingAffixes(
                    "alloying",
                    true,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.HEAT_TRANSFER,
                    MachineStat.HEAT_ISOLATION,
                    MachineStat.WARMUP_TIME,
                    MachineStat.COOLING_RATE,
                    MachineStat.TEMPERATURE_STABILITY,
                    MachineStat.OVERHEAT_TOLERANCE,
                    MachineStat.STABILITY
            ),
            POWER_GRACE_BEHAVIOR
    );
    public static final ModifierEligibilityProfile HEAT_CORE = profile(
            "heat_core",
            Set.of(
                    ModifierCapability.HAS_GEAR_STATS,
                    ModifierCapability.HAS_FE_GENERATION,
                    ModifierCapability.HAS_FUEL_SLOT,
                    ModifierCapability.HAS_HEAT
            ),
            affixes(
                    List.of(HEAT_CORE_HEAT_SOAK),
                    MachineStat.FUEL_EFFICIENCY,
                    MachineStat.HEAT_ISOLATION,
                    MachineStat.HEAT_TRANSFER,
                    MachineStat.MAX_TEMPERATURE,
                    MachineStat.WARMUP_TIME,
                    MachineStat.COOLING_RATE,
                    MachineStat.TEMPERATURE_STABILITY,
                    MachineStat.OVERHEAT_TOLERANCE,
                    MachineStat.ENERGY_GENERATION
            ),
            POWER_GRACE_BEHAVIOR
    );
    public static final ModifierEligibilityProfile SERVO = profile(
            "servo",
            Set.of(
                    ModifierCapability.HAS_GEAR_STATS,
                    ModifierCapability.HAS_FE_INPUT,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_HEAT,
                    ModifierCapability.HAS_STABILITY,
                    ModifierCapability.HAS_FLUID_OUTPUT
            ),
            poweredProcessingAffixes(
                    "serving",
                    false,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.ENERGY_USAGE,
                    MachineStat.HEAT_TRANSFER,
                    MachineStat.HEAT_ISOLATION,
                    MachineStat.WARMUP_TIME,
                    MachineStat.COOLING_RATE,
                    MachineStat.STABILITY,
                    MachineStat.TEMPERATURE_STABILITY,
                    MachineStat.OVERHEAT_TOLERANCE,
                    MachineStat.FLUID_TRANSFER
            ),
            POWER_GRACE_BEHAVIOR
    );
    public static final ModifierEligibilityProfile FUEL_BOX = profile(
            "fuel_box",
            Set.of(
                    ModifierCapability.HAS_GEAR_STATS,
                    ModifierCapability.HAS_FUEL_SLOT,
                    ModifierCapability.HAS_INPUT_SLOT,
                    ModifierCapability.HAS_STABILITY
            ),
            affixes(
                    List.of(FUEL_BOX_COAL_SILO, FUEL_BOX_DRAFT_COLLAR, FUEL_BOX_ASH_TRAP, FUEL_BOX_EMBER_METER, FUEL_BOX_CINDER_RETURN, FUEL_BOX_QUEUE_SEQUENCER),
                    MachineStat.FUEL_EFFICIENCY,
                    MachineStat.FUEL_DURATION,
                    MachineStat.STABILITY
            )
    );
    public static final ModifierEligibilityProfile BIO_CHAMBER = profile(
            "bio_chamber",
            Set.of(
                    ModifierCapability.HAS_GEAR_STATS,
                    ModifierCapability.HAS_FUEL_SLOT,
                    ModifierCapability.HAS_FE_GENERATION
            ),
            affixes(
                    List.of(ENERGY_GENERATION),
                    MachineStat.FUEL_EFFICIENCY,
                    MachineStat.POTATO_POWER,
                    MachineStat.CARROT_POWER,
                    MachineStat.BREAD_POWER,
                    MachineStat.SAPLING_POWER,
                    MachineStat.SEED_POWER,
                    MachineStat.PLANT_POWER,
                    MachineStat.ORGANIC_REAGENT_POWER,
                    MachineStat.COMPOSTED_BIOMASS_POWER,
                    MachineStat.ALGAE_POWER,
                    MachineStat.RICH_BIOMASS_POWER,
                    MachineStat.FUEL_DURATION
            )
    );
    public static final ModifierEligibilityProfile REACTOR_CHAMBER = profile(
            "reactor_chamber",
            Set.of(
                    ModifierCapability.HAS_GEAR_STATS,
                    ModifierCapability.HAS_FE_GENERATION,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_STABILITY
            ),
            processingStatAffixes(
                    "reacting",
                    List.of(REACTOR_SAMPLE_LOOP, REACTOR_MODERATOR_TEETH, REACTOR_FLUX_LENS, REACTOR_EXHAUST_PORT),
                    MachineStat.ENERGY_GENERATION,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.STABILITY
            )
    );
    public static final ModifierEligibilityProfile RECOVERY_FILTER = profile(
            "recovery_filter",
            Set.of(
                    ModifierCapability.HAS_GEAR_STATS,
                    ModifierCapability.HAS_FE_GENERATION,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_OUTPUT_SLOT,
                    ModifierCapability.HAS_STABILITY
            ),
            processingStatAffixes(
                    "screening",
                    List.of(FILTER_FINE_MESH, FILTER_MAGNETIC_CATCH, FILTER_SIEVE_MEMORY, FILTER_FAST_BACKFLUSH, FILTER_CATALYST_SWEEP, FILTER_LOW_DRAG),
                    MachineStat.EFFICIENCY,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.OUTPUT_AMOUNT,
                    MachineStat.STABILITY
            )
    );
    public static final ModifierEligibilityProfile CONTAINMENT_LINING = profile(
            "containment_lining",
            Set.of(
                    ModifierCapability.HAS_GEAR_STATS,
                    ModifierCapability.HAS_STABILITY
            ),
            affixes(
                    List.of(LINING_EMERGENCY_SCRAM, LINING_NEUTRON_BAFFLE, LINING_RESIDUE_QUARANTINE, LINING_PRESSURE_CURTAIN, LINING_QUIET_CORE, LINING_SEALED_DUMP, LINING_CONTAINMENT_WINDOW),
                    MachineStat.STABILITY
            )
    );
    public static final ModifierEligibilityProfile AMMONIA_CATALYST_BED = profile(
            "ammonia_catalyst_bed",
            Set.of(
                    ModifierCapability.HAS_GEAR_STATS,
                    ModifierCapability.HAS_FE_INPUT,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_STABILITY,
                    ModifierCapability.HAS_FLUID_OUTPUT
            ),
            poweredProcessingAffixes(
                    "catalyzing",
                    false,
                    MachineStat.ENERGY_USAGE,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.EFFICIENCY,
                    MachineStat.STABILITY,
                    MachineStat.FLUID_TRANSFER
            )
    );
    public static final ModifierEligibilityProfile REFORMING_CATALYST_BED = profile(
            "reforming_catalyst_bed",
            Set.of(
                    ModifierCapability.HAS_GEAR_STATS,
                    ModifierCapability.HAS_FE_INPUT,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_STABILITY,
                    ModifierCapability.HAS_FLUID_OUTPUT
            ),
            poweredProcessingAffixes(
                    "reforming",
                    false,
                    MachineStat.ENERGY_USAGE,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.EFFICIENCY,
                    MachineStat.STABILITY,
                    MachineStat.FLUID_TRANSFER
            )
    );
    public static final ModifierEligibilityProfile FUEL_CELL_MEMBRANE = profile(
            "fuel_cell_membrane",
            Set.of(
                    ModifierCapability.HAS_GEAR_STATS,
                    ModifierCapability.HAS_FE_OUTPUT,
                    ModifierCapability.HAS_FE_GENERATION,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_STABILITY,
                    ModifierCapability.HAS_FLUID_INPUT
            ),
            processingStatAffixes(
                    "membrane",
                    MachineStat.ENERGY_GENERATION,
                    MachineStat.ENERGY_TRANSFER,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.EFFICIENCY,
                    MachineStat.STABILITY,
                    MachineStat.FLUID_TRANSFER
            )
    );
    public static final ModifierEligibilityProfile CAVITATION_ROTOR = profile(
            "cavitation_rotor",
            Set.of(
                    ModifierCapability.HAS_GEAR_STATS,
                    ModifierCapability.HAS_DURABILITY,
                    ModifierCapability.HAS_FE_GENERATION,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_STABILITY,
                    ModifierCapability.HAS_OUTPUT_SLOT
            ),
            processingStatAffixes(
                    "cavitating",
                    MachineStat.DURABILITY,
                    MachineStat.ENERGY_GENERATION,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.OUTPUT_AMOUNT,
                    MachineStat.STABILITY
            )
    );
    public static final ModifierEligibilityProfile VOID_CHAMBER = profile(
            "void_chamber",
            Set.of(
                    ModifierCapability.HAS_GEAR_STATS,
                    ModifierCapability.HAS_FE_GENERATION,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_STABILITY
            ),
            processingStatAffixes(
                    "containing",
                    MachineStat.ENERGY_GENERATION,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.STABILITY
            )
    );
    public static final ModifierEligibilityProfile COLLAPSE_NOZZLE = profile(
            "collapse_nozzle",
            Set.of(
                    ModifierCapability.HAS_GEAR_STATS,
                    ModifierCapability.HAS_FE_GENERATION,
                    ModifierCapability.HAS_FE_OUTPUT,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_HEAT,
                    ModifierCapability.HAS_STABILITY,
                    ModifierCapability.HAS_FLUID_OUTPUT,
                    ModifierCapability.HAS_OUTPUT_SLOT
            ),
            processingStatAffixes(
                    "focusing",
                    MachineStat.ENERGY_GENERATION,
                    MachineStat.ENERGY_TRANSFER,
                    MachineStat.TEMPERATURE_STABILITY,
                    MachineStat.OUTPUT_AMOUNT,
                    MachineStat.FLUID_TRANSFER,
                    MachineStat.STABILITY
            )
    );
    public static final ModifierEligibilityProfile DIMENSIONAL_STABILIZER = profile(
            "dimensional_stabilizer",
            Set.of(
                    ModifierCapability.HAS_GEAR_STATS,
                    ModifierCapability.HAS_FE_GENERATION,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_STABILITY
            ),
            processingStatAffixes(
                    "stabilizing",
                    MachineStat.EFFICIENCY,
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.STABILITY
            )
    );
    public static final ModifierEligibilityProfile DISASSEMBLY_HEAD = profile(
            "disassembly_head",
            Set.of(
                    ModifierCapability.HAS_GEAR_STATS,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_OUTPUT_SLOT,
                    ModifierCapability.HAS_STABILITY
            ),
            processingAffixes(
                    "disassembling",
                    false,
                    List.of(DISASSEMBLY_FASTENER_FINDER, DISASSEMBLY_GENTLE_PRY, DISASSEMBLY_TRACE_CUTTER),
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.STABILITY
            )
    );
    public static final ModifierEligibilityProfile FLUID_PUMP = profile(
            "fluid_pump",
            Set.of(
                    ModifierCapability.HAS_GEAR_STATS,
                    ModifierCapability.HAS_FLUID_STORAGE,
                    ModifierCapability.HAS_FLUID_INPUT,
                    ModifierCapability.HAS_FLUID_OUTPUT
            ),
            affixes(
                    List.of(PUMP_SELF_PRIMING, PUMP_CHECK_VALVE, PUMP_PULSE, PUMP_SEAL_FLUSH, PUMP_HIGH_HEAD, PUMP_VAPOR_LOCK_BREAKER, PUMP_METERED_DRIP,
                            FLUID_CAPACITY_ADD),
                    MachineStat.FLUID_TRANSFER,
                    MachineStat.FLUID_CAPACITY
            )
    );
    public static final ModifierEligibilityProfile SOLAR_ARRAY_EXTENDER = profile(
            "solar_array_extender",
            Set.of(
                    ModifierCapability.HAS_GEAR_STATS,
                    ModifierCapability.HAS_FE_GENERATION,
                    ModifierCapability.HAS_SOLAR_ARRAY
            ),
            affixes(
                    MachineStat.ENERGY_GENERATION,
                    MachineStat.SOLAR_PANEL_LIMIT,
                    MachineStat.MOONLIGHT_CONVERSION,
                    MachineStat.WEATHER_RECOVERY,
                    MachineStat.CLEAR_SKY_AMPLIFICATION
            )
    );
    public static final ModifierEligibilityProfile RESONANCE_COIL = profile(
            "resonance_coil",
            Set.of(
                    ModifierCapability.HAS_GEAR_STATS,
                    ModifierCapability.HAS_FE_INPUT,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_CALIBRATION
            ),
            poweredProcessingAffixes(
                    "calibrating",
                    false,
                    List.of(COIL_HARMONIC_LOCK, COIL_PHASE_TAP, COIL_RETURN_WINDING),
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.ENERGY_TRANSFER,
                    MachineStat.ENERGY_USAGE,
                    MachineStat.CALIBRATION_QUALITY
            )
    );
    public static final ModifierEligibilityProfile CONTROL_BOARD = profile(
            "control_board",
            Set.of(
                    ModifierCapability.HAS_GEAR_STATS,
                    ModifierCapability.HAS_PROCESSING,
                    ModifierCapability.HAS_STABILITY,
                    ModifierCapability.HAS_CALIBRATION
            ),
            affixes(
                    List.of(BOARD_BRANCH_PREDICTOR, BOARD_LENS_ROUTER, BOARD_ERROR_MAP, BOARD_INTERRUPT_MASK, BOARD_CLOCK_DIVIDER),
                    MachineStat.CALIBRATION_PRECISION,
                    MachineStat.STABILITY,
                    MachineStat.REFINEMENT_POTENTIAL_BONUS
            )
    );
    public static final ModifierEligibilityProfile STABILIZER_MATRIX = profile(
            "stabilizer_matrix",
            Set.of(
                    ModifierCapability.HAS_GEAR_STATS,
                    ModifierCapability.HAS_FE_INPUT,
                    ModifierCapability.HAS_STABILITY,
                    ModifierCapability.HAS_CALIBRATION,
                    ModifierCapability.HAS_CATALYST_CONTROL
            ),
            affixes(
                    List.of(MATRIX_QUENCH_GATE, MATRIX_CATALYST_CRADLE, MATRIX_DEADBAND_TUNER, MATRIX_POTENTIAL_CLAMP),
                    MachineStat.CALIBRATION_QUALITY,
                    MachineStat.STABILITY,
                    MachineStat.CATALYST_EFFICIENCY,
                    MachineStat.ENERGY_USAGE
            )
    );

    public static List<ModifierDefinition> definitionsForCapability(ModifierCapability capability) {
        return MODIFIERS_BY_CAPABILITY.getOrDefault(capability, List.of());
    }

    public static List<ModifierDefinition> definitionsForCapabilities(Set<ModifierCapability> capabilities) {
        LinkedHashSet<ModifierDefinition> definitions = new LinkedHashSet<>();
        for (ModifierCapability capability : ModifierCapability.values()) {
            if (capabilities.contains(capability)) {
                definitions.addAll(definitionsForCapability(capability));
            }
        }
        return List.copyOf(definitions);
    }

    public static Map<ModifierCapability, List<ModifierDefinition>> definitionsByCapability() {
        return MODIFIERS_BY_CAPABILITY;
    }

    public static List<ModifierEligibilityProfile> allProfiles() {
        return List.of(
                CRUSHER,
                MINERS_COMPANION,
                FORESTRY_COMPANION,
                FURNACE,
                ELECTRIC_FURNACE,
                SOLID_FUEL_BURNER,
                BIO_GENERATOR,
                SOLAR_PANEL,
                SOLAR_ARRAY_CONTROLLER,
                POTENTIAL_REACTOR,
                CORROSION_CELL,
                VACUUM_COLLAPSE_GENERATOR,
                COMPONENT_RECYCLER,
                METAL_PRESS,
                MELTER,
                BATTERY_ASSEMBLER,
                COAL_GASIFIER,
                SYNGAS_COMBUSTOR,
                STEAM_METHANE_REFORMER,
                CAVITATION_GENERATOR,
                AMMONIA_SYNTHESIZER,
                AMMONIA_FUEL_CELL,
                COMPRESSOR_TANK,
                FLUID_TANK,
                ALLOY_FURNACE,
                RESONANCE_CALIBRATOR,
                TOOL_HEAD,
                PICK_HEAD,
                TOOL_ROD,
                MODULAR_TOOL,
                BATTERY_CELL,
                UNIQUE_BATTERY_CELL,
                BATTERY_CHASSIS,
                CRUSH_HEAD,
                ALLOY_CRUCIBLE,
                HEAT_CORE,
                SERVO,
                FUEL_BOX,
                BIO_CHAMBER,
                REACTOR_CHAMBER,
                RECOVERY_FILTER,
                CONTAINMENT_LINING,
                AMMONIA_CATALYST_BED,
                REFORMING_CATALYST_BED,
                FUEL_CELL_MEMBRANE,
                CAVITATION_ROTOR,
                VOID_CHAMBER,
                COLLAPSE_NOZZLE,
                DIMENSIONAL_STABILIZER,
                DISASSEMBLY_HEAD,
                FLUID_PUMP,
                SOLAR_ARRAY_EXTENDER,
                RESONANCE_COIL,
                CONTROL_BOARD,
                STABILIZER_MATRIX
        );
    }

    public static ModifierEligibilityProfile forMachine(MachineType type) {
        return switch (type) {
            case BATTERY_CELL -> BATTERY_CELL;
            case BATTERY_CHASSIS -> BATTERY_CHASSIS;
            case BATTERY_ASSEMBLER -> BATTERY_ASSEMBLER;
            case SOLID_FUEL_BURNER -> SOLID_FUEL_BURNER;
            case BIO_GENERATOR -> BIO_GENERATOR;
            case SOLAR_PANEL -> SOLAR_PANEL;
            case SOLAR_ARRAY_CONTROLLER -> SOLAR_ARRAY_CONTROLLER;
            case POTENTIAL_REACTOR -> POTENTIAL_REACTOR;
            case CORROSION_CELL -> CORROSION_CELL;
            case VACUUM_COLLAPSE_GENERATOR -> VACUUM_COLLAPSE_GENERATOR;
            case COMPONENT_RECYCLER -> COMPONENT_RECYCLER;
            case CRUSHER -> CRUSHER;
            case FURNACE -> FURNACE;
            case ELECTRIC_FURNACE -> ELECTRIC_FURNACE;
            case ALLOY_FURNACE -> ALLOY_FURNACE;
            case METAL_PRESS -> METAL_PRESS;
            case MELTER -> MELTER;
            case COAL_GASIFIER -> COAL_GASIFIER;
            case SYNGAS_COMBUSTOR -> SYNGAS_COMBUSTOR;
            case STEAM_METHANE_REFORMER -> STEAM_METHANE_REFORMER;
            case AMMONIA_SYNTHESIZER -> AMMONIA_SYNTHESIZER;
            case AMMONIA_FUEL_CELL -> AMMONIA_FUEL_CELL;
            case CAVITATION_GENERATOR -> CAVITATION_GENERATOR;
            case FLUID_TANK -> FLUID_TANK;
            case COMPRESSOR_TANK -> COMPRESSOR_TANK;
            case RESONANCE_CALIBRATOR -> RESONANCE_CALIBRATOR;
            case TOOL_HEAD -> TOOL_HEAD;
            case TOOL_ROD -> TOOL_ROD;
            case MODULAR_TOOL -> MODULAR_TOOL;
            case MINERS_COMPANION -> MINERS_COMPANION;
            case FORESTRY_COMPANION -> FORESTRY_COMPANION;
        };
    }

    public static ModifierEligibilityProfile forBatteryCell(boolean unique) {
        return unique ? UNIQUE_BATTERY_CELL : BATTERY_CELL;
    }

    public static ModifierEligibilityProfile forToolHead(boolean pickHead) {
        return pickHead ? PICK_HEAD : TOOL_HEAD;
    }

    public static ModifierEligibilityProfile forMachinePart(MachinePartType partType, MachineType machineType) {
        return switch (partType) {
            case CRUSH_HEAD -> CRUSH_HEAD;
            case ALLOY_CRUCIBLE -> ALLOY_CRUCIBLE;
            case HEAT_CORE -> HEAT_CORE;
            case BIO_CHAMBER -> BIO_CHAMBER;
            case SERVO -> SERVO;
            case FUEL_BOX -> FUEL_BOX;
            case REACTOR_CHAMBER -> REACTOR_CHAMBER;
            case RECOVERY_FILTER -> RECOVERY_FILTER;
            case CONTAINMENT_LINING -> CONTAINMENT_LINING;
            case DISASSEMBLY_HEAD -> DISASSEMBLY_HEAD;
            case FLUID_PUMP -> FLUID_PUMP;
            case REFORMING_CATALYST_BED -> REFORMING_CATALYST_BED;
            case AMMONIA_CATALYST_BED -> AMMONIA_CATALYST_BED;
            case FUEL_CELL_MEMBRANE -> FUEL_CELL_MEMBRANE;
            case CAVITATION_ROTOR -> CAVITATION_ROTOR;
            case VOID_CHAMBER -> VOID_CHAMBER;
            case COLLAPSE_NOZZLE -> COLLAPSE_NOZZLE;
            case DIMENSIONAL_STABILIZER -> DIMENSIONAL_STABILIZER;
            case SOLAR_ARRAY_EXTENDER -> SOLAR_ARRAY_EXTENDER;
            case RESONANCE_COIL -> RESONANCE_COIL;
            case CONTROL_BOARD -> CONTROL_BOARD;
            case STABILIZER_MATRIX -> STABILIZER_MATRIX;
        };
    }

    static ModifierValueRange percentRangeForTier(int tier) {
        return tierRange(PERCENT_TIER_RANGES, tier);
    }

    static ModifierValueRange machineEnergyCapacityAddRangeForTier(int tier) {
        return tierRange(MACHINE_ENERGY_CAPACITY_ADD_RANGES, tier);
    }

    static ModifierValueRange batteryCellEnergyCapacityAddRangeForTier(int tier) {
        return tierRange(BATTERY_CELL_ENERGY_CAPACITY_ADD_RANGES, tier);
    }

    static ModifierValueRange inputSlotsAddRangeForTier(int tier) {
        return tierRange(INPUT_SLOTS_ADD_RANGES, tier);
    }

    public static int maxBatteryChassisAdditionalSlots() {
        return (int) BATTERY_CHASSIS_BATTERY_SLOTS_ADD_RANGES.getLast().max();
    }

    static ModifierValueRange batteryChassisBatterySlotsAddRangeForTier(int tier) {
        return tierRange(BATTERY_CHASSIS_BATTERY_SLOTS_ADD_RANGES, tier);
    }

    static ModifierValueRange minersCompanionFilterSlotsAddRangeForTier(int tier) {
        return tierRange(MINERS_COMPANION_FILTER_SLOTS_ADD_RANGES, tier);
    }

    private static ModifierEligibilityProfile profile(
            String id,
            Set<ModifierCapability> capabilities,
            List<ModifierDefinition> definitions
    ) {
        return profile(id, capabilities, definitions, List.of());
    }

    private static ModifierEligibilityProfile profile(
            String id,
            Set<ModifierCapability> capabilities,
            List<ModifierDefinition> definitions,
            List<MachineBehavior> rollableBehaviors
    ) {
        ModifierEligibilityProfile profile = new ModifierEligibilityProfile(id, capabilities, definitions, rollableBehaviors);
        validateProfile(profile);
        return profile;
    }

    public static String behaviorModGroup(MachineBehavior behavior) {
        return behavior == MachineBehavior.BULK_SPEED ? PROCESSING_SPEED_GROUP : behavior.getSerializedName();
    }

    public static int behaviorRollWeight(MachineBehavior behavior) {
        return behavior == MachineBehavior.BULK_SPEED ? BULK_SPEED_WEIGHT : ModifierDefinition.DEFAULT_ROLL_WEIGHT;
    }

    public static boolean enablesBehavior(MachineModifier modifier, MachineBehavior behavior) {
        return switch (behavior) {
            case CHARGE_BALANCER -> BATTERY_CHASSIS_BALANCE_MODE_AFFIX_ID.equals(modifier.affixId());
            case OUTPUT_GUARD -> CRUSHER_FRAME_AFFIX_ID.equals(modifier.affixId());
            case POWER_GRACE -> BATTERY_BROWNOUT_CUSHION.id().equals(modifier.affixId());
            default -> false;
        };
    }

    public static boolean isBatteryChassisBalanceMode(MachineModifier modifier) {
        return BATTERY_CHASSIS_BALANCE_MODE_AFFIX_ID.equals(modifier.affixId());
    }

    public static boolean isBatteryChassisBalanceMode(ModifierDefinition definition) {
        return BATTERY_CHASSIS_BALANCE_MODE_AFFIX_ID.equals(definition.id());
    }

    public static boolean isBehaviorOnlyAffix(MachineModifier modifier) {
        return BEHAVIOR_ONLY_AFFIX_IDS.contains(modifier.affixId());
    }

    public static boolean isBehaviorOnlyAffix(ModifierDefinition definition) {
        return definition.isBehaviorOnly();
    }

    public static boolean hasCustomDescription(MachineModifier modifier) {
        return DESCRIBED_AFFIX_IDS.contains(modifier.affixId());
    }

    public static boolean hasCustomDescription(ModifierDefinition definition) {
        return DESCRIBED_AFFIX_IDS.contains(definition.id());
    }

    private static List<ModifierDefinition> affixes(MachineStat... stats) {
        return affixes(List.of(), stats);
    }

    private static List<ModifierDefinition> affixes(ModifierDefinition extra, MachineStat... stats) {
        return affixes(List.of(extra), stats);
    }

    private static List<ModifierDefinition> affixes(List<ModifierDefinition> extras, MachineStat... stats) {
        LinkedHashSet<ModifierDefinition> definitions = new LinkedHashSet<>(extras);
        for (MachineStat stat : stats) {
            if (stat == MachineStat.ENERGY_GENERATION) {
                definitions.add(ENERGY_GENERATION_ADD);
            }
            if (stat == MachineStat.MAX_TEMPERATURE) {
                definitions.add(MAX_TEMPERATURE_ADD);
            }
            if (stat == MachineStat.DURABILITY) {
                definitions.add(DURABILITY_ADD);
            }
            definitions.add(definitionForStat(stat));
        }
        return List.copyOf(definitions);
    }

    private static List<ModifierDefinition> processingAffixes(
            String actionId,
            boolean itemOutput,
            MachineStat... stats
    ) {
        return processingAffixes(actionId, itemOutput, List.of(), stats);
    }

    private static List<ModifierDefinition> processingAffixes(
            String actionId,
            boolean itemOutput,
            ModifierDefinition extra,
            MachineStat... stats
    ) {
        return processingAffixes(actionId, itemOutput, List.of(extra), stats);
    }

    private static List<ModifierDefinition> processingAffixes(
            String actionId,
            boolean itemOutput,
            List<ModifierDefinition> extras,
            MachineStat... stats
    ) {
        LinkedHashSet<ModifierDefinition> definitions = new LinkedHashSet<>(affixes(extras, stats));
        definitions.add(processingSpecific(actionId));
        definitions.add(INSTANT_PROCESS);
        if (itemOutput) {
            definitions.add(SUPER_OUTPUT);
        }
        return List.copyOf(definitions);
    }

    private static List<ModifierDefinition> crusherAffixes() {
        LinkedHashSet<ModifierDefinition> definitions = new LinkedHashSet<>(poweredProcessingAffixes(
                "crushing",
                true,
                MachineStat.ENERGY_USAGE,
                MachineStat.OUTPUT_AMOUNT,
                MachineStat.PROCESSING_SPEED
        ));
        definitions.addAll(List.of(
                CRUSHER_FRAME,
                CRUSHER_KINETICS,
                CRUSHER_JAWS,
                CRUSHER_ORE_HANDLING,
                CRUSHER_BATTERY_LINK,
                CRUSHER_FEED_CONTROL,
                CRUSHER_COMPRESSION,
                CRUSHER_VIBRATION,
                CRUSHER_THROUGHPUT,
                CRUSHER_SALVAGE
        ));
        return List.copyOf(definitions);
    }

    private static List<ModifierDefinition> crushHeadAffixes() {
        LinkedHashSet<ModifierDefinition> definitions = new LinkedHashSet<>(processingAffixes(
                "crushing",
                true,
                MachineStat.PROCESSING_SPEED,
                MachineStat.OUTPUT_AMOUNT
        ));
        definitions.addAll(List.of(
                CRUSH_HEAD_PULVERIZING,
                CRUSH_HEAD_JAGGED,
                CRUSH_HEAD_KINETIC,
                CRUSH_HEAD_SCUFFED,
                CRUSH_HEAD_DUST_GROOVE
        ));
        return List.copyOf(definitions);
    }

    private static List<ModifierDefinition> processingStatAffixes(
            String actionId,
            ModifierDefinition extra,
            MachineStat... stats
    ) {
        return processingStatAffixes(actionId, List.of(extra), stats);
    }

    private static List<ModifierDefinition> processingStatAffixes(
            String actionId,
            MachineStat... stats
    ) {
        return processingStatAffixes(actionId, List.of(), stats);
    }

    private static List<ModifierDefinition> processingStatAffixes(
            String actionId,
            List<ModifierDefinition> extras,
            MachineStat... stats
    ) {
        LinkedHashSet<ModifierDefinition> definitions = new LinkedHashSet<>(affixes(extras, stats));
        definitions.add(processingSpecific(actionId));
        return List.copyOf(definitions);
    }

    private static List<ModifierDefinition> poweredProcessingAffixes(
            String actionId,
            boolean itemOutput,
            MachineStat... stats
    ) {
        LinkedHashSet<ModifierDefinition> definitions = new LinkedHashSet<>(processingAffixes(actionId, itemOutput, stats));
        definitions.add(OVERCLOCKED);
        return List.copyOf(definitions);
    }

    private static List<ModifierDefinition> poweredProcessingAffixes(
            String actionId,
            boolean itemOutput,
            ModifierDefinition extra,
            MachineStat... stats
    ) {
        LinkedHashSet<ModifierDefinition> definitions = new LinkedHashSet<>(processingAffixes(actionId, itemOutput, extra, stats));
        definitions.add(OVERCLOCKED);
        return List.copyOf(definitions);
    }

    private static List<ModifierDefinition> poweredProcessingAffixes(
            String actionId,
            boolean itemOutput,
            List<ModifierDefinition> extras,
            MachineStat... stats
    ) {
        LinkedHashSet<ModifierDefinition> definitions = new LinkedHashSet<>(
                processingAffixes(actionId, itemOutput, extras, stats)
        );
        definitions.add(OVERCLOCKED);
        return List.copyOf(definitions);
    }

    private static ModifierDefinition processingSpecific(String actionId) {
        return ModifierDefinition.rollable(
                actionId,
                PROCESSING_SPEED_GROUP,
                ModifierSlot.SUFFIX,
                MachineStat.PROCESSING_SPEED,
                ModifierOperation.INCREASED_PERCENT,
                PROCESSING_SPECIFIC_SPEED_RANGES
        ).withRollWeight(PROCESSING_SPECIFIC_SPEED_WEIGHT);
    }

    private static ModifierDefinition behaviorOnly(
            String id,
            ModifierSlot slot,
            MachineStat stat,
            int rollWeight
    ) {
        return ModifierDefinition.rollableUntiered(
                id,
                id,
                slot,
                stat,
                ModifierOperation.ADD,
                ModifierValueRange.fixed(0)
        ).asBehaviorOnly().withRollWeight(rollWeight);
    }

    private static ModifierDefinition runtime(
            String id,
            String group,
            ModifierSlot slot,
            MachineStat stat,
            ModifierOperation operation,
            List<ModifierValueRange> ranges,
            int rollWeight
    ) {
        return ModifierDefinition.rollable(id, group, slot, stat, operation, ranges)
                .withoutTargetedRefinement()
                .withRollWeight(rollWeight);
    }

    private static ModifierDefinition runtimeUntiered(
            String id,
            String group,
            ModifierSlot slot,
            MachineStat stat,
            ModifierOperation operation,
            double value,
            int rollWeight
    ) {
        return ModifierDefinition.rollableUntiered(id, group, slot, stat, operation, ModifierValueRange.fixed(value))
                .withoutTargetedRefinement()
                .withRollWeight(rollWeight);
    }

    private static void validateProfile(ModifierEligibilityProfile profile) {
        Set<MachineStat> capabilityStats = statsForCapabilities(profile.capabilities());
        List<ModifierDefinition> unsupportedDefinitions = profile.definitions().stream()
                .filter(definition -> definition.effects().stream()
                        .map(ModifierEffectDefinition::stat)
                        .anyMatch(stat -> !capabilityStats.contains(stat)))
                .toList();
        if (!unsupportedDefinitions.isEmpty()) {
            throw new IllegalStateException("Modifier profile " + profile.id()
                    + " has definitions not attached to its capabilities: " + unsupportedDefinitions);
        }
        boolean hasNonPowerGraceBehavior = profile.rollableBehaviors().stream()
                .anyMatch(behavior -> behavior != MachineBehavior.POWER_GRACE);
        if (hasNonPowerGraceBehavior && !profile.hasCapability(ModifierCapability.HAS_PROCESSING)) {
            throw new IllegalStateException("Modifier profile " + profile.id()
                    + " has processing behaviors without HAS_PROCESSING: " + profile.rollableBehaviors());
        }
    }

    private static Set<MachineStat> statsForCapabilities(Set<ModifierCapability> capabilities) {
        LinkedHashSet<MachineStat> stats = new LinkedHashSet<>();
        for (ModifierDefinition definition : definitionsForCapabilities(capabilities)) {
            definition.effects().stream().map(ModifierEffectDefinition::stat).forEach(stats::add);
        }
        return stats;
    }

    private static Map<ModifierCapability, List<ModifierDefinition>> createModifiersByCapability() {
        EnumMap<ModifierCapability, List<ModifierDefinition>> definitions = new EnumMap<>(ModifierCapability.class);
        put(
                definitions,
                ModifierCapability.HAS_FE_STORAGE,
                MACHINE_ENERGY_CAPACITY_ADD,
                BATTERY_CELL_ENERGY_CAPACITY_ADD,
                ENERGY_CAPACITY
        );
        put(definitions, ModifierCapability.HAS_FE_INPUT, ENERGY_TRANSFER, ENERGY_USAGE);
        put(definitions, ModifierCapability.HAS_FE_OUTPUT, ENERGY_TRANSFER, EFFICIENCY);
        put(definitions, ModifierCapability.HAS_FE_GENERATION, ENERGY_GENERATION_ADD, ENERGY_GENERATION, EFFICIENCY);
        put(definitions, ModifierCapability.HAS_FUEL_SLOT, FUEL_SLOTS, FUEL_EFFICIENCY, FUEL_DURATION, EFFICIENCY);
        put(definitions, ModifierCapability.HAS_INPUT_SLOT, FUEL_SLOTS);
        put(definitions, ModifierCapability.HAS_OUTPUT_SLOT, OUTPUT_AMOUNT, SUPER_OUTPUT);
        put(definitions, ModifierCapability.HAS_PROCESSING, PROCESSING_SPEED, EFFICIENCY, INSTANT_PROCESS);
        put(
                definitions,
                ModifierCapability.HAS_HEAT,
                HEAT_TRANSFER,
                MAX_TEMPERATURE,
                MAX_TEMPERATURE_ADD,
                HEAT_ISOLATION,
                WARMUP_TIME,
                COOLING_RATE,
                TEMPERATURE_STABILITY,
                OVERHEAT_TOLERANCE
        );
        put(definitions, ModifierCapability.HAS_BATTERY_CELLS, BATTERY_CHASSIS_BATTERY_SLOTS);
        put(definitions, ModifierCapability.HAS_BURST_TRANSFER, BURST_TRANSFER, BURST_DURATION);
        put(definitions, ModifierCapability.HAS_IDLE_LOSS, IDLE_LOSS);
        put(definitions, ModifierCapability.HAS_GLOBAL_MODIFIER_EFFECTS, GLOBAL_MODIFIER_STRENGTH);
        put(definitions, ModifierCapability.HAS_STABILITY, STABILITY);
        put(definitions, ModifierCapability.HAS_FLUID_STORAGE, FLUID_CAPACITY_ADD, FLUID_CAPACITY);
        put(definitions, ModifierCapability.HAS_FLUID_INPUT, FLUID_TRANSFER);
        put(definitions, ModifierCapability.HAS_FLUID_OUTPUT, FLUID_TRANSFER);
        put(
                definitions,
                ModifierCapability.HAS_CALIBRATION,
                CALIBRATION_QUALITY,
                CALIBRATION_PRECISION,
                REFINEMENT_POTENTIAL_BONUS
        );
        put(
                definitions,
                ModifierCapability.HAS_CATALYST_CONTROL,
                CATALYST_EFFICIENCY
        );
        put(definitions, ModifierCapability.HAS_DURABILITY, DURABILITY_ADD, DURABILITY_PERCENT);
        put(
                definitions,
                ModifierCapability.HAS_FIELD_TOOL,
                TOOL_SELF_REPAIR,
                TOOL_MINING_SPEED,
                TOOL_ATTACK_SPEED,
                TOOL_FE_USAGE,
                TOOL_FE_TRANSFER,
                TOOL_STABILITY,
                TOOL_CONTROL,
                TOOL_BATTERY_SUPPORT,
                TOOL_LUCK,
                TOOL_TREE_FELL_LIMIT,
                TOOL_VEIN_MINER,
                TOOL_ORE_BURST_SPEED
        );
        put(
                definitions,
                ModifierCapability.HAS_SOLAR_ARRAY,
                SOLAR_PANEL_LIMIT,
                MOONLIGHT_CONVERSION,
                CLOUD_PIERCER,
                PANEL_SYNCHRONIZER,
                PANEL_ARBITRATION,
                CLEAR_SKY_AMPLIFIER,
                LUNAR_INVERTER
        );
        put(definitions, ModifierCapability.HAS_SOLAR_PANEL, PEAK_SOLAR);
        put(
                definitions,
                ModifierCapability.HAS_CRUSHER_SPECIALS,
                CRUSHER_FRAME,
                CRUSHER_THROUGHPUT,
                CRUSHER_BATTERY_LINK,
                CRUSHER_FEED_CONTROL,
                CRUSHER_COMPRESSION,
                CRUSHER_SALVAGE,
                CRUSH_HEAD_SCUFFED,
                CRUSH_HEAD_DUST_GROOVE
        );
        put(
                definitions,
                ModifierCapability.HAS_MINERS_COMPANION,
                MINERS_COMPANION_FILTER_SLOTS,
                ENERGY_USAGE
        );
        put(
                definitions,
                ModifierCapability.HAS_GEAR_STATS,
                CARROT_POWER,
                BREAD_POWER,
                SAPLING_POWER,
                SEED_POWER,
                POTATO_POWER,
                PLANT_POWER,
                ORGANIC_REAGENT_POWER,
                COMPOSTED_BIOMASS_POWER,
                ALGAE_POWER,
                RICH_BIOMASS_POWER
        );
        return Map.copyOf(definitions);
    }

    private static ModifierDefinition definitionForStat(MachineStat stat) {
        return switch (stat) {
            case ENERGY_CAPACITY_FLAT -> MACHINE_ENERGY_CAPACITY_ADD;
            case ENERGY_CAPACITY -> ENERGY_CAPACITY;
            case PROCESSING_SPEED -> PROCESSING_SPEED;
            case EFFICIENCY -> EFFICIENCY;
            case ENERGY_USAGE -> ENERGY_USAGE;
            case OUTPUT_AMOUNT -> OUTPUT_AMOUNT;
            case BATCH_SIZE -> CRUSHER_THROUGHPUT;
            case HEAT_TRANSFER -> HEAT_TRANSFER;
            case MAX_TEMPERATURE -> MAX_TEMPERATURE;
            case HEAT_ISOLATION -> HEAT_ISOLATION;
            case WARMUP_TIME -> WARMUP_TIME;
            case COOLING_RATE -> COOLING_RATE;
            case INPUT_SLOTS -> FUEL_SLOTS;
            case BATTERY_SLOTS -> BATTERY_CHASSIS_BATTERY_SLOTS;
            case ENERGY_GENERATION -> ENERGY_GENERATION;
            case ENERGY_TRANSFER -> ENERGY_TRANSFER;
            case FLUID_TRANSFER -> FLUID_TRANSFER;
            case FLUID_CAPACITY -> FLUID_CAPACITY;
            case FUEL_EFFICIENCY -> FUEL_EFFICIENCY;
            case STABILITY -> STABILITY;
            case TEMPERATURE_STABILITY -> TEMPERATURE_STABILITY;
            case OVERHEAT_TOLERANCE -> OVERHEAT_TOLERANCE;
            case BURST_TRANSFER -> BURST_TRANSFER;
            case BURST_DURATION -> BURST_DURATION;
            case IDLE_LOSS -> IDLE_LOSS;
            case GLOBAL_MODIFIER_STRENGTH -> GLOBAL_MODIFIER_STRENGTH;
            case CALIBRATION_QUALITY -> CALIBRATION_QUALITY;
            case CALIBRATION_PRECISION -> CALIBRATION_PRECISION;
            case CATALYST_EFFICIENCY -> CATALYST_EFFICIENCY;
            case REFINEMENT_POTENTIAL_BONUS -> REFINEMENT_POTENTIAL_BONUS;
            case DURABILITY -> DURABILITY_PERCENT;
            case SELF_REPAIR -> TOOL_SELF_REPAIR;
            case MINING_SPEED -> TOOL_MINING_SPEED;
            case ATTACK_SPEED -> TOOL_ATTACK_SPEED;
            case FE_USAGE -> TOOL_FE_USAGE;
            case FE_TRANSFER -> TOOL_FE_TRANSFER;
            case CONTROL -> TOOL_CONTROL;
            case ORE_BURST_SPEED -> TOOL_ORE_BURST_SPEED;
            case BATTERY_SUPPORT -> TOOL_BATTERY_SUPPORT;
            case LUCK -> TOOL_LUCK;
            case TREE_FELL_LIMIT -> TOOL_TREE_FELL_LIMIT;
            case VEIN_MINE_LIMIT, VEIN_MINE_FE_USAGE -> TOOL_VEIN_MINER;
            case POTATO_POWER -> POTATO_POWER;
            case CARROT_POWER -> CARROT_POWER;
            case BREAD_POWER -> BREAD_POWER;
            case SAPLING_POWER -> SAPLING_POWER;
            case SEED_POWER -> SEED_POWER;
            case PLANT_POWER -> PLANT_POWER;
            case ORGANIC_REAGENT_POWER -> ORGANIC_REAGENT_POWER;
            case COMPOSTED_BIOMASS_POWER -> COMPOSTED_BIOMASS_POWER;
            case ALGAE_POWER -> ALGAE_POWER;
            case RICH_BIOMASS_POWER -> RICH_BIOMASS_POWER;
            case FUEL_DURATION -> FUEL_DURATION;
            case SOLAR_PANEL_LIMIT -> SOLAR_PANEL_LIMIT;
            case MOONLIGHT_CONVERSION -> MOONLIGHT_CONVERSION;
            case WEATHER_RECOVERY -> CLOUD_PIERCER;
            case SOLAR_PANEL_SYNCHRONIZATION -> PANEL_SYNCHRONIZER;
            case SOLAR_PANEL_ARBITRATION -> PANEL_ARBITRATION;
            case OVERFLOW_SHUNTING -> OVERFLOW_SHUNTING;
            case CLEAR_SKY_AMPLIFICATION -> CLEAR_SKY_AMPLIFIER;
            case LUNAR_INVERSION -> LUNAR_INVERTER;
            case PEAK_SOLAR_GENERATION -> PEAK_SOLAR;
            case INSTANT_PROCESS_CHANCE -> INSTANT_PROCESS;
            case SUPER_OUTPUT_CHANCE -> SUPER_OUTPUT;
            case OUTPUT_GUARD_GRACE -> CRUSHER_FRAME;
            case NO_BATTERY_OUTPUT_RETENTION -> CRUSHER_BATTERY_LINK;
            case HIGH_HARDNESS_ENERGY_MITIGATION -> CRUSHER_COMPRESSION;
            case CRUSHER_INPUT_FILTER -> CRUSHER_FEED_CONTROL;
            case CRUSHER_SALVAGE_CHANCE -> CRUSHER_SALVAGE;
            case BLOCK_FILTER_SLOTS -> MINERS_COMPANION_FILTER_SLOTS;
            default -> throw new IllegalArgumentException("No rollable modifier definition for stat " + stat);
        };
    }

    private static List<ModifierDefinition> solarPanelAffixes() {
        LinkedHashSet<ModifierDefinition> definitions = new LinkedHashSet<>(affixes(
                MACHINE_ENERGY_CAPACITY_ADD,
                MachineStat.ENERGY_CAPACITY,
                MachineStat.EFFICIENCY,
                MachineStat.ENERGY_GENERATION,
                MachineStat.ENERGY_TRANSFER
        ));
        definitions.addAll(List.of(PEAK_SOLAR, SOLAR_HORIZON_CATCHER));
        return List.copyOf(definitions);
    }

    private static List<ModifierDefinition> batteryChassisAffixes() {
        LinkedHashSet<ModifierDefinition> definitions = new LinkedHashSet<>(affixes(
                List.of(BATTERY_CHASSIS_CHARGED_STORAGE, BATTERY_CHASSIS_BALANCE_MODE, BATTERY_CHASSIS_BATTERY_SLOTS),
                MachineStat.ENERGY_CAPACITY,
                MachineStat.EFFICIENCY,
                MachineStat.ENERGY_TRANSFER,
                MachineStat.BURST_TRANSFER,
                MachineStat.BURST_DURATION,
                MachineStat.STABILITY,
                MachineStat.IDLE_LOSS,
                MachineStat.GLOBAL_MODIFIER_STRENGTH
        ));
        return List.copyOf(definitions);
    }

    private static List<ModifierDefinition> solarArrayControllerAffixes() {
        LinkedHashSet<ModifierDefinition> definitions = new LinkedHashSet<>(affixes(
                MACHINE_ENERGY_CAPACITY_ADD,
                MachineStat.ENERGY_CAPACITY,
                MachineStat.EFFICIENCY,
                MachineStat.ENERGY_GENERATION,
                MachineStat.STABILITY
        ));
        definitions.addAll(List.of(
                SOLAR_PANEL_LIMIT,
                MOONLIGHT_CONVERSION,
                CLOUD_PIERCER,
                PANEL_SYNCHRONIZER,
                PANEL_ARBITRATION,
                CLEAR_SKY_AMPLIFIER,
                LUNAR_INVERTER
        ));
        return List.copyOf(definitions);
    }

    private static void put(
            EnumMap<ModifierCapability, List<ModifierDefinition>> definitions,
            ModifierCapability capability,
            ModifierDefinition... modifierDefinitions
    ) {
        definitions.put(capability, List.of(modifierDefinitions));
    }

    private static ModifierDefinition percent(ModifierSlot slot, MachineStat stat) {
        return ModifierDefinition.rollable(slot, stat, operationFor(stat), PERCENT_TIER_RANGES);
    }

    private static ModifierDefinition reducedPercent(ModifierSlot slot, MachineStat stat) {
        return ModifierDefinition.rollable(slot, stat, ModifierOperation.DECREASED_PERCENT, PERCENT_TIER_RANGES);
    }

    private static ModifierOperation operationFor(MachineStat stat) {
        return stat == MachineStat.ENERGY_USAGE
                || stat == MachineStat.FE_USAGE
                || stat == MachineStat.ORE_BURST_FE_USAGE
                || stat == MachineStat.IDLE_LOSS
                || stat == MachineStat.WARMUP_TIME
                || stat == MachineStat.COOLING_RATE
                ? ModifierOperation.DECREASED_PERCENT
                : ModifierOperation.INCREASED_PERCENT;
    }

    private static ModifierValueRange tierRange(List<ModifierValueRange> ranges, int tier) {
        int index = Math.max(1, Math.min(tier, ranges.size())) - 1;
        return ranges.get(index);
    }

    private ModifierEligibilityProfiles() {
    }
}
