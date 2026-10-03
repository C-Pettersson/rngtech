package com.rngtech.content.item;

import com.rngtech.rpg.progression.AscendancyCatalog;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** An Ascendancy Seal of one tier. It carries no data, so a Seal from loot, quests, or /give works like a crafted one. */
public class AscendancySealItem extends Item {
    private final int tier;

    public AscendancySealItem(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("rngtech.ascendancy_seal.tooltip.points", AscendancyCatalog.POINTS_PER_TIER)
                .withStyle(ChatFormatting.GRAY));
        if (tier == 1) {
            tooltipComponents.add(Component.translatable("rngtech.ascendancy_seal.tooltip.first", AscendancyCatalog.ENTRY_STAGE)
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltipComponents.add(Component.translatable("rngtech.ascendancy_seal.tooltip.later", tier - 1).withStyle(ChatFormatting.GRAY));
        }
        tooltipComponents.add(Component.translatable("rngtech.ascendancy_seal.tooltip.use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
