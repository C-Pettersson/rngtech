package com.rngtech.content.menu;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * A slot in a machine's input grid that sort mods may sort, such as the Wooden Composter's compost inputs. Use it only
 * for slots that all take the same items, so a sort can put any stack back in any of them.
 *
 * <p>Add one grid's slots one after another, with no other slot of the same inventory between them: Simple Inventory
 * Sorter sorts every menu slot from the first grid slot of an inventory to the last one. Every other machine slot is a
 * {@link MachineSlot}, which sort mods skip.
 */
public final class MachineGridSlot extends SlotItemHandler {
    public MachineGridSlot(IItemHandler itemHandler, int index, int x, int y) {
        super(itemHandler, index, x, y);
    }
}
