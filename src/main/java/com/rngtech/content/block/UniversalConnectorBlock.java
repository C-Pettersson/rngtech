package com.rngtech.content.block;

import com.rngtech.content.blockentity.UniversalConnectorBlockEntity;
import com.rngtech.content.menu.UniversalConnectorMenu;
import com.rngtech.content.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class UniversalConnectorBlock extends Block implements EntityBlock {
    private static final Direction[] DIRECTIONS = Direction.values();
    public static final DirectionProperty FACING = DirectionProperty.create("facing");
    public static final BooleanProperty CABLE_DOWN = BooleanProperty.create("cable_down");
    public static final BooleanProperty CABLE_UP = BooleanProperty.create("cable_up");
    public static final BooleanProperty CABLE_NORTH = BooleanProperty.create("cable_north");
    public static final BooleanProperty CABLE_SOUTH = BooleanProperty.create("cable_south");
    public static final BooleanProperty CABLE_WEST = BooleanProperty.create("cable_west");
    public static final BooleanProperty CABLE_EAST = BooleanProperty.create("cable_east");
    private static final VoxelShape DOWN_SHAPE = Shapes.or(
            Block.box(4.0, 0.0, 4.0, 12.0, 3.0, 6.0),
            Block.box(4.0, 0.0, 10.0, 12.0, 3.0, 12.0),
            Block.box(4.0, 0.0, 6.0, 6.0, 3.0, 10.0),
            Block.box(10.0, 0.0, 6.0, 12.0, 3.0, 10.0)
    );
    private static final VoxelShape UP_SHAPE = Shapes.or(
            Block.box(4.0, 13.0, 4.0, 12.0, 16.0, 6.0),
            Block.box(4.0, 13.0, 10.0, 12.0, 16.0, 12.0),
            Block.box(4.0, 13.0, 6.0, 6.0, 16.0, 10.0),
            Block.box(10.0, 13.0, 6.0, 12.0, 16.0, 10.0)
    );
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(4.0, 4.0, 0.0, 12.0, 6.0, 3.0),
            Block.box(4.0, 10.0, 0.0, 12.0, 12.0, 3.0),
            Block.box(4.0, 6.0, 0.0, 6.0, 10.0, 3.0),
            Block.box(10.0, 6.0, 0.0, 12.0, 10.0, 3.0)
    );
    private static final VoxelShape SOUTH_SHAPE = Shapes.or(
            Block.box(4.0, 4.0, 13.0, 12.0, 6.0, 16.0),
            Block.box(4.0, 10.0, 13.0, 12.0, 12.0, 16.0),
            Block.box(4.0, 6.0, 13.0, 6.0, 10.0, 16.0),
            Block.box(10.0, 6.0, 13.0, 12.0, 10.0, 16.0)
    );
    private static final VoxelShape WEST_SHAPE = Shapes.or(
            Block.box(0.0, 4.0, 4.0, 3.0, 6.0, 12.0),
            Block.box(0.0, 10.0, 4.0, 3.0, 12.0, 12.0),
            Block.box(0.0, 6.0, 4.0, 3.0, 10.0, 6.0),
            Block.box(0.0, 6.0, 10.0, 3.0, 10.0, 12.0)
    );
    private static final VoxelShape EAST_SHAPE = Shapes.or(
            Block.box(13.0, 4.0, 4.0, 16.0, 6.0, 12.0),
            Block.box(13.0, 10.0, 4.0, 16.0, 12.0, 12.0),
            Block.box(13.0, 6.0, 4.0, 16.0, 10.0, 6.0),
            Block.box(13.0, 6.0, 10.0, 16.0, 10.0, 12.0)
    );

    public UniversalConnectorBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any().setValue(FACING, Direction.NORTH);
        for (Direction direction : DIRECTIONS) {
            state = state.setValue(cableProperty(direction), false);
        }
        registerDefaultState(state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace().getOpposite();
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(connectorProperty(facing), true);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UniversalConnectorBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(
                blockEntityType,
                ModBlockEntities.UNIVERSAL_CONNECTOR.get(),
                UniversalConnectorBlockEntity::serverTick
        );
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = Shapes.empty();
        boolean found = false;
        for (Direction direction : DIRECTIONS) {
            if (hasConnector(state, direction)) {
                shape = Shapes.or(shape, connectorShape(direction));
                found = true;
            }
        }
        return found ? shape : connectorShape(state.getValue(FACING));
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        return state;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState rotatedState = state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
        for (Direction direction : DIRECTIONS) {
            rotatedState = rotatedState.setValue(connectorProperty(direction), false);
        }
        for (Direction direction : DIRECTIONS) {
            if (state.getValue(connectorProperty(direction))) {
                rotatedState = rotatedState.setValue(connectorProperty(rotation.rotate(direction)), true);
            }
        }
        return rotatedState;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof UniversalConnectorBlockEntity connector) {
            Direction direction = resolveConnectorDirection(state, hitResult);
            if (direction != null && connector.universalConnector(direction) != null) {
                openConnectorMenu(player, connector, pos, direction);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof UniversalConnectorBlockEntity connector) {
            connector.dropInventory(level);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CABLE_DOWN, CABLE_UP, CABLE_NORTH, CABLE_SOUTH, CABLE_WEST, CABLE_EAST);
    }

    public static boolean hasCableConnection(BlockState state, Direction direction) {
        return false;
    }

    public static boolean canConnectNetworkFrom(BlockState state, Direction side) {
        return false;
    }

    public static BlockState refreshCableLinks(LevelAccessor level, BlockPos pos, BlockState state) {
        return state;
    }

    public static BooleanProperty cableProperty(Direction direction) {
        return connectorProperty(direction);
    }

    public static boolean hasConnector(BlockState state, Direction direction) {
        return state.getBlock() instanceof UniversalConnectorBlock
                && (state.getValue(FACING) == direction || state.getValue(connectorProperty(direction)));
    }

    public static BooleanProperty connectorProperty(Direction direction) {
        return switch (direction) {
            case DOWN -> CABLE_DOWN;
            case UP -> CABLE_UP;
            case NORTH -> CABLE_NORTH;
            case SOUTH -> CABLE_SOUTH;
            case WEST -> CABLE_WEST;
            case EAST -> CABLE_EAST;
        };
    }

    public static BlockState setConnector(BlockState state, Direction direction, boolean value) {
        return state.setValue(connectorProperty(direction), value);
    }

    public static Direction resolveConnectorDirection(BlockState state, BlockHitResult hitResult) {
        Direction hitSide = hitResult.getDirection();
        if (hasConnector(state, hitSide)) {
            return hitSide;
        }
        Direction target = null;
        for (Direction direction : DIRECTIONS) {
            if (!hasConnector(state, direction)) {
                continue;
            }
            if (target != null) {
                return null;
            }
            target = direction;
        }
        return target;
    }

    private static void openConnectorMenu(
            Player player,
            UniversalConnectorBlockEntity connector,
            BlockPos pos,
            Direction direction
    ) {
        MenuProvider provider = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.rngtech.universal_connector");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                return new UniversalConnectorMenu(
                        containerId,
                        inventory,
                        connector.universalConnectorAccess(direction),
                        connector.universalConnector(direction).menuData()
                );
            }
        };
        player.openMenu(provider, buffer -> {
            buffer.writeBlockPos(pos);
            buffer.writeByte(direction.ordinal());
        });
    }

    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actualType,
            BlockEntityType<E> expectedType,
            BlockEntityTicker<? super E> ticker
    ) {
        return expectedType == actualType ? (level, pos, state, blockEntity) -> ticker.tick(level, pos, state, (E) blockEntity) : null;
    }

    private static VoxelShape connectorShape(Direction direction) {
        return switch (direction) {
            case DOWN -> DOWN_SHAPE;
            case UP -> UP_SHAPE;
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
        };
    }
}
