package com.rngtech.content.item;

import com.rngtech.content.energy.CavitationRotorMaterial;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.rpg.ComponentBaseStatCatalog;
import com.rngtech.rpg.MachineModifierText;
import com.rngtech.rpg.MachinePartType;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class CavitationPartItem extends MachinePartItem {
    private final CavitationRotorMaterial rotor;

    public CavitationPartItem(CavitationRotorMaterial material, Properties properties) {
        super(MachinePartType.CAVITATION_ROTOR, MachineType.CAVITATION_GENERATOR, material.modifierSet(), properties);
        rotor = material;
    }

    public int stage() {
        return rotor.stage();
    }

    public CavitationRotorMaterial rotorMaterial() {
        return rotor;
    }

    public static int rotorWear(ItemStack stack) {
        return Mth.clamp(stack.getOrDefault(ModDataComponents.CAVITATION_ROTOR_WEAR.get(), 0), 0, maxRotorDurability(stack));
    }

    public static int maxRotorDurability(ItemStack stack) {
        if (!(stack.getItem() instanceof CavitationPartItem)) {
            return 0;
        }
        MachineStatAccumulator stats = ComponentBaseStatCatalog.effectiveStats(stack);
        return stats == null ? 0 : Math.max(1, (int) Math.round(stats.value(MachineStat.DURABILITY)));
    }

    public static void setRotorWear(ItemStack stack, int wear) {
        int maxDurability = maxRotorDurability(stack);
        if (maxDurability <= 0) {
            return;
        }
        int clampedWear = Mth.clamp(wear, 0, maxDurability);
        if (clampedWear == 0) {
            stack.remove(ModDataComponents.CAVITATION_ROTOR_WEAR.get());
        } else {
            stack.set(ModDataComponents.CAVITATION_ROTOR_WEAR.get(), clampedWear);
        }
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
        tooltipComponents.add(Component.translatable("rngtech.tooltip.cavitation_part.stage", stage()).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.cavitation_rotor.profile",
                decimal(rotor.generationMultiplier()),
                decimal(rotor.processingSpeedMultiplier()),
                decimal(rotor.wearMultiplier()),
                decimal(rotor.outputMultiplier())
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.cavitation_rotor.durability",
                maxRotorDurability(stack) - rotorWear(stack),
                maxRotorDurability(stack)
        ).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return rotorWear(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int maxDurability = Math.max(1, maxRotorDurability(stack));
        return Math.round(13.0F - 13.0F * rotorWear(stack) / maxDurability);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xD9822B;
    }

    private static String decimal(double value) {
        return MachineModifierText.formatValue(value);
    }
}
