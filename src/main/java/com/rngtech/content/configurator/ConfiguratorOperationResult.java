package com.rngtech.content.configurator;

import net.minecraft.network.chat.Component;

public record ConfiguratorOperationResult(boolean success, Component message) {
    public static ConfiguratorOperationResult success(String translationKey) {
        return new ConfiguratorOperationResult(true, Component.translatable(translationKey));
    }

    public static ConfiguratorOperationResult success(String translationKey, Object... args) {
        return new ConfiguratorOperationResult(true, Component.translatable(translationKey, args));
    }

    public static ConfiguratorOperationResult fail(String translationKey) {
        return new ConfiguratorOperationResult(false, Component.translatable(translationKey));
    }
}
