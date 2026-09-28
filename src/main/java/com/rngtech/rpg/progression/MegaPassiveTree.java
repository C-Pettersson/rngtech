package com.rngtech.rpg.progression;

import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineModifierEffect;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.ModifierOperation;
import com.rngtech.rpg.ModifierSlot;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public final class MegaPassiveTree {
    public static final int VERSION = 1;
    public static final int MAX_ALLOCATIONS = 79;
    private static final Map<String, MegaPassiveNode> CATALOG = load();
    public static final PassiveTree<MegaPassiveNode> TREE = new PassiveTree<>(List.copyOf(CATALOG.values()));

    private MegaPassiveTree() { }

    public static MegaPassiveNode node(String id) { return CATALOG.get(id); }
    public static MegaPassiveNode byIndex(int index) {
        return index >= 0 && index < TREE.nodes().size() ? TREE.nodes().get(index) : null;
    }
    public static MegaPassiveNode byButtonId(int id) { return TREE.byButtonId(id); }

    public static boolean has(MachineProgressionState state, String behavior) {
        return state.allocatedNodes().stream().map(CATALOG::get).filter(java.util.Objects::nonNull)
                .anyMatch(node -> node.behaviors().contains(behavior));
    }

    public static int passive(MachineProgressionState state, PassiveStatType stat) {
        return state.allocatedNodes().stream().map(CATALOG::get).filter(java.util.Objects::nonNull)
                .mapToInt(node -> node.passiveStat(stat)).sum();
    }

    public static boolean acceptsHardness(MachineProgressionState state, int hardness) {
        return state.allocatedNodes().stream().map(CATALOG::get).filter(java.util.Objects::nonNull)
                .allMatch(node -> node.recipeHardnessCeiling() <= 0 || hardness <= node.recipeHardnessCeiling());
    }

    public static boolean validBuild(String start, List<String> order) {
        MegaPassiveNode root = node(start);
        if (root == null || root.kind() != PassiveNodeKind.STARTER || order.size() > MAX_ALLOCATIONS) { return false; }
        Set<String> connected = new HashSet<>();
        connected.add(start);
        for (String id : order) {
            MegaPassiveNode node = node(id);
            if (node == null || node.kind() == PassiveNodeKind.STARTER || connected.contains(id)
                    || node.links().stream().noneMatch(connected::contains)) { return false; }
            connected.add(id);
        }
        return true;
    }

    public static boolean connected(String start, List<String> allocations) {
        Set<String> remaining = new HashSet<>(allocations);
        Set<String> reached = new HashSet<>(Set.of(start));
        boolean changed;
        do {
            changed = remaining.removeIf(id -> {
                MegaPassiveNode node = node(id);
                if (node != null && node.links().stream().anyMatch(reached::contains)) { reached.add(id); return true; }
                return false;
            });
        } while (changed);
        return remaining.isEmpty();
    }

    /**
     * Returns the shortest run of unallocated nodes that connects {@code target} to the allocated graph, in allocation
     * order. Foreign starts are never part of a route. Returns an empty list when the target is allocated or unreachable.
     */
    public static List<MegaPassiveNode> allocationPath(Predicate<MegaPassiveNode> allocated, MegaPassiveNode target) {
        if (target == null || target.kind() == PassiveNodeKind.STARTER || allocated.test(target)) { return List.of(); }
        List<MegaPassiveNode> nodes = TREE.nodes();
        int[] previous = new int[nodes.size()];
        java.util.Arrays.fill(previous, -2);
        java.util.ArrayDeque<MegaPassiveNode> queue = new java.util.ArrayDeque<>();
        for (MegaPassiveNode node : nodes) {
            if (allocated.test(node)) { previous[node.index()] = -1; queue.add(node); }
        }
        while (!queue.isEmpty()) {
            MegaPassiveNode current = queue.remove();
            for (String id : current.links()) {
                MegaPassiveNode next = node(id);
                if (previous[next.index()] != -2 || next.kind() == PassiveNodeKind.STARTER) { continue; }
                previous[next.index()] = current.index();
                if (next == target) {
                    List<MegaPassiveNode> path = new ArrayList<>();
                    for (int index = target.index(); previous[index] != -1; index = previous[index]) { path.addFirst(byIndex(index)); }
                    return List.copyOf(path);
                }
                queue.add(next);
            }
        }
        return List.of();
    }

    public static List<String> allocationOrder(String start, List<String> allocations) {
        List<String> remaining = new ArrayList<>(allocations);
        List<String> ordered = new ArrayList<>();
        Set<String> connected = new HashSet<>(Set.of(start));
        while (!remaining.isEmpty()) {
            String next = remaining.stream().filter(id -> node(id) != null && node(id).links().stream().anyMatch(connected::contains)).findFirst().orElse(null);
            if (next == null) { return List.of(); }
            remaining.remove(next); ordered.add(next); connected.add(next);
        }
        return List.copyOf(ordered);
    }

    public static MachineProgressionState follow(MachineProgressionState state, Predicate<MachineProgressionState> gearAllows) {
        if (!state.following() || state.targetNodes().isEmpty()) { return state; }
        for (String id : state.targetNodes()) {
            if (state.allocatedNodes().contains(id)) { continue; }
            if (state.unspentPoints() <= 0) { break; }
            MegaPassiveNode node = node(id);
            if (!TREE.canUnlock(node, state)) { return state.withFollowing(false); }
            MachineProgressionState next = state.withUnlockedNode(node.index());
            if (!gearAllows.test(next)) { return state.withFollowing(false); }
            state = next;
        }
        return state;
    }

    public static void applyStats(MachineStatAccumulator stats, MachineProgressionState state, MachineMasteryFamily family) {
        double[] base = switch (family) { case CRUSHER -> new double[] {0, 20, 0}; case FURNACE -> new double[] {0, 10, 10}; case FORESTRY -> new double[] {10, 10, 0}; };
        MachineStat[] attributes = {MachineStat.CONTROL, MachineStat.DRIVE, MachineStat.RESERVE};
        for (int i = 0; i < attributes.length; i++) { apply(stats, attributes[i], ModifierOperation.ADD, base[i]); }
        List<MegaPassiveNode> allocated = state.allocatedNodes().stream().map(CATALOG::get).filter(java.util.Objects::nonNull).toList();
        for (MegaPassiveNode node : allocated) {
            for (MachineModifierEffect effect : node.effects()) {
                if (family.supports(effect.stat())) { apply(stats, effect.stat(), effect.operation(), effect.value()); }
            }
            node.fixed().forEach((stat, value) -> { if (family.supports(stat)) { stats.setAbsolute(stat, value); } });
            node.ceilings().forEach((stat, value) -> { if (family.supports(stat)) { stats.capAbsolute(stat, value); } });
        }
        double control = Math.max(0, stats.value(MachineStat.CONTROL));
        double drive = Math.max(0, stats.value(MachineStat.DRIVE));
        double reserve = Math.max(0, stats.value(MachineStat.RESERVE));
        if (!has(state, "NO_INHERENT_ATTRIBUTES")) {
            apply(stats, MachineStat.STABILITY, ModifierOperation.INCREASED_PERCENT, control * 0.1);
            apply(stats, MachineStat.ENERGY_USAGE, ModifierOperation.DECREASED_PERCENT, control * 0.02);
            apply(stats, MachineStat.PROCESSING_SPEED, ModifierOperation.INCREASED_PERCENT, drive * 0.15);
            if (family == MachineMasteryFamily.FORESTRY) {
                apply(stats, MachineStat.TREE_FELL_LIMIT, ModifierOperation.ADD, Math.floor(drive / 20));
            } else {
                apply(stats, MachineStat.ENERGY_CAPACITY, ModifierOperation.INCREASED_PERCENT, reserve * 0.25);
            }
            if (family == MachineMasteryFamily.FURNACE) {
                apply(stats, MachineStat.TEMPERATURE_STABILITY, ModifierOperation.INCREASED_PERCENT, control * 0.05);
                apply(stats, MachineStat.HEAT_ISOLATION, ModifierOperation.INCREASED_PERCENT, reserve * 0.1);
            }
        }
        for (MegaPassiveNode node : allocated) {
            for (MegaPassiveNode.AttributeScaling scaling : node.scaling()) {
                if (family.supports(scaling.stat())) {
                    apply(stats, scaling.stat(), scaling.operation(), Math.max(0, stats.value(scaling.attribute())) * scaling.perPoint());
                }
            }
        }
    }

    private static void apply(MachineStatAccumulator stats, MachineStat stat, ModifierOperation operation, double value) {
        stats.apply(new MachineModifier(ModifierSlot.IMPLICIT, stat, operation, value));
    }

    private static Map<String, MegaPassiveNode> load() {
        try (var stream = MegaPassiveTree.class.getResourceAsStream("/data/rngtech/mastery/machine_tree.json")) {
            if (stream == null) { throw new IllegalStateException("Missing shared machine tree"); }
            JsonObject data = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (data.get("version").getAsInt() != VERSION) { throw new IllegalStateException("Unsupported machine tree version"); }
            Map<String, List<String>> links = new HashMap<>();
            for (JsonElement element : data.getAsJsonArray("links")) {
                var link = element.getAsJsonArray(); String a = link.get(0).getAsString(), b = link.get(1).getAsString();
                links.computeIfAbsent(a, ignored -> new ArrayList<>()).add(b);
                links.computeIfAbsent(b, ignored -> new ArrayList<>()).add(a);
            }
            Map<String, MegaPassiveNode> nodes = new LinkedHashMap<>();
            for (JsonElement element : data.getAsJsonArray("nodes")) {
                JsonObject raw = element.getAsJsonObject(); String id = raw.get("id").getAsString();
                List<MachineModifierEffect> effects = new ArrayList<>();
                for (JsonElement item : raw.getAsJsonArray("effects")) {
                    JsonObject e = item.getAsJsonObject();
                    effects.add(MachineModifierEffect.fixed(MachineStat.valueOf(e.get("stat").getAsString()), ModifierOperation.valueOf(e.get("operation").getAsString()), e.get("value").getAsDouble()));
                }
                Set<String> behaviors = new HashSet<>(); raw.getAsJsonArray("behaviors").forEach(e -> behaviors.add(e.getAsString()));
                List<MegaPassiveNode.AttributeScaling> scaling = new ArrayList<>();
                raw.getAsJsonArray("scaling").forEach(e -> {
                    JsonObject s = e.getAsJsonObject();
                    scaling.add(new MegaPassiveNode.AttributeScaling(MachineStat.valueOf(s.get("attribute").getAsString()), MachineStat.valueOf(s.get("stat").getAsString()), ModifierOperation.valueOf(s.get("operation").getAsString()), s.get("perPoint").getAsDouble()));
                });
                Map<PassiveStatType, Integer> passive = new HashMap<>();
                raw.getAsJsonObject("passive").entrySet().forEach(e -> passive.put(PassiveStatType.valueOf(e.getKey()), e.getValue().getAsInt()));
                MegaPassiveNode node = new MegaPassiveNode(nodes.size(), id, raw.get("name").getAsString(), PassiveNodeKind.valueOf(raw.get("kind").getAsString()), raw.get("x").getAsInt(), raw.get("y").getAsInt(), List.copyOf(links.getOrDefault(id, List.of())), List.copyOf(effects), Set.copyOf(behaviors), statsMap(raw.getAsJsonObject("fixed")), statsMap(raw.getAsJsonObject("ceilings")), List.copyOf(scaling), Map.copyOf(passive), raw.has("recipeHardnessCeiling") ? raw.get("recipeHardnessCeiling").getAsInt() : 0);
                if (nodes.put(id, node) != null) { throw new IllegalStateException("Duplicate mastery node " + id); }
            }
            for (MegaPassiveNode node : nodes.values()) {
                if (node.links().stream().anyMatch(id -> !nodes.containsKey(id)) || new HashSet<>(node.links()).size() != node.links().size()) { throw new IllegalStateException("Invalid links: " + node.id()); }
            }
            return java.util.Collections.unmodifiableMap(nodes);
        } catch (java.io.IOException exception) { throw new IllegalStateException("Cannot load machine tree", exception); }
    }

    private static Map<MachineStat, Double> statsMap(JsonObject raw) {
        Map<MachineStat, Double> result = new HashMap<>();
        raw.entrySet().forEach(e -> result.put(MachineStat.valueOf(e.getKey()), e.getValue().getAsDouble()));
        return Map.copyOf(result);
    }
}
