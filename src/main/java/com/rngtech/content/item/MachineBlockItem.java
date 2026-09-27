package com.rngtech.content.item;

import com.rngtech.content.recycling.RecyclingData;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.rpg.MachineImplicitCatalog;
import com.rngtech.rpg.MachineNameGenerator;
import com.rngtech.rpg.MachineTraitRoller;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.MachineType;
import com.rngtech.rpg.progression.MachineProgressionState;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class MachineBlockItem extends BlockItem {
    private final MachineType machineType;

    public MachineBlockItem(Block block, MachineType machineType, Item.Properties properties) {
        super(block, properties);
        this.machineType = machineType;
    }

    public MachineType machineType() {
        return machineType;
    }

    @Override
    public void onCraftedPostProcess(ItemStack stack, Level level) {
        super.onCraftedPostProcess(stack, level);
        CraftedTraitOutputs.handleCrafted(stack, level.random);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        if (RecyclingData.isStripped(stack)) {
            return InteractionResult.FAIL;
        }
        boolean temporaryPlacementRoll = !level.isClientSide && !stack.has(ModDataComponents.MACHINE_TRAITS.get());
        UnidentifiedTraitRoll unidentifiedRoll = stack.get(ModDataComponents.UNIDENTIFIED_TRAIT_ROLL.get());
        if (temporaryPlacementRoll) {
            rollTraitsIfMissing(stack, level);
        }

        InteractionResult result = super.place(context);
        if (temporaryPlacementRoll) {
            stack.remove(ModDataComponents.MACHINE_TRAITS.get());
            if (!stack.isEmpty() && unidentifiedRoll != null) {
                stack.set(ModDataComponents.UNIDENTIFIED_TRAIT_ROLL.get(), unidentifiedRoll);
            }
        }
        return result;
    }

    protected void rollTraitsIfMissing(ItemStack stack, Level level) {
        CraftedTraitOutputs.rollIfMissing(stack, level.random);
    }

    protected int componentStage() {
        return 0;
    }

    public final int recyclingComponentStage() {
        return componentStage();
    }

    protected int modifierRollComponentStage() {
        return componentStage();
    }

    public final int traitRollComponentStage() {
        return modifierRollComponentStage();
    }

    protected void appendMaterialTooltip(ItemStack stack, List<Component> tooltipComponents) {
    }

    @Override
    public Component getName(ItemStack stack) {
        if (CraftedTraitOutputs.isUnidentified(stack)) {
            return CraftedTraitOutputs.unidentifiedName(super.getName(stack));
        }
        MachineTraits traits = stack.get(ModDataComponents.MACHINE_TRAITS.get());
        if (traits == null) {
            return super.getName(stack);
        }
        return MachineNameGenerator.generatedName(traits, super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        if (RecyclingData.isStripped(stack)) {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.recycling_stripped").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        MachineTraitTooltip.appendUnidentified(stack, tooltipComponents);
        MachineTraits stored = stack.getOrDefault(ModDataComponents.MACHINE_TRAITS.get(), MachineTraits.EMPTY);
        MachineTraits traits = MachineImplicitCatalog.effectiveTraits(stored, stack);
        if (stack.has(ModDataComponents.MACHINE_TRAITS.get()) || RecyclingData.isStripped(stack)) {
            MachineTraitTooltip.appendTraitHeader(traits, tooltipComponents, true);
        } else {
            MachineTraitTooltip.appendUnrolledTraitHeader(
                    MachineTraitRoller.refinementPotentialRange(traitRollComponentStage()),
                    tooltipComponents
            );
        }
        if (MachineTraitTooltip.shouldShowMaterialData()) {
            appendMaterialTooltip(stack, tooltipComponents);
        }
        MachineTraitTooltip.appendIdentity(stack, tooltipComponents);
        MachineProgressionState progression = stack.get(ModDataComponents.MACHINE_PROGRESSION.get());
        if (progression != null) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.machine_progression",
                    progression.level(),
                    progression.xp()
            ).withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        MachineTraitTooltip.appendBaseStats(stack, tooltipComponents);
        MachineTraitTooltip.appendTraitDetails(traits, tooltipComponents);
        if (!RecyclingData.isStripped(stack)) {
            MachineTraitTooltip.appendTraitKeyHints(tooltipComponents);
        }
    }
}
