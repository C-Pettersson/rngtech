package com.rngtech.rpg;

import java.util.List;
import java.util.Map;

/** Installed-part generation modifiers are local to the part that rolled them. */
public final class PartGenerationChecks {
    private static int checks;

    private PartGenerationChecks() {
    }

    public static int run() {
        checks = 0;
        partModifiersScaleOnlyTheirOwnPart();
        machineWideIncreasesScalePartFlatsOnce();
        additivePartsKeepTheirFlatSeparate();
        generationBreakdownExplainsFinalOutput();
        return checks;
    }

    private static void partModifiersScaleOnlyTheirOwnPart() {
        MachineStatAccumulator machine = machineWithChamberAndNozzle();
        double recipe = 1000 * 1.35 * 1.97;
        double flat = (207 + 258 * 1.97) * 10;
        near(machine.generatedEnergyTotal(1000, 10), recipe + flat, "part generation modifiers stay local");
        near(machine.effectiveFlatEnergyGenerationBonus(), 207 + 258 * 1.97, "part flats ignore part multipliers");
    }

    private static void machineWideIncreasesScalePartFlatsOnce() {
        MachineStatAccumulator machine = machineWithChamberAndNozzle();
        machine.apply(new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.ENERGY_GENERATION, ModifierOperation.INCREASED_PERCENT, 50));
        machine.apply(flatGeneration(100));
        double recipe = 1000 * 1.35 * 1.97 * 1.5;
        double flat = (207 + 258 * 1.97 + 100) * 10 * 1.5;
        near(machine.generatedEnergyTotal(1000, 10), recipe + flat, "machine-wide increases scale every flat once");
    }

    private static void additivePartsKeepTheirFlatSeparate() {
        MachineStatAccumulator core = MachineStatAccumulator.componentBase(Map.of(MachineStat.ENERGY_GENERATION, 24.0));
        core.apply(flatGeneration(10));
        core.apply(new MachineModifier(ModifierSlot.SUFFIX, MachineStat.ENERGY_GENERATION, ModifierOperation.INCREASED_PERCENT, 50));
        MachineStatAccumulator machine = MachineStatAccumulator.componentBase(Map.of());
        machine.applyPartEnergyGeneration(core, false);
        near(machine.value(MachineStat.ENERGY_GENERATION), (24 + 10) * 1.5, "additive part generation keeps its local increase");
        near(machine.effectiveFlatEnergyGenerationBonus(), 15, "additive part flat counts once");
    }

    private static void generationBreakdownExplainsFinalOutput() {
        MachineStatAccumulator machine = machineWithChamberAndNozzle();
        double multiplier = machine.effectiveEnergyGenerationMultiplier();
        double flat = machine.effectiveFlatEnergyGenerationBonus();
        double finalPerTick = (1000 * multiplier + flat) * 1.08;
        near(MachineStatDisplay.generationOtherFactor(finalPerTick, 1000, multiplier, flat), 1.08, "breakdown isolates other stats");
        near(MachineStatDisplay.generationOtherFactor(finalPerTick, 0, multiplier, flat), 1.0, "unknown base hides the other line");
        require(MachineStatDisplay.energyRate(3592.4).equals("3,592 FE/t"), "Stats tab row shows final FE/t");
    }

    private static MachineStatAccumulator machineWithChamberAndNozzle() {
        MachineStatAccumulator chamber = MachineStatAccumulator.componentBase(Map.of(MachineStat.ENERGY_GENERATION, 1.0));
        chamber.apply(flatGeneration(207));
        MachineStatAccumulator nozzle = MachineStatAccumulator.componentBase(Map.of(MachineStat.ENERGY_GENERATION, 1.35));
        nozzle.apply(flatGeneration(258));
        nozzle.apply(new MachineModifier(ModifierSlot.SUFFIX, MachineStat.ENERGY_GENERATION, ModifierOperation.INCREASED_PERCENT, 97));
        MachineStatAccumulator machine = MachineStatAccumulator.componentBase(Map.of(MachineStat.ENERGY_GENERATION, 1.0));
        machine.applyPartEnergyGeneration(chamber, true);
        machine.applyPartEnergyGeneration(nozzle, true);
        return machine;
    }

    private static MachineModifier flatGeneration(double value) {
        String id = ModifierEligibilityProfiles.ENERGY_GENERATION_FLAT_AFFIX_ID;
        return new MachineModifier(id, id, ModifierSlot.PREFIX, MachineStat.ENERGY_GENERATION, ModifierOperation.ADD, 0,
                ModifierValueRange.fixed(value), value, List.of());
    }

    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError(label);
        }
    }

    private static void near(double actual, double expected, String label) {
        checks++;
        if (Math.abs(actual - expected) > 0.0001) {
            throw new AssertionError(label + ": expected " + expected + " but was " + actual);
        }
    }
}
