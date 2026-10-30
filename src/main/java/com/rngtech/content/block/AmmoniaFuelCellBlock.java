package com.rngtech.content.block;

import com.rngtech.content.blockentity.AmmoniaFuelCellBlockEntity;
import com.rngtech.content.blockentity.BaseMachineBlockEntity;
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

public class AmmoniaFuelCellBlock extends BaseMachineBlock {
    public AmmoniaFuelCellBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AmmoniaFuelCellBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AmmoniaFuelCellBlockEntity fuelCell) {
            player.openMenu(fuelCell);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) {
            return null;
        }
        return withBodyCues(createTickerHelper(blockEntityType, ModBlockEntities.AMMONIA_FUEL_CELL.get(), AmmoniaFuelCellBlockEntity::serverTick));
    }

    /** Nitrogen and water vapour leave the vent stack above the residue trap while the cell runs. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (BaseMachineBlockEntity.skipsParticle(level, pos, random)) {
            return;
        }
        if (!state.getValue(ACTIVE) || random.nextInt(3) != 0) {
            return;
        }
        double x = 14.625 / 16.0 - 0.5;
        double z = 14.625 / 16.0 - 0.5;
        double angle = Math.toRadians(180.0 - state.getValue(FACING).toYRot());
        double worldX = x * Math.cos(angle) + z * Math.sin(angle);
        double worldZ = -x * Math.sin(angle) + z * Math.cos(angle);
        level.addParticle(ParticleTypes.WHITE_SMOKE, pos.getX() + 0.5 + worldX, pos.getY() + 15.5 / 16.0, pos.getZ() + 0.5 + worldZ, 0.0, 0.03, 0.0);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AmmoniaFuelCellBlockEntity fuelCell) {
            fuelCell.dropInventory(level);
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
