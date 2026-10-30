package com.rngtech.content.block;

import com.rngtech.content.blockentity.BaseMachineBlockEntity;
import com.rngtech.content.blockentity.CavitationGeneratorBlockEntity;
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

public class CavitationGeneratorBlock extends BaseMachineBlock {
    public CavitationGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CavitationGeneratorBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CavitationGeneratorBlockEntity generator) {
            player.openMenu(generator);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) {
            return null;
        }
        return withBodyCues(createTickerHelper(blockEntityType, ModBlockEntities.CAVITATION_GENERATOR.get(), CavitationGeneratorBlockEntity::serverTick));
    }

    /** Heat haze rises off the rear fins once heat strain passes 60%. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (BaseMachineBlockEntity.skipsParticle(level, pos, random)) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof CavitationGeneratorBlockEntity generator)) {
            return;
        }
        float strain = generator.visualState().strain();
        if (strain < 0.6F || random.nextFloat() > strain) {
            return;
        }
        double x = (4.0 + random.nextDouble() * 8.0) / 16.0 - 0.5;
        double z = (11.5 + random.nextDouble() * 3.25) / 16.0 - 0.5;
        double angle = Math.toRadians(180.0 - state.getValue(FACING).toYRot());
        double worldX = x * Math.cos(angle) + z * Math.sin(angle);
        double worldZ = -x * Math.sin(angle) + z * Math.cos(angle);
        level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5 + worldX, pos.getY() + 15.2 / 16.0, pos.getZ() + 0.5 + worldZ, 0.0, 0.03, 0.0);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CavitationGeneratorBlockEntity generator) {
            generator.dropInventory(level);
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
