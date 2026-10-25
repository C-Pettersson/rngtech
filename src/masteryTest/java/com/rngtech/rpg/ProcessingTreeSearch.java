package com.rngtech.rpg;

import com.rngtech.rpg.ProcessingBalanceSimulation.Recipes;
import com.rngtech.rpg.ProcessingBalanceSimulation.Step;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MegaPassiveNode;
import com.rngtech.rpg.progression.MegaPassiveTree;
import com.rngtech.rpg.progression.PassiveNodeKind;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Regenerates the shared Mastery tree allocations under {@code trees} in {@code tools/balance/processing-setups.json}.
 *
 * <p>Each tree under {@code treeSearch.objectives} names a scoring archetype and the metrics it maximizes. A candidate
 * build scores the sum, over every stage, of the log ratio of each metric to the same machine with no tree, measured by
 * the ore line model itself: the Crusher on the stage's ore, the Furnace on the stage's crushed ore. The search is greedy:
 * each step allocates the shortest path to whichever node gains the most score per point, ties going to the shorter path
 * and then the lower node index, and stops when no path gains or the level's points run out. Nodes that cap recipe
 * temperature or hardness, disable bonus output (Single Pass) or block the Battery Cell (Cell Bypass) are never allocated.
 */
public final class ProcessingTreeSearch {
    private static final double MIN_GAIN = 1e-9;

    private final List<JsonObject> stages = new ArrayList<>();
    private final List<Recipes> recipes = new ArrayList<>();
    private final List<MegaPassiveNode> nodes = MegaPassiveTree.TREE.nodes();
    private final boolean[] excluded = new boolean[nodes.size()];

    private ProcessingTreeSearch(ProcessingBalanceSimulation simulation) throws IOException {
        for (JsonElement stage : simulation.setups.getAsJsonArray("stages")) {
            stages.add(stage.getAsJsonObject());
            recipes.add(simulation.recipes(stage.getAsJsonObject().get("metal").getAsString()));
        }
        for (MegaPassiveNode node : nodes) {
            excluded[node.index()] = blocksRecipes(node);
        }
    }

    public static void main(String[] args) throws IOException {
        Path projectDir = Path.of(args.length > 0 ? args[0] : ".").toAbsolutePath().normalize();
        Path setupsPath = projectDir.resolve("tools/balance/processing-setups.json");
        ProcessingBalanceSimulation.loadDefaultConfig();
        ProcessingBalanceSimulation simulation = new ProcessingBalanceSimulation(projectDir);
        ProcessingTreeSearch search = new ProcessingTreeSearch(simulation);
        JsonObject setups = simulation.setups;
        JsonObject trees = setups.getAsJsonObject("trees");
        for (Map.Entry<String, JsonElement> entry : setups.getAsJsonObject("treeSearch").getAsJsonObject("objectives").entrySet()) {
            JsonObject objective = entry.getValue().getAsJsonObject();
            Objective scoring = new Objective(
                    MachineMasteryFamily.valueOf(objective.get("family").getAsString().toUpperCase(Locale.ROOT)),
                    setups.getAsJsonObject("archetypes").getAsJsonObject(objective.get("archetype").getAsString()),
                    objective.getAsJsonArray("metrics").asList().stream().map(JsonElement::getAsString).toList());
            JsonObject byLevel = new JsonObject();
            for (JsonElement level : setups.getAsJsonArray("levels")) {
                List<String> build = search.search(scoring, level.getAsInt());
                JsonArray allocation = new JsonArray();
                build.forEach(allocation::add);
                byLevel.add(level.getAsString(), allocation);
                System.out.printf(Locale.ROOT, "%s L%d: %d points, score %.4f%n", entry.getKey(), level.getAsInt(), build.size(),
                        search.score(scoring, level.getAsInt(), build));
            }
            trees.add(entry.getKey(), byLevel);
        }
        Files.writeString(setupsPath, json(setups), StandardCharsets.UTF_8);
        System.out.println("Tree allocations -> " + setupsPath);
    }

    private record Objective(MachineMasteryFamily family, JsonObject archetype, List<String> metrics) {
        JsonObject build() {
            return archetype.getAsJsonObject(family == MachineMasteryFamily.CRUSHER ? "crusher" : "furnace");
        }
    }

    /** The greedy allocation for a machine at {@code level}, in a valid allocation order. */
    private List<String> search(Objective objective, int level) {
        String start = objective.family().startNodeId();
        List<String> build = new ArrayList<>();
        double current = score(objective, level, build);
        while (build.size() < level - 1) {
            int points = level - 1 - build.size();
            List<List<String>> paths = paths(start, build, points);
            double base = current;
            Candidate best = paths.parallelStream()
                    .map(path -> {
                        List<String> next = new ArrayList<>(build);
                        next.addAll(path);
                        double score = score(objective, level, next);
                        return new Candidate(path, score, (score - base) / path.size());
                    })
                    .filter(candidate -> candidate.score() - base > MIN_GAIN)
                    .max(Comparator.comparingDouble(Candidate::rate)
                            .thenComparing(candidate -> -candidate.path().size())
                            .thenComparing(candidate -> -MegaPassiveTree.node(candidate.path().getLast()).index()))
                    .orElse(null);
            if (best == null) {
                break;
            }
            build.addAll(best.path());
            current = best.score();
        }
        if (!MegaPassiveTree.validBuild(start, build)) {
            throw new IllegalStateException("Search produced an invalid build " + build);
        }
        return List.copyOf(build);
    }

    private record Candidate(List<String> path, double score, double rate) {
    }

    /** The shortest path, in allocation order, from the allocated nodes to every node within {@code points} steps. */
    private List<List<String>> paths(String start, List<String> build, int points) {
        int[] previous = new int[nodes.size()];
        int[] depth = new int[nodes.size()];
        Arrays.fill(previous, -2);
        ArrayDeque<MegaPassiveNode> queue = new ArrayDeque<>();
        Set<String> allocated = new HashSet<>(build);
        allocated.add(start);
        for (String id : allocated) {
            MegaPassiveNode node = MegaPassiveTree.node(id);
            previous[node.index()] = -1;
            queue.add(node);
        }
        List<List<String>> paths = new ArrayList<>();
        while (!queue.isEmpty()) {
            MegaPassiveNode current = queue.remove();
            if (depth[current.index()] >= points) {
                continue;
            }
            for (String id : current.links()) {
                MegaPassiveNode next = MegaPassiveTree.node(id);
                if (previous[next.index()] != -2 || next.kind() == PassiveNodeKind.STARTER || excluded[next.index()]) {
                    continue;
                }
                previous[next.index()] = current.index();
                depth[next.index()] = depth[current.index()] + 1;
                List<String> path = new ArrayList<>();
                for (int index = next.index(); previous[index] != -1; index = previous[index]) {
                    path.addFirst(nodes.get(index).id());
                }
                paths.add(path);
                queue.add(next);
            }
        }
        return paths;
    }

    /** Sum over stages and metrics of the log ratio to the empty tree; a step the build blocks scores negative infinity. */
    private double score(Objective objective, int level, List<String> build) {
        return IntStream.range(0, stages.size()).mapToDouble(stage -> {
            double[] empty = metrics(objective, stage, level, List.of());
            if (empty == null) {
                return 0.0;
            }
            double[] metrics = metrics(objective, stage, level, build);
            if (metrics == null) {
                return Double.NEGATIVE_INFINITY;
            }
            double total = 0.0;
            for (int i = 0; i < metrics.length; i++) {
                total += Math.log(metrics[i] / empty[i]);
            }
            return total;
        }).sum();
    }

    private double[] metrics(Objective objective, int stageIndex, int level, List<String> build) {
        JsonObject stage = stages.get(stageIndex);
        JsonObject machine = objective.build();
        var state = ProcessingBalanceSimulation.progression(stage, machine, level, build, objective.family());
        Step step;
        int batch = 1;
        if (objective.family() == MachineMasteryFamily.CRUSHER) {
            ProcessingBalanceSimulation.Crusher crusher = ProcessingBalanceSimulation.crusher(stage, machine, state);
            step = crusher.crush(recipes.get(stageIndex).crushOre());
            batch = BatchProcessing.batchSize(crusher.stats(), crusher.oath());
        } else {
            step = ProcessingBalanceSimulation.furnace(stage, stage.getAsJsonObject("furnace"), machine, state)
                    .smelt(recipes.get(stageIndex).smeltCrushed());
        }
        if (step.blocked()) {
            return null;
        }
        double[] values = new double[objective.metrics().size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = switch (objective.metrics().get(i)) {
                case "output" -> step.outputPerInput();
                case "speed" -> 1.0 / step.ticksPerInput();
                case "efficiency" -> 1.0 / Math.max(1e-9, step.fePerInput());
                case "batch" -> batch;
                default -> throw new IllegalArgumentException("Unknown tree search metric " + objective.metrics().get(i));
            };
        }
        return values;
    }

    /** Temperature and hardness caps, Single Pass and Cell Bypass would block recipes the model runs. */
    static boolean blocksRecipes(MegaPassiveNode node) {
        return node.recipeHardnessCeiling() > 0
                || node.ceilings().containsKey(MachineStat.MAX_TEMPERATURE)
                || node.behaviors().contains("NO_BONUS_OUTPUT")
                || node.behaviors().contains("BLOCK_BATTERY");
    }

    /** Four-space indented JSON, matching the checked-in file. */
    private static String json(JsonObject setups) throws IOException {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        StringWriter out = new StringWriter();
        JsonWriter writer = gson.newJsonWriter(out);
        writer.setIndent("    ");
        gson.toJson(setups, writer);
        writer.flush();
        return out + "\n";
    }
}
