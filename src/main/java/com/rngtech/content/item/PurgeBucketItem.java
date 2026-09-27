package com.rngtech.content.item;

import com.rngtech.content.purge.FluidPurgeSupport;
import com.rngtech.content.purge.PurgeableFluidStorage;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public class PurgeBucketItem extends Item {
    public PurgeBucketItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = context.getLevel().getBlockEntity(context.getClickedPos());
        if (blockEntity instanceof PurgeableFluidStorage storage) {
            FluidPurgeSupport.purgeFromItem(player, storage, player.isShiftKeyDown());
        } else {
            FluidPurgeSupport.showUnsupported(player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("rngtech.tooltip.purge_bucket.role").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("rngtech.tooltip.purge_bucket.sneak").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
