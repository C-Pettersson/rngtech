package com.rngtech.content.block;

import com.rngtech.content.blockentity.BaseMachineBlockEntity;
import com.rngtech.content.blockentity.PhaseCoilBlockEntity;
import com.rngtech.content.blockentity.PhaseLine;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.rpg.MachineTraits;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** One position of a Phase Array line. Blank until a Glyph is inscribed; lit while its line is formed. */
public class PhaseCoilBlock extends BaseMachineBlock {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public PhaseCoilBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FORMED, false));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PhaseCoilBlockEntity(pos, state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(oldState.getBlock())) {
            PhaseLine.requestNearbyCradleRescans(level, pos);
        }
        super.onPlace(state, level, pos, oldState, movedByPiston);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) {
            PhaseLine.requestNearbyCradleRescans(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            PhaseLine.requestNearbyCradleRescans(level, pos);
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
            if (machine instanceof PhaseCoilBlockEntity coil && coil.coilTier() > 0) {
                stack.set(ModDataComponents.COIL_TIER.get(), coil.coilTier());
            }
        }
        return stack;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FORMED);
    }
}
