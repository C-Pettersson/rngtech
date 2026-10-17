package com.rngtech.content.dormant;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** The ruin built around a Dormant Machine, and how it finds its height. */
public enum DormantLayout implements StringRepresentable {
    WORKSHOP(11, 7, 11, Placement.SURFACE),
    SIDING(5, 5, 13, Placement.UNDERGROUND_SHALLOW),
    VAULT(9, 6, 9, Placement.UNDERGROUND_DEEP),
    FOUNDRY(11, 6, 11, Placement.OCEAN_FLOOR),
    SMELTWORKS(11, 7, 11, Placement.NETHER_GROUND),
    RELAY(9, 9, 9, Placement.END_ISLAND);

    public static final Codec<DormantLayout> CODEC = StringRepresentable.fromEnum(DormantLayout::values);

    public enum Placement { SURFACE, UNDERGROUND_SHALLOW, UNDERGROUND_DEEP, OCEAN_FLOOR, NETHER_GROUND, END_ISLAND }

    private final int width;
    private final int height;
    private final int depth;
    private final Placement placement;

    DormantLayout(int width, int height, int depth, Placement placement) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.placement = placement;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int depth() {
        return depth;
    }

    public Placement placement() {
        return placement;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
