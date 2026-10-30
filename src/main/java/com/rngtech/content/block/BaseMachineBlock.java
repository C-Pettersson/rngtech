package com.rngtech.content.block;

import com.rngtech.content.blockentity.BaseMachineBlockEntity;
import com.rngtech.content.blockentity.MachineInfoProvider;
import com.rngtech.content.blockentity.MachineInfoSnapshot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public abstract class BaseMachineBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    /** Which Glow light level the machine shows, as an index into {@link #GLOW_LIGHT_LEVELS}; 0 is dark. */
    public static final IntegerProperty GLOW = IntegerProperty.create("glow", 0, 4);
    public static final int[] GLOW_LIGHT_LEVELS = {0, 4, 7, 10, 13};

    protected BaseMachineBlock(Properties properties) {
        super(properties.lightLevel(state -> GLOW_LIGHT_LEVELS[state.getValue(GLOW)]));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /**
     * Comparators read the machine's work state: blocked 15, running 8, paused 4, idle 1, and 0 for a machine without one.
     * A Telemetry body sends its gauge instead.
     */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof BaseMachineBlockEntity machine) {
            int telemetry = machine.telemetrySignal();
            if (telemetry >= 0) {
                return telemetry;
            }
        }
        return blockEntity instanceof MachineInfoProvider provider
                ? comparatorSignal(provider.machineInfo().state())
                : 0;
    }

    /** Runs {@code ticker}, then the body cues every machine shares: the Glow light and Telemetry comparator updates. */
    protected static <T extends BlockEntity> BlockEntityTicker<T> withBodyCues(BlockEntityTicker<T> ticker) {
        if (ticker == null) {
            return null;
        }
        return (level, pos, state, blockEntity) -> {
            ticker.tick(level, pos, state, blockEntity);
            if (blockEntity instanceof BaseMachineBlockEntity machine && !machine.isRemoved()) {
                machine.tickBodyCues(level, pos);
            }
        };
    }

    public static int comparatorSignal(MachineInfoSnapshot.WorkState state) {
        return switch (state) {
            case BLOCKED -> 15;
            case RUNNING -> 8;
            case PAUSED -> 4;
            case IDLE -> 1;
        };
    }

    /**
     * Sets {@link #ACTIVE} on the block at {@code pos}. {@code state} may be the state a tick started with, so the change
     * applies to the current state, keeping properties the tick changed since, such as a Job Chip lamp.
     */
    public static void setActive(Level level, BlockPos pos, BlockState state, boolean active) {
        if (state.getValue(ACTIVE) != active) {
            BlockState current = level.getBlockState(pos);
            level.setBlock(pos, (current.is(state.getBlock()) ? current : state).setValue(ACTIVE, active), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE, GLOW);
    }
}
