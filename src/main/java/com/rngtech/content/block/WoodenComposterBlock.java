package com.rngtech.content.block;

import com.rngtech.content.blockentity.WoodenComposterBlockEntity;
import com.rngtech.content.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.joml.Vector3f;

public class WoodenComposterBlock extends Block implements EntityBlock {
    public static final BooleanProperty COMPOSTING = BooleanProperty.create("composting");
    private static final DustParticleOptions COMPOST_BUBBLE = new DustParticleOptions(new Vector3f(0.33F, 0.19F, 0.08F), 0.85F);

    public WoodenComposterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(COMPOSTING, false));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WoodenComposterBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof WoodenComposterBlockEntity composter) {
            player.openMenu(composter);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(COMPOSTING) || random.nextInt(3) != 0) {
            return;
        }
        int count = 1 + random.nextInt(2);
        for (int i = 0; i < count; i++) {
            double x = pos.getX() + 0.25D + random.nextDouble() * 0.5D;
            double y = pos.getY() + 1.02D + random.nextDouble() * 0.04D;
            double z = pos.getZ() + 0.25D + random.nextDouble() * 0.5D;
            double xSpeed = (random.nextDouble() - 0.5D) * 0.025D;
            double ySpeed = 0.018D + random.nextDouble() * 0.025D;
            double zSpeed = (random.nextDouble() - 0.5D) * 0.025D;
            level.addParticle(COMPOST_BUBBLE, x, y, z, xSpeed, ySpeed, zSpeed);
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(blockEntityType, ModBlockEntities.WOODEN_COMPOSTER.get(), WoodenComposterBlockEntity::serverTick);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof WoodenComposterBlockEntity composter) {
            composter.dropInventory(level);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public static void setComposting(Level level, BlockPos pos, BlockState state, boolean composting) {
        if (state.getValue(COMPOSTING) != composting) {
            level.setBlock(pos, state.setValue(COMPOSTING, composting), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COMPOSTING);
    }

    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actualType,
            BlockEntityType<E> expectedType,
            BlockEntityTicker<? super E> ticker
    ) {
        return expectedType == actualType ? (level, pos, state, blockEntity) -> ticker.tick(level, pos, state, (E) blockEntity) : null;
    }
}
