package com.rngtech.content.item;

import com.rngtech.content.machine.ServoMaterial;
import com.rngtech.rpg.MachinePartType;
import com.rngtech.rpg.MachineType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class ServoItem extends MachinePartItem {
    private final ServoMaterial material;

    public ServoItem(ServoMaterial material, Properties properties) {
        super(MachinePartType.SERVO, MachineType.METAL_PRESS, material.modifierSet(), properties);
        this.material = material;
    }

    public ServoMaterial material() {
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
    public void onCraftedPostProcess(ItemStack stack, Level level) {
        super.onCraftedPostProcess(stack, level);
    }

    @Override
    protected void appendMaterialTooltip(ItemStack stack, List<Component> tooltipComponents) {
        tooltipComponents.add(Component.translatable("rngtech.tooltip.servo.stage", stage()).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("rngtech.tooltip.servo.profile").withStyle(ChatFormatting.GRAY));
    }
}
