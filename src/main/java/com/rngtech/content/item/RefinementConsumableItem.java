package com.rngtech.content.item;

import com.rngtech.content.blockentity.AffixForgeBlockEntity;
import com.rngtech.rpg.refinement.RefinementOperation;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class RefinementConsumableItem extends Item {
    private final RefinementOperation operation;

    public RefinementConsumableItem(RefinementOperation operation, Properties properties) {
        super(properties);
        this.operation = operation;
    }

    public RefinementOperation operation() {
        return operation;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("rngtech.tooltip.refinement_consumable").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(operation.tooltipKey()).withStyle(ChatFormatting.DARK_AQUA));
        if (AffixForgeBlockEntity.lockedMessage(operation, true, false) != null) {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.refinement_consumable.requires_resonance_matrix")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
