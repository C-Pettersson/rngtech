package com.rngtech.rpg.progression;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A family-specific ascendancy tree: a free root and its non-root nodes in authored order. */
public record Ascendancy(String id, MachineMasteryFamily family, AscendancyNode root, Map<String, AscendancyNode> nodes) {
    public String translationKey() { return "rngtech.mastery.ascendancy." + id; }

    /** A non-root node, or {@code null}. */
    public AscendancyNode node(String nodeId) { return nodes.get(nodeId); }

    /** A non-root node's position in authored order, or -1. */
    public int nodeIndex(String nodeId) {
        int index = 0;
        for (String id : nodes.keySet()) {
            if (id.equals(nodeId)) { return index; }
            index++;
        }
        return -1;
    }

    public AscendancyNode nodeAt(int index) {
        return index < 0 || index >= nodes.size() ? null : nodes.values().stream().skip(index).findFirst().orElse(null);
    }

    public List<AscendancyNode> children(String parentId) {
        return nodes.values().stream().filter(node -> node.parent().equals(parentId)).toList();
    }

    /** A notable reached through a small node that sits behind another notable. */
    public boolean isDeep(AscendancyNode node) {
        if (node.kind() != AscendancyNode.Kind.NOTABLE) { return false; }
        AscendancyNode small = nodes.get(node.parent());
        return small != null && nodes.containsKey(small.parent()) && nodes.get(small.parent()).kind() == AscendancyNode.Kind.NOTABLE;
    }

    /** Whether every node in {@code allocated} is a known non-root node whose parent is the root or also allocated. */
    public boolean connected(Collection<String> allocated) {
        for (String id : allocated) {
            AscendancyNode node = nodes.get(id);
            if (node == null || !(node.parent().equals(root.id()) || allocated.contains(node.parent()))) { return false; }
        }
        return true;
    }

    /** Whether {@code order} allocates known, distinct non-root nodes, each after its parent. */
    public boolean validOrder(List<String> order) {
        Set<String> seen = new HashSet<>();
        for (String id : order) {
            AscendancyNode node = nodes.get(id);
            if (node == null || !(node.parent().equals(root.id()) || seen.contains(node.parent())) || !seen.add(id)) { return false; }
        }
        return true;
    }
}
