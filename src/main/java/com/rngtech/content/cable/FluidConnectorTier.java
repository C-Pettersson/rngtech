package com.rngtech.content.cable;

import com.rngtech.RNGTech;

import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum FluidConnectorTier implements StringRepresentable {
    BASIC("basic_fluid_connector", 1, 100, 80, 120, 160, false),
    COPPER("copper_fluid_connector", 2, 250, 40, 80, 120, false),
    GOLD("gold_fluid_connector", 3, 500, 20, 50, 80, false),
    SPARKSTEEL("sparksteel_fluid_connector", 5, 1_000, 10, 20, 60, false),
    ARCLITE("arclite_fluid_connector", 6, 4_000, 4, 10, 40, false),
    DEBUG("debug_fluid_connector", 0, 16_000, 1, 0, 0, true);

    private final String itemId;
    private final int stage;
    private final int fluidPerShipment;
    private final int waitTicks;
    private final int jamChancePerThousand;
    private final int jamTicks;
    private final boolean debugOnly;
    private final String serializedName;

    FluidConnectorTier(
            String itemId,
            int stage,
            int fluidPerShipment,
            int waitTicks,
            int jamChancePerThousand,
            int jamTicks,
            boolean debugOnly
    ) {
        this.itemId = itemId;
        this.stage = stage;
        this.fluidPerShipment = fluidPerShipment;
        this.waitTicks = waitTicks;
        this.jamChancePerThousand = jamChancePerThousand;
        this.jamTicks = jamTicks;
        this.debugOnly = debugOnly;
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public String itemId() {
        return itemId;
    }

    public int stage() {
        return stage;
    }

    public int fluidPerShipment() {
        return fluidPerShipment;
    }

    public int waitTicks() {
        return waitTicks;
    }

    public int jamChancePerThousand() {
        return jamChancePerThousand;
    }

    public int jamTicks() {
        return jamTicks;
    }

    public boolean debugOnly() {
        return debugOnly;
    }

    public boolean enabled() {
        return !debugOnly || RNGTech.isDebugContentEnabled();
    }

    public String translationKey() {
        return "rngtech.fluid_connector_tier." + serializedName;
    }

    public static Optional<FluidConnectorTier> bySerializedName(String name) {
        return Arrays.stream(values())
                .filter(tier -> tier.serializedName.equals(name))
                .findFirst();
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
