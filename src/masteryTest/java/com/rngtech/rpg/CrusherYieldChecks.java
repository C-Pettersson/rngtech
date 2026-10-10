package com.rngtech.rpg;

import com.rngtech.content.machine.CrushHeadMaterial;

import java.util.List;
import java.util.Map;

/** Crusher yield: one soft-capped Output Amount bucket, the Crush Head merge, the time cost, and Energy Usage reductions. */
public final class CrusherYieldChecks {
    private static int checks;

    private CrusherYieldChecks() {
    }

    public static int run() {
        checks = 0;
        softCapBendsTheYieldBucket();
        crushHeadJoinsTheIncreasedBucket();
        yieldTiersStaySmall();
        yieldCostsProcessingTime();
        energyUsageReductionsDivide();
        superOutputStopsAtTheCeiling();
        return checks;
    }

    private static void softCapBendsTheYieldBucket() {
        MachineStatAccumulator exotic = crusher(1.2);
        near(exotic.value(MachineStat.OUTPUT_AMOUNT), 1.2, "an empty bucket leaves the chassis base");
        increase(exotic, MachineStat.OUTPUT_AMOUNT, 100);
        near(exotic.increasedPercent(MachineStat.OUTPUT_AMOUNT), 100, "the raw bucket keeps every percent");
        near(exotic.effectiveIncreasedPercent(MachineStat.OUTPUT_AMOUNT, 0), 50, "+100% increased pays +50%");
        near(exotic.value(MachineStat.OUTPUT_AMOUNT), 1.8, "only the chassis base multiplies the soft-capped bucket");
        increase(exotic, MachineStat.OUTPUT_AMOUNT, 100_000);
        require(exotic.value(MachineStat.OUTPUT_AMOUNT) < 2.4, "the bonus never passes +100%");

        MachineStatAccumulator atLevel = crusher(1.0);
        increase(atLevel, MachineStat.OUTPUT_AMOUNT, 80);
        near(atLevel.valueWithIncreased(MachineStat.OUTPUT_AMOUNT, 20), 1.5, "At-Level Output joins the bucket before the soft cap");

        atLevel.apply(new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.OUTPUT_AMOUNT, ModifierOperation.LESS, 0.75));
        near(atLevel.value(MachineStat.OUTPUT_AMOUNT), 0.75 * (1 + 0.8 / 1.8), "the no-battery penalty stays a less multiplier");

        MachineStatAccumulator furnace = MachineStatAccumulator.componentBase(Map.of(MachineStat.OUTPUT_AMOUNT, 1.0));
        increase(furnace, MachineStat.OUTPUT_AMOUNT, 100);
        near(furnace.value(MachineStat.OUTPUT_AMOUNT), 2.0, "other machines keep a linear Output Amount bucket");
    }

    private static void crushHeadJoinsTheIncreasedBucket() {
        ModifierEligibilityProfile head = ModifierEligibilityProfiles.forMachinePart(MachinePartType.CRUSH_HEAD, MachineType.CRUSHER);
        MachineTraits pulverizing = traits(head, "crush_head_pulverizing", 6);
        MachineStatAccumulator crusher = crusher(1.2);
        ComponentBaseStatCatalog.applyContribution(crusher, ComponentBaseStatCatalog.crushHead(CrushHeadMaterial.EXOTIC), pulverizing);
        near(crusher.increasedPercent(MachineStat.OUTPUT_AMOUNT), 45, "the Exotic head's 25% and Pulverizing's 20% add to the bucket");
        near(crusher.value(MachineStat.OUTPUT_AMOUNT), 1.2 * (1 + 0.45 / 1.45), "the head is no longer a separate more multiplier");

        MachineTraits suffix = traits(head, "output_amount", 6);
        MachineStatAccumulator iron = crusher(1.0);
        ComponentBaseStatCatalog.applyContribution(iron, ComponentBaseStatCatalog.crushHead(CrushHeadMaterial.IRON), suffix);
        near(iron.increasedPercent(MachineStat.OUTPUT_AMOUNT), 20, "a head with no base yield still passes its suffix through");
        near(ComponentBaseStatCatalog.effectiveStats(ComponentBaseStatCatalog.crushHead(CrushHeadMaterial.EXOTIC), pulverizing)
                .value(MachineStat.OUTPUT_AMOUNT), 25, "head yield rolls never scale the head locally");
    }

    private static void yieldTiersStaySmall() {
        ModifierEligibilityProfile crusher = ModifierEligibilityProfiles.forMachine(MachineType.CRUSHER);
        ModifierEligibilityProfile head = ModifierEligibilityProfiles.forMachinePart(MachinePartType.CRUSH_HEAD, MachineType.CRUSHER);
        for (ModifierDefinition definition : List.of(
                definition(crusher, "crusher_jaws"),
                definition(crusher, "output_amount"),
                definition(head, "crush_head_pulverizing"),
                definition(head, "output_amount")
        )) {
            near(definition.rangeForTier(6).max(), 20, definition.id() + " tops out at 20% on T6");
            near(definition.rangeForTier(7).max(), 25, definition.id() + " tops out at 25% on T7");
        }
        require(ModifierEligibilityProfiles.allProfiles().stream()
                .filter(profile -> profile != crusher && profile != head)
                .flatMap(profile -> profile.definitions().stream())
                .anyMatch(definition -> definition.id().equals("output_amount") && definition.rangeForTier(6).max() == 100),
                "other machines keep the shared percent table");
    }

    private static void yieldCostsProcessingTime() {
        MachineStatAccumulator crusher = crusher(1.0);
        require(CrusherYield.jobTicks(crusher, 120, CrusherYield.bonusPercent(crusher, 0)) == 120, "no yield, no time cost");
        increase(crusher, MachineStat.OUTPUT_AMOUNT, 100);
        double bonus = CrusherYield.bonusPercent(crusher, 0);
        int ticks = CrusherYield.jobTicks(crusher, 120, bonus);
        require(ticks == 180, "a +50% yield bonus makes the cycle 50% longer: " + ticks);
        near(crusher.value(MachineStat.OUTPUT_AMOUNT) / ticks, 1.0 / 120, "yield alone keeps items per tick at the bucket-free rate");
        increase(crusher, MachineStat.PROCESSING_SPEED, 100);
        require(CrusherYield.jobTicks(crusher, 120, bonus) == 90, "speed still shortens a yield cycle");

        MachineStatAccumulator penalized = crusher(1.0);
        increase(penalized, MachineStat.OUTPUT_AMOUNT, -50);
        near(CrusherYield.bonusPercent(penalized, 0), 0, "a yield penalty never buys speed");
    }

    private static void energyUsageReductionsDivide() {
        MachineStatAccumulator single = usage();
        single.apply(new MachineModifier(ModifierSlot.SUFFIX, MachineStat.ENERGY_USAGE, ModifierOperation.DECREASED_PERCENT, 100));
        near(single.value(MachineStat.ENERGY_USAGE), 0.5, "one -100% roll halves usage instead of reaching the floor");
        require(single.adjustedEnergyCost(1000) == 500, "FE per craft follows the divided usage");

        MachineStatAccumulator mixed = usage();
        mixed.apply(new MachineModifier(ModifierSlot.SUFFIX, MachineStat.ENERGY_USAGE, ModifierOperation.DECREASED_PERCENT, 50));
        increase(mixed, MachineStat.ENERGY_USAGE, 50);
        mixed.apply(new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.ENERGY_USAGE, ModifierOperation.MORE, 1.5));
        near(mixed.value(MachineStat.ENERGY_USAGE), 1.5 * 1.5 / 1.5, "increases multiply, reductions divide, more stays separate");

        MachineStatAccumulator negativeIncrease = usage();
        increase(negativeIncrease, MachineStat.ENERGY_USAGE, -25);
        near(negativeIncrease.value(MachineStat.ENERGY_USAGE), 1 / 1.25, "a negative increase counts as a reduction");
    }

    private static void superOutputStopsAtTheCeiling() {
        MachineStatAccumulator crusher = crusher(1.2);
        crusher.apply(new MachineModifier(ModifierSlot.SUFFIX, MachineStat.SUPER_OUTPUT_CHANCE, ModifierOperation.ADD, 26));
        near(crusher.value(MachineStat.SUPER_OUTPUT_CHANCE), MachineStatAccumulator.CRUSHER_SUPER_OUTPUT_CEILING,
                "two T7 rolls plus the Assayer stop at 25%");
    }

    /** A Crusher with the chassis's base Output Amount; the chassis itself reads config, which checks do not load. */
    private static MachineStatAccumulator crusher(double outputAmount) {
        return MachineStatAccumulator.componentBase(Map.of(MachineStat.OUTPUT_AMOUNT, outputAmount, MachineStat.PROCESSING_SPEED, 1.0))
                .withCrusherYieldRules();
    }

    private static MachineStatAccumulator usage() {
        return MachineStatAccumulator.componentBase(Map.of(MachineStat.ENERGY_USAGE, 1.0));
    }

    private static void increase(MachineStatAccumulator stats, MachineStat stat, double percent) {
        stats.apply(new MachineModifier(ModifierSlot.IMPLICIT, stat, ModifierOperation.INCREASED_PERCENT, percent));
    }

    private static ModifierDefinition definition(ModifierEligibilityProfile profile, String id) {
        return profile.definitions().stream()
                .filter(definition -> definition.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError(profile.id() + " has no " + id));
    }

    /** A Magic item with one affix at {@code tier}'s maximum roll. */
    private static MachineTraits traits(ModifierEligibilityProfile profile, String id, int tier) {
        ModifierDefinition definition = definition(profile, id);
        ModifierValueRange range = definition.rangeForTier(tier);
        MachineModifier modifier = new MachineModifier(definition.id(), definition.modGroup(), definition.slot(), definition.stat(),
                definition.operation(), tier, range, range.max(), List.of());
        return new MachineTraits(Rarity.MAGIC, 0, List.of(modifier));
    }

    private static void near(double actual, double expected, String message) {
        require(Math.abs(actual - expected) < 0.0001, message + ": expected " + expected + " but was " + actual);
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
