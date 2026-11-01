package com.rngtech.rpg;

public enum ModifierCapability {
    HAS_FE_STORAGE,
    HAS_FE_INPUT,
    HAS_FE_OUTPUT,
    HAS_FE_GENERATION,
    HAS_FUEL_SLOT,
    HAS_INPUT_SLOT,
    HAS_OUTPUT_SLOT,
    HAS_PROCESSING,
    HAS_HEAT,
    HAS_BATTERY_CELLS,
    HAS_BURST_TRANSFER,
    HAS_IDLE_LOSS,
    HAS_GLOBAL_MODIFIER_EFFECTS,
    HAS_STABILITY,
    HAS_GEAR_STATS,
    HAS_FLUID_STORAGE,
    HAS_FLUID_INPUT,
    HAS_FLUID_OUTPUT,
    HAS_CALIBRATION,
    HAS_CATALYST_CONTROL,
    HAS_DURABILITY,
    HAS_FIELD_TOOL,
    HAS_SOLAR_ARRAY,
    HAS_SOLAR_PANEL,
    HAS_CRUSHER_SPECIALS,
    HAS_MINERS_COMPANION,
    /** Gear and bodies of machines with Machine Mastery, which can roll Control, Drive, and Reserve. */
    HAS_MASTERY_ATTRIBUTES,
    HAS_PHASE_ARRAY,
    /** Recipe machines that work several items per cycle through Batch Size, other than the Crusher. */
    HAS_BATCHING,
    /** Machine bodies that light up while they hold charge or heat, which can roll Glow. */
    HAS_WORK_LIGHT,
    /** Machine bodies with a sound profile and no Machine Mastery, which can roll Discreet. */
    HAS_MACHINE_NOISE,
    /** Machine bodies with a comparator gauge to report, which can roll Telemetry. */
    HAS_TELEMETRY,
    /** Jewels, which roll the machine-agnostic jewel pool and nothing else. */
    HAS_JEWEL,
    /** Placed machine bodies with a screen, which can roll Bedazzling body sockets. */
    HAS_JEWEL_SOCKETS
}
