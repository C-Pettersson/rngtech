package com.rngtech.content.item;

import com.rngtech.content.energy.SolarPanelMaterial;
import com.rngtech.rpg.MachineType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class SolarPanelBlockItem extends MachineBlockItem {
    private final SolarPanelMaterial material;

    public SolarPanelBlockItem(Block block, SolarPanelMaterial material, Item.Properties properties) {
        super(block, MachineType.SOLAR_PANEL, properties);
        this.material = material;
    }

    public SolarPanelMaterial material() {
        return material;
    }

    @Override
    protected int componentStage() {
        return material.stage();
    }

    @Override
    protected void appendMaterialTooltip(ItemStack stack, List<Component> tooltipComponents) {
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.solar_panel.stage",
                material.stage()
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.solar_panel.output",
                material.clearGeneration(),
                material.rainGeneration()
        ).withStyle(ChatFormatting.GRAY));
    }
}
