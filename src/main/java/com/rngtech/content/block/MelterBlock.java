package com.rngtech.content.block;

import com.rngtech.content.blockentity.BaseMachineBlockEntity;
import com.rngtech.content.blockentity.MelterBlockEntity;
import com.rngtech.content.blockentity.MelterVisualState;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineProgressionState;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class MelterBlock extends WorkOrderMachineBlock {
    /** Where the rolls meet in the feed hopper, in model pixels; see tools/blockmodels/melter.py. */
    private static final double NIP_Z = 7.25;
    private static final double NIP_TOP = 15.0;

    public MelterBlock(Properties properties) {
        super(properties);
    }

    /** Crumbs of the ingredients fly up from the rolls while the Melter works, and a little smoke leaves the hopper. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE) || !(level.getBlockEntity(pos) instanceof MelterBlockEntity melter)) {
            return;
        }
        MelterVisualState visual = melter.visualState();
        double angle = Math.toRadians(180.0 - state.getValue(FACING).toYRot());
        double x = (8.0 + random.nextGaussian() * 1.2) / 16.0 - 0.5;
        double z = (NIP_Z + random.nextGaussian() * 0.6) / 16.0 - 0.5;
        double worldX = pos.getX() + 0.5 + x * Math.cos(angle) + z * Math.sin(angle);
        double worldZ = pos.getZ() + 0.5 - x * Math.sin(angle) + z * Math.cos(angle);
        double y = pos.getY() + NIP_TOP / 16.0;
        if (!visual.crushHead().isEmpty() && visual.primary() != Items.AIR && random.nextInt(3) == 0) {
            ItemStack crumb = new ItemStack(random.nextBoolean() || visual.secondary() == Items.AIR ? visual.primary() : visual.secondary());
            level.addParticle(new ItemParticleOption(ParticleTypes.ITEM, crumb), worldX, y, worldZ,
                    random.nextGaussian() * 0.02, 0.05, random.nextGaussian() * 0.02);
        }
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SMOKE, worldX, y + 0.05, worldZ, 0.0, 0.02, 0.0);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MelterBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MelterBlockEntity melter) {
            player.openMenu(melter);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(blockEntityType, ModBlockEntities.MELTER.get(), MelterBlockEntity::serverTick);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MelterBlockEntity melter) {
            melter.dropInventory(level);
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
            MachineProgressionState progression = machine.machineProgression();
            if (!progression.equals(MachineProgressionState.EMPTY)
                    && !progression.equals(MachineProgressionState.EMPTY.forFamily(MachineMasteryFamily.MELTER))) {
                stack.set(ModDataComponents.MACHINE_PROGRESSION.get(), progression);
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
