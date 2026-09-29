package com.rngtech.rpg.progression;

import com.rngtech.rpg.MachineStat;

import java.util.Locale;

public enum MachineMasteryFamily {
    CRUSHER("drive"), FURNACE("drive_reserve"), FORESTRY("control_drive"),
    ALLOY_FURNACE("drive_reserve"), METAL_PRESS("control"), MELTER("reserve_control"), RESONANCE_CALIBRATOR("control");

    private final String start;

    MachineMasteryFamily(String start) {
        this.start = "start_" + start;
    }

    public String startNodeId() {
        return start;
    }

    public String translationKey() {
        return "rngtech.mastery.family." + name().toLowerCase(Locale.ROOT);
    }

    public boolean has(MachineTag tag) {
        return switch (tag) {
            case HEATED -> heatChassis();
            case CRUSHING -> this == CRUSHER;
            case BONUS_OUTPUT -> supports(MachineStat.OUTPUT_AMOUNT) || supports(MachineStat.SUPER_OUTPUT_CHANCE);
            case ENERGY_BUFFER -> supports(MachineStat.ENERGY_CAPACITY);
        };
    }

    public boolean supportsBehavior(String behavior) {
        return switch (behavior) {
            case "NO_INHERENT_ATTRIBUTES", "MUTE_MACHINE_SOUND" -> true;
            case "NO_BONUS_OUTPUT" -> has(MachineTag.BONUS_OUTPUT);
            case "BLOCK_BATTERY", "MATCHING_HEAD", "DENSE_PARALLEL" -> this == CRUSHER;
            case "QUENCH_PROTOCOL", "CLOSED_LOOP_RECUPERATOR" -> this == FURNACE;
            case "MAGNET_MODE", "SERRATED_LEAF_PROTOCOL", "MANUAL_THROTTLE", "COASTING_CLUTCH", "SEEDLING_MAGNET" -> this == FORESTRY;
            default -> false;
        };
    }

    public boolean supports(PassiveStatType stat) {
        return switch (stat) {
            case COMPONENT_STAGE_SUPPORT -> this == CRUSHER;
            case MANAGED_CELLS -> this == FORESTRY;
        };
    }

    public boolean supports(MachineStat stat) {
        if (stat == MachineStat.CONTROL || stat == MachineStat.DRIVE || stat == MachineStat.RESERVE) {
            return true;
        }
        return switch (stat) {
            case PROCESSING_SPEED, ENERGY_USAGE -> true;
            case STABILITY -> this != MELTER;
            case ENERGY_CAPACITY, INSTANT_PROCESS_CHANCE -> this != FORESTRY;
            case SUPER_OUTPUT_CHANCE -> this != FORESTRY && this != MELTER;
            case ENERGY_CAPACITY_FLAT -> this == CRUSHER || this == FURNACE;
            // Only the Crusher reads Output Amount and Parallel Jobs; other machines' bonus output is Super Output.
            case OUTPUT_AMOUNT, PARALLEL_JOBS -> this == CRUSHER;
            case MAX_TEMPERATURE, HEAT_TRANSFER -> heatChassis() || this == MELTER;
            case HEAT_ISOLATION, TEMPERATURE_STABILITY, WARMUP_TIME, COOLING_RATE, OVERHEAT_TOLERANCE -> heatChassis();
            case FUEL_DURATION, FUEL_EFFICIENCY -> this == FURNACE;
            case FLUID_TRANSFER -> this == MELTER;
            case CALIBRATION_PRECISION, CATALYST_EFFICIENCY -> this == RESONANCE_CALIBRATOR;
            case PROCESSING_LEVEL, OUTPUT_GUARD_GRACE, NO_BATTERY_OUTPUT_RETENTION,
                    HIGH_HARDNESS_ENERGY_MITIGATION, CRUSHER_INPUT_FILTER, CRUSHER_SALVAGE_CHANCE -> this == CRUSHER;
            case TREE_FELL_LIMIT -> this == FORESTRY;
            default -> false;
        };
    }

    /** Fixed values and ceilings; the Melter keeps ordinary heat bonuses because every Melter recipe needs more heat than those constraints allow. */
    public boolean supportsAbsolute(MachineStat stat) {
        return supports(stat) && !(this == MELTER && stat == MachineStat.MAX_TEMPERATURE);
    }

    private boolean heatChassis() {
        return this == FURNACE || this == ALLOY_FURNACE || this == METAL_PRESS;
    }
}
