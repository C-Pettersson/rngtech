package com.rngtech.content.workorder;

import com.rngtech.rpg.workorder.LoadedWorkOrder;
import com.rngtech.rpg.workorder.MachineWorkOrders;
import com.rngtech.rpg.workorder.WorkOrder;
import com.rngtech.rpg.workorder.WorkOrderHost;
import com.rngtech.rpg.workorder.WorkOrderRules;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.List;

/**
 * The Orders tab's buttons: activate, Rush, pause, or abandon a loaded chip. Chips go in and out through the drive bays, and a
 * finished chip has no buttons: the player takes its Completed Job Chip out.
 */
public final class WorkOrderOperations {
    private WorkOrderOperations() { }

    /** Runs {@code action} on the server. {@code value} is a drive bay. */
    public static boolean perform(Player player, WorkOrderHost host, String action, String value) {
        if (host == null || player.isSpectator() || player.level().isClientSide) { return false; }
        MachineWorkOrders orders = host.workOrders();
        int index = parse(value);
        if (index >= 0 && index < orders.orders().size() && orders.orders().get(index).fulfilled()) {
            player.displayClientMessage(Component.translatable("rngtech.work_order.message.done"), true);
            return false;
        }
        String failure = switch (action) {
            case "activate" -> change(host, orders, orders.withActive(index), index == orders.active());
            case "deactivate" -> change(host, orders, orders.withActive(MachineWorkOrders.NONE), orders.active() == MachineWorkOrders.NONE);
            case "abandon" -> change(host, orders, orders.without(index), false);
            case "rush" -> rush(host, orders, index);
            default -> "unknown";
        };
        if (failure != null) {
            player.displayClientMessage(Component.translatable("rngtech.work_order.message." + failure), true);
            return false;
        }
        player.displayClientMessage(Component.translatable("rngtech.work_order.message." + action), true);
        return true;
    }

    /**
     * Activates chip {@code index} under Rush: its deadline is the job's Rush clock from now, and its listed reward, or its
     * Bounty when it has no list, grows by the Rush bonus with Short Fuse's. A chip that already has Rush keeps its deadline.
     */
    private static String rush(WorkOrderHost host, MachineWorkOrders orders, int index) {
        if (index < 0 || index >= orders.orders().size()) { return "slot"; }
        LoadedWorkOrder loaded = orders.orders().get(index);
        if (loaded.rushed()) { return "rushed"; }
        WorkOrder order = loaded.order();
        int bonus = WorkOrderRules.rushBonus(loaded.traits());
        List<ItemStack> listed = WorkOrderRewards.items(order.rewards());
        WorkOrder boosted = listed.isEmpty() ? order.withBounty(WorkOrder.rushAmount(order.bounty(), bonus))
                : order.withRewards(ItemContainerContents.fromItems(listed.stream()
                        .map(stack -> stack.copyWithCount(WorkOrder.rushAmount(stack.getCount(), bonus))).toList()));
        LoadedWorkOrder rushed = loaded.withOrder(boosted).withRush(host.workOrderGameTime() + WorkOrderRules.rushTicks(order, loaded.traits()));
        host.setWorkOrders(orders.withReplaced(index, rushed).withActive(index));
        return null;
    }

    private static String change(WorkOrderHost host, MachineWorkOrders before, MachineWorkOrders after, boolean unchangedIsFine) {
        if (after == before) { return unchangedIsFine ? null : "slot"; }
        host.setWorkOrders(after);
        return null;
    }

    private static int parse(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }
}
