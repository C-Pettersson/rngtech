package com.rngtech.rpg;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ModifierEligibilityProfile(
        String id,
        Set<ModifierCapability> capabilities,
        List<ModifierDefinition> definitions,
        List<MachineBehavior> rollableBehaviors
) {
    public ModifierEligibilityProfile(String id, Set<ModifierCapability> capabilities, List<ModifierDefinition> definitions) {
        this(id, capabilities, definitions, List.of());
    }

    public ModifierEligibilityProfile {
        Objects.requireNonNull(id, "id");
        capabilities = Set.copyOf(capabilities);
        definitions = List.copyOf(definitions);
        rollableBehaviors = List.copyOf(rollableBehaviors);
    }

    public boolean hasCapability(ModifierCapability capability) {
        return capabilities.contains(capability);
    }

    public List<ModifierDefinition> rollableDefinitions(ModifierSlot slot) {
        return definitions.stream()
                .filter(ModifierDefinition::canRoll)
                .filter(definition -> definition.slot() == slot)
                .toList();
    }

    public List<ModifierDefinition> lensTargetableDefinitions(ModifierSlot slot) {
        return rollableDefinitions(slot).stream()
                .filter(ModifierDefinition::canTargetWithLens)
                .toList();
    }

}
