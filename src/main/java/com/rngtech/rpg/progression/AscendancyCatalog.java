package com.rngtech.rpg.progression;

import com.rngtech.rpg.MachineStat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Data-driven ascendancy trees. {@code data/rngtech/mastery/ascendancies/index.json} lists one file per ascendancy in
 * display order, and a family may have any number of them. Every entry is validated when it loads.
 */
public final class AscendancyCatalog {
    public static final int VERSION = 1;
    public static final int MAX_TIERS = 3;
    public static final int POINTS_PER_TIER = 2;
    public static final int MAX_POINTS = MAX_TIERS * POINTS_PER_TIER;
    /** Minimum chassis stage, or its host equivalent, for a first Ascendancy Seal. */
    public static final int ENTRY_STAGE = 4;
    public static final int MIN_NODES = 12;
    public static final int MAX_NODES = 16;
    /** Notables that the full budget must be able to reach, at two points each. */
    private static final int REACHABLE_NOTABLES = MAX_POINTS / 2;
    private static final String DIRECTORY = "/data/rngtech/mastery/ascendancies/";
    /** Present only on the domain-check classpath. */
    private static final String FIXTURE_INDEX = "fixtures/index.json";
    private static final Pattern ID = Pattern.compile("[a-z0-9_]+");
    private static final Map<String, Ascendancy> CATALOG = load();

    private AscendancyCatalog() { }

    public static Ascendancy get(String id) { return id == null ? null : CATALOG.get(id); }
    public static Collection<Ascendancy> all() { return CATALOG.values(); }
    public static List<Ascendancy> forFamily(MachineMasteryFamily family) {
        return CATALOG.values().stream().filter(ascendancy -> ascendancy.family() == family).toList();
    }

    /** The chosen ascendancy's root and allocated nodes, or nothing without a valid choice. */
    public static List<AscendancyNode> allocated(MachineProgressionState state) {
        Ascendancy chosen = get(state.ascendancy());
        if (chosen == null) { return List.of(); }
        List<AscendancyNode> result = new ArrayList<>();
        result.add(chosen.root());
        state.ascendancyNodes().stream().map(chosen::node).filter(java.util.Objects::nonNull).forEach(result::add);
        return result;
    }

    public static List<AscendancyNode> allocated(MachineProgressionState state, MachineMasteryFamily family) {
        Ascendancy chosen = get(state.ascendancy());
        return chosen != null && chosen.family() == family ? allocated(state) : List.of();
    }

    /** Known, connected allocations in stored order. Everything is refunded when they exceed {@code budget}. */
    static List<String> sanitize(Ascendancy ascendancy, List<String> allocated, int budget) {
        List<String> kept = new ArrayList<>(new LinkedHashSet<>(allocated));
        kept.removeIf(id -> ascendancy.node(id) == null);
        boolean pruned = true;
        while (pruned) {
            List<String> disconnected = kept.stream().filter(id -> {
                String parent = ascendancy.node(id).parent();
                return !parent.equals(ascendancy.root().id()) && !kept.contains(parent);
            }).toList();
            pruned = kept.removeAll(disconnected);
        }
        return kept.size() > budget ? List.of() : List.copyOf(kept);
    }

    /** Parses one ascendancy file, rejecting it with every violation found. */
    public static Ascendancy parse(JsonObject raw) {
        String id = raw.get("id").getAsString();
        MachineMasteryFamily family = MachineMasteryFamily.valueOf(raw.get("family").getAsString().toUpperCase(Locale.ROOT));
        List<String> violations = new ArrayList<>();
        AscendancyNode root = null;
        Map<String, AscendancyNode> nodes = new LinkedHashMap<>();
        for (JsonElement element : MasteryNodeJson.array(raw, "nodes")) {
            JsonObject node = element.getAsJsonObject();
            String nodeId = node.get("id").getAsString();
            AscendancyNode.Kind kind = AscendancyNode.Kind.valueOf(node.get("kind").getAsString());
            String parent = node.has("parent") ? node.get("parent").getAsString() : "";
            MasteryNodeJson.Effects e = MasteryNodeJson.effects(node);
            AscendancyNode parsed = new AscendancyNode(id, nodeId, kind, parent, node.get("x").getAsInt(), node.get("y").getAsInt(),
                    e.effects(), e.tagged(), e.behaviors(), e.fixed(), e.ceilings(), e.scaling(), e.passive(), e.recipeHardnessCeiling());
            if (kind == AscendancyNode.Kind.ROOT && root == null) {
                root = parsed;
                if (!parent.isEmpty()) { violations.add(nodeId + ": the root has no parent"); }
            } else if (nodes.put(nodeId, parsed) != null) {
                violations.add(nodeId + ": duplicate node");
            }
        }
        if (root == null) { throw new IllegalStateException("Invalid ascendancy " + id + ": no root"); }
        Ascendancy ascendancy = new Ascendancy(id, family, root, Collections.unmodifiableMap(nodes));
        violations.addAll(violations(ascendancy));
        if (!violations.isEmpty()) { throw new IllegalStateException("Invalid ascendancy " + id + ": " + violations); }
        return ascendancy;
    }

    /** Tree shape and family support rules that every ascendancy must follow. */
    public static List<String> violations(Ascendancy ascendancy) {
        List<String> violations = new ArrayList<>();
        AscendancyNode root = ascendancy.root();
        if (!ID.matcher(ascendancy.id()).matches()) { violations.add("the id must use lowercase letters, digits, and underscores"); }
        int count = ascendancy.nodes().size() + 1;
        if (count < MIN_NODES || count > MAX_NODES) { violations.add(count + " nodes; expected " + MIN_NODES + "-" + MAX_NODES); }
        if (ascendancy.nodes().containsKey(root.id())) { violations.add(root.id() + ": a node reuses the root id"); }
        for (AscendancyNode node : Stream.concat(Stream.of(root), ascendancy.nodes().values().stream()).toList()) {
            if (!ID.matcher(node.id()).matches()) { violations.add(node.id() + ": invalid id"); }
            if (node.grantsNothing()) { violations.add(node.id() + " grants nothing"); }
            violations.addAll(unsupported(ascendancy.family(), node));
        }
        for (AscendancyNode node : ascendancy.nodes().values()) {
            AscendancyNode parent = node.parent().equals(root.id()) ? root : ascendancy.node(node.parent());
            if (parent == null) { violations.add(node.id() + " has an unknown parent " + node.parent()); continue; }
            switch (node.kind()) {
                case ROOT -> violations.add(node.id() + " is a second root");
                case SMALL -> {
                    if (parent.kind() == AscendancyNode.Kind.SMALL) { violations.add(node.id() + ": a small node sits behind the root or a notable"); }
                    List<AscendancyNode> children = ascendancy.children(node.id());
                    if (children.size() != 1 || children.getFirst().kind() != AscendancyNode.Kind.NOTABLE) {
                        violations.add(node.id() + ": a small node leads to exactly one notable");
                    }
                }
                case NOTABLE -> {
                    if (parent.kind() != AscendancyNode.Kind.SMALL) { violations.add(node.id() + ": a notable sits behind a small node"); }
                }
            }
            if (!reachesRoot(ascendancy, node)) { violations.add(node.id() + " does not reach the root"); }
        }
        if (ascendancy.nodes().values().stream().noneMatch(ascendancy::isDeep)) { violations.add("needs at least one deep notable"); }
        long firstPairs = ascendancy.nodes().values().stream()
                .filter(node -> node.kind() == AscendancyNode.Kind.SMALL && node.parent().equals(root.id())).count();
        if (firstPairs < REACHABLE_NOTABLES) { violations.add("the full budget must reach at least " + REACHABLE_NOTABLES + " notables"); }
        return violations;
    }

    private static boolean reachesRoot(Ascendancy ascendancy, AscendancyNode node) {
        AscendancyNode current = node;
        for (int steps = 0; steps <= ascendancy.nodes().size(); steps++) {
            if (current.parent().equals(ascendancy.root().id())) { return true; }
            current = ascendancy.node(current.parent());
            if (current == null) { return false; }
        }
        return false;
    }

    /** Ascendancies belong to one family, so every effect must apply to it; the shared tree's inactive effects are not allowed. */
    private static List<String> unsupported(MachineMasteryFamily family, AscendancyNode node) {
        List<String> violations = new ArrayList<>();
        node.effects().stream().filter(effect -> !family.supports(effect.stat()))
                .forEach(effect -> violations.add(node.id() + ": " + effect.stat() + " does not apply to " + family));
        node.tagged().stream().filter(tagged -> !tagged.appliesTo(family))
                .forEach(tagged -> violations.add(node.id() + ": tagged " + tagged.effect().stat() + " does not apply to " + family));
        Stream.concat(node.fixed().keySet().stream(), node.ceilings().keySet().stream()).filter(stat -> !family.supportsAbsolute(stat))
                .forEach(stat -> violations.add(node.id() + ": absolute " + stat + " does not apply to " + family));
        for (MegaPassiveNode.AttributeScaling scaling : node.scaling()) {
            boolean attribute = scaling.attribute() == MachineStat.CONTROL || scaling.attribute() == MachineStat.DRIVE || scaling.attribute() == MachineStat.RESERVE;
            if (!attribute || !family.supports(scaling.stat())) { violations.add(node.id() + ": scaling " + scaling.stat() + " does not apply to " + family); }
        }
        node.behaviors().stream().filter(behavior -> !family.supportsBehavior(behavior))
                .forEach(behavior -> violations.add(node.id() + ": behavior " + behavior + " is not declared for " + family));
        node.passive().keySet().stream().filter(stat -> !family.supports(stat))
                .forEach(stat -> violations.add(node.id() + ": passive " + stat + " does not apply to " + family));
        if (node.recipeHardnessCeiling() > 0 && !family.supports(MachineStat.PROCESSING_LEVEL)) {
            violations.add(node.id() + ": a hardness ceiling does not apply to " + family);
        }
        return violations;
    }

    private static Map<String, Ascendancy> load() {
        Map<String, Ascendancy> catalog = new LinkedHashMap<>();
        JsonObject index = MasteryNodeJson.resource(DIRECTORY + "index.json");
        if (index == null) { throw new IllegalStateException("Missing ascendancy index"); }
        readIndex(index, catalog);
        JsonObject fixtures = MasteryNodeJson.resource(DIRECTORY + FIXTURE_INDEX);
        if (fixtures != null) { readIndex(fixtures, catalog); }
        return Collections.unmodifiableMap(catalog);
    }

    private static void readIndex(JsonObject index, Map<String, Ascendancy> catalog) {
        if (index.get("version").getAsInt() != VERSION) { throw new IllegalStateException("Unsupported ascendancy catalog version"); }
        for (JsonElement file : MasteryNodeJson.array(index, "ascendancies")) {
            JsonObject raw = MasteryNodeJson.resource(DIRECTORY + file.getAsString());
            if (raw == null) { throw new IllegalStateException("Missing ascendancy file " + file.getAsString()); }
            Ascendancy ascendancy = parse(raw);
            if (catalog.put(ascendancy.id(), ascendancy) != null) { throw new IllegalStateException("Duplicate ascendancy " + ascendancy.id()); }
        }
    }
}
