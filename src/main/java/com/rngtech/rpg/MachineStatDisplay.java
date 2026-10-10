package com.rngtech.rpg;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MachineStatDisplay {
    private static final double EPSILON = 0.0001;

    public static String statValue(MachineStat stat, double value) {
        return switch (stat) {
            case INPUT_SLOTS, OUTPUT_SLOTS, ADDON_SLOTS, BLOCK_FILTER_SLOTS -> formatUnit(value, "slot", "slots");
            case CONTROL, DRIVE, RESERVE -> formatUnit(value, "point", "points");
            case BATTERY_SLOTS -> formatUnit(value, "cell", "cells");
            case CRUSHER_INPUT_FILTER -> formatUnit(value, "filter", "filters");
            case BATCH_SIZE -> formatUnit(value, "item", "items");
            case BATCH_OVERHEAD -> formatNumber(value) + "% per extra item";
            case ENERGY_CAPACITY, ENERGY_CAPACITY_FLAT, BUFFER_SIZE -> formatNumber(value) + " FE";
            case ENERGY_GENERATION -> formatNumber(value) + " FE/t";
            case ENERGY_TRANSFER, BURST_TRANSFER, FE_TRANSFER -> formatNumber(value) + " FE/t";
            case ENERGY_USAGE, FE_USAGE -> formatMultiplierDelta(value) + " FE/craft";
            case FLUID_CAPACITY -> formatNumber(value) + " mB";
            case FLUID_TRANSFER -> formatNumber(value) + " mB/t";
            case COMPRESSION_RATIO -> formatNumber(value) + ":1";
            case MAX_TEMPERATURE -> formatNumber(value) + " heat";
            case BURST_DURATION, ORE_BURST_DURATION, ORE_BURST_COOLDOWN -> formatUnit(value, "tick", "ticks");
            case DURABILITY -> formatNumber(value) + " durability";
            case SELF_REPAIR -> formatNumber(value) + " durability";
            case AREA_WIDTH, AREA_HEIGHT, TREE_FELL_LIMIT, VEIN_MINE_LIMIT -> formatUnit(value, "block", "blocks");
            case VEIN_MINE_FE_USAGE, ORE_BURST_FE_USAGE -> formatNumber(value) + " FE";
            case IDLE_LOSS -> formatNumber(value) + "%/min";
            case SUPER_OUTPUT_CHANCE, INSTANT_PROCESS_CHANCE, CRUSHER_SALVAGE_CHANCE -> formatSignedPercentPoints(value) + " chance";
            case MOONLIGHT_CONVERSION,
                    WEATHER_RECOVERY,
                    SOLAR_PANEL_SYNCHRONIZATION,
                    SOLAR_PANEL_ARBITRATION,
                    OVERFLOW_SHUNTING,
                    CLEAR_SKY_AMPLIFICATION,
                    LUNAR_INVERSION,
                    NO_BATTERY_OUTPUT_RETENTION,
                    HIGH_HARDNESS_ENERGY_MITIGATION -> formatSignedPercentPoints(value);
            case OUTPUT_GUARD_GRACE -> formatSignedNumber(value) + " ticks";
            case REFINEMENT_POTENTIAL_BONUS -> formatSignedNumber(value) + " RP";
            case OUTPUT_AMOUNT,
                    PROCESSING_SPEED,
                    EFFICIENCY,
                    STABILITY,
                    HEAT_TRANSFER,
                    HEAT_ISOLATION,
                    TEMPERATURE_STABILITY,
                    FUEL_EFFICIENCY,
                    WARMUP_TIME,
                    COOLING_RATE,
                    OVERHEAT_TOLERANCE,
                    GLOBAL_MODIFIER_STRENGTH,
                    CALIBRATION_QUALITY,
                    CALIBRATION_PRECISION,
                    CATALYST_EFFICIENCY,
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
                    FUEL_DURATION,
                    PEAK_SOLAR_GENERATION,
                    MINING_SPEED,
                    ATTACK_SPEED,
                    ORE_BURST_SPEED -> formatMultiplierDelta(value);
            case BATTERY_SUPPORT -> "stage " + formatNumber(value);
            case SOLAR_PANEL_LIMIT -> formatUnit(value, "panel", "panels");
            case HARDNESS_TOLERANCE -> formatUnit(value, "level", "levels");
            case BANK_MEMORY -> formatUnit(value, "input", "inputs");
            case JAM_CHANCE -> formatMultiplierDelta(value);
            case JAM_RECOVERY, UNDER_LEVEL_EFFICIENCY, AT_LEVEL_OUTPUT, OVERDRIVE_CAP, LEDGER_RATE, FLUX_RATE, BLEND_SPEED -> formatSignedPercentPoints(value);
            case CYCLE_JAM_CHANCE -> formatSignedPercentPoints(value) + " chance";
            case ESCAPEMENT_SPEED -> formatSignedPercentPoints(value);
            case BLEND_HEAT_REDUCTION -> formatNumber(value) + " \u00b0C";
            case MOLD_SWAP_TIME -> formatUnit(value, "tick", "ticks");
            case HEAT_WINDOW -> formatSignedPercentPoints(value);
            case STREAK_FLOOR, STREAK_CAP -> formatNumber(value) + " stability";
            case COIL_REACH -> formatUnit(value, "stage", "stages");
            case FLUID_YIELD, OVERLEVEL_SPEED -> formatSignedPercentPoints(value);
            case CART_SPEED -> formatMultiplierDelta(value);
            case GROWTH_PULSE -> formatNumber(value) + " bone meal";
            case WORK_RANGE -> formatUnit(value, "row", "rows");
            case IDLE_CART_SPEED -> formatSignedPercentPoints(value);
            case SUPER_OUTPUT_CADENCE -> value < 1.0 ? "off" : "every " + formatUnit(value, "cycle", "cycles");
            case OVERDRIVE_SPEED -> formatSignedPercentPoints(value) + " per 10 \u00b0C";
            case OVERDRIVE_MARGIN -> formatNumber(value) + " \u00b0C";
            case STRAIN_RECOVERY -> formatNumber(value) + " per tick";
            default -> formatNumber(value);
        };
    }

    public static MutableComponent statTooltip(MachineStat stat, double value) {
        return Component.translatable(stat.translationKey() + ".description", statValue(stat, value));
    }

    public static String energyRate(double perTick) {
        return formatNumber(Math.round(perTick)) + " FE/t";
    }

    /**
     * Where a generator's final FE/t comes from: {@code finalPerTick = (basePerTick * multiplier + flatPerTick) * other},
     * where {@code other} folds in speed, efficiency, stability, and machine-specific effects. An unknown base omits the
     * base and other lines.
     */
    public static List<Component> generationBreakdown(double finalPerTick, double basePerTick, double multiplier, double flatPerTick) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("rngtech.stat.energy_generation.breakdown.final", energyRate(finalPerTick)));
        if (basePerTick > EPSILON) {
            lines.add(Component.translatable("rngtech.stat.energy_generation.breakdown.base", energyRate(basePerTick)));
        }
        lines.add(Component.translatable("rngtech.stat.energy_generation.breakdown.multiplier", formatMultiplierDelta(multiplier)));
        if (Math.abs(flatPerTick) > EPSILON) {
            lines.add(Component.translatable(
                    "rngtech.stat.energy_generation.breakdown.flat",
                    formatSignedNumber(Math.round(flatPerTick)) + " FE/t"
            ));
        }
        double other = generationOtherFactor(finalPerTick, basePerTick, multiplier, flatPerTick);
        if (Math.abs(other - 1.0) >= 0.005) {
            lines.add(Component.translatable("rngtech.stat.energy_generation.breakdown.other", formatMultiplierDelta(other)));
        }
        return lines;
    }

    /** The share of final FE/t not explained by base, multiplier, and flat generation; 1 when the base is unknown. */
    public static double generationOtherFactor(double finalPerTick, double basePerTick, double multiplier, double flatPerTick) {
        double beforeOther = basePerTick * multiplier + flatPerTick;
        if (basePerTick <= EPSILON || finalPerTick <= EPSILON || beforeOther <= EPSILON) {
            return 1.0;
        }
        return finalPerTick / beforeOther;
    }

    public static MutableComponent effectText(MachineModifierEffect effect) {
        double amount = switch (effect.operation()) {
            case MORE -> (effect.value() - 1.0) * 100.0;
            case LESS -> (1.0 - effect.value()) * 100.0;
            default -> effect.value();
        };
        return Component.translatable("rngtech.stat.keyword." + effect.operation().name().toLowerCase(Locale.ROOT),
                effect.operation() == ModifierOperation.ADD ? formatAdditiveEffectValue(effect.stat(), amount) : formatNumber(amount), Component.translatable(effect.stat().translationKey()));
    }

    public static String effectValue(MachineModifierEffect effect) {
        return switch (effect.operation()) {
            case ADD -> formatAdditiveEffectValue(effect.stat(), effect.value());
            case INCREASED_PERCENT -> formatSignedPercentPoints(effect.value());
            case DECREASED_PERCENT -> formatSignedPercentPoints(-effect.value());
            case MORE -> formatMultiplierDelta(effect.value());
            case LESS -> formatMultiplierDelta(effect.value());
        };
    }

    public static String rangeValue(MachineModifierEffect effect) {
        ModifierValueRange range = effect.range();
        if (range.min() == range.max()) {
            return effectValue(new MachineModifierEffect(effect.stat(), effect.operation(), range, range.min()));
        }
        return effectValue(new MachineModifierEffect(effect.stat(), effect.operation(), range, range.min()))
                + " to "
                + effectValue(new MachineModifierEffect(effect.stat(), effect.operation(), range, range.max()));
    }

    public static String formatNumber(double value) {
        if (Math.abs(value - Math.rint(value)) <= EPSILON) {
            return String.format(Locale.ROOT, "%,d", Math.round(value));
        }
        String formatted = String.format(Locale.ROOT, "%,.2f", value);
        while (formatted.endsWith("0")) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }
        if (formatted.endsWith(".")) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }
        return formatted;
    }

    static String formatAdditiveEffectValue(MachineStat stat, double value) {
        return switch (stat) {
            case ENERGY_CAPACITY, ENERGY_CAPACITY_FLAT, BUFFER_SIZE -> formatSignedNumber(value) + " FE";
            case ENERGY_GENERATION -> formatSignedNumber(value) + " FE/t";
            case ENERGY_TRANSFER, BURST_TRANSFER, FE_TRANSFER -> formatSignedNumber(value) + " FE/t";
            case INPUT_SLOTS, OUTPUT_SLOTS, ADDON_SLOTS, BLOCK_FILTER_SLOTS -> formatSignedUnit(value, "slot", "slots");
            case CONTROL, DRIVE, RESERVE -> formatSignedUnit(value, "point", "points");
            case BATTERY_SLOTS -> formatSignedUnit(value, "cell", "cells");
            case CRUSHER_INPUT_FILTER -> formatSignedUnit(value, "filter", "filters");
            case BATCH_SIZE -> formatSignedUnit(value, "item", "items");
            case BATCH_OVERHEAD -> formatSignedPercentPoints(value) + " per extra item";
            case FLUID_CAPACITY -> formatSignedNumber(value) + " mB";
            case FLUID_TRANSFER -> formatSignedNumber(value) + " mB/t";
            case MAX_TEMPERATURE -> formatSignedNumber(value) + " heat";
            case BURST_DURATION, ORE_BURST_DURATION, ORE_BURST_COOLDOWN -> formatSignedUnit(value, "tick", "ticks");
            case DURABILITY, SELF_REPAIR -> formatSignedNumber(value) + " durability";
            case AREA_WIDTH, AREA_HEIGHT, TREE_FELL_LIMIT, VEIN_MINE_LIMIT -> formatSignedUnit(value, "block", "blocks");
            case VEIN_MINE_FE_USAGE, ORE_BURST_FE_USAGE -> formatSignedNumber(value) + " FE";
            case SUPER_OUTPUT_CHANCE,
                    INSTANT_PROCESS_CHANCE,
                    CRUSHER_SALVAGE_CHANCE,
                    MOONLIGHT_CONVERSION,
                    WEATHER_RECOVERY,
                    SOLAR_PANEL_SYNCHRONIZATION,
                    SOLAR_PANEL_ARBITRATION,
                    OVERFLOW_SHUNTING,
                    CLEAR_SKY_AMPLIFICATION,
                    LUNAR_INVERSION,
                    PEAK_SOLAR_GENERATION,
                    NO_BATTERY_OUTPUT_RETENTION,
                    HIGH_HARDNESS_ENERGY_MITIGATION -> formatSignedPercentPoints(value);
            case OUTPUT_GUARD_GRACE -> formatSignedNumber(value) + " ticks";
            case REFINEMENT_POTENTIAL_BONUS -> formatSignedNumber(value) + " RP";
            case HARDNESS_TOLERANCE -> formatSignedUnit(value, "level", "levels");
            case BANK_MEMORY -> formatSignedUnit(value, "input", "inputs");
            case JAM_RECOVERY, UNDER_LEVEL_EFFICIENCY, AT_LEVEL_OUTPUT, OVERDRIVE_CAP, LEDGER_RATE, FLUX_RATE, BLEND_SPEED -> formatSignedPercentPoints(value);
            case CYCLE_JAM_CHANCE -> formatSignedPercentPoints(value) + " chance";
            case ESCAPEMENT_SPEED -> formatSignedPercentPoints(value);
            case BLEND_HEAT_REDUCTION -> formatSignedNumber(value) + " \u00b0C";
            case MOLD_SWAP_TIME -> formatSignedUnit(value, "tick", "ticks");
            case HEAT_WINDOW -> formatSignedPercentPoints(value);
            case STREAK_FLOOR, STREAK_CAP -> formatSignedNumber(value) + " stability";
            case COIL_REACH -> formatSignedUnit(value, "stage", "stages");
            case FLUID_YIELD, OVERLEVEL_SPEED -> formatSignedPercentPoints(value);
            case GROWTH_PULSE -> formatSignedNumber(value) + " bone meal";
            case WORK_RANGE -> formatSignedUnit(value, "row", "rows");
            case IDLE_CART_SPEED -> formatSignedPercentPoints(value);
            case SUPER_OUTPUT_CADENCE -> formatSignedUnit(value, "cycle", "cycles");
            case OVERDRIVE_SPEED -> formatSignedPercentPoints(value) + " per 10 \u00b0C";
            case OVERDRIVE_MARGIN -> formatSignedNumber(value) + " \u00b0C";
            case STRAIN_RECOVERY -> formatSignedNumber(value) + " per tick";
            default -> formatSignedNumber(value);
        };
    }

    private static String formatUnit(double value, String singular, String plural) {
        String unit = Math.abs(value - 1.0) <= EPSILON ? singular : plural;
        return formatNumber(value) + " " + unit;
    }

    /** The unit a flat {@code stat} value carries after its number, such as " °C" or " items", for range labels. */
    public static String additiveUnit(MachineStat stat, double magnitude) {
        String formatted = formatAdditiveEffectValue(stat, magnitude);
        String number = formatSignedNumber(magnitude);
        return formatted.startsWith(number) ? formatted.substring(number.length()) : "";
    }

    private static String formatSignedUnit(double value, String singular, String plural) {
        String unit = Math.abs(Math.abs(value) - 1.0) <= EPSILON ? singular : plural;
        return formatSignedNumber(value) + " " + unit;
    }

    private static String formatMultiplierDelta(double value) {
        return formatSignedPercentPoints(Math.round((value - 1.0) * 100.0));
    }

    static String formatSignedPercentPoints(double value) {
        return formatSignedNumber(value) + "%";
    }

    static String formatSignedNumber(double value) {
        if (Math.abs(value) <= EPSILON) {
            return formatNumber(0.0);
        }
        return value > 0.0 ? "+" + formatNumber(value) : formatNumber(value);
    }

    private static String effectTextKey(MachineStat stat) {
        return switch (stat) {
            case OUTPUT_AMOUNT -> "rngtech.tooltip.stat_effect.output_amount";
            case SUPER_OUTPUT_CHANCE -> "rngtech.tooltip.stat_effect.super_output_chance";
            case PROCESSING_SPEED -> "rngtech.tooltip.stat_effect.processing_speed";
            case INSTANT_PROCESS_CHANCE -> "rngtech.tooltip.stat_effect.instant_process_chance";
            case ENERGY_USAGE -> "rngtech.tooltip.stat_effect.energy_usage";
            case ENERGY_CAPACITY, ENERGY_CAPACITY_FLAT -> "rngtech.tooltip.stat_effect.energy_capacity";
            case ENERGY_GENERATION -> "rngtech.tooltip.stat_effect.energy_generation";
            case ENERGY_TRANSFER, FE_TRANSFER -> "rngtech.tooltip.stat_effect.energy_transfer";
            case EFFICIENCY -> "rngtech.tooltip.stat_effect.efficiency";
            case STABILITY -> "rngtech.tooltip.stat_effect.stability";
            case MAX_TEMPERATURE -> "rngtech.tooltip.stat_effect.max_temperature";
            case HEAT_TRANSFER -> "rngtech.tooltip.stat_effect.heat_transfer";
            case HEAT_ISOLATION -> "rngtech.tooltip.stat_effect.heat_isolation";
            case TEMPERATURE_STABILITY -> "rngtech.tooltip.stat_effect.temperature_stability";
            case FUEL_EFFICIENCY -> "rngtech.tooltip.stat_effect.fuel_efficiency";
            case WARMUP_TIME -> "rngtech.tooltip.stat_effect.warmup_time";
            case COOLING_RATE -> "rngtech.tooltip.stat_effect.cooling_rate";
            case OVERHEAT_TOLERANCE -> "rngtech.tooltip.stat_effect.overheat_tolerance";
            case REFINEMENT_POTENTIAL_BONUS -> "rngtech.tooltip.stat_effect.refinement_potential_bonus";
            case FLUID_TRANSFER -> "rngtech.tooltip.stat_effect.fluid_transfer";
            case CALIBRATION_QUALITY -> "rngtech.tooltip.stat_effect.calibration_quality";
            case CALIBRATION_PRECISION -> "rngtech.tooltip.stat_effect.calibration_precision";
            case CATALYST_EFFICIENCY -> "rngtech.tooltip.stat_effect.catalyst_efficiency";
            case POTATO_POWER,
                    CARROT_POWER,
                    BREAD_POWER,
                    SAPLING_POWER,
                    SEED_POWER,
                    PLANT_POWER,
                    ORGANIC_REAGENT_POWER,
                    COMPOSTED_BIOMASS_POWER,
                    ALGAE_POWER,
                    RICH_BIOMASS_POWER -> "rngtech.tooltip.stat_effect.bio_fuel_power";
            case FUEL_DURATION -> "rngtech.tooltip.stat_effect.fuel_duration";
            case OUTPUT_GUARD_GRACE -> "rngtech.tooltip.stat_effect.output_guard_grace";
            case NO_BATTERY_OUTPUT_RETENTION -> "rngtech.tooltip.stat_effect.no_battery_output_retention";
            case HIGH_HARDNESS_ENERGY_MITIGATION -> "rngtech.tooltip.stat_effect.high_hardness_energy_mitigation";
            case CRUSHER_INPUT_FILTER -> "rngtech.tooltip.stat_effect.crusher_input_filter";
            case CRUSHER_SALVAGE_CHANCE -> "rngtech.tooltip.stat_effect.crusher_salvage_chance";
            case MINING_SPEED -> "rngtech.tooltip.stat_effect.mining_speed";
            case ATTACK_SPEED -> "rngtech.tooltip.stat_effect.attack_speed";
            case DURABILITY -> "rngtech.tooltip.stat_effect.durability";
            case SELF_REPAIR -> "rngtech.tooltip.stat_effect.self_repair";
            case LUCK -> "rngtech.tooltip.stat_effect.luck";
            case CONTROL -> "rngtech.tooltip.stat_effect.control";
            case DRIVE -> "rngtech.tooltip.stat_effect.drive";
            case RESERVE -> "rngtech.tooltip.stat_effect.reserve";
            default -> "rngtech.tooltip.stat_effect.generic";
        };
    }

    private MachineStatDisplay() {
    }
}
