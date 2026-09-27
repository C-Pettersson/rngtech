package com.rngtech.content.item;

import com.rngtech.content.energy.BatteryChassisMaterial;
import com.rngtech.rpg.MachineType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class BatteryChassisBlockItem extends MachineBlockItem {
    private final BatteryChassisMaterial material;

    public BatteryChassisBlockItem(Block block, BatteryChassisMaterial material, Item.Properties properties) {
        super(block, MachineType.BATTERY_CHASSIS, properties);
        this.material = material;
    }

    public BatteryChassisMaterial material() {
        return material;
    }

    @Override
    protected int componentStage() {
        return material.stage();
    }

    @Override
    protected void appendMaterialTooltip(ItemStack stack, List<Component> tooltipComponents) {
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.battery_chassis.material",
                Component.translatable(material.translationKey())
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.battery_chassis.slots",
                material.slots()
        ).withStyle(ChatFormatting.GRAY));
    }
}
