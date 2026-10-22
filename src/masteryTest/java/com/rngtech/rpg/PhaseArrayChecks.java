package com.rngtech.rpg;

import com.rngtech.RNGTech;
import com.rngtech.content.loot.GlyphSources;
import com.rngtech.rpg.refinement.PhaseLineStats;
import com.rngtech.rpg.refinement.PhaseOutcome;
import com.rngtech.rpg.refinement.PhasePulse;
import com.rngtech.rpg.refinement.PhasePulseSequence;
import com.rngtech.rpg.refinement.RefinementEngine;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/** The Phase Array parts: pools deep enough to roll, every affix read by the line, and the PRD's base numbers. */
public final class PhaseArrayChecks {
    private static final int MIN_AFFIXES_PER_SLOT = 6;
    private static int checks;

    private PhaseArrayChecks() {
    }

    public static int run() {
        checks = 0;
        for (ModifierEligibilityProfile profile : List.of(ModifierEligibilityProfiles.PHASE_EMITTER, ModifierEligibilityProfiles.PHASE_COIL)) {
            poolsAreDeepEnough(profile);
            everyAffixIsRead(profile);
        }
        baseStatsMatchTheRules();
        positionFloorsClimb();
        pulseOddsMatchTheSequence();
        costsFollowTheFormula();
        engineOperationsRespectTheirLimits();
        outcomeRulesFollowThePrd();
        fractureChanceFollowsStrain();
        sequenceFramesFollowThePulse();
        dormantSitesPayByTier();
        return checks;
    }

    /** A Rare rolls three of each slot, so a pool with fewer than six per slot would hand every Rare the same affixes. */
    private static void poolsAreDeepEnough(ModifierEligibilityProfile profile) {
        for (ModifierSlot slot : List.of(ModifierSlot.PREFIX, ModifierSlot.SUFFIX)) {
            long count = profile.definitions().stream()
                    .filter(definition -> definition.slot() == slot && definition.flags().contains(ModifierFlag.CAN_ROLL))
                    .count();
            check(count >= MIN_AFFIXES_PER_SLOT, profile.id() + " rolls " + count + " " + slot + " affixes; at least "
                    + MIN_AFFIXES_PER_SLOT + " are needed");
        }
    }

    private static void everyAffixIsRead(ModifierEligibilityProfile profile) {
        for (ModifierDefinition definition : profile.definitions()) {
            for (ModifierEffectDefinition effect : definition.effects()) {
                check(PhaseArrayRules.LINE_STATS.contains(effect.stat()),
                        profile.id() + " affix " + definition.id() + " rolls " + effect.stat() + ", which the line never reads");
            }
        }
    }

    private static void baseStatsMatchTheRules() {
        MachineStatAccumulator emitter = MachineBaseStatCatalog.phaseEmitter();
        near(emitter.value(MachineStat.CHARGE_CAPACITY), PhaseArrayRules.emitterChargeCapacity(), "Emitter Charge Capacity");
        near(emitter.value(MachineStat.CHARGE_RATE), PhaseArrayRules.emitterChargeRate(), "Emitter Charge Rate");
        near(emitter.value(MachineStat.PARTIAL_WINDOW), PhaseArrayRules.basePartialWindow(), "Emitter Partial Window");
        near(emitter.value(MachineStat.HOLD_CHANCE), 0, "Emitter has no Hold Chance");
        MachineStatAccumulator coil = MachineBaseStatCatalog.phaseCoil();
        near(coil.value(MachineStat.HOLD_CHANCE), PhaseArrayRules.coilHoldChance(), "Coil Hold Chance");
        near(coil.value(MachineStat.CHARGE_CAPACITY), PhaseArrayRules.coilChargeCapacity(), "Coil Charge Capacity");
        near(coil.value(MachineStat.STRAIN_TOLERANCE), 0, "Coil Strain Tolerance starts at 0");
    }

    private static void positionFloorsClimb() {
        int previous = 0;
        for (int position = 1; position <= PhaseArrayRules.MAX_COILS; position++) {
            int floor = PhaseArrayRules.lowestGlyphTier(position);
            check(floor >= previous && floor >= 1 && floor <= PhaseArrayRules.GLYPH_TIERS,
                    "Position " + position + " needs tier " + floor + ", below position " + (position - 1));
            previous = floor;
        }
        check(PhaseArrayRules.lowestGlyphTier(PhaseArrayRules.MAX_COILS) == PhaseArrayRules.GLYPH_TIERS,
                "The last position needs the top Glyph tier");
        check(PhaseArrayRules.lowestGlyphTier(PhaseArrayRules.MIN_COILS) == 1, "The first three positions need Tier I");
    }

    /** The closed-form odds the Cradle previews are what the pulse actually rolls. */
    private static void pulseOddsMatchTheSequence() {
        for (int coils : new int[] { 5, 7, 9 }) {
            PhaseLineStats line = line(coils, 0);
            for (int strain : new int[] { 0, 10, 30 }) {
                PhasePulse.Odds odds = PhasePulse.odds(line, 5, strain);
                near(odds.success() + odds.overhold() + odds.partial() + odds.miss() + odds.fracture(), 1.0, "odds sum to one");
                double expectedFracture = Math.min(1.0, strain * PhaseArrayRules.fracturePerStrain());
                near(odds.fracture(), expectedFracture, "fracture is 1% per Strain point");
                RandomSource random = RandomSource.create(coils * 1000L + strain);
                int trials = 40_000;
                int[] counts = new int[PhasePulse.Outcome.values().length];
                for (int trial = 0; trial < trials; trial++) {
                    counts[PhasePulse.resolve(line, 5, strain, random).outcome().ordinal()]++;
                }
                double[] expected = { odds.success(), odds.overhold(), odds.partial(), odds.miss(), odds.fracture() };
                for (PhasePulse.Outcome outcome : PhasePulse.Outcome.values()) {
                    double observed = counts[outcome.ordinal()] / (double) trials;
                    check(Math.abs(observed - expected[outcome.ordinal()]) < 0.012,
                            coils + " Coils, Strain " + strain + ": " + outcome + " rolled " + observed + ", preview says " + expected[outcome.ordinal()]);
                }
            }
        }
        PhaseLineStats tolerant = line(7, 5);
        near(PhasePulse.fractureChance(tolerant, 5), 0.0, "Strain within Tolerance adds no Fracture");
        near(PhasePulse.fractureChance(tolerant, 8), 0.03, "Strain above Tolerance counts from the Tolerance");
        check(PhasePulse.odds(line(4, 0), 5, 0).success() == 0.0, "A line shorter than the operation cannot succeed");
    }

    private static PhaseLineStats line(int coils, int tolerance) {
        List<MachineStatAccumulator> coilStats = new ArrayList<>();
        for (int i = 0; i < coils; i++) {
            coilStats.add(MachineBaseStatCatalog.phaseCoil());
        }
        PhaseLineStats base = PhaseLineStats.of(MachineBaseStatCatalog.phaseEmitter(), coilStats, coils);
        return new PhaseLineStats(base.chargeCapacity(), base.chargeRate(), base.primedChargeRate(), base.holdChance(), base.secondHold(),
                base.overholdChance(), base.partialWindow(), tolerance, base.feThrift(), base.feRecovery(),
                base.glyphSalvage(), base.strainMercy(), coils);
    }

    private static void costsFollowTheFormula() {
        near(PhaseArrayRules.pulseCost(2_000_000L, 8, 0), 3_000_000, "a fresh Stage 8 Resonate costs base times the stage factor");
        near(PhaseArrayRules.pulseCost(2_000_000L, 8, 42), Math.round(3_000_000 * Math.pow(1.5, 12.0)), "Quality 42 is twelve steps up the curve");
        near(PhaseArrayRules.pulseCost(2_000_000L, 2, 12), Math.round(200_000 * Math.pow(1.5, 12 / 3.5)), "a Stage 2 item pays a tenth");
        check(PhaseArrayRules.stageFactor(7) == 1.0 && PhaseArrayRules.stageFactor(5) == 0.5 && PhaseArrayRules.stageFactor(4) == 0.25,
                "stage factors");
    }

    private static void engineOperationsRespectTheirLimits() {
        ModifierEligibilityProfile coil = ModifierEligibilityProfiles.PHASE_COIL;
        ModifierEligibilityProfile emitter = ModifierEligibilityProfiles.PHASE_EMITTER;
        RandomSource random = RandomSource.create(7L);
        MachineTraits traits = MachineTraitRoller.roll(coil, PhaseArrayRules.PART_STAGE, random);
        for (int attempt = 0; attempt < 50 && traits.rarity() != Rarity.RARE; attempt++) {
            traits = MachineTraitRoller.roll(coil, PhaseArrayRules.PART_STAGE, random);
        }
        check(traits.rarity() == Rarity.RARE, "a Rare Coil rolls within fifty tries");
        int affixes = (int) traits.modifiers().stream().filter(modifier -> modifier.slot().isAffix()).count();
        check(affixes >= 2, "a Rare Coil has affixes to work on");

        MachineTraits raised = RefinementEngine.phaseRaiseTier(coil, traits, 0, 1, MachineTraitRoller.maxUpgradeTier(), random);
        check(raised == null || raised.modifiers().get(0).tier() <= MachineTraitRoller.maxUpgradeTier(), "Resonate stops at the upgrade cap");
        check(raised == null || raised.refinementPotential() == traits.refinementPotential(), "the engine never spends RP for the Array");
        MachineTraits removed = RefinementEngine.phaseRemove(coil, traits, 0, random);
        check(removed != null && removed.modifiers().size() == traits.modifiers().size() - 1, "Excise removes one affix");
        MachineTraits fractured = RefinementEngine.phaseFracture(coil, traits, random);
        check(fractured != null && RefinementEngine.phaseQuality(fractured) < RefinementEngine.phaseQuality(traits),
                "Fracture lowers Quality");

        List<ModifierDefinition> candidates = RefinementEngine.phaseImprintCandidates(emitter, MachineTraits.EMPTY, ModifierSlot.PREFIX);
        check(candidates.size() == 6, "a blank Emitter can take any of its six prefixes, found " + candidates.size());
        MachineTraits imprinted = RefinementEngine.phaseAdd(emitter, MachineTraits.EMPTY, ModifierSlot.PREFIX, candidates.get(0),
                PhaseArrayRules.PART_STAGE, false, false, random);
        check(imprinted != null && imprinted.rarity() == Rarity.MAGIC && imprinted.modifiers().size() == 1, "Imprint adds the chosen affix");
        check(RefinementEngine.phaseImprintCandidates(emitter, imprinted, ModifierSlot.PREFIX).isEmpty(), "a Magic item has one prefix slot");

        ModifierDefinition thrift = coil.definitions().stream().filter(definition -> definition.id().equals("phase_thrift")).findFirst().orElseThrow();
        MachineModifier donor = thrift.roll(3, random);
        MachineTraits grafted = RefinementEngine.phaseGraft(emitter, MachineTraits.EMPTY, donor, ModifierSlot.SUFFIX, false, random);
        check(grafted != null && grafted.modifiers().get(0).tier() == 3 && grafted.modifiers().get(0).value() == donor.value(),
                "Graft keeps the donor's tier and value");
        MachineTraits graftedDown = RefinementEngine.phaseGraft(emitter, MachineTraits.EMPTY, donor, ModifierSlot.SUFFIX, true, random);
        check(graftedDown != null && graftedDown.modifiers().get(0).tier() == 2, "a Partial Graft arrives one tier lower");
        check(RefinementEngine.phaseGraft(emitter, MachineTraits.EMPTY, donor, ModifierSlot.PREFIX, false, random) == null,
                "a suffix cannot be grafted onto a prefix slot");
    }

    /** RP goes only where the item changed, and a Graft donor survives only a keep-donor Overhold. */
    private static void outcomeRulesFollowThePrd() {
        for (PhasePulse.Outcome outcome : PhasePulse.Outcome.values()) {
            boolean spends = outcome == PhasePulse.Outcome.SUCCESS || outcome == PhasePulse.Outcome.OVERHOLD
                    || outcome == PhasePulse.Outcome.PARTIAL;
            check(PhasePulse.spendsRefinementPotential(outcome) == spends, "RP is spent on " + outcome + " only if the item changed");
        }
        check(PhasePulse.graftConsumesDonor(PhasePulse.Outcome.SUCCESS, PhaseOutcome.Overhold.KEEP_DONOR), "Graft destroys the donor on Success");
        check(!PhasePulse.graftConsumesDonor(PhasePulse.Outcome.OVERHOLD, PhaseOutcome.Overhold.KEEP_DONOR), "a keep-donor Overhold spares it");
        check(PhasePulse.graftConsumesDonor(PhasePulse.Outcome.OVERHOLD, PhaseOutcome.Overhold.NONE), "any other Overhold still destroys it");
        check(PhasePulse.graftConsumesDonor(PhasePulse.Outcome.PARTIAL, PhaseOutcome.Overhold.KEEP_DONOR), "a Partial Graft destroys the donor");
    }

    /** 0% within Tolerance, then 1% per point. */
    private static void fractureChanceFollowsStrain() {
        near(PhasePulse.fractureChance(line(3, 0), 0), 0.0, "no Fracture at 0 Strain");
        near(PhasePulse.fractureChance(line(3, 0), 10), 0.10, "10 Strain fractures 10% of the time");
        near(PhasePulse.fractureChance(line(3, 5), 5), 0.0, "Strain within Tolerance adds nothing");
        near(PhasePulse.fractureChance(line(3, 5), 7), 0.02, "two points past Tolerance add 2%");
        near(PhasePulse.fractureChance(line(3, 0), 500), 1.0, "the chance caps at certain");
        PhasePulse.Odds odds = PhasePulse.odds(line(3, 0), 3, 25);
        near(odds.fracture(), 0.25, "the preview carries the Fracture chance");
        near(odds.success() + odds.overhold() + odds.partial() + odds.miss() + odds.fracture(), 1.0, "the preview sums to one");
    }

    /** The frames the Cradle and the screen read from a rolled pulse: engage, slip, unwind, hold, apply. */
    private static void sequenceFramesFollowThePulse() {
        PhasePulse.Result slipped = new PhasePulse.Result(PhasePulse.Outcome.PARTIAL, 3, 4);
        check(PhasePulseSequence.engagedCoils(slipped) == 4, "the slipped Coil engages too");
        int engaging = 4 * PhasePulseSequence.ENGAGE_TICKS;
        check(PhasePulseSequence.totalTicks(slipped) == engaging + PhasePulseSequence.SLIP_TICKS + 3 * PhasePulseSequence.UNWIND_TICKS,
                "a slip engages, flashes, then unwinds every held Coil");
        PhasePulseSequence.Frame first = PhasePulseSequence.frame(slipped, 0);
        check(first.stage() == PhasePulseSequence.Stage.ENGAGING && first.lit() == 1, "the first Coil lights on Fire");
        check(PhasePulseSequence.engagingOn(slipped, 0) == 1 && PhasePulseSequence.engagingOn(slipped, PhasePulseSequence.ENGAGE_TICKS) == 2
                && PhasePulseSequence.engagingOn(slipped, 1) == 0, "Coils engage on the beat");
        PhasePulseSequence.Frame flash = PhasePulseSequence.frame(slipped, engaging);
        check(flash.stage() == PhasePulseSequence.Stage.SLIPPED && flash.flashing() == 4 && flash.lit() == 3, "the slipped Coil flashes with the held ones lit");
        PhasePulseSequence.Frame unwind = PhasePulseSequence.frame(slipped, engaging + PhasePulseSequence.SLIP_TICKS + PhasePulseSequence.UNWIND_TICKS);
        check(unwind.stage() == PhasePulseSequence.Stage.UNWINDING && unwind.lit() == 1, "lit Coils go dark one by one toward the Emitter");
        check(PhasePulseSequence.frame(slipped, PhasePulseSequence.totalTicks(slipped)).done(), "the outcome applies on the last tick");
        int previousLit = 0;
        boolean falling = false;
        for (int tick = 0; tick < PhasePulseSequence.totalTicks(slipped); tick++) {
            int lit = PhasePulseSequence.frame(slipped, tick).lit();
            if (lit < previousLit) {
                falling = true;
            }
            check(!falling || lit <= previousLit, "once Coils unwind they never relight");
            previousLit = lit;
        }

        PhasePulse.Result held = new PhasePulse.Result(PhasePulse.Outcome.SUCCESS, 5, 0);
        check(PhasePulseSequence.totalTicks(held) == 5 * PhasePulseSequence.ENGAGE_TICKS + PhasePulseSequence.HOLD_TICKS, "a full hold engages then holds");
        PhasePulseSequence.Frame holding = PhasePulseSequence.frame(held, 5 * PhasePulseSequence.ENGAGE_TICKS + 1);
        check(holding.stage() == PhasePulseSequence.Stage.HOLDING && holding.lit() == 5 && holding.flashing() == 0, "every Coil stays lit on a hold");

        PhasePulse.Result fracture = new PhasePulse.Result(PhasePulse.Outcome.FRACTURE, 0, 0);
        check(PhasePulseSequence.totalTicks(fracture) == PhasePulseSequence.FRACTURE_TICKS, "a Fracture shows without engaging");
        check(PhasePulseSequence.frame(fracture, 1).stage() == PhasePulseSequence.Stage.FRACTURED && PhasePulseSequence.engagingOn(fracture, 0) == 0,
                "no Coil engages on a Fracture");
        check(PhasePulseSequence.engagePitch(9) > PhasePulseSequence.engagePitch(1), "the tone climbs along the line");
    }

    private static void dormantSitesPayByTier() {
        check(GlyphSources.dormantTier(RNGTech.id("collapsed_workshop"), 8) == 1, "a Collapsed Workshop pays Tier I");
        check(GlyphSources.dormantTier(RNGTech.id("flooded_foundry"), 40) == 2, "a Flooded Foundry pays Tier II");
        check(GlyphSources.dormantTier(RNGTech.id("nether_smeltworks"), 50) == 3, "a Nether Smeltworks pays Tier III");
        check(GlyphSources.dormantTier(RNGTech.id("end_relay"), 60) == 4, "an End Relay pays Tier IV, never Tier V");
        check(GlyphSources.dormantTier(RNGTech.id("some_pack_site"), 70) == 4 && GlyphSources.dormantTier(RNGTech.id("some_pack_site"), 20) == 1,
                "an unknown site pays by soul level");
    }

    private static void near(double actual, double expected, String what) {
        check(Math.abs(actual - expected) < 1.0E-6, what + ": expected " + expected + ", got " + actual);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }
}
