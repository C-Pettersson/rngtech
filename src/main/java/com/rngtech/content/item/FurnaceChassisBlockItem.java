package com.rngtech.content.item;

import com.rngtech.content.machine.FurnaceChassisMaterial;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class FurnaceChassisBlockItem extends MachineBlockItem {
    private final FurnaceChassisMaterial material;

    public FurnaceChassisBlockItem(Block block, FurnaceChassisMaterial material, Item.Properties properties) {
        super(block, material.machineType(), properties);
        this.material = material;
    }

    public FurnaceChassisMaterial material() {
        return material;
    }

    @Override
    protected int componentStage() {
        return material.stage();
    }

    @Override
    protected void appendMaterialTooltip(ItemStack stack, List<Component> tooltipComponents) {
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.furnace_chassis.material",
                Component.translatable(material.translationKey())
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.component_stage",
                material.stage()
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                material.electric()
                        ? "rngtech.tooltip.furnace_chassis.mode.electric"
                        : "rngtech.tooltip.furnace_chassis.mode.fuel"
        ).withStyle(ChatFormatting.GRAY));
        if (material.processingSlots() > 1) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.furnace_chassis.multi_lane",
                    material.processingSlots(),
                    material.heatCoreSlots(),
                    material.processingSpeed()
            ).withStyle(ChatFormatting.GRAY));
        }
    }
}
