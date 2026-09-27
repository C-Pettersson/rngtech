package com.rngtech.content.item;

import com.rngtech.content.calibration.CalibrationGearMaterial;
import com.rngtech.rpg.MachinePartType;
import com.rngtech.rpg.MachineType;
import com.rngtech.rpg.ModifierSet;
import com.rngtech.rpg.Rarity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class CalibrationGearItem extends MachinePartItem {
    private final CalibrationGearMaterial material;

    public CalibrationGearItem(MachinePartType partType, CalibrationGearMaterial material, Properties properties) {
        super(
                partType,
                MachineType.RESONANCE_CALIBRATOR,
                new ModifierSet(Rarity.NORMAL, material.refinementPotential(), List.of()),
                properties
        );
        this.material = material;
    }

    public CalibrationGearMaterial material() {
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
        tooltipComponents.add(Component.translatable("rngtech.tooltip.calibration_gear.stage", stage()).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("rngtech.tooltip.calibration_gear." + partType().name().toLowerCase()).withStyle(ChatFormatting.GRAY));
    }
}
