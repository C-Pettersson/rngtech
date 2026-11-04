package com.rngtech.content.item;

import com.rngtech.content.affixsorter.RuleCoreMaterial;
import com.rngtech.rpg.MachineModifierText;
import com.rngtech.rpg.MachinePartType;
import com.rngtech.rpg.MachineType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class RuleCoreItem extends MachinePartItem {
    private final RuleCoreMaterial material;

    public RuleCoreItem(RuleCoreMaterial material, Properties properties) {
        super(MachinePartType.RULE_CORE, MachineType.AFFIX_SORTER, material.modifierSet(), properties);
        this.material = material;
    }

    public RuleCoreMaterial material() {
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
        tooltipComponents.add(Component.translatable("rngtech.tooltip.rule_core.stage", material.stage()).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.rule_core.profile",
                material.ruleCapacity(),
                MachineModifierText.formatValue(material.processingSpeed())
        ).withStyle(ChatFormatting.GRAY));
    }
}
