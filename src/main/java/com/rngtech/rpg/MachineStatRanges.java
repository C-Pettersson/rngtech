package com.rngtech.rpg;

import net.minecraft.util.RandomSource;

import java.util.List;

public final class MachineStatRanges {
    public static final List<MachineStatRange> BASE_MACHINE = List.of(
            new MachineStatRange(MachineStat.INPUT_SLOTS, 1.0, 2.0, true),
            new MachineStatRange(MachineStat.OUTPUT_SLOTS, 1.0, 2.0, true),
            new MachineStatRange(MachineStat.ADDON_SLOTS, 0.0, 2.0, true),
            new MachineStatRange(MachineStat.PROCESSING_SPEED, 0.8, 1.2),
            new MachineStatRange(MachineStat.PROCESSING_LEVEL, 1.0, 1.0, true),
            new MachineStatRange(MachineStat.ENERGY_USAGE, 0.9, 1.1),
            new MachineStatRange(MachineStat.ENERGY_CAPACITY, 0.8, 1.2),
            new MachineStatRange(MachineStat.EFFICIENCY, 0.9, 1.1),
            new MachineStatRange(MachineStat.BATCH_SIZE, 1.0, 1.0, true),
            new MachineStatRange(MachineStat.BUFFER_SIZE, 1.0, 1.0, true),
            new MachineStatRange(MachineStat.STABILITY, 0.9, 1.1),
            new MachineStatRange(MachineStat.UPGRADE_LIMIT, 1.0, 3.0, true)
    );

    public static final List<MachineStatRange> FURNACE = List.of(
            new MachineStatRange(MachineStat.HEAT_TRANSFER, 0.8, 1.2),
            new MachineStatRange(MachineStat.HEAT_ISOLATION, 0.8, 1.2),
            new MachineStatRange(MachineStat.MAX_TEMPERATURE, 800.0, 1200.0),
            new MachineStatRange(MachineStat.TEMPERATURE_STABILITY, 0.85, 1.15),
            new MachineStatRange(MachineStat.FUEL_EFFICIENCY, 0.9, 1.1),
            new MachineStatRange(MachineStat.WARMUP_TIME, 0.9, 1.2),
            new MachineStatRange(MachineStat.COOLING_RATE, 0.8, 1.2),
            new MachineStatRange(MachineStat.OVERHEAT_TOLERANCE, 1.0, 1.25)
    );

    public static MachineStatAccumulator rollBaseMachine(RandomSource random) {
        return MachineStatAccumulator.fromRanges(BASE_MACHINE, random);
    }

    public static MachineStatAccumulator rollFurnace(RandomSource random) {
        MachineStatAccumulator stats = rollBaseMachine(random);
        stats.rollBaseValues(FURNACE, random);
        return stats;
    }

    private MachineStatRanges() {
    }
}
