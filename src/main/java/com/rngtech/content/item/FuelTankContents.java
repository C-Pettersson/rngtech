package com.rngtech.content.item;

import com.rngtech.content.registry.ModDataComponents;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** The fuel a Fuel Tank or Gas Jet holds. It stays on the part, so removing the part never loses fluid. */
public final class FuelTankContents {
    private FuelTankContents() {
    }

    public static FluidStack fluid(ItemStack stack) {
        TankContents contents = stack.get(ModDataComponents.TANK_CONTENTS.get());
        return contents == null ? FluidStack.EMPTY : contents.loose().copy();
    }

    public static void set(ItemStack stack, FluidStack fluid) {
        if (fluid.isEmpty()) {
            stack.remove(ModDataComponents.TANK_CONTENTS.get());
        } else {
            stack.set(ModDataComponents.TANK_CONTENTS.get(), TankContents.of(fluid, FluidStack.EMPTY));
        }
    }
}
