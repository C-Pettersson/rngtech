package com.rngtech.content.workorder;

import com.rngtech.content.loot.ChallengeContext;
import com.rngtech.content.loot.ChallengeLoot;
import com.rngtech.rpg.workorder.FulfilledWorkOrder;
import com.rngtech.rpg.workorder.WorkOrder;
import com.rngtech.rpg.workorder.WorkOrderRules;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * The one reward a Work Order pays. The terminal rolls the job's reward table once when it lists the job, which picks one
 * reward kind and its tier's amount, then scales the amount by Bounty. The table is {@code rngtech:challenges/work_order}
 * unless a template names its own. The chip lists the result, and redeeming the Completed Job Chip pays exactly that.
 */
public final class WorkOrderRewards {
    private static final int MAX_STACKS = 27;

    private WorkOrderRewards() {
    }

    /** {@code order} with its reward rolled in {@code level}. On the client, or with no level, the order comes back unchanged. */
    public static WorkOrder withRewards(Level level, BlockPos pos, WorkOrder order, RandomGenerator random) {
        if (level == null || level.isClientSide) {
            return order;
        }
        FulfilledWorkOrder context = new FulfilledWorkOrder(order.tier(), order.family(), "", order.bounty(), order.prestige(), null, order.rewardTable());
        return order.withRewards(ItemContainerContents.fromItems(roll(level, pos, context, random)));
    }

    /** Rolls the reward table once for {@code order} and scales each result's amount by Bounty. */
    public static List<ItemStack> roll(Level level, BlockPos pos, FulfilledWorkOrder order, RandomGenerator random) {
        List<ItemStack> merged = new ArrayList<>();
        for (ItemStack stack : ChallengeLoot.roll(level, pos, table(order.rewardTable()), ChallengeContext.of(order, 0))) {
            int amount = WorkOrderRules.scaledAmount(stack.getCount(), order.bounty(), random);
            while (amount > 0 && merged.size() < MAX_STACKS) {
                int count = Math.min(amount, stack.getMaxStackSize());
                merge(merged, stack.copyWithCount(count));
                amount -= count;
            }
        }
        return merged;
    }

    /** The loot table {@code id} names, or {@code rngtech:challenges/work_order} for "" or an id that does not parse. */
    static ResourceKey<LootTable> table(String id) {
        ResourceLocation key = id.isEmpty() ? null : ResourceLocation.tryParse(id);
        return key == null ? ChallengeLoot.WORK_ORDER : ResourceKey.create(Registries.LOOT_TABLE, key);
    }

    /** The listed items, or for an order without a list, a fresh roll of the reward table. */
    public static List<ItemStack> payout(Level level, BlockPos pos, FulfilledWorkOrder order, RandomGenerator random) {
        List<ItemStack> listed = items(order.rewards());
        return listed.isEmpty() ? roll(level, pos, order, random) : listed;
    }

    /** Copies of the listed stacks; none for a missing list. */
    public static List<ItemStack> items(ItemContainerContents rewards) {
        List<ItemStack> stacks = new ArrayList<>();
        if (rewards != null) {
            rewards.nonEmptyItemsCopy().forEach(stacks::add);
        }
        return stacks;
    }

    public static void give(Player player, List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            ItemStack copy = stack.copy();
            if (!player.getInventory().add(copy)) {
                player.drop(copy, false);
            }
        }
    }

    /** Tooltip lines: "Pays:" and one line per listed stack, or nothing for an order without a list. */
    public static void appendLines(List<Component> lines, ItemContainerContents rewards) {
        List<ItemStack> stacks = items(rewards);
        if (stacks.isEmpty()) {
            return;
        }
        lines.add(Component.translatable("rngtech.tooltip.work_order.pays").withStyle(ChatFormatting.GRAY));
        for (ItemStack stack : stacks) {
            lines.add(Component.translatable("rngtech.tooltip.work_order.pays_line", stack.getCount(), stack.getHoverName()).withStyle(ChatFormatting.DARK_GREEN));
        }
    }

    private static void merge(List<ItemStack> merged, ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (ItemStack existing : merged) {
            if (remaining.isEmpty()) {
                return;
            }
            if (ItemStack.isSameItemSameComponents(existing, remaining) && existing.getCount() < existing.getMaxStackSize()) {
                int moved = Math.min(existing.getMaxStackSize() - existing.getCount(), remaining.getCount());
                existing.grow(moved);
                remaining.shrink(moved);
            }
        }
        if (!remaining.isEmpty()) {
            merged.add(remaining);
        }
    }
}
