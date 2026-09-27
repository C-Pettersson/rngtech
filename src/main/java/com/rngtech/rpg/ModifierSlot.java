package com.rngtech.rpg;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum ModifierSlot implements StringRepresentable {
    IMPLICIT,
    PREFIX,
    SUFFIX,
    ENCHANT;

    public static final Codec<ModifierSlot> CODEC = StringRepresentable.fromEnum(ModifierSlot::values);
    public static final StreamCodec<ByteBuf, ModifierSlot> STREAM_CODEC =
            ByteBufCodecs.idMapper(ModifierSlot::byId, ModifierSlot::ordinal);
    private static final ModifierSlot[] VALUES = values();

    private final String serializedName;

    ModifierSlot() {
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public static ModifierSlot byId(int id) {
        return VALUES[id];
    }

    public boolean isAffix() {
        return this == PREFIX || this == SUFFIX;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
