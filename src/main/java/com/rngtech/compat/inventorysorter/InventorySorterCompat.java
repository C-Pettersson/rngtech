package com.rngtech.compat.inventorysorter;

import com.rngtech.content.menu.SortExemptSlots;

import net.minecraft.world.inventory.Slot;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;

/**
 * Keeps Simple Inventory Sorter out of machine inventories. Its middle-click sort treats every slot of one item handler
 * as a single grid: it empties each slot before refilling it, so a slot that refuses the next stack, such as an output
 * or an empty-container slot, loses its contents. Blacklisted slots drop out of sorting and scroll moves, while the
 * player inventory stays sortable inside machine screens.
 */
public final class InventorySorterCompat {
    private static final String MOD_ID = "inventorysorter";
    private static final String SLOT_BLACKLIST = "slotblacklist";

    public static void enqueue(InterModEnqueueEvent event) {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return;
        }
        for (Class<? extends Slot> type : SortExemptSlots.TYPES) {
            String name = type.getName();
            InterModComms.sendTo(MOD_ID, SLOT_BLACKLIST, () -> name);
        }
    }

    private InventorySorterCompat() {
    }
}
