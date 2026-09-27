package com.rngtech.content.item;

import com.rngtech.content.tool.PruningShearsMaterial;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

import java.util.List;

public class PruningShearsItem extends Item {
    private final PruningShearsMaterial material;

    public PruningShearsItem(PruningShearsMaterial material, Properties properties) {
        super(properties);
        this.material = material;
    }

    public PruningShearsMaterial material() {
        return material;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        return itemAbility == ItemAbilities.SHEARS_DIG;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.pruning_shears.stage",
                material.stage()
        ).withStyle(ChatFormatting.GRAY));
        if (stack.isDamageableItem()) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.pruning_shears.durability",
                    Math.max(0, stack.getMaxDamage() - stack.getDamageValue()),
                    stack.getMaxDamage()
            ).withStyle(ChatFormatting.GRAY));
        }
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.pruning_shears.cart_leaf_budget",
                material.cartLeafWearBudget()
        ).withStyle(ChatFormatting.DARK_GRAY));
    }
}
