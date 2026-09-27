package com.rngtech.content.calibration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public record CalibrationValueRange(int min, int max) {
    public static final Codec<CalibrationValueRange> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("min").forGetter(CalibrationValueRange::min),
            Codec.INT.fieldOf("max").forGetter(CalibrationValueRange::max)
    ).apply(instance, CalibrationValueRange::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CalibrationValueRange> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            CalibrationValueRange::min,
            ByteBufCodecs.VAR_INT,
            CalibrationValueRange::max,
            CalibrationValueRange::new
    );

    public CalibrationValueRange {
        int lower = Math.min(min, max);
        int upper = Math.max(min, max);
        min = lower;
        max = upper;
    }

    public int roll(RandomSource random) {
        return min + random.nextInt(max - min + 1);
    }

    public CalibrationValueRange clamped(int lowerBound, int upperBound) {
        int clampedMin = Mth.clamp(min, lowerBound, upperBound);
        int clampedMax = Mth.clamp(max, lowerBound, upperBound);
        return new CalibrationValueRange(Math.min(clampedMin, clampedMax), Math.max(clampedMin, clampedMax));
    }
}
