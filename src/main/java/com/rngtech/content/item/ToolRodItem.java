package com.rngtech.content.item;

import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.tool.ToolBaseStatCatalog;
import com.rngtech.content.tool.ToolRodMaterial;
import com.rngtech.rpg.MachineNameGenerator;
import com.rngtech.rpg.MachineTraitRoller;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.MachineType;
import com.rngtech.rpg.ModifierEligibilityProfiles;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class ToolRodItem extends Item {
    private final ToolRodMaterial material;

    public ToolRodItem(ToolRodMaterial material, Properties properties) {
        super(properties);
        this.material = material;
    }

    public ToolRodMaterial material() {
        return material;
    }

    public MachineTraits traits(ItemStack stack) {
        MachineTraits traits = stack.get(ModDataComponents.MACHINE_TRAITS.get());
        return traits == null ? MachineTraits.EMPTY : traits;
    }

    @Override
    public void onCraftedPostProcess(ItemStack stack, Level level) {
        super.onCraftedPostProcess(stack, level);
        CraftedTraitOutputs.handleCrafted(stack, level.random);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (CraftedTraitOutputs.isUnidentified(stack)) {
            return CraftedTraitOutputs.unidentifiedName(super.getName(stack));
        }
        MachineTraits traits = traits(stack);
        return traits.isEmpty() ? super.getName(stack) : MachineNameGenerator.generatedName(traits, super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        MachineTraitTooltip.appendUnidentified(stack, tooltipComponents);
        MachineTraits traits = traits(stack);
        if (stack.has(ModDataComponents.MACHINE_TRAITS.get())) {
            MachineTraitTooltip.appendTraitHeader(traits, tooltipComponents, true);
        } else {
            MachineTraitTooltip.appendUnrolledTraitHeader(
                    MachineTraitRoller.refinementPotentialRange(ModifierEligibilityProfiles.forMachine(MachineType.TOOL_ROD), material.stage()),
                    tooltipComponents
            );
        }
        if (MachineTraitTooltip.shouldShowMaterialData()) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.tool_rod.battery_support",
                    material.batterySupport()
            ).withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.tool_rod.accepts_cells",
                    material.batterySupport()
            ).withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.tool_part.stage",
                    material.stage()
            ).withStyle(ChatFormatting.GRAY));
        }
        if (ModularToolItem.hasStoredWear(stack)) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.tool_part.stored_wear",
                    ModularToolItem.storedWearPercent(stack)
            ).withStyle(ChatFormatting.RED));
        }
        FieldToolTooltip.appendBaseStats(stack, ToolBaseStatCatalog.baseStats(stack), tooltipComponents);
        MachineTraitTooltip.appendTraitDetails(traits, tooltipComponents);
        MachineTraitTooltip.appendTraitKeyHints(tooltipComponents);
    }
}
