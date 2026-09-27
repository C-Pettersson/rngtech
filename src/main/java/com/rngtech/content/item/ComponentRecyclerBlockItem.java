package com.rngtech.content.item;

import com.rngtech.content.recycling.ComponentRecyclerChassis;
import com.rngtech.rpg.MachineType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class ComponentRecyclerBlockItem extends MachineBlockItem {
    private final ComponentRecyclerChassis chassis;

    public ComponentRecyclerBlockItem(Block block, ComponentRecyclerChassis chassis, Item.Properties properties) {
        super(block, MachineType.COMPONENT_RECYCLER, properties);
        this.chassis = chassis;
    }

    public ComponentRecyclerChassis chassis() {
        return chassis;
    }

    @Override
    protected int componentStage() {
        return chassis.stage();
    }

    @Override
    protected void rollTraitsIfMissing(ItemStack stack, Level level) {
        if (!chassis.manual()) {
            super.rollTraitsIfMissing(stack, level);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        if (chassis.manual()) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.component_recycler_chassis.material",
                    Component.translatable(chassis.translationKey())
            ).withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable("rngtech.tooltip.component_stage", chassis.stage()).withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable("rngtech.tooltip.crude_recycler.manual").withStyle(ChatFormatting.GRAY));
            return;
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    @Override
    protected void appendMaterialTooltip(ItemStack stack, List<Component> tooltipComponents) {
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.component_recycler_chassis.material",
                Component.translatable(chassis.translationKey())
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("rngtech.tooltip.component_stage", chassis.stage()).withStyle(ChatFormatting.GRAY));
    }
}
