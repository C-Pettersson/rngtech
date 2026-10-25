package com.rngtech.content.item;

import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.workorder.WorkOrderRewards;
import com.rngtech.rpg.workorder.FulfilledWorkOrder;
import com.rngtech.rpg.workorder.LoadedWorkOrder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.SplittableRandom;

/**
 * The item a finished Work Order drops. Using it pays the exact items the order listed; an order without a list rolls
 * {@code rngtech:challenges/work_order} instead, once per reward roll its Bounty earns.
 */
public class CompletedJobChipItem extends Item {
    public CompletedJobChipItem(Properties properties) {
        super(properties);
    }

    public static FulfilledWorkOrder data(ItemStack stack) {
        FulfilledWorkOrder data = stack.get(ModDataComponents.FULFILLED_WORK_ORDER.get());
        return data == null ? FulfilledWorkOrder.BASE : data;
    }

    /** The Fulfilled Work Order for {@code loaded}, finished on a machine with {@code ascendancy}, or "" without one. */
    public static ItemStack of(LoadedWorkOrder loaded, String ascendancy) {
        ItemStack stack = new ItemStack(ModItems.COMPLETED_JOB_CHIP.get());
        var order = loaded.order();
        stack.set(ModDataComponents.FULFILLED_WORK_ORDER.get(),
                new FulfilledWorkOrder(order.tier(), order.family(), ascendancy, order.bounty(), order.prestige(), order.rewards(), order.rewardTable()));
        return stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        FulfilledWorkOrder order = data(stack);
        WorkOrderRewards.give(player, WorkOrderRewards.payout(level, player.blockPosition(), order, new SplittableRandom(level.getRandom().nextLong())));
        stack.consume(1, player);
        player.displayClientMessage(Component.translatable("rngtech.work_order.message.opened"), true);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.rngtech.completed_job_chip.tiered", GlyphItem.numeral(data(stack).tier()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        FulfilledWorkOrder order = data(stack);
        if (!order.family().isEmpty()) {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.fulfilled_work_order.family",
                    JobChipItem.familyName(order.family())).withStyle(ChatFormatting.GRAY));
        }
        if (!WorkOrderRewards.items(order.rewards()).isEmpty()) {
            WorkOrderRewards.appendLines(tooltipComponents, order.rewards());
        } else {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.fulfilled_work_order.unlisted").withStyle(ChatFormatting.GRAY));
        }
        if (order.prestige() > 0) {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.fulfilled_work_order.prestige", order.prestige()).withStyle(ChatFormatting.GRAY));
        }
        tooltipComponents.add(Component.translatable("rngtech.tooltip.fulfilled_work_order.use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
