package com.rngtech.content.item;

import com.rngtech.content.recycling.DisassemblyHeadItemAccess;
import com.rngtech.content.recycling.DisassemblyHeadMaterial;
import com.rngtech.rpg.MachineModifierText;
import com.rngtech.rpg.MachinePartType;
import com.rngtech.rpg.MachineType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class DisassemblyHeadItem extends MachinePartItem implements DisassemblyHeadItemAccess {
    private final DisassemblyHeadMaterial material;

    public DisassemblyHeadItem(DisassemblyHeadMaterial material, Properties properties) {
        super(MachinePartType.DISASSEMBLY_HEAD, MachineType.COMPONENT_RECYCLER, material.modifierSet(), properties);
        this.material = material;
    }

    public DisassemblyHeadMaterial material() {
        return material;
    }

    public int stage() {
        return material.stage();
    }

    @Override
    public int recyclingStage() {
        return stage();
    }

    @Override
    protected int componentStage() {
        return material.stage();
    }

    @Override
    protected void appendMaterialTooltip(ItemStack stack, List<Component> tooltipComponents) {
        tooltipComponents.add(Component.translatable("rngtech.tooltip.disassembly_head.stage", material.stage()).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.disassembly_head.profile",
                MachineModifierText.formatValue(material.processingSpeed()),
                MachineModifierText.formatValue(material.stability())
        ).withStyle(ChatFormatting.GRAY));
    }
}
