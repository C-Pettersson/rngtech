package com.rngtech.content.item;

import com.rngtech.content.machine.CrushHeadMaterial;
import com.rngtech.rpg.MachinePartType;
import com.rngtech.rpg.MachineType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class CrushHeadItem extends MachinePartItem {
    private final CrushHeadMaterial material;

    public CrushHeadItem(CrushHeadMaterial material, Item.Properties properties) {
        super(MachinePartType.CRUSH_HEAD, MachineType.CRUSHER, material.modifierSet(), properties);
        this.material = material;
    }

    public CrushHeadMaterial material() {
        return material;
    }

    @Override
    protected int componentStage() {
        return material.stage();
    }

    @Override
    protected void appendMaterialTooltip(ItemStack stack, List<Component> tooltipComponents) {
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.crush_head.stage",
                material.stage(),
                material.processingLevel()
        ).withStyle(ChatFormatting.GRAY));
    }
}
