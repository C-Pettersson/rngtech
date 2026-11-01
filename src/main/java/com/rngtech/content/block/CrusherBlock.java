package com.rngtech.content.block;

import com.rngtech.content.blockentity.BaseMachineBlockEntity;
import com.rngtech.content.blockentity.CrusherBlockEntity;
import com.rngtech.content.blockentity.CrusherVisualState;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.progression.MachineProgressionState;

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

public class CrusherBlock extends WorkOrderMachineBlock {
    /** Height of the nip under the primary rolls, in model pixels; see tools/blockmodels/crusher.py. */
    private static final double NIP_Y = 6.0;
    private final CrusherChassisMaterial material;

    public CrusherBlock(CrusherChassisMaterial material, Properties properties) {
        super(properties);
        this.material = material;
    }

    public CrusherChassisMaterial material() {
        return material;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrusherBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CrusherBlockEntity crusher) {
            player.openMenu(crusher);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) {
            return null;
        }
        return withBodyCues(createTickerHelper(blockEntityType, ModBlockEntities.CRUSHER.get(), CrusherBlockEntity::serverTick));
    }

    /** Grit of the input falls from the nip under the rolls while they crush; a jammed Crusher puffs smoke instead. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof CrusherBlockEntity crusher)) {
            return;
        }
        CrusherVisualState visual = crusher.visualState();
        double angle = Math.toRadians(180.0 - state.getValue(FACING).toYRot());
        double x = (8.0 + random.nextGaussian() * 0.6) / 16.0 - 0.5;
        double z = (4.0 + random.nextDouble() * 4.0) / 16.0 - 0.5;
        double worldX = pos.getX() + 0.5 + x * Math.cos(angle) + z * Math.sin(angle);
        double worldZ = pos.getZ() + 0.5 - x * Math.sin(angle) + z * Math.cos(angle);
        double y = pos.getY() + NIP_Y / 16.0;
        if (visual.jammed() && random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SMOKE, worldX, y, worldZ, 0.0, 0.03, 0.0);
        } else if (state.getValue(ACTIVE) && !visual.input().isEmpty()) {
            for (int i = 0; i < 2; i++) {
                level.addParticle(new ItemParticleOption(ParticleTypes.ITEM, visual.input()), worldX, y, worldZ,
                        random.nextGaussian() * 0.01, -0.05, random.nextGaussian() * 0.01);
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof CrusherBlockEntity crusher) {
                crusher.dropInventory(level);
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
            if (!machine.machineProgression().equals(MachineProgressionState.EMPTY)) {
                stack.set(ModDataComponents.MACHINE_PROGRESSION.get(), machine.machineProgression());
            }
            if (machine instanceof CrusherBlockEntity crusher && !crusher.persistentBanks().isEmpty()) {
                stack.set(ModDataComponents.OUTPUT_BANKS.get(), crusher.persistentBanks());
            }
        }
        if (level.getBlockEntity(pos) instanceof BaseMachineBlockEntity jewelled && !jewelled.jewels().isEmpty()) {
            stack.set(ModDataComponents.JEWELS.get(), jewelled.jewels());
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
