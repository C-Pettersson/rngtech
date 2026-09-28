package com.rngtech.content.item;

import com.rngtech.content.entity.ForestryCartEntity;
import com.rngtech.content.recycling.RecyclingData;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.rpg.MachineNameGenerator;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraitRoller;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.MachineType;
import com.rngtech.rpg.ModifierEligibilityProfiles;
import com.rngtech.rpg.progression.ForestryCompanionPassiveTree;
import com.rngtech.rpg.progression.MachineProgressionState;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class ForestryCartItem extends Item {
    public static final int COMPONENT_STAGE = 2;

    public ForestryCartItem(Properties properties) {
        super(properties);
    }

    public static MachineTraits storedTraits(ItemStack stack) {
        MachineTraits traits = stack.get(ModDataComponents.MACHINE_TRAITS.get());
        return traits == null || RecyclingData.isStripped(stack) ? MachineTraits.EMPTY : traits;
    }

    public static MachineTraits traits(ItemStack stack) {
        return storedTraits(stack);
    }

    public static MachineStatAccumulator effectiveStats(ItemStack stack) {
        return effectiveStats(
                traits(stack),
                stack.getOrDefault(ModDataComponents.MACHINE_PROGRESSION.get(), MachineProgressionState.EMPTY)
        );
    }

    public static MachineStatAccumulator effectiveStats(MachineTraits traits) {
        return effectiveStats(traits, MachineProgressionState.EMPTY);
    }

    public static MachineStatAccumulator effectiveStats(MachineTraits traits, MachineProgressionState progression) {
        MachineStatAccumulator stats = MachineStatAccumulator.forestryCompanionBase();
        stats.apply(traits);
        ForestryCompanionPassiveTree.applyStats(stats, progression);
        return stats;
    }

    @Override
    public void onCraftedPostProcess(ItemStack stack, Level level) {
        super.onCraftedPostProcess(stack, level);
        CraftedTraitOutputs.handleCrafted(stack, level.random);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (CraftedTraitOutputs.isUnidentified(stack)) {
            return CraftedTraitOutputs.unidentifiedName(super.getName(stack));
        }
        MachineTraits traits = traits(stack);
        return traits.isEmpty() ? super.getName(stack) : MachineNameGenerator.generatedName(traits, super.getName(stack));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos railPos = railPos(level, context.getClickedPos(), context.getClickedFace());
        if (railPos == null) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        if (RecyclingData.isStripped(stack)) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        boolean temporaryPlacementRoll = !stack.has(ModDataComponents.MACHINE_TRAITS.get());
        UnidentifiedTraitRoll unidentifiedRoll = stack.get(ModDataComponents.UNIDENTIFIED_TRAIT_ROLL.get());
        Boolean identifyOnCraft = stack.get(ModDataComponents.IDENTIFY_TRAIT_ROLL_ON_CRAFT.get());
        if (temporaryPlacementRoll) {
            CraftedTraitOutputs.rollIfMissing(stack, level.random);
        }

        ForestryCartEntity cart = new ForestryCartEntity((ServerLevel) level, railPos.getX() + 0.5D, railPos.getY() + 0.1D, railPos.getZ() + 0.5D);
        Player player = context.getPlayer();
        Direction direction = player == null ? Direction.NORTH : player.getDirection();
        if (player != null) {
            cart.setOwner(player);
        }
        if (stack.has(DataComponents.CUSTOM_NAME)) {
            cart.setCustomName(stack.get(DataComponents.CUSTOM_NAME));
        }
        cart.setMachineTraits(traits(stack));
        cart.setMachineProgression(stack.getOrDefault(ModDataComponents.MACHINE_PROGRESSION.get(), MachineProgressionState.EMPTY));
        cart.setRouteDirection(direction);
        level.addFreshEntity(cart);
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        } else if (temporaryPlacementRoll) {
            stack.remove(ModDataComponents.MACHINE_TRAITS.get());
            if (unidentifiedRoll != null) {
                stack.set(ModDataComponents.UNIDENTIFIED_TRAIT_ROLL.get(), unidentifiedRoll);
            }
            if (identifyOnCraft != null) {
                stack.set(ModDataComponents.IDENTIFY_TRAIT_ROLL_ON_CRAFT.get(), identifyOnCraft);
            } else {
                stack.remove(ModDataComponents.IDENTIFY_TRAIT_ROLL_ON_CRAFT.get());
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        if (RecyclingData.isStripped(stack)) {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.recycling_stripped").withStyle(ChatFormatting.GRAY));
        }
        MachineTraitTooltip.appendUnidentified(stack, tooltipComponents);
        MachineTraits traits = traits(stack);
        if (stack.has(ModDataComponents.MACHINE_TRAITS.get()) || RecyclingData.isStripped(stack)) {
            MachineTraitTooltip.appendTraitHeader(traits, tooltipComponents, true);
        } else {
            MachineTraitTooltip.appendUnrolledTraitHeader(
                    MachineTraitRoller.refinementPotentialRange(
                            ModifierEligibilityProfiles.forMachine(MachineType.FORESTRY_COMPANION),
                            COMPONENT_STAGE
                    ),
                    tooltipComponents
            );
        }
        tooltipComponents.add(Component.translatable("rngtech.tooltip.forestry_companion.place").withStyle(ChatFormatting.GRAY));
        if (MachineTraitTooltip.shouldShowMaterialData()) {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.forestry_companion.gear").withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable("rngtech.tooltip.forestry_companion.affixes").withStyle(ChatFormatting.GRAY));
        }
        MachineProgressionState progression = stack.get(ModDataComponents.MACHINE_PROGRESSION.get());
        if (progression != null) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.machine_progression",
                    progression.level(),
                    progression.xp()
            ).withStyle(ChatFormatting.GRAY));
        }
        MachineTraitTooltip.appendBaseStats(stack, tooltipComponents);
        MachineTraitTooltip.appendTraitDetails(traits, tooltipComponents);
        if (!RecyclingData.isStripped(stack)) {
            MachineTraitTooltip.appendTraitKeyHints(tooltipComponents);
        }
    }

    private static BlockPos railPos(Level level, BlockPos clickedPos, Direction clickedFace) {
        if (isRail(level, clickedPos)) {
            return clickedPos;
        }
        BlockPos adjacent = clickedPos.relative(clickedFace);
        return isRail(level, adjacent) ? adjacent : null;
    }

    private static boolean isRail(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(BlockTags.RAILS);
    }
}
