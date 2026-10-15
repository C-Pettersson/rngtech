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

    public static final MapCodec<Fields> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("heat").forGetter(Fields::heat),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("energy").forGetter(Fields::energy)
    ).apply(instance, Fields::new));

    private RecipeHeat() {
    }

    public static Fields of(int heat) {
        return new Fields(Optional.of(heat), Optional.empty());
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
