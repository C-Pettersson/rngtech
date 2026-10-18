package com.rngtech.content.block;

import com.rngtech.content.blockentity.AlgaePhotobioreactorBlockEntity;
import com.rngtech.content.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class AlgaePhotobioreactorBlock extends BaseMachineBlock {
    public AlgaePhotobioreactorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlgaePhotobioreactorBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AlgaePhotobioreactorBlockEntity reactor) {
            player.openMenu(reactor);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected <T extends BlockEntity> BlockEntityTicker<T> machineTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(
                blockEntityType,
                ModBlockEntities.ALGAE_PHOTOBIOREACTOR.get(),
                AlgaePhotobioreactorBlockEntity::serverTick
        );
    }

    /** Oxygen bubbles off the degassing vent while the culture grows. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE) || random.nextInt(4) != 0) {
            return;
        }
        double x = 2.5 / 16.0 - 0.5;
        double z = 5.0 / 16.0 - 0.5;
        double angle = Math.toRadians(180.0 - state.getValue(FACING).toYRot());
        double worldX = x * Math.cos(angle) + z * Math.sin(angle);
        double worldZ = -x * Math.sin(angle) + z * Math.cos(angle);
        level.addParticle(ParticleTypes.BUBBLE_POP, pos.getX() + 0.5 + worldX, pos.getY() + 1.02, pos.getZ() + 0.5 + worldZ, 0.0, 0.02, 0.0);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AlgaePhotobioreactorBlockEntity reactor) {
            reactor.dropInventory(level);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return super.getCloneItemStack(level, pos, state);
    }

    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actualType,
            BlockEntityType<E> expectedType,
            BlockEntityTicker<? super E> ticker
    ) {
        return expectedType == actualType ? (level, pos, state, blockEntity) -> ticker.tick(level, pos, state, (E) blockEntity) : null;
    }
}
