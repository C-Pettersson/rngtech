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

    /** The highest stage any machine of this family reaches, as its ascendancy entry stage reports it. */
    public int topStage() {
        return switch (this) {
            case CRUSHER, FURNACE -> 8;
            case RESONANCE_CALIBRATOR -> 7;
            case ALLOY_FURNACE, MELTER -> 6;
            case METAL_PRESS, FORESTRY -> 4;
        };
    }

    public String translationKey() {
        return "rngtech.mastery.family." + name().toLowerCase(Locale.ROOT);
    }

    public boolean has(MachineTag tag) {
        return switch (tag) {
            case HEATED -> heatChassis();
            case SMELTING -> this == FURNACE;
            case CRUSHING -> this == CRUSHER;
            case BONUS_OUTPUT -> supports(MachineStat.OUTPUT_AMOUNT) || supports(MachineStat.SUPER_OUTPUT_CHANCE);
            case ENERGY_BUFFER -> supports(MachineStat.ENERGY_CAPACITY);
        };
    }

    public boolean supportsBehavior(String behavior) {
        return switch (behavior) {
            case "NO_INHERENT_ATTRIBUTES", "NO_INHERENT_CONTROL", "NO_INHERENT_DRIVE", "NO_INHERENT_RESERVE", "MUTE_MACHINE_SOUND" -> true;
            case "NO_BONUS_OUTPUT" -> has(MachineTag.BONUS_OUTPUT);
            case "SURGE_CYCLING" -> supports(MachineStat.INSTANT_PROCESS_CHANCE);
            case "BLOCK_BATTERY", "MATCHING_HEAD" -> this == CRUSHER;
            case "QUENCH_PROTOCOL" -> heatChassis();
            case "CLOSED_LOOP_RECUPERATOR" -> this == FURNACE;
            case "MAGNET_MODE", "SERRATED_LEAF_PROTOCOL", "MANUAL_THROTTLE", "COASTING_CLUTCH", "SEEDLING_MAGNET" -> this == FORESTRY;
            default -> MasteryDeclarations.families(behavior).contains(this);
        };
    }

    public boolean supports(PassiveStatType stat) {
        return switch (stat) {
            case COMPONENT_STAGE_SUPPORT -> this == CRUSHER;
            case MANAGED_CELLS -> this == FORESTRY;
        };
    }

    /**
     * Whether this family's machines read {@code stat}. A display-only stat does not count: the keystone and theme audits
     * rely on this to find payoffs that do nothing.
     */
    public boolean supports(MachineStat stat) {
        if (stat == MachineStat.CONTROL || stat == MachineStat.DRIVE || stat == MachineStat.RESERVE) {
            return true;
        }
        return switch (stat) {
            case PROCESSING_SPEED, ENERGY_USAGE -> true;
            // The Forestry Companion has no FE buffer and no instant jobs.
            case ENERGY_CAPACITY, INSTANT_PROCESS_CHANCE -> this != FORESTRY;
            // The Resonance Calibrator's calibration floor and the Metal Press servo read Stability; elsewhere it is display only.
            case STABILITY -> this == RESONANCE_CALIBRATOR || this == METAL_PRESS;
            case SUPER_OUTPUT_CHANCE -> this != FORESTRY && this != MELTER;
            case ENERGY_CAPACITY_FLAT -> this == CRUSHER || this == FURNACE;
            // Only the Crusher reads Output Amount; other machines' bonus output is Super Output.
            case OUTPUT_AMOUNT -> this == CRUSHER;
            case BATCH_SIZE -> this != FORESTRY;
            // The Melter reads its Heat Core's maximum temperature, but none of the warmup and cooling stats.
            case MAX_TEMPERATURE -> heatChassis() || this == MELTER;
            case HEAT_TRANSFER, HEAT_ISOLATION, TEMPERATURE_STABILITY, WARMUP_TIME, COOLING_RATE, OVERHEAT_TOLERANCE -> heatChassis();
            case FUEL_DURATION, FUEL_EFFICIENCY -> this == FURNACE;
            case FLUID_TRANSFER -> this == MELTER;
            case CALIBRATION_PRECISION, CATALYST_EFFICIENCY -> this == RESONANCE_CALIBRATOR;
            case PROCESSING_LEVEL, OUTPUT_GUARD_GRACE, NO_BATTERY_OUTPUT_RETENTION,
                    HIGH_HARDNESS_ENERGY_MITIGATION, CRUSHER_INPUT_FILTER, CRUSHER_SALVAGE_CHANCE -> this == CRUSHER;
            case TREE_FELL_LIMIT -> this == FORESTRY;
            default -> MasteryDeclarations.families(stat).contains(this);
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
