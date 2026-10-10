package com.rngtech.rpg.corruption;

import com.rngtech.rpg.CorruptionOutcome;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.Rarity;
import com.rngtech.rpg.refinement.RefinementEngine;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The outcome table a Volatile Catalyst rolls on one target, after the fallbacks the engine applies: Reforged or Warped
 * with nothing to change, and Blessed or Blighted with an empty pool, become Untouched.
 */
public final class CorruptionPreview {
    public record Row(CorruptionOutcome outcome, double percent, List<CorruptionCatalog.Entry> entries) {
    }

    private CorruptionPreview() {
    }

    public static List<Row> rows(CorruptionCatalog catalog, String hostId, MachineTraits traits, boolean warded) {
        Map<CorruptionOutcome, Integer> weights = catalog.weights(warded);
        Map<CorruptionOutcome, Integer> effective = new EnumMap<>(CorruptionOutcome.class);
        for (CorruptionOutcome outcome : CorruptionOutcome.values()) {
            int weight = weights.getOrDefault(outcome, 0);
            CorruptionOutcome resolved = canResolve(catalog, hostId, traits, outcome) ? outcome : CorruptionOutcome.UNTOUCHED;
            effective.merge(resolved, weight, Integer::sum);
        }
        int total = effective.values().stream().mapToInt(Integer::intValue).sum();
        List<Row> rows = new ArrayList<>();
        for (CorruptionOutcome outcome : CorruptionOutcome.values()) {
            int weight = effective.getOrDefault(outcome, 0);
            double percent = total <= 0 ? 0.0 : weight * 100.0 / total;
            rows.add(new Row(outcome, percent, catalog.entries(hostId, outcome)));
        }
        return rows;
    }

    /** Each entry's chance within its pool, in percent. */
    public static double entryPercent(List<CorruptionCatalog.Entry> entries, CorruptionCatalog.Entry entry) {
        int total = entries.stream().mapToInt(CorruptionCatalog.Entry::weight).sum();
        return total <= 0 ? 0.0 : entry.weight() * 100.0 / total;
    }

    private static boolean canResolve(CorruptionCatalog catalog, String hostId, MachineTraits traits, CorruptionOutcome outcome) {
        return switch (outcome) {
            case UNTOUCHED -> true;
            case BLESSED, BLIGHTED -> !catalog.entries(hostId, outcome).isEmpty();
            case REFORGED -> traits.rarity() != Rarity.UNIQUE
                    && traits.modifiers().stream().anyMatch(modifier -> modifier.slot() == ModifierSlot.PREFIX
                    || modifier.slot() == ModifierSlot.SUFFIX);
            case WARPED -> traits.rarity() != Rarity.UNIQUE
                    && traits.modifiers().stream().anyMatch(RefinementEngine::isWarpable);
        };
    }
}
