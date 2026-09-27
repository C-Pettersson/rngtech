package com.rngtech.rpg;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum Rarity implements StringRepresentable {
    NORMAL,
    MAGIC,
    RARE,
    UNIQUE;

    public static final Codec<Rarity> CODEC = StringRepresentable.fromEnum(Rarity::values);
    public static final StreamCodec<ByteBuf, Rarity> STREAM_CODEC = ByteBufCodecs.idMapper(Rarity::byId, Rarity::ordinal);
    private static final Rarity[] VALUES = values();

    private final String serializedName;

    Rarity() {
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public static Rarity byId(int id) {
        return VALUES[id];
    }

    public String translationKey() {
        return "rngtech.rarity." + serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
