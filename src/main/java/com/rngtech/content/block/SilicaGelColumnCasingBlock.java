package com.rngtech.content.block;

import com.rngtech.content.blockentity.SilicaGelDehumidifierBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class SilicaGelColumnCasingBlock extends Block {
    public static final EnumProperty<ColumnPart> PART = EnumProperty.create("part", ColumnPart.class);

    public SilicaGelColumnCasingBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, ColumnPart.SINGLE));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!state.is(oldState.getBlock()) && !level.isClientSide) {
            SilicaGelDehumidifierBlockEntity.requestControllerRescan(level, pos);
        }
        super.onPlace(state, level, pos, oldState, movedByPiston);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            BlockPos fromPos,
            boolean isMoving
    ) {
        if (!level.isClientSide) {
            SilicaGelDehumidifierBlockEntity.requestControllerRescan(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            SilicaGelDehumidifierBlockEntity.requestControllerRescan(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    public enum ColumnPart implements StringRepresentable {
        SINGLE("single"),
        LOWER("lower"),
        UPPER("upper");

        private final String serializedName;

        ColumnPart(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}
