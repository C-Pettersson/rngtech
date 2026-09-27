package com.rngtech.content.recycling;

import com.rngtech.rpg.ModifierSet;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum DisassemblyHeadMaterial implements StringRepresentable {
    IRON(1, 0.90, 0.96, 8),
    COPPER(2, 1.05, 0.98, 10),
    BRONZE(3, 1.00, 1.04, 12),
    STEEL(4, 1.12, 1.12, 14),
    ALUMINUM(5, 1.24, 1.10, 16),
    TITANIUM(6, 1.36, 1.16, 18),
    TUNGSTENSTEEL(7, 1.22, 1.24, 20),
    EXOTIC(8, 1.50, 1.30, 24);

    private final int stage;
    private final double processingSpeed;
    private final double stability;
    private final int refinementPotential;
    private final String serializedName;

    DisassemblyHeadMaterial(int stage, double processingSpeed, double stability, int refinementPotential) {
        this.stage = stage;
        this.processingSpeed = processingSpeed;
        this.stability = stability;
        this.refinementPotential = refinementPotential;
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public int stage() {
        return stage;
    }

    public double processingSpeed() {
        return processingSpeed;
    }

    public double stability() {
        return stability;
    }

    public int refinementPotential() {
        return refinementPotential;
    }

    public String itemId() {
        return serializedName + "_disassembly_head";
    }

    public String translationKey() {
        return "rngtech.disassembly_head_material." + serializedName;
    }

    public ModifierSet modifierSet() {
        return com.rngtech.rpg.MachineImplicitCatalog.disassemblyHead(this).traits().modifierSet();
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
