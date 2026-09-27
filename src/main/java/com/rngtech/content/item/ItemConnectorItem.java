package com.rngtech.content.item;

import com.rngtech.content.cable.ItemConnectorTier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

public class ItemConnectorItem extends Item {
    private final ItemConnectorTier tier;

    public ItemConnectorItem(ItemConnectorTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public ItemConnectorTier tier() {
        return tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.item_connector.items",
                tier.itemsPerShipment()
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.item_connector.wait",
                tier.waitTicks()
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.item_connector.jam",
                String.format(Locale.ROOT, "%.1f", tier.jamChancePerThousand() / 10.0D),
                tier.jamTicks() / 20
        ).withStyle(ChatFormatting.GRAY));
        if (tier.stage() > 0) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.item_connector.stage",
                    tier.stage()
            ).withStyle(ChatFormatting.GRAY));
        }
        if (tier.debugOnly()) {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.item_connector.debug").withStyle(ChatFormatting.RED));
        }
        tooltipComponents.add(Component.translatable("rngtech.tooltip.item_connector.install").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
