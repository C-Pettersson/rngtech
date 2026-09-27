package com.rngtech.content.machine;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum CompressorTankMaterial implements StringRepresentable {
    IRON(1, 16, 0, 1, 0, 0, 0, 0),
    COPPER(2, 24, 0, 1, 0, 0, 0, 0),
    BRONZE(3, 32, 0, 1, 0, 0, 0, 0),
    STEEL(4, 48, 48, 10, 150, 200, 2400, 256),
    ALUMINUM(5, 64, 64, 16, 250, 320, 4000, 512),
    TITANIUM(6, 96, 96, 24, 400, 480, 8000, 1024),
    TUNGSTENSTEEL(7, 128, 128, 32, 700, 640, 16000, 2048),
    EXOTIC(8, 192, 192, 48, 1000, 960, 32000, 4096);

    private final int stage;
    private final int looseBuckets;
    private final int compressedPhysicalBuckets;
    private final int compressionRatio;
    private final int baseRate;
    private final int compressFePerBucket;
    private final int energyCapacity;
    private final int energyTransfer;
    private final String serializedName;

    CompressorTankMaterial(
            int stage,
            int looseBuckets,
            int compressedPhysicalBuckets,
            int compressionRatio,
            int baseRate,
            int compressFePerBucket,
            int energyCapacity,
            int energyTransfer
    ) {
        this.stage = stage;
        this.looseBuckets = looseBuckets;
        this.compressedPhysicalBuckets = compressedPhysicalBuckets;
        this.compressionRatio = compressionRatio;
        this.baseRate = baseRate;
        this.compressFePerBucket = compressFePerBucket;
        this.energyCapacity = energyCapacity;
        this.energyTransfer = energyTransfer;
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public int stage() {
        return stage;
    }

    public int looseBuckets() {
        return looseBuckets;
    }

    public int compressedPhysicalBuckets() {
        return compressedPhysicalBuckets;
    }

    public int compressionRatio() {
        return compressionRatio;
    }

    public int baseRate() {
        return baseRate;
    }

    public int compressFePerBucket() {
        return compressFePerBucket;
    }

    public int decompressFePerBucket() {
        return (int) Math.ceil(compressFePerBucket * 0.25D);
    }

    public int energyCapacity() {
        return energyCapacity;
    }

    public int energyTransfer() {
        return energyTransfer;
    }

    public boolean supportsCompression() {
        return compressedPhysicalBuckets > 0 && compressionRatio > 1;
    }

    public String blockId() {
        return serializedName + "_compressor_tank";
    }

    public String translationKey() {
        return "rngtech.compressor_tank_material." + serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
