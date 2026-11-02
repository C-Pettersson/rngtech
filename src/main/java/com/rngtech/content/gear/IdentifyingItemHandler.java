package com.rngtech.content.gear;

import com.rngtech.content.item.CraftedTraitOutputs;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.function.Supplier;

/**
 * An inventory that holds installed Gear. A single unidentified item rolls its traits when a player puts it in a Gear
 * slot, on the server only. Menu slots and Shift-click set the stack; cables, hoppers, and other automation insert it,
 * which never rolls, so automation identifies only through the Identifier. Loading from NBT never rolls either.
 */
public class IdentifyingItemHandler extends ItemStackHandler {
    private final Supplier<Level> level;

    public IdentifyingItemHandler(int size, Supplier<Level> level) {
        super(size);
        this.level = level;
    }

    /** Whether an item in {@code slot} is installed Gear. Inventories that mix Gear with other slots override it. */
    protected boolean identifiesSlot(int slot) {
        return true;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        super.setStackInSlot(slot, stack);
        identify(slot);
    }

    private void identify(int slot) {
        Level currentLevel = level.get();
        if (currentLevel == null || currentLevel.isClientSide() || !identifiesSlot(slot)) {
            return;
        }
        ItemStack stack = getStackInSlot(slot);
        if (stack.getCount() != 1 || !CraftedTraitOutputs.isUnidentified(stack)) {
            return;
        }
        // A new stack, so caches that compare installed stacks by identity see the change.
        ItemStack identified = CraftedTraitOutputs.rollIfMissing(stack.copy(), currentLevel.random);
        if (!CraftedTraitOutputs.isUnidentified(identified)) {
            super.setStackInSlot(slot, identified);
        }
    }
}
