package com.rngtech.content.block;

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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public abstract class BaseMachineBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    /** Set while the machine wants to work but cannot; the client draws a warning sign on the front for it. */
    public static final BooleanProperty BLOCKED = BooleanProperty.create("blocked");
    private static final int BLOCKED_CHECK_INTERVAL = 20;

    protected BaseMachineBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ACTIVE, false)
                .setValue(BLOCKED, false));
    }

    /** The machine's own ticker; the server one is wrapped so every machine also keeps {@link #BLOCKED} current. */
    protected abstract <T extends BlockEntity> BlockEntityTicker<T> machineTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> blockEntityType
    );

    @Override
    public final <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        BlockEntityTicker<T> ticker = machineTicker(level, state, blockEntityType);
        if (ticker == null || level.isClientSide) {
            return ticker;
        }
        return (tickLevel, pos, tickState, blockEntity) -> {
            ticker.tick(tickLevel, pos, tickState, blockEntity);
            refreshBlocked(tickLevel, pos, blockEntity);
        };
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

    /** Comparators read the machine's work state: blocked 15, running 8, paused 4, idle 1, and 0 for a machine without one. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof MachineInfoProvider provider
                ? comparatorSignal(provider.machineInfo().state())
                : 0;
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
     * A blocked machine shows the warning sign unless the stop is the player's choice or only means nothing draws its
     * power: a redstone pause, or a generator with a full buffer.
     */
    public static boolean showsBlocked(MachineInfoSnapshot info) {
        return info.state() == MachineInfoSnapshot.WorkState.BLOCKED
                && info.blockedReason() != MachineInfoSnapshot.BlockedReason.REDSTONE_PAUSED
                && info.blockedReason() != MachineInfoSnapshot.BlockedReason.ENERGY_FULL
                && info.outputSummary() != MachineInfoSnapshot.OutputSummary.ENERGY_FULL;
    }

    /** Checks once a second, staggered by position, and changes the block only when the value flips. */
    private static void refreshBlocked(Level level, BlockPos pos, BlockEntity blockEntity) {
        if (Math.floorMod(level.getGameTime() + pos.hashCode(), BLOCKED_CHECK_INTERVAL) != 0
                || !(blockEntity instanceof MachineInfoProvider provider)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(BLOCKED)) {
            return;
        }
        boolean blocked = showsBlocked(provider.machineInfo());
        if (state.getValue(BLOCKED) != blocked) {
            level.setBlock(pos, state.setValue(BLOCKED, blocked), Block.UPDATE_ALL);
        }
    }

    public static void setActive(Level level, BlockPos pos, BlockState state, boolean active) {
        if (state.getValue(ACTIVE) != active) {
            level.setBlock(pos, state.setValue(ACTIVE, active), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE, BLOCKED);
    }
}
