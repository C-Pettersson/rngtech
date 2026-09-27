package com.rngtech.content.item;

import com.rngtech.content.material.MaterialEnablement;
import com.rngtech.content.material.MaterialItemDefinition;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class MaterialItem extends Item {
    private final MaterialItemDefinition definition;

    public MaterialItem(MaterialItemDefinition definition, Properties properties) {
        super(properties);
        this.definition = definition;
    }

    public MaterialItemDefinition definition() {
        return definition;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        if (!MaterialEnablement.isEnabled(definition)) {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.material.disabled").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
