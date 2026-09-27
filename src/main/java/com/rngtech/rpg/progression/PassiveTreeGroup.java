package com.rngtech.rpg.progression;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record PassiveTreeGroup(
        String id,
        PassiveTreeLayouts.Point center,
        Map<Integer, List<Integer>> orbitOccupancy,
        List<String> nodeIds
) {
    public PassiveTreeGroup {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Passive tree group id must not be blank");
        }
        center = Objects.requireNonNull(center, "center");
        Map<Integer, List<Integer>> copiedOrbitOccupancy = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<Integer>> entry : Objects.requireNonNull(orbitOccupancy, "orbitOccupancy").entrySet()) {
            copiedOrbitOccupancy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        orbitOccupancy = Map.copyOf(copiedOrbitOccupancy);
        nodeIds = List.copyOf(Objects.requireNonNull(nodeIds, "nodeIds"));
        if (nodeIds.isEmpty()) {
            throw new IllegalArgumentException("Passive tree group must contain at least one node: " + id);
        }
    }
}
