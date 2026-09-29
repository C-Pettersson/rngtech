package com.rngtech.rpg.progression;

import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModItems;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class MasteryOperations {
    private MasteryOperations() { }

    public static boolean perform(Player player, MachineMasteryHost host, String action, String value) {
        if (host == null || player.isSpectator() || player.level().isClientSide) { return false; }
        MachineProgressionState state = host.masteryState();
        switch (action) {
            case "allocate":
                if (host.allocateMasteryPath(path(value))) { return true; }
                return fail(player, "allocation_failed");
            case "paste":
                if (!state.allocatedNodes().isEmpty()) { return fail(player, "requires_clear"); }
                try {
                    MasteryBuildCode code = MasteryBuildCode.decode(value);
                    if (!code.start().equals(state.startNodeId())) { return fail(player, "wrong_start"); }
                    host.applyMasteryState(state.withTarget(code.nodes()));
                    pasteAscendancy(host, code);
                    return true;
                } catch (IllegalArgumentException exception) { return fail(player, "invalid_build"); }
            case "pause":
                host.applyMasteryState(state.withFollowing(false)); return true;
            case "resume":
                host.applyMasteryState(state.withFollowing(true)); return true;
            case "refund":
            case "clear":
                MachineProgressionState next = action.equals("clear") ? state.cleared() : state.withoutNode(value);
                int cost = state.spentPoints() - next.spentPoints();
                if (cost <= 0 || !host.masteryGearAllows(next)) { return fail(player, "refund_failed"); }
                if (!consumeRefunds(player, cost)) { return fail(player, "refund_cost"); }
                host.applyMasteryState(next); return true;
            case "copy_configurator":
                for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                    ItemStack stack = player.getInventory().getItem(slot);
                    if (stack.is(ModItems.CONFIGURATOR.get())) {
                        stack.set(ModDataComponents.MASTERY_BUILD.get(), MasteryBuildCode.copy(state).encode());
                        stack.set(ModDataComponents.MASTERY_CONFIGURATOR_MODE.get(), true);
                        player.getInventory().setChanged();
                        player.displayClientMessage(Component.translatable("rngtech.mastery.message.configurator_copied"), true);
                        return true;
                    }
                }
                return fail(player, "no_configurator");
            default: return false;
        }
    }

    /** Ascendancy nodes paste only onto the same ascendancy with none allocated, as far as its earned points reach. */
    static void pasteAscendancy(MachineMasteryHost host, MasteryBuildCode code) {
        MachineProgressionState state = host.masteryState();
        if (!code.ascendancy().isEmpty() && code.ascendancy().equals(state.ascendancy()) && state.ascendancyNodes().isEmpty()) {
            host.allocateAscendancyNodes(code.ascendancyNodes());
        }
    }

    /** Parses one node ID or a comma-separated route in allocation order. */
    private static List<MegaPassiveNode> path(String value) {
        String[] ids = value.split(",", MegaPassiveTree.MAX_ALLOCATIONS + 1);
        if (ids.length > MegaPassiveTree.MAX_ALLOCATIONS) { return List.of(); }
        List<MegaPassiveNode> path = new ArrayList<>();
        for (String id : ids) {
            MegaPassiveNode node = MegaPassiveTree.node(id);
            if (node == null) { return List.of(); }
            path.add(node);
        }
        return path;
    }

    private static boolean consumeRefunds(Player player, int cost) {
        if (player.isCreative()) { return true; }
        int available = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ModItems.MASTERY_REFUND.get())) { available += stack.getCount(); }
        }
        if (available < cost) { return false; }
        for (int slot = 0; slot < player.getInventory().getContainerSize() && cost > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ModItems.MASTERY_REFUND.get())) {
                int taken = Math.min(cost, stack.getCount()); stack.shrink(taken); cost -= taken;
            }
        }
        player.getInventory().setChanged();
        return true;
    }
    private static boolean fail(Player player, String key) {
        player.displayClientMessage(Component.translatable("rngtech.mastery.message." + key), true); return false;
    }
}
