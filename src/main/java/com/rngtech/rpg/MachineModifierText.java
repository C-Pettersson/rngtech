package com.rngtech.rpg;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

public final class MachineModifierText {
    public static MutableComponent displayName(MachineModifier modifier) {
        if (MachineNameGenerator.hasModifierWord(modifier)) {
            return MachineNameGenerator.modifierWord(modifier).copy();
        }
        return Component.translatable(slotTranslationKey(modifier.slot()));
    }

    public static MutableComponent displayName(ModifierDefinition definition) {
        if (definition.slot().isAffix()) {
            return Component.translatable(
                    MachineNameGenerator.affixTranslationKey(definition.slot().getSerializedName(), definition.id())
            );
        }
        return Component.translatable(slotTranslationKey(definition.slot()));
    }

    public static MutableComponent definitionStats(ModifierDefinition definition) {
        if (ModifierEligibilityProfiles.isBatteryChassisBalanceMode(definition)) {
            return Component.translatable(MachineBehavior.CHARGE_BALANCER.translationKey());
        }
        if (ModifierEligibilityProfiles.isBehaviorOnlyAffix(definition)) {
            return Component.translatable("rngtech.modifier.effect.behavior_only");
        }
        MutableComponent combined = Component.empty();
        for (int index = 0; index < definition.effects().size(); index++) {
            if (index > 0) {
                combined.append(Component.literal(", "));
            }
            combined.append(Component.translatable(definition.effects().get(index).stat().translationKey()));
        }
        return combined;
    }

    public static MutableComponent slotLabel(ModifierSlot slot) {
        return Component.translatable(slotTranslationKey(slot));
    }

    public static MutableComponent tooltipLine(MachineModifier modifier) {
        if (ModifierEligibilityProfiles.isBatteryChassisBalanceMode(modifier)) {
            return Component.translatable("rngtech.tooltip.modifier.balance_mode");
        }
        if (ModifierEligibilityProfiles.isBehaviorOnlyAffix(modifier)) {
            return Component.translatable("rngtech.modifier.effect.behavior_only");
        }
        if (ModifierEligibilityProfiles.hasCustomDescription(modifier)) {
            return Component.translatable(customDescriptionKey(modifier.affixId()), effectWithStat(modifier));
        }
        if (ModifierEligibilityProfiles.BATTERY_CHASSIS_CHARGED_STORAGE_AFFIX_ID.equals(modifier.affixId())) {
            return Component.translatable("rngtech.tooltip.modifier.charged_storage", effectWithStat(modifier));
        }
        if (modifier.matchesTargetStat(MachineStat.SELF_REPAIR)) {
            return Component.translatable("rngtech.tooltip.modifier.self_repair", formatValue(modifier.value()));
        }
        return effectWithStat(modifier);
    }

    public static boolean hasCustomDescriptionDetail(MachineModifier modifier) {
        return ModifierEligibilityProfiles.hasCustomDescription(modifier);
    }

    public static MutableComponent customDescriptionDetail(MachineModifier modifier) {
        return Component.translatable(customDescriptionDetailKey(modifier.affixId()));
    }

    public static MutableComponent tierTooltipLine(MachineModifier modifier) {
        return Component.translatable("rngtech.tooltip.modifier_tier.effect", rangeEffectWithStat(modifier), modifier.tier());
    }

    public static MutableComponent fullEffect(MachineModifier modifier) {
        if (ModifierEligibilityProfiles.isBatteryChassisBalanceMode(modifier)) {
            return Component.translatable("rngtech.tooltip.modifier.balance_mode.effect");
        }
        if (ModifierEligibilityProfiles.isBehaviorOnlyAffix(modifier)) {
            return Component.translatable("rngtech.modifier.effect.behavior_only");
        }
        return effect(modifier, false);
    }

    public static MutableComponent compactEffect(MachineModifier modifier) {
        if (ModifierEligibilityProfiles.isBatteryChassisBalanceMode(modifier)) {
            return Component.translatable("rngtech.tooltip.modifier.balance_mode.effect");
        }
        if (ModifierEligibilityProfiles.isBehaviorOnlyAffix(modifier)) {
            return Component.translatable("rngtech.modifier.effect.behavior_only");
        }
        return effect(modifier, true);
    }

    public static String formatValue(double value) {
        if (value == Math.rint(value)) {
            return Integer.toString((int) value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static MutableComponent effect(MachineModifier modifier, boolean compact) {
        MutableComponent combined = Component.empty();
        for (int index = 0; index < modifier.effects().size(); index++) {
            if (index > 0) {
                combined.append(Component.literal(", "));
            }
            combined.append(effect(modifier.effects().get(index), compact));
        }
        return combined;
    }

    private static MutableComponent effect(MachineModifierEffect modifierEffect, boolean compact) {
        if (compact) {
            return Component.translatable(
                    "rngtech.modifier.effect.compact.readable",
                    Component.translatable(modifierEffect.stat().translationKey()),
                    MachineStatDisplay.effectValue(modifierEffect)
            );
        }
        return MachineStatDisplay.effectText(modifierEffect);
    }

    private static MutableComponent effectWithStat(MachineModifier modifier) {
        MutableComponent combined = Component.empty();
        for (int index = 0; index < modifier.effects().size(); index++) {
            if (index > 0) {
                combined.append(Component.literal(", "));
            }
            combined.append(effectWithStat(modifier.effects().get(index)));
        }
        return combined;
    }

    private static MutableComponent effectWithStat(MachineModifierEffect modifierEffect) {
        return MachineStatDisplay.effectText(modifierEffect);
    }

    private static MutableComponent rangeEffectWithStat(MachineModifier modifier) {
        MutableComponent combined = Component.empty();
        for (int index = 0; index < modifier.effects().size(); index++) {
            if (index > 0) {
                combined.append(Component.literal(", "));
            }
            combined.append(rangeEffectWithStat(modifier.effects().get(index)));
        }
        return combined;
    }

    private static MutableComponent rangeEffectWithStat(MachineModifierEffect modifierEffect) {
        return Component.translatable(
                "rngtech.tooltip.modifier.effect.range",
                Component.translatable(modifierEffect.stat().translationKey()),
                MachineStatDisplay.rangeValue(modifierEffect)
        );
    }

    private static String slotTranslationKey(ModifierSlot slot) {
        return "rngtech.modifier_slot." + slot.getSerializedName();
    }

    private static String customDescriptionKey(String affixId) {
        return "rngtech.tooltip.modifier.affix." + affixId;
    }

    private static String customDescriptionDetailKey(String affixId) {
        return customDescriptionKey(affixId) + ".extra";
    }

    private MachineModifierText() {
    }
}
