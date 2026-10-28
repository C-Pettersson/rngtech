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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class UniversalConnectorItem extends BlockItem implements PlacementPreview.Source {
    public UniversalConnectorItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ConnectorUse use = resolveUse(context);
        return switch (use.action()) {
            case FAIL -> InteractionResult.FAIL;
            case MERGE -> merge(context, use.pos(), use.side());
            case INSTALL -> install(context, use.pos(), use.side());
            case PLACE -> super.useOn(context);
        };
    }

    @Override
    public PlacementPreview placementPreview(UseOnContext context) {
        Level level = context.getLevel();
        ConnectorUse use = resolveUse(context);
        return switch (use.action()) {
            case FAIL -> null;
            case MERGE -> new PlacementPreview(
                    use.pos(),
                    UniversalConnectorBlock.setConnector(level.getBlockState(use.pos()), use.side(), true)
            );
            case INSTALL -> new PlacementPreview(
                    use.pos(),
                    level.getBlockState(use.pos()).setValue(CableBlock.connectorProperty(use.side()), true)
            );
            case PLACE -> PlacementPreview.placed(this, new BlockPlaceContext(context), this::getPlacementState);
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("rngtech.tooltip.universal_connector.install").withStyle(ChatFormatting.DARK_GRAY));
    }

    /**
     * Adds a face to a Universal Connector block, then fits a connector to a cable, then places a new block. A click on
     * an open cable face mounts the connector on that face, or places a plate in the space beyond while sneaking. A
     * click on any other connector or cable face with nothing to serve places a plate beyond only while sneaking, and
     * a click beside one fails.
     */
    private static ConnectorUse resolveUse(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        BlockPos besidePos = clickedPos.relative(clickedFace);
        Direction besideSide = clickedFace.getOpposite();
        Player player = context.getPlayer();
        boolean placeBeyond = player == null || player.isSecondaryUseActive();
        ConnectorUse use = null;
        if (level.getBlockState(clickedPos).getBlock() instanceof UniversalConnectorBlock) {
            use = connectorFace(level, clickedPos, clickedFace, true);
        } else if (level.getBlockState(besidePos).getBlock() instanceof UniversalConnectorBlock) {
            use = connectorFace(level, besidePos, besideSide, false);
        }
        if (use == null && level.getBlockState(clickedPos).getBlock() instanceof CableBlock) {
            use = cableFace(level, clickedPos, clickedFace, true, !placeBeyond);
        } else if (use == null && level.getBlockState(besidePos).getBlock() instanceof CableBlock) {
            use = cableFace(level, besidePos, besideSide, false, false);
        }
        if (use != null) {
            return use;
        }
        Block clickedBlock = level.getBlockState(clickedPos).getBlock();
        if ((clickedBlock instanceof CableBlock || clickedBlock instanceof UniversalConnectorBlock) && !placeBeyond) {
            return new ConnectorUse(Action.FAIL, clickedPos, clickedFace);
        }
        return new ConnectorUse(Action.PLACE, clickedPos, clickedFace);
    }

    private static ConnectorUse connectorFace(Level level, BlockPos pos, Direction side, boolean mayFallThrough) {
        if (UniversalConnectorBlock.hasConnector(level.getBlockState(pos), side)) {
            return new ConnectorUse(Action.FAIL, pos, side);
        }
        if (!CableBlock.isValidConnectorTarget(level.getBlockState(pos.relative(side)))) {
            return mayFallThrough ? null : new ConnectorUse(Action.FAIL, pos, side);
        }
        return new ConnectorUse(Action.MERGE, pos, side);
    }

    private static ConnectorUse cableFace(
            Level level,
            BlockPos pos,
            Direction side,
            boolean mayFallThrough,
            boolean mountOnOpenFace
    ) {
        BlockState target = level.getBlockState(pos.relative(side));
        if (!(mountOnOpenFace && target.isAir()) && !CableBlock.isValidConnectorTarget(target)) {
            return mayFallThrough ? null : new ConnectorUse(Action.FAIL, pos, side);
        }
        if (level.getBlockEntity(pos) instanceof CableBlockEntity cable && cable.hasAnyConnector(side)) {
            return new ConnectorUse(Action.FAIL, pos, side);
        }
        return new ConnectorUse(Action.INSTALL, pos, side);
    }

    private InteractionResult merge(UseOnContext context, BlockPos connectorPos, Direction newConnectorSide) {
        Level level = context.getLevel();
        BlockState connectorState = level.getBlockState(connectorPos);
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

    private InteractionResult install(UseOnContext context, BlockPos cablePos, Direction cableSide) {
        Level level = context.getLevel();
        BlockState cableState = level.getBlockState(cablePos);
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

    private enum Action {
        MERGE,
        INSTALL,
        PLACE,
        FAIL
    }

    private record ConnectorUse(Action action, BlockPos pos, Direction side) {
    }
}
