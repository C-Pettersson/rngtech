package com.rngtech.content.item;

import com.rngtech.rpg.MachineType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class CorrosionCellBlockItem extends MachineBlockItem {
    public CorrosionCellBlockItem(Block block, Item.Properties properties) {
        super(block, MachineType.CORROSION_CELL, properties);
    }

    @Override
    protected int componentStage() {
        return 4;
    }

    @Override
    protected void appendMaterialTooltip(ItemStack stack, List<Component> tooltipComponents) {
        tooltipComponents.add(Component.translatable("rngtech.tooltip.corrosion_cell.role").withStyle(ChatFormatting.GRAY));
    }
}
