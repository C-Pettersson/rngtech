package com.rngtech.content.item;

import com.rngtech.content.block.CableBlock;
import com.rngtech.content.block.UniversalConnectorBlock;
import com.rngtech.content.blockentity.CableBlockEntity;
import com.rngtech.content.blockentity.UniversalConnectorBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class UniversalConnectorItem extends BlockItem {
    public UniversalConnectorItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult universalMerge = tryMergeIntoUniversalConnector(context);
        if (universalMerge != InteractionResult.PASS) {
            return universalMerge;
        }
        InteractionResult cableInstall = tryInstallOnCable(context);
        if (cableInstall != InteractionResult.PASS) {
            return cableInstall;
        }
        return super.useOn(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("rngtech.tooltip.universal_connector.install").withStyle(ChatFormatting.DARK_GRAY));
    }

    private InteractionResult tryInstallOnCable(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        BlockState clickedState = level.getBlockState(clickedPos);

        if (clickedState.getBlock() instanceof CableBlock) {
            return tryInstall(context, clickedPos, clickedFace, true);
        }

        BlockPos cablePos = clickedPos.relative(clickedFace);
        BlockState cableState = level.getBlockState(cablePos);
        if (cableState.getBlock() instanceof CableBlock) {
            return tryInstall(context, cablePos, clickedFace.getOpposite(), false);
        }

        return InteractionResult.PASS;
    }

    private InteractionResult tryMergeIntoUniversalConnector(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        BlockState clickedState = level.getBlockState(clickedPos);

        if (clickedState.getBlock() instanceof UniversalConnectorBlock) {
            return tryMerge(context, clickedPos, clickedFace, true);
        }

        BlockPos connectorPos = clickedPos.relative(clickedFace);
        BlockState connectorState = level.getBlockState(connectorPos);
        if (connectorState.getBlock() instanceof UniversalConnectorBlock) {
            return tryMerge(context, connectorPos, clickedFace.getOpposite(), false);
        }

        return InteractionResult.PASS;
    }

    private InteractionResult tryMerge(
            UseOnContext context,
            BlockPos connectorPos,
            Direction newConnectorSide,
            boolean allowFallbackPlacement
    ) {
        Level level = context.getLevel();
        BlockState connectorState = level.getBlockState(connectorPos);
        if (UniversalConnectorBlock.hasConnector(connectorState, newConnectorSide)) {
            return InteractionResult.FAIL;
        }

        if (!CableBlock.isValidConnectorTarget(level.getBlockState(connectorPos.relative(newConnectorSide)))) {
            return allowFallbackPlacement ? InteractionResult.PASS : InteractionResult.FAIL;
        }

        Player player = context.getPlayer();
        if (!level.isClientSide) {
            if (!(level.getBlockEntity(connectorPos) instanceof UniversalConnectorBlockEntity connector)
                    || !connector.installUniversalConnector(newConnectorSide)) {
                return InteractionResult.FAIL;
            }
            level.setBlock(
                    connectorPos,
                    UniversalConnectorBlock.setConnector(connectorState, newConnectorSide, true),
                    Block.UPDATE_ALL
            );

            ItemStack stack = context.getItemInHand();
            if (player == null) {
                stack.shrink(1);
            } else {
                stack.consume(1, player);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private InteractionResult tryInstall(
            UseOnContext context,
            BlockPos cablePos,
            Direction cableSide,
            boolean allowFallbackPlacement
    ) {
        Level level = context.getLevel();
        BlockState cableState = level.getBlockState(cablePos);
        BlockState targetState = level.getBlockState(cablePos.relative(cableSide));
        if (!CableBlock.isValidConnectorTarget(targetState)) {
            return allowFallbackPlacement ? InteractionResult.PASS : InteractionResult.FAIL;
        }

        if (level.getBlockEntity(cablePos) instanceof CableBlockEntity cable
                && cable.hasAnyConnector(cableSide)) {
            return InteractionResult.FAIL;
        }

        Player player = context.getPlayer();
        if (!level.isClientSide && level.getBlockEntity(cablePos) instanceof CableBlockEntity cable) {
            if (!cable.installUniversalConnector(cableSide)) {
                return InteractionResult.FAIL;
            }
            level.setBlock(cablePos, cableState.setValue(CableBlock.connectorProperty(cableSide), true), Block.UPDATE_ALL);
            ItemStack stack = context.getItemInHand();
            if (player == null) {
                stack.shrink(1);
            } else {
                stack.consume(1, player);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
