package com.rngtech.content.block;

import com.rngtech.content.blockentity.BaseMachineBlockEntity;
import com.rngtech.content.blockentity.PhaseCoilBlockEntity;
import com.rngtech.content.blockentity.PhaseLine;
import com.rngtech.content.item.GlyphItem;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.PhaseArrayRules;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/** One position of a Phase Array line. Blank until a Glyph is inscribed; lit while its line is formed. */
public class PhaseCoilBlock extends BaseMachineBlock {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    /** The inscribed Glyph tier, 0 while blank, mirrored from the block entity so the model can show it. */
    public static final IntegerProperty TIER = IntegerProperty.create("tier", 0, PhaseArrayRules.GLYPH_TIERS);

    public PhaseCoilBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FORMED, false).setValue(TIER, 0));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PhaseCoilBlockEntity(pos, state);
    }

    /** Along an adjacent line part when there is one, else the way the player faces, so walking either way reads the same. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction along = PhaseLine.axisNextTo(context.getLevel(), context.getClickedPos());
        return defaultBlockState().setValue(FACING, along != null ? along : context.getHorizontalDirection());
    }

    /** Right-click with a Glyph inscribes it: the Coil keeps the tier for good and the Glyph is used up. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!GlyphItem.isGlyph(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof PhaseCoilBlockEntity coil) || !coil.inscribe(GlyphItem.tier(stack))) {
            return ItemInteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 1.0F + 0.1F * coil.coilTier());
        return ItemInteractionResult.CONSUME;
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
        builder.add(FORMED, TIER);
    }
}
