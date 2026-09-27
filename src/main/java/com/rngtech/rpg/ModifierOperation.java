package com.rngtech.rpg;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum ModifierOperation implements StringRepresentable {
    ADD,
    INCREASED_PERCENT,
    DECREASED_PERCENT,
    MORE,
    LESS;

    public static final Codec<ModifierOperation> CODEC = StringRepresentable.fromEnum(ModifierOperation::values);
    public static final StreamCodec<ByteBuf, ModifierOperation> STREAM_CODEC =
            ByteBufCodecs.idMapper(ModifierOperation::byId, ModifierOperation::ordinal);
    private static final ModifierOperation[] VALUES = values();

    private final String serializedName;

    ModifierOperation() {
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public static ModifierOperation byId(int id) {
        return VALUES[id];
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
