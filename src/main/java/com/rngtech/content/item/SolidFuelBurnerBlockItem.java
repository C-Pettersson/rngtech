package com.rngtech.content.item;

import com.rngtech.content.energy.SolidFuelBurnerChassis;
import com.rngtech.rpg.MachineType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class SolidFuelBurnerBlockItem extends MachineBlockItem {
    private final SolidFuelBurnerChassis chassis;

    public SolidFuelBurnerBlockItem(Block block, SolidFuelBurnerChassis chassis, Item.Properties properties) {
        super(block, MachineType.SOLID_FUEL_BURNER, properties);
        this.chassis = chassis;
    }

    public SolidFuelBurnerChassis chassis() {
        return chassis;
    }

    @Override
    protected int componentStage() {
        return chassis.stage();
    }

    @Override
    protected void appendMaterialTooltip(ItemStack stack, List<Component> tooltipComponents) {
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.solid_fuel_burner.stage",
                chassis.stage(),
                chassis.maxPartStage()
        ).withStyle(ChatFormatting.GRAY));
    }
}
