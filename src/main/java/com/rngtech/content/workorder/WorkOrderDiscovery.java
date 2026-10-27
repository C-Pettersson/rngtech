package com.rngtech.content.workorder;

import com.rngtech.content.registry.ModAttachments;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.workorder.WorkOrderHost;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;

import java.util.Set;

/**
 * Whether a player has found Work Orders. Machines hide their Orders tab until then. A player finds them by opening a
 * Dispatch Terminal, by opening a machine while carrying a terminal or a chip, or by opening a machine that has a chip
 * loaded. The flag stays for good, through death.
 */
public final class WorkOrderDiscovery {
    private WorkOrderDiscovery() {
    }

    public static boolean found(Player player) {
        return player.getData(ModAttachments.WORK_ORDERS_FOUND);
    }

    public static void find(Player player) {
        if (!found(player)) {
            player.setData(ModAttachments.WORK_ORDERS_FOUND, true);
        }
    }

    /**
     * A machine menu's Orders tab flag, 1 when the tab shows. On the server it reads the player's flag live and sets it
     * when the player carries Work Order items or the machine gets a chip; on the client it holds the synced value.
     */
    public static DataSlot shownSlot(Player player, WorkOrderHost host) {
        if (player.level().isClientSide()) {
            return DataSlot.standalone();
        }
        if (carriesWorkOrders(player)) {
            find(player);
        }
        return new DataSlot() {
            @Override
            public int get() {
                if (!host.workOrders().isEmpty()) {
                    find(player);
                }
                return found(player) ? 1 : 0;
            }

            @Override
            public void set(int value) {
            }
        };
    }

    private static boolean carriesWorkOrders(Player player) {
        return player.getInventory().hasAnyOf(Set.of(
                ModItems.DISPATCH_TERMINAL.get(),
                ModItems.JOB_CHIP.get(),
                ModItems.COMPLETED_JOB_CHIP.get()
        ));
    }
}
