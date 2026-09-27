package com.rngtech.content.item;

import com.rngtech.content.cable.EnergyConnectorTier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class EnergyConnectorItem extends Item {
    private final EnergyConnectorTier tier;

    public EnergyConnectorItem(EnergyConnectorTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public EnergyConnectorTier tier() {
        return tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.energy_connector.transfer",
                tier.transferRate()
        ).withStyle(ChatFormatting.GRAY));
        if (tier.stage() > 0) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.energy_connector.stage",
                    tier.stage()
            ).withStyle(ChatFormatting.GRAY));
        }
        if (tier.debugOnly()) {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.energy_connector.debug").withStyle(ChatFormatting.RED));
        }
        tooltipComponents.add(Component.translatable("rngtech.tooltip.energy_connector.install").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
