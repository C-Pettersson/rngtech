package com.rngtech.content.cable;

import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum CableConnectorMode implements StringRepresentable {
    BOTH(true, true),
    INPUT(true, false),
    OUTPUT(false, true);

    private final boolean acceptsNetworkInput;
    private final boolean sendsNetworkOutput;
    private final String serializedName;

    CableConnectorMode(boolean acceptsNetworkInput, boolean sendsNetworkOutput) {
        this.acceptsNetworkInput = acceptsNetworkInput;
        this.sendsNetworkOutput = sendsNetworkOutput;
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public boolean acceptsNetworkInput() {
        return acceptsNetworkInput;
    }

    public boolean sendsNetworkOutput() {
        return sendsNetworkOutput;
    }

    public CableConnectorMode next() {
        CableConnectorMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public String translationKey() {
        return "rngtech.cable_connector.mode." + serializedName;
    }

    public static Optional<CableConnectorMode> bySerializedName(String name) {
        return Arrays.stream(values())
                .filter(mode -> mode.serializedName.equals(name))
                .findFirst();
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
