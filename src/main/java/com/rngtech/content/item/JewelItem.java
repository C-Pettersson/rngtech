package com.rngtech.content.item;

import com.rngtech.content.calibration.CalibrationState;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.rpg.MachineNameGenerator;
import com.rngtech.rpg.MachineTraitRoller;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.jewel.JewelAffixes;
import com.rngtech.rpg.jewel.JewelColour;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A rolled jewel: up to two prefixes and two suffixes from the machine-agnostic jewel pool, one tier each. A jewel is
 * cut at the Resonance Calibrator, whose stability sets its Refinement Potential, and identified like any part.
 */
public class JewelItem extends Item {
    private final JewelColour colour;

    public JewelItem(JewelColour colour, Properties properties) {
        super(properties);
        this.colour = colour;
    }

    public JewelColour colour() {
        return colour;
    }

    public MachineTraits traits(ItemStack stack) {
        MachineTraits traits = stack.get(ModDataComponents.MACHINE_TRAITS.get());
        return traits == null ? MachineTraits.EMPTY : traits;
    }

    /** The stability the Calibrator cut this jewel at, or null for a jewel that was never cut. */
    public static Integer cutStability(ItemStack stack) {
        CalibrationState state = stack.get(ModDataComponents.CALIBRATION_STATE.get());
        return state == null ? null : state.stability();
    }

    /** The Refinement Potential this jewel will identify with: fixed by its cut, or the whole cut range when uncut. */
    public static MachineTraitRoller.RefinementPotentialRange refinementPotentialRange(ItemStack stack) {
        Integer stability = cutStability(stack);
        if (stability == null) {
            return new MachineTraitRoller.RefinementPotentialRange(JewelAffixes.MIN_REFINEMENT_POTENTIAL, JewelAffixes.MAX_REFINEMENT_POTENTIAL);
        }
        int potential = JewelAffixes.refinementPotential(stability);
        return new MachineTraitRoller.RefinementPotentialRange(potential, potential);
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
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        MachineTraitTooltip.appendUnidentified(stack, tooltipComponents);
        MachineTraits traits = traits(stack);
        if (stack.has(ModDataComponents.MACHINE_TRAITS.get())) {
            MachineTraitTooltip.appendTraitHeader(stack, traits, tooltipComponents, true);
        } else {
            MachineTraitTooltip.appendUnrolledTraitHeader(refinementPotentialRange(stack), tooltipComponents);
        }
        tooltipComponents.add(Component.translatable(colour.descriptionKey()).withStyle(ChatFormatting.GRAY));
        Integer stability = cutStability(stack);
        if (stability != null) {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.jewel.cut", stability).withStyle(ChatFormatting.GRAY));
        }
        MachineTraitTooltip.appendTraitDetails(traits, tooltipComponents, ItemStack.EMPTY, stack);
        MachineTraitTooltip.appendTraitKeyHints(tooltipComponents);
    }
}
