package com.rngtech.rpg.progression;

import com.rngtech.rpg.MachineModifierEffect;
import com.rngtech.rpg.MachineStat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Balance audits that keep shared-tree nodes from doing nothing: every claimed stat is read by the machine, every node
 * pays some machine, keystone limits leave recipes to run, and no keystone is strictly worse than another.
 */
final class TreeAuditChecks {
    private static final Set<MachineStat> LOWER_IS_BETTER = Set.of(
            MachineStat.ENERGY_USAGE, MachineStat.THERMAL_MASS, MachineStat.COOLING_RATE, MachineStat.IDLE_LOSS, MachineStat.BATCH_OVERHEAD);
    /** Dense Batching's cost is waiting for a full batch. */
    private static final Set<String> COST_BEHAVIORS = Set.of("BLOCK_BATTERY", "MATCHING_HEAD", "NO_BONUS_OUTPUT", "DENSE_BATCHING",
            "NO_INHERENT_ATTRIBUTES", "NO_INHERENT_CONTROL", "NO_INHERENT_DRIVE", "NO_INHERENT_RESERVE");
    /** A keystone limit must leave at least this share of a paid machine's recipes runnable. */
    private static final double MIN_RECIPE_SHARE = 0.2;

    private static final Map<MachineMasteryFamily, String> SOURCES = Map.of(
            MachineMasteryFamily.CRUSHER, "content/blockentity/CrusherBlockEntity.java",
            MachineMasteryFamily.FURNACE, "content/blockentity/FurnaceBlockEntity.java",
            MachineMasteryFamily.ALLOY_FURNACE, "content/blockentity/AlloyFurnaceBlockEntity.java",
            MachineMasteryFamily.METAL_PRESS, "content/blockentity/MetalPressBlockEntity.java",
            MachineMasteryFamily.MELTER, "content/blockentity/MelterBlockEntity.java",
            MachineMasteryFamily.RESONANCE_CALIBRATOR, "content/blockentity/ResonanceCalibratorBlockEntity.java",
            MachineMasteryFamily.FORESTRY, "content/entity/ForestryCartEntity.java");
    /** Accumulator helpers and the stats they read for the machine that calls them. */
    private static final Map<String, Set<MachineStat>> ACCUMULATOR_HELPERS = Map.of(
            "adjustedProcessingTicks", Set.of(MachineStat.PROCESSING_SPEED),
            "adjustedHeatProcessingTicks", Set.of(MachineStat.PROCESSING_SPEED),
            "adjustedEnergyCost", Set.of(MachineStat.ENERGY_USAGE),
            "adjustedFuelTicks", Set.of(MachineStat.FUEL_EFFICIENCY, MachineStat.FUEL_DURATION),
            // Flat Energy Capacity folds into Energy Capacity inside the accumulator.
            "MachineStat.ENERGY_CAPACITY)", Set.of(MachineStat.ENERGY_CAPACITY_FLAT));
    /** Static helper classes whose methods read stats for the machine that calls them. */
    private static final List<String> HELPER_CLASSES = List.of(
            "content/blockentity/HeatControl.java", "content/blockentity/HeatEngine.java", "content/blockentity/ProcessingChance.java",
            "rpg/BatchProcessing.java",
            "content/recipe/ProcessingEnergyScaling.java", "rpg/progression/AscendancyFormulas.java");
    private static final Pattern STATIC_METHOD = Pattern.compile("(?m)^\\s+(?:public |private |protected )?static\\s+[\\w<>\\[\\], ?]+\\s+(\\w+)\\s*\\(");
    private static final Pattern STAT_REFERENCE = Pattern.compile("MachineStat\\.([A-Z_]+)\\b");
    /** Recipe types whose heat a keystone's maximum temperature limit can block. */
    private static final Map<MachineMasteryFamily, String> HEAT_RECIPES = Map.of(
            MachineMasteryFamily.FURNACE, "rngtech:furnace",
            MachineMasteryFamily.ALLOY_FURNACE, "rngtech:alloy_furnace",
            MachineMasteryFamily.METAL_PRESS, "rngtech:metal_press");

    private static int checks;

    private TreeAuditChecks() {
    }

    static int run() {
        Path root = projectRoot();
        claimedStatsAreRead(root);
        everyNodePaysSomeMachine();
        fuelFurnacePaysForPayoffs();
        limitsLeaveRecipes(root);
        noKeystoneIsDominated();
        dominanceAuditCatchesOldHeatKeystones();
        singlePassDisablesDeclaredYield();
        return checks;
    }

    /** A family that claims a stat must read it outside its menu sync, directly or through a shared helper. */
    private static void claimedStatsAreRead(Path root) {
        Set<String> fixtures = new HashSet<>();
        // Test-only fixture stats have no machine to read them.
        JsonObject fixtureDeclarations = JsonParser.parseString(String.join("\n",
                lines(root.resolve("src/masteryTest/resources/data/rngtech/mastery/ascendancies/fixtures/declarations.json")))).getAsJsonObject();
        fixtureDeclarations.getAsJsonArray("stats").forEach(stat -> fixtures.add(stat.getAsJsonObject().get("stat").getAsString()));
        List<String> unread = new ArrayList<>();
        Map<String, Set<MachineStat>> helpers = helperReads(root);
        for (MachineMasteryFamily family : MachineMasteryFamily.values()) {
            Set<MachineStat> read = readStats(root.resolve("src/main/java/com/rngtech").resolve(SOURCES.get(family)), helpers);
            for (MachineStat stat : MachineStat.values()) {
                if (!MachineStat.isAttribute(stat) && family.supports(stat) && !fixtures.contains(stat.name()) && !read.contains(stat)) {
                    unread.add(family + " " + stat);
                }
            }
        }
        require(unread.isEmpty(), "families claim stats their machines never read: " + unread);
    }

    private static Set<MachineStat> readStats(Path source, Map<String, Set<MachineStat>> helpers) {
        Set<MachineStat> read = new HashSet<>();
        for (String line : lines(source)) {
            if (line.contains("DATA_") || line.contains("scaledStat(")) {
                continue;
            }
            read.addAll(statsIn(line));
            helpers.forEach((helper, stats) -> {
                if (line.contains(helper)) {
                    read.addAll(stats);
                }
            });
        }
        return read;
    }

    /** {@code Class.method(} for every static helper method, with the stats it reads directly or through its own class. */
    private static Map<String, Set<MachineStat>> helperReads(Path root) {
        Map<String, Set<MachineStat>> helpers = new HashMap<>(ACCUMULATOR_HELPERS);
        for (String file : HELPER_CLASSES) {
            String text = String.join("\n", lines(root.resolve("src/main/java/com/rngtech").resolve(file)));
            String className = file.substring(file.lastIndexOf('/') + 1, file.length() - ".java".length());
            List<Integer> starts = new ArrayList<>();
            List<String> names = new ArrayList<>();
            Matcher matcher = STATIC_METHOD.matcher(text);
            while (matcher.find()) {
                starts.add(matcher.start());
                names.add(matcher.group(1));
            }
            Map<String, String> bodies = new HashMap<>();
            for (int i = 0; i < names.size(); i++) {
                int end = i + 1 < starts.size() ? starts.get(i + 1) : text.length();
                bodies.merge(names.get(i), text.substring(starts.get(i), end), String::concat);
            }
            Map<String, Set<MachineStat>> reads = new HashMap<>();
            bodies.forEach((name, body) -> reads.put(name, statsIn(body)));
            for (boolean changed = true; changed; ) {
                changed = false;
                for (var body : bodies.entrySet()) {
                    for (String callee : bodies.keySet()) {
                        if (!callee.equals(body.getKey()) && body.getValue().contains(callee + "(")) {
                            changed |= reads.get(body.getKey()).addAll(reads.get(callee));
                        }
                    }
                }
            }
            reads.forEach((name, stats) -> helpers.put(className + "." + name + "(", stats));
        }
        return helpers;
    }

    private static Set<MachineStat> statsIn(String text) {
        Set<MachineStat> stats = new HashSet<>();
        Matcher matcher = STAT_REFERENCE.matcher(text);
        while (matcher.find()) {
            MachineStat stat = MachineStat.bySerializedName(matcher.group(1));
            if (stat != null) {
                stats.add(stat);
            }
        }
        return stats;
    }

    /** Every reward node and keystone must benefit at least one machine; otherwise its points buy nothing. */
    private static void everyNodePaysSomeMachine() {
        for (MegaPassiveNode node : MegaPassiveTree.TREE.nodes()) {
            if (node.kind() == PassiveNodeKind.STARTER || node.kind() == PassiveNodeKind.TRAVEL) {
                continue;
            }
            boolean pays = false;
            for (MachineMasteryFamily family : MachineMasteryFamily.values()) {
                pays |= !live(node, family).payoffs().isEmpty() || node.passive().keySet().stream().anyMatch(family::supports);
            }
            require(pays, node.id() + " pays no machine");
        }
    }

    /**
     * The Stage 0 Furnace burns fuel instead of FE, so a keystone that pays the Furnace and costs only Energy Usage would
     * be free on it. Such a keystone needs a fuel cost too.
     */
    private static void fuelFurnacePaysForPayoffs() {
        for (MegaPassiveNode keystone : keystones()) {
            Live live = live(keystone, MachineMasteryFamily.FURNACE);
            if (live.payoffs().isEmpty() || !live.costs().containsKey("ENERGY_USAGE")) {
                continue;
            }
            boolean otherCost = live.costs().keySet().stream().anyMatch(key -> !key.equals("ENERGY_USAGE"))
                    || !live.costBehaviors().isEmpty() || !live.ceilings().isEmpty();
            require(otherCost, keystone.id() + " pays the fuel Furnace while costing only Energy Usage");
        }
    }

    /** A maximum temperature limit must leave a paid heat machine enough recipes to run, unlike the old 600 cap. */
    private static void limitsLeaveRecipes(Path root) {
        Map<String, List<Integer>> heat = recipeHeat(root.resolve("src/main/resources/data/rngtech/recipe"));
        for (MegaPassiveNode keystone : keystones()) {
            Double cap = keystone.ceilings().get(MachineStat.MAX_TEMPERATURE);
            if (cap == null) {
                continue;
            }
            for (var entry : HEAT_RECIPES.entrySet()) {
                MachineMasteryFamily family = entry.getKey();
                if (live(keystone, family).payoffs().isEmpty()) {
                    continue;
                }
                List<Integer> recipes = heat.getOrDefault(entry.getValue(), List.of());
                long usable = recipes.stream().filter(temperature -> temperature <= cap).count();
                require(!recipes.isEmpty() && usable >= recipes.size() * MIN_RECIPE_SHARE,
                        keystone.id() + " pays " + family + " but its " + cap.intValue() + " limit leaves " + usable + " of " + recipes.size() + " recipes");
            }
        }
    }

    private static Map<String, List<Integer>> recipeHeat(Path recipes) {
        Map<String, List<Integer>> heat = new HashMap<>();
        try (Stream<Path> files = Files.walk(recipes)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                JsonObject recipe = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                String type = recipe.has("type") ? recipe.get("type").getAsString() : "";
                if (!HEAT_RECIPES.containsValue(type)) {
                    continue;
                }
                int minimum = recipe.has("minimum_temperature") ? recipe.get("minimum_temperature").getAsInt() : type.equals("rngtech:alloy_furnace") ? 700 : 0;
                int target = recipe.has("target_temperature") ? recipe.get("target_temperature").getAsInt() : minimum;
                heat.computeIfAbsent(type, ignored -> new ArrayList<>()).add(target);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return heat;
    }

    /** A keystone strictly worse than another on every machine it pays is a trap, like Flash Annealing before #156. */
    private static void noKeystoneIsDominated() {
        List<MegaPassiveNode> keystones = keystones();
        for (MegaPassiveNode loser : keystones) {
            for (MegaPassiveNode winner : keystones) {
                require(winner == loser || !dominated(loser, winner), loser.id() + " is never better than " + winner.id());
            }
        }
    }

    private static void dominanceAuditCatchesOldHeatKeystones() {
        MegaPassiveNode lowHeat = MegaPassiveTree.node("low_heat_specialist");
        MegaPassiveNode oldLowHeat = keystone(lowHeat, List.of(), List.of(new MegaPassiveNode.TaggedEffect(MachineTag.HEATED,
                MachineModifierEffect.fixed(MachineStat.PROCESSING_SPEED, com.rngtech.rpg.ModifierOperation.MORE, 2.0))), Map.of(MachineStat.MAX_TEMPERATURE, 800.0));
        MegaPassiveNode oldFlash = keystone(lowHeat, List.of(MachineModifierEffect.fixed(MachineStat.ENERGY_USAGE, com.rngtech.rpg.ModifierOperation.MORE, 1.5)),
                List.of(new MegaPassiveNode.TaggedEffect(MachineTag.HEATED, MachineModifierEffect.fixed(MachineStat.PROCESSING_SPEED, com.rngtech.rpg.ModifierOperation.MORE, 2.0))),
                Map.of(MachineStat.MAX_TEMPERATURE, 600.0));
        require(dominated(oldFlash, oldLowHeat), "dominance audit catches the old Flash Annealing");
        require(!dominated(oldLowHeat, oldFlash), "dominance audit keeps the old Low Heat Specialist");
    }

    private static MegaPassiveNode keystone(MegaPassiveNode base, List<MachineModifierEffect> effects, List<MegaPassiveNode.TaggedEffect> tagged, Map<MachineStat, Double> ceilings) {
        return new MegaPassiveNode(base.index(), base.id(), base.name(), base.kind(), base.x(), base.y(), base.links(),
                effects, tagged, Set.of(), Map.of(), ceilings, List.of(), Map.of(), 0);
    }

    /** Single Pass must zero every declared output-yield stat and drop every declared output-yield behavior. */
    private static void singlePassDisablesDeclaredYield() {
        for (MachineStat stat : MachineStat.values()) {
            if (MasteryDeclarations.declared(stat) && MasteryDeclarations.yield(stat) == MasteryDeclarations.Yield.OUTPUT) {
                require(MegaPassiveTree.NO_BONUS_OUTPUT_CEILINGS.get(stat) == 0.0, "Single Pass zeroes declared yield " + stat);
            }
        }
    }

    private static boolean dominated(MegaPassiveNode loser, MegaPassiveNode winner) {
        boolean paysSomewhere = false;
        for (MachineMasteryFamily family : MachineMasteryFamily.values()) {
            Live lose = live(loser, family);
            if (lose.payoffs().isEmpty()) {
                continue;
            }
            paysSomewhere = true;
            if (!atLeastAsGood(live(winner, family), lose)) {
                return false;
            }
        }
        return paysSomewhere;
    }

    private static boolean atLeastAsGood(Live winner, Live loser) {
        boolean payoffs = loser.payoffs().entrySet().stream().allMatch(e -> winner.payoffs().getOrDefault(e.getKey(), 0.0) >= e.getValue());
        boolean costs = winner.costs().entrySet().stream().allMatch(e -> loser.costs().getOrDefault(e.getKey(), 0.0) >= e.getValue());
        boolean behaviors = winner.payoffBehaviors().containsAll(loser.payoffBehaviors()) && loser.costBehaviors().containsAll(winner.costBehaviors());
        boolean limits = winner.ceilings().entrySet().stream().allMatch(e -> loser.ceilings().containsKey(e.getKey()) && loser.ceilings().get(e.getKey()) <= e.getValue());
        return payoffs && costs && behaviors && limits;
    }

    /**
     * What a node does on one family, as the size of each benefit and each cost. A payoff key is a stat plus operation;
     * its value grows with the benefit, so a larger value is always better for a payoff and worse for a cost.
     */
    private record Live(Map<String, Double> payoffs, Map<String, Double> costs, Set<String> payoffBehaviors, Set<String> costBehaviors,
            Map<MachineStat, Double> ceilings) {
    }

    private static Live live(MegaPassiveNode node, MachineMasteryFamily family) {
        Map<String, Double> payoffs = new HashMap<>(), costs = new HashMap<>();
        List<MachineModifierEffect> effects = new ArrayList<>();
        node.effects().stream().filter(effect -> family.supports(effect.stat())).forEach(effects::add);
        node.tagged().stream().filter(tagged -> tagged.appliesTo(family)).forEach(tagged -> effects.add(tagged.effect()));
        for (MachineModifierEffect effect : effects) {
            boolean raises;
            double size;
            switch (effect.operation()) {
                case MORE, LESS -> { raises = effect.value() > 1.0; size = Math.max(effect.value(), 1.0 / effect.value()); }
                case INCREASED_PERCENT -> { raises = effect.value() > 0; size = Math.abs(effect.value()); }
                case DECREASED_PERCENT -> { raises = effect.value() < 0; size = Math.abs(effect.value()); }
                default -> { raises = effect.value() > 0; size = Math.abs(effect.value()); }
            }
            String key = effect.stat().name() + (effect.operation().name().startsWith("MORE") || effect.operation().name().startsWith("LESS") ? ":MULT" : ":" + effect.operation().name().replace("DECREASED", "INCREASED"));
            boolean benefit = raises != LOWER_IS_BETTER.contains(effect.stat());
            (benefit ? payoffs : costs).merge(benefit ? key : effect.stat().name(), size, benefit ? Double::sum : Math::max);
        }
        for (MegaPassiveNode.AttributeScaling scaling : node.scaling()) {
            if (family.supports(scaling.stat())) {
                payoffs.merge("scaling:" + scaling.attribute() + ":" + scaling.stat(), scaling.perPoint(), Double::sum);
            }
        }
        Set<String> payoffBehaviors = new HashSet<>(), costBehaviors = new HashSet<>();
        for (String behavior : node.behaviors()) {
            if (family.supportsBehavior(behavior)) {
                (COST_BEHAVIORS.contains(behavior) ? costBehaviors : payoffBehaviors).add(behavior);
            }
        }
        payoffBehaviors.forEach(behavior -> payoffs.put("behavior:" + behavior, 1.0));
        Map<MachineStat, Double> ceilings = new EnumMap<>(MachineStat.class);
        node.ceilings().forEach((stat, value) -> { if (family.supportsAbsolute(stat)) { ceilings.put(stat, value); } });
        node.fixed().forEach((stat, value) -> { if (family.supportsAbsolute(stat)) { ceilings.put(stat, value); } });
        if (node.recipeHardnessCeiling() > 0 && family.supports(MachineStat.PROCESSING_LEVEL)) {
            ceilings.put(MachineStat.PROCESSING_LEVEL, (double) node.recipeHardnessCeiling());
        }
        return new Live(payoffs, costs, payoffBehaviors, costBehaviors, ceilings);
    }

    private static List<MegaPassiveNode> keystones() {
        return MegaPassiveTree.TREE.nodes().stream().filter(node -> node.kind() == PassiveNodeKind.KEYSTONE).toList();
    }

    private static Path projectRoot() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            if (Files.exists(dir.resolve("settings.gradle"))) {
                return dir;
            }
        }
        throw new IllegalStateException("No project root above " + Path.of("").toAbsolutePath());
    }

    private static List<String> lines(Path file) {
        try {
            return Files.readAllLines(file);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError(label);
        }
    }
}
