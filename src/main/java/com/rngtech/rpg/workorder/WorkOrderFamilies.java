package com.rngtech.rpg.workorder;

import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.progression.MachineMasteryFamily;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The machine kinds Work Orders name, by id. A Mastery machine's id is its Mastery family's. Machines without Mastery
 * join here when they get objectives.
 */
public final class WorkOrderFamilies {
    /**
     * The stats each family's stat goals may name: effective stats players raise through Gear, refinement, or Mastery and
     * the family's machine reads. A goal never grants a stat, it only asks for one.
     */
    private static final Map<String, Set<MachineStat>> GOAL_STATS = Map.of(
            "crusher", Set.of(MachineStat.PROCESSING_SPEED, MachineStat.SUPER_OUTPUT_CHANCE, MachineStat.OUTPUT_AMOUNT,
                    MachineStat.INSTANT_PROCESS_CHANCE, MachineStat.BATCH_SIZE),
            "furnace", Set.of(MachineStat.PROCESSING_SPEED, MachineStat.SUPER_OUTPUT_CHANCE, MachineStat.HEAT_TRANSFER,
                    MachineStat.TEMPERATURE_STABILITY, MachineStat.MAX_TEMPERATURE, MachineStat.BATCH_SIZE));

    private WorkOrderFamilies() { }

    /** Whether a stat goal for {@code id} may name {@code stat}. */
    public static boolean goalStat(String id, MachineStat stat) {
        return stat != null && GOAL_STATS.getOrDefault(id, Set.of()).contains(stat);
    }

    public static String of(MachineMasteryFamily family) {
        return family.name().toLowerCase(Locale.ROOT);
    }

    /** The highest stage any machine of {@code id} reaches, or 0 for an id no machine uses. */
    public static int topStage(String id) {
        for (MachineMasteryFamily family : MachineMasteryFamily.values()) {
            if (of(family).equals(id)) { return family.topStage(); }
        }
        return 0;
    }

    public static boolean known(String id) {
        return topStage(id) > 0;
    }
}
