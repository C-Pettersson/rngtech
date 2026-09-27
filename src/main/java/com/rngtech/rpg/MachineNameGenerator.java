package com.rngtech.rpg;

import net.minecraft.network.chat.Component;

import java.util.Set;

public final class MachineNameGenerator {
    private static final Set<String> TIER_NAMED_PREFIXES = Set.of(
            "crusher_frame",
            "crusher_kinetics",
            "crusher_jaws",
            "crusher_ore_handling",
            "crusher_battery_link",
            "crusher_feed_control",
            "crusher_compression",
            "crusher_vibration",
            "crusher_throughput",
            "crusher_salvage",
            "crush_head_pulverizing",
            "crush_head_jagged",
            "crush_head_kinetic"
    );

    public static Component generatedName(MachineTraits traits, Component baseName) {
        MachineModifier prefix = firstModifier(traits, ModifierSlot.PREFIX);
        MachineModifier suffix = firstModifier(traits, ModifierSlot.SUFFIX);
        if (prefix == null && suffix == null) {
            return baseName;
        }

        Component prefixWord = prefix == null ? Component.empty() : affixWord("prefix", prefix);
        Component suffixWord = suffix == null ? Component.empty() : affixWord("suffix", suffix);
        if (prefix != null && suffix != null) {
            return Component.translatable("rngtech.machine_name.prefix_suffix", prefixWord, baseName, suffixWord);
        }
        if (prefix != null) {
            return Component.translatable("rngtech.machine_name.prefix", prefixWord, baseName);
        }
        return Component.translatable("rngtech.machine_name.suffix", baseName, suffixWord);
    }

    public static Component modifierWord(MachineModifier modifier) {
        if (modifier.slot() == ModifierSlot.PREFIX) {
            return affixWord("prefix", modifier);
        }
        if (modifier.slot() == ModifierSlot.SUFFIX) {
            return affixWord("suffix", modifier);
        }
        return Component.empty();
    }

    public static boolean hasModifierWord(MachineModifier modifier) {
        return modifier.slot().isAffix();
    }

    private static MachineModifier firstModifier(MachineTraits traits, ModifierSlot slot) {
        for (MachineModifier modifier : traits.modifiers()) {
            if (modifier.slot() == slot) {
                return modifier;
            }
        }
        return null;
    }

    private static Component affixWord(String affixType, MachineModifier modifier) {
        String id = modifier.hasAffixId() ? modifier.affixId() : modifier.stat().getSerializedName();
        if ("prefix".equals(affixType) && modifier.hasTier() && TIER_NAMED_PREFIXES.contains(id)) {
            return Component.translatable(affixTierTranslationKey(affixType, id, modifier.tier()));
        }
        return Component.translatable(affixTranslationKey(affixType, id));
    }

    public static String affixTranslationKey(String affixType, String id) {
        return "rngtech.affix." + affixType + "." + displayId(id);
    }

    public static String affixTierTranslationKey(String affixType, String id, int tier) {
        return affixTranslationKey(affixType, id) + ".tier." + Math.max(1, tier);
    }

    private static String displayId(String id) {
        for (String suffix : new String[] {"_tuned", "_reinforced", "_harmonic", "_focused", "_amplified"}) {
            if (id.endsWith(suffix)) {
                return suffix.substring(1);
            }
        }
        return id;
    }

    private MachineNameGenerator() {
    }
}
