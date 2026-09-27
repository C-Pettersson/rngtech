package com.rngtech.content.item;

import com.rngtech.rpg.MachineType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class PotentialReactorBlockItem extends MachineBlockItem {
    public PotentialReactorBlockItem(Block block, Item.Properties properties) {
        super(block, MachineType.POTENTIAL_REACTOR, properties);
    }

    @Override
    protected int componentStage() {
        return 3;
    }

    @Override
    protected void appendMaterialTooltip(ItemStack stack, List<Component> tooltipComponents) {
        tooltipComponents.add(Component.translatable("rngtech.tooltip.potential_reactor.role").withStyle(ChatFormatting.GRAY));
    }
}
