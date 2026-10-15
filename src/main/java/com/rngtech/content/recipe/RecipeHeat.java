package com.rngtech.content.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * The heat units (HU) a heated recipe's input absorbs. Recipes declare {@code heat}; a legacy {@code energy} field in FE
 * still loads, converted at {@link #HEAT_PER_LEGACY_FE}, so older datapacks keep working.
 */
public final class RecipeHeat {
    /** Share of an old FE cost that is the input's own heat; the rest now comes from the chamber's heat loss. */
    public static final double HEAT_PER_LEGACY_FE = 0.6;
    /** Recipes at or below this temperature keep their heat; hotter ones scale up with {@link #temperatureScale}. */
    public static final int TEMPERATURE_SCALE_BASE = 800;
    /** How steeply a hotter recipe's input heat grows: 1.6x at 1,450 °C, 2.1x at 2,000 °C. */
    public static final double TEMPERATURE_SCALE_EXPONENT = 0.8;
    /** Metal Press heat after the move to heat units, which had left forming above its old FE. */
    public static final double PRESS_HEAT_SCALE = 0.6;

    public static final MapCodec<Fields> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("heat").forGetter(Fields::heat),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("energy").forGetter(Fields::energy)
    ).apply(instance, Fields::new));

    private RecipeHeat() {
    }

    public static Fields of(int heat) {
        return new Fields(Optional.of(heat), Optional.empty());
    }

    /**
     * How much more heat an input needs at a recipe temperature than at 800 °C: heating metal hotter takes more heat.
     * Shipped recipes have it built into their {@code heat}; legacy {@code energy} recipes get it on conversion.
     */
    public static double temperatureScale(int target) {
        return Math.pow(Math.max(TEMPERATURE_SCALE_BASE, target) / (double) TEMPERATURE_SCALE_BASE, TEMPERATURE_SCALE_EXPONENT);
    }

    public static int fromLegacyEnergy(int energy, double multiplier) {
        long heat = Math.round(Math.max(1, energy) * Math.max(0.01, multiplier) * HEAT_PER_LEGACY_FE);
        return (int) Math.max(1, Math.min(Integer.MAX_VALUE, heat));
    }

    public record Fields(Optional<Integer> heat, Optional<Integer> energy) {
        /** {@code heat} when present; otherwise the legacy FE (or its default) converted, with any old FE surcharge. */
        public int resolve(int defaultEnergy, double legacyMultiplier) {
            return heat.orElseGet(() -> fromLegacyEnergy(energy.orElse(defaultEnergy), legacyMultiplier));
        }
    }
}
