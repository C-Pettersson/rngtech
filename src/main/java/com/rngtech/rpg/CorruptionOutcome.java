package com.rngtech.rpg;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** The Volatile Catalyst outcomes. Every outcome leaves the target Corrupted. */
public enum CorruptionOutcome implements StringRepresentable {
    UNTOUCHED,
    BLESSED,
    REFORGED,
    /** Scales every rolled affix by a random percent, past its tier range. */
    WARPED,
    BLIGHTED;

    public static final Codec<CorruptionOutcome> CODEC = StringRepresentable.fromEnum(CorruptionOutcome::values);
    public static final StreamCodec<ByteBuf, CorruptionOutcome> STREAM_CODEC =
            ByteBufCodecs.idMapper(CorruptionOutcome::byId, CorruptionOutcome::ordinal);
    private static final CorruptionOutcome[] VALUES = values();

    private final String serializedName;

    CorruptionOutcome() {
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public static CorruptionOutcome byId(int id) {
        return VALUES[id];
    }

    public boolean grantsImplicit() {
        return this == BLESSED || this == BLIGHTED;
    }

    public String translationKey() {
        return "rngtech.corruption.outcome." + serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
