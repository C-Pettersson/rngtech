package com.rngtech.content.block;

import com.rngtech.content.blockentity.BaseMachineBlockEntity;
import com.rngtech.content.blockentity.ComponentRecyclerBlockEntity;
import com.rngtech.content.blockentity.ComponentRecyclerVisualState;
import com.rngtech.content.recycling.ComponentRecyclerChassis;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.rpg.MachineTraits;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
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

public class ComponentRecyclerBlock extends BaseMachineBlock {
    /** Where the item being recycled lies, in model pixels; see tools/blockmodels/component_recycler.py. */
    private static final double TABLE_Z = 5.0;
    private static final double TABLE_TOP = 7.0;
    private static final double RUNNER_Z = 8.0;
    private static final double RUNNER_TOP = 7.0;
    private final ComponentRecyclerChassis chassis;

    public ComponentRecyclerBlock(ComponentRecyclerChassis chassis, Properties properties) {
        super(properties);
        this.chassis = chassis;
    }

    public ComponentRecyclerChassis chassis() {
        return chassis;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ComponentRecyclerBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ComponentRecyclerBlockEntity recycler) {
            player.openMenu(recycler);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(
                blockEntityType,
                ModBlockEntities.COMPONENT_RECYCLER.get(),
                ComponentRecyclerBlockEntity::serverTick
        );
    }

    /**
     * Bits of the item fly off the turntable while the claw takes it apart, with sparks from the driver; the Crude
     * Recycler sheds bits of its input from the runner stone instead.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE) || !(level.getBlockEntity(pos) instanceof ComponentRecyclerBlockEntity recycler)) {
            return;
        }
        ComponentRecyclerVisualState visual = recycler.visualState();
        double angle = Math.toRadians(180.0 - state.getValue(FACING).toYRot());
        double x = (8.0 + random.nextGaussian() * 0.8) / 16.0 - 0.5;
        double z = ((chassis.manual() ? RUNNER_Z : TABLE_Z) + random.nextGaussian() * 0.8) / 16.0 - 0.5;
        double worldX = pos.getX() + 0.5 + x * Math.cos(angle) + z * Math.sin(angle);
        double worldZ = pos.getZ() + 0.5 - x * Math.sin(angle) + z * Math.cos(angle);
        double y = pos.getY() + (chassis.manual() ? RUNNER_TOP : TABLE_TOP) / 16.0;
        if (!visual.input().isEmpty() && random.nextInt(3) == 0) {
            level.addParticle(new ItemParticleOption(ParticleTypes.ITEM, visual.input()), worldX, y, worldZ,
                    random.nextGaussian() * 0.03, 0.06, random.nextGaussian() * 0.03);
        }
        if (!chassis.manual() && !visual.head().isEmpty() && random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, worldX, y + 0.05, worldZ,
                    random.nextGaussian() * 0.04, 0.02, random.nextGaussian() * 0.04);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ComponentRecyclerBlockEntity recycler) {
            recycler.dropInventory(level);
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
