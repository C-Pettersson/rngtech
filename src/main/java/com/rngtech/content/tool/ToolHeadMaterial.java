package com.rngtech.content.tool;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum ToolHeadMaterial implements StringRepresentable {
    FLINT(0, 1, 3.0, 96, 0.65, false, "flint"),
    IRON(1, 1, 5.0, 256, 1.00, false, "iron"),
    COPPER(2, 2, 5.8, 220, 0.92, true, "copper"),
    BRONZE(3, 2, 5.5, 320, 1.05, false, "bronze"),
    STEEL(4, 3, 6.2, 512, 1.15, false, "steel"),
    ALUMINUM(5, 5, 7.2, 430, 0.95, false, "aluminum"),
    TITANIUM(6, 6, 7.0, 760, 1.25, false, "titanium"),
    TUNGSTENSTEEL(7, 7, 5.6, 1200, 1.30, false, "tungstensteel"),
    NULLITE(7, 7, 8.2, 900, 1.20, true, "nullite"),
    EXOTIC(8, 8, 8.8, 1600, 1.35, true, "naquadah");

    public static final Codec<ToolHeadMaterial> CODEC = StringRepresentable.fromEnum(ToolHeadMaterial::values);
    public static final StreamCodec<ByteBuf, ToolHeadMaterial> STREAM_CODEC =
            ByteBufCodecs.idMapper(ToolHeadMaterial::byId, ToolHeadMaterial::ordinal);
    private static final ToolHeadMaterial[] VALUES = values();

    private final int stage;
    private final int miningLevel;
    private final double miningSpeed;
    private final int durability;
    private final double stability;
    private final boolean oreBurst;
    private final String serializedName;
    private final String materialId;

    ToolHeadMaterial(
            int stage,
            int miningLevel,
            double miningSpeed,
            int durability,
            double stability,
            boolean oreBurst,
            String materialId
    ) {
        this.stage = stage;
        this.miningLevel = miningLevel;
        this.miningSpeed = miningSpeed;
        this.durability = durability;
        this.stability = stability;
        this.oreBurst = oreBurst;
        this.materialId = materialId;
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public static ToolHeadMaterial byId(int id) {
        return VALUES[id];
    }

    public int stage() {
        return stage;
    }

    public int miningLevel() {
        return miningLevel;
    }

    public double miningSpeed() {
        return miningSpeed;
    }

    public int durability() {
        return durability;
    }

    public double stability() {
        return stability;
    }

    public boolean oreBurst() {
        return oreBurst;
    }

    public String materialId() {
        return materialId;
    }

    public String serializedName() {
        return serializedName;
    }

    public String translationKey() {
        return "rngtech.tool_head_material." + serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
