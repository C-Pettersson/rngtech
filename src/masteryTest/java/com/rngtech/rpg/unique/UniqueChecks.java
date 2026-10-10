package com.rngtech.rpg.unique;

import com.rngtech.rpg.ComponentBaseStatCatalog;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.ModifierEligibilityProfiles;
import com.rngtech.rpg.ModifierOperation;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.Rarity;
import com.rngtech.rpg.progression.AscendancyFormulas;
import com.rngtech.rpg.refinement.RefinementEngine;
import com.rngtech.rpg.refinement.RefinementOperation;
import com.rngtech.rpg.refinement.RefinementResult;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.RandomSource;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** Unique catalog, roll, and merge checks, run from {@code MasteryChecks}. */
public final class UniqueChecks {
    private static final String FORTRESS = "fortress_heater_element";
    private static final String PICK_JAW = "mineshaft_worn_pick_jaw";
    private static int checks;

    private UniqueChecks() {
    }

    public static int run() {
        catalogLoads();
        mutatedFixturesFail();
        rollsStayInRange();
        rollsAreSeeded();
        mergeMatchesProfilePlusRolls();
        ascendancyHooksNeedTheirAscendancy();
        storageKeepsRollsOnlyOnUniques();
        changedRangesClamp();
        refinementRejectsUniques();
        potatoCellUnchanged();
        slotStageDrivesGates();
        escapementSpeedsOnlyFirstCycles();
        echoStreakSurvivesOneDetour();
        packUniquesLoadFromAFolder();
        return checks;
    }

    private static void catalogLoads() {
        require(UniqueCatalog.all().size() == 9, "the catalog holds the Potato cell and eight launch Uniques");
        Set<UniqueHost> hosts = new HashSet<>();
        for (UniqueDefinition unique : UniqueCatalog.all()) {
            require(UniqueCatalog.violations(unique).isEmpty(), unique.id() + " follows the catalog rules");
            hosts.add(unique.host());
        }
        require(hosts.size() == UniqueHost.values().length, "the launch Uniques cover every supported host");
        for (UniqueStatReaders.Reader reader : List.of(UniqueStatReaders.Reader.CRUSHER, UniqueStatReaders.Reader.FURNACE,
                UniqueStatReaders.Reader.ALLOY_FURNACE, UniqueStatReaders.Reader.METAL_PRESS, UniqueStatReaders.Reader.MELTER,
                UniqueStatReaders.Reader.RESONANCE_CALIBRATOR, UniqueStatReaders.Reader.FORESTRY_CART, UniqueStatReaders.Reader.BATTERY_CHASSIS)) {
            require(UniqueCatalog.all().stream().anyMatch(unique -> readsSomething(reader, unique)),
                    "a launch Unique reaches " + reader);
        }
    }

    private static boolean readsSomething(UniqueStatReaders.Reader reader, UniqueDefinition unique) {
        return UniqueStatReaders.readers(unique.host()).contains(reader)
                && (unique.lines().stream().anyMatch(line -> UniqueStatReaders.reads(reader, line.stat()))
                || unique.behaviors().stream().anyMatch(behavior -> UniqueStatReaders.reads(reader, behavior)));
    }

    private static void mutatedFixturesFail() {
        rejects(raw -> raw.addProperty("host", "warp_drive"), "unknown host");
        rejects(raw -> raw.addProperty("slot_stage", 9), "outside heat_core stages");
        rejects(raw -> stat(raw, "rngtech:not_a_stat", "flat", 1, 2, "signature"), "unknown stat");
        rejects(raw -> stat(raw, "rngtech:calibration_quality", "more", 0.1, 0.2, "signature"), "is read by no heat_core host");
        rejects(raw -> stat(raw, "rngtech:jam_recovery", "flat", 5, 10, "signature"), "is read by no heat_core host");
        rejects(raw -> stat(raw, "rngtech:output_amount", "flat", 5, 10, "signature"), "is a yield stat");
        rejectsPick(raw -> stat(raw, "rngtech:output_amount", "increased", 0.05, 0.1, "signature"), "is a yield stat");
        rejectsPick(raw -> stat(raw, "rngtech:batch_size", "flat", 1, 2.5, "signature"), "needs whole-number bounds");
        rejects(raw -> stat(raw, "rngtech:energy_usage", "increased", 0.2, -0.1, "drawback"), "penalty at its best roll");
        rejects(raw -> stat(raw, "rngtech:warmup_time", "more", 0.1, -0.5, "signature"), "benefit at its worst roll");
        rejectsPick(raw -> stat(raw, "rngtech:processing_level", "flat", 1, 2, "signature"), "past the stage");
        rejects(raw -> raw.addProperty("base_profile", "rngtech:titanium_heat_core"), "max_temperature reaches");
        rejects(raw -> stat(raw, "rngtech:overdrive_margin", "flat", 10, 20, "hook"), "a hook names its ascendancy");
        rejects(raw -> stats(raw).getAsJsonObject("rngtech:overdrive_margin").addProperty("ascendancy", "rockbreaker"),
                "not read by rockbreaker");
        rejects(raw -> raw.addProperty("id", "rngtech:nameless_heater"), "missing language key item.rngtech.nameless_heater");
        rejects(raw -> raw.addProperty("id", "rngtech:nameless_heater"), "missing item model");
        rejects(raw -> raw.addProperty("id", "rngtech:nameless_heater"), "missing default loot table");
        require(UniqueCatalog.violations(fixture(FORTRESS)).isEmpty(), "the unmodified fixture passes");
    }

    private static void rollsStayInRange() {
        RandomSource random = RandomSource.create(42L);
        for (UniqueDefinition unique : UniqueCatalog.all()) {
            for (UniqueStatLine line : unique.lines()) {
                Set<Double> seen = new HashSet<>();
                for (int sample = 0; sample < 400; sample++) {
                    double rolled = line.roll(random);
                    require(rolled >= line.lower() - 1.0E-9 && rolled <= line.upper() + 1.0E-9,
                            unique.id() + " " + line.stat() + " rolls inside its range");
                    seen.add(rolled);
                }
                if (line.rollsWhole() && line.ranged()) {
                    require(seen.contains(line.lower()) && seen.contains(line.upper()), unique.id() + " " + line.stat() + " hits both integer ends");
                    require(seen.stream().allMatch(value -> value == Math.rint(value)), unique.id() + " " + line.stat() + " rolls whole numbers");
                }
            }
        }
    }

    private static void rollsAreSeeded() {
        UniqueDefinition fortress = UniqueCatalog.get(FORTRESS);
        MachineTraits first = fortress.roll(RandomSource.create(7L));
        MachineTraits second = fortress.roll(RandomSource.create(7L));
        require(first.equals(second), "a seed always rolls the same copy");
        require(first.rarity() == Rarity.UNIQUE && first.refinementPotential() == 0, "identified Uniques are Unique with no Refinement Potential");
        require(first.modifiers().size() == fortress.lines().stream().filter(UniqueStatLine::ranged).count(), "identification rolls every ranged line");
        require(first.modifiers().stream().allMatch(modifier -> modifier.slot() == ModifierSlot.UNIQUE), "rolls live in the Unique slot");
    }

    private static void mergeMatchesProfilePlusRolls() {
        UniqueDefinition fortress = UniqueCatalog.get(FORTRESS);
        List<MachineModifier> rolls = fortress.rollModifiers(List.of(-0.7, 30.0, 0.5, -0.5, -0.5));
        MachineStatAccumulator part = ComponentBaseStatCatalog.uniqueStats(fortress, rolls);
        near(part.baseValue(MachineStat.WARMUP_TIME), 0.3, "70% less Warmup Time on the part");
        near(part.baseValue(MachineStat.OVERHEAT_TOLERANCE), 0.5, "50% less Overheat Tolerance");
        near(part.baseValue(MachineStat.TEMPERATURE_STABILITY), 1.12, "Sparksteel Temperature Stability is untouched");
        near(part.baseValue(MachineStat.MAX_TEMPERATURE), 1000, "the fixed Sparksteel Maximum Temperature");
        near(part.baseValue(MachineStat.FUEL_EFFICIENCY), 1.25 * 0.5, "the fixed 50% less Fuel Efficiency");
        MachineStatAccumulator furnace = new MachineStatAccumulator();
        furnace.apply(new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.WARMUP_TIME, ModifierOperation.ADD, 1.0));
        furnace.apply(new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.MAX_TEMPERATURE, ModifierOperation.ADD,
                MachineStatAccumulator.FURNACE_BASE_MAX_TEMPERATURE));
        furnace.setAscendancy("crucible_keeper");
        ComponentBaseStatCatalog.applyUniqueContribution(furnace, fortress, rolls);
        near(furnace.value(MachineStat.WARMUP_TIME), 0.3, "the host's Warmup Time takes the part's multiplier");
        near(furnace.increasedPercent(MachineStat.ENERGY_USAGE), 50.0, "increased Energy Usage joins the host bucket");
        near(furnace.value(MachineStat.MAX_TEMPERATURE), MachineStatAccumulator.FURNACE_BASE_MAX_TEMPERATURE + 1000, "Maximum Temperature adds");

        UniqueDefinition pickJaw = UniqueCatalog.get(PICK_JAW);
        List<MachineModifier> pickRolls = pickJaw.rollModifiers(List.of(1.0, 2.0, 20.0, 5.0, 0.2));
        MachineStatAccumulator crusher = new MachineStatAccumulator();
        ComponentBaseStatCatalog.applyUniqueContribution(crusher, pickJaw, pickRolls);
        near(crusher.value(MachineStat.PROCESSING_LEVEL), 3, "the Pick-Jaw crushes one level above its Copper base");
        near(crusher.value(MachineStat.BATCH_SIZE), 2, "Batch Size adds to the host");
        near(crusher.increasedPercent(MachineStat.ENERGY_USAGE), 20, "increased Energy Use joins the Crusher's bucket");
        near(crusher.value(MachineStat.CYCLE_JAM_CHANCE), 5, "Jam Chance per Cycle adds to the host");
        near(crusher.value(MachineStat.OUTPUT_AMOUNT), 0, "the Pick-Jaw never touches Output Amount");

        UniqueDefinition board = UniqueCatalog.get("ancient_echo_control_board");
        MachineStatAccumulator calibrator = new MachineStatAccumulator();
        ComponentBaseStatCatalog.applyUniqueContribution(calibrator, board, board.rollModifiers(List.of(2.0, -0.2, -1.0)));
        near(calibrator.value(MachineStat.REFINEMENT_POTENTIAL_BONUS), 0.0, "the Echo board grants no Refinement Potential Bonus");

        UniqueDefinition pump = UniqueCatalog.get("witch_bottle_reflux_pump");
        MachineStatAccumulator melter = new MachineStatAccumulator();
        melter.apply(new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.FLUID_CAPACITY, ModifierOperation.ADD, 1000));
        ComponentBaseStatCatalog.applyUniqueContribution(melter, pump, pump.rollModifiers(List.of(1.0, -0.4)));
        near(melter.value(MachineStat.FLUID_CAPACITY), 2000, "100% increased host Fluid Capacity");
        near(melter.value(MachineStat.FLUID_TRANSFER), 125 * 0.6, "Osmium pump Fluid Transfer, then 40% less");
    }

    private static void ascendancyHooksNeedTheirAscendancy() {
        UniqueDefinition fortress = UniqueCatalog.get(FORTRESS);
        List<MachineModifier> rolls = fortress.rollModifiers(List.of(-0.7, 30.0, 0.5, -0.5, -0.5));
        MachineStatAccumulator without = new MachineStatAccumulator();
        ComponentBaseStatCatalog.applyUniqueContribution(without, fortress, rolls);
        near(without.value(MachineStat.OVERDRIVE_MARGIN), 0, "Overdrive Margin needs Crucible Keeper");
        MachineStatAccumulator other = new MachineStatAccumulator();
        other.setAscendancy("bloomer");
        ComponentBaseStatCatalog.applyUniqueContribution(other, fortress, rolls);
        near(other.value(MachineStat.OVERDRIVE_MARGIN), 0, "another ascendancy does not unlock the hook");
        MachineStatAccumulator keeper = new MachineStatAccumulator();
        keeper.setAscendancy("crucible_keeper");
        ComponentBaseStatCatalog.applyUniqueContribution(keeper, fortress, rolls);
        near(keeper.value(MachineStat.OVERDRIVE_MARGIN), 30, "Crucible Keeper reads the rolled Overdrive Margin");
        near(keeper.value(MachineStat.WARMUP_TIME), without.value(MachineStat.WARMUP_TIME), "non-hook lines ignore the ascendancy");
    }

    private static void storageKeepsRollsOnlyOnUniques() {
        MachineModifier roll = UniqueCatalog.get(FORTRESS).lines().getFirst().rollModifier(-0.7);
        MachineModifier affix = new MachineModifier(ModifierSlot.PREFIX, MachineStat.PROCESSING_SPEED, ModifierOperation.INCREASED_PERCENT, 10);
        MachineTraits uniqueIdentity = new MachineTraits(Rarity.UNIQUE, 0, List.of());
        MachineTraits stored = new MachineTraits(Rarity.UNIQUE, 0, List.of(roll));
        require(MachineTraits.normalizedStored(stored).modifiers().contains(roll), "normalizedStored keeps a Unique's rolls");
        require(MachineTraits.withIdentity(stored, uniqueIdentity).modifiers().contains(roll), "withIdentity keeps a Unique's rolls");
        MachineTraits normal = new MachineTraits(Rarity.RARE, 12, List.of(roll, affix));
        require(!MachineTraits.normalizedStored(normal).modifiers().contains(roll), "normalizedStored drops Unique rolls on other items");
        require(MachineTraits.normalizedStored(normal).modifiers().contains(affix), "normalizedStored still keeps affixes");
        MachineTraits normalIdentity = new MachineTraits(Rarity.NORMAL, 0, List.of(), List.of(com.rngtech.rpg.MachineBehavior.QUICK_FEED));
        require(!MachineTraits.withIdentity(normal, normalIdentity).modifiers().contains(roll), "withIdentity drops Unique rolls on other items");
        MachineTraits forced = MachineTraits.withIdentity(new MachineTraits(Rarity.RARE, 20, List.of(roll)), uniqueIdentity);
        require(forced.rarity() == Rarity.UNIQUE && forced.refinementPotential() == 0 && forced.modifiers().contains(roll),
                "withIdentity forces Unique rarity and 0 RP while keeping rolls");
    }

    private static void changedRangesClamp() {
        UniqueStatLine line = new UniqueStatLine(MachineStat.WARMUP_TIME, UniqueStatLine.Operation.MORE, -0.6, -0.8,
                UniqueStatLine.Role.SIGNATURE, "");
        UniqueDefinition narrowed = new UniqueDefinition("fixture", UniqueHost.HEAT_CORE, 4, "titanium", List.of(line), List.of(), "");
        MachineModifier outside = new MachineModifier("", "", ModifierSlot.UNIQUE, MachineStat.WARMUP_TIME, ModifierOperation.MORE, 0,
                new com.rngtech.rpg.ModifierValueRange(0.0, 1.0), 0.05, List.of());
        near(narrowed.value(line, List.of(outside)), -0.8, "a stored roll past the new range clamps to its best end");
        near(narrowed.value(line, List.of()), -0.7, "a line missing from an old copy uses its midpoint");
        MachineModifier removed = new MachineModifier("", "", ModifierSlot.UNIQUE, MachineStat.OVERDRIVE_MARGIN, ModifierOperation.ADD, 0,
                new com.rngtech.rpg.ModifierValueRange(0.0, 99.0), 50, List.of());
        near(narrowed.value(line, List.of(removed)), -0.7, "a removed line's roll is ignored");
        near(line.quality(-0.8), 1.0, "the best roll has full quality");
        near(line.quality(-0.6), 0.0, "the worst roll has no quality");
        UniqueStatLine drawback = new UniqueStatLine(MachineStat.ENERGY_USAGE, UniqueStatLine.Operation.INCREASED, 0.6, 0.4,
                UniqueStatLine.Role.DRAWBACK, "");
        near(drawback.quality(0.4), 1.0, "the smallest penalty is the best drawback roll");
        UniqueStatLine percentPoints = new UniqueStatLine(MachineStat.JAM_RECOVERY, UniqueStatLine.Operation.FLAT, 10, 30,
                UniqueStatLine.Role.HOOK, "rockbreaker");
        require(percentPoints.rollsWhole(), "flat lines with whole-number bounds roll whole numbers");
        near(percentPoints.clamp(22.29), 22, "an old fractional roll snaps to a whole number");
    }

    private static void refinementRejectsUniques() {
        MachineTraits traits = UniqueCatalog.get(FORTRESS).roll(RandomSource.create(3L));
        for (RefinementOperation operation : RefinementOperation.values()) {
            RefinementResult result = RefinementEngine.apply(
                    ModifierEligibilityProfiles.forUniquePart(UniqueHost.HEAT_CORE.partType()), traits, operation, RandomSource.create(1L));
            require(!result.success() && result.traits().equals(traits), operation + " rejects a Unique and changes nothing");
        }
        require(ModifierEligibilityProfiles.forUniquePart(UniqueHost.SERVO.partType()).definitions().isEmpty(), "Unique part profiles roll no affixes");
    }

    private static void potatoCellUnchanged() {
        UniqueDefinition potato = UniqueCatalog.get("unique_potato_battery_cell");
        MachineStatAccumulator cell = ComponentBaseStatCatalog.uniqueStats(potato, List.of());
        near(cell.baseValue(MachineStat.ENERGY_CAPACITY), 10000, "Potato capacity");
        near(cell.baseValue(MachineStat.ENERGY_TRANSFER), 128, "Potato transfer");
        near(cell.baseValue(MachineStat.EFFICIENCY), 0.60, "Potato efficiency");
        near(cell.baseValue(MachineStat.IDLE_LOSS), 6.0, "Potato idle loss");
        require(!potato.hasRangedLines(), "the Potato cell has nothing to identify");
        require(potato.roll(RandomSource.create(1L)).equals(new MachineTraits(Rarity.UNIQUE, 0, List.of())), "the Potato rolls the old stored traits");
    }

    private static void slotStageDrivesGates() {
        UniqueDefinition pickJaw = UniqueCatalog.get(PICK_JAW);
        require(pickJaw.slotStage() == 2, "the Pick-Jaw counts as Stage 2 for chassis acceptance");
        near(ComponentBaseStatCatalog.normalMaximum(UniqueHost.CRUSH_HEAD, MachineStat.PROCESSING_LEVEL, 3), 3,
                "Stage 3 heads reach Processing Level 3");
        require(UniqueCatalog.get(FORTRESS).slotStage() <= 6 && UniqueCatalog.get("igloo_basement_thermostat").slotStage() <= 6,
                "launch Heat Cores fit the Metal Press and Melter Stage 6 cap");
    }

    private static void escapementSpeedsOnlyFirstCycles() {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        stats.apply(new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.ESCAPEMENT_SPEED, ModifierOperation.ADD, 50));
        com.rngtech.rpg.EscapementState state = new com.rngtech.rpg.EscapementState();
        Object press = new Object();
        Object otherMold = new Object();
        state.observe(press);
        near(state.speed(true, stats), 1.5, "the first cycle runs 50% faster");
        near(state.speed(false, stats), 1.0, "without Escapement nothing changes");
        state.completed();
        state.observe(press);
        near(state.speed(true, stats), 1.0, "later cycles of the same recipe run at normal speed");
        state.observe(otherMold);
        near(state.speed(true, stats), 1.5, "a recipe or mold change primes the next cycle");
        state.completed();
        state.idle();
        state.observe(otherMold);
        near(state.speed(true, stats), 1.5, "idling primes the next cycle");
        MachineStatAccumulator costs = new MachineStatAccumulator();
        int normal = costs.adjustedEnergyCostForProgress(1200, 100, 100);
        int fast = costs.adjustedEnergyCostForProgress(1200, 67, 67);
        require(normal == fast, "a faster cycle spends the same FE per craft: " + normal + " vs " + fast);
    }

    private static void echoStreakSurvivesOneDetour() {
        require(AscendancyFormulas.echoDetour(4, "alloy", "plate", false, false), "Echo Streak covers one calibration of another family");
        require(!AscendancyFormulas.echoDetour(4, "alloy", "alloy", false, false), "the streak's own family is no detour");
        require(!AscendancyFormulas.echoDetour(4, "alloy", "plate", true, false), "only one detour per streak");
        require(!AscendancyFormulas.echoDetour(0, "", "plate", false, false), "there is no streak to keep");
        require(!AscendancyFormulas.echoDetour(4, "alloy", "plate", false, true), "Pattern Memory's carry-over comes first");
    }

    private static void packUniquesLoadFromAFolder() {
        try {
            java.nio.file.Path folder = java.nio.file.Files.createTempDirectory("rngtech-pack-uniques");
            JsonObject valid = fixture(FORTRESS);
            valid.addProperty("id", "rngtech:mypack_ember_core");
            valid.addProperty("version", 1);
            write(folder, "a_valid.json", valid);
            JsonObject unknownStat = fixture(FORTRESS);
            unknownStat.addProperty("id", "mypack_bad_core");
            stat(unknownStat, "rngtech:not_a_stat", "flat", 1, 2, "signature");
            write(folder, "b_unknown_stat.json", unknownStat);
            JsonObject cell = fixture("bastion_coin_stack_capacitor");
            cell.addProperty("id", "mypack_pack_cell");
            write(folder, "c_cell.json", cell);
            write(folder, "d_taken.json", fixture(FORTRESS));
            JsonObject future = fixture(FORTRESS);
            future.addProperty("id", "mypack_future_core");
            future.addProperty("version", UniqueCatalog.FORMAT_VERSION + 1);
            write(folder, "e_future.json", future);
            java.nio.file.Files.writeString(folder.resolve("f_broken.json"), "{ not json");
            JsonObject foreign = fixture(FORTRESS);
            foreign.addProperty("id", "mypack:ember_core");
            write(folder, "g_foreign.json", foreign);
            java.nio.file.Files.writeString(folder.resolve("notes.txt"), "ignored");
            java.nio.file.Files.writeString(folder.resolve("h_guide_example.json"), GUIDE_EXAMPLE);
            Set<String> builtIn = new HashSet<>();
            UniqueCatalog.all().forEach(unique -> builtIn.add(unique.id()));
            UniqueCatalog.ExternalUniques result = UniqueCatalog.loadExternal(folder, builtIn);
            require(result.loaded().size() == 2 && result.loaded().getFirst().id().equals("mypack_ember_core"),
                    "a valid pack file loads under rngtech: " + result.loaded());
            require(result.loaded().get(1).id().equals("mypack_guide_core"), "the pack-maker guide's example loads");
            require(result.errors().size() == 6, "every broken pack file is skipped with an error: " + result.errors());
            require(result.errors().get(0).contains("unknown stat"), "an unknown stat skips the file");
            require(result.errors().get(1).contains("Battery Cells"), "pack Uniques cannot be Battery Cells");
            require(result.errors().get(2).contains("already a Unique"), "a pack file cannot reuse a built-in id");
            require(result.errors().get(3).contains("newer than this RNGTech reads"), "a newer format version is refused");
            require(result.errors().get(4).startsWith("f_broken.json"), "malformed JSON skips the file");
            require(result.errors().get(5).contains("register under rngtech"), "another namespace gets a clear error");
            require(UniqueCatalog.loadExternal(folder.resolve("missing"), builtIn).loaded().isEmpty(), "a missing folder adds nothing");
            require(UniqueCatalog.all().stream().noneMatch(unique -> UniqueCatalog.isExternal(unique.id())),
                    "domain checks see only the built-in catalog");
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    /** The example in extras/uniques/README.md, with its id changed; keep the two in step. */
    private static final String GUIDE_EXAMPLE = """
            {
                "version": 1,
                "id": "rngtech:mypack_guide_core",
                "host": "heat_core",
                "slot_stage": 3,
                "base_profile": "rngtech:bronze_heat_core",
                "stats": {
                    "rngtech:warmup_time": { "operation": "more", "min": -0.3, "max": -0.5, "role": "signature" },
                    "rngtech:overdrive_margin": { "operation": "flat", "min": 10, "max": 20, "role": "hook", "ascendancy": "crucible_keeper" },
                    "rngtech:energy_usage": { "operation": "increased", "min": 0.3, "max": 0.15, "role": "drawback" }
                },
                "behaviors": [],
                "source": "rngtech:unique_source.mypack_volcano"
            }
            """;

    private static void write(java.nio.file.Path folder, String name, JsonObject raw) throws java.io.IOException {
        java.nio.file.Files.writeString(folder.resolve(name), raw.toString());
    }

    private static void rejects(Consumer<JsonObject> mutation, String expected) {
        expectViolation(fixture(FORTRESS), mutation, expected);
    }

    private static void rejectsPick(Consumer<JsonObject> mutation, String expected) {
        expectViolation(fixture(PICK_JAW), mutation, expected);
    }

    private static void expectViolation(JsonObject raw, Consumer<JsonObject> mutation, String expected) {
        mutation.accept(raw);
        List<String> violations = UniqueCatalog.violations(raw);
        require(violations.stream().anyMatch(violation -> violation.contains(expected)), "fixture rejects with '" + expected + "': " + violations);
        boolean thrown = false;
        try {
            UniqueCatalog.parse(raw);
        } catch (IllegalStateException exception) {
            thrown = true;
        }
        require(thrown || expected.startsWith("missing"), "parse rejects '" + expected + "'");
    }

    private static JsonObject stats(JsonObject raw) {
        return raw.getAsJsonObject("stats");
    }

    private static void stat(JsonObject raw, String stat, String operation, double worst, double best, String role) {
        JsonObject line = new JsonObject();
        line.addProperty("operation", operation);
        line.addProperty("min", worst);
        line.addProperty("max", best);
        line.addProperty("role", role);
        stats(raw).add(stat, line);
    }

    private static JsonObject fixture(String id) {
        try (InputStreamReader reader = new InputStreamReader(
                UniqueChecks.class.getResourceAsStream("/data/rngtech/uniques/" + id + ".json"), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void near(double actual, double expected, String label) {
        require(Math.abs(actual - expected) < 1.0E-6, label + ": expected " + expected + ", got " + actual);
    }

    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError(label);
        }
    }
}
