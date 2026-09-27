package com.rngtech.content.purge;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.List;

public interface PurgeableFluidStorage {
    List<FluidPurgeTarget> fluidPurgeTargets();

    default FluidPurgeResult purgeFluid(int targetId, int maxAmount) {
        FluidPurgeTarget target = fluidPurgeTargets().stream()
                .filter(candidate -> candidate.id() == targetId)
                .findFirst()
                .orElse(null);
        if (target == null) {
            return FluidPurgeResult.unsupported();
        }
        if (maxAmount <= 0 || target.fluid().isEmpty()) {
            return FluidPurgeResult.empty(target);
        }

        int amount = Math.min(maxAmount, target.fluid().getAmount());
        FluidStack simulated = target.drain(amount, IFluidHandler.FluidAction.SIMULATE);
        if (simulated.isEmpty()) {
            return FluidPurgeResult.empty(target);
        }

        target.beforePurge().run();
        FluidStack drained = target.drain(Math.min(amount, simulated.getAmount()), IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return FluidPurgeResult.empty(target);
        }
        target.afterPurge().run();
        return FluidPurgeResult.success(target, drained);
    }

    default FluidPurgeResult purgeFirst(boolean preferOutputs, int maxAmount) {
        List<FluidPurgeTarget> targets = fluidPurgeTargets();
        if (targets.isEmpty()) {
            return FluidPurgeResult.unsupported();
        }

        FluidPurgeRole[] priority = preferOutputs
                ? new FluidPurgeRole[] {FluidPurgeRole.OUTPUT, FluidPurgeRole.STORAGE, FluidPurgeRole.INPUT}
                : new FluidPurgeRole[] {FluidPurgeRole.INPUT, FluidPurgeRole.STORAGE, FluidPurgeRole.OUTPUT};
        for (FluidPurgeRole role : priority) {
            for (FluidPurgeTarget target : targets) {
                if (target.role() == role && !target.fluid().isEmpty()) {
                    return purgeFluid(target.id(), maxAmount);
                }
            }
        }
        return FluidPurgeResult.empty();
    }
}
