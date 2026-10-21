package com.rngtech.rpg.refinement;

import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.PhaseArrayRules;

import java.util.ArrayList;
import java.util.List;

/**
 * What a formed Phase Array line reads from its parts, in line order with position 1 next to the Emitter. Sums are the
 * PRD's: charge, Tolerance, Thrift, Recovery, Salvage, and Mercy add up over every part; Hold, Second Hold, and Overhold
 * are per Coil, with the Emitter's Overhold added to each Coil; the Partial Window is the Emitter's. Primed Charge
 * Rate is the Emitter's percent on top of the rate, paid only while Battery Cells sit in the Cradle's sockets.
 */
public record PhaseLineStats(
        long chargeCapacity,
        int chargeRate,
        double primedChargeRate,
        List<Double> holdChance,
        List<Double> secondHold,
        List<Double> overholdChance,
        int partialWindow,
        int strainTolerance,
        double feThrift,
        double feRecovery,
        double glyphSalvage,
        double strainMercy,
        int usableCoils
) {
    private static final double PERCENT_CAP = 100.0;
    /** Thrift stops short of free pulses. */
    private static final double THRIFT_CAP = 50.0;

    public static PhaseLineStats empty() {
        return new PhaseLineStats(0L, 0, 0, List.of(), List.of(), List.of(), PhaseArrayRules.BASE_PARTIAL_WINDOW, 0, 0, 0, 0, 0, 0);
    }

    /** {@code coilsFromEmitter} holds each Coil's effective stats, position 1 first. */
    public static PhaseLineStats of(MachineStatAccumulator emitter, List<MachineStatAccumulator> coilsFromEmitter, int usableCoils) {
        long capacity = Math.round(emitter.value(MachineStat.CHARGE_CAPACITY));
        double rate = emitter.value(MachineStat.CHARGE_RATE);
        double tolerance = emitter.value(MachineStat.STRAIN_TOLERANCE);
        double thrift = emitter.value(MachineStat.FE_THRIFT);
        double recovery = emitter.value(MachineStat.FE_RECOVERY);
        double salvage = emitter.value(MachineStat.GLYPH_SALVAGE);
        double mercy = emitter.value(MachineStat.STRAIN_MERCY);
        double emitterOverhold = emitter.value(MachineStat.OVERHOLD_CHANCE);
        List<Double> hold = new ArrayList<>();
        List<Double> second = new ArrayList<>();
        List<Double> overhold = new ArrayList<>();
        for (MachineStatAccumulator coil : coilsFromEmitter) {
            capacity += Math.round(coil.value(MachineStat.CHARGE_CAPACITY));
            rate += coil.value(MachineStat.CHARGE_RATE);
            tolerance += coil.value(MachineStat.STRAIN_TOLERANCE);
            thrift += coil.value(MachineStat.FE_THRIFT);
            recovery += coil.value(MachineStat.FE_RECOVERY);
            salvage += coil.value(MachineStat.GLYPH_SALVAGE);
            mercy += coil.value(MachineStat.STRAIN_MERCY);
            hold.add(Math.min(PERCENT_CAP, coil.value(MachineStat.HOLD_CHANCE)));
            second.add(Math.min(PERCENT_CAP, coil.value(MachineStat.SECOND_HOLD)));
            overhold.add(Math.min(PERCENT_CAP, coil.value(MachineStat.OVERHOLD_CHANCE) + emitterOverhold));
        }
        return new PhaseLineStats(
                capacity,
                (int) Math.round(rate),
                Math.max(0.0, emitter.value(MachineStat.PRIMED_CHARGE_RATE)),
                List.copyOf(hold),
                List.copyOf(second),
                List.copyOf(overhold),
                (int) Math.round(emitter.value(MachineStat.PARTIAL_WINDOW)),
                (int) Math.round(tolerance),
                Math.min(THRIFT_CAP, thrift),
                Math.min(PERCENT_CAP, recovery),
                Math.min(PERCENT_CAP, salvage),
                Math.min(PERCENT_CAP, mercy),
                usableCoils
        );
    }

    public int coilCount() {
        return holdChance.size();
    }

    /** The rate the line charges at: the sum of its parts, plus the Emitter's Primed percent while cells are socketed. */
    public int chargeRate(boolean cellsSocketed) {
        return cellsSocketed ? (int) Math.round(chargeRate * (1.0 + primedChargeRate / 100.0)) : chargeRate;
    }

    /** The chance one Coil holds, counting its Second Hold reroll on a slip, as a fraction. */
    public double effectiveHold(int position) {
        double hold = holdChance.get(position - 1) / PERCENT_CAP;
        double second = secondHold.get(position - 1) / PERCENT_CAP;
        return hold + (1 - hold) * second;
    }

    public double overholdFraction(int position) {
        return overholdChance.get(position - 1) / PERCENT_CAP;
    }
}
