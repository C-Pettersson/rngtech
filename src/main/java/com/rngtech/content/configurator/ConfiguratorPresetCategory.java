package com.rngtech.content.configurator;

public enum ConfiguratorPresetCategory {
    ALL,
    ENERGY,
    FLUID,
    ITEM;

    public ConfiguratorPresetCategory next() {
        ConfiguratorPresetCategory[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public String translationKey() {
        return "rngtech.configurator.category." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
