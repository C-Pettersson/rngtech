package com.rngtech.content.menu;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

final class FluidMenuSupport {
    private FluidMenuSupport() {
    }

    static Component fluidName(int fluidId, Component emptyName) {
        Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
        if (fluid == null || fluid == Fluids.EMPTY) {
            return emptyName;
        }
        return new FluidStack(fluid, 1).getHoverName();
    }
}
