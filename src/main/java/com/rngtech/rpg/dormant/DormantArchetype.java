package com.rngtech.rpg.dormant;

import com.rngtech.rpg.MachineModifierEffect;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MegaPassiveNode;
import com.rngtech.rpg.progression.MegaPassiveTree;
import com.rngtech.rpg.progression.PassiveNodeKind;
import com.rngtech.rpg.progression.PassiveStatType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * Builds the followed build a dormant soul carries. A site may name target nodes; otherwise the build walks from the
 * family's start to a random pick among the nearest notables the family can use, then on from there, so it never
 * spends points on a keystone's trade-off or a stat the machine ignores.
 */
public final class DormantArchetype {
    /** How many of the nearest usable notables each step picks from. */
    private static final int NEAREST = 4;
    private static final int UNREACHED = -2;
    private static final int ROOT = -1;

    private DormantArchetype() {
    }

    /** The allocation order through {@code targets}, or an empty build when a target cannot be reached. */
    public static List<String> through(MachineMasteryFamily family, List<String> targets, int maxPoints) {
        Set<String> allocated = new HashSet<>(Set.of(family.startNodeId()));
        List<String> order = new ArrayList<>();
        for (String id : targets) {
            MegaPassiveNode target = MegaPassiveTree.node(id);
            if (target == null || allocated.contains(id)) { continue; }
            int[] previous = search(allocated);
            if (previous[target.index()] == UNREACHED) { return List.of(); }
            order.addAll(path(previous, target, allocated));
            if (order.size() >= maxPoints) { break; }
        }
        return trimmed(family, order, maxPoints);
    }

    /** A generated build of up to {@code maxPoints} nodes through notables {@code family} can use. */
    public static List<String> generate(MachineMasteryFamily family, RandomGenerator random, int maxPoints) {
        Set<String> allocated = new HashSet<>(Set.of(family.startNodeId()));
        List<String> order = new ArrayList<>();
        while (order.size() < maxPoints) {
            int[] previous = search(allocated);
            int[] distance = distances(previous);
            List<MegaPassiveNode> candidates = MegaPassiveTree.TREE.nodes().stream()
                    .filter(node -> previous[node.index()] != UNREACHED && !allocated.contains(node.id()) && usable(family, node))
                    .sorted((a, b) -> Integer.compare(distance[a.index()], distance[b.index()]))
                    .limit(NEAREST)
                    .toList();
            if (candidates.isEmpty()) { break; }
            order.addAll(path(previous, candidates.get(random.nextInt(candidates.size())), allocated));
        }
        return trimmed(family, order, maxPoints);
    }

    /** A notable whose every effect the family reads, with no behavior attached. */
    static boolean usable(MachineMasteryFamily family, MegaPassiveNode node) {
        if (node.kind() != PassiveNodeKind.NOTABLE || !node.behaviors().isEmpty() || node.grantsNothing()) { return false; }
        for (MachineModifierEffect effect : node.effects()) {
            if (!family.supports(effect.stat())) { return false; }
        }
        for (MegaPassiveNode.TaggedEffect tagged : node.tagged()) {
            if (!tagged.appliesTo(family)) { return false; }
        }
        for (PassiveStatType stat : node.passive().keySet()) {
            if (!family.supports(stat)) { return false; }
        }
        return node.fixed().keySet().stream().allMatch(family::supportsAbsolute)
                && node.ceilings().keySet().stream().allMatch(family::supportsAbsolute)
                && node.scaling().stream().allMatch(scaling -> family.supports(scaling.stat()));
    }

    /** Breadth-first predecessors from the allocated nodes, never through starts or keystones. */
    private static int[] search(Set<String> allocated) {
        List<MegaPassiveNode> nodes = MegaPassiveTree.TREE.nodes();
        int[] previous = new int[nodes.size()];
        Arrays.fill(previous, UNREACHED);
        ArrayDeque<MegaPassiveNode> queue = new ArrayDeque<>();
        for (String id : allocated) {
            MegaPassiveNode node = MegaPassiveTree.node(id);
            previous[node.index()] = ROOT;
            queue.add(node);
        }
        while (!queue.isEmpty()) {
            MegaPassiveNode current = queue.remove();
            for (String id : current.links()) {
                MegaPassiveNode next = MegaPassiveTree.node(id);
                if (next == null || previous[next.index()] != UNREACHED
                        || next.kind() == PassiveNodeKind.STARTER || next.kind() == PassiveNodeKind.KEYSTONE) {
                    continue;
                }
                previous[next.index()] = current.index();
                queue.add(next);
            }
        }
        return previous;
    }

    private static int[] distances(int[] previous) {
        int[] distance = new int[previous.length];
        for (int index = 0; index < previous.length; index++) {
            int steps = 0;
            for (int at = index; at >= 0 && previous[at] != ROOT; at = previous[at]) { steps++; }
            distance[index] = steps;
        }
        return distance;
    }

    private static List<String> path(int[] previous, MegaPassiveNode target, Set<String> allocated) {
        List<String> path = new ArrayList<>();
        for (int index = target.index(); previous[index] != ROOT; index = previous[index]) {
            path.addFirst(MegaPassiveTree.byIndex(index).id());
        }
        allocated.addAll(path);
        return path;
    }

    private static List<String> trimmed(MachineMasteryFamily family, List<String> order, int maxPoints) {
        List<String> build = List.copyOf(order.subList(0, Math.min(order.size(), Math.max(0, maxPoints))));
        return build.isEmpty() || MegaPassiveTree.validBuild(family.startNodeId(), build) ? build : List.of();
    }
}
