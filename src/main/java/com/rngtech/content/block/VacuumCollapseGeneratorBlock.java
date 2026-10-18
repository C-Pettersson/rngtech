package com.rngtech.content.block;

import com.rngtech.content.blockentity.BaseMachineBlockEntity;
import com.rngtech.content.blockentity.VacuumCollapseGeneratorBlockEntity;
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

public class VacuumCollapseGeneratorBlock extends BaseMachineBlock {
    public VacuumCollapseGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VacuumCollapseGeneratorBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof VacuumCollapseGeneratorBlockEntity generator) {
            player.openMenu(generator);
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
                ModBlockEntities.VACUUM_COLLAPSE_GENERATOR.get(),
                VacuumCollapseGeneratorBlockEntity::serverTick
        );
    }

    /** While a collapse runs, each Dimensional Stabilizer lens draws motes into the core. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE) || random.nextInt(3) != 0
                || !(level.getBlockEntity(pos) instanceof VacuumCollapseGeneratorBlockEntity generator)
                || generator.visualState().stabilizer().isEmpty()) {
            return;
        }
        double lensX = (random.nextBoolean() ? 3.9 : 12.1) / 16.0 - 0.5;
        double coreZ = 6.5 / 16.0 - 0.5;
        double angle = Math.toRadians(180.0 - state.getValue(FACING).toYRot());
        double coreWorldX = coreZ * Math.sin(angle);
        double coreWorldZ = coreZ * Math.cos(angle);
        double offsetX = lensX * Math.cos(angle);
        double offsetZ = -lensX * Math.sin(angle);
        // Portal particles travel from their offset back to their spawn point, so they spawn at the core.
        level.addParticle(
                ParticleTypes.PORTAL,
                pos.getX() + 0.5 + coreWorldX,
                pos.getY() + 7.75 / 16.0,
                pos.getZ() + 0.5 + coreWorldZ,
                offsetX,
                (random.nextDouble() - 0.5) * 0.1,
                offsetZ
        );
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof VacuumCollapseGeneratorBlockEntity generator) {
                generator.dropInventory(level);
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
