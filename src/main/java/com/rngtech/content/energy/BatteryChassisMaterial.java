package com.rngtech.content.energy;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum BatteryChassisMaterial implements StringRepresentable {
    WOODEN(0, 1, 16, 1.0, 0, 0.80, 0.25, 0.10, 1.50),
    IRON(1, 1, 128, 1.0, 0, 1.00, 1.00, 0.00, 1.30),
    COPPER(2, 2, 192, 1.1, 20, 0.96, 0.95, 0.02, 1.20),
    GOLD(3, 3, 384, 2.5, 40, 0.92, 0.80, 0.05, 1.10),
    STEEL(4, 4, 256, 1.0, 0, 1.00, 1.15, 0.00, 1.00),
    SPARKSTEEL(5, 5, 512, 1.4, 60, 1.04, 1.00, 0.01, 0.95),
    ARCLITE(6, 6, 1024, 2.0, 60, 0.98, 0.90, 0.03, 0.90),
    NULLITE(7, 8, 2048, 2.0, 80, 1.02, 0.95, 0.02, 0.80),
    AETHERGOLD(7, 6, 3072, 4.0, 100, 1.03, 1.05, 0.00, 0.90, true),
    EXOTIC(8, 10, 4096, 1.5, 80, 1.05, 1.20, 0.00, 0.70);

    public static final Codec<BatteryChassisMaterial> CODEC = StringRepresentable.fromEnum(BatteryChassisMaterial::values);
    public static final StreamCodec<ByteBuf, BatteryChassisMaterial> STREAM_CODEC =
            ByteBufCodecs.idMapper(BatteryChassisMaterial::byId, BatteryChassisMaterial::ordinal);
    private static final BatteryChassisMaterial[] VALUES = values();

    private final int stage;
    private final int slots;
    private final int transferRate;
    private final double burstTransfer;
    private final int burstDuration;
    private final double efficiency;
    private final double stability;
    private final double idleLossPercentPerMinute;
    private final double globalModifierStrength;
    private final boolean sealsCellLeakage;
    private final String serializedName;

    BatteryChassisMaterial(
            int stage,
            int slots,
            int transferRate,
            double burstTransfer,
            int burstDuration,
            double efficiency,
            double stability,
            double idleLossPercentPerMinute,
            double globalModifierStrength
    ) {
        this(
                stage,
                slots,
                transferRate,
                burstTransfer,
                burstDuration,
                efficiency,
                stability,
                idleLossPercentPerMinute,
                globalModifierStrength,
                false
        );
    }

    BatteryChassisMaterial(
            int stage,
            int slots,
            int transferRate,
            double burstTransfer,
            int burstDuration,
            double efficiency,
            double stability,
            double idleLossPercentPerMinute,
            double globalModifierStrength,
            boolean sealsCellLeakage
    ) {
        this.stage = stage;
        this.slots = slots;
        this.transferRate = transferRate;
        this.burstTransfer = burstTransfer;
        this.burstDuration = burstDuration;
        this.efficiency = efficiency;
        this.stability = stability;
        this.idleLossPercentPerMinute = idleLossPercentPerMinute;
        this.globalModifierStrength = globalModifierStrength;
        this.sealsCellLeakage = sealsCellLeakage;
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public static BatteryChassisMaterial byId(int id) {
        return VALUES[id];
    }

    public int stage() {
        return stage;
    }

    public int slots() {
        return slots;
    }

    public int transferRate() {
        return transferRate;
    }

    public double burstTransfer() {
        return burstTransfer;
    }

    public int burstDuration() {
        return burstDuration;
    }

    public double efficiency() {
        return efficiency;
    }

    public double stability() {
        return stability;
    }

    public double idleLossPercentPerMinute() {
        return idleLossPercentPerMinute;
    }

    public double globalModifierStrength() {
        return globalModifierStrength;
    }

    public boolean sealsCellLeakage() {
        return sealsCellLeakage;
    }

    public String blockId() {
        return serializedName + "_battery_chassis";
    }

    public String materialId() {
        return this == EXOTIC ? "naquadah" : serializedName;
    }

    public String translationKey() {
        return "rngtech.battery_chassis_material." + serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
