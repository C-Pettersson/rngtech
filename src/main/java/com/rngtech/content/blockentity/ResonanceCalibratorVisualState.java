package com.rngtech.content.blockentity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * What a Resonance Calibrator shows in the world: the input on each lane's pin, the active Calibration Pattern, the
 * finished components in the tray, the cycle's progress and stability range on the scope, the last rolled stability,
 * the Harmonist streak, and the installed Gear. The part keys are empty when the slot is empty, {@code charge} is
 * negative without a Battery Cell, the stability range is 0 to 0 without a recipe, and {@code lastStability} is
 * negative until a calibration finishes.
 *
 * @param lanes           pins holding an input this cycle, 0 to the chassis's lane count
 * @param progress        the share of the cycle done, 0 to 1
 * @param cycleTicks      the cycle's length in ticks, 0 without a recipe
 * @param completions     a counter that steps with each finished calibration, so the client can flash the field
 * @param streak          the Harmonist's stability-floor bonus from the current streak
 * @param streakCap       the most that bonus can reach, 0 without the Harmonist's Resonant Streak
 */
public record ResonanceCalibratorVisualState(
        int lanes,
        Item input,
        Item pattern,
        Item output,
        boolean working,
        float progress,
        int cycleTicks,
        int stabilityMin,
        int stabilityMax,
        int lastStability,
        int completions,
        int streak,
        int streakCap,
        String coil,
        String board,
        String matrix,
        String cell,
        float charge
) {
    public static final int CHARGE_SEGMENTS = 8;
    public static final int STREAK_PIPS = 8;
    public static final ResonanceCalibratorVisualState IDLE = new ResonanceCalibratorVisualState(
            0, Items.AIR, Items.AIR, Items.AIR, false, 0.0F, 0, 0, 0, -1, 0, 0, 0, "", "", "", "", -1.0F);

    /** Lit charge-meter segments: none for an empty or missing cell, one as soon as it holds any FE. */
    public int chargeSegments() {
        return charge <= 0.0F ? 0 : Math.max(1, Math.min(CHARGE_SEGMENTS, Math.round(charge * CHARGE_SEGMENTS)));
    }

    /** Lit streak pips under the scope, out of {@link #STREAK_PIPS}. */
    public int streakPips() {
        if (streakCap <= 0 || streak <= 0) {
            return 0;
        }
        return Math.max(1, Math.min(STREAK_PIPS, Math.round(streak * STREAK_PIPS / (float) streakCap)));
    }

    /** Quantized so a running cycle or a draining cell does not send an update every tick. */
    public Key key() {
        return new Key(
                lanes,
                input,
                pattern,
                output,
                working,
                Math.round(progress * 8.0F),
                cycleTicks / 5,
                stabilityMin,
                stabilityMax,
                lastStability,
                completions,
                streakPips(),
                streakCap > 0,
                coil,
                board,
                matrix,
                cell,
                charge < 0.0F ? -1 : chargeSegments()
        );
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Lanes", lanes);
        tag.putString("Input", BuiltInRegistries.ITEM.getKey(input).toString());
        tag.putString("Pattern", BuiltInRegistries.ITEM.getKey(pattern).toString());
        tag.putString("Output", BuiltInRegistries.ITEM.getKey(output).toString());
        tag.putBoolean("Working", working);
        tag.putFloat("Progress", progress);
        tag.putInt("CycleTicks", cycleTicks);
        tag.putInt("StabilityMin", stabilityMin);
        tag.putInt("StabilityMax", stabilityMax);
        tag.putInt("LastStability", lastStability);
        tag.putInt("Completions", completions);
        tag.putInt("Streak", streak);
        tag.putInt("StreakCap", streakCap);
        tag.putString("Coil", coil);
        tag.putString("Board", board);
        tag.putString("Matrix", matrix);
        tag.putString("Cell", cell);
        tag.putFloat("Charge", charge);
        return tag;
    }

    public static ResonanceCalibratorVisualState load(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return IDLE;
        }
        return new ResonanceCalibratorVisualState(
                Math.max(0, tag.getInt("Lanes")),
                item(tag.getString("Input")),
                item(tag.getString("Pattern")),
                item(tag.getString("Output")),
                tag.getBoolean("Working"),
                tag.getFloat("Progress"),
                tag.getInt("CycleTicks"),
                tag.getInt("StabilityMin"),
                tag.getInt("StabilityMax"),
                tag.contains("LastStability") ? tag.getInt("LastStability") : -1,
                tag.getInt("Completions"),
                tag.getInt("Streak"),
                tag.getInt("StreakCap"),
                tag.getString("Coil"),
                tag.getString("Board"),
                tag.getString("Matrix"),
                tag.getString("Cell"),
                tag.contains("Charge") ? tag.getFloat("Charge") : -1.0F
        );
    }

    private static Item item(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        return location == null ? Items.AIR : BuiltInRegistries.ITEM.get(location);
    }

    public record Key(
            int lanes,
            Item input,
            Item pattern,
            Item output,
            boolean working,
            int progress,
            int cycleTicks,
            int stabilityMin,
            int stabilityMax,
            int lastStability,
            int completions,
            int streakPips,
            boolean harmonist,
            String coil,
            String board,
            String matrix,
            String cell,
            int charge
    ) {
    }

    /** Client-side easing between updates: the cycle runs on between syncs, and the field fades in, out, and flashes. */
    public static final class Animation {
        public double lastTime = Double.NaN;
        public float progress;
        public float field;
        public float flash;
        public float spin;
        public float matrixTurn;
        public int completions = -1;
    }
}
