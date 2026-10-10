package com.rngtech.content.calibration;

import com.rngtech.rpg.progression.AscendancyFormulas;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

/**
 * The Harmonist's streak: consecutive calibrations of one family on one pattern. Pattern Memory keeps it on a dropped
 * machine and lets it survive one change of pattern or family; Echo Streak lets it wait out one calibration of another
 * family.
 */
public record CalibrationStreak(int streak, String family, ItemStack pattern, boolean swapUsed, int harmonic, boolean echoUsed) {
    public static final CalibrationStreak EMPTY = new CalibrationStreak(0, "", ItemStack.EMPTY, false, 0, false);
    public static final Codec<CalibrationStreak> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("streak", 0).forGetter(CalibrationStreak::streak),
            Codec.STRING.optionalFieldOf("family", "").forGetter(CalibrationStreak::family),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("pattern", ItemStack.EMPTY).forGetter(CalibrationStreak::pattern),
            Codec.BOOL.optionalFieldOf("swap_used", false).forGetter(CalibrationStreak::swapUsed),
            Codec.INT.optionalFieldOf("harmonic", 0).forGetter(CalibrationStreak::harmonic),
            Codec.BOOL.optionalFieldOf("echo_used", false).forGetter(CalibrationStreak::echoUsed)
    ).apply(instance, CalibrationStreak::new));

    public CalibrationStreak {
        streak = Math.max(0, streak);
        family = family == null ? "" : family;
        pattern = pattern == null || pattern.isEmpty() ? ItemStack.EMPTY : pattern.copyWithCount(1);
        harmonic = Math.max(0, harmonic);
    }

    /** Whether the next calibration continues this streak. */
    public boolean continues(ItemStack nextPattern, String nextFamily) {
        return streak > 0 && family.equals(nextFamily) && !pattern.isEmpty() && ItemStack.isSameItemSameComponents(pattern, nextPattern);
    }

    /** The streak a calibration of {@code nextPattern} and {@code nextFamily} builds on; Pattern Memory keeps it once. */
    public CalibrationStreak before(ItemStack nextPattern, String nextFamily, boolean patternMemory) {
        if (streak == 0 || continues(nextPattern, nextFamily)) {
            return this;
        }
        return patternMemory && !swapUsed ? new CalibrationStreak(streak, nextFamily, nextPattern, true, harmonic, echoUsed) : EMPTY;
    }

    /**
     * Whether a calibration of {@code nextFamily} is the one detour Echo Streak allows: it builds on nothing, and the streak
     * resumes when its own family returns.
     */
    public boolean echoes(String nextFamily, boolean patternMemory) {
        return AscendancyFormulas.echoDetour(streak, family, nextFamily, echoUsed, patternMemory && !swapUsed);
    }

    public CalibrationStreak withEchoUsed() {
        return new CalibrationStreak(streak, family, pattern, swapUsed, harmonic, true);
    }

    public CalibrationStreak withHarmonic(int nextHarmonic) {
        return new CalibrationStreak(streak, family, pattern, swapUsed, nextHarmonic, echoUsed);
    }

    /** The streak after a completed calibration of {@code nextPattern} and {@code nextFamily}. */
    public CalibrationStreak after(ItemStack nextPattern, String nextFamily, int nextHarmonic) {
        return new CalibrationStreak(streak + 1, nextFamily, nextPattern, swapUsed, nextHarmonic, echoUsed);
    }
}
