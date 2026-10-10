package com.rngtech.rpg.progression;

import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineModifierEffect;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.ModifierOperation;
import com.rngtech.rpg.ModifierSlot;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public final class MegaPassiveTree {
    public static final int VERSION = 2;
    /** Levels grant 99 points; the remaining capacity is reserved for future non-level point sources. */
    public static final int MAX_ALLOCATIONS = 120;
    /** Disabled bonus output: Output Amount cannot exceed the base 1x, and duplication chances drop to zero. */
    public static final Map<MachineStat, Double> NO_BONUS_OUTPUT_CEILINGS = Map.of(
            MachineStat.OUTPUT_AMOUNT, 1.0, MachineStat.SUPER_OUTPUT_CHANCE, 0.0, MachineStat.CRUSHER_SALVAGE_CHANCE, 0.0);
    private static final Map<String, MegaPassiveNode> CATALOG = load();
    public static final PassiveTree<MegaPassiveNode> TREE = new PassiveTree<>(List.copyOf(CATALOG.values()));

    private MegaPassiveTree() { }

    public static MegaPassiveNode node(String id) { return CATALOG.get(id); }
    public static MegaPassiveNode byIndex(int index) {
        return index >= 0 && index < TREE.nodes().size() ? TREE.nodes().get(index) : null;
    }
    public static MegaPassiveNode byButtonId(int id) { return TREE.byButtonId(id); }

    /** Shared-tree allocations followed by the chosen ascendancy's root and allocated nodes. */
    public static List<MasteryEffectSource> sources(MachineProgressionState state) {
        List<MasteryEffectSource> sources = new ArrayList<>();
        state.allocatedNodes().stream().map(CATALOG::get).filter(java.util.Objects::nonNull).forEach(sources::add);
        sources.addAll(AscendancyCatalog.allocated(state));
        return sources;
    }

    private static List<MasteryEffectSource> sources(MachineProgressionState state, MachineMasteryFamily family) {
        List<MasteryEffectSource> sources = new ArrayList<>();
        state.allocatedNodes().stream().map(CATALOG::get).filter(java.util.Objects::nonNull).forEach(sources::add);
        sources.addAll(AscendancyCatalog.allocated(state, family));
        return sources;
    }

    /** Every behavior granted by allocated shared-tree and ascendancy nodes. */
    public static Set<String> behaviors(MachineProgressionState state) {
        Set<String> behaviors = new HashSet<>();
        sources(state).forEach(source -> behaviors.addAll(source.behaviors()));
        return Set.copyOf(behaviors);
    }

    public static boolean has(MachineProgressionState state, String behavior) {
        return sources(state).stream().anyMatch(source -> source.behaviors().contains(behavior));
    }

    public static int passive(MachineProgressionState state, PassiveStatType stat) {
        return sources(state).stream().mapToInt(source -> source.passive().getOrDefault(stat, 0)).sum();
    }

    public static boolean acceptsHardness(MachineProgressionState state, int hardness) {
        return sources(state).stream().allMatch(source -> source.recipeHardnessCeiling() <= 0 || hardness <= source.recipeHardnessCeiling());
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

    /**
     * The nodes of {@code order} that still form a legal build from {@code start}. Retired, duplicate and disconnected ids
     * are dropped, so a node removed from the tree refunds its point instead of voiding the rest of the build.
     */
    public static List<String> salvage(String start, List<String> order) {
        MegaPassiveNode root = node(start);
        if (root == null || root.kind() != PassiveNodeKind.STARTER) { return List.of(); }
        Set<String> connected = new HashSet<>(Set.of(start));
        List<String> remaining = new ArrayList<>(order);
        List<String> kept = new ArrayList<>();
        boolean changed = true;
        while (changed && kept.size() < MAX_ALLOCATIONS) {
            changed = false;
            for (var iterator = remaining.iterator(); iterator.hasNext() && kept.size() < MAX_ALLOCATIONS; ) {
                String id = iterator.next();
                MegaPassiveNode node = node(id);
                if (node == null || node.kind() == PassiveNodeKind.STARTER || connected.contains(id)) { iterator.remove(); continue; }
                if (node.links().stream().anyMatch(connected::contains)) {
                    connected.add(id); kept.add(id); iterator.remove(); changed = true;
                }
            }
        }
        return List.copyOf(kept);
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
     * order. Foreign starts are never part of a path. Returns an empty list when the target is allocated or unreachable.
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
        double[] base = switch (family) {
            case CRUSHER -> new double[] {0, 20, 0};
            case FURNACE, ALLOY_FURNACE -> new double[] {0, 10, 10};
            case FORESTRY -> new double[] {10, 10, 0};
            case METAL_PRESS, RESONANCE_CALIBRATOR -> new double[] {20, 0, 0};
            case MELTER -> new double[] {10, 0, 10};
        };
        MachineStat[] attributes = {MachineStat.CONTROL, MachineStat.DRIVE, MachineStat.RESERVE};
        Ascendancy chosen = AscendancyCatalog.get(state.ascendancy());
        stats.setAscendancy(chosen != null && chosen.family() == family ? chosen.id() : "");
        try (MachineStatAccumulator.Source ignored = stats.source(Component.translatable("rngtech.stat.breakdown.source.inherent"))) {
            for (int i = 0; i < attributes.length; i++) { apply(stats, attributes[i], ModifierOperation.ADD, base[i]); }
        }
        List<MasteryEffectSource> allocated = sources(state, family);
        for (MasteryEffectSource node : allocated) {
            try (MachineStatAccumulator.Source ignored = stats.source(Component.translatable(node.translationKey()))) {
                for (MachineModifierEffect effect : node.effects()) {
                    if (family.supports(effect.stat())) { apply(stats, effect.stat(), effect.operation(), effect.value()); }
                }
                for (MegaPassiveNode.TaggedEffect tagged : node.tagged()) {
                    if (tagged.appliesTo(family)) { apply(stats, tagged.effect().stat(), tagged.effect().operation(), tagged.effect().value()); }
                }
                node.fixed().forEach((stat, value) -> { if (family.supportsAbsolute(stat)) { stats.setAbsolute(stat, value); } });
                node.ceilings().forEach((stat, value) -> { if (family.supportsAbsolute(stat)) { stats.capAbsolute(stat, value); } });
            }
        }
        if (has(state, "NO_BONUS_OUTPUT")) {
            try (MachineStatAccumulator.Source ignored = stats.source(behaviorSource(allocated, "NO_BONUS_OUTPUT"))) {
                NO_BONUS_OUTPUT_CEILINGS.forEach((stat, value) -> { if (family.supportsAbsolute(stat)) { stats.capAbsolute(stat, value); } });
            }
        }
        if (!has(state, "NO_INHERENT_ATTRIBUTES")) {
            for (Conversion conversion : inherentConversions(family, stats.value(MachineStat.CONTROL), stats.value(MachineStat.DRIVE), stats.value(MachineStat.RESERVE))) {
                MachineModifierEffect effect = conversion.effect();
                try (MachineStatAccumulator.Source ignored = stats.source(Component.translatable(
                        "rngtech.stat.breakdown.source.attribute", Component.translatable(conversion.attribute().translationKey())))) {
                    apply(stats, effect.stat(), effect.operation(), effect.value());
                }
            }
        }
        for (MasteryEffectSource node : allocated) {
            for (MegaPassiveNode.AttributeScaling scaling : node.scaling()) {
                if (family.supports(scaling.stat())) {
                    try (MachineStatAccumulator.Source ignored = stats.source(Component.translatable(
                            "rngtech.stat.breakdown.source.scaling",
                            Component.translatable(node.translationKey()),
                            Component.translatable(scaling.attribute().translationKey())))) {
                        apply(stats, scaling.stat(), scaling.operation(), Math.max(0, stats.value(scaling.attribute())) * scaling.perPoint());
                    }
                }
            }
        }
    }

    /** The allocated node that grants {@code behavior}, as a stat breakdown source label. */
    public static Component behaviorSource(MachineProgressionState state, String behavior) {
        return behaviorSource(sources(state), behavior);
    }

    private static Component behaviorSource(List<MasteryEffectSource> allocated, String behavior) {
        for (MasteryEffectSource node : allocated) {
            if (node.behaviors().contains(behavior)) {
                return Component.translatable(node.translationKey());
            }
        }
        return Component.translatable("rngtech.stat.breakdown.source.machine");
    }

    /** An attribute's inherent contribution to one stat. */
    public record Conversion(MachineStat attribute, MachineModifierEffect effect) {
    }

    /** Inherent attribute conversions for {@code family}, from final attribute totals; negative totals convert nothing. */
    public static List<Conversion> inherentConversions(MachineMasteryFamily family, double control, double drive, double reserve) {
        control = Math.max(0, control);
        drive = Math.max(0, drive);
        reserve = Math.max(0, reserve);
        List<Conversion> conversions = new ArrayList<>();
        conversions.add(conversion(MachineStat.CONTROL, MachineStat.STABILITY, ModifierOperation.INCREASED_PERCENT, control * 0.1));
        conversions.add(conversion(MachineStat.CONTROL, MachineStat.ENERGY_USAGE, ModifierOperation.DECREASED_PERCENT, control * 0.02));
        conversions.add(conversion(MachineStat.DRIVE, MachineStat.PROCESSING_SPEED, ModifierOperation.INCREASED_PERCENT, drive * 0.15));
        if (family == MachineMasteryFamily.FORESTRY) {
            conversions.add(conversion(MachineStat.DRIVE, MachineStat.TREE_FELL_LIMIT, ModifierOperation.ADD, Math.floor(drive / 20)));
        } else {
            conversions.add(conversion(MachineStat.RESERVE, MachineStat.ENERGY_CAPACITY, ModifierOperation.INCREASED_PERCENT, reserve * 0.25));
        }
        switch (family) {
            case FURNACE, ALLOY_FURNACE -> {
                conversions.add(conversion(MachineStat.CONTROL, MachineStat.TEMPERATURE_STABILITY, ModifierOperation.INCREASED_PERCENT, control * 0.05));
                conversions.add(conversion(MachineStat.RESERVE, MachineStat.HEAT_ISOLATION, ModifierOperation.INCREASED_PERCENT, reserve * 0.1));
            }
            case METAL_PRESS -> conversions.add(conversion(MachineStat.CONTROL, MachineStat.TEMPERATURE_STABILITY, ModifierOperation.INCREASED_PERCENT, control * 0.05));
            case MELTER -> conversions.add(conversion(MachineStat.RESERVE, MachineStat.FLUID_TRANSFER, ModifierOperation.INCREASED_PERCENT, reserve * 0.1));
            case RESONANCE_CALIBRATOR -> conversions.add(conversion(MachineStat.CONTROL, MachineStat.CALIBRATION_PRECISION, ModifierOperation.INCREASED_PERCENT, control * 0.05));
            default -> { }
        }
        return List.copyOf(conversions);
    }

    private static Conversion conversion(MachineStat attribute, MachineStat stat, ModifierOperation operation, double value) {
        return new Conversion(attribute, MachineModifierEffect.fixed(stat, operation, value));
    }

    private static void apply(MachineStatAccumulator stats, MachineStat stat, ModifierOperation operation, double value) {
        stats.apply(new MachineModifier(ModifierSlot.IMPLICIT, stat, operation, value));
    }

    private static Map<String, MegaPassiveNode> load() {
        JsonObject data = MasteryNodeJson.resource("/data/rngtech/mastery/machine_tree.json");
        if (data == null) { throw new IllegalStateException("Missing shared machine tree"); }
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
            MasteryNodeJson.Effects e = MasteryNodeJson.effects(raw);
            MegaPassiveNode node = new MegaPassiveNode(nodes.size(), id, raw.get("name").getAsString(), PassiveNodeKind.valueOf(raw.get("kind").getAsString()), raw.get("x").getAsInt(), raw.get("y").getAsInt(), List.copyOf(links.getOrDefault(id, List.of())), e.effects(), e.tagged(), e.behaviors(), e.fixed(), e.ceilings(), e.scaling(), e.passive(), e.recipeHardnessCeiling());
            if (nodes.put(id, node) != null) { throw new IllegalStateException("Duplicate mastery node " + id); }
        }
        for (MegaPassiveNode node : nodes.values()) {
            if (node.links().stream().anyMatch(id -> !nodes.containsKey(id)) || new HashSet<>(node.links()).size() != node.links().size()) { throw new IllegalStateException("Invalid links: " + node.id()); }
        }
        return java.util.Collections.unmodifiableMap(nodes);
    }
}
