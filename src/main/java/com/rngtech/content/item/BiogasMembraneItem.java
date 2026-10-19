package com.rngtech.content.item;

import com.rngtech.content.calibration.CalibrationState;
import com.rngtech.content.energy.BiogasDigestion;
import com.rngtech.content.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

/**
 * A membrane module that separates a Bio Generator's biogas into Methane and CO2. Its Methane recovery is fixed by the
 * stability of the calibrated component the Component Assembler built it from.
 */
public class BiogasMembraneItem extends Item {
    public BiogasMembraneItem(Properties properties) {
        super(properties);
    }

    /** Share of the biogas Methane this membrane recovers; one with no recorded calibration counts as stability 0. */
    public static double recovery(ItemStack stack) {
        CalibrationState calibration = stack.get(ModDataComponents.SOURCE_CALIBRATION.get());
        return BiogasDigestion.membraneRecovery(calibration == null ? 0 : calibration.stability());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.biogas_membrane.recovery",
                String.format(Locale.ROOT, "%.1f", recovery(stack) * 100.0)
        ).withStyle(ChatFormatting.GREEN));
        CalibrationState calibration = stack.get(ModDataComponents.SOURCE_CALIBRATION.get());
        if (calibration != null) {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.biogas_membrane.stability", calibration.stability())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltipComponents.add(Component.translatable("rngtech.tooltip.biogas_membrane.usage").withStyle(ChatFormatting.GRAY));
    }
}
