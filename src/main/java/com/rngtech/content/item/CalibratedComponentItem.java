package com.rngtech.content.item;

import com.rngtech.content.calibration.CalibrationFamily;
import com.rngtech.content.calibration.CalibrationState;
import com.rngtech.content.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class CalibratedComponentItem extends Item {
    private final CalibrationFamily family;

    public CalibratedComponentItem(CalibrationFamily family, Properties properties) {
        super(properties);
        this.family = family;
    }

    public CalibrationFamily family() {
        return family;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        CalibrationState state = stack.get(ModDataComponents.CALIBRATION_STATE.get());
        if (state == null) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.calibration.family",
                    Component.translatable(family.translationKey())
            ).withStyle(ChatFormatting.GRAY));
        } else {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.calibration.family",
                    Component.translatable(state.family().translationKey())
            ).withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable("rngtech.tooltip.calibration.stage", state.stage()).withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable("rngtech.tooltip.calibration.stability", state.stability()).withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.calibration.refinement_potential",
                    state.refinementPotential()
            ).withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
