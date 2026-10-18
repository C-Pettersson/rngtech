package com.rngtech.content.block;

import com.rngtech.content.blockentity.BaseMachineBlockEntity;
import com.rngtech.content.blockentity.GasChemistryBlockEntity;
import com.rngtech.content.chemistry.GasChemistryMachine;
import com.rngtech.content.energy.CombustionMixture;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.rpg.MachineTraits;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
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

public class GasChemistryBlock extends BaseMachineBlock {
    private final GasChemistryMachine machine;

    public GasChemistryBlock(GasChemistryMachine machine, Properties properties) {
        super(properties);
        this.machine = machine;
    }

    public GasChemistryMachine machine() {
        return machine;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GasChemistryBlockEntity(pos, state, machine);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GasChemistryBlockEntity gasChemistry) {
            player.openMenu(gasChemistry);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected <T extends BlockEntity> BlockEntityTicker<T> machineTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(blockEntityType, ModBlockEntities.GAS_CHEMISTRY.get(), GasChemistryBlockEntity::serverTick);
    }

    /**
     * A firing Syngas Combustor smokes from its exhaust stack: thin wisps when lean, heavy black smoke when rich. A running
     * Coal Gasifier steams off its residue bin, and a running Steam Methane Reformer smokes from its flue stack.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE)) {
            return;
        }
        if (machine == GasChemistryMachine.COAL_GASIFIER) {
            if (random.nextFloat() < 0.2F) {
                addModelParticle(level, pos, state, ParticleTypes.WHITE_SMOKE, 8.0, 4.0, 3.25, 0.02);
            }
            return;
        }
        if (machine == GasChemistryMachine.STEAM_METHANE_REFORMER) {
            if (random.nextInt(3) == 0) {
                addModelParticle(level, pos, state, ParticleTypes.SMOKE, 14.625, 16.1, 12.875, 0.04);
            }
            return;
        }
        if (machine != GasChemistryMachine.SYNGAS_COMBUSTOR) {
            return;
        }
        int mixture = level.getBlockEntity(pos) instanceof GasChemistryBlockEntity combustor
                ? combustor.visualState().mixture()
                : CombustionMixture.RATED_MIXTURE;
        double richness = (mixture - CombustionMixture.MIN_MIXTURE) / (double) (CombustionMixture.MAX_MIXTURE - CombustionMixture.MIN_MIXTURE);
        if (random.nextDouble() > 0.15 + 0.6 * richness) {
            return;
        }
        double x = 8.0 / 16.0 - 0.5;
        double z = 9.125 / 16.0 - 0.5;
        double angle = Math.toRadians(180.0 - state.getValue(FACING).toYRot());
        double worldX = x * Math.cos(angle) + z * Math.sin(angle);
        double worldZ = -x * Math.sin(angle) + z * Math.cos(angle);
        double spread = (random.nextDouble() - 0.5) * 0.08;
        boolean heavy = mixture > CombustionMixture.RATED_MIXTURE && random.nextDouble() < richness;
        level.addParticle(heavy ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE,
                pos.getX() + 0.5 + worldX + spread, pos.getY() + 1.0, pos.getZ() + 0.5 + worldZ - spread, 0.0, 0.04, 0.0);
    }

    /** Spawns a particle at a model position in pixels, front facing north, turned with the block's facing. */
    private static void addModelParticle(
            Level level, BlockPos pos, BlockState state, ParticleOptions particle, double x, double y, double z, double rise
    ) {
        double localX = x / 16.0 - 0.5;
        double localZ = z / 16.0 - 0.5;
        double angle = Math.toRadians(180.0 - state.getValue(FACING).toYRot());
        double worldX = localX * Math.cos(angle) + localZ * Math.sin(angle);
        double worldZ = -localX * Math.sin(angle) + localZ * Math.cos(angle);
        level.addParticle(particle, pos.getX() + 0.5 + worldX, pos.getY() + y / 16.0, pos.getZ() + 0.5 + worldZ, 0.0, rise, 0.0);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof GasChemistryBlockEntity gasChemistry) {
            gasChemistry.dropInventory(level);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        if (level.getBlockEntity(pos) instanceof BaseMachineBlockEntity machineBlockEntity) {
            MachineTraits traits = machineBlockEntity.machineTraits();
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
