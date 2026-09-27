package com.rngtech.content.energy;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum SolidFuelBurnerChassis implements StringRepresentable {
    CRUDE("crude_solid_fuel_burner", 1, 1, 80, 0.75),
    COPPER("copper_solid_fuel_burner", 2, 2, 128, 0.90),
    ALLOY("alloy_solid_fuel_burner", 3, 3, 160, 1.00),
    STEEL("steel_solid_fuel_burner", 4, 4, 192, 1.15);

    public static final Codec<SolidFuelBurnerChassis> CODEC = StringRepresentable.fromEnum(SolidFuelBurnerChassis::values);
    public static final StreamCodec<ByteBuf, SolidFuelBurnerChassis> STREAM_CODEC =
            ByteBufCodecs.idMapper(SolidFuelBurnerChassis::byId, SolidFuelBurnerChassis::ordinal);
    private static final SolidFuelBurnerChassis[] VALUES = values();

    private final String blockId;
    private final int stage;
    private final int maxPartStage;
    private final int transferRate;
    private final double stability;
    private final String serializedName;

    SolidFuelBurnerChassis(String blockId, int stage, int maxPartStage, int transferRate, double stability) {
        this.blockId = blockId;
        this.stage = stage;
        this.maxPartStage = maxPartStage;
        this.transferRate = transferRate;
        this.stability = stability;
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public static SolidFuelBurnerChassis byId(int id) {
        return VALUES[id];
    }

    public String blockId() {
        return blockId;
    }

    public int stage() {
        return stage;
    }

    public int maxPartStage() {
        return maxPartStage;
    }

    public int transferRate() {
        return transferRate;
    }

    public double stability() {
        return stability;
    }

    public String translationKey() {
        return "rngtech.solid_fuel_burner_chassis." + serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
