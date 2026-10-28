package com.rngtech.content.block;

import com.rngtech.content.blockentity.SilicaGelDehumidifierBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class SilicaGelColumnCasingBlock extends Block {
    public static final EnumProperty<ColumnPart> PART = EnumProperty.create("part", ColumnPart.class);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public SilicaGelColumnCasingBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, ColumnPart.SINGLE).setValue(ACTIVE, false));
    }

    /** Condensation beads up on the glass of a working column and runs off. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE)) {
            return;
        }
        Direction side = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        double across = 0.3 + random.nextDouble() * 0.4;
        double x = side.getAxis() == Direction.Axis.X ? 0.5 + side.getStepX() * 0.52 : across;
        double z = side.getAxis() == Direction.Axis.Z ? 0.5 + side.getStepZ() * 0.52 : across;
        double y = 0.15 + random.nextDouble() * 0.75;
        level.addParticle(ParticleTypes.DRIPPING_WATER, pos.getX() + x, pos.getY() + y, pos.getZ() + z, 0.0, 0.0, 0.0);
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
        builder.add(PART, ACTIVE);
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
