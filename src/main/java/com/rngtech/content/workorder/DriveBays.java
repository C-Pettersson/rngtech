package com.rngtech.content.workorder;

import com.rngtech.content.item.CompletedJobChipItem;
import com.rngtech.content.item.JobChipItem;
import com.rngtech.content.menu.WorkOrderMenuView;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.workorder.LoadedWorkOrder;
import com.rngtech.rpg.workorder.MachineWorkOrders;
import com.rngtech.rpg.workorder.WorkOrder;
import com.rngtech.rpg.workorder.WorkOrderHost;
import com.rngtech.rpg.workorder.WorkOrderProgress;
import com.rngtech.rpg.workorder.WorkOrderRules;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A machine's three drive bays as slots over its stored chips. Putting a Job Chip in loads its job with the progress it
 * kept; taking one out unloads it as a chip that keeps its progress, and taking out the active chip leaves none active. A
 * finished job stays in its bay as a Completed Job Chip until the player takes it out.
 * The bays only take chips for the machine's family that it meets. Chips sit in the bays in load order, so taking one
 * out moves the chips after it up a bay.
 */
public final class DriveBays implements IItemHandlerModifiable {
    private final WorkOrderHost host;

    public DriveBays(WorkOrderHost host) {
        this.host = host;
    }

    /** The client side of the bays: a plain three-slot handler that checks chips against the menu's synced machine. */
    public static ItemStackHandler client(WorkOrderMenuView view) {
        return new ItemStackHandler(MachineWorkOrders.SLOTS) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                WorkOrder order = JobChipItem.isWorkOrder(stack) ? JobChipItem.data(stack) : null;
                return order != null && order.family().equals(view.workOrderFamily()) && view.workOrderStage() >= order.minStage()
                        && (order.minMasteryLevel() <= 0 || view.workOrderMasteryLevel() >= order.minMasteryLevel());
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };
    }

    /** The chip in a bay: a Completed Job Chip once the job is done, else a Job Chip carrying its progress. */
    private ItemStack chip(LoadedWorkOrder loaded) {
        if (loaded.fulfilled()) {
            return CompletedJobChipItem.of(loaded, host.workOrderAscendancy());
        }
        ItemStack stack = JobChipItem.of(ModItems.JOB_CHIP.get(), loaded.order(), loaded.traits());
        WorkOrderProgress progress = WorkOrderProgress.of(loaded);
        if (!progress.isNone()) {
            stack.set(ModDataComponents.WORK_ORDER_PROGRESS.get(), progress);
        }
        return stack;
    }

    /** The job a chip holds, loaded with the progress it kept, or null for a blank chip or another item. */
    private static LoadedWorkOrder loaded(ItemStack stack) {
        WorkOrder order = JobChipItem.isWorkOrder(stack) ? JobChipItem.data(stack) : null;
        if (order == null) {
            return null;
        }
        return stack.getOrDefault(ModDataComponents.WORK_ORDER_PROGRESS.get(), WorkOrderProgress.NONE).load(order, JobChipItem.traits(stack));
    }

    @Override
    public int getSlots() {
        return MachineWorkOrders.SLOTS;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        MachineWorkOrders orders = host.workOrders();
        return slot >= 0 && slot < orders.orders().size() ? chip(orders.orders().get(slot)) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        MachineWorkOrders orders = host.workOrders();
        LoadedWorkOrder chip = loaded(stack);
        if (slot < orders.orders().size() || chip == null || !isItemValid(slot, stack)) {
            return stack;
        }
        if (!simulate) {
            host.setWorkOrders(orders.withLoaded(chip));
        }
        return stack.copyWithCount(stack.getCount() - 1);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        MachineWorkOrders orders = host.workOrders();
        if (amount <= 0 || slot < 0 || slot >= orders.orders().size()) {
            return ItemStack.EMPTY;
        }
        ItemStack chip = chip(orders.orders().get(slot));
        if (!simulate) {
            host.setWorkOrders(orders.without(slot));
        }
        return chip;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        MachineWorkOrders orders = host.workOrders();
        LoadedWorkOrder chip = loaded(stack);
        if (stack.isEmpty()) {
            host.setWorkOrders(orders.without(slot));
        } else if (chip != null && slot < orders.orders().size()) {
            host.setWorkOrders(orders.withReplaced(slot, chip));
        } else if (chip != null && isItemValid(slot, stack)) {
            host.setWorkOrders(orders.withLoaded(chip));
        }
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        WorkOrder order = JobChipItem.isWorkOrder(stack) ? JobChipItem.data(stack) : null;
        LoadedWorkOrder chip = loaded(stack);
        if (chip != null && chip.expired(host.workOrderGameTime())) {
            return false;
        }
        MachineWorkOrders orders = host.workOrders();
        MachineWorkOrders room = slot < orders.orders().size() ? orders.without(slot) : orders;
        return WorkOrderRules.loadFailure(room, host.workOrderFamily(), host.workOrderStage(), host.workOrderMasteryLevel(), order) == null;
    }
}
