package com.rngtech.content.machine;

import com.rngtech.rpg.ModifierSet;
import com.rngtech.rpg.Rarity;

import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.Locale;

public enum CrushHeadMaterial implements StringRepresentable {
    FLINT(0, 1, 0, -35, 0),
    IRON(1, 2, 8, 5, 0),
    COPPER(2, 2, 10, 20, 0),
    BRONZE(3, 3, 12, 10, 10),
    STEEL(4, 4, 14, 15, 5),
    ALUMINUM(5, 5, 16, 35, 0),
    TITANIUM(6, 6, 18, 25, 15),
    TUNGSTENSTEEL(7, 7, 20, 10, 30),
    EXOTIC(8, 8, 24, 35, 25);

    private final int stage;
    private final int processingLevel;
    private final int refinementPotential;
    private final String serializedName;

    CrushHeadMaterial(int stage, int processingLevel, int refinementPotential, int processingSpeedPercent, int outputPercent) {
        this.stage = stage;
        this.processingLevel = processingLevel;
        this.refinementPotential = refinementPotential;
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public int stage() {
        return stage;
    }

    public int processingLevel() {
        return processingLevel;
    }

    public int refinementPotential() {
        return refinementPotential;
    }

    public ModifierSet modifierSet() {
        return new ModifierSet(Rarity.NORMAL, refinementPotential, List.of());
    }

    public String itemId() {
        return serializedName + "_crush_head";
    }

    public String materialId() {
        return this == EXOTIC ? "naquadah" : serializedName;
    }

    public String translationKey() {
        return "rngtech.crush_head_material." + serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

}
