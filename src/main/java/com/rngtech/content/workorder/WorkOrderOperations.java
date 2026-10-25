package com.rngtech.content.workorder;

import com.rngtech.rpg.workorder.MachineWorkOrders;
import com.rngtech.rpg.workorder.WorkOrderHost;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * The Orders tab's buttons: activate, pause, or abandon a loaded chip. Chips go in and out through the drive bays, and a
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
            default -> "unknown";
        };
        if (failure != null) {
            player.displayClientMessage(Component.translatable("rngtech.work_order.message." + failure), true);
            return false;
        }
        player.displayClientMessage(Component.translatable("rngtech.work_order.message." + action), true);
        return true;
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
