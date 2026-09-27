package com.rngtech.content.tool;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum PruningShearsMaterial implements StringRepresentable {
    IRON(ToolHeadMaterial.IRON),
    COPPER(ToolHeadMaterial.COPPER),
    BRONZE(ToolHeadMaterial.BRONZE),
    STEEL(ToolHeadMaterial.STEEL),
    ALUMINUM(ToolHeadMaterial.ALUMINUM),
    TITANIUM(ToolHeadMaterial.TITANIUM),
    TUNGSTENSTEEL(ToolHeadMaterial.TUNGSTENSTEEL),
    NULLITE(ToolHeadMaterial.NULLITE),
    EXOTIC(ToolHeadMaterial.EXOTIC);

    public static final Codec<PruningShearsMaterial> CODEC = StringRepresentable.fromEnum(PruningShearsMaterial::values);
    public static final StreamCodec<ByteBuf, PruningShearsMaterial> STREAM_CODEC =
            ByteBufCodecs.idMapper(PruningShearsMaterial::byId, PruningShearsMaterial::ordinal);
    private static final PruningShearsMaterial[] VALUES = values();
    private static final int BASE_CART_LEAF_BUDGET = 12;
    private static final int CART_LEAF_BUDGET_PER_STAGE = 4;

    private final ToolHeadMaterial toolMaterial;
    private final String serializedName;

    PruningShearsMaterial(ToolHeadMaterial toolMaterial) {
        this.toolMaterial = toolMaterial;
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public static PruningShearsMaterial byId(int id) {
        return VALUES[id];
    }

    public ToolHeadMaterial toolMaterial() {
        return toolMaterial;
    }

    public int stage() {
        return toolMaterial.stage();
    }

    public int durability() {
        return Math.max(1, toolMaterial.durability() * 2);
    }

    public int cartLeafWearBudget() {
        return BASE_CART_LEAF_BUDGET + Math.max(0, stage() - 1) * CART_LEAF_BUDGET_PER_STAGE;
    }

    public String materialId() {
        return toolMaterial.materialId();
    }

    public String itemId() {
        return serializedName + "_pruning_shears";
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
