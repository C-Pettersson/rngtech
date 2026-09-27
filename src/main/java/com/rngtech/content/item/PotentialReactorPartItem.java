package com.rngtech.content.item;

import com.rngtech.content.energy.ContainmentLiningMaterial;
import com.rngtech.content.energy.ReactorChamberMaterial;
import com.rngtech.content.energy.RecoveryFilterMaterial;
import com.rngtech.rpg.MachineModifierText;
import com.rngtech.rpg.MachinePartType;
import com.rngtech.rpg.MachineType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class PotentialReactorPartItem extends MachinePartItem {
    private final ReactorChamberMaterial chamber;
    private final RecoveryFilterMaterial filter;
    private final ContainmentLiningMaterial lining;

    public PotentialReactorPartItem(ReactorChamberMaterial material, Properties properties) {
        super(MachinePartType.REACTOR_CHAMBER, MachineType.POTENTIAL_REACTOR, material.modifierSet(), properties);
        chamber = material;
        filter = null;
        lining = null;
    }

    public PotentialReactorPartItem(RecoveryFilterMaterial material, Properties properties) {
        super(MachinePartType.RECOVERY_FILTER, MachineType.POTENTIAL_REACTOR, material.modifierSet(), properties);
        chamber = null;
        filter = material;
        lining = null;
    }

    public PotentialReactorPartItem(ContainmentLiningMaterial material, Properties properties) {
        super(MachinePartType.CONTAINMENT_LINING, MachineType.POTENTIAL_REACTOR, material.modifierSet(), properties);
        chamber = null;
        filter = null;
        lining = material;
    }

    public int stage() {
        if (chamber != null) {
            return chamber.stage();
        }
        if (filter != null) {
            return filter.stage();
        }
        return lining.stage();
    }

    public ReactorChamberMaterial chamberMaterial() {
        return chamber;
    }

    public RecoveryFilterMaterial filterMaterial() {
        return filter;
    }

    public ContainmentLiningMaterial liningMaterial() {
        return lining;
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
        tooltipComponents.add(Component.translatable("rngtech.tooltip.potential_reactor_part.stage", stage()).withStyle(ChatFormatting.GRAY));
        if (chamber != null) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.reactor_chamber.profile",
                    chamber.stage(),
                    decimal(chamber.energyGenerationBonus()),
                    decimal(chamber.stability())
            ).withStyle(ChatFormatting.GRAY));
        } else if (filter != null) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.recovery_filter.profile",
                    decimal(filter.efficiency()),
                    decimal(filter.processingSpeed())
            ).withStyle(ChatFormatting.GRAY));
        } else {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.containment_lining.profile",
                decimal(lining.stability())
            ).withStyle(ChatFormatting.GRAY));
        }
    }

    private static String decimal(double value) {
        return MachineModifierText.formatValue(value);
    }
}
