package com.rngtech.content.item;

import com.rngtech.rpg.ComponentBaseStatCatalog;
import com.rngtech.rpg.MachineBaseStatCatalog;
import com.rngtech.rpg.MachineBehavior;
import com.rngtech.rpg.MachineImplicitCatalog;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineModifierText;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraitRoller;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.ModifierSet;
import com.rngtech.rpg.Rarity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

final class MachineTraitTooltip {
    static boolean shouldShowMaterialData() {
        return TooltipKeyState.hasShiftDown();
    }

    static void appendIdentity(ItemStack stack, List<Component> tooltipComponents) {
        if (!shouldShowMaterialData()) {
            return;
        }
        MachineImplicitCatalog.Identity identity = MachineImplicitCatalog.identity(stack);
        if (!identity.translationKey().isEmpty()) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.identity",
                    Component.translatable(identity.translationKey())
            ).withStyle(ChatFormatting.GRAY));
        }
    }

    static void appendUnidentified(ItemStack stack, List<Component> tooltipComponents) {
        if (CraftedTraitOutputs.isUnidentified(stack)) {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.unidentified_traits").withStyle(ChatFormatting.GRAY));
        }
    }

    static void appendBaseStats(ItemStack stack, List<Component> tooltipComponents) {
        if (!shouldShowMaterialData()) {
            return;
        }
        MachineStatAccumulator baseStats = MachineBaseStatCatalog.forStack(stack);
        if (baseStats == null) {
            return;
        }
        List<MachineStat> stats = MachineBaseStatCatalog.summaryStats(stack).stream()
                .filter(stat -> shouldShowBaseStat(stat, baseStats.baseValue(stat)))
                .toList();
        if (stats.isEmpty()) {
            return;
        }

        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.translatable("rngtech.tooltip.base_stats").withStyle(ChatFormatting.DARK_AQUA));
        for (MachineStat stat : stats) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.base_stat",
                    Component.translatable(stat.translationKey()),
                    MachineModifierText.formatValue(baseStats.baseValue(stat))
            ).withStyle(ChatFormatting.BLUE));
        }
    }

    static void appendComponentBaseStats(ItemStack stack, List<Component> tooltipComponents) {
        if (!shouldShowMaterialData()) {
            return;
        }
        MachineStatAccumulator baseStats = ComponentBaseStatCatalog.baseStats(stack);
        if (baseStats == null) {
            return;
        }
        List<MachineStat> stats = ComponentBaseStatCatalog.summaryStats(stack).stream()
                .filter(stat -> shouldShowComponentBaseStat(stat, baseStats.baseValue(stat)))
                .toList();
        if (stats.isEmpty()) {
            return;
        }

        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.translatable("rngtech.tooltip.base_stats").withStyle(ChatFormatting.DARK_AQUA));
        for (MachineStat stat : stats) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.base_stat",
                    Component.translatable(stat.translationKey()),
                    MachineModifierText.formatValue(baseStats.baseValue(stat))
            ).withStyle(ChatFormatting.BLUE));
        }
    }

    static void appendTraits(MachineTraits traits, List<Component> tooltipComponents, boolean showRarity) {
        if (!appendTraitHeader(traits, tooltipComponents, showRarity)) {
            return;
        }
        appendTraitDetails(traits, tooltipComponents);
        appendTraitKeyHints(tooltipComponents);
    }

    static boolean appendTraitHeader(MachineTraits traits, List<Component> tooltipComponents, boolean showRarity) {
        if (traits.isEmpty()) {
            return false;
        }
        if (showRarity) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.rarity",
                    Component.translatable(traits.rarity().translationKey()).withStyle(rarityColor(traits.rarity()))
            ).withStyle(ChatFormatting.GRAY));
        }
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.refinement_potential",
                refinementPotentialValue(traits.refinementPotential())
        ).withStyle(ChatFormatting.GRAY));
        return true;
    }

    static void appendTraitDetails(MachineTraits traits, List<Component> tooltipComponents) {
        appendModifierSections(traits, tooltipComponents);
        appendBehaviorSection(traits, tooltipComponents);
        if (TooltipKeyState.hasAltDown()) {
            appendTierTooltip(tooltipComponents, traits.modifierSet());
        }
    }

    static void appendTraitKeyHints(List<Component> tooltipComponents) {
        if (TooltipKeyState.hasDetailKeyDown()) {
            return;
        }
        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.translatable("rngtech.tooltip.hint.shift").withStyle(ChatFormatting.DARK_GRAY));
        tooltipComponents.add(Component.translatable("rngtech.tooltip.hint.alt").withStyle(ChatFormatting.DARK_GRAY));
    }

    static void appendModifierSet(ModifierSet modifierSet, List<Component> tooltipComponents) {
        if (modifierSet.modifiers().isEmpty() && modifierSet.refinementPotential() <= 0) {
            return;
        }
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.refinement_potential",
                refinementPotentialValue(modifierSet.refinementPotential())
        ).withStyle(ChatFormatting.GRAY));
        appendModifierSections(modifierSet, tooltipComponents);
        if (TooltipKeyState.hasAltDown()) {
            appendTierTooltip(tooltipComponents, modifierSet);
        }
        appendTraitKeyHints(tooltipComponents);
    }

    static void appendUnrolledTraits(
            MachineTraits baseTraits,
            MachineTraitRoller.RefinementPotentialRange refinementPotentialRange,
            List<Component> tooltipComponents
    ) {
        appendUnrolledTraitHeader(refinementPotentialRange, tooltipComponents);
        appendTraitDetails(baseTraits, tooltipComponents);
        appendTraitKeyHints(tooltipComponents);
    }

    static void appendUnrolledTraitHeader(
            MachineTraitRoller.RefinementPotentialRange refinementPotentialRange,
            List<Component> tooltipComponents
    ) {
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.roll_preview",
                refinementPotentialValue(refinementPotentialRange.min()),
                refinementPotentialValue(refinementPotentialRange.max())
        ).withStyle(ChatFormatting.GRAY));
    }

    private static void appendModifierSections(MachineTraits traits, List<Component> tooltipComponents) {
        appendModifierSections(traits.modifierSet(), tooltipComponents);
    }

    private static void appendModifierSections(ModifierSet modifierSet, List<Component> tooltipComponents) {
        boolean hasBaseModifiers = modifierSet.modifiers().stream().anyMatch(modifier -> !modifier.slot().isAffix());
        boolean hasRolledModifiers = modifierSet.modifiers().stream().anyMatch(modifier -> modifier.slot().isAffix());
        if (hasBaseModifiers) {
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("rngtech.tooltip.base_modifiers").withStyle(ChatFormatting.DARK_AQUA));
            for (var modifier : modifierSet.modifiers()) {
                if (!modifier.slot().isAffix()) {
                    tooltipComponents.add(MachineModifierText.tooltipLine(modifier).withStyle(ChatFormatting.BLUE));
                    appendModifierDescriptionDetail(modifier, tooltipComponents);
                }
            }
        }
        if (hasRolledModifiers) {
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("rngtech.tooltip.modifiers").withStyle(ChatFormatting.DARK_AQUA));
            for (var modifier : modifierSet.modifiers()) {
                if (modifier.slot().isAffix()) {
                    tooltipComponents.add(MachineModifierText.tooltipLine(modifier).withStyle(ChatFormatting.BLUE));
                    appendModifierDescriptionDetail(modifier, tooltipComponents);
                }
            }
        }
    }

    private static void appendModifierDescriptionDetail(MachineModifier modifier, List<Component> tooltipComponents) {
        if (!TooltipKeyState.hasShiftDown() || !MachineModifierText.hasCustomDescriptionDetail(modifier)) {
            return;
        }
        tooltipComponents.add(MachineModifierText.customDescriptionDetail(modifier).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static void appendBehaviorSection(MachineTraits traits, List<Component> tooltipComponents) {
        if (traits.behaviors().isEmpty()) {
            return;
        }
        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.translatable("rngtech.tooltip.base_behaviors").withStyle(ChatFormatting.DARK_AQUA));
        for (MachineBehavior behavior : traits.behaviors()) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.behavior",
                    Component.translatable(behavior.translationKey()),
                    Component.translatable(behavior.descriptionKey())
            ).withStyle(ChatFormatting.BLUE));
        }
    }

    private static void appendTierTooltip(List<Component> tooltipComponents, ModifierSet modifierSet) {
        boolean hasTieredModifiers = modifierSet.modifiers().stream().anyMatch(modifier -> modifier.hasTier());
        if (!hasTieredModifiers) {
            return;
        }

        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.translatable("rngtech.tooltip.modifier_tiers").withStyle(ChatFormatting.DARK_AQUA));
        for (var modifier : modifierSet.modifiers()) {
            if (modifier.hasTier()) {
                tooltipComponents.add(MachineModifierText.tierTooltipLine(modifier).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    private static boolean shouldShowBaseStat(MachineStat stat, double value) {
        if (Math.abs(value) <= 0.0001) {
            return false;
        }
        if (stat == MachineStat.PARALLEL_JOBS) {
            return value > 1.0001;
        }
        if (stat == MachineStat.BURST_DURATION || stat == MachineStat.FLUID_TRANSFER) {
            return value > 0.0001;
        }
        return true;
    }

    private static boolean shouldShowComponentBaseStat(MachineStat stat, double value) {
        if (Math.abs(value) <= 0.0001) {
            return false;
        }
        if (Math.abs(value - 1.0) > 0.0001) {
            return true;
        }
        return switch (stat) {
            case PROCESSING_LEVEL,
                    ENERGY_CAPACITY,
                    ENERGY_TRANSFER,
                    ENERGY_GENERATION,
                    INPUT_SLOTS,
                    MAX_TEMPERATURE,
                    FLUID_TRANSFER,
                    REFINEMENT_POTENTIAL_BONUS -> true;
            default -> false;
        };
    }

    private static ChatFormatting rarityColor(Rarity rarity) {
        return switch (rarity) {
            case NORMAL -> ChatFormatting.WHITE;
            case MAGIC -> ChatFormatting.AQUA;
            case RARE -> ChatFormatting.GOLD;
            case UNIQUE -> ChatFormatting.LIGHT_PURPLE;
        };
    }

    private static Component refinementPotentialValue(int value) {
        return Component.literal(Integer.toString(value)).withStyle(value > 0 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.DARK_GRAY);
    }

    private MachineTraitTooltip() {
    }
}
