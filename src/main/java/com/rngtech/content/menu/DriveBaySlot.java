package com.rngtech.content.menu;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.BooleanSupplier;

/** A drive bay slot on a machine's Orders tab, shown only while that tab is open. */
public final class DriveBaySlot extends SlotItemHandler {
    public static final int X = 9;
    public static final int Y = 21;
    public static final int STEP = 26;

    private final BooleanSupplier active;

    public DriveBaySlot(IItemHandler bays, int bay, BooleanSupplier active) {
        super(bays, bay, X, Y + bay * STEP);
        this.active = active;
    }

    @Override
    public boolean isActive() {
        return active.getAsBoolean();
    }

    /** One chip per bay. The default probes the limit by emptying and refilling the slot, which would unload the chip. */
    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }
}
