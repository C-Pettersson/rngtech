package com.rngtech.content.item;

import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.rpg.MachineType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class CrusherChassisBlockItem extends MachineBlockItem {
    private final CrusherChassisMaterial material;

    public CrusherChassisBlockItem(Block block, CrusherChassisMaterial material, Item.Properties properties) {
        super(block, MachineType.CRUSHER, properties);
        this.material = material;
    }

    public CrusherChassisMaterial material() {
        return material;
    }

    @Override
    protected int componentStage() {
        return material.stage();
    }

    @Override
    protected void appendMaterialTooltip(ItemStack stack, List<Component> tooltipComponents) {
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.crusher_chassis.material",
                Component.translatable(material.translationKey())
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.component_stage",
                material.stage()
        ).withStyle(ChatFormatting.GRAY));
    }
}
