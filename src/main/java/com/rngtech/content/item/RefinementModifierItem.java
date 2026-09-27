package com.rngtech.content.item;

import com.rngtech.rpg.refinement.RefinementModifier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class RefinementModifierItem extends Item {
    private final RefinementModifier modifier;

    public RefinementModifierItem(RefinementModifier modifier, Properties properties) {
        super(properties);
        this.modifier = modifier;
    }

    public RefinementModifier modifier() {
        return modifier;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("rngtech.tooltip.refinement_modifier").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(modifier.tooltipKey()).withStyle(ChatFormatting.DARK_AQUA));
    }
}
