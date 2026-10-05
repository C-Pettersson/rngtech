package com.rngtech.content.purge;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

public record FluidPurgeTarget(
        int id,
        Component name,
        FluidPurgeRole role,
        Supplier<FluidStack> fluidSupplier,
        Drain drain,
        Runnable beforePurge,
        Runnable afterPurge,
        IntSupplier capacitySupplier
) {
    private static final Runnable NO_OP = () -> {
    };

    public FluidStack fluid() {
        FluidStack stack = fluidSupplier.get();
        return stack == null ? FluidStack.EMPTY : stack;
    }

    public int capacity() {
        return capacitySupplier == null ? 0 : Math.max(0, capacitySupplier.getAsInt());
    }

    public FluidPurgeTarget withCapacity(IntSupplier capacity) {
        return new FluidPurgeTarget(id, name, role, fluidSupplier, drain, beforePurge, afterPurge, capacity);
    }

    public FluidStack drain(int amount, IFluidHandler.FluidAction action) {
        if (amount <= 0) {
            return FluidStack.EMPTY;
        }
        return drain.drain(amount, action);
    }

    public static FluidPurgeTarget of(
            int id,
            Component name,
            FluidPurgeRole role,
            Supplier<FluidStack> fluidSupplier,
            Drain drain
    ) {
        return of(id, name, role, fluidSupplier, drain, NO_OP, NO_OP);
    }

    public static FluidPurgeTarget of(
            int id,
            Component name,
            FluidPurgeRole role,
            Supplier<FluidStack> fluidSupplier,
            Drain drain,
            Runnable afterPurge
    ) {
        return of(id, name, role, fluidSupplier, drain, NO_OP, afterPurge);
    }

    public static FluidPurgeTarget of(
            int id,
            Component name,
            FluidPurgeRole role,
            Supplier<FluidStack> fluidSupplier,
            Drain drain,
            Runnable beforePurge,
            Runnable afterPurge
    ) {
        return new FluidPurgeTarget(id, name, role, fluidSupplier, drain, beforePurge, afterPurge, null);
    }

    @FunctionalInterface
    public interface Drain {
        FluidStack drain(int amount, IFluidHandler.FluidAction action);
    }
}
