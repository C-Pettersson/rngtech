package com.rngtech.content.energy;

/**
 * Cavitation Generator heat strain and Water, read from the machine's Heat Core, Servo, and Bonus Output stats. Each stat
 * keeps the meaning it has in the furnaces: Heat Transfer and Strain Recovery shed strain faster, Overheat Tolerance
 * raises the strain cap, and Max Temperature softens the FE lost at that cap.
 */
public final class CavitationHeat {
    public static final double BASE_STRAIN_CAP = 1000.0;
    public static final double BASE_MAX_PENALTY = 0.5;
    /** Heat Core Max Temperature per extra 1,000 strain of headroom in the penalty curve. */
    private static final double MAX_TEMPERATURE_SCALE = 4.0;
    private static final double MIN_OUTPUT_AMOUNT = 0.5;

    private CavitationHeat() {
    }

    /**
     * Strain shed per tick: 1, plus half the Heat Core's stage scaled by Heat Transfer beyond the core's own base, plus
     * flat Strain Recovery.
     */
    public static double cooling(int heatCoreStage, double heatTransferBonus, double strainRecovery) {
        return 1.0 + Math.max(0, heatCoreStage / 2) * Math.max(0.1, heatTransferBonus) + Math.max(0.0, strainRecovery);
    }

    public static double strainCap(double overheatTolerance) {
        return BASE_STRAIN_CAP * Math.max(1.0, overheatTolerance);
    }

    /** The FE fraction lost at the strain cap; a hotter-rated Heat Core loses less. */
    public static double maxPenalty(double maxTemperature) {
        return BASE_MAX_PENALTY * BASE_STRAIN_CAP / (BASE_STRAIN_CAP + Math.max(0.0, maxTemperature) / MAX_TEMPERATURE_SCALE);
    }

    public static double penalty(double strain, double cap, double maxPenalty) {
        return maxPenalty * Math.max(0.0, Math.min(1.0, strain / Math.max(1.0, cap)));
    }

    /** Water one cycle draws: Bonus Output gets the same FE from less Water when the recipe has no fluid output. */
    public static int waterPerCycle(int recipeAmount, double outputAmount) {
        return Math.max(1, (int) Math.ceil(recipeAmount / Math.max(MIN_OUTPUT_AMOUNT, outputAmount) - 1e-9));
    }
}
