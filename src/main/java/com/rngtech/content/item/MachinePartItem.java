package com.rngtech.content.item;

import com.rngtech.content.recycling.RecyclingData;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.rpg.MachineImplicitCatalog;
import com.rngtech.rpg.MachineNameGenerator;
import com.rngtech.rpg.MachinePartType;
import com.rngtech.rpg.MachineTraitRoller;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.MachineType;
import com.rngtech.rpg.ModifierSet;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class MachinePartItem extends Item {
    private final MachinePartType partType;
    private final MachineType machineType;
    private final ModifierSet modifierSet;

    public MachinePartItem(MachinePartType partType, MachineType machineType, ModifierSet modifierSet, Properties properties) {
        super(properties);
        this.partType = partType;
        this.machineType = machineType;
        this.modifierSet = modifierSet;
    }

    public MachinePartType partType() {
        return partType;
    }

    public ModifierSet modifierSet() {
        return modifierSet;
    }

    public ModifierSet modifierSet(ItemStack stack) {
        return traits(stack).modifierSet();
    }

    public MachineType machineType() {
        return machineType;
    }

    public MachineTraits traits(ItemStack stack) {
        if (RecyclingData.isStripped(stack)) {
            return MachineTraits.EMPTY;
        }
        MachineTraits traits = stack.get(ModDataComponents.MACHINE_TRAITS.get());
        if (traits == null) {
            traits = new MachineTraits(modifierSet.rarity(), modifierSet.refinementPotential(), List.of());
        }
        return MachineImplicitCatalog.effectiveTraits(traits, stack);
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
        MachineTraits traits = traits(stack);
        if (stack.has(ModDataComponents.MACHINE_TRAITS.get()) || RecyclingData.isStripped(stack)) {
            MachineTraitTooltip.appendTraitHeader(traits, tooltipComponents, true);
        } else {
            MachineTraitTooltip.appendUnrolledTraitHeader(
                    MachineTraitRoller.refinementPotentialRange(componentStage()).offset(modifierSet.refinementPotential()),
                    tooltipComponents
            );
        }
        if (MachineTraitTooltip.shouldShowMaterialData()) {
            appendMaterialTooltip(stack, tooltipComponents);
        }
        MachineTraitTooltip.appendIdentity(stack, tooltipComponents);
        MachineTraitTooltip.appendComponentBaseStats(stack, tooltipComponents);
        MachineTraitTooltip.appendTraitDetails(traits, tooltipComponents, stack);
        if (!RecyclingData.isStripped(stack)) {
            MachineTraitTooltip.appendTraitKeyHints(tooltipComponents);
        }
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

    public final int traitRollComponentStage() {
        return componentStage();
    }

    protected void appendMaterialTooltip(ItemStack stack, List<Component> tooltipComponents) {
    }
}
