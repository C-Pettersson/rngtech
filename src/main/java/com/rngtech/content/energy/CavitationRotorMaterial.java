package com.rngtech.content.energy;

import com.rngtech.rpg.ModifierSet;

import java.util.Locale;

public enum CavitationRotorMaterial {
    STEEL(5, 1.00, 1.00, 1.00, 14),
    TITANIUM(6, 1.20, 0.85, 1.00, 16),
    NITROGEN_EXTRACTION(6, 0.35, 1.15, 1.75, 18, "nitrogen_extraction_rotor"),
    TUNGSTENSTEEL(7, 1.35, 0.70, 1.00, 18),
    AETHERGOLD(7, 6.00, 5.00, 1.00, 4.00, 1.00, 20),
    NULLITE(8, 1.60, 0.60, 1.00, 20);

    public static final int BASE_DURABILITY = 1000;

    private final int stage;
    private final double generationMultiplier;
    private final double wearMultiplier;
    private final double outputMultiplier;
    private final double processingSpeedMultiplier;
    private final double energyTransferMultiplier;
    private final int refinementPotential;
    private final String serializedName;
    private final String itemId;

    CavitationRotorMaterial(
            int stage,
            double generationMultiplier,
            double wearMultiplier,
            double outputMultiplier,
            int refinementPotential
    ) {
        this(stage, generationMultiplier, wearMultiplier, outputMultiplier, 1.0, 1.0, refinementPotential, null);
    }

    CavitationRotorMaterial(
            int stage,
            double generationMultiplier,
            double wearMultiplier,
            double outputMultiplier,
            double processingSpeedMultiplier,
            double energyTransferMultiplier,
            int refinementPotential
    ) {
        this(stage, generationMultiplier, wearMultiplier, outputMultiplier, processingSpeedMultiplier, energyTransferMultiplier, refinementPotential, null);
    }

    CavitationRotorMaterial(
            int stage,
            double generationMultiplier,
            double wearMultiplier,
            double outputMultiplier,
            int refinementPotential,
            String itemId
    ) {
        this(stage, generationMultiplier, wearMultiplier, outputMultiplier, 1.0, 1.0, refinementPotential, itemId);
    }

    CavitationRotorMaterial(
            int stage,
            double generationMultiplier,
            double wearMultiplier,
            double outputMultiplier,
            double processingSpeedMultiplier,
            double energyTransferMultiplier,
            int refinementPotential,
            String itemId
    ) {
        this.stage = stage;
        this.generationMultiplier = generationMultiplier;
        this.wearMultiplier = wearMultiplier;
        this.outputMultiplier = outputMultiplier;
        this.processingSpeedMultiplier = processingSpeedMultiplier;
        this.energyTransferMultiplier = energyTransferMultiplier;
        this.refinementPotential = refinementPotential;
        serializedName = name().toLowerCase(Locale.ROOT);
        this.itemId = itemId == null ? serializedName + "_cavitation_rotor" : itemId;
    }

    public int stage() {
        return stage;
    }

    public double generationMultiplier() {
        return generationMultiplier;
    }

    public double wearMultiplier() {
        return wearMultiplier;
    }

    public double outputMultiplier() {
        return outputMultiplier;
    }

    public double processingSpeedMultiplier() {
        return processingSpeedMultiplier;
    }

    public double energyTransferMultiplier() {
        return energyTransferMultiplier;
    }

    public int refinementPotential() {
        return refinementPotential;
    }

    public String itemId() {
        return itemId;
    }

    public String getSerializedName() {
        return serializedName;
    }

    public ModifierSet modifierSet() {
        return com.rngtech.rpg.MachineImplicitCatalog.cavitationRotor(this).traits().modifierSet();
    }
}
