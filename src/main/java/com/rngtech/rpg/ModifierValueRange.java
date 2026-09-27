package com.rngtech.rpg;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;

public record ModifierValueRange(double min, double max, boolean wholeNumber) {
    public static final Codec<ModifierValueRange> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("min").forGetter(ModifierValueRange::min),
            Codec.DOUBLE.fieldOf("max").forGetter(ModifierValueRange::max),
            Codec.BOOL.optionalFieldOf("whole_number", false).forGetter(ModifierValueRange::wholeNumber)
    ).apply(instance, ModifierValueRange::new));

    public static final StreamCodec<ByteBuf, ModifierValueRange> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE,
            ModifierValueRange::min,
            ByteBufCodecs.DOUBLE,
            ModifierValueRange::max,
            ByteBufCodecs.BOOL,
            ModifierValueRange::wholeNumber,
            ModifierValueRange::new
    );

    public ModifierValueRange(double min, double max) {
        this(min, max, false);
    }

    public ModifierValueRange {
        if (max < min) {
            throw new IllegalArgumentException("Modifier value range max must be greater than or equal to min.");
        }
        if (wholeNumber && Math.ceil(min) > Math.floor(max)) {
            throw new IllegalArgumentException("Whole-number modifier value range must include at least one whole number.");
        }
    }

    public static ModifierValueRange fixed(double value) {
        return new ModifierValueRange(value, value);
    }

    public double roll(RandomSource random) {
        if (wholeNumber) {
            int minValue = (int) Math.ceil(min);
            int maxValue = (int) Math.floor(max);
            return minValue + random.nextInt(maxValue - minValue + 1);
        }
        if (min == max) {
            return min;
        }
        return min + (random.nextDouble() * (max - min));
    }
}
