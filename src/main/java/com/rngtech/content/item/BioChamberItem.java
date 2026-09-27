package com.rngtech.content.item;

import com.rngtech.rpg.MachinePartType;
import com.rngtech.rpg.MachineType;
import com.rngtech.rpg.ModifierSet;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class BioChamberItem extends MachinePartItem {
    public BioChamberItem(Properties properties) {
        super(MachinePartType.BIO_CHAMBER, MachineType.BIO_GENERATOR, ModifierSet.empty(), properties);
    }

    @Override
    protected int componentStage() {
        return 1;
    }

    @Override
    protected void appendMaterialTooltip(ItemStack stack, List<Component> tooltipComponents) {
        tooltipComponents.add(Component.translatable("rngtech.tooltip.bio_chamber.profile").withStyle(ChatFormatting.GRAY));
    }
}
