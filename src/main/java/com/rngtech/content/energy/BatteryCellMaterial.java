package com.rngtech.content.energy;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum BatteryCellMaterial implements StringRepresentable {
    POTATO(0, 1000, 8, 4, 0.25, 12.00),
    IRON(1, 10000, 128, 64, 1.00, 0.00),
    COPPER(2, 12000, 256, 128, 0.98, 0.01),
    LEAD(3, 32000, 256, 128, 1.00, 0.00),
    INVAR(4, 50000, 512, 256, 1.02, 0.00),
    SPARKSTEEL(5, 100000, 1024, 512, 1.04, 0.01),
    ARCLITE(6, 400000, 2048, 1024, 0.99, 0.02),
    NULLITE(7, 1600000, 4096, 2048, 1.02, 0.02),
    AETHERGOLD(7, 1200000, 6144, 3072, 1.03, 0.00),
    EXOTIC(8, 10000000, 8192, 4096, 1.06, 0.00),
    UNIQUE_POTATO(0, 10000, 128, 64, 0.60, 6.00, true);

    public static final Codec<BatteryCellMaterial> CODEC = StringRepresentable.fromEnum(BatteryCellMaterial::values);
    public static final StreamCodec<ByteBuf, BatteryCellMaterial> STREAM_CODEC =
            ByteBufCodecs.idMapper(BatteryCellMaterial::byId, BatteryCellMaterial::ordinal);
    private static final BatteryCellMaterial[] VALUES = values();

    private final int stage;
    private final int capacity;
    private final int inputRate;
    private final int outputRate;
    private final double efficiency;
    private final double idleLossPercentPerMinute;
    private final boolean unique;
    private final String serializedName;

    BatteryCellMaterial(
            int stage,
            int capacity,
            int inputRate,
            int outputRate,
            double efficiency,
            double idleLossPercentPerMinute
    ) {
        this(stage, capacity, inputRate, outputRate, efficiency, idleLossPercentPerMinute, false);
    }

    BatteryCellMaterial(
            int stage,
            int capacity,
            int inputRate,
            int outputRate,
            double efficiency,
            double idleLossPercentPerMinute,
            boolean unique
    ) {
        this.stage = stage;
        this.capacity = capacity;
        this.inputRate = inputRate;
        this.outputRate = outputRate;
        this.efficiency = efficiency;
        this.idleLossPercentPerMinute = idleLossPercentPerMinute;
        this.unique = unique;
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public static BatteryCellMaterial byId(int id) {
        return VALUES[id];
    }

    public int stage() {
        return stage;
    }

    public int capacity() {
        return capacity;
    }

    public int inputRate() {
        return inputRate;
    }

    public int outputRate() {
        return outputRate;
    }

    public double efficiency() {
        return efficiency;
    }

    public double idleLossPercentPerMinute() {
        return idleLossPercentPerMinute;
    }

    public boolean unique() {
        return unique;
    }

    public String itemId() {
        return serializedName + "_battery_cell";
    }

    public String materialId() {
        return this == EXOTIC ? "naquadah" : serializedName;
    }

    public String translationKey() {
        return "rngtech.battery_cell_material." + serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
