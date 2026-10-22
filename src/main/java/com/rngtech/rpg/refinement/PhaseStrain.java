package com.rngtech.rpg.refinement;

import com.rngtech.content.registry.ModDataComponents;

import net.minecraft.world.item.ItemStack;

/** Strain on an item: one point per Phase Array pulse, read against the line's Tolerance in the Fracture check. */
public final class PhaseStrain {
    private PhaseStrain() {
    }

    public static int get(ItemStack stack) {
        Integer strain = stack.get(ModDataComponents.STRAIN.get());
        return strain == null ? 0 : strain;
    }

    public static void set(ItemStack stack, int strain) {
        if (strain <= 0) {
            stack.remove(ModDataComponents.STRAIN.get());
        } else {
            stack.set(ModDataComponents.STRAIN.get(), strain);
        }
    }

    public static void add(ItemStack stack, int points) {
        set(stack, get(stack) + points);
    }
}
