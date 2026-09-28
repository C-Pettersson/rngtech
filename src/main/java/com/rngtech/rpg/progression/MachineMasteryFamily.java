package com.rngtech.rpg.progression;

import com.rngtech.rpg.MachineStat;

public enum MachineMasteryFamily {
    CRUSHER("drive"), FURNACE("drive_reserve"), FORESTRY("control_drive");

    private final String start;

    MachineMasteryFamily(String start) {
        this.start = "start_" + start;
    }

    public String startNodeId() {
        return start;
    }

    public boolean supportsBehavior(String behavior) {
        return switch (behavior) {
            case "NO_INHERENT_ATTRIBUTES", "MUTE_MACHINE_SOUND" -> true;
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
            case PROCESSING_SPEED, ENERGY_USAGE, STABILITY -> true;
            case ENERGY_CAPACITY, ENERGY_CAPACITY_FLAT, OUTPUT_AMOUNT, SUPER_OUTPUT_CHANCE,
                    INSTANT_PROCESS_CHANCE, PARALLEL_JOBS -> this != FORESTRY;
            case MAX_TEMPERATURE, HEAT_TRANSFER, HEAT_ISOLATION, TEMPERATURE_STABILITY,
                    WARMUP_TIME, COOLING_RATE, OVERHEAT_TOLERANCE, FUEL_DURATION, FUEL_EFFICIENCY -> this == FURNACE;
            case PROCESSING_LEVEL, OUTPUT_GUARD_GRACE, NO_BATTERY_OUTPUT_RETENTION,
                    HIGH_HARDNESS_ENERGY_MITIGATION, CRUSHER_INPUT_FILTER, CRUSHER_SALVAGE_CHANCE -> this == CRUSHER;
            case TREE_FELL_LIMIT -> this == FORESTRY;
            default -> false;
        };
    }
}
