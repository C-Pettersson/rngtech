package com.rngtech.content.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** A machine that loads Job Chips. Its model lights a Job Chip lamp while {@link #WORK_ORDER} is true. */
public abstract class WorkOrderMachineBlock extends BaseMachineBlock {
    public static final BooleanProperty WORK_ORDER = BooleanProperty.create("work_order");

    protected WorkOrderMachineBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WORK_ORDER, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WORK_ORDER);
    }
}
