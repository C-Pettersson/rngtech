package com.rngtech.content.item;

import com.rngtech.content.energy.FuelBoxMaterial;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.rpg.MachineBehavior;
import com.rngtech.rpg.MachineImplicitCatalog;
import com.rngtech.rpg.MachinePartType;
import com.rngtech.rpg.MachineType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class SolidFuelBurnerPartItem extends MachinePartItem {
    private final HeatCoreMaterial heatCore;
    private final FuelBoxMaterial fuelBox;

    public SolidFuelBurnerPartItem(HeatCoreMaterial material, Properties properties) {
        super(MachinePartType.HEAT_CORE, MachineType.SOLID_FUEL_BURNER, material.modifierSet(), properties);
        heatCore = material;
        fuelBox = null;
    }

    public SolidFuelBurnerPartItem(FuelBoxMaterial material, Properties properties) {
        super(MachinePartType.FUEL_BOX, MachineType.SOLID_FUEL_BURNER, material.modifierSet(), properties);
        heatCore = null;
        fuelBox = material;
    }

    public int stage() {
        return heatCore != null ? heatCore.stage() : fuelBox.stage();
    }

    public HeatCoreMaterial heatCoreMaterial() {
        return heatCore;
    }

    public FuelBoxMaterial fuelBoxMaterial() {
        return fuelBox;
    }

    public int maxFuelTier() {
        return heatCore == null ? 0 : heatCore.maxFuelTier();
    }

    public int baseFuelSlots() {
        return fuelBox == null ? 0 : fuelBox.fuelSlots();
    }

    public boolean acceptsItemFuels() {
        return fuelBox != null && fuelBox.acceptsItemFuels();
    }

    public boolean acceptsBlockFuels() {
        return fuelBox != null && MachineImplicitCatalog.fuelBox(fuelBox).traits().hasBehavior(MachineBehavior.BLOCK_FEED);
    }

    public boolean fuelGovernor() {
        return fuelBox != null && MachineImplicitCatalog.fuelBox(fuelBox).traits().hasBehavior(MachineBehavior.FUEL_GOVERNOR);
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
        tooltipComponents.add(Component.translatable("rngtech.tooltip.solid_fuel_part.stage", stage()).withStyle(ChatFormatting.GRAY));
        if (heatCore != null) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.heat_core.profile",
                    heatCore.energyGeneration(),
                    decimal(heatCore.fuelEfficiency()),
                    heatCore.maxFuelTier()
            ).withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.heat_core.heat_profile",
                    heatCore.maxTemperatureBonus(),
                    decimal(heatCore.heatTransfer()),
                    decimal(heatCore.temperatureStability())
            ).withStyle(ChatFormatting.GRAY));
        } else {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.fuel_box.profile",
                    baseFuelSlots(),
                    acceptsItemFuels() ? Component.translatable("rngtech.fuel_form.items") : Component.translatable("rngtech.fuel_form.none"),
                    acceptsBlockFuels() ? Component.translatable("rngtech.fuel_form.blocks") : Component.translatable("rngtech.fuel_form.none")
            ).withStyle(ChatFormatting.GRAY));
            if (fuelGovernor()) {
                tooltipComponents.add(Component.translatable("rngtech.tooltip.fuel_box.governor").withStyle(ChatFormatting.GRAY));
            }
        }
    }

    private static String decimal(double value) {
        return com.rngtech.rpg.MachineModifierText.formatValue(value);
    }
}
