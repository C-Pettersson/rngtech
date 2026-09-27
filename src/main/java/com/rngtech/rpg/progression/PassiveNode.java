package com.rngtech.rpg.progression;

import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineModifierEffect;

import java.util.List;
import java.util.Set;

public interface PassiveNode {
    int index();

    int requiredLevel();

    PassiveNodeKind kind();

    int x();

    int y();

    boolean alwaysAllocated();

    boolean grantsNothing();

    List<? extends PassiveNode> parents();

    List<MachineModifierEffect> effects();

    MachineModifier modifier();

    String masteryIconKey();

    String translationKey();

    Set<PassiveNodeFlag> flags();

    int passiveStat(PassiveStatType stat);

    default long mask() {
        return 1L << (index() % Long.SIZE);
    }

    default int maskBank() {
        return index() / Long.SIZE;
    }

    default int buttonId() {
        return 100 + index();
    }

    default int size() {
        return kind().size();
    }

    default List<PassiveTreeLayouts.Point> linkPathTo(PassiveNode other) {
        return List.of(
                PassiveTreeLayouts.Point.of(x() + size() / 2, y() + size() / 2),
                PassiveTreeLayouts.Point.of(other.x() + other.size() / 2, other.y() + other.size() / 2)
        );
    }

    default boolean isUnlocked(PassiveProgressionView progression) {
        return alwaysAllocated() || progression.hasNode(index());
    }

    /**
     * Parentless nodes are connected only when they are always allocated.
     */
    default boolean parentUnlocked(PassiveProgressionView progression) {
        if (alwaysAllocated()) {
            return true;
        }
        for (PassiveNode parent : parents()) {
            if (parent.isUnlocked(progression)) {
                return true;
            }
        }
        return false;
    }
}
