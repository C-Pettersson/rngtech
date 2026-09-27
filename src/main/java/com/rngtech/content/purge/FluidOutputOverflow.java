package com.rngtech.content.purge;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public final class FluidOutputOverflow {
    public static boolean canAcceptOrVoidExcess(FluidTank tank, FluidStack output, boolean voidExcess) {
        if (output.isEmpty()) {
            return true;
        }
        if (!tank.isFluidValid(output)) {
            return false;
        }
        if (!voidExcess) {
            return tank.fill(output, IFluidHandler.FluidAction.SIMULATE) >= output.getAmount();
        }
        return tank.isEmpty() || FluidStack.isSameFluidSameComponents(output, tank.getFluid());
    }

    private FluidOutputOverflow() {
    }
}
