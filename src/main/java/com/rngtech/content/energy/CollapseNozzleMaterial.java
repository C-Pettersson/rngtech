package com.rngtech.content.energy;

import com.rngtech.rpg.ModifierSet;

import java.util.Locale;

public enum CollapseNozzleMaterial {
    STEEL(5, 1.10, 1.20, 1.00, 1.00, 1.00, 1.00, 12, true, true),
    TITANIUM(6, 1.20, 1.10, 1.00, 1.00, 1.00, 1.00, 14, true, true),
    NITROGEN_SEPARATION(6, 0.40, 1.20, 1.50, 1.50, 1.00, 1.00, 16, true, false, "nitrogen_separation_nozzle"),
    TUNGSTENSTEEL(7, 1.35, 1.00, 1.00, 1.00, 1.00, 1.00, 18, true, true),
    NULLITE(8, 1.50, 0.95, 1.00, 1.00, 1.25, 1.15, 22, true, true),
    EXOTIC(8, 1.60, 0.90, 1.00, 1.10, 1.60, 1.30, 26, true, true);

    private final int stage;
    private final double generationMultiplier;
    private final double strainMultiplier;
    private final double outputMultiplier;
    private final double fluidTransferMultiplier;
    private final double vacuumGenerationMultiplier;
    private final double vacuumStabilityMultiplier;
    private final int refinementPotential;
    private final boolean cavitationCompatible;
    private final boolean vacuumCollapseCompatible;
    private final String serializedName;
    private final String itemId;

    CollapseNozzleMaterial(
            int stage,
            double generationMultiplier,
            double strainMultiplier,
            double outputMultiplier,
            double fluidTransferMultiplier,
            double vacuumGenerationMultiplier,
            double vacuumStabilityMultiplier,
            int refinementPotential,
            boolean cavitationCompatible,
            boolean vacuumCollapseCompatible
    ) {
        this(
                stage,
                generationMultiplier,
                strainMultiplier,
                outputMultiplier,
                fluidTransferMultiplier,
                vacuumGenerationMultiplier,
                vacuumStabilityMultiplier,
                refinementPotential,
                cavitationCompatible,
                vacuumCollapseCompatible,
                null
        );
    }

    CollapseNozzleMaterial(
            int stage,
            double generationMultiplier,
            double strainMultiplier,
            double outputMultiplier,
            double fluidTransferMultiplier,
            double vacuumGenerationMultiplier,
            double vacuumStabilityMultiplier,
            int refinementPotential,
            boolean cavitationCompatible,
            boolean vacuumCollapseCompatible,
            String itemId
    ) {
        this.stage = stage;
        this.generationMultiplier = generationMultiplier;
        this.strainMultiplier = strainMultiplier;
        this.outputMultiplier = outputMultiplier;
        this.fluidTransferMultiplier = fluidTransferMultiplier;
        this.vacuumGenerationMultiplier = vacuumGenerationMultiplier;
        this.vacuumStabilityMultiplier = vacuumStabilityMultiplier;
        this.refinementPotential = refinementPotential;
        this.cavitationCompatible = cavitationCompatible;
        this.vacuumCollapseCompatible = vacuumCollapseCompatible;
        serializedName = name().toLowerCase(Locale.ROOT);
        this.itemId = itemId == null ? serializedName + "_collapse_nozzle" : itemId;
    }

    public int stage() {
        return stage;
    }

    public double generationMultiplier() {
        return generationMultiplier;
    }

    public double strainMultiplier() {
        return strainMultiplier;
    }

    public double outputMultiplier() {
        return outputMultiplier;
    }

    public double fluidTransferMultiplier() {
        return fluidTransferMultiplier;
    }

    public double vacuumGenerationMultiplier() {
        return vacuumGenerationMultiplier;
    }

    public double vacuumProcessingSpeedMultiplier() {
        return stage < 7 ? 1.0 : 1.0 + (stage - 6) * 0.05;
    }

    public double vacuumStabilityMultiplier() {
        return vacuumStabilityMultiplier;
    }

    public int refinementPotential() {
        return refinementPotential;
    }

    public boolean cavitationCompatible() {
        return cavitationCompatible;
    }

    public boolean vacuumCollapseCompatible() {
        return vacuumCollapseCompatible;
    }

    public String itemId() {
        return itemId;
    }

    public String getSerializedName() {
        return serializedName;
    }

    public ModifierSet modifierSet() {
        return com.rngtech.rpg.MachineImplicitCatalog.collapseNozzle(this).traits().modifierSet();
    }
}
