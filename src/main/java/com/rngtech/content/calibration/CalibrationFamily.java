package com.rngtech.content.calibration;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum CalibrationFamily implements StringRepresentable {
    STRUCTURAL,
    KINETIC,
    THERMAL,
    CONDUCTIVE,
    STORAGE,
    LOGIC;

    public static final Codec<CalibrationFamily> CODEC = StringRepresentable.fromEnum(CalibrationFamily::values);
    public static final StreamCodec<ByteBuf, CalibrationFamily> STREAM_CODEC =
            ByteBufCodecs.idMapper(CalibrationFamily::byId, CalibrationFamily::ordinal);
    private static final CalibrationFamily[] VALUES = values();

    private final String serializedName;

    CalibrationFamily() {
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public static CalibrationFamily byId(int id) {
        return VALUES[id];
    }

    public String patternItemId() {
        return serializedName + "_calibration_pattern";
    }

    public String componentItemId() {
        return "calibrated_" + serializedName + "_component";
    }

    public String translationKey() {
        return "rngtech.calibration_family." + serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
