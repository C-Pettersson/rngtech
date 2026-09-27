package com.rngtech.content.tool;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum ToolRodMaterial implements StringRepresentable {
    WOODEN(0, 0, 64, -0.2, -0.15, 0.70, 0.65, 4, 4, 0, false, "wood"),
    IRON(1, 1, 160, 0.0, 0.00, 1.00, 0.90, 32, 8, 0, false, "iron"),
    COPPER_CONDUIT(2, 2, 130, 0.2, 0.05, 0.86, 0.88, 64, 10, -2, true, "copper"),
    BRONZE(3, 3, 220, 0.1, 0.03, 1.08, 1.02, 64, 14, -1, false, "bronze"),
    STEEL_REINFORCED(4, 4, 420, -0.3, -0.08, 1.22, 1.12, 96, 18, 0, false, "steel"),
    ALUMINUM(5, 5, 260, 0.7, 0.18, 0.95, 1.05, 96, 18, 0, false, "aluminum"),
    SPARKSTEEL_ROUTED(5, 5, 300, 0.3, 0.12, 1.02, 1.08, 160, 20, -4, true, "sparksteel"),
    TITANIUM_STABILIZED(6, 6, 520, 0.3, 0.10, 1.32, 1.24, 192, 26, -1, false, "titanium"),
    TUNGSTENSTEEL_HEAVY(7, 7, 900, -0.5, -0.18, 1.45, 1.05, 256, 40, 2, false, "tungstensteel"),
    NULLITE_PHASE(7, 7, 650, 0.8, 0.22, 1.25, 1.38, 512, 42, -5, true, "nullite"),
    EXOTIC_HARMONIC(8, 8, 1100, 0.6, 0.20, 1.55, 1.45, 1024, 56, -6, true, "naquadah");

    public static final Codec<ToolRodMaterial> CODEC = StringRepresentable.fromEnum(ToolRodMaterial::values);
    public static final StreamCodec<ByteBuf, ToolRodMaterial> STREAM_CODEC =
            ByteBufCodecs.idMapper(ToolRodMaterial::byId, ToolRodMaterial::ordinal);
    private static final ToolRodMaterial[] VALUES = values();

    private final int stage;
    private final int batterySupport;
    private final int durability;
    private final double miningSpeed;
    private final double attackSpeed;
    private final double stability;
    private final double control;
    private final int feTransfer;
    private final int treeLimitBonus;
    private final int feUsageAdjustment;
    private final boolean oreBurst;
    private final String materialId;
    private final String serializedName;

    ToolRodMaterial(
            int stage,
            int batterySupport,
            int durability,
            double miningSpeed,
            double attackSpeed,
            double stability,
            double control,
            int feTransfer,
            int treeLimitBonus,
            int feUsageAdjustment,
            boolean oreBurst,
            String materialId
    ) {
        this.stage = stage;
        this.batterySupport = batterySupport;
        this.durability = durability;
        this.miningSpeed = miningSpeed;
        this.attackSpeed = attackSpeed;
        this.stability = stability;
        this.control = control;
        this.feTransfer = feTransfer;
        this.treeLimitBonus = treeLimitBonus;
        this.feUsageAdjustment = feUsageAdjustment;
        this.oreBurst = oreBurst;
        this.materialId = materialId;
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public static ToolRodMaterial byId(int id) {
        return VALUES[id];
    }

    public String itemId() {
        return serializedName + "_tool_rod";
    }

    public int stage() {
        return stage;
    }

    public int batterySupport() {
        return batterySupport;
    }

    public int durability() {
        return durability;
    }

    public double miningSpeed() {
        return miningSpeed;
    }

    public double attackSpeed() {
        return attackSpeed;
    }

    public double stability() {
        return stability;
    }

    public double control() {
        return control;
    }

    public int feTransfer() {
        return feTransfer;
    }

    public int treeLimitBonus() {
        return treeLimitBonus;
    }

    public int feUsageAdjustment() {
        return feUsageAdjustment;
    }

    public boolean oreBurst() {
        return oreBurst;
    }

    public String materialId() {
        return materialId;
    }

    public String translationKey() {
        return "rngtech.tool_rod_material." + serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
