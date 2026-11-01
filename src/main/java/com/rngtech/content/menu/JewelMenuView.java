package com.rngtech.content.menu;

import net.minecraft.world.item.ItemStack;

/** What a Jewels tab shows: how many sockets of each row are open and the jewel in each slot. */
public interface JewelMenuView {
    /** Open tree jewel slots: the machine's allocated Jewel Sockets. */
    int jewelTreeCapacity();

    /** Open body jewel slots, from the body's socket affix and implicit. */
    default int jewelBodyCapacity() {
        return 0;
    }

    /** The jewel in slot {@code index} of the combined tree-then-body row, or empty. */
    ItemStack jewel(int index);

    /** Whether the Jewels tab is the open one, for its slots and renderer. */
    boolean jewelsTabSelected();
}
