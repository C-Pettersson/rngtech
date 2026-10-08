package com.rngtech.content.item;

import com.rngtech.content.energy.CathodeMaterial;
import com.rngtech.rpg.MachinePartType;
import com.rngtech.rpg.MachineType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class CathodeItem extends MachinePartItem {
    private final CathodeMaterial material;

    public CathodeItem(CathodeMaterial material, Properties properties) {
        super(MachinePartType.CATHODE, MachineType.CORROSION_CELL, material.modifierSet(), properties);
        this.material = material;
    }

    public CathodeMaterial material() {
        return material;
    }

    public int stage() {
        return material.stage();
    }

    @Override
    protected int componentStage() {
        return material.stage();
    }

    @Override
    protected void appendMaterialTooltip(ItemStack stack, List<Component> tooltipComponents) {
        tooltipComponents.add(Component.translatable("rngtech.tooltip.cathode.stage", stage()).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("rngtech.tooltip.cathode.profile", stage()).withStyle(ChatFormatting.GRAY));
    }
}
