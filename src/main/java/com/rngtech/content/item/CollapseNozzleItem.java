package com.rngtech.content.item;

import com.rngtech.content.energy.CollapseNozzleMaterial;
import com.rngtech.rpg.MachineModifierText;
import com.rngtech.rpg.MachinePartType;
import com.rngtech.rpg.MachineType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class CollapseNozzleItem extends MachinePartItem {
    private final CollapseNozzleMaterial material;

    public CollapseNozzleItem(CollapseNozzleMaterial material, Properties properties) {
        super(MachinePartType.COLLAPSE_NOZZLE, MachineType.CAVITATION_GENERATOR, material.modifierSet(), properties);
        this.material = material;
    }

    public CollapseNozzleMaterial material() {
        return material;
    }

    public int stage() {
        return material.stage();
    }

    @Override
    protected int componentStage() {
        return stage();
    }

    @Override
    protected void appendMaterialTooltip(ItemStack stack, List<Component> tooltipComponents) {
        tooltipComponents.add(Component.translatable("rngtech.tooltip.cavitation_part.stage", stage()).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.collapse_nozzle.profile",
                decimal(material.generationMultiplier()),
                decimal(material.strainMultiplier()),
                decimal(material.outputMultiplier()),
                decimal(material.fluidTransferMultiplier())
        ).withStyle(ChatFormatting.GRAY));
        if (material.vacuumCollapseCompatible()) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.collapse_nozzle.vacuum_profile",
                    decimal(material.vacuumGenerationMultiplier()),
                    decimal(material.vacuumProcessingSpeedMultiplier()),
                    decimal(material.vacuumStabilityMultiplier())
            ).withStyle(ChatFormatting.GRAY));
        }
    }

    private static String decimal(double value) {
        return MachineModifierText.formatValue(value);
    }
}
