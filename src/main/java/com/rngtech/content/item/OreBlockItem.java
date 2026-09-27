package com.rngtech.content.item;

import com.rngtech.content.material.MaterialEnablement;
import com.rngtech.content.material.OreDefinition;
import com.rngtech.content.material.OreHost;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class OreBlockItem extends BlockItem {
    private final OreDefinition definition;
    private final OreHost host;

    public OreBlockItem(Block block, OreDefinition definition, OreHost host, Properties properties) {
        super(block, properties);
        this.definition = definition;
        this.host = host;
    }

    public OreDefinition definition() {
        return definition;
    }

    public OreHost host() {
        return host;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.ore.material",
                definition.materialDisplayName()
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.ore.hardness",
                definition.hardnessLevel()
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.ore.drops",
                definition.materialDisplayName()
        ).withStyle(ChatFormatting.GRAY));
        if (!MaterialEnablement.isEnabled(definition.materialId())) {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.material.disabled").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
