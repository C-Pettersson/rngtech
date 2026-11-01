package com.rngtech.content.jewel;

import com.rngtech.content.item.CraftedTraitOutputs;
import com.rngtech.content.item.JewelItem;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineModifierEffect;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.ModifierOperation;
import com.rngtech.rpg.jewel.JewelHost;
import com.rngtech.rpg.jewel.MachineJewels;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A machine's jewel slots as an item handler over its stored jewels: the tree row first, then the body row. The slots
 * themselves decide which are open; this view only moves jewels in and out of the component.
 */
public final class JewelSockets implements IItemHandlerModifiable {
    public static final int TREE_START = 0;
    public static final int BODY_START = MachineJewels.TREE_SLOTS;
    public static final int SLOTS = MachineJewels.TREE_SLOTS + MachineJewels.BODY_SLOTS;

    private final JewelHost host;

    public JewelSockets(JewelHost host) {
        this.host = host;
    }

    /** The client side: a plain handler the menu fills from its slots, taking only identified jewels. */
    public static ItemStackHandler client() {
        return new ItemStackHandler(SLOTS) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return accepts(stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };
    }

    /** Body sockets a body's traits grant: every flat Jewel Sockets effect, affix or identity, up to the body row. */
    public static int bodySockets(MachineTraits traits) {
        double sockets = 0.0;
        for (MachineModifier modifier : traits.activeModifiers()) {
            for (MachineModifierEffect effect : modifier.effects()) {
                if (effect.stat() == MachineStat.JEWEL_SOCKETS && effect.operation() == ModifierOperation.ADD) {
                    sockets += effect.value();
                }
            }
        }
        return Math.max(0, Math.min(MachineJewels.BODY_SLOTS, (int) Math.floor(sockets)));
    }

    /** Only an identified jewel: an Unidentified one has no lines to read until the player identifies it. */
    public static boolean accepts(ItemStack stack) {
        return stack.getItem() instanceof JewelItem && !CraftedTraitOutputs.isUnidentified(stack);
    }

    @Override
    public int getSlots() {
        return SLOTS;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        MachineJewels jewels = host.jewels();
        return slot < BODY_START ? jewels.tree(slot) : jewels.body(slot - BODY_START);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        MachineJewels jewels = host.jewels();
        host.setJewels(slot < BODY_START ? jewels.withTree(slot, stack) : jewels.withBody(slot - BODY_START, stack));
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !isItemValid(slot, stack) || !getStackInSlot(slot).isEmpty()) {
            return stack;
        }
        if (!simulate) {
            setStackInSlot(slot, stack.copyWithCount(1));
        }
        return stack.copyWithCount(stack.getCount() - 1);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack current = getStackInSlot(slot);
        if (current.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        if (!simulate) {
            setStackInSlot(slot, ItemStack.EMPTY);
        }
        return current.copy();
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return accepts(stack);
    }
}
