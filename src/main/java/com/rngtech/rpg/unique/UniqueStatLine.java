package com.rngtech.rpg.unique;

import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.ModifierOperation;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.ModifierValueRange;

import net.minecraft.util.RandomSource;

import java.util.Locale;

/**
 * One catalog stat on a Unique, in authored units: {@code flat} adds the value, {@code increased} adds a fraction to the
 * increased bucket, and {@code more} multiplies by one plus the value. {@code worst} and {@code best} are equal for a fixed
 * line; a drawback range can run downward.
 */
public record UniqueStatLine(MachineStat stat, Operation operation, double worst, double best, Role role, String ascendancy) {
    public enum Operation {
        FLAT(ModifierOperation.ADD),
        INCREASED(ModifierOperation.INCREASED_PERCENT),
        MORE(ModifierOperation.MORE);

        private final ModifierOperation engine;

        Operation(ModifierOperation engine) {
            this.engine = engine;
        }

        public ModifierOperation engine() {
            return engine;
        }

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** What a line does for the Unique; it drives the benefit and penalty checks and the tooltip sections. */
    public enum Role {
        IDENTITY,
        SIGNATURE,
        DRAWBACK,
        HOOK;

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** Non-integer rolls land on hundredths, so percent lines roll whole percents. */
    private static final double ROLL_STEP = 0.01;

    public UniqueStatLine {
        ascendancy = ascendancy == null ? "" : ascendancy;
    }

    public boolean ranged() {
        return worst != best;
    }

    public double lower() {
        return Math.min(worst, best);
    }

    public double upper() {
        return Math.max(worst, best);
    }

    public double midpoint() {
        double middle = (worst + best) / 2.0;
        return rollsWhole() ? Math.round(middle) : middle;
    }

    /** Flat lines with whole-number bounds roll whole numbers, so +10 to +30% rolls +22%, not +22.29%. */
    public boolean rollsWhole() {
        return wholeNumber() || operation == Operation.FLAT && worst == Math.rint(worst) && best == Math.rint(best);
    }

    /** Integer stats roll whole numbers, both ends included. */
    public boolean wholeNumber() {
        return operation == Operation.FLAT && UniqueStatReaders.wholeNumber(stat);
    }

    public double roll(RandomSource random) {
        if (!ranged()) {
            return worst;
        }
        if (rollsWhole()) {
            int low = (int) Math.ceil(lower());
            int high = (int) Math.floor(upper());
            return low + random.nextInt(high - low + 1);
        }
        double rolled = lower() + random.nextDouble() * (upper() - lower());
        return Math.max(lower(), Math.min(upper(), Math.round(rolled / ROLL_STEP) * ROLL_STEP));
    }

    public double clamp(double rolled) {
        double clamped = Math.max(lower(), Math.min(upper(), rolled));
        return rollsWhole() ? Math.round(clamped) : Math.round(clamped / ROLL_STEP) * ROLL_STEP;
    }

    /** Where {@code rolled} sits in the range, from 0 for the worst roll to 1 for the best. */
    public double quality(double rolled) {
        return ranged() ? Math.max(0.0, Math.min(1.0, (clamp(rolled) - worst) / (best - worst))) : 1.0;
    }

    /** The accumulator value for an authored value: percent for increased, a multiplier for more. */
    public double engineValue(double authored) {
        return switch (operation) {
            case FLAT -> authored;
            case INCREASED -> authored * 100.0;
            case MORE -> 1.0 + authored;
        };
    }

    public double authoredValue(double engine) {
        return switch (operation) {
            case FLAT -> engine;
            case INCREASED -> engine / 100.0;
            case MORE -> engine - 1.0;
        };
    }

    public ModifierValueRange engineRange() {
        double low = engineValue(lower());
        double high = engineValue(upper());
        return new ModifierValueRange(Math.min(low, high), Math.max(low, high), wholeNumber());
    }

    /** The stored roll for {@code authored}, kept in the Unique modifier slot. */
    public MachineModifier rollModifier(double authored) {
        return new MachineModifier("", "", ModifierSlot.UNIQUE, stat, operation.engine(), 0, engineRange(), engineValue(clamp(authored)), java.util.List.of());
    }

    /** Whether the stored modifier is this line's roll. */
    public boolean matches(MachineModifier modifier) {
        return modifier.slot() == ModifierSlot.UNIQUE && modifier.stat() == stat && modifier.operation() == operation.engine();
    }

    /** Whether {@code authored} helps the host, by the stat's direction; zero helps nothing. */
    public boolean isBenefit(double authored) {
        return UniqueStatReaders.lowerIsBetter(stat) ? authored < 0.0 : authored > 0.0;
    }

    public boolean isPenalty(double authored) {
        return UniqueStatReaders.lowerIsBetter(stat) ? authored > 0.0 : authored < 0.0;
    }
}
