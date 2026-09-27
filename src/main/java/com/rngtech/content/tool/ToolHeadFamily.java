package com.rngtech.content.tool;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Locale;

public enum ToolHeadFamily implements StringRepresentable {
    PICK(1.0, 1.0, 1.20, 0, 16, TargetType.PICKAXE),
    HAMMER(0.75, 1.4, 0.80, 18, 32, TargetType.PICKAXE),
    SHOVEL(1.0, 1.0, 1.00, 0, 12, TargetType.SHOVEL),
    DIGGER(0.75, 1.4, 0.80, 18, 28, TargetType.SHOVEL),
    AXE(1.0, 0.9, 0.90, 0, 16, TargetType.AXE),
    TREEFELLER(0.70, 1.25, 0.70, 14, 28, TargetType.AXE);

    public static final Codec<ToolHeadFamily> CODEC = StringRepresentable.fromEnum(ToolHeadFamily::values);
    public static final StreamCodec<ByteBuf, ToolHeadFamily> STREAM_CODEC =
            ByteBufCodecs.idMapper(ToolHeadFamily::byId, ToolHeadFamily::ordinal);
    private static final ToolHeadFamily[] VALUES = values();

    private final double speedScale;
    private final double durabilityScale;
    private final double attackSpeed;
    private final int feUsage;
    private final int durabilityProtectionFeCost;
    private final TargetType targetType;
    private final String serializedName;

    ToolHeadFamily(
            double speedScale,
            double durabilityScale,
            double attackSpeed,
            int feUsage,
            int durabilityProtectionFeCost,
            TargetType targetType
    ) {
        this.speedScale = speedScale;
        this.durabilityScale = durabilityScale;
        this.attackSpeed = attackSpeed;
        this.feUsage = feUsage;
        this.durabilityProtectionFeCost = durabilityProtectionFeCost;
        this.targetType = targetType;
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public static ToolHeadFamily byId(int id) {
        return VALUES[id];
    }

    public String itemId(ToolHeadMaterial material) {
        return material.serializedName() + "_" + serializedName + "_head";
    }

    public String assembledItemId() {
        return "modular_" + serializedName;
    }

    public String translationKey() {
        return "rngtech.tool_head_family." + serializedName;
    }

    public double speedScale() {
        return speedScale;
    }

    public double durabilityScale() {
        return durabilityScale;
    }

    public double attackSpeed() {
        return attackSpeed;
    }

    public int feUsage() {
        return feUsage;
    }

    public int durabilityProtectionFeCost() {
        return durabilityProtectionFeCost;
    }

    public boolean supportsOreBurst() {
        return this == PICK || this == HAMMER;
    }

    public boolean minesArea() {
        return this == HAMMER || this == DIGGER;
    }

    public boolean requiresTinyAnvil() {
        return minesArea() || this == TREEFELLER;
    }

    public boolean shovelTool() {
        return targetType == TargetType.SHOVEL;
    }

    public boolean validTarget(BlockState state) {
        return switch (targetType) {
            case PICKAXE -> state.is(BlockTags.MINEABLE_WITH_PICKAXE);
            case SHOVEL -> state.is(BlockTags.MINEABLE_WITH_SHOVEL);
            case AXE -> state.is(BlockTags.MINEABLE_WITH_AXE) || state.is(BlockTags.LOGS);
        };
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    private enum TargetType {
        PICKAXE,
        SHOVEL,
        AXE
    }
}
