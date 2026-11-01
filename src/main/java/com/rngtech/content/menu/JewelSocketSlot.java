package com.rngtech.content.menu;

import com.rngtech.content.jewel.JewelSockets;
import com.rngtech.rpg.jewel.MachineJewels;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.BooleanSupplier;

/**
 * One jewel slot on a machine's Jewels tab. The tree row sits over the inventory grid's columns, the body row below
 * it. A slot is shown only while the tab is open and the socket behind it exists, so a refunded socket's jewel stays
 * stored but out of reach until the socket is allocated again.
 */
public final class JewelSocketSlot extends SlotItemHandler {
    public static final int X = 38;
    public static final int STEP = 18;
    public static final int TREE_Y = 32;
    public static final int BODY_Y = 68;

    private final BooleanSupplier active;

    public JewelSocketSlot(IItemHandler sockets, int index, BooleanSupplier active) {
        super(sockets, index, X + column(index) * STEP, index < JewelSockets.BODY_START ? TREE_Y : BODY_Y);
        this.active = active;
    }

    private static int column(int index) {
        return index < JewelSockets.BODY_START ? index : index - JewelSockets.BODY_START;
    }

    public static boolean isTreeSlot(int index) {
        return index < MachineJewels.TREE_SLOTS;
    }

    @Override
    public boolean isActive() {
        return active.getAsBoolean();
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }
}
