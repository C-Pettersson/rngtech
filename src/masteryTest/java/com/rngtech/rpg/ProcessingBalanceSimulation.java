package com.rngtech.rpg;

import com.rngtech.RNGTechConfig;
import com.rngtech.content.cable.EnergyConnectorTier;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.machine.CrushHeadMaterial;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.machine.FurnaceChassisMaterial;
import com.rngtech.rpg.progression.AscendancyFormulas;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.MegaPassiveTree;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Deterministic expected-value model of the ore line (Crusher, then Furnace) per stage, Mastery level and archetype.
 *
 * <p>Stats are assembled the way {@code CrusherBlockEntity#effectiveStats} and {@code FurnaceBlockEntity#statsForLane}
 * assemble them: chassis base, rolled affixes, the Crush Head or Heat Core merge through {@link ComponentBaseStatCatalog},
 * then the shared Mastery tree and ascendancy through {@link MegaPassiveTree}. Ticks, FE and yield mirror the block entity
 * methods named on each method below; update them when those change. Setups and archetypes live in
 * {@code tools/balance/processing-setups.json}. Every affix is a perfect crafted roll: the highest tier an upgrade reaches,
 * at its maximum value.
 */
public final class ProcessingBalanceSimulation {
    private static final int OUTPUT_STACK = 64;
    /** {@code MachineTraitRoller.MAX_UPGRADE_TIER}: the highest tier refinement upgrades reach. */
    private static final int CRAFTED_TIER = 6;
    /** {@code FurnaceRecipe}: an unauthored safe maximum is 115% of the target. */
    private static final double SAFE_MAXIMUM_SCALE = 1.15;

    private final Path recipeRoot;
    private final JsonObject setups;

    private ProcessingBalanceSimulation(Path projectDir) throws IOException {
        recipeRoot = projectDir.resolve("src/main/resources/data/rngtech/recipe");
        setups = read(projectDir.resolve("tools/balance/processing-setups.json"));
    }

    public static void main(String[] args) throws IOException {
        Path projectDir = Path.of(args.length > 0 ? args[0] : ".").toAbsolutePath().normalize();
        Path output = Path.of(args.length > 1 ? args[1] : "build/processing-balance/processing-balance.json");
        loadDefaultConfig();
        ProcessingBalanceSimulation simulation = new ProcessingBalanceSimulation(projectDir);
        Map<String, Object> report = simulation.run();
        Files.createDirectories(output.toAbsolutePath().getParent());
        Gson gson = new GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create();
        Files.writeString(output, gson.toJson(report), StandardCharsets.UTF_8);
        System.out.println("Processing balance report -> " + output);
    }

    /**
     * Loads RNGTechConfig defaults the way FML builds a fresh config file, so stat bases and recipe scaling read the real
     * values headless. {@code LoadedConfig} is FML-internal; with a corrected config, accepting it never saves.
     */
    private static void loadDefaultConfig() {
        CommentedConfig config = CommentedConfig.inMemory();
        RNGTechConfig.SPEC.correct(config);
        try {
            Constructor<?> constructor = Class.forName("net.neoforged.fml.config.LoadedConfig")
                    .getDeclaredConstructor(CommentedConfig.class, Path.class, ModConfig.class);
            constructor.setAccessible(true);
            RNGTechConfig.SPEC.acceptConfig((IConfigSpec.ILoadedConfig) constructor.newInstance(config, null, null));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot load RNGTech config defaults", exception);
        }
    }

    private Map<String, Object> run() throws IOException {
        List<Integer> levels = new ArrayList<>();
        setups.getAsJsonArray("levels").forEach(level -> levels.add(level.getAsInt()));
        Map<String, Object> rows = new LinkedHashMap<>();
        for (JsonElement stageElement : setups.getAsJsonArray("stages")) {
            JsonObject stage = stageElement.getAsJsonObject();
            Recipes recipes = recipes(stage.get("metal").getAsString());
            Map<String, Object> stageRow = new LinkedHashMap<>();
            stageRow.put("metal", stage.get("metal").getAsString());
            stageRow.put("generatorFePerTick", stage.get("generatorFePerTick").getAsDouble());
            stageRow.put("connectorFePerTick", EnergyConnectorTier.valueOf(upper(stage, "connector")).transferRate());
            Map<String, Object> builds = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> archetype : setups.getAsJsonObject("archetypes").entrySet()) {
                Map<String, Object> byLevel = new LinkedHashMap<>();
                for (int level : levels) {
                    byLevel.put(Integer.toString(level), line(stage, archetype.getValue().getAsJsonObject(), level, recipes));
                }
                builds.put(archetype.getKey(), byLevel);
            }
            stageRow.put("archetypes", builds);
            Map<String, Object> furnaceVariants = new LinkedHashMap<>();
            for (JsonElement variant : stage.has("furnaceVariants") ? stage.getAsJsonArray("furnaceVariants") : new JsonArray()) {
                JsonObject variantSetup = variant.getAsJsonObject();
                Map<String, Object> byArchetype = new LinkedHashMap<>();
                for (Map.Entry<String, JsonElement> archetype : setups.getAsJsonObject("archetypes").entrySet()) {
                    Map<String, Object> byLevel = new LinkedHashMap<>();
                    for (int level : levels) {
                        Furnace furnace = furnace(stage, variantSetup, archetype.getValue().getAsJsonObject().getAsJsonObject("furnace"), level);
                        byLevel.put(Integer.toString(level), furnace.smelt(recipes.smeltDust()).summary());
                    }
                    byArchetype.put(archetype.getKey(), byLevel);
                }
                furnaceVariants.put(variantSetup.get("id").getAsString(), byArchetype);
            }
            stageRow.put("furnaceVariants", furnaceVariants);
            rows.put("S" + stage.get("stage").getAsInt(), stageRow);
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("levels", levels);
        report.put("stages", rows);
        return report;
    }

    /** One archetype on one stage: the Crusher's ore and dust steps feeding the Furnace, for each smelting route. */
    private Map<String, Object> line(JsonObject stage, JsonObject archetype, int level, Recipes recipes) {
        Crusher crusher = crusher(stage, archetype.getAsJsonObject("crusher"), level);
        Furnace furnace = furnace(stage, stage.getAsJsonObject("furnace"), archetype.getAsJsonObject("furnace"), level);
        Step ore = crusher.crush(recipes.crushOre());
        Step dust = crusher.crush(recipes.crushDust());
        Step smeltDust = furnace.smelt(recipes.smeltDust());
        Step smeltCrushed = furnace.smelt(recipes.smeltCrushed());
        Step smeltOre = furnace.smelt(recipes.smeltOre());

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("crusher", crusher.describe());
        row.put("furnace", furnace.describe());
        row.put("crushOre", ore.summary());
        row.put("crushDust", dust.summary());
        row.put("smeltDust", smeltDust.summary());
        row.put("smeltCrushed", smeltCrushed.summary());
        row.put("smeltOre", smeltOre.summary());
        Map<String, Object> routes = new LinkedHashMap<>();
        routes.put("ore_crushed_dust_ingot", route(List.of(ore, dust, smeltDust)));
        routes.put("ore_crushed_ingot", route(List.of(ore, smeltCrushed)));
        routes.put("ore_ingot", route(List.of(smeltOre)));
        row.put("routes", routes);
        return row;
    }

    /** Chains steps: each step runs once per item the previous step produced. */
    private static Map<String, Object> route(List<Step> steps) {
        double runs = 1.0;
        double fe = 0.0;
        double crusherTicks = 0.0;
        double furnaceTicks = 0.0;
        double suppliedCrusherTicks = 0.0;
        double suppliedFurnaceTicks = 0.0;
        boolean blocked = false;
        for (Step step : steps) {
            blocked |= step.blocked();
            fe += runs * step.fePerInput();
            if (step.furnace()) {
                furnaceTicks += runs * step.ticksPerInput();
                suppliedFurnaceTicks += runs * step.suppliedTicksPerInput();
            } else {
                crusherTicks += runs * step.ticksPerInput();
                suppliedCrusherTicks += runs * step.suppliedTicksPerInput();
            }
            runs *= step.outputPerInput();
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("blocked", blocked);
        row.put("ingotsPerOre", runs);
        row.put("fePerIngot", fe / runs);
        row.put("fePerOre", fe);
        row.put("crusherTicksPerOre", crusherTicks);
        row.put("furnaceTicksPerOre", furnaceTicks);
        row.put("crusherTicksPerIngot", crusherTicks / runs);
        row.put("furnaceTicksPerIngot", furnaceTicks / runs);
        row.put("furnacesPerCrusher", crusherTicks <= 0 ? 0.0 : furnaceTicks / crusherTicks);
        row.put("suppliedCrusherTicksPerIngot", suppliedCrusherTicks / runs);
        row.put("suppliedFurnaceTicksPerIngot", suppliedFurnaceTicks / runs);
        return row;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Crusher
    // ---------------------------------------------------------------------------------------------------------------

    private Crusher crusher(JsonObject stage, JsonObject build, int level) {
        CrusherChassisMaterial chassis = CrusherChassisMaterial.valueOf(upper(stage, "crusherChassis"));
        CrushHeadMaterial head = CrushHeadMaterial.valueOf(upper(stage, "crushHead"));
        BatteryCellMaterial cell = BatteryCellMaterial.valueOf(upper(stage, "batteryCell"));
        MachineProgressionState state = progression(MachineMasteryFamily.CRUSHER, stage, build, level);

        // CrusherBlockEntity#effectiveStats
        MachineStatAccumulator stats = MachineBaseStatCatalog.crusher(chassis);
        stats.apply(traits(ModifierEligibilityProfiles.forMachine(MachineType.CRUSHER), build.getAsJsonArray("chassis")));
        ComponentBaseStatCatalog.applyContribution(stats, ComponentBaseStatCatalog.crushHead(head),
                traits(ModifierEligibilityProfiles.forMachinePart(MachinePartType.CRUSH_HEAD, MachineType.CRUSHER), build.getAsJsonArray("part")));
        MegaPassiveTree.applyStats(stats, state, MachineMasteryFamily.CRUSHER);
        boolean oath = MegaPassiveTree.has(state, "REFINERS_OATH");
        boolean battery = !MegaPassiveTree.has(state, "BLOCK_BATTERY");
        if (!battery) {
            double retained = Math.max(0, Math.min(100, stats.value(MachineStat.NO_BATTERY_OUTPUT_RETENTION))) / 100.0;
            stats.apply(new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.OUTPUT_AMOUNT, ModifierOperation.LESS,
                    RNGTechConfig.CRUSHER_NO_BATTERY_CELL_OUTPUT_MULTIPLIER.get() + (1 - RNGTechConfig.CRUSHER_NO_BATTERY_CELL_OUTPUT_MULTIPLIER.get()) * retained));
        }
        // CrusherBlockEntity#consumeWorkingEnergy: one tick draws from the internal buffer plus the cell's output rate.
        double supply = Math.min(stats.value(MachineStat.ENERGY_CAPACITY) + (battery ? cell.outputRate() : 0), connector(stage));
        return new Crusher(stats, state, oath, supply, build, level);
    }

    private record Crusher(MachineStatAccumulator stats, MachineProgressionState state, boolean oath, double feSupply,
            JsonObject build, int level) {
        boolean has(String behavior) {
            return MegaPassiveTree.has(state, behavior);
        }

        /**
         * CrusherBlockEntity#adjustedProcessingTicks, #energyCostPerCraft, #outputAmountFor, #process, #withSuperOutput.
         */
        Step crush(Recipe recipe) {
            if (!MegaPassiveTree.acceptsHardness(state, recipe.level())) {
                return Step.blocked(false, "hardness ceiling");
            }
            int deficit = Math.max(0, recipe.level() - stats.intValue(MachineStat.PROCESSING_LEVEL));
            int penalized = AscendancyFormulas.penalizedDeficit(deficit, stats);
            double under = AscendancyFormulas.underLevelPenaltyMultiplier(penalized, RNGTechConfig.CRUSHER_UNDER_LEVEL_PENALTY_MULTIPLIER_PER_LEVEL.get(), stats);
            boolean bonus = recipe.bonusOutput() && deficit == 0;

            // One soft-capped yield bucket; At-Level Output joins it before the cap.
            double outputAmount = 1.0;
            if (recipe.bonusOutput()) {
                double atLevel = recipe.level() == stats.intValue(MachineStat.PROCESSING_LEVEL) ? stats.value(MachineStat.AT_LEVEL_OUTPUT) : 0.0;
                outputAmount = stats.valueWithIncreased(MachineStat.OUTPUT_AMOUNT, atLevel);
                if (deficit > 0 && !has("FAULT_LINES")) {
                    outputAmount = Math.min(1.0, outputAmount);
                }
            }
            double scaled = Math.max(1.0, recipe.count() * Math.max(0, outputAmount));
            double superChance = bonus ? clampChance(stats.value(MachineStat.SUPER_OUTPUT_CHANCE)) : 0.0;
            int cadence = stats.intValue(MachineStat.SUPER_OUTPUT_CADENCE);
            double superRate = !bonus ? 0.0 : cadence > 0 ? 1.0 / cadence + (1.0 - 1.0 / cadence) * superChance : superChance;
            double compound = bonus && has("COMPOUND_YIELD") ? superChance * (scaled - Math.floor(scaled + 1e-9)) : 0.0;
            double salvage = !recipe.bonusOutput() ? 0.0
                    : deficit == 0 ? clampChance(stats.value(MachineStat.CRUSHER_SALVAGE_CHANCE))
                    : has("RUBBLE_RECLAIMER") ? clampChance(stats.value(MachineStat.CRUSHER_SALVAGE_CHANCE) * 0.5) : 0.0;
            double perCraft = scaled + recipe.count() * superRate + compound + salvage;

            // Jobs lock at cycle start to the largest batch whose banked output fits an empty 64 stack.
            int limit = BatchProcessing.batchSize(stats, oath);
            int jobs = BatchProcessing.largestFitting(limit, n -> Math.ceil(n * scaled - 1e-9) <= OUTPUT_STACK);
            jobs = Math.max(1, jobs);
            int single = stats.adjustedProcessingTicks(recipe.ticks());
            int cycle = (int) Math.max(1, Math.ceil(BatchProcessing.batchTicks(single, stats, jobs) * under));
            double instant = deficit == 0 ? clampChance(stats.value(MachineStat.INSTANT_PROCESS_CHANCE)) : 0.0;
            double expectedCycle = instant + (1 - instant) * cycle;

            double surcharge = recipe.level() >= RNGTechConfig.CRUSHER_HIGH_HARDNESS_ENERGY_THRESHOLD.get()
                    ? RNGTechConfig.CRUSHER_HIGH_HARDNESS_ENERGY_MULTIPLIER.get() : 1.0;
            double mitigation = Math.max(0, Math.min(100, stats.value(MachineStat.HIGH_HARDNESS_ENERGY_MITIGATION))) / 100.0;
            double mitigated = 1 + (surcharge - 1) * (1 - mitigation);
            double surchargeScale = has("SHATTER_POINT") ? 0.5 : 1.0;
            int baseEnergy = (int) Math.round(Math.max(1, recipe.energy()) * Math.max(0.01, 1 + (mitigated - 1) * surchargeScale));
            int perCraftFe = (int) Math.max(1, Math.ceil(stats.adjustedEnergyCost(baseEnergy) * under));
            double runningFePerTick = (double) perCraftFe * jobs / cycle;

            return new Step(false, false, "", recipe.id(), recipe.count(), perCraft, perCraftFe, expectedCycle / jobs,
                    runningFePerTick, feSupply, jobs, cycle, deficit,
                    Map.of("outputAmount", outputAmount, "superRate", superRate, "compound", compound, "salvage", salvage,
                            "instant", instant));
        }

        Map<String, Object> describe() {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("processingSpeed", stats.value(MachineStat.PROCESSING_SPEED));
            row.put("energyUsage", stats.value(MachineStat.ENERGY_USAGE));
            row.put("outputAmount", stats.value(MachineStat.OUTPUT_AMOUNT));
            row.put("yieldIncreasedPercent", stats.increasedPercent(MachineStat.OUTPUT_AMOUNT));
            row.put("yieldBonusPercent", stats.effectiveIncreasedPercent(MachineStat.OUTPUT_AMOUNT, 0.0));
            row.put("superOutputChance", stats.value(MachineStat.SUPER_OUTPUT_CHANCE));
            row.put("superOutputCadence", stats.intValue(MachineStat.SUPER_OUTPUT_CADENCE));
            row.put("salvageChance", stats.value(MachineStat.CRUSHER_SALVAGE_CHANCE));
            row.put("instantChance", stats.value(MachineStat.INSTANT_PROCESS_CHANCE));
            row.put("batchSize", BatchProcessing.batchSize(stats, oath));
            row.put("processingLevel", stats.intValue(MachineStat.PROCESSING_LEVEL));
            row.put("atLevelOutput", stats.value(MachineStat.AT_LEVEL_OUTPUT));
            row.put("feSupplyPerTick", feSupply);
            row.put("mastery", masterySummary(state));
            return row;
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Furnace
    // ---------------------------------------------------------------------------------------------------------------

    private Furnace furnace(JsonObject stage, JsonObject setup, JsonObject build, int level) {
        FurnaceChassisMaterial chassis = FurnaceChassisMaterial.valueOf(upper(setup, "chassis"));
        HeatCoreMaterial core = HeatCoreMaterial.valueOf(upper(setup, "heatCore"));
        BatteryCellMaterial cell = BatteryCellMaterial.valueOf(upper(stage, "batteryCell"));
        MachineProgressionState state = progression(MachineMasteryFamily.FURNACE, stage, build, level);

        // FurnaceBlockEntity#baseEffectiveStats then #statsForLane. Every lane gets the same core, so lanes share stats.
        MachineStatAccumulator stats = MachineBaseStatCatalog.furnace(chassis);
        JsonArray chassisAffixes = setup.has("chassisAffixes") ? setup.getAsJsonArray("chassisAffixes") : build.getAsJsonArray("chassis");
        stats.apply(traits(ModifierEligibilityProfiles.forMachine(chassis.machineType()), chassisAffixes));
        MegaPassiveTree.applyStats(stats, state, MachineMasteryFamily.FURNACE);
        int lanes = Math.max(1, Math.min(4, stats.intValue(MachineStat.INPUT_SLOTS)));
        JsonArray coreAffixes = setup.has("coreAffixes") ? setup.getAsJsonArray("coreAffixes") : build.getAsJsonArray("part");
        ComponentBaseStatCatalog.applyContribution(stats, ComponentBaseStatCatalog.heatCore(core),
                traits(ModifierEligibilityProfiles.forMachinePart(MachinePartType.HEAT_CORE, MachineType.SOLID_FUEL_BURNER), coreAffixes));
        int coresNeeded = chassis.heatCoreSlots() > 1 ? Math.min(lanes, chassis.heatCoreSlots()) : 1;
        // FurnaceBlockEntity#consumeWorkingEnergy runs per lane, so each lane can draw the cell's output rate.
        double supply = Math.min(stats.value(MachineStat.ENERGY_CAPACITY) + cell.outputRate() * lanes, connector(stage));
        return new Furnace(stats, state, lanes, coresNeeded, supply);
    }

    private record Furnace(MachineStatAccumulator stats, MachineProgressionState state, int lanes, int coresNeeded, double feSupply) {
        boolean has(String behavior) {
            return MegaPassiveTree.has(state, behavior);
        }

        /**
         * FurnaceBlockEntity#processingTicks, #energyCostPerCraft, #craft and the Bloom Ledger. Steady state: lanes stay at
         * temperature between crafts, so warm-up is not counted; Overdrive runs at its settled temperature.
         */
        Step smelt(Recipe recipe) {
            if (recipe == null) {
                return Step.blocked(true, "no recipe");
            }
            int maxTemperature = stats.intValue(MachineStat.MAX_TEMPERATURE);
            if (maxTemperature < recipe.temperature()) {
                return Step.blocked(true, "max temperature " + maxTemperature + " < " + recipe.temperature());
            }
            if (stats.value(MachineStat.TEMPERATURE_STABILITY) + 1e-9 < recipe.stability()) {
                return Step.blocked(true, "temperature stability");
            }
            int settled = maxTemperature;
            int margin = stats.intValue(MachineStat.OVERDRIVE_MARGIN);
            if (margin > 0 || has("SAFE_OVERDRIVE")) {
                settled = Math.min(settled, (int) Math.round(SAFE_MAXIMUM_SCALE * recipe.temperature()) - margin);
            }
            settled = Math.max(recipe.temperature(), settled);
            boolean overdrive = stats.value(MachineStat.OVERDRIVE_CAP) > 0 && stats.value(MachineStat.OVERDRIVE_SPEED) > 0;
            int temperature = overdrive ? settled : recipe.temperature();
            double overdriveMultiplier = AscendancyFormulas.overdriveSpeedMultiplier(temperature, recipe.temperature(), stats);
            int ticks = Math.max(1, (int) Math.ceil(stats.adjustedHeatProcessingTicks(recipe.ticks()) / overdriveMultiplier));
            double instant = clampChance(stats.value(MachineStat.INSTANT_PROCESS_CHANCE));
            double expectedTicks = instant + (1 - instant) * ticks;
            int crafts = has("CRUCIBLE_HEART") && temperature >= 2 * recipe.temperature() ? 2 : 1;

            int energy = (int) Math.round(Math.max(1, recipe.energy())
                    * (recipe.temperature() >= RNGTechConfig.FURNACE_HIGH_HEAT_ENERGY_THRESHOLD.get()
                    ? RNGTechConfig.FURNACE_HIGH_HEAT_ENERGY_MULTIPLIER.get() : 1.0));
            int perCraftFe = stats.adjustedEnergyCost(energy);
            double refund = has("CLOSED_LOOP_RECUPERATOR") ? Math.max(1, Math.ceil(0.05 * perCraftFe)) : 0.0;

            double superChance = recipe.bonusOutput() ? clampChance(stats.value(MachineStat.SUPER_OUTPUT_CHANCE)) : 0.0;
            double ledger = recipe.bonusOutput() && recipe.ledgerInput()
                    ? AscendancyFormulas.ledgerShare(stats, recipe.count(), recipe.crushedInput() && has("CRUSHER_LINE")) : 0.0;
            double perCraft = recipe.count() * (1 + superChance) + ledger;
            double runningFePerTick = (double) perCraftFe * crafts * lanes / ticks;

            return new Step(true, false, "", recipe.id(), recipe.count(), perCraft, perCraftFe - refund, expectedTicks / (crafts * lanes),
                    runningFePerTick, feSupply, lanes, ticks, 0,
                    Map.of("superChance", superChance, "ledger", ledger, "overdrive", overdriveMultiplier, "crafts", crafts,
                            "instant", instant));
        }

        Map<String, Object> describe() {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("processingSpeed", stats.value(MachineStat.PROCESSING_SPEED));
            row.put("heatTransfer", stats.value(MachineStat.HEAT_TRANSFER));
            row.put("energyUsage", stats.value(MachineStat.ENERGY_USAGE));
            row.put("maxTemperature", stats.value(MachineStat.MAX_TEMPERATURE));
            row.put("temperatureStability", stats.value(MachineStat.TEMPERATURE_STABILITY));
            row.put("superOutputChance", stats.value(MachineStat.SUPER_OUTPUT_CHANCE));
            row.put("ledgerRate", stats.value(MachineStat.LEDGER_RATE));
            row.put("lanes", lanes);
            row.put("heatCores", coresNeeded);
            row.put("feSupplyPerTick", feSupply);
            row.put("mastery", masterySummary(state));
            return row;
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Steps, traits and Mastery
    // ---------------------------------------------------------------------------------------------------------------

    /** One machine step per input item, in expectation. */
    private record Step(boolean furnace, boolean blocked, String reason, String recipe, int baseCount, double outputPerInput,
            double fePerInput, double ticksPerInput, double runningFePerTick, double feSupplyPerTick, int parallel,
            int cycleTicks, int hardnessDeficit, Map<String, Object> detail) {
        /** Ticks per input when FE arrives no faster than the machine can draw it; a short tick pauses work. */
        double suppliedTicksPerInput() {
            return feSupplyPerTick <= 0 ? ticksPerInput : Math.max(ticksPerInput, fePerInput / feSupplyPerTick);
        }

        static Step blocked(boolean furnace, String reason) {
            return new Step(furnace, true, reason, "", 0, 0, 0, 0, 0, 0, 0, 0, 0, Map.of());
        }

        Map<String, Object> summary() {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("recipe", recipe);
            if (blocked) {
                row.put("blocked", reason);
                return row;
            }
            row.put("outputPerInput", outputPerInput);
            row.put("fePerInput", fePerInput);
            row.put("fePerOutput", fePerInput / outputPerInput);
            row.put("ticksPerInput", ticksPerInput);
            row.put("outputPerSecond", 20.0 * outputPerInput / ticksPerInput);
            row.put("runningFePerTick", runningFePerTick);
            row.put("powerLimited", runningFePerTick > feSupplyPerTick);
            row.put("suppliedTicksPerInput", suppliedTicksPerInput());
            row.put("parallel", parallel);
            row.put("cycleTicks", cycleTicks);
            row.put("hardnessDeficit", hardnessDeficit);
            row.putAll(detail);
            return row;
        }
    }

    /** A Magic item with each listed affix at its highest crafted tier and maximum roll. */
    private static MachineTraits traits(ModifierEligibilityProfile profile, JsonArray affixes) {
        List<MachineModifier> modifiers = new ArrayList<>();
        for (JsonElement element : affixes) {
            String[] parts = element.getAsString().split(":", 2);
            ModifierSlot slot = ModifierSlot.valueOf(parts[0].toUpperCase(Locale.ROOT));
            ModifierDefinition definition = profile.definitions().stream()
                    .filter(candidate -> candidate.slot() == slot && candidate.id().equals(parts[1]) && candidate.canRoll())
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(profile.id() + " has no " + element.getAsString()));
            for (MachineModifier existing : modifiers) {
                if (definition.conflictsWith(existing)) {
                    throw new IllegalArgumentException(profile.id() + ": " + element.getAsString() + " conflicts with " + existing.affixId());
                }
            }
            modifiers.add(maxRoll(definition, Math.min(CRAFTED_TIER, definition.maxTier())));
        }
        return new MachineTraits(modifiers.isEmpty() ? Rarity.NORMAL : Rarity.MAGIC, 0, modifiers);
    }

    private static MachineModifier maxRoll(ModifierDefinition definition, int tier) {
        int stored = definition.isTiered() ? tier : 0;
        if (definition.effects().size() <= 1) {
            ModifierValueRange range = definition.rangeForTier(tier);
            return new MachineModifier(definition.id(), definition.modGroup(), definition.slot(), definition.stat(),
                    definition.operation(), stored, range, range.max(), List.of());
        }
        List<MachineModifierEffect> effects = definition.effects().stream()
                .map(effect -> new MachineModifierEffect(effect.stat(), effect.operation(), effect.rangeForTier(tier), effect.rangeForTier(tier).max()))
                .toList();
        return MachineModifier.roll(definition.id(), definition.modGroup(), definition.slot(), stored, effects);
    }

    /** The archetype's predefined build, truncated to the points a machine at {@code level} has. */
    private MachineProgressionState progression(MachineMasteryFamily family, JsonObject stage, JsonObject build, int level) {
        List<String> nodes = new ArrayList<>();
        JsonObject trees = setups.getAsJsonObject("trees").getAsJsonObject(build.get("tree").getAsString());
        JsonArray allocation = trees.getAsJsonArray(Integer.toString(level));
        if (allocation == null) {
            throw new IllegalArgumentException("Tree " + build.get("tree").getAsString() + " has no level " + level);
        }
        allocation.forEach(node -> nodes.add(node.getAsString()));
        if (nodes.size() > level - 1 || !MegaPassiveTree.validBuild(family.startNodeId(), nodes)) {
            throw new IllegalArgumentException("Invalid " + family + " tree " + build.get("tree").getAsString() + " at level " + level);
        }
        int seals = stage.get("sealTiers").getAsInt();
        String ascendancy = seals > 0 && build.has("ascendancy") ? build.get("ascendancy").getAsString() : "";
        List<String> ascendancyNodes = new ArrayList<>();
        if (!ascendancy.isEmpty()) {
            JsonArray order = build.getAsJsonArray("ascendancyNodes");
            for (int i = 0; i < Math.min(order.size(), seals * 2); i++) {
                ascendancyNodes.add(order.get(i).getAsString());
            }
        }
        MachineProgressionState state = new MachineProgressionState(MachineProgressionState.xpForLevel(level), 0, level, nodes,
                family.startNodeId(), List.of(), false, ascendancy, ascendancyNodes, seals);
        if (state.allocatedNodes().size() != nodes.size() || state.ascendancyNodes().size() != ascendancyNodes.size()) {
            throw new IllegalStateException("Mastery state rejected " + build.get("tree").getAsString() + " / " + ascendancy);
        }
        return state;
    }

    private static Map<String, Object> masterySummary(MachineProgressionState state) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("points", state.allocatedNodes().size());
        row.put("keystones", state.allocatedNodes().stream()
                .filter(id -> MegaPassiveTree.node(id).kind() == com.rngtech.rpg.progression.PassiveNodeKind.KEYSTONE).toList());
        row.put("ascendancy", state.ascendancy());
        row.put("ascendancyNodes", state.ascendancyNodes());
        return row;
    }

    /** The stage's Universal Connector tier caps FE per tick into one machine. */
    private static double connector(JsonObject stage) {
        return EnergyConnectorTier.valueOf(upper(stage, "connector")).transferRate();
    }

    private static double clampChance(double percent) {
        return Math.max(0.0, Math.min(1.0, percent / 100.0));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Recipes
    // ---------------------------------------------------------------------------------------------------------------

    private record Recipe(String id, int count, int ticks, int energy, int level, int temperature, double stability,
            boolean bonusOutput, boolean ledgerInput, boolean crushedInput) {
    }

    private record Recipes(Recipe crushOre, Recipe crushDust, Recipe smeltDust, Recipe smeltCrushed, Recipe smeltOre) {
    }

    private Recipes recipes(String metal) throws IOException {
        String furnaceOre = switch (metal) {
            case "iron", "copper" -> "vanilla/" + metal + "_ingot_from_smelting_" + metal + "_ore";
            default -> "metals/" + metal + "_from_ore";
        };
        return new Recipes(
                crusher(metal + "_ore"),
                crusher(metal + "_dust_from_crushed"),
                furnace("metals/" + metal + "_from_dust", false, false),
                furnace("metals/" + metal + "_from_crushed", true, true),
                furnace(furnaceOre, true, false));
    }

    /** CrusherRecipe codec defaults. */
    private Recipe crusher(String id) throws IOException {
        JsonObject json = read(recipeRoot.resolve("crusher/" + id + ".json"));
        int ticks = intOr(json, "processing_ticks", 120);
        int count = json.getAsJsonObject("result").has("count") ? json.getAsJsonObject("result").get("count").getAsInt() : 1;
        return new Recipe("crusher/" + id, count, ticks, intOr(json, "energy", ticks * 48), intOr(json, "required_processing_level", 1),
                0, 0, !json.has("bonus_output") || json.get("bonus_output").getAsBoolean(), false, false);
    }

    /** FurnaceRecipe codec defaults; {@code ledgerInput} mirrors the rngtech:bloom_ledger_inputs tag for ore, raw and crushed forms. */
    private Recipe furnace(String id, boolean ledgerInput, boolean crushedInput) throws IOException {
        Path path = recipeRoot.resolve("furnace/" + id + ".json");
        if (!Files.exists(path)) {
            return null;
        }
        JsonObject json = read(path);
        int ticks = intOr(json, "processing_ticks", 200);
        int count = json.getAsJsonObject("result").has("count") ? json.getAsJsonObject("result").get("count").getAsInt() : 1;
        int minimum = intOr(json, "minimum_temperature", 0);
        int target = intOr(json, "target_temperature", minimum);
        double stability = json.has("required_temperature_stability") ? json.get("required_temperature_stability").getAsDouble() : 0.0;
        return new Recipe("furnace/" + id, count, ticks, intOr(json, "energy", ticks * 24), 0, Math.max(minimum, target), stability,
                !json.has("bonus_output") || json.get("bonus_output").getAsBoolean(), ledgerInput, crushedInput);
    }

    private static int intOr(JsonObject json, String key, int fallback) {
        return json.has(key) ? json.get(key).getAsInt() : fallback;
    }

    private static String upper(JsonObject json, String key) {
        return json.get(key).getAsString().toUpperCase(Locale.ROOT);
    }

    private static JsonObject read(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
