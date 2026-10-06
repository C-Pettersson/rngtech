package com.rngtech.rpg;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public enum ModifierLensTag {
    POWER,
    SPEED,
    YIELD,
    STABILITY,
    CONTROL,
    KINETIC,
    EFFICIENCY;

    private final String serializedName;

    ModifierLensTag() {
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public String serializedName() {
        return serializedName;
    }

    public String tooltipKey() {
        return "rngtech.tooltip.modifier_lens." + serializedName;
    }

    public static Set<ModifierLensTag> infer(MachineStat stat, List<ModifierEffectDefinition> effects) {
        EnumSet<ModifierLensTag> tags = EnumSet.noneOf(ModifierLensTag.class);
        if (effects.isEmpty()) {
            addStatTags(tags, stat);
        } else {
            effects.stream().map(ModifierEffectDefinition::stat).forEach(effectStat -> addStatTags(tags, effectStat));
        }
        return tags;
    }

    private static void addStatTags(EnumSet<ModifierLensTag> tags, MachineStat stat) {
        switch (stat) {
            case ENERGY_CAPACITY,
                    ENERGY_CAPACITY_FLAT,
                    ENERGY_GENERATION,
                    ENERGY_TRANSFER,
                    FE_TRANSFER,
                    BATTERY_SUPPORT,
                    BURST_TRANSFER,
                    BURST_DURATION,
                    POTATO_POWER,
                    CARROT_POWER,
                    BREAD_POWER,
                    SAPLING_POWER,
                    SEED_POWER,
                    PLANT_POWER,
                    ORGANIC_REAGENT_POWER,
                    COMPOSTED_BIOMASS_POWER,
                    ALGAE_POWER,
                    RICH_BIOMASS_POWER,
                    PEAK_SOLAR_GENERATION -> tags.add(POWER);
            case PROCESSING_SPEED,
                    INSTANT_PROCESS_CHANCE,
                    MINING_SPEED,
                    ATTACK_SPEED,
                    ORE_BURST_SPEED,
                    WARMUP_TIME,
                    COOLING_RATE -> tags.add(SPEED);
            case OUTPUT_AMOUNT,
                    SUPER_OUTPUT_CHANCE,
                    MAGIC_FIND,
                    LUCK,
                    REFINEMENT_POTENTIAL,
                    REFINEMENT_POTENTIAL_BONUS,
                    CRUSHER_SALVAGE_CHANCE -> tags.add(YIELD);
            case STABILITY,
                    TEMPERATURE_STABILITY,
                    OVERHEAT_TOLERANCE,
                    DURABILITY,
                    SELF_REPAIR,
                    HEAT_ISOLATION,
                    IDLE_LOSS,
                    HIGH_HARDNESS_ENERGY_MITIGATION -> tags.add(STABILITY);
            case INPUT_SLOTS,
                    OUTPUT_SLOTS,
                    ADDON_SLOTS,
                    UPGRADE_LIMIT,
                    BUFFER_SIZE,
                    FLUID_CAPACITY,
                    COMPRESSION_RATIO,
                    FLUID_TRANSFER,
                    CALIBRATION_QUALITY,
                    CALIBRATION_PRECISION,
                    CATALYST_EFFICIENCY,
                    AREA_WIDTH,
                    AREA_HEIGHT,
                    TREE_FELL_LIMIT,
                    VEIN_MINE_LIMIT,
                    BLOCK_FILTER_SLOTS,
                    CONTROL,
                    BATTERY_SLOTS,
                    SOLAR_PANEL_LIMIT,
                    MOONLIGHT_CONVERSION,
                    WEATHER_RECOVERY,
                    SOLAR_PANEL_SYNCHRONIZATION,
                    SOLAR_PANEL_ARBITRATION,
                    OVERFLOW_SHUNTING,
                    CLEAR_SKY_AMPLIFICATION,
                    LUNAR_INVERSION,
                    OUTPUT_GUARD_GRACE,
                    NO_BATTERY_OUTPUT_RETENTION,
                    CRUSHER_INPUT_FILTER -> tags.add(CONTROL);
            case PROCESSING_LEVEL,
                    ENERGY_USAGE,
                    MAX_TEMPERATURE,
                    HEAT_TRANSFER,
                    FUEL_EFFICIENCY,
                    GLOBAL_MODIFIER_STRENGTH,
                    FE_USAGE,
                    MINING_LEVEL,
                    VEIN_MINE_FE_USAGE,
                    ORE_BURST_DURATION,
                    ORE_BURST_FE_USAGE,
                    ORE_BURST_COOLDOWN,
                    FUEL_DURATION -> {
            }
        }

        switch (stat) {
            case PROCESSING_SPEED,
                    INSTANT_PROCESS_CHANCE,
                    BATCH_SIZE,
                    MINING_SPEED,
                    ATTACK_SPEED,
                    ORE_BURST_SPEED -> tags.add(KINETIC);
            default -> {
            }
        }

        switch (stat) {
            case EFFICIENCY,
                    ENERGY_USAGE,
                    FUEL_EFFICIENCY,
                    IDLE_LOSS,
                    HIGH_HARDNESS_ENERGY_MITIGATION,
                    NO_BATTERY_OUTPUT_RETENTION,
                    DURABILITY,
                    SELF_REPAIR,
                    FE_USAGE,
                    VEIN_MINE_FE_USAGE,
                    CONTROL -> tags.add(EFFICIENCY);
            default -> {
            }
        }

        switch (stat) {
            case ENERGY_USAGE, FE_USAGE, VEIN_MINE_FE_USAGE, ORE_BURST_FE_USAGE, FUEL_EFFICIENCY, EFFICIENCY -> {
                tags.add(CONTROL);
                tags.add(EFFICIENCY);
            }
            case MAX_TEMPERATURE, HEAT_TRANSFER, FUEL_DURATION -> tags.add(POWER);
            case PROCESSING_LEVEL, MINING_LEVEL, BATCH_SIZE -> tags.add(SPEED);
            case GLOBAL_MODIFIER_STRENGTH -> {
                tags.add(POWER);
                tags.add(YIELD);
            }
            default -> {
            }
        }
    }
}
