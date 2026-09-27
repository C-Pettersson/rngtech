package com.rngtech.content.item;

import com.rngtech.content.machine.AlloyCrucibleMaterial;
import com.rngtech.rpg.MachinePartType;
import com.rngtech.rpg.MachineType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class AlloyCrucibleItem extends MachinePartItem {
    private final AlloyCrucibleMaterial material;

    public AlloyCrucibleItem(AlloyCrucibleMaterial material, Properties properties) {
        super(MachinePartType.ALLOY_CRUCIBLE, MachineType.ALLOY_FURNACE, material.modifierSet(), properties);
        this.material = material;
    }

    public AlloyCrucibleMaterial material() {
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
        tooltipComponents.add(Component.translatable("rngtech.tooltip.component_stage", material.stage()).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.alloy_crucible.profile",
                material.inputSlots()
        ).withStyle(ChatFormatting.GRAY));
    }
}
