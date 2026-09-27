package com.rngtech.content.block;

import com.rngtech.content.blockentity.CableBlockEntity;
import com.rngtech.content.item.EnergyConnectorItem;
import com.rngtech.content.menu.CableConnectorMenu;
import com.rngtech.content.menu.UniversalConnectorMenu;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CableBlock extends Block implements EntityBlock {
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final Map<UUID, ConnectorBreakTarget> PENDING_CONNECTOR_BREAKS = new HashMap<>();
    public static final BooleanProperty CABLE_DOWN = BooleanProperty.create("cable_down");
    public static final BooleanProperty CABLE_UP = BooleanProperty.create("cable_up");
    public static final BooleanProperty CABLE_NORTH = BooleanProperty.create("cable_north");
    public static final BooleanProperty CABLE_SOUTH = BooleanProperty.create("cable_south");
    public static final BooleanProperty CABLE_WEST = BooleanProperty.create("cable_west");
    public static final BooleanProperty CABLE_EAST = BooleanProperty.create("cable_east");
    public static final BooleanProperty CONNECTOR_DOWN = BooleanProperty.create("connector_down");
    public static final BooleanProperty CONNECTOR_UP = BooleanProperty.create("connector_up");
    public static final BooleanProperty CONNECTOR_NORTH = BooleanProperty.create("connector_north");
    public static final BooleanProperty CONNECTOR_SOUTH = BooleanProperty.create("connector_south");
    public static final BooleanProperty CONNECTOR_WEST = BooleanProperty.create("connector_west");
    public static final BooleanProperty CONNECTOR_EAST = BooleanProperty.create("connector_east");

    private static final VoxelShape CORE_SHAPE = Block.box(6.0, 6.0, 6.0, 10.0, 10.0, 10.0);
    private static final VoxelShape DOWN_CABLE_SHAPE = Block.box(6.0, 0.0, 6.0, 10.0, 6.0, 10.0);
    private static final VoxelShape UP_CABLE_SHAPE = Block.box(6.0, 10.0, 6.0, 10.0, 16.0, 10.0);
    private static final VoxelShape NORTH_CABLE_SHAPE = Block.box(6.0, 6.0, 0.0, 10.0, 10.0, 6.0);
    private static final VoxelShape SOUTH_CABLE_SHAPE = Block.box(6.0, 6.0, 10.0, 10.0, 10.0, 16.0);
    private static final VoxelShape WEST_CABLE_SHAPE = Block.box(0.0, 6.0, 6.0, 6.0, 10.0, 10.0);
    private static final VoxelShape EAST_CABLE_SHAPE = Block.box(10.0, 6.0, 6.0, 16.0, 10.0, 10.0);
    private static final VoxelShape DOWN_CONNECTOR_SHAPE = Shapes.or(
            Block.box(4.0, 0.0, 4.0, 12.0, 3.0, 6.0),
            Block.box(4.0, 0.0, 10.0, 12.0, 3.0, 12.0),
            Block.box(4.0, 0.0, 6.0, 6.0, 3.0, 10.0),
            Block.box(10.0, 0.0, 6.0, 12.0, 3.0, 10.0)
    );
    private static final VoxelShape UP_CONNECTOR_SHAPE = Shapes.or(
            Block.box(4.0, 13.0, 4.0, 12.0, 16.0, 6.0),
            Block.box(4.0, 13.0, 10.0, 12.0, 16.0, 12.0),
            Block.box(4.0, 13.0, 6.0, 6.0, 16.0, 10.0),
            Block.box(10.0, 13.0, 6.0, 12.0, 16.0, 10.0)
    );
    private static final VoxelShape NORTH_CONNECTOR_SHAPE = Shapes.or(
            Block.box(4.0, 4.0, 0.0, 12.0, 6.0, 3.0),
            Block.box(4.0, 10.0, 0.0, 12.0, 12.0, 3.0),
            Block.box(4.0, 6.0, 0.0, 6.0, 10.0, 3.0),
            Block.box(10.0, 6.0, 0.0, 12.0, 10.0, 3.0)
    );
    private static final VoxelShape SOUTH_CONNECTOR_SHAPE = Shapes.or(
            Block.box(4.0, 4.0, 13.0, 12.0, 6.0, 16.0),
            Block.box(4.0, 10.0, 13.0, 12.0, 12.0, 16.0),
            Block.box(4.0, 6.0, 13.0, 6.0, 10.0, 16.0),
            Block.box(10.0, 6.0, 13.0, 12.0, 10.0, 16.0)
    );
    private static final VoxelShape WEST_CONNECTOR_SHAPE = Shapes.or(
            Block.box(0.0, 4.0, 4.0, 3.0, 6.0, 12.0),
            Block.box(0.0, 10.0, 4.0, 3.0, 12.0, 12.0),
            Block.box(0.0, 6.0, 4.0, 3.0, 10.0, 6.0),
            Block.box(0.0, 6.0, 10.0, 3.0, 10.0, 12.0)
    );
    private static final VoxelShape EAST_CONNECTOR_SHAPE = Shapes.or(
            Block.box(13.0, 4.0, 4.0, 16.0, 6.0, 12.0),
            Block.box(13.0, 10.0, 4.0, 16.0, 12.0, 12.0),
            Block.box(13.0, 6.0, 4.0, 16.0, 10.0, 6.0),
            Block.box(13.0, 6.0, 10.0, 16.0, 10.0, 12.0)
    );

    public CableBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (Direction direction : DIRECTIONS) {
            state = state
                    .setValue(cableProperty(direction), false)
                    .setValue(connectorProperty(direction), false);
        }
        registerDefaultState(state);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CableBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(
                blockEntityType,
                ModBlockEntities.CABLE.get(),
                CableBlockEntity::serverTick
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return stateWithCableLinks(level, pos, defaultBlockState());
    }

    public BlockState stateForConnectorPlacement(LevelAccessor level, BlockPos pos, Direction connectorDirection) {
        return stateWithCableLinks(level, pos, defaultBlockState().setValue(connectorProperty(connectorDirection), true));
    }

    private BlockState stateWithCableLinks(LevelAccessor level, BlockPos pos, BlockState state) {
        for (Direction direction : DIRECTIONS) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (canConnectCable(level, pos, neighborPos, neighborState, direction)) {
                state = state.setValue(cableProperty(direction), true);
            }
        }
        return state;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        Direction direction = hitResult.getDirection();
        if (stack.is(ModItems.WRENCH.get())) {
            return useWrench(state, level, pos, player, hitResult);
        }

        if (stack.getItem() instanceof EnergyConnectorItem connector) {
            if (!connector.tier().enabled()) {
                return ItemInteractionResult.FAIL;
            }
            if (isValidConnectorTarget(level.getBlockState(pos.relative(direction)))
                    && level.getBlockEntity(pos) instanceof CableBlockEntity cable
                    && cable.hasAnyConnector(direction)) {
                return ItemInteractionResult.FAIL;
            }
            Direction connectorDirection = resolveConnectorInstallDirection(level, pos, direction);
            if (connectorDirection != null) {
                return installConnector(stack, state, level, pos, player, connectorDirection, connector);
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.is(ModItems.UNIVERSAL_CONNECTOR.get())) {
            if (!isValidConnectorTarget(level.getBlockState(pos.relative(direction)))) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (level.getBlockEntity(pos) instanceof CableBlockEntity cable && cable.hasAnyConnector(direction)) {
                return ItemInteractionResult.FAIL;
            }
            return installUniversalConnector(stack, state, level, pos, player, direction);
        }

        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof CableBlock) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (neighborState.getBlock() instanceof CableBlock && !state.getValue(cableProperty(direction))) {
                if (!level.isClientSide) {
                    setDisabledLink(level, pos, direction, false);
                    setDisabledLink(level, neighborPos, direction.getOpposite(), false);
                    level.setBlock(pos, state.setValue(cableProperty(direction), true), Block.UPDATE_ALL);
                    level.setBlock(
                            neighborPos,
                            neighborState.setValue(cableProperty(direction.getOpposite()), true),
                            Block.UPDATE_ALL
                    );
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
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
        BlockState updatedState = state.setValue(
                cableProperty(direction),
                canConnectCable(level, pos, neighborPos, neighborState, direction)
        );
        if (updatedState.getValue(connectorProperty(direction)) && shouldDropConnectorForTarget(neighborState)) {
            updatedState = updatedState.setValue(connectorProperty(direction), false);
            if (level instanceof Level realLevel
                    && !realLevel.isClientSide
                    && realLevel.getBlockEntity(pos) instanceof CableBlockEntity cable) {
                ItemStack removed = cable.removeConnector(direction);
                if (!removed.isEmpty()) {
                    Containers.dropItemStack(realLevel, pos.getX(), pos.getY(), pos.getZ(), removed);
                }
                ItemStack universal = cable.removeUniversalConnector(direction);
                if (!universal.isEmpty()) {
                    Containers.dropItemStack(realLevel, pos.getX(), pos.getY(), pos.getZ(), universal);
                }
            }
        } else if (updatedState.getValue(connectorProperty(direction))
                && level instanceof Level realLevel
                && !realLevel.isClientSide
                && realLevel.getBlockEntity(pos) instanceof CableBlockEntity cable) {
            cable.connectorTargetChanged();
        }
        return updatedState;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        if (target instanceof BlockHitResult hitResult && level.getBlockEntity(pos) instanceof CableBlockEntity cable) {
            Direction direction = resolveInstalledConnectorDirection(state, hitResult, false);
            if (direction != null && cable.hasUniversalConnector(direction)) {
                return new ItemStack(ModItems.UNIVERSAL_CONNECTOR.get());
            }
            if (direction != null && cable.connector(direction) != null) {
                return new ItemStack(ModItems.energyConnector(cable.connector(direction).tier()).get());
            }
        }
        return super.getCloneItemStack(state, target, level, pos, player);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE_SHAPE;
        for (Direction direction : DIRECTIONS) {
            if (hasCableConnection(state, direction) || hasConnector(state, direction)) {
                shape = Shapes.or(shape, cableShape(direction));
            }
            if (hasConnector(state, direction)) {
                shape = Shapes.or(shape, connectorShape(direction));
            }
        }
        return shape;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof CableBlockEntity cable) {
            Direction direction = resolveInstalledConnectorDirection(state, hitResult, true);
            if (direction != null && cable.hasUniversalConnector(direction)) {
                if (!level.isClientSide) {
                    openUniversalConnectorMenu(player, cable, pos, direction);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (direction != null && cable.hasConnector(direction)) {
                if (!level.isClientSide) {
                    openConnectorMenu(player, cable, pos, direction);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CableBlockEntity cable) {
            cable.dropConnectors(level);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        Direction direction = resolveTargetedConnectorFromPick(state, level, pos, player);
        if (direction == null) {
            PENDING_CONNECTOR_BREAKS.remove(player.getUUID());
        } else {
            PENDING_CONNECTOR_BREAKS.put(player.getUUID(), new ConnectorBreakTarget(pos.immutable(), direction));
        }
        super.attack(state, level, pos, player);
    }

    @Override
    public boolean onDestroyedByPlayer(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            boolean willHarvest,
            FluidState fluid
    ) {
        Direction connectorDirection = resolveTargetedConnectorForBreaking(state, level, pos, player);
        if (connectorDirection != null && removeTargetedConnector(state, level, pos, player, connectorDirection)) {
            return false;
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(
                CABLE_DOWN,
                CABLE_UP,
                CABLE_NORTH,
                CABLE_SOUTH,
                CABLE_WEST,
                CABLE_EAST,
                CONNECTOR_DOWN,
                CONNECTOR_UP,
                CONNECTOR_NORTH,
                CONNECTOR_SOUTH,
                CONNECTOR_WEST,
                CONNECTOR_EAST
        );
    }

    public static boolean hasCableConnection(BlockState state, Direction direction) {
        return state.getBlock() instanceof CableBlock && state.getValue(cableProperty(direction));
    }

    public static boolean hasConnector(BlockState state, Direction direction) {
        return state.getBlock() instanceof CableBlock && state.getValue(connectorProperty(direction));
    }

    public static boolean isDisabled(LevelAccessor level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof CableBlockEntity cable && cable.isLinkDisabled(direction);
    }

    public static BooleanProperty cableProperty(Direction direction) {
        return switch (direction) {
            case DOWN -> CABLE_DOWN;
            case UP -> CABLE_UP;
            case NORTH -> CABLE_NORTH;
            case SOUTH -> CABLE_SOUTH;
            case WEST -> CABLE_WEST;
            case EAST -> CABLE_EAST;
        };
    }

    public static BooleanProperty connectorProperty(Direction direction) {
        return switch (direction) {
            case DOWN -> CONNECTOR_DOWN;
            case UP -> CONNECTOR_UP;
            case NORTH -> CONNECTOR_NORTH;
            case SOUTH -> CONNECTOR_SOUTH;
            case WEST -> CONNECTOR_WEST;
            case EAST -> CONNECTOR_EAST;
        };
    }

    private ItemInteractionResult installConnector(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            Direction direction,
            EnergyConnectorItem connector
    ) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CableBlockEntity cable) {
            if (!cable.installConnector(direction, connector.tier())) {
                return ItemInteractionResult.FAIL;
            }
            level.setBlock(pos, state.setValue(connectorProperty(direction), true), Block.UPDATE_ALL);
            stack.consume(1, player);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    public static ItemInteractionResult installUniversalConnector(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            Direction direction
    ) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CableBlockEntity cable) {
            if (!cable.installUniversalConnector(direction)) {
                return ItemInteractionResult.FAIL;
            }
            level.setBlock(pos, state.setValue(connectorProperty(direction), true), Block.UPDATE_ALL);
            stack.consume(1, player);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static Direction resolveTargetedConnectorForBreaking(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player
    ) {
        if (level.getBlockEntity(pos) instanceof CableBlockEntity cable) {
            ConnectorBreakTarget target = PENDING_CONNECTOR_BREAKS.remove(player.getUUID());
            if (target != null && target.pos().equals(pos) && cable.hasAnyConnector(target.direction())) {
                return target.direction();
            }
        }

        return resolveTargetedConnectorFromPick(state, level, pos, player);
    }

    private static Direction resolveTargetedConnectorFromPick(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player
    ) {
        if (!(level.getBlockEntity(pos) instanceof CableBlockEntity cable)) {
            return null;
        }
        HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
        if (!(hit instanceof BlockHitResult hitResult)
                || hitResult.getType() != HitResult.Type.BLOCK
                || !hitResult.getBlockPos().equals(pos)) {
            return null;
        }

        Direction direction = resolveInstalledConnectorDirection(state, hitResult, false);
        return direction != null && cable.hasAnyConnector(direction) ? direction : null;
    }

    private record ConnectorBreakTarget(BlockPos pos, Direction direction) {
    }

    private static boolean removeTargetedConnector(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            Direction direction
    ) {
        if (!(level.getBlockEntity(pos) instanceof CableBlockEntity cable) || !cable.hasAnyConnector(direction)) {
            return false;
        }
        if (level.isClientSide) {
            level.setBlock(pos, state.setValue(connectorProperty(direction), false), Block.UPDATE_ALL);
            return true;
        }

        ItemStack removed = cable.hasUniversalConnector(direction)
                ? cable.removeUniversalConnector(direction)
                : cable.removeConnector(direction);
        if (!removed.isEmpty() && !player.isCreative()) {
            popResourceFromFace(level, pos, direction, removed);
        }
        level.setBlock(pos, state.setValue(connectorProperty(direction), false), Block.UPDATE_ALL);
        return true;
    }

    private ItemInteractionResult useWrench(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        Direction direction = hitResult.getDirection();
        if (level.getBlockEntity(pos) instanceof CableBlockEntity cable) {
            Direction connectorDirection = resolveInstalledConnectorDirection(state, hitResult, false);
            if (connectorDirection != null) {
                direction = connectorDirection;
            }
        }

        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = level.getBlockState(neighborPos);
        if (neighborState.getBlock() instanceof CableBlock) {
            if (!level.isClientSide) {
                toggleCableLink(level, pos, state, direction, neighborPos, neighborState);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (neighborState.getBlock() instanceof UniversalConnectorBlock) {
            if (!level.isClientSide) {
                toggleExternalLink(level, pos, state, direction);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (level.getBlockEntity(pos) instanceof CableBlockEntity cable) {
            if (cable.hasUniversalConnector(direction)) {
                if (!level.isClientSide) {
                    useWrenchOnUniversalConnector(state, level, pos, player, cable, direction);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            Direction connectorDirection = cable.hasConnector(direction)
                    ? direction
                    : resolveSingleInstalledConnector(cable);
            if (connectorDirection != null) {
                if (!level.isClientSide) {
                    useWrenchOnConnector(state, level, pos, player, cable, connectorDirection);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        Direction cableDirection = resolveSingleCableLinkDirection(level, pos, direction);
        if (cableDirection == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide) {
            toggleCableLink(
                    level,
                    pos,
                    state,
                    cableDirection,
                    pos.relative(cableDirection),
                    level.getBlockState(pos.relative(cableDirection))
            );
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void useWrenchOnConnector(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            CableBlockEntity cable,
            Direction direction
    ) {
        if (player.isShiftKeyDown()) {
            ItemStack removed = cable.removeConnector(direction);
            if (!removed.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), removed);
            }
            level.setBlock(pos, state.setValue(connectorProperty(direction), false), Block.UPDATE_ALL);
        } else {
            openConnectorMenu(player, cable, pos, direction);
        }
    }

    private static void useWrenchOnUniversalConnector(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            CableBlockEntity cable,
            Direction direction
    ) {
        if (player.isShiftKeyDown()) {
            ItemStack removed = cable.removeUniversalConnector(direction);
            if (!removed.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), removed);
            }
            level.setBlock(pos, state.setValue(connectorProperty(direction), false), Block.UPDATE_ALL);
        } else {
            openUniversalConnectorMenu(player, cable, pos, direction);
        }
    }

    private void toggleCableLink(
            Level level,
            BlockPos pos,
            BlockState state,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState
    ) {
        boolean disable = state.getValue(cableProperty(direction));
        setDisabledLink(level, pos, direction, disable);
        setDisabledLink(level, neighborPos, direction.getOpposite(), disable);
        level.setBlock(pos, state.setValue(cableProperty(direction), !disable), Block.UPDATE_ALL);
        level.setBlock(neighborPos, neighborState.setValue(cableProperty(direction.getOpposite()), !disable), Block.UPDATE_ALL);
    }

    private void toggleExternalLink(Level level, BlockPos pos, BlockState state, Direction direction) {
        boolean disable = state.getValue(cableProperty(direction));
        setDisabledLink(level, pos, direction, disable);
        level.setBlock(pos, state.setValue(cableProperty(direction), !disable), Block.UPDATE_ALL);
    }

    private static void setDisabledLink(Level level, BlockPos pos, Direction direction, boolean disabled) {
        if (level.getBlockEntity(pos) instanceof CableBlockEntity cable) {
            cable.setLinkDisabled(direction, disabled);
        }
    }

    private static void openConnectorMenu(Player player, CableBlockEntity cable, BlockPos pos, Direction direction) {
        MenuProvider provider = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.rngtech.cable_connector");
            }

            @Override
            public CableConnectorMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory inventory, Player player) {
                return new CableConnectorMenu(containerId, inventory, cable, direction, cable.connectorMenuData(direction));
            }
        };
        player.openMenu(provider, buffer -> {
            buffer.writeBlockPos(pos);
            buffer.writeByte(direction.ordinal());
        });
    }

    private static void openUniversalConnectorMenu(Player player, CableBlockEntity cable, BlockPos pos, Direction direction) {
        MenuProvider provider = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.rngtech.universal_connector");
            }

            @Override
            public UniversalConnectorMenu createMenu(
                    int containerId,
                    net.minecraft.world.entity.player.Inventory inventory,
                    Player player
            ) {
                return new UniversalConnectorMenu(
                        containerId,
                        inventory,
                        cable.universalConnectorAccess(direction),
                        cable.universalConnector(direction).menuData()
                );
            }
        };
        player.openMenu(provider, buffer -> {
            buffer.writeBlockPos(pos);
            buffer.writeByte(direction.ordinal());
        });
    }

    private static boolean canConnectCable(
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos,
            BlockState neighborState,
            Direction direction
    ) {
        if (neighborState.getBlock() instanceof CableBlock) {
            return !isDisabled(level, pos, direction)
                    && !isDisabled(level, neighborPos, direction.getOpposite());
        }
        if (neighborState.getBlock() instanceof UniversalConnectorBlock) {
            return !isDisabled(level, pos, direction)
                    && UniversalConnectorBlock.canConnectNetworkFrom(neighborState, direction.getOpposite());
        }
        return false;
    }

    public static boolean isValidConnectorTarget(BlockState neighborState) {
        return !neighborState.isAir()
                && !(neighborState.getBlock() instanceof CableBlock)
                && !(neighborState.getBlock() instanceof UniversalConnectorBlock);
    }

    private static boolean shouldDropConnectorForTarget(BlockState neighborState) {
        return !neighborState.isAir() && !isValidConnectorTarget(neighborState);
    }

    private static Direction resolveConnectorInstallDirection(Level level, BlockPos pos, Direction clickedDirection) {
        if (canInstallConnectorOn(level, pos, clickedDirection)) {
            return clickedDirection;
        }

        Direction target = null;
        CableBlockEntity cable = level.getBlockEntity(pos) instanceof CableBlockEntity c ? c : null;
        for (Direction direction : DIRECTIONS) {
            if (direction == clickedDirection || !canInstallConnectorOn(level, pos, direction)) {
                continue;
            }

            // Only auto-pick an unoccupied side when there is exactly one valid candidate. Exact-face clicks may still
            // intentionally replace an existing connector, but ambiguous multi-connector blocks should not replace a
            // random installed side.
            if (cable != null && cable.hasConnector(direction)) {
                continue;
            }

            if (target != null) {
                return null;
            }
            target = direction;
        }
        return target;
    }

    private static boolean canInstallConnectorOn(Level level, BlockPos pos, Direction direction) {
        return isValidConnectorTarget(level.getBlockState(pos.relative(direction)))
                && (!(level.getBlockEntity(pos) instanceof CableBlockEntity cable) || !cable.hasAnyConnector(direction));
    }

    private static Direction resolveSingleInstalledConnector(CableBlockEntity cable) {
        Direction target = null;
        for (Direction direction : DIRECTIONS) {
            if (!cable.hasConnector(direction)) {
                continue;
            }
            if (target != null) {
                return null;
            }
            target = direction;
        }
        return target;
    }

    public static Direction resolveInstalledConnectorDirection(
            BlockState state,
            BlockHitResult hitResult,
            boolean allowSingleFallback
    ) {
        Direction hitSide = hitResult.getDirection();
        Direction localSide = resolveInstalledConnectorDirectionFromLocation(state, hitResult);
        if (localSide != null) {
            return localSide;
        }
        if (hasConnector(state, hitSide)) {
            return hitSide;
        }
        return allowSingleFallback ? resolveSingleInstalledAnyConnector(state) : null;
    }

    private static Direction resolveInstalledConnectorDirectionFromLocation(BlockState state, BlockHitResult hitResult) {
        Vec3 hit = hitResult.getLocation();
        BlockPos pos = hitResult.getBlockPos();
        double x = hit.x - pos.getX();
        double y = hit.y - pos.getY();
        double z = hit.z - pos.getZ();
        double threshold = 4.0D / 16.0D;

        Direction bestDirection = null;
        double bestScore = 0.0D;
        for (Direction direction : DIRECTIONS) {
            if (!hasConnector(state, direction)) {
                continue;
            }
            double score = connectorHitScore(direction, x, y, z, threshold);
            if (score > bestScore) {
                bestScore = score;
                bestDirection = direction;
            }
        }
        return bestDirection;
    }

    private static double connectorHitScore(Direction direction, double x, double y, double z, double threshold) {
        return switch (direction) {
            case DOWN -> y <= threshold ? threshold - y : 0.0D;
            case UP -> y >= 1.0D - threshold ? y - (1.0D - threshold) : 0.0D;
            case NORTH -> z <= threshold ? threshold - z : 0.0D;
            case SOUTH -> z >= 1.0D - threshold ? z - (1.0D - threshold) : 0.0D;
            case WEST -> x <= threshold ? threshold - x : 0.0D;
            case EAST -> x >= 1.0D - threshold ? x - (1.0D - threshold) : 0.0D;
        };
    }

    private static Direction resolveSingleInstalledAnyConnector(BlockState state) {
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

    private static Direction resolveSingleCableLinkDirection(Level level, BlockPos pos, Direction clickedDirection) {
        Direction target = null;
        for (Direction direction : DIRECTIONS) {
            if (direction == clickedDirection || !(level.getBlockState(pos.relative(direction)).getBlock() instanceof CableBlock)) {
                continue;
            }
            if (target != null) {
                return null;
            }
            target = direction;
        }
        return target;
    }

    private static VoxelShape cableShape(Direction direction) {
        return switch (direction) {
            case DOWN -> DOWN_CABLE_SHAPE;
            case UP -> UP_CABLE_SHAPE;
            case NORTH -> NORTH_CABLE_SHAPE;
            case SOUTH -> SOUTH_CABLE_SHAPE;
            case WEST -> WEST_CABLE_SHAPE;
            case EAST -> EAST_CABLE_SHAPE;
        };
    }

    private static VoxelShape connectorShape(Direction direction) {
        return switch (direction) {
            case DOWN -> DOWN_CONNECTOR_SHAPE;
            case UP -> UP_CONNECTOR_SHAPE;
            case NORTH -> NORTH_CONNECTOR_SHAPE;
            case SOUTH -> SOUTH_CONNECTOR_SHAPE;
            case WEST -> WEST_CONNECTOR_SHAPE;
            case EAST -> EAST_CONNECTOR_SHAPE;
        };
    }

    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actualType,
            BlockEntityType<E> expectedType,
            BlockEntityTicker<? super E> ticker
    ) {
        return expectedType == actualType ? (level, pos, state, blockEntity) -> ticker.tick(level, pos, state, (E) blockEntity) : null;
    }
}
