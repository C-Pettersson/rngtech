package com.rngtech.content.block;

import com.rngtech.content.blockentity.WoodenDehumidifierBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class WoodenDehumidifierFrameBlock extends Block {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    private static final int CONTROLLER_NOTIFY_RADIUS = 5;

    public WoodenDehumidifierFrameBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(oldState.getBlock())) {
            level.invalidateCapabilities(pos);
            requestNearbyControllerRescans(level, pos);
        }
        super.onPlace(state, level, pos, oldState, movedByPiston);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            BlockPos fromPos,
            boolean isMoving
    ) {
        if (!level.isClientSide) {
            requestNearbyControllerRescans(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            level.invalidateCapabilities(pos);
            requestNearbyControllerRescans(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public static void requestNearbyControllerRescans(Level level, BlockPos pos) {
        BlockPos.betweenClosedStream(
                        pos.offset(-CONTROLLER_NOTIFY_RADIUS, -CONTROLLER_NOTIFY_RADIUS, -CONTROLLER_NOTIFY_RADIUS),
                        pos.offset(CONTROLLER_NOTIFY_RADIUS, CONTROLLER_NOTIFY_RADIUS, CONTROLLER_NOTIFY_RADIUS)
                )
                .forEach(candidate -> {
                    if (level.getBlockEntity(candidate) instanceof WoodenDehumidifierBlockEntity dehumidifier) {
                        dehumidifier.requestStructureRescan();
                    }
                });
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }
}
