package com.rngtech.content.blockentity;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Spreads an inserted stack across a run of lane input slots: each item goes to the emptiest lane that can take it, so a
 * trickle or a bulk shipment fills every lane instead of lane 1 first. A lane holding a different item is never used.
 */
final class EvenLaneFeed {
    private EvenLaneFeed() {
    }

    static ItemStack insert(IItemHandler inventory, int firstSlot, int lanes, ItemStack stack, boolean simulate) {
        int[] counts = new int[lanes];
        int[] room = new int[lanes];
        for (int lane = 0; lane < lanes; lane++) {
            int slot = firstSlot + lane;
            ItemStack existing = inventory.getStackInSlot(slot);
            counts[lane] = existing.getCount();
            if (existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, stack)) {
                room[lane] = Math.max(0, Math.min(inventory.getSlotLimit(slot), stack.getMaxStackSize()) - existing.getCount());
            }
        }
        int[] added = new int[lanes];
        int remaining = stack.getCount();
        while (remaining > 0) {
            int best = -1;
            for (int lane = 0; lane < lanes; lane++) {
                if (added[lane] < room[lane] && (best < 0 || counts[lane] + added[lane] < counts[best] + added[best])) {
                    best = lane;
                }
            }
            if (best < 0) {
                break;
            }
            added[best]++;
            remaining--;
        }
        if (!simulate) {
            for (int lane = 0; lane < lanes; lane++) {
                if (added[lane] > 0) {
                    inventory.insertItem(firstSlot + lane, stack.copyWithCount(added[lane]), false);
                }
            }
        }
        return remaining == stack.getCount() ? stack : stack.copyWithCount(remaining);
    }
}
