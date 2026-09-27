package com.rngtech.content.item;

import com.rngtech.rpg.MachineType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class SolarArrayControllerBlockItem extends MachineBlockItem {
    public static final int STAGE = 4;

    public SolarArrayControllerBlockItem(Block block, Item.Properties properties) {
        super(block, MachineType.SOLAR_ARRAY_CONTROLLER, properties);
    }

    @Override
    protected int componentStage() {
        return STAGE;
    }

    @Override
    protected void appendMaterialTooltip(ItemStack stack, List<Component> tooltipComponents) {
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.solar_array_controller.stage",
                STAGE
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("rngtech.tooltip.solar_array_controller.scan").withStyle(ChatFormatting.GRAY));
    }
}
