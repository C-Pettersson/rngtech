package com.rngtech.content.purge;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;

public record FluidPurgeResult(
        boolean supported,
        boolean success,
        int targetId,
        Component targetName,
        FluidStack fluid,
        int amount
) {
    public static FluidPurgeResult unsupported() {
        return new FluidPurgeResult(false, false, -1, Component.empty(), FluidStack.EMPTY, 0);
    }

    public static FluidPurgeResult empty() {
        return new FluidPurgeResult(true, false, -1, Component.empty(), FluidStack.EMPTY, 0);
    }

    public static FluidPurgeResult empty(FluidPurgeTarget target) {
        return new FluidPurgeResult(true, false, target.id(), target.name(), FluidStack.EMPTY, 0);
    }

    public static FluidPurgeResult success(FluidPurgeTarget target, FluidStack drained) {
        return new FluidPurgeResult(true, true, target.id(), target.name(), drained.copy(), drained.getAmount());
    }
}
