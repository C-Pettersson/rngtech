package com.rngtech.content.configurator;

public enum ConfiguratorTargetType {
    UNIVERSAL_CONNECTOR,
    CABLE_UNIVERSAL_CONNECTOR,
    CABLE_ENERGY_CONNECTOR;

    public static ConfiguratorTargetType byOrdinal(int ordinal) {
        ConfiguratorTargetType[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : UNIVERSAL_CONNECTOR;
    }

    public String translationKey() {
        return "rngtech.configurator.target." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
