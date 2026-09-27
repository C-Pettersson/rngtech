package com.rngtech.content.item;

import com.rngtech.content.machine.AlloyFurnaceChassisMaterial;
import com.rngtech.rpg.MachineType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class AlloyFurnaceChassisBlockItem extends MachineBlockItem {
    private final AlloyFurnaceChassisMaterial material;

    public AlloyFurnaceChassisBlockItem(Block block, AlloyFurnaceChassisMaterial material, Item.Properties properties) {
        super(block, MachineType.ALLOY_FURNACE, properties);
        this.material = material;
    }

    public AlloyFurnaceChassisMaterial material() {
        return material;
    }

    @Override
    protected int componentStage() {
        return material.stage();
    }

    @Override
    protected void appendMaterialTooltip(ItemStack stack, List<Component> tooltipComponents) {
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.alloy_furnace_chassis.material",
                Component.translatable(material.translationKey())
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("rngtech.tooltip.component_stage", material.stage()).withStyle(ChatFormatting.GRAY));
    }
}
