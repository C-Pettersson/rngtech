package com.rngtech.content.cable;

import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum ItemConnectorMode implements StringRepresentable {
    INPUT(true, false),
    OUTPUT(false, true);

    private final boolean pullsFromTarget;
    private final boolean insertsIntoTarget;
    private final String serializedName;

    ItemConnectorMode(boolean pullsFromTarget, boolean insertsIntoTarget) {
        this.pullsFromTarget = pullsFromTarget;
        this.insertsIntoTarget = insertsIntoTarget;
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public boolean pullsFromTarget() {
        return pullsFromTarget;
    }

    public boolean insertsIntoTarget() {
        return insertsIntoTarget;
    }

    public ItemConnectorMode next() {
        ItemConnectorMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public String translationKey() {
        return "rngtech.item_connector.mode." + serializedName;
    }

    public static Optional<ItemConnectorMode> bySerializedName(String name) {
        return Arrays.stream(values())
                .filter(mode -> mode.serializedName.equals(name))
                .findFirst();
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
