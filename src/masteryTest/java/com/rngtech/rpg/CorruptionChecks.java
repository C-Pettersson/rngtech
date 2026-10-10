package com.rngtech.rpg;

import com.rngtech.content.blockentity.JamDebris;
import com.rngtech.content.blockentity.MachineInfoSnapshot;
import com.rngtech.content.calibration.CalibrationFamily;
import com.rngtech.content.calibration.CalibrationState;
import com.rngtech.content.machine.CrushHeadMaterial;
import com.rngtech.content.recipe.CalibrationRequirement;
import com.rngtech.content.recipe.MalformedIngotIngredient;
import com.rngtech.rpg.corruption.CorruptionCatalog;
import com.rngtech.rpg.corruption.CorruptionPreview;
import com.rngtech.rpg.refinement.RefinementEngine;
import com.rngtech.rpg.refinement.RefinementModifier;
import com.rngtech.rpg.refinement.RefinementOperation;
import com.rngtech.rpg.refinement.RefinementResult;
import com.rngtech.rpg.refinement.RefinementSelection;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.RandomSource;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/** Volatile Catalyst storage, outcomes, pools, station rejection and the catalyst's challenge ingredients. */
public final class CorruptionChecks {
    private static final int SAMPLES = 4000;
    private static final double TOLERANCE = 3.0;
    private static final int STAGE = 5;
    private static int checks;

    private CorruptionChecks() {
    }

    public static int run() {
        checks = 0;
        codecsRoundTrip();
        normalizersKeepCorruption();
        corruptionAppliesAfterAffixes();
        outcomeWeightsMatchTheTable();
        implicitsUseTheirFixedValues();
        corruptedTargetsRejectEveryOperation();
        uniquesAcceptOnlyTheCatalyst();
        reforgedKeepsWhatItShould();
        warpedScalesAffixesWithinTheRange();
        corruptedTargetsHideRefinementPotential();
        defaultPoolsLoadAndCoverEveryHost();
        poolValidationRejectsForbiddenEntries();
        calibrationStabilityCeiling();
        malformedIngotStage();
        jamDebrisNeedsLevelFive();
        return checks;
    }

    private static void codecsRoundTrip() {
        MachineTraits corrupted = corruptedRare();
        JsonElement json = MachineTraits.CODEC.encodeStart(JsonOps.INSTANCE, corrupted).getOrThrow();
        require(MachineTraits.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow().equals(corrupted), "codec round-trips a Corrupted target");

        MachineTraits plain = new MachineTraits(Rarity.MAGIC, 7, corrupted.modifiers());
        JsonElement plainJson = MachineTraits.CODEC.encodeStart(JsonOps.INSTANCE, plain).getOrThrow();
        require(!plainJson.getAsJsonObject().has("corruption"), "an uncorrupted target writes no corruption field");
        require(MachineTraits.CODEC.parse(JsonOps.INSTANCE, plainJson).getOrThrow().equals(plain), "codec round-trips without corruption");

        JsonElement legacy = JsonParser.parseString("{\"rarity\":\"rare\",\"refinement_potential\":4,\"modifiers\":[]}");
        MachineTraits loaded = MachineTraits.CODEC.parse(JsonOps.INSTANCE, legacy).getOrThrow();
        require(!loaded.isCorrupted() && loaded.refinementPotential() == 4, "existing saves load unchanged");

        for (MachineTraits traits : List.of(corrupted, plain, uniqueCorrupted())) {
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            MachineTraits.STREAM_CODEC.encode(buffer, traits);
            require(MachineTraits.STREAM_CODEC.decode(buffer).equals(traits), "stream codec round-trips " + traits.rarity());
        }
    }

    private static void normalizersKeepCorruption() {
        MachineTraits corrupted = corruptedRare();
        MachineTraits identity = new MachineTraits(Rarity.NORMAL, 0, List.of(
                new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.STABILITY, ModifierOperation.ADD, 1.0)
        ), List.of(MachineBehavior.OUTPUT_GUARD));
        MachineTraits effective = MachineTraits.withIdentity(MachineTraits.normalizedStored(corrupted), identity);
        require(effective.corruption().equals(corrupted.corruption()), "withIdentity keeps corruption");
        require(MachineTraits.normalizedStored(effective).corruption().equals(corrupted.corruption()), "normalizedStored keeps corruption");
        require(MachineTraits.normalizedStored(effective).modifiers().stream().noneMatch(modifier -> modifier.slot().isCorruption()),
                "stored traits keep the implicit only in the corruption field");
        MachineTraits twice = MachineTraits.withIdentity(MachineTraits.normalizedStored(effective), identity);
        require(twice.equals(effective), "effective traits are stable when normalized again");
        require(effective.hasBehavior(MachineBehavior.POWER_GRACE), "effective traits read corruption behaviors");
        require(!corrupted.isEmpty() && !new MachineTraits(Rarity.NORMAL, 0, List.of(), List.of(), MachineCorruption.of(CorruptionOutcome.UNTOUCHED)).isEmpty(),
                "a Corrupted target is never empty");

        MachineTraits uniqueIdentity = new MachineTraits(Rarity.UNIQUE, 0, List.of());
        MachineTraits unique = MachineTraits.withIdentity(new MachineTraits(Rarity.NORMAL, 9, List.of(), List.of(),
                corrupted.corruption()), uniqueIdentity);
        require(unique.rarity() == Rarity.UNIQUE && unique.refinementPotential() == 0 && unique.isCorrupted(),
                "withIdentity forces Unique and 0 RP but keeps corruption");

        RefinementResult upgraded = RefinementEngine.apply(
                crushHead(),
                new MachineTraits(Rarity.MAGIC, 30, corrupted.modifiers().subList(0, 1)),
                RefinementOperation.UPGRADE_RANDOM_MODIFIER,
                STAGE,
                RandomSource.create(3L)
        );
        RefinementResult corruptedAfter = RefinementEngine.apply(crushHead(), upgraded.traits(), RefinementOperation.CORRUPT, STAGE,
                RandomSource.create(4L));
        require(corruptedAfter.success() && corruptedAfter.traits().isCorrupted(), "CORRUPT keeps the corruption field through withModifiers");
    }

    private static void corruptionAppliesAfterAffixes() {
        MachineModifier speed = affix(crushHead(), ModifierSlot.PREFIX);
        MachineCorruption level = new MachineCorruption(CorruptionOutcome.BLESSED, List.of(
                new MachineModifier(ModifierSlot.CORRUPTION, MachineStat.PROCESSING_LEVEL, ModifierOperation.ADD, 1.0),
                new MachineModifier(ModifierSlot.CORRUPTION, MachineStat.JAM_RECOVERY, ModifierOperation.ADD, 25.0)
        ), List.of());
        MachineTraits plain = MachineTraits.withIdentity(new MachineTraits(Rarity.MAGIC, 3, List.of(speed)), MachineTraits.EMPTY);
        MachineTraits corrupted = MachineTraits.withIdentity(new MachineTraits(Rarity.MAGIC, 3, List.of(speed), List.of(), level),
                MachineTraits.EMPTY);
        require(corrupted.modifiers().getLast().slot().isCorruption() && corrupted.modifiers().getFirst().slot().isAffix(),
                "effective traits list corruption implicits after affixes");

        MachineStatAccumulator withoutCorruption = crusherStats();
        MachineStatAccumulator withCorruption = crusherStats();
        ComponentBaseStatCatalog.applyContribution(withoutCorruption, ComponentBaseStatCatalog.crushHead(CrushHeadMaterial.STEEL), plain);
        ComponentBaseStatCatalog.applyContribution(withCorruption, ComponentBaseStatCatalog.crushHead(CrushHeadMaterial.STEEL), corrupted);
        require(withCorruption.intValue(MachineStat.PROCESSING_LEVEL) == withoutCorruption.intValue(MachineStat.PROCESSING_LEVEL) + 1,
                "a Blessed Crush Head raises the Crusher's Processing Level by one");
        require(Math.abs(withCorruption.value(MachineStat.JAM_RECOVERY) - withoutCorruption.value(MachineStat.JAM_RECOVERY) - 25.0) < 0.001,
                "a part stat the Crush Head does not carry reaches the host");

        MachineTraits machine = new MachineTraits(Rarity.NORMAL, 0, List.of(), List.of(), new MachineCorruption(CorruptionOutcome.BLESSED,
                List.of(new MachineModifier(ModifierSlot.CORRUPTION, MachineStat.BATCH_SIZE, ModifierOperation.ADD, 1.0)), List.of()));
        MachineStatAccumulator placed = crusherStats();
        MachineStatAccumulator base = crusherStats();
        placed.apply(MachineTraits.withIdentity(machine, MachineTraits.EMPTY));
        require(placed.intValue(MachineStat.BATCH_SIZE) == base.intValue(MachineStat.BATCH_SIZE) + 1,
                "a placed machine's corruption implicit applies to its stats");
        require(machine.activeModifiers().size() == 1, "raw stored traits expose the implicit through activeModifiers");
    }

    private static void outcomeWeightsMatchTheTable() {
        CorruptionCatalog catalog = CorruptionCatalog.active();
        ModifierEligibilityProfile profile = crushHead();
        List<MachineTraits> targets = List.of(
                new MachineTraits(Rarity.NORMAL, 10, List.of()),
                new MachineTraits(Rarity.MAGIC, 10, List.of(affix(profile, ModifierSlot.PREFIX))),
                new MachineTraits(Rarity.RARE, 10, List.of(affix(profile, ModifierSlot.PREFIX), affix(profile, ModifierSlot.SUFFIX)))
        );
        for (MachineTraits target : targets) {
            for (boolean warded : List.of(false, true)) {
                sampleOutcomes(profile, target, warded, catalog);
            }
        }
        ModifierEligibilityProfile uniqueCell = ModifierEligibilityProfiles.forBatteryCell(true);
        for (boolean warded : List.of(false, true)) {
            sampleOutcomes(uniqueCell, new MachineTraits(Rarity.UNIQUE, 0, List.of()), warded, catalog);
        }
    }

    private static void sampleOutcomes(ModifierEligibilityProfile profile, MachineTraits target, boolean warded, CorruptionCatalog catalog) {
        RefinementModifier focus = warded ? RefinementModifier.CORRUPTION_WARD : RefinementModifier.NONE;
        Map<CorruptionOutcome, Integer> counts = new EnumMap<>(CorruptionOutcome.class);
        for (int seed = 0; seed < SAMPLES; seed++) {
            RefinementResult result = RefinementEngine.apply(profile, target, RefinementOperation.CORRUPT, STAGE, RefinementSelection.none(),
                    focus, Set.of(), RandomSource.create(seed));
            require(result.success() && result.traits().isCorrupted(), "CORRUPT always succeeds and corrupts " + target.rarity());
            require(result.consumedPotential() == 0 && result.traits().refinementPotential() == target.refinementPotential(),
                    "CORRUPT costs no RP");
            require(result.consumeCatalyst() && result.consumeModifier() == warded, "the catalyst, and a Stabilization Crystal, are consumed");
            counts.merge(result.traits().corruption().outcome(), 1, Integer::sum);
        }
        String label = target.rarity() + (warded ? " warded" : "");
        for (CorruptionPreview.Row row : CorruptionPreview.rows(catalog, profile.id(), target, warded)) {
            double observed = counts.getOrDefault(row.outcome(), 0) * 100.0 / SAMPLES;
            require(Math.abs(observed - row.percent()) <= TOLERANCE,
                    label + " " + row.outcome() + " lands near " + row.percent() + "%, saw " + observed + "%");
        }
        if (warded) {
            require(counts.getOrDefault(CorruptionOutcome.BLIGHTED, 0) == 0, label + " never rolls Blighted");
        }
    }

    private static void implicitsUseTheirFixedValues() {
        CorruptionCatalog catalog = CorruptionCatalog.active();
        for (String host : catalog.hosts()) {
            for (CorruptionOutcome outcome : List.of(CorruptionOutcome.BLESSED, CorruptionOutcome.BLIGHTED)) {
                for (CorruptionCatalog.Entry entry : catalog.entries(host, outcome)) {
                    MachineCorruption corruption = entry.toCorruption(outcome);
                    require(corruption.modifiers().size() == entry.effects().size(), host + " " + entry.id() + " keeps every effect");
                    for (int index = 0; index < entry.effects().size(); index++) {
                        MachineModifier modifier = corruption.modifiers().get(index);
                        MachineModifierEffect effect = entry.effects().get(index);
                        require(modifier.slot() == ModifierSlot.CORRUPTION && modifier.tier() == 0
                                        && modifier.value() == effect.value()
                                        && modifier.range().min() == modifier.range().max(),
                                host + " " + entry.id() + " is one fixed value with no tier");
                    }
                }
            }
        }
        for (int seed = 0; seed < 400; seed++) {
            RefinementResult result = RefinementEngine.apply(crushHead(), new MachineTraits(Rarity.NORMAL, 4, List.of()),
                    RefinementOperation.CORRUPT, STAGE, RandomSource.create(seed));
            MachineCorruption corruption = result.traits().corruption();
            if (!corruption.outcome().grantsImplicit()) {
                require(!corruption.hasImplicit(), "Untouched and Reforged grant no implicit");
                continue;
            }
            String id = corruption.modifiers().isEmpty() ? null : corruption.modifiers().getFirst().modGroup().substring("corruption:".length());
            CorruptionCatalog.Entry entry = catalog.entries("crush_head", corruption.outcome()).stream()
                    .filter(candidate -> candidate.id().equals(id))
                    .findFirst()
                    .orElse(null);
            require(entry != null && entry.toCorruption(corruption.outcome()).equals(corruption), "a rolled implicit equals its pool entry");
        }
    }

    private static void corruptedTargetsRejectEveryOperation() {
        ModifierEligibilityProfile profile = crushHead();
        MachineTraits corrupted = corruptedRare();
        for (RefinementOperation operation : RefinementOperation.values()) {
            RefinementResult result = RefinementEngine.apply(profile, corrupted, operation, STAGE, RefinementSelection.none(),
                    RefinementModifier.CONSERVE_CATALYST, Set.of(), RandomSource.create(1L));
            requireRejected(result, corrupted, "rngtech.refinement.failure.corrupted", operation.name());
        }
        RefinementSelection first = RefinementSelection.existingModifier(0);
        RandomSource random = RandomSource.create(2L);
        requireRejected(RefinementEngine.refineAllSameTier(profile, corrupted, 1, random), corrupted,
                "rngtech.refinement.failure.corrupted", "Exotic refine all");
        requireRejected(RefinementEngine.refineSelectedSameTier(profile, corrupted, first, 1, random), corrupted,
                "rngtech.refinement.failure.corrupted", "Exotic refine selected");
        requireRejected(RefinementEngine.upgradeRandomModifier(profile, corrupted, STAGE, random), corrupted,
                "rngtech.refinement.failure.corrupted", "Exotic random upgrade");
        requireRejected(RefinementEngine.upgradeSelectedModifier(profile, corrupted, STAGE, first, RefinementModifier.NONE, random), corrupted,
                "rngtech.refinement.failure.corrupted", "Exotic selected upgrade");
        requireRejected(RefinementEngine.addSelectedModifier(profile, corrupted, STAGE, RefinementSelection.emptySlot(ModifierSlot.SUFFIX), random),
                corrupted, "rngtech.refinement.failure.corrupted", "Exotic selected add");
        requireRejected(RefinementEngine.removeSelectedModifier(profile, corrupted, first, 1), corrupted,
                "rngtech.refinement.failure.corrupted", "Exotic selected remove");

        RefinementResult wardedUpgrade = RefinementEngine.apply(profile, new MachineTraits(Rarity.MAGIC, 30, corrupted.modifiers().subList(0, 1)),
                RefinementOperation.UPGRADE_RANDOM_MODIFIER, STAGE, RefinementSelection.none(), RefinementModifier.CORRUPTION_WARD, Set.of(),
                RandomSource.create(5L));
        require(!wardedUpgrade.success() && !wardedUpgrade.consumeModifier(),
                "the Stabilization Crystal is rejected without a Volatile Catalyst");
        RefinementResult conservedCorrupt = RefinementEngine.apply(profile, new MachineTraits(Rarity.MAGIC, 30, List.of()),
                RefinementOperation.CORRUPT, STAGE, RefinementSelection.none(), RefinementModifier.CONSERVE_CATALYST, Set.of(),
                RandomSource.create(5L));
        require(!conservedCorrupt.success(), "other crystals are rejected with a Volatile Catalyst");
        RefinementResult toolHead = RefinementEngine.apply(ModifierEligibilityProfiles.forToolHead(true), new MachineTraits(Rarity.MAGIC, 5,
                List.of()), RefinementOperation.CORRUPT, STAGE, RandomSource.create(6L));
        require(!toolHead.success() && "rngtech.refinement.failure.cannot_corrupt".equals(toolHead.messageKey()),
                "tool heads have no pool and cannot be corrupted");
    }

    private static void uniquesAcceptOnlyTheCatalyst() {
        ModifierEligibilityProfile profile = ModifierEligibilityProfiles.forBatteryCell(true);
        MachineTraits unique = new MachineTraits(Rarity.UNIQUE, 0, List.of());
        for (RefinementOperation operation : RefinementOperation.values()) {
            RefinementResult result = RefinementEngine.apply(profile, unique, operation, STAGE, RandomSource.create(1L));
            if (operation == RefinementOperation.CORRUPT) {
                require(result.success() && result.traits().rarity() == Rarity.UNIQUE, "a Unique accepts the Volatile Catalyst");
            } else {
                requireRejected(result, unique, "rngtech.refinement.failure.unique", "Unique " + operation);
            }
        }
        requireRejected(RefinementEngine.apply(profile, uniqueCorrupted(), RefinementOperation.CORRUPT, STAGE, RandomSource.create(1L)),
                uniqueCorrupted(), "rngtech.refinement.failure.corrupted", "a Corrupted Unique");
    }

    private static void reforgedKeepsWhatItShould() {
        ModifierEligibilityProfile profile = crushHead();
        MachineModifier implicit = new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.STABILITY, ModifierOperation.ADD, 2.0);
        MachineTraits rare = new MachineTraits(Rarity.RARE, 11, List.of(implicit, affix(profile, ModifierSlot.PREFIX),
                affix(profile, ModifierSlot.SUFFIX)), List.of(MachineBehavior.OUTPUT_GUARD));
        int reforged = 0;
        for (int seed = 0; seed < 300; seed++) {
            RefinementResult result = RefinementEngine.apply(profile, rare, RefinementOperation.CORRUPT, STAGE, RandomSource.create(seed));
            if (result.traits().corruption().outcome() != CorruptionOutcome.REFORGED) {
                continue;
            }
            reforged++;
            MachineTraits traits = result.traits();
            require(traits.rarity() == Rarity.RARE && traits.refinementPotential() == 11, "Reforged keeps rarity and RP");
            require(traits.modifiers().contains(implicit) && traits.behaviors().equals(rare.behaviors()),
                    "Reforged keeps fixed identity and behaviors");
            require(traits.modifiers().stream().anyMatch(modifier -> modifier.slot().isAffix()), "Reforged rolls new affixes");
        }
        require(reforged > 0, "Reforged happens on a Rare target");

        for (MachineTraits empty : List.of(new MachineTraits(Rarity.NORMAL, 3, List.of()), new MachineTraits(Rarity.UNIQUE, 0, List.of()))) {
            ModifierEligibilityProfile host = empty.rarity() == Rarity.UNIQUE ? ModifierEligibilityProfiles.forBatteryCell(true) : profile;
            for (int seed = 0; seed < 300; seed++) {
                RefinementResult result = RefinementEngine.apply(host, empty, RefinementOperation.CORRUPT, STAGE, RandomSource.create(seed));
                require(result.traits().corruption().outcome() != CorruptionOutcome.REFORGED,
                        "Reforged becomes Untouched on a " + empty.rarity() + " target with nothing to reroll");
            }
        }
    }

    private static void warpedScalesAffixesWithinTheRange() {
        ModifierEligibilityProfile profile = crushHead();
        MachineModifier implicit = new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.STABILITY, ModifierOperation.ADD, 2.0);
        MachineModifier speed = new MachineModifier("crush_head_speed", "processing_speed", ModifierSlot.PREFIX, MachineStat.PROCESSING_SPEED,
                ModifierOperation.INCREASED_PERCENT, 3, new ModifierValueRange(80.0, 100.0), 100.0, List.of());
        MachineModifier yield = new MachineModifier("output_amount", "", ModifierSlot.SUFFIX, MachineStat.OUTPUT_AMOUNT,
                ModifierOperation.INCREASED_PERCENT, 3, new ModifierValueRange(10.0, 20.0), 20.0, List.of());
        MachineModifier slower = new MachineModifier("", "less:processing_speed", ModifierSlot.SUFFIX, MachineStat.PROCESSING_SPEED,
                ModifierOperation.LESS, 2, new ModifierValueRange(0.8, 0.9), 0.8, List.of());
        MachineTraits rare = new MachineTraits(Rarity.RARE, 9, List.of(implicit, speed, yield, slower));
        int warpedCount = 0;
        double lowest = Double.MAX_VALUE;
        double highest = -Double.MAX_VALUE;
        for (int seed = 0; seed < 2000; seed++) {
            RefinementResult result = RefinementEngine.apply(profile, rare, RefinementOperation.CORRUPT, STAGE, RandomSource.create(seed));
            if (result.traits().corruption().outcome() != CorruptionOutcome.WARPED) {
                continue;
            }
            warpedCount++;
            MachineTraits traits = result.traits();
            require(traits.rarity() == Rarity.RARE && traits.refinementPotential() == 9 && !traits.corruption().hasImplicit(),
                    "Warped keeps rarity and RP and grants no implicit");
            require(traits.modifiers().get(0).equals(implicit), "Warped leaves fixed identity alone");
            require(traits.modifiers().get(2).equals(yield), "Warped leaves yield affixes alone");
            MachineModifier warpedSpeed = traits.modifiers().get(1);
            require(warpedSpeed.tier() == 3 && warpedSpeed.affixId().equals("crush_head_speed"), "Warped keeps the affix and its tier");
            require(warpedSpeed.value() == Math.rint(warpedSpeed.value()), "a warped percent lands on a whole percent");
            require(Math.abs(traits.modifiers().get(3).value() * 100.0 - Math.rint(traits.modifiers().get(3).value() * 100.0)) < 1e-6,
                    "a warped less multiplier lands on a whole percent step");
            require(warpedSpeed.value() >= 85.0 - 1e-9 && warpedSpeed.value() <= 115.0 + 1e-9
                            && warpedSpeed.effects().getFirst().value() == warpedSpeed.value(),
                    "100% increased warps to 85%-115%, saw " + warpedSpeed.value());
            double lessDelta = 1.0 - traits.modifiers().get(3).value();
            require(lessDelta >= 0.17 - 1e-9 && lessDelta <= 0.23 + 1e-9, "20% less warps to 17%-23% less, saw " + lessDelta);
            lowest = Math.min(lowest, warpedSpeed.value());
            highest = Math.max(highest, warpedSpeed.value());
        }
        require(warpedCount > 0, "Warped happens on a Rare target");
        require(highest > 110.0 && lowest < 90.0, "Warped reaches past the tier range in both directions");

        MachineTraits yieldOnly = new MachineTraits(Rarity.MAGIC, 4, List.of(yield));
        for (int seed = 0; seed < 300; seed++) {
            RefinementResult result = RefinementEngine.apply(profile, yieldOnly, RefinementOperation.CORRUPT, STAGE, RandomSource.create(seed));
            require(result.traits().corruption().outcome() != CorruptionOutcome.WARPED, "Warped becomes Untouched with only yield affixes");
        }
    }

    private static void corruptedTargetsHideRefinementPotential() {
        MachineInfoSnapshot snapshot = MachineInfoSnapshot.builder("crusher").refinement(corruptedRare()).build();
        require(snapshot.corrupted() && snapshot.refinementPotential() == 0, "Jade reports a Corrupted machine with no RP");
        MachineInfoSnapshot plain = MachineInfoSnapshot.builder("crusher").refinement(new MachineTraits(Rarity.RARE, 6, List.of())).build();
        require(!plain.corrupted() && plain.refinementPotential() == 6, "Jade still reports RP on an uncorrupted machine");
        MachineInfoSnapshot loaded = MachineInfoSnapshot.load(snapshot.save());
        require(loaded.corrupted() && loaded.refinementPotential() == 0, "the Jade snapshot syncs the Corrupted flag");
    }

    private static void defaultPoolsLoadAndCoverEveryHost() {
        CorruptionCatalog catalog = CorruptionCatalog.defaults();
        Map<CorruptionOutcome, Integer> weights = catalog.weights(false);
        for (CorruptionOutcome outcome : CorruptionOutcome.values()) {
            require(weights.get(outcome) == 20, "default " + outcome + " weight is 20");
        }
        require(catalog.warp().minPercent() == -15.0 && catalog.warp().maxPercent() == 15.0, "the default warp range is -15% to +15%");
        Map<CorruptionOutcome, Integer> warded = catalog.weights(true);
        require(warded.get(CorruptionOutcome.BLIGHTED) == 0 && warded.get(CorruptionOutcome.UNTOUCHED) == 40,
                "the Stabilization Crystal moves Blighted's weight to Untouched");

        Set<String> expected = new TreeSet<>();
        for (MachinePartType part : MachinePartType.values()) {
            expected.add(ModifierEligibilityProfiles.forMachinePart(part, MachineType.CRUSHER).id());
        }
        expected.add("battery_cell");
        expected.add("unique_battery_cell");
        for (MachineType type : MachineType.values()) {
            switch (type) {
                case BATTERY_CELL, FLUID_TANK, TOOL_HEAD, TOOL_ROD, MODULAR_TOOL, MINERS_COMPANION, FORESTRY_COMPANION -> {
                }
                default -> expected.add(ModifierEligibilityProfiles.forMachine(type).id());
            }
        }
        for (String host : expected) {
            require(!catalog.entries(host, CorruptionOutcome.BLESSED).isEmpty(), host + " has a Blessed pool");
            require(!catalog.entries(host, CorruptionOutcome.BLIGHTED).isEmpty(), host + " has a Blighted pool");
        }
        for (String host : List.of("tool_head", "pick_head", "tool_rod", "modular_tool", "miners_companion", "forestry_companion")) {
            require(!catalog.canCorrupt(host), host + " cannot be corrupted in 2.0");
        }
        require(poolFilesOnDisk().equals(new TreeSet<>(catalog.source().pools().keySet())), "pool_index.json lists every pool file");
    }

    private static void poolValidationRejectsForbiddenEntries() {
        String outcomes = "{\"weights\":{\"untouched\":25,\"blessed\":25,\"reforged\":25,\"blighted\":25}}";
        Map<String, String> cases = Map.of(
                "unread stat", entry("crush_head", "max_temperature", "increased_percent", 10),
                "yield stat", entry("crush_head", "output_amount", "increased_percent", 10),
                "declared yield stat", entry("crush_head", "at_level_output", "add", 10),
                "Refinement Potential", entry("control_board", "refinement_potential_bonus", "add", 1),
                "stage acceptance", entry("crusher", "upgrade_limit", "add", 1),
                "range", "{\"hosts\":[\"crush_head\"],\"blessed\":[{\"id\":\"x\",\"modifiers\":[{\"stat\":\"processing_speed\","
                        + "\"operation\":\"increased_percent\",\"value\":5,\"min\":1,\"max\":9}]}]}",
                "unknown host", entry("not_a_host", "processing_speed", "increased_percent", 10),
                "unread behavior", "{\"hosts\":[\"crush_head\"],\"blessed\":[{\"id\":\"x\",\"behaviors\":[\"auto_purge\"]}]}"
        );
        for (Map.Entry<String, String> testCase : cases.entrySet()) {
            boolean rejected;
            try {
                CorruptionCatalog.parse(new CorruptionCatalog.Source(outcomes, Map.of("test", testCase.getValue())));
                rejected = false;
            } catch (IllegalArgumentException expected) {
                rejected = true;
            }
            require(rejected, "pool validation rejects an entry with a " + testCase.getKey());
        }
        CorruptionCatalog accepted = CorruptionCatalog.parse(new CorruptionCatalog.Source(outcomes,
                Map.of("test", entry("crush_head", "processing_level", "add", 1))));
        require(accepted.canCorrupt("crush_head"), "pool validation accepts a stat the host reads");
    }

    private static void calibrationStabilityCeiling() {
        CalibrationRequirement band = CalibrationRequirement.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"family\":\"kinetic\",\"min_stage\":5,\"min_stability\":20,\"max_stability\":35}")).getOrThrow();
        require(band.accepts(new CalibrationState(CalibrationFamily.KINETIC, 5, 20, 0)), "the band accepts its floor");
        require(band.accepts(new CalibrationState(CalibrationFamily.KINETIC, 6, 35, 0)), "the band accepts its ceiling");
        require(!band.accepts(new CalibrationState(CalibrationFamily.KINETIC, 5, 36, 0)), "the band rejects parts above it");
        require(!band.accepts(new CalibrationState(CalibrationFamily.KINETIC, 4, 30, 0)), "the band keeps its stage floor");
        CalibrationRequirement old = CalibrationRequirement.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"family\":\"conductive\",\"min_stage\":5,\"min_stability\":70}")).getOrThrow();
        require(old.maxStability() == CalibrationRequirement.MAX_STABILITY && !old.hasStabilityCeiling()
                        && old.accepts(new CalibrationState(CalibrationFamily.CONDUCTIVE, 5, 100, 0)),
                "recipes without max_stability behave as before");
    }

    private static void malformedIngotStage() {
        require(MalformedIngotIngredient.meetsStage("sparksteel", 5) && MalformedIngotIngredient.meetsStage("nullite", 5),
                "Stage 5+ Malformed Ingots count");
        require(!MalformedIngotIngredient.meetsStage("steel", 5), "a Stage 4 Malformed Ingot does not count");
        require(!MalformedIngotIngredient.meetsStage("", 5) && !MalformedIngotIngredient.meetsStage(null, 5),
                "a Malformed Ingot with no material does not count");
    }

    private static void jamDebrisNeedsLevelFive() {
        require(JamDebris.dropsFrom(5) && JamDebris.dropsFrom(8), "jams on Processing Level 5+ recipes drop Jam Debris");
        require(!JamDebris.dropsFrom(4), "jams on lower recipes drop nothing");
    }

    private static String entry(String host, String stat, String operation, double value) {
        return "{\"hosts\":[\"" + host + "\"],\"blessed\":[{\"id\":\"x\",\"modifiers\":[{\"stat\":\"" + stat + "\",\"operation\":\""
                + operation + "\",\"value\":" + value + "}]}]}";
    }

    private static Set<String> poolFilesOnDisk() {
        try {
            Path index = Path.of(CorruptionChecks.class.getResource("/" + CorruptionCatalog.POOL_INDEX).toURI());
            try (Stream<Path> files = Files.list(index.resolveSibling("pools"))) {
                Set<String> names = new TreeSet<>();
                files.map(path -> path.getFileName().toString())
                        .filter(name -> name.endsWith(".json"))
                        .forEach(name -> names.add(name.substring(0, name.length() - ".json".length())));
                return names;
            }
        } catch (URISyntaxException | java.io.IOException exception) {
            throw new AssertionError("corruption pools are readable from the check classpath", exception);
        }
    }

    private static void requireRejected(RefinementResult result, MachineTraits traits, String messageKey, String label) {
        require(!result.success() && messageKey.equals(result.messageKey()), label + " is rejected with " + messageKey + ", got " + result.messageKey());
        require(result.traits().equals(traits) && !result.consumeCatalyst() && !result.consumeModifier(), label + " rejection consumes nothing");
    }

    private static MachineTraits corruptedRare() {
        ModifierEligibilityProfile profile = crushHead();
        return new MachineTraits(Rarity.RARE, 7, List.of(affix(profile, ModifierSlot.PREFIX), affix(profile, ModifierSlot.SUFFIX)), List.of(),
                new MachineCorruption(CorruptionOutcome.BLESSED, List.of(
                        new MachineModifier(ModifierSlot.CORRUPTION, MachineStat.PROCESSING_SPEED, ModifierOperation.INCREASED_PERCENT, 10.0)
                ), List.of(MachineBehavior.POWER_GRACE)));
    }

    private static MachineTraits uniqueCorrupted() {
        return new MachineTraits(Rarity.UNIQUE, 0, List.of(), List.of(), MachineCorruption.of(CorruptionOutcome.UNTOUCHED));
    }

    private static MachineModifier affix(ModifierEligibilityProfile profile, ModifierSlot slot) {
        List<ModifierDefinition> definitions = new ArrayList<>(profile.rollableDefinitions(slot));
        return definitions.getFirst().roll(1, RandomSource.create(slot.ordinal()));
    }

    private static MachineStatAccumulator crusherStats() {
        return MachineStatAccumulator.componentBase(Map.of(
                MachineStat.PROCESSING_LEVEL, 0.0,
                MachineStat.PROCESSING_SPEED, 1.0,
                MachineStat.BATCH_SIZE, 1.0,
                MachineStat.JAM_RECOVERY, 0.0
        ));
    }

    private static ModifierEligibilityProfile crushHead() {
        return ModifierEligibilityProfiles.forMachinePart(MachinePartType.CRUSH_HEAD, MachineType.CRUSHER);
    }

    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError(label);
        }
    }
}
