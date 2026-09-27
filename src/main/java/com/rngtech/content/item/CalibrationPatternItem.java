package com.rngtech.content.item;

import com.rngtech.content.calibration.CalibrationFamily;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class CalibrationPatternItem extends Item {
    private final CalibrationFamily family;

    public CalibrationPatternItem(CalibrationFamily family, Properties properties) {
        super(properties);
        this.family = family;
    }

    public CalibrationFamily family() {
        return family;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.calibration_pattern.family",
                Component.translatable(family.translationKey())
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("rngtech.tooltip.calibration_pattern.reusable").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
