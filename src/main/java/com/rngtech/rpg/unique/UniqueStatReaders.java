package com.rngtech.rpg.unique;

import com.rngtech.rpg.MachineBehavior;
import com.rngtech.rpg.MachineStat;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Which machines accept each Unique host, and which stats and behaviors each machine reads from its installed Gear. The
 * catalog rejects a Unique stat that no accepting machine reads, and the Gear tab dims a stat its machine ignores. Update
 * this table when a machine starts or stops reading a stat; the machine list must match {@code GearSlotCatalog}.
 */
public final class UniqueStatReaders {
    public enum Reader {
        CRUSHER,
        MINERS_COMPANION,
        FURNACE,
        ALLOY_FURNACE,
        METAL_PRESS,
        MELTER,
        SOLID_FUEL_BURNER,
        CAVITATION_GENERATOR,
        GAS_CHEMISTRY,
        COMPRESSOR_TANK,
        CORROSION_CELL,
        FORESTRY_CART,
        RESONANCE_CALIBRATOR,
        /** A Battery Cell's own storage, in any machine that holds one. */
        BATTERY_CELL,
        BATTERY_CHASSIS
    }

    private static final Map<UniqueHost, List<Reader>> HOSTS = new EnumMap<>(Map.of(
            UniqueHost.CRUSH_HEAD, List.of(Reader.CRUSHER, Reader.MELTER, Reader.MINERS_COMPANION),
            UniqueHost.HEAT_CORE, List.of(Reader.FURNACE, Reader.ALLOY_FURNACE, Reader.METAL_PRESS, Reader.MELTER,
                    Reader.SOLID_FUEL_BURNER, Reader.CAVITATION_GENERATOR, Reader.GAS_CHEMISTRY),
            UniqueHost.ALLOY_CRUCIBLE, List.of(Reader.ALLOY_FURNACE),
            UniqueHost.SERVO, List.of(Reader.METAL_PRESS, Reader.ALLOY_FURNACE, Reader.MELTER, Reader.CAVITATION_GENERATOR,
                    Reader.GAS_CHEMISTRY, Reader.COMPRESSOR_TANK),
            UniqueHost.FLUID_PUMP, List.of(Reader.MELTER, Reader.CORROSION_CELL, Reader.FORESTRY_CART),
            UniqueHost.CONTROL_BOARD, List.of(Reader.RESONANCE_CALIBRATOR),
            UniqueHost.BATTERY_CELL, List.of(Reader.BATTERY_CELL, Reader.BATTERY_CHASSIS)
    ));

    private static final Set<MachineStat> HEAT_CONTROL = EnumSet.of(MachineStat.COOLING_RATE, MachineStat.HEAT_ISOLATION,
            MachineStat.HEAT_TRANSFER, MachineStat.OVERHEAT_TOLERANCE, MachineStat.TEMPERATURE_STABILITY, MachineStat.WARMUP_TIME,
            MachineStat.MAX_TEMPERATURE, MachineStat.STRAIN_RECOVERY);
    private static final Set<MachineStat> BATCHING = EnumSet.of(MachineStat.BATCH_SIZE, MachineStat.BATCH_OVERHEAD);

    private static final Map<Reader, Set<MachineStat>> STATS = new EnumMap<>(Reader.class);
    private static final Map<Reader, Set<MachineBehavior>> BEHAVIORS = new EnumMap<>(Reader.class);

    static {
        stats(Reader.CRUSHER, BATCHING, MachineStat.PROCESSING_LEVEL, MachineStat.PROCESSING_SPEED, MachineStat.ENERGY_USAGE,
                MachineStat.OUTPUT_AMOUNT, MachineStat.SUPER_OUTPUT_CHANCE, MachineStat.CRUSHER_SALVAGE_CHANCE,
                MachineStat.INSTANT_PROCESS_CHANCE, MachineStat.JAM_CHANCE, MachineStat.JAM_RECOVERY, MachineStat.CYCLE_JAM_CHANCE,
                MachineStat.UNDER_LEVEL_EFFICIENCY);
        stats(Reader.MINERS_COMPANION, Set.of(), MachineStat.PROCESSING_LEVEL);
        stats(Reader.FURNACE, HEAT_CONTROL, MachineStat.ENERGY_USAGE, MachineStat.FUEL_EFFICIENCY, MachineStat.PROCESSING_SPEED,
                MachineStat.EFFICIENCY, MachineStat.OVERDRIVE_MARGIN, MachineStat.OVERDRIVE_CAP, MachineStat.OVERDRIVE_SPEED);
        stats(Reader.ALLOY_FURNACE, HEAT_CONTROL, MachineStat.ENERGY_USAGE, MachineStat.PROCESSING_SPEED, MachineStat.STABILITY,
                MachineStat.INPUT_SLOTS, MachineStat.BLEND_SPEED, MachineStat.BLEND_HEAT_REDUCTION, MachineStat.ESCAPEMENT_SPEED);
        stats(Reader.METAL_PRESS, HEAT_CONTROL, MachineStat.ENERGY_USAGE, MachineStat.PROCESSING_SPEED, MachineStat.STABILITY,
                MachineStat.EFFICIENCY, MachineStat.MOLD_SWAP_TIME, MachineStat.HEAT_WINDOW, MachineStat.ESCAPEMENT_SPEED,
                MachineStat.BATCH_SIZE, MachineStat.BATCH_OVERHEAD);
        stats(Reader.MELTER, BATCHING, MachineStat.MAX_TEMPERATURE, MachineStat.HEAT_TRANSFER, MachineStat.PROCESSING_LEVEL,
                MachineStat.PROCESSING_SPEED, MachineStat.ENERGY_USAGE, MachineStat.EFFICIENCY, MachineStat.FLUID_CAPACITY,
                MachineStat.FLUID_TRANSFER, MachineStat.ESCAPEMENT_SPEED);
        stats(Reader.SOLID_FUEL_BURNER, Set.of(), MachineStat.ENERGY_GENERATION, MachineStat.FUEL_EFFICIENCY, MachineStat.HEAT_ISOLATION,
                MachineStat.STABILITY, MachineStat.INPUT_SLOTS, MachineStat.EFFICIENCY, MachineStat.FUEL_DURATION);
        stats(Reader.CAVITATION_GENERATOR, HEAT_CONTROL, MachineStat.PROCESSING_SPEED, MachineStat.STABILITY, MachineStat.EFFICIENCY,
                MachineStat.FLUID_TRANSFER, MachineStat.OUTPUT_AMOUNT);
        stats(Reader.GAS_CHEMISTRY, Set.of(), MachineStat.MAX_TEMPERATURE, MachineStat.PROCESSING_SPEED, MachineStat.ENERGY_USAGE,
                MachineStat.EFFICIENCY);
        stats(Reader.COMPRESSOR_TANK, Set.of(), MachineStat.PROCESSING_SPEED, MachineStat.ENERGY_USAGE, MachineStat.STABILITY,
                MachineStat.FLUID_CAPACITY, MachineStat.FLUID_TRANSFER);
        stats(Reader.CORROSION_CELL, Set.of(), MachineStat.FLUID_CAPACITY, MachineStat.FLUID_TRANSFER, MachineStat.PROCESSING_SPEED,
                MachineStat.STABILITY, MachineStat.EFFICIENCY);
        stats(Reader.FORESTRY_CART, Set.of(), MachineStat.FLUID_CAPACITY, MachineStat.FLUID_TRANSFER);
        stats(Reader.RESONANCE_CALIBRATOR, BATCHING, MachineStat.CALIBRATION_PRECISION, MachineStat.CALIBRATION_QUALITY,
                MachineStat.CATALYST_EFFICIENCY, MachineStat.STABILITY, MachineStat.PROCESSING_SPEED, MachineStat.ENERGY_USAGE,
                MachineStat.REFINEMENT_POTENTIAL_BONUS, MachineStat.STREAK_CAP, MachineStat.STREAK_FLOOR);
        stats(Reader.BATTERY_CELL, Set.of(), MachineStat.ENERGY_CAPACITY, MachineStat.ENERGY_TRANSFER, MachineStat.EFFICIENCY,
                MachineStat.IDLE_LOSS);
        stats(Reader.BATTERY_CHASSIS, Set.of(), MachineStat.BURST_TRANSFER, MachineStat.BURST_DURATION);

        behaviors(Reader.FURNACE, MachineBehavior.POWER_GRACE);
        behaviors(Reader.ALLOY_FURNACE, MachineBehavior.POWER_GRACE, MachineBehavior.ESCAPEMENT);
        behaviors(Reader.METAL_PRESS, MachineBehavior.POWER_GRACE, MachineBehavior.ESCAPEMENT);
        behaviors(Reader.MELTER, MachineBehavior.SIDE_FLUID_OUTPUT, MachineBehavior.ESCAPEMENT, MachineBehavior.AUTO_PURGE);
        behaviors(Reader.CAVITATION_GENERATOR, MachineBehavior.AUTO_PURGE);
        behaviors(Reader.GAS_CHEMISTRY, MachineBehavior.AUTO_PURGE);
        behaviors(Reader.FORESTRY_CART, MachineBehavior.REFLUX);
        behaviors(Reader.RESONANCE_CALIBRATOR, MachineBehavior.ECHO_STREAK);
    }

    /** Stats where a smaller value helps, so a negative roll is the benefit. */
    private static final Set<MachineStat> LOWER_IS_BETTER = EnumSet.of(MachineStat.ENERGY_USAGE, MachineStat.WARMUP_TIME,
            MachineStat.IDLE_LOSS, MachineStat.JAM_CHANCE, MachineStat.CYCLE_JAM_CHANCE, MachineStat.MOLD_SWAP_TIME,
            MachineStat.BATCH_OVERHEAD, MachineStat.FE_USAGE, MachineStat.VEIN_MINE_FE_USAGE, MachineStat.ORE_BURST_FE_USAGE,
            MachineStat.ORE_BURST_COOLDOWN);

    /** Stats that hold whole numbers, so flat lines on them roll integers. */
    private static final Set<MachineStat> WHOLE_NUMBER = EnumSet.of(MachineStat.INPUT_SLOTS, MachineStat.OUTPUT_SLOTS,
            MachineStat.ADDON_SLOTS, MachineStat.BATTERY_SLOTS, MachineStat.BATCH_SIZE, MachineStat.PROCESSING_LEVEL,
            MachineStat.MAX_TEMPERATURE, MachineStat.MOLD_SWAP_TIME, MachineStat.OVERDRIVE_MARGIN, MachineStat.BLEND_HEAT_REDUCTION,
            MachineStat.STREAK_CAP, MachineStat.STREAK_FLOOR, MachineStat.REFINEMENT_POTENTIAL_BONUS, MachineStat.BURST_DURATION);

    /** Stats that gate which recipes a machine can run, heat included; a Unique may reach at most one stage past its slot stage. */
    private static final Map<UniqueHost, Set<MachineStat>> RECIPE_GATES = new EnumMap<>(Map.of(
            UniqueHost.CRUSH_HEAD, EnumSet.of(MachineStat.PROCESSING_LEVEL),
            UniqueHost.HEAT_CORE, EnumSet.of(MachineStat.MAX_TEMPERATURE),
            UniqueHost.ALLOY_CRUCIBLE, EnumSet.of(MachineStat.INPUT_SLOTS)
    ));

    /** Yield stats outside the Mastery declarations. */
    private static final Set<MachineStat> YIELD = EnumSet.of(MachineStat.OUTPUT_AMOUNT, MachineStat.SUPER_OUTPUT_CHANCE,
            MachineStat.CRUSHER_SALVAGE_CHANCE, MachineStat.FLUID_YIELD, MachineStat.LEDGER_RATE);

    private UniqueStatReaders() {
    }

    public static List<Reader> readers(UniqueHost host) {
        return HOSTS.getOrDefault(host, List.of());
    }

    public static boolean reads(Reader reader, MachineStat stat) {
        return STATS.getOrDefault(reader, Set.of()).contains(stat);
    }

    public static boolean reads(Reader reader, MachineBehavior behavior) {
        return BEHAVIORS.getOrDefault(reader, Set.of()).contains(behavior);
    }

    public static boolean readByHost(UniqueHost host, MachineStat stat) {
        return readers(host).stream().anyMatch(reader -> reads(reader, stat));
    }

    public static boolean readByHost(UniqueHost host, MachineBehavior behavior) {
        return readers(host).stream().anyMatch(reader -> reads(reader, behavior));
    }

    public static boolean lowerIsBetter(MachineStat stat) {
        return LOWER_IS_BETTER.contains(stat);
    }

    public static boolean wholeNumber(MachineStat stat) {
        return WHOLE_NUMBER.contains(stat);
    }

    public static Set<MachineStat> recipeGates(UniqueHost host) {
        return RECIPE_GATES.getOrDefault(host, Set.of());
    }

    public static boolean yieldStat(MachineStat stat) {
        return YIELD.contains(stat);
    }

    private static void stats(Reader reader, Set<MachineStat> shared, MachineStat... stats) {
        Set<MachineStat> read = EnumSet.noneOf(MachineStat.class);
        read.addAll(shared);
        read.addAll(List.of(stats));
        STATS.put(reader, read);
    }

    private static void behaviors(Reader reader, MachineBehavior... behaviors) {
        BEHAVIORS.put(reader, EnumSet.copyOf(List.of(behaviors)));
    }
}
