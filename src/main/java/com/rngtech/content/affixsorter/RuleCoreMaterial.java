package com.rngtech.content.affixsorter;

import com.rngtech.rpg.ModifierSet;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** The Affix Sorter's required part. One Exotic body, which carries the sorter's base Rule Capacity. */
public enum RuleCoreMaterial implements StringRepresentable {
    EXOTIC(8, 1.0, 24);

    private final int stage;
    private final double processingSpeed;
    private final int refinementPotential;
    private final String serializedName;

    RuleCoreMaterial(int stage, double processingSpeed, int refinementPotential) {
        this.stage = stage;
        this.processingSpeed = processingSpeed;
        this.refinementPotential = refinementPotential;
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public int stage() {
        return stage;
    }

    public double processingSpeed() {
        return processingSpeed;
    }

    /** The PRD's base of 8 enabled rules, from the {@code affixSorter.baseRuleCapacity} config key. */
    public int ruleCapacity() {
        return AffixSorterCosts.baseRuleCapacity();
    }

    public int refinementPotential() {
        return refinementPotential;
    }

    public String itemId() {
        return serializedName + "_rule_core";
    }

    public String translationKey() {
        return "rngtech.rule_core_material." + serializedName;
    }

    public ModifierSet modifierSet() {
        return com.rngtech.rpg.MachineImplicitCatalog.ruleCore(this).traits().modifierSet();
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
