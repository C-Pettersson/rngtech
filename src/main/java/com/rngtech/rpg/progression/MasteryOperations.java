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
            case "ascend", "choose_ascendancy", "switch_ascendancy", "allocate_ascendancy", "refund_ascendancy":
                String failure = ascendancy(host, action, value, payment(player));
                if (failure != null) { return fail(player, failure); }
                if (action.equals("ascend") || action.equals("switch_ascendancy")) {
                    player.displayClientMessage(Component.translatable("rngtech.mastery.message.ascended", host.masteryState().sealTiers()), true);
                }
                return true;
            default: return false;
        }
    }

    /** What an ascendancy action costs: a Seal of one tier, or Mastery Refunds. Each returns false when it cannot be paid. */
    interface AscendancyPayment {
        boolean seal(int tier);
        boolean refunds(int count);
    }

    /**
     * Validates, pays for, and applies an ascendancy action. Returns null on success or the failure message key. Nothing is
     * paid when the action is invalid.
     */
    static String ascendancy(MachineMasteryHost host, String action, String value, AscendancyPayment payment) {
        MachineProgressionState state = host.masteryState();
        MachineMasteryFamily family = host.masteryFamily();
        MachineProgressionState next;
        switch (action) {
            case "ascend" -> {
                int tier = state.sealTiers() + 1;
                if (tier > AscendancyCatalog.MAX_TIERS) { return "all_seals_used"; }
                if (tier == 1 && host.ascendancyEntryStage() < AscendancyCatalog.ENTRY_STAGE) { return "entry_stage"; }
                next = state.withSealTier(tier, family, host.ascendancyEntryStage(), value);
                if (next.equals(state)) { return "ascend_failed"; }
                if (!payment.seal(tier)) { return "seal_missing"; }
            }
            case "choose_ascendancy" -> {
                next = state.withAscendancyChoice(family, value);
                if (next.equals(state)) { return "choice_failed"; }
            }
            case "switch_ascendancy" -> {
                next = state.withSwitchedAscendancy(family, value);
                if (next.equals(state)) { return "switch_failed"; }
                if (!payment.seal(1)) { return "seal_missing"; }
            }
            case "allocate_ascendancy" -> {
                return host.allocateAscendancyNodes(List.of(value)) > 0 ? null : "ascendancy_allocation_failed";
            }
            case "refund_ascendancy" -> {
                next = state.withoutAscendancyNode(value);
                if (next.equals(state) || !host.masteryGearAllows(next)) { return "ascendancy_refund_failed"; }
                if (!payment.refunds(AscendancyCatalog.REFUNDS_PER_NODE)) { return "ascendancy_refund_cost"; }
            }
            default -> { return "unknown_action"; }
        }
        host.applyMasteryState(next);
        return null;
    }

    /** Creative players need the Seal in inventory but do not consume it, and pay no refunds. */
    private static AscendancyPayment payment(Player player) {
        return new AscendancyPayment() {
            @Override public boolean seal(int tier) {
                var inventory = player.getInventory();
                for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                    ItemStack stack = inventory.getItem(slot);
                    if (stack.is(ModItems.ascendancySeal(tier).get())) {
                        if (!player.isCreative()) { stack.shrink(1); inventory.setChanged(); }
                        return true;
                    }
                }
                return false;
            }
            @Override public boolean refunds(int count) { return consumeRefunds(player, count); }
        };
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
