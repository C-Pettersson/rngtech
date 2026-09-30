package com.rngtech.content.recipe;

import com.rngtech.RNGTechConfig;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;

public final class ProcessingEnergyScaling {
    public static int crusherEnergy(CrusherRecipe recipe) {
        return scaledEnergy(recipe.energy(), crusherEnergyMultiplier(recipe));
    }

    public static int crusherEnergy(CrusherRecipe recipe, MachineStatAccumulator stats) {
        return crusherEnergy(recipe, stats, 1.0D);
    }

    /** {@code surchargeScale} scales what remains of the high-hardness surcharge after mitigation. */
    public static int crusherEnergy(CrusherRecipe recipe, MachineStatAccumulator stats, double surchargeScale) {
        double multiplier = crusherEnergyMultiplier(recipe);
        double mitigation = stats == null ? 0.0D : stats.value(MachineStat.HIGH_HARDNESS_ENERGY_MITIGATION);
        double mitigated = mitigatedMultiplier(multiplier, mitigation);
        return scaledEnergy(recipe.energy(), 1.0D + (mitigated - 1.0D) * Math.max(0.0D, surchargeScale));
    }

    public static double crusherEnergyMultiplier(CrusherRecipe recipe) {
        return recipe.requiredProcessingLevel() >= RNGTechConfig.CRUSHER_HIGH_HARDNESS_ENERGY_THRESHOLD.get()
                ? RNGTechConfig.CRUSHER_HIGH_HARDNESS_ENERGY_MULTIPLIER.get()
                : 1.0D;
    }

    public static int furnaceEnergy(FurnaceRecipe recipe) {
        return scaledEnergy(recipe.energy(), furnaceEnergyMultiplier(recipe));
    }

    public static double furnaceEnergyMultiplier(FurnaceRecipe recipe) {
        return recipe.minimumTemperature() >= RNGTechConfig.FURNACE_HIGH_HEAT_ENERGY_THRESHOLD.get()
                ? RNGTechConfig.FURNACE_HIGH_HEAT_ENERGY_MULTIPLIER.get()
                : 1.0D;
    }

    private static int scaledEnergy(int baseEnergy, double multiplier) {
        long scaled = Math.round(Math.max(1, baseEnergy) * Math.max(0.01D, multiplier));
        return (int) Math.max(1, Math.min(Integer.MAX_VALUE, scaled));
    }

    private static double mitigatedMultiplier(double multiplier, double mitigationPercent) {
        double extra = Math.max(0.0D, multiplier - 1.0D);
        double retainedExtra = extra * (1.0D - Math.max(0.0D, Math.min(100.0D, mitigationPercent)) / 100.0D);
        return 1.0D + retainedExtra;
    }

    private ProcessingEnergyScaling() {
    }
}
