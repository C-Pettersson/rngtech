package com.rngtech.rpg.progression;

import com.rngtech.rpg.MachineStatAccumulator;

import java.util.List;
import java.util.Objects;

public final class PassiveTree<N extends PassiveNode> {
    private static final int MAX_NODE_COUNT = 4096;

    private final List<N> nodes;

    public PassiveTree(List<N> nodes) {
        Objects.requireNonNull(nodes, "nodes");
        if (nodes.size() > MAX_NODE_COUNT) {
            throw new IllegalStateException("Passive tree supports at most " + MAX_NODE_COUNT + " nodes");
        }
        this.nodes = List.copyOf(nodes);
        for (int index = 0; index < this.nodes.size(); index++) {
            N node = Objects.requireNonNull(this.nodes.get(index), "nodes[" + index + "]");
            if (node.index() != index) {
                throw new IllegalStateException("Passive node " + node + " has index " + node.index() + " at position " + index);
            }
        }
    }

    public void applyStats(MachineStatAccumulator stats, PassiveProgressionView progression) {
        for (N node : nodes) {
            if (!node.grantsNothing() && node.isUnlocked(progression)) {
                stats.apply(node.modifier());
            }
        }
    }

    public boolean canUnlock(N node, PassiveProgressionView progression) {
        return node != null
                && !node.isUnlocked(progression)
                && !node.alwaysAllocated()
                && node.kind() != PassiveNodeKind.STARTER
                && progression.level() >= node.requiredLevel()
                && progression.unspentPoints() > 0
                && node.parentUnlocked(progression);
    }

    public boolean hasUnlockedFlag(PassiveNodeFlag flag, PassiveProgressionView progression) {
        for (N node : nodes) {
            if (node.isUnlocked(progression) && node.flags().contains(flag)) {
                return true;
            }
        }
        return false;
    }

    public int statTotal(PassiveStatType stat, PassiveProgressionView progression) {
        int total = 0;
        for (N node : nodes) {
            if (node.isUnlocked(progression)) {
                total += node.passiveStat(stat);
            }
        }
        return total;
    }

    public N byButtonId(int buttonId) {
        int index = buttonId - 100;
        return index >= 0 && index < nodes.size() ? nodes.get(index) : null;
    }

    public List<N> nodes() {
        return nodes;
    }

    public int contentMaxX() {
        int contentMax = 0;
        for (N node : nodes) {
            contentMax = Math.max(contentMax, node.x() + node.size());
        }
        return contentMax;
    }

    public int contentMaxY() {
        int contentMax = 0;
        for (N node : nodes) {
            contentMax = Math.max(contentMax, node.y() + node.size());
        }
        return contentMax;
    }
}
