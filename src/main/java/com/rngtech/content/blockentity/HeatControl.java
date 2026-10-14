package com.rngtech.content.blockentity;

import com.rngtech.rpg.MachineBehavior;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

final class HeatControl {
    static final int AMBIENT_TEMPERATURE = 20;
    static final int FAILURE_STRAIN_THRESHOLD = 6000;
    static final int POWER_DROP_STRAIN = 1400;
    private static final double MIN_COOLING_PER_TICK = 0.25;
    private static final double GOLDEN_RATIO_CONJUGATE = 0.6180339887498949;

    static int warmupRate(MachineStatAccumulator stats) {
        double heatTransfer = Math.max(0.1, stats.value(MachineStat.HEAT_TRANSFER));
        double warmupTime = Math.max(0.25, stats.value(MachineStat.WARMUP_TIME));
        return Math.max(1, (int) Math.round(10.0 * heatTransfer / warmupTime));
    }

    /** Degrees lost per idle tick on average; Heat Isolation and Cooling Rate reach down to an eighth of the base 2. */
    static double coolingPerTick(MachineStatAccumulator stats) {
        double coolingRate = Math.max(0.1, stats.value(MachineStat.COOLING_RATE));
        double heatIsolation = Math.max(0.25, stats.value(MachineStat.HEAT_ISOLATION));
        return Math.max(MIN_COOLING_PER_TICK, 2.0 * coolingRate / heatIsolation);
    }

    /**
     * Whole degrees lost this tick. The fractional part is spread over ticks, so every Heat Isolation and Cooling Rate
     * bonus slows cooling instead of rounding away.
     */
    static int coolingStep(MachineStatAccumulator stats, long gameTime) {
        double rate = coolingPerTick(stats);
        int whole = (int) Math.floor(rate);
        double phase = (gameTime * GOLDEN_RATIO_CONJUGATE) % 1.0;
        return whole + (phase < rate - whole ? 1 : 0);
    }

    /** Halves temperature failure strain for Quench Protocol, keeping at least one point. */
    static int quenched(int addedStrain) {
        return addedStrain > 0 ? Math.max(1, (int) Math.ceil(addedStrain * 0.5D)) : addedStrain;
    }

    static int defaultSafeMaximumTemperature(int targetTemperature) {
        if (targetTemperature <= 0) {
            return 0;
        }
        return Math.max(targetTemperature, (int) Math.round(targetTemperature * 1.15));
    }

    static int effectiveOverheatTemperature(int safeMaximumTemperature, MachineStatAccumulator stats) {
        double tolerance = Math.max(1.0, stats.value(MachineStat.OVERHEAT_TOLERANCE));
        return Math.max(safeMaximumTemperature, (int) Math.round(safeMaximumTemperature * tolerance));
    }

    static int simulatedTemperature(
            int currentTemperature,
            int targetTemperature,
            double requiredTemperatureStability,
            MachineStatAccumulator stats,
            BlockPos pos,
            long gameTime,
            int salt
    ) {
        if (targetTemperature <= 0 || requiredTemperatureStability <= 0.0) {
            return currentTemperature;
        }
        double stability = Math.max(0.0, stats.value(MachineStat.TEMPERATURE_STABILITY));
        double gap = Math.max(0.0, requiredTemperatureStability - stability);
        if (gap <= 0.0001) {
            return currentTemperature;
        }

        int amplitude = Math.max(1, (int) Math.round(targetTemperature * Math.min(0.25, gap * 0.12)));
        int range = amplitude * 2 + 1;
        long mixed = (gameTime / 10L) + pos.asLong() * 31L + salt * 131L;
        int wobble = Math.floorMod((int) (mixed ^ mixed >>> 32), range) - amplitude;
        return currentTemperature + wobble;
    }

    static int failureStrainFromTemperature(
            int simulatedTemperature,
            int minimumTemperature,
            int overheatTemperature,
            double requiredTemperatureStability,
            MachineStatAccumulator stats
    ) {
        int underheat = Math.max(0, minimumTemperature - simulatedTemperature);
        int overheat = Math.max(0, simulatedTemperature - overheatTemperature);
        double instability = Math.max(0.0, requiredTemperatureStability - stats.value(MachineStat.TEMPERATURE_STABILITY));
        double risk = underheat / 5.0 + overheat / 5.0 + instability * 80.0;
        return Mth.clamp((int) Math.round(risk), 0, 100);
    }

    /** Strain drained per tick inside the safe band; Strain Recovery adds to it. */
    static int strainRecovery(MachineStatAccumulator stats) {
        return Math.max(4, (int) Math.round(6.0 * Math.max(0.25, stats.value(MachineStat.TEMPERATURE_STABILITY))))
                + stats.intValue(MachineStat.STRAIN_RECOVERY);
    }

    static int powerDropStrain(MachineTraits traits) {
        return traits.hasBehavior(MachineBehavior.POWER_GRACE) ? POWER_DROP_STRAIN / 2 : POWER_DROP_STRAIN;
    }

    static int failureProgress(int failureStrain) {
        return Mth.clamp((int) Math.round(failureStrain * 100.0 / FAILURE_STRAIN_THRESHOLD), 0, 100);
    }

    private HeatControl() {
    }
}
