package com.rngtech.content.item;

import com.rngtech.content.cable.FluidConnectorTier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

public class FluidConnectorItem extends Item {
    private final FluidConnectorTier tier;

    public FluidConnectorItem(FluidConnectorTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public FluidConnectorTier tier() {
        return tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.fluid_connector.fluid",
                tier.fluidPerShipment()
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.fluid_connector.wait",
                tier.waitTicks()
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.fluid_connector.jam",
                String.format(Locale.ROOT, "%.1f", tier.jamChancePerThousand() / 10.0D),
                tier.jamTicks() / 20
        ).withStyle(ChatFormatting.GRAY));
        if (tier.stage() > 0) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.fluid_connector.stage",
                    tier.stage()
            ).withStyle(ChatFormatting.GRAY));
        }
        if (tier.debugOnly()) {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.fluid_connector.debug").withStyle(ChatFormatting.RED));
        }
        tooltipComponents.add(Component.translatable("rngtech.tooltip.fluid_connector.install").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
