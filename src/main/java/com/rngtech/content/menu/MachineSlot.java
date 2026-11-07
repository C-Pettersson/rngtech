package com.rngtech.content.menu;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * A slot over a machine's own inventory. Sort mods skip it (see {@link SortExemptSlots}): a machine inventory mixes
 * inputs, outputs, containers, and Gear behind tabs, so sorting it moves stacks into the wrong role or deletes the
 * ones a slot refuses. The class is final because sort mods match the exact class name.
 */
public final class MachineSlot extends SlotItemHandler {
    private static final BooleanSupplier ALWAYS = () -> true;

    private final BooleanSupplier active;
    private Predicate<ItemStack> filter;
    private boolean locked;
    private int maxStackSize;

    public MachineSlot(IItemHandler itemHandler, int index, int x, int y) {
        this(itemHandler, index, x, y, ALWAYS);
    }

    /** A slot that is shown and usable only while {@code active} holds, such as one on a single machine tab. */
    public MachineSlot(IItemHandler itemHandler, int index, int x, int y, BooleanSupplier active) {
        super(itemHandler, index, x, y);
        this.active = active;
    }

    /** Narrows what a player may place here, on top of the inventory's own rules. */
    public MachineSlot accepting(Predicate<ItemStack> filter) {
        this.filter = filter;
        return this;
    }

    /** A slot the player can see but not take from or place into. */
    public MachineSlot locked() {
        locked = true;
        return this;
    }

    public MachineSlot singleItem() {
        maxStackSize = 1;
        return this;
    }

    @Override
    public boolean isActive() {
        return active.getAsBoolean();
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (locked) {
            return false;
        }
        return filter == null ? super.mayPlace(stack) : filter.test(stack);
    }

    @Override
    public boolean mayPickup(Player player) {
        return !locked && super.mayPickup(player);
    }

    @Override
    public int getMaxStackSize() {
        return maxStackSize > 0 ? maxStackSize : super.getMaxStackSize();
    }
}
