package com.rngtech.content.block;

import com.rngtech.content.blockentity.BaseMachineBlockEntity;
import com.rngtech.content.blockentity.CorrosionCellBlockEntity;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.rpg.MachineTraits;

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

public class CorrosionCellBlock extends BaseMachineBlock {
    public CorrosionCellBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CorrosionCellBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CorrosionCellBlockEntity cell) {
            player.openMenu(cell);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected <T extends BlockEntity> BlockEntityTicker<T> machineTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(blockEntityType, ModBlockEntities.CORROSION_CELL.get(), CorrosionCellBlockEntity::serverTick);
    }

    /** Hydrogen from the dissolving fuel vents as a faint mist from the stack, more of it at higher current. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof CorrosionCellBlockEntity cell)) {
            return;
        }
        float current = cell.visualState().current();
        if (current <= 0.0F) {
            return;
        }
        double x = 2.5 / 16.0 - 0.5;
        double z = 11.75 / 16.0 - 0.5;
        double angle = Math.toRadians(180.0 - state.getValue(FACING).toYRot());
        double worldX = x * Math.cos(angle) + z * Math.sin(angle);
        double worldZ = -x * Math.sin(angle) + z * Math.cos(angle);
        int puffs = 1 + (random.nextFloat() < current ? 1 : 0);
        for (int puff = 0; puff < puffs; puff++) {
            level.addParticle(
                    ParticleTypes.WHITE_SMOKE,
                    pos.getX() + 0.5 + worldX + (random.nextDouble() - 0.5) * 0.05,
                    pos.getY() + 15.9 / 16.0,
                    pos.getZ() + 0.5 + worldZ + (random.nextDouble() - 0.5) * 0.05,
                    0.0,
                    0.02 + 0.03 * current,
                    0.0
            );
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof CorrosionCellBlockEntity cell) {
                cell.dropInventory(level);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        if (level.getBlockEntity(pos) instanceof BaseMachineBlockEntity machine) {
            MachineTraits traits = machine.machineTraits();
            if (!traits.isEmpty()) {
                stack.set(ModDataComponents.MACHINE_TRAITS.get(), traits);
            }
        }
        return stack;
    }

    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actualType,
            BlockEntityType<E> expectedType,
            BlockEntityTicker<? super E> ticker
    ) {
        return expectedType == actualType ? (level, pos, state, blockEntity) -> ticker.tick(level, pos, state, (E) blockEntity) : null;
    }
}
