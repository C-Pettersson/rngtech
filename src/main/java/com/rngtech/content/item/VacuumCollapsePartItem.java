package com.rngtech.content.item;

import com.rngtech.content.energy.VacuumCollapsePartMaterial;
import com.rngtech.rpg.MachineModifierText;
import com.rngtech.rpg.MachinePartType;
import com.rngtech.rpg.MachineType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class VacuumCollapsePartItem extends MachinePartItem {
    private final VacuumCollapsePartMaterial material;

    public VacuumCollapsePartItem(MachinePartType partType, VacuumCollapsePartMaterial material, Properties properties) {
        super(partType, MachineType.VACUUM_COLLAPSE_GENERATOR, material.modifierSet(identityId(partType)), properties);
        this.material = material;
    }

    public VacuumCollapsePartMaterial material() {
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
    public void onCraftedPostProcess(ItemStack stack, Level level) {
        super.onCraftedPostProcess(stack, level);
    }

    @Override
    protected void appendMaterialTooltip(ItemStack stack, List<Component> tooltipComponents) {
        tooltipComponents.add(Component.translatable("rngtech.tooltip.vacuum_collapse_part.stage", stage()).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.vacuum_collapse_part.profile",
                MachineModifierText.formatValue(material.generation()),
                MachineModifierText.formatValue(material.stability())
        ).withStyle(ChatFormatting.GRAY));
    }

    private static String identityId(MachinePartType partType) {
        return switch (partType) {
            case VOID_CHAMBER -> "void_chamber";
            case COLLAPSE_NOZZLE -> "collapse_nozzle";
            case DIMENSIONAL_STABILIZER -> "dimensional_stabilizer";
            default -> "vacuum_collapse_part";
        };
    }
}
