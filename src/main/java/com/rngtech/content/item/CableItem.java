package com.rngtech.content.item;

import com.rngtech.content.block.CableBlock;
import com.rngtech.content.block.UniversalConnectorBlock;
import com.rngtech.content.blockentity.CableBlockEntity;
import com.rngtech.content.blockentity.UniversalConnectorBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CableItem extends BlockItem {
    public CableItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult merged = tryMergeIntoUniversalConnector(context);
        if (merged != InteractionResult.PASS) {
            return merged;
        }
        return super.useOn(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("rngtech.tooltip.cable.universal").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("rngtech.tooltip.cable.connectors").withStyle(ChatFormatting.DARK_GRAY));
    }

    private InteractionResult tryMergeIntoUniversalConnector(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clickedPos);
        if (clickedState.getBlock() instanceof UniversalConnectorBlock) {
            return mergeAt(context, clickedPos, clickedState);
        }

        BlockPos placementPos = clickedPos.relative(context.getClickedFace());
        BlockState placementState = level.getBlockState(placementPos);
        if (placementState.getBlock() instanceof UniversalConnectorBlock) {
            return mergeAt(context, placementPos, placementState);
        }

        return InteractionResult.PASS;
    }

    private InteractionResult mergeAt(UseOnContext context, BlockPos pos, BlockState connectorState) {
        Level level = context.getLevel();
        List<Direction> connectorSides = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            if (UniversalConnectorBlock.hasConnector(connectorState, direction)) {
                connectorSides.add(direction);
            }
        }
        if (connectorSides.isEmpty()) {
            return InteractionResult.FAIL;
        }
        for (Direction connectorSide : connectorSides) {
            BlockState targetState = level.getBlockState(pos.relative(connectorSide));
            if (!CableBlock.isValidConnectorTarget(targetState)) {
                return InteractionResult.FAIL;
            }
        }

        if (!level.isClientSide) {
            Map<Direction, CompoundTag> connectorTags = Map.of(
                    connectorState.getValue(UniversalConnectorBlock.FACING),
                    new CompoundTag()
            );
            if (level.getBlockEntity(pos) instanceof UniversalConnectorBlockEntity connector) {
                connectorTags = connector.exportConnectorDataByFace(level.registryAccess());
            }
            if (connectorTags.isEmpty()) {
                return InteractionResult.FAIL;
            }

            CableBlock cableBlock = (CableBlock) getBlock();
            Direction firstSide = connectorTags.keySet().iterator().next();
            BlockState cableState = cableBlock.stateForConnectorPlacement(level, pos, firstSide);
            for (Direction connectorSide : connectorTags.keySet()) {
                cableState = cableState.setValue(CableBlock.connectorProperty(connectorSide), true);
            }
            level.setBlock(pos, cableState, Block.UPDATE_ALL);

            if (!(level.getBlockEntity(pos) instanceof CableBlockEntity cable)) {
                return InteractionResult.FAIL;
            }
            for (Map.Entry<Direction, CompoundTag> entry : connectorTags.entrySet()) {
                if (!cable.installUniversalConnector(entry.getKey(), entry.getValue(), level.registryAccess())) {
                    return InteractionResult.FAIL;
                }
            }

            ItemStack stack = context.getItemInHand();
            Player player = context.getPlayer();
            if (player == null) {
                stack.shrink(1);
            } else {
                stack.consume(1, player);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
