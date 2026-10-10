package com.rngtech.content.item;

import com.rngtech.rpg.MachinePartType;

import net.minecraft.world.item.ItemStack;

/**
 * Part type and stage for Gear slot checks. Unique parts count as their host type at their slot stage, so machines accept
 * them wherever a normal part of that type and stage fits.
 */
public final class GearParts {
    private GearParts() {
    }

    public static boolean is(ItemStack stack, MachinePartType type) {
        return stack.getItem() instanceof MachinePartItem part && part.partType() == type;
    }

    /** The part's stage for stage gates: its material stage, or a Unique's slot stage; 0 for anything else. */
    public static int stage(ItemStack stack) {
        return stack.getItem() instanceof MachinePartItem part ? part.traitRollComponentStage() : 0;
    }

    public static boolean is(ItemStack stack, MachinePartType type, int maxStage) {
        return is(stack, type) && stage(stack) <= maxStage;
    }

    /** The highest solid fuel tier an installed Heat Core burns. */
    public static int heatCoreFuelTier(ItemStack stack) {
        if (stack.getItem() instanceof SolidFuelBurnerPartItem part) {
            return part.maxFuelTier();
        }
        return stack.getItem() instanceof UniquePartItem unique && unique.partType() == MachinePartType.HEAT_CORE ? unique.maxFuelTier() : 0;
    }

    public static boolean isUnique(ItemStack stack) {
        return stack.getItem() instanceof UniquePartItem;
    }
}
