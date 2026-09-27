package com.rngtech.rpg;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ModifierLensTargets {
    public static List<ModifierDefinition> globalTargets(Set<ModifierLensTag> lensTags) {
        if (lensTags.isEmpty()) {
            return List.of();
        }
        Map<DefinitionKey, ModifierDefinition> definitions = new LinkedHashMap<>();
        for (ModifierEligibilityProfile profile : ModifierEligibilityProfiles.allProfiles()) {
            for (ModifierDefinition definition : profile.definitions()) {
                if (definition.canTargetWithLens() && definition.matchesLensTags(lensTags)) {
                    definitions.putIfAbsent(DefinitionKey.of(definition), definition);
                }
            }
        }
        return definitions.values().stream()
                .sorted(Comparator
                        .comparing(ModifierDefinition::slot)
                        .thenComparing(ModifierDefinition::id))
                .toList();
    }

    public static MatchSummary summarizeDefinitions(
            List<ModifierDefinition> definitions,
            Set<ModifierLensTag> lensTags
    ) {
        if (lensTags.isEmpty()) {
            return new MatchSummary(0, definitions.size());
        }
        int matching = 0;
        for (ModifierDefinition definition : definitions) {
            if (definition.matchesLensTags(lensTags)) {
                matching++;
            }
        }
        return new MatchSummary(matching, definitions.size() - matching);
    }

    public static List<ModifierDefinition> matchingDefinitions(
            List<ModifierDefinition> definitions,
            Set<ModifierLensTag> lensTags
    ) {
        if (lensTags.isEmpty()) {
            return List.of();
        }
        return definitions.stream()
                .filter(definition -> definition.matchesLensTags(lensTags))
                .toList();
    }

    public static MatchSummary summarizeModifiers(
            ModifierEligibilityProfile profile,
            List<MachineModifier> modifiers,
            Set<ModifierLensTag> lensTags
    ) {
        if (lensTags.isEmpty()) {
            return new MatchSummary(0, modifiers.size());
        }
        int matching = 0;
        for (MachineModifier modifier : modifiers) {
            ModifierDefinition definition = definitionFor(profile, modifier);
            if (definition != null && definition.matchesLensTags(lensTags)) {
                matching++;
            }
        }
        return new MatchSummary(matching, modifiers.size() - matching);
    }

    public static List<MachineModifier> matchingModifiers(
            ModifierEligibilityProfile profile,
            List<MachineModifier> modifiers,
            Set<ModifierLensTag> lensTags
    ) {
        if (lensTags.isEmpty()) {
            return List.of();
        }
        List<MachineModifier> matching = new ArrayList<>();
        for (MachineModifier modifier : modifiers) {
            ModifierDefinition definition = definitionFor(profile, modifier);
            if (definition != null && definition.matchesLensTags(lensTags)) {
                matching.add(modifier);
            }
        }
        return matching;
    }

    public static ModifierDefinition definitionFor(ModifierEligibilityProfile profile, MachineModifier modifier) {
        return profile.definitions().stream()
                .filter(ModifierDefinition::canRoll)
                .filter(definition -> definition.matches(modifier))
                .findFirst()
                .orElse(null);
    }

    private record DefinitionKey(ModifierSlot slot, String id) {
        private static DefinitionKey of(ModifierDefinition definition) {
            return new DefinitionKey(definition.slot(), definition.id());
        }
    }

    public record MatchSummary(int matchingCount, int stillPossibleCount) {
    }

    private ModifierLensTargets() {
    }
}
