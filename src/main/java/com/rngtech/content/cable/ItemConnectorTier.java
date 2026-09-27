package com.rngtech.content.cable;

import com.rngtech.RNGTech;

import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum ItemConnectorTier implements StringRepresentable {
    BASIC("basic_item_connector", 1, 1, 80, 120, 160, false),
    COPPER("copper_item_connector", 2, 4, 40, 80, 120, false),
    GOLD("gold_item_connector", 3, 16, 20, 50, 80, false),
    SPARKSTEEL("sparksteel_item_connector", 5, 32, 10, 20, 60, false),
    ARCLITE("arclite_item_connector", 6, 64, 4, 10, 40, false),
    DEBUG("debug_item_connector", 0, 64, 1, 0, 0, true);

    private final String itemId;
    private final int stage;
    private final int itemsPerShipment;
    private final int waitTicks;
    private final int jamChancePerThousand;
    private final int jamTicks;
    private final boolean debugOnly;
    private final String serializedName;

    ItemConnectorTier(
            String itemId,
            int stage,
            int itemsPerShipment,
            int waitTicks,
            int jamChancePerThousand,
            int jamTicks,
            boolean debugOnly
    ) {
        this.itemId = itemId;
        this.stage = stage;
        this.itemsPerShipment = itemsPerShipment;
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

    public int itemsPerShipment() {
        return itemsPerShipment;
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
        return "rngtech.item_connector_tier." + serializedName;
    }

    public static Optional<ItemConnectorTier> bySerializedName(String name) {
        return Arrays.stream(values())
                .filter(tier -> tier.serializedName.equals(name))
                .findFirst();
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
