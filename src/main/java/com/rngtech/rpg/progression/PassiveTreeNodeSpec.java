package com.rngtech.rpg.progression;

import java.util.List;
import java.util.Objects;

public record PassiveTreeNodeSpec<N extends Enum<N> & PassiveNode>(
        N node,
        String groupId,
        int orbit,
        int orbitIndex,
        PassiveNodeKind kind,
        List<N> linkedNodes
) {
    public PassiveTreeNodeSpec {
        node = Objects.requireNonNull(node, "node");
        if (groupId == null || groupId.isBlank()) {
            throw new IllegalArgumentException("Passive tree node group id must not be blank: " + node.name());
        }
        if (orbit < 0) {
            throw new IllegalArgumentException("Passive tree node orbit must be non-negative: " + node.name());
        }
        if (orbitIndex < 0) {
            throw new IllegalArgumentException("Passive tree node orbit index must be non-negative: " + node.name());
        }
        kind = Objects.requireNonNull(kind, "kind");
        linkedNodes = List.copyOf(Objects.requireNonNull(linkedNodes, "linkedNodes"));
    }

    public String nodeId() {
        return node.name();
    }
}
