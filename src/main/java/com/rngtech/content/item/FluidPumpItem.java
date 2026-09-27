package com.rngtech.content.item;

import com.rngtech.content.machine.FluidPumpMaterial;
import com.rngtech.rpg.MachinePartType;
import com.rngtech.rpg.MachineType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class FluidPumpItem extends MachinePartItem {
    private final FluidPumpMaterial material;

    public FluidPumpItem(FluidPumpMaterial material, Properties properties) {
        super(MachinePartType.FLUID_PUMP, MachineType.MELTER, material.modifierSet(), properties);
        this.material = material;
    }

    public FluidPumpMaterial material() {
        return material;
    }

    public int stage() {
        return material.stage();
    }

    public int transferRate() {
        return material.transferRate();
    }

    @Override
    protected int componentStage() {
        return material.stage();
    }

    @Override
    public void onCraftedPostProcess(ItemStack stack, Level level) {
        super.onCraftedPostProcess(stack, level);
    }

    @Override
    protected void appendMaterialTooltip(ItemStack stack, List<Component> tooltipComponents) {
        tooltipComponents.add(Component.translatable("rngtech.tooltip.fluid_pump.stage", stage()).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("rngtech.tooltip.fluid_pump.profile", transferRate()).withStyle(ChatFormatting.GRAY));
    }
}
