package com.rngtech.content.menu;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

final class FluidMenuSupport {
    private FluidMenuSupport() {
    }

    static Fluid fluid(int fluidId) {
        Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
        return fluid == null ? Fluids.EMPTY : fluid;
    }

    static Component fluidName(int fluidId, Component emptyName) {
        Fluid fluid = fluid(fluidId);
        return fluid == Fluids.EMPTY ? emptyName : new FluidStack(fluid, 1).getHoverName();
    }
}
