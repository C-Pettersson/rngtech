package com.rngtech.content.item;

import com.rngtech.content.block.CableBlock;
import com.rngtech.content.block.UniversalConnectorBlock;
import com.rngtech.content.blockentity.CableBlockEntity;
import com.rngtech.content.blockentity.UniversalConnectorBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CableItem extends BlockItem implements PlacementPreview.Source {
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
        DyeColor color = stack.get(DataComponents.BASE_COLOR);
        if (color != null) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.cable.color",
                    Component.translatable("color.minecraft." + color.getName())
            ).withStyle(ChatFormatting.GRAY));
        }
        tooltipComponents.add(Component.translatable("rngtech.tooltip.cable.universal").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("rngtech.tooltip.cable.connectors").withStyle(ChatFormatting.DARK_GRAY));
        tooltipComponents.add(Component.translatable("rngtech.tooltip.cable.dye").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** A water cauldron washes the dye out of a whole stack of cable, or of Battery Chassis, for one water level. */
    public static ItemInteractionResult washInCauldron(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            ItemStack stack
    ) {
        if (!stack.has(DataComponents.BASE_COLOR)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            stack.remove(DataComponents.BASE_COLOR);
            LayeredCauldronBlock.lowerFillLevel(state, level, pos);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public PlacementPreview placementPreview(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos mergePos = universalConnectorAt(level, context.getClickedPos(), context.getClickedFace());
        if (mergePos == null) {
            return PlacementPreview.placed(this, new BlockPlaceContext(context), this::getPlacementState);
        }
        List<Direction> connectorSides = mergeableConnectorSides(level, mergePos, level.getBlockState(mergePos));
        if (connectorSides == null) {
            return null;
        }
        DyeColor color = context.getItemInHand().get(DataComponents.BASE_COLOR);
        return new PlacementPreview(mergePos, mergedCableState(level, mergePos, connectorSides, color));
    }

    /** The Universal Connector block that this use turns into a cable, or null to place cable normally. */
    private static BlockPos universalConnectorAt(Level level, BlockPos clickedPos, Direction clickedFace) {
        if (level.getBlockState(clickedPos).getBlock() instanceof UniversalConnectorBlock) {
            return clickedPos;
        }
        BlockPos placementPos = clickedPos.relative(clickedFace);
        return level.getBlockState(placementPos).getBlock() instanceof UniversalConnectorBlock ? placementPos : null;
    }

    /** The connector's faces, or null when one of them no longer faces a block a cable connector can serve. */
    private static List<Direction> mergeableConnectorSides(Level level, BlockPos pos, BlockState connectorState) {
        List<Direction> connectorSides = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            if (UniversalConnectorBlock.hasConnector(connectorState, direction)) {
                connectorSides.add(direction);
            }
        }
        if (connectorSides.isEmpty()) {
            return null;
        }
        for (Direction connectorSide : connectorSides) {
            if (!CableBlock.isValidConnectorTarget(level.getBlockState(pos.relative(connectorSide)))) {
                return null;
            }
        }
        return connectorSides;
    }

    private BlockState mergedCableState(Level level, BlockPos pos, Collection<Direction> connectorSides, DyeColor color) {
        Direction firstSide = connectorSides.iterator().next();
        BlockState cableState = ((CableBlock) getBlock()).stateForConnectorPlacement(level, pos, firstSide, color);
        for (Direction connectorSide : connectorSides) {
            cableState = cableState.setValue(CableBlock.connectorProperty(connectorSide), true);
        }
        return cableState;
    }

    private InteractionResult tryMergeIntoUniversalConnector(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = universalConnectorAt(level, context.getClickedPos(), context.getClickedFace());
        if (pos == null) {
            return InteractionResult.PASS;
        }
        BlockState connectorState = level.getBlockState(pos);
        if (mergeableConnectorSides(level, pos, connectorState) == null) {
            return InteractionResult.FAIL;
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

            DyeColor color = context.getItemInHand().get(DataComponents.BASE_COLOR);
            level.setBlock(pos, mergedCableState(level, pos, connectorTags.keySet(), color), Block.UPDATE_ALL);

            if (!(level.getBlockEntity(pos) instanceof CableBlockEntity cable)) {
                return InteractionResult.FAIL;
            }
            cable.setColor(color);
            CableBlock.refreshCableLinks(level, pos);
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
