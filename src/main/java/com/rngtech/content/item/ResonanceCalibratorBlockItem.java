package com.rngtech.content.item;

import com.rngtech.content.calibration.ResonanceCalibratorChassis;
import com.rngtech.rpg.MachineType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class ResonanceCalibratorBlockItem extends MachineBlockItem {
    private final ResonanceCalibratorChassis chassis;

    public ResonanceCalibratorBlockItem(Block block, ResonanceCalibratorChassis chassis, Item.Properties properties) {
        super(block, MachineType.RESONANCE_CALIBRATOR, properties);
        this.chassis = chassis;
    }

    public ResonanceCalibratorChassis chassis() {
        return chassis;
    }

    @Override
    protected int componentStage() {
        return chassis.stage();
    }

    @Override
    protected void appendMaterialTooltip(ItemStack stack, List<Component> tooltipComponents) {
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.resonance_calibrator_chassis.material",
                Component.translatable(chassis.translationKey())
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("rngtech.tooltip.component_stage", chassis.stage()).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.resonance_calibrator_chassis.lanes",
                chassis.lanes()
        ).withStyle(ChatFormatting.GRAY));
    }
}
