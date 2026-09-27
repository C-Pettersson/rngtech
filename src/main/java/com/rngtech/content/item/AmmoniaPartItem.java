package com.rngtech.content.item;

import com.rngtech.rpg.MachinePartType;
import com.rngtech.rpg.MachineType;
import com.rngtech.rpg.ModifierSet;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class AmmoniaPartItem extends MachinePartItem {
    private final int stage;

    public AmmoniaPartItem(MachinePartType partType, MachineType machineType, int stage, Properties properties) {
        super(partType, machineType, ModifierSet.empty(), properties);
        this.stage = stage;
    }

    public int stage() {
        return stage;
    }

    @Override
    protected int componentStage() {
        return stage;
    }

    @Override
    protected void appendMaterialTooltip(ItemStack stack, List<Component> tooltipComponents) {
        tooltipComponents.add(Component.translatable("rngtech.tooltip.ammonia_part.stage", stage).withStyle(ChatFormatting.GRAY));
    }
}
