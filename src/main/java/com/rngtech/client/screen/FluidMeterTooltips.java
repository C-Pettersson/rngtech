package com.rngtech.client.screen;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

final class FluidMeterTooltips {
    private FluidMeterTooltips() {
    }

    static MutableComponent amount(Component fluidName, int amount, int capacity) {
        return Component.empty().append(fluidName).append(": " + CompactValueText.fluidAmountPair(amount, capacity));
    }

    static Component amount(Component fluidName, int amount, int capacity, Component detail) {
        return amount(fluidName, amount, capacity).append(", ").append(detail);
    }

    static Component amount(Component fluidName, int amount, int capacity, String detail) {
        return amount(fluidName, amount, capacity).append(", " + detail);
    }
}
