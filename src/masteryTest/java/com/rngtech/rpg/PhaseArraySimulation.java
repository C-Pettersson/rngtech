package com.rngtech.rpg;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.RandomSource;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Seeded model of the Phase Array PRD ({@code docs/prds/phase-array.md}): the hold sequence, Strain and Fracture, FE by
 * Quality, and the RP ceiling, for a few player behaviors on a few fresh items. Fresh affix tiers and Refinement
 * Potential roll through {@link MachineTraitRoller} so the starting items match what the game crafts; everything else is
 * the PRD's draft rules, read from {@code tools/balance/phase-array-scenarios.json}. The Array does not exist in code yet,
 * so nothing here mirrors a block entity. When it does, point each rule at the method it follows.
 */
public final class PhaseArraySimulation {
    private static final int MAX_TIER = 7;
    private static final int RESONATE_CAP = MachineTraitRoller.maxUpgradeTier();
    private static final double TICKS_PER_MINUTE = 1200.0;
    private static final int[] QUALITY_LADDER = { 0, 6, 12, 18, 24, 30, 36, 42 };
    private static final int[] STRAIN_LADDER = { 0, 5, 10, 25 };
    private static final int MAX_PULSES = 400;
    /** A budget-minded player stops when one pulse would take longer than this at the reference supply. */
    private static final double BUDGET_MINUTES = 10.0;
    /** Every player reads the preview, so none fires once the Fracture chance reaches this. */
    private static final double STRAIN_STOP = 0.25;
    /** The tempering player Tempers once the Fracture chance reaches this. */
    private static final double TEMPER_AT = 0.10;
    /** The Reforge fisher wants a Rare at or above this Quality. */
    private static final int FISH_QUALITY = 12;

    private final JsonObject config;

    private PhaseArraySimulation(JsonObject config) {
        this.config = config;
    }

    public static void main(String[] args) throws IOException {
        Path projectDir = Path.of(args.length > 0 ? args[0] : ".").toAbsolutePath().normalize();
        Path output = Path.of(args.length > 1 ? args[1] : "build/phase-array/phase-array.json");
        ProcessingBalanceSimulation.loadDefaultConfig();
        JsonObject config = read(projectDir.resolve("tools/balance/phase-array-scenarios.json"));
        Map<String, Object> report = new PhaseArraySimulation(config).run();
        Files.createDirectories(output.toAbsolutePath().getParent());
        Gson gson = new GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create();
        Files.writeString(output, gson.toJson(report), StandardCharsets.UTF_8);
        System.out.println("Phase Array report -> " + output);
    }

    private static JsonObject read(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private Map<String, Object> run() {
        int trials = config.get("trials").getAsInt();
        List<Integer> lineSizes = new ArrayList<>();
        config.getAsJsonArray("line_sizes").forEach(size -> lineSizes.add(size.getAsInt()));
        JsonObject scenarios = config.getAsJsonObject("scenarios");
        JsonObject prd = scenarios.getAsJsonObject("prd");
        Map<String, Object> report = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : scenarios.entrySet()) {
            JsonObject merged = merge(prd, entry.getValue().getAsJsonObject());
            Rules rules = Rules.from(merged);
            Map<String, Object> scenarioReport = new LinkedHashMap<>();
            scenarioReport.put("label", merged.has("label") ? merged.get("label").getAsString() : entry.getKey());
            scenarioReport.put("resonateTimeByQuality", timeByQuality(rules));
            scenarioReport.put("ladder", ladder(rules, lineSizes));
            scenarioReport.put("behaviors", behaviors(rules, lineSizes, trials, entry.getKey()));
            report.put(entry.getKey(), scenarioReport);
        }
        return report;
    }

    private static JsonObject merge(JsonObject base, JsonObject override) {
        JsonObject result = base.deepCopy();
        for (Map.Entry<String, JsonElement> entry : override.entrySet()) {
            JsonElement current = result.get(entry.getKey());
            if (current != null && current.isJsonObject() && entry.getValue().isJsonObject()) {
                result.add(entry.getKey(), merge(current.getAsJsonObject(), entry.getValue().getAsJsonObject()));
            } else {
                result.add(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    // -- Rules -------------------------------------------------------------------------------------------------------

    private record Operation(String id, double baseFe, int coils, int rp, int glyph) {
    }

    private record Rules(
            double holdChance,
            double secondHold,
            double overholdPerSurplus,
            int partialWindow,
            double fracturePerStrain,
            int strainTolerance,
            double qualityBase,
            double qualityDivisor,
            Map<Integer, Double> stageFactors,
            double referenceSupply,
            Map<String, Operation> operations
    ) {
        static Rules from(JsonObject json) {
            Map<Integer, Double> stageFactors = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("stage_factors").entrySet()) {
                stageFactors.put(Integer.parseInt(entry.getKey()), entry.getValue().getAsDouble());
            }
            Map<String, Operation> operations = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("operations").entrySet()) {
                JsonObject op = entry.getValue().getAsJsonObject();
                operations.put(entry.getKey(), new Operation(
                        entry.getKey(),
                        op.get("base_fe").getAsDouble(),
                        op.get("coils").getAsInt(),
                        op.get("rp").getAsInt(),
                        op.get("glyph").getAsInt()
                ));
            }
            return new Rules(
                    json.get("hold_chance").getAsDouble(),
                    json.get("second_hold").getAsDouble(),
                    json.get("overhold_per_surplus").getAsDouble(),
                    json.get("partial_window").getAsInt(),
                    json.get("fracture_per_strain").getAsDouble(),
                    json.get("strain_tolerance").getAsInt(),
                    json.get("quality_base").getAsDouble(),
                    json.get("quality_divisor").getAsDouble(),
                    stageFactors,
                    json.get("reference_supply_fe_per_tick").getAsDouble(),
                    operations
            );
        }

        /** PRD, Costs: {@code FE = base x stage x qualityBase ^ (Quality / qualityDivisor)}. */
        double fe(Operation operation, int stage, int quality) {
            return operation.baseFe() * stageFactors.get(stage) * Math.pow(qualityBase, quality / qualityDivisor);
        }

        double minutes(double fe) {
            return fe / referenceSupply / TICKS_PER_MINUTE;
        }

        /** The chance one Coil holds, counting a Second Hold reroll on a slip. */
        double effectiveHold() {
            return holdChance + (1 - holdChance) * secondHold;
        }

        double fractureChance(int strain) {
            return Math.min(1.0, Math.max(0, strain - strainTolerance) * fracturePerStrain);
        }
    }

    // -- Items -------------------------------------------------------------------------------------------------------

    private static final class Item {
        final int stage;
        int[] tiers;
        int rp;
        int strain;

        Item(int stage, int[] tiers, int rp) {
            this.stage = stage;
            this.tiers = tiers;
            this.rp = rp;
        }

        int quality() {
            return Arrays.stream(tiers).sum();
        }

        int lowestBelow(int cap) {
            int index = -1;
            for (int i = 0; i < tiers.length; i++) {
                if (tiers[i] < cap && (index < 0 || tiers[i] < tiers[index])) {
                    index = i;
                }
            }
            return index;
        }

        boolean allAtLeast(int tier) {
            return tiers.length > 0 && Arrays.stream(tiers).allMatch(t -> t >= tier);
        }
    }

    private static int affixCount(Rarity rarity) {
        return switch (rarity) {
            case NORMAL -> 0;
            case MAGIC -> 2;
            case RARE -> 6;
            default -> 0;
        };
    }

    /** Mirrors {@code MachineTraitRoller#rollRarity} for a Reforge; the game's method is private. */
    private static Rarity rollRarity(int stage, RandomSource random) {
        int roll = random.nextInt(100);
        if (stage <= 0) {
            return roll < 35 ? Rarity.NORMAL : roll < 85 ? Rarity.MAGIC : Rarity.RARE;
        }
        if (stage <= 4) {
            return roll < 70 ? Rarity.MAGIC : Rarity.RARE;
        }
        return roll < 50 ? Rarity.NORMAL : roll < 90 ? Rarity.MAGIC : Rarity.RARE;
    }

    private static Item fresh(int stage, Rarity rarity, RandomSource random) {
        int[] tiers = new int[affixCount(rarity)];
        for (int i = 0; i < tiers.length; i++) {
            tiers[i] = MachineTraitRoller.rollModifierTier(rarity, stage, random);
        }
        return new Item(stage, tiers, MachineTraitRoller.refinementPotentialRange(stage).roll(random));
    }

    private static Item allAtTier(int stage, int affixes, int tier, RandomSource random) {
        int[] tiers = new int[affixes];
        Arrays.fill(tiers, tier);
        return new Item(stage, tiers, MachineTraitRoller.refinementPotentialRange(stage).roll(random));
    }

    // -- The hold sequence --------------------------------------------------------------------------------------------

    private enum Outcome { SUCCESS, OVERHOLD, PARTIAL, MISS, FRACTURE }

    /** PRD, The Hold Sequence: the Fracture check, then Coils in order until the first slip. */
    private static Outcome pulse(Rules rules, Operation operation, int lineSize, int strain, RandomSource random) {
        if (random.nextDouble() < rules.fractureChance(strain)) {
            return Outcome.FRACTURE;
        }
        int held = 0;
        for (int coil = 0; coil < lineSize; coil++) {
            boolean holds = random.nextDouble() < rules.holdChance()
                    || (rules.secondHold() > 0 && random.nextDouble() < rules.secondHold());
            if (!holds) {
                break;
            }
            held++;
        }
        int required = operation.coils();
        if (held == lineSize && lineSize > required) {
            for (int surplus = 0; surplus < lineSize - required; surplus++) {
                if (random.nextDouble() < rules.overholdPerSurplus()) {
                    return Outcome.OVERHOLD;
                }
            }
            return Outcome.SUCCESS;
        }
        if (held >= required) {
            return Outcome.SUCCESS;
        }
        if (held >= required - rules.partialWindow()) {
            return Outcome.PARTIAL;
        }
        return Outcome.MISS;
    }

    /** The same ladder in closed form, for the preview the Cradle shows. */
    private static Map<String, Double> ladderOdds(Rules rules, Operation operation, int lineSize, int strain) {
        double p = rules.effectiveHold();
        int required = operation.coils();
        double fracture = rules.fractureChance(strain);
        double success = 0;
        double overhold = 0;
        double partial = 0;
        for (int held = 0; held <= lineSize; held++) {
            double chance = Math.pow(p, held) * (held == lineSize ? 1 : 1 - p);
            if (held == lineSize && lineSize > required) {
                double anyOverhold = 1 - Math.pow(1 - rules.overholdPerSurplus(), lineSize - required);
                overhold += chance * anyOverhold;
                success += chance * (1 - anyOverhold);
            } else if (held >= required) {
                success += chance;
            } else if (held >= required - rules.partialWindow()) {
                partial += chance;
            }
        }
        double live = 1 - fracture;
        Map<String, Double> odds = new LinkedHashMap<>();
        odds.put("success", round(live * success));
        odds.put("overhold", round(live * overhold));
        odds.put("partial", round(live * partial));
        odds.put("miss", round(live * (1 - success - overhold - partial)));
        odds.put("fracture", round(fracture));
        return odds;
    }

    private Map<String, Object> ladder(Rules rules, List<Integer> lineSizes) {
        Map<String, Object> byOperation = new LinkedHashMap<>();
        for (Operation operation : rules.operations().values()) {
            Map<String, Object> bySize = new LinkedHashMap<>();
            for (int size : lineSizes) {
                if (size < operation.coils()) {
                    continue;
                }
                Map<String, Object> byStrain = new LinkedHashMap<>();
                for (int strain : STRAIN_LADDER) {
                    byStrain.put("strain" + strain, ladderOdds(rules, operation, size, strain));
                }
                bySize.put("coils" + size, byStrain);
            }
            byOperation.put(operation.id(), bySize);
        }
        return byOperation;
    }

    private Map<String, Object> timeByQuality(Rules rules) {
        Operation resonate = rules.operations().get("resonate");
        Map<String, Object> rows = new LinkedHashMap<>();
        for (int stage : new int[] { 2, 5, 8 }) {
            Map<String, Object> byQuality = new LinkedHashMap<>();
            for (int quality : QUALITY_LADDER) {
                double fe = rules.fe(resonate, stage, quality);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("fe", Math.round(fe));
                row.put("minutesAtReference", round(rules.minutes(fe)));
                byQuality.put("quality" + quality, row);
            }
            rows.put("stage" + stage, byQuality);
        }
        return rows;
    }

    // -- Behaviors ---------------------------------------------------------------------------------------------------

    private enum Behavior {
        /** Resonate the lowest affix until every affix is Tier 6, or RP runs out. */
        RESONATE_UNTIL_RP,
        /** As above, but also stop when one pulse would take more than ten minutes at the reference supply. */
        RESONATE_WITH_BUDGET,
        /** As RESONATE_UNTIL_RP, tempering whenever the Fracture chance reaches 10%, with Glyphs assumed on hand. */
        RESONATE_AND_TEMPER,
        /** Start from an all-Tier-6 item with fresh RP and Exalt each affix to Tier 7 until RP runs out. */
        EXALT_ALL,
        /** Reforge until the item is Rare with Quality at least 18, paying each Reforge at the current Quality. */
        REFORGE_FISH
    }

    private record Trial(
            int pulses,
            double fe,
            double minutes,
            int finalQuality,
            int fractures,
            int strain,
            int rpLeft,
            int glyphs,
            boolean reachedGoal,
            String stoppedBy
    ) {
    }

    private Map<String, Object> behaviors(Rules rules, List<Integer> lineSizes, int trials, String scenarioKey) {
        Map<String, Object> byBehavior = new LinkedHashMap<>();
        for (Behavior behavior : Behavior.values()) {
            Map<String, Object> byItem = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> itemEntry : config.getAsJsonObject("items").entrySet()) {
                JsonObject item = itemEntry.getValue().getAsJsonObject();
                int stage = item.get("stage").getAsInt();
                Rarity rarity = Rarity.valueOf(item.get("rarity").getAsString());
                if (behavior == Behavior.EXALT_ALL && stage < 8) {
                    continue;
                }
                Map<String, Object> bySize = new LinkedHashMap<>();
                for (int size : lineSizes) {
                    Operation main = rules.operations().get(behavior == Behavior.EXALT_ALL ? "exalt"
                            : behavior == Behavior.REFORGE_FISH ? "reforge" : "resonate");
                    if (size < main.coils()) {
                        continue;
                    }
                    List<Trial> results = new ArrayList<>();
                    long seed = (scenarioKey + behavior + itemEntry.getKey()).hashCode() * 131L + size;
                    RandomSource random = RandomSource.create(seed);
                    for (int trial = 0; trial < trials; trial++) {
                        results.add(runTrial(rules, behavior, stage, rarity, size, random));
                    }
                    bySize.put("coils" + size, summarize(results));
                }
                Map<String, Object> itemReport = new LinkedHashMap<>();
                itemReport.put("label", item.get("label").getAsString());
                itemReport.putAll(bySize);
                byItem.put(itemEntry.getKey(), itemReport);
            }
            byBehavior.put(behavior.name().toLowerCase(Locale.ROOT), byItem);
        }
        return byBehavior;
    }

    private Trial runTrial(Rules rules, Behavior behavior, int stage, Rarity rarity, int lineSize, RandomSource random) {
        Item item = behavior == Behavior.EXALT_ALL ? allAtTier(stage, affixCount(rarity), RESONATE_CAP, random)
                : fresh(stage, rarity, random);
        Operation resonate = rules.operations().get("resonate");
        Operation exalt = rules.operations().get("exalt");
        Operation temper = rules.operations().get("temper");
        Operation reforge = rules.operations().get("reforge");
        int pulses = 0;
        double fe = 0;
        int fractures = 0;
        int glyphs = 0;
        String stoppedBy = "limit";
        boolean reachedGoal = false;
        while (pulses < MAX_PULSES) {
            Operation operation;
            int target;
            if (behavior == Behavior.REFORGE_FISH) {
                if (item.tiers.length == 6 && item.quality() >= FISH_QUALITY) {
                    reachedGoal = true;
                    stoppedBy = "goal";
                    break;
                }
                operation = reforge;
                target = -1;
            } else if (behavior == Behavior.EXALT_ALL) {
                target = item.lowestBelow(MAX_TIER);
                if (target < 0) {
                    reachedGoal = item.tiers.length > 0;
                    stoppedBy = reachedGoal ? "goal" : "dead";
                    break;
                }
                operation = exalt;
            } else {
                target = item.lowestBelow(RESONATE_CAP);
                if (target < 0) {
                    reachedGoal = item.tiers.length > 0;
                    stoppedBy = reachedGoal ? "goal" : "dead";
                    break;
                }
                operation = resonate;
                if (behavior == Behavior.RESONATE_AND_TEMPER && rules.fractureChance(item.strain) >= TEMPER_AT
                        && lineSize >= temper.coils()) {
                    operation = temper;
                }
            }
            if (operation != temper && rules.fractureChance(item.strain) >= STRAIN_STOP) {
                stoppedBy = "strain";
                break;
            }
            if (item.rp < operation.rp()) {
                stoppedBy = "rp";
                break;
            }
            double cost = rules.fe(operation, item.stage, item.quality());
            if (behavior == Behavior.RESONATE_WITH_BUDGET && rules.minutes(cost) > BUDGET_MINUTES) {
                stoppedBy = "budget";
                break;
            }
            Outcome outcome = pulse(rules, operation, lineSize, item.strain, random);
            pulses++;
            fe += cost;
            glyphs += operation.glyph() > 0 ? 1 : 0;
            item.strain++;
            switch (outcome) {
                case FRACTURE -> {
                    fractures++;
                    int victim = random.nextInt(Math.max(1, item.tiers.length));
                    if (item.tiers.length > 0) {
                        item.tiers[victim]--;
                        if (item.tiers[victim] <= 0) {
                            item.tiers = remove(item.tiers, victim);
                        }
                    }
                }
                case MISS -> {
                }
                case SUCCESS, OVERHOLD, PARTIAL -> {
                    item.rp -= operation.rp();
                    apply(rules, operation, outcome, item, target, random);
                }
            }
        }
        return new Trial(pulses, fe, rules.minutes(fe), item.quality(), fractures, item.strain, item.rp, glyphs,
                reachedGoal, stoppedBy);
    }

    /** PRD, Operations: the Success, Overhold and Partial effects of each operation the behaviors use. */
    private static void apply(Rules rules, Operation operation, Outcome outcome, Item item, int target, RandomSource random) {
        switch (operation.id()) {
            case "resonate" -> {
                if (outcome == Outcome.OVERHOLD) {
                    item.tiers[target] = Math.min(RESONATE_CAP, item.tiers[target] + 2);
                } else if (outcome == Outcome.SUCCESS) {
                    item.tiers[target] = Math.min(RESONATE_CAP, item.tiers[target] + 1);
                }
            }
            case "exalt" -> {
                if (outcome != Outcome.PARTIAL) {
                    item.tiers[target] = MAX_TIER;
                }
            }
            case "temper" -> item.strain = outcome == Outcome.PARTIAL ? item.strain / 2 : 0;
            case "reforge" -> {
                Rarity rarity = rollRarity(item.stage, random);
                if (outcome == Outcome.OVERHOLD) {
                    Rarity second = rollRarity(item.stage, random);
                    rarity = second.ordinal() > rarity.ordinal() ? second : rarity;
                }
                if (outcome == Outcome.PARTIAL) {
                    rarity = item.tiers.length >= 6 ? Rarity.RARE : item.tiers.length >= 2 ? Rarity.MAGIC : Rarity.NORMAL;
                }
                Item rolled = fresh(item.stage, rarity, random);
                item.tiers = rolled.tiers;
                if (outcome != Outcome.PARTIAL) {
                    item.rp = rolled.rp;
                }
            }
            default -> throw new IllegalStateException("No behavior uses " + operation.id());
        }
    }

    private static int[] remove(int[] tiers, int index) {
        int[] result = new int[tiers.length - 1];
        System.arraycopy(tiers, 0, result, 0, index);
        System.arraycopy(tiers, index + 1, result, index, tiers.length - index - 1);
        return result;
    }

    // -- Reporting ---------------------------------------------------------------------------------------------------

    private static Map<String, Object> summarize(List<Trial> trials) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("trials", trials.size());
        summary.put("pulses", distribution(trials.stream().mapToDouble(Trial::pulses).toArray()));
        summary.put("feSpent", distribution(trials.stream().mapToDouble(Trial::fe).toArray()));
        summary.put("minutesAtReference", distribution(trials.stream().mapToDouble(Trial::minutes).toArray()));
        summary.put("finalQuality", distribution(trials.stream().mapToDouble(Trial::finalQuality).toArray()));
        summary.put("fractures", distribution(trials.stream().mapToDouble(Trial::fractures).toArray()));
        summary.put("strainEnd", distribution(trials.stream().mapToDouble(Trial::strain).toArray()));
        summary.put("rpLeft", distribution(trials.stream().mapToDouble(Trial::rpLeft).toArray()));
        summary.put("glyphs", distribution(trials.stream().mapToDouble(Trial::glyphs).toArray()));
        summary.put("reachedGoal", round(trials.stream().filter(Trial::reachedGoal).count() / (double) trials.size()));
        Map<String, Double> stoppedBy = new LinkedHashMap<>();
        for (String reason : List.of("goal", "rp", "budget", "strain", "dead", "limit")) {
            stoppedBy.put(reason, round(trials.stream().filter(t -> t.stoppedBy().equals(reason)).count()
                    / (double) trials.size()));
        }
        summary.put("stoppedBy", stoppedBy);
        return summary;
    }

    private static Map<String, Double> distribution(double[] values) {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        Map<String, Double> result = new LinkedHashMap<>();
        result.put("mean", round(Arrays.stream(sorted).average().orElse(0)));
        result.put("p10", round(percentile(sorted, 0.10)));
        result.put("p50", round(percentile(sorted, 0.50)));
        result.put("p90", round(percentile(sorted, 0.90)));
        return result;
    }

    private static double percentile(double[] sorted, double fraction) {
        if (sorted.length == 0) {
            return 0;
        }
        int index = (int) Math.min(sorted.length - 1, Math.round(fraction * (sorted.length - 1)));
        return sorted[index];
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
