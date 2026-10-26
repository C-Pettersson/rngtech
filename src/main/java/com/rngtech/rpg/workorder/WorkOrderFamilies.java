package com.rngtech.rpg.workorder;

import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.progression.MachineMasteryFamily;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The machine kinds Work Orders name, by id. A Mastery machine's id is its Mastery family's. Machines without Mastery
 * join here when they get objectives. A stageless machine, such as the Component Assembler, has no chassis stage: its
 * chips need none, while the tier still caps which outputs a job can name.
 */
public final class WorkOrderFamilies {
    public static final String COMPONENT_ASSEMBLER = "component_assembler";
    public static final String COMPONENT_RECYCLER = "component_recycler";
    private static final Set<String> STAGELESS = Set.of(COMPONENT_ASSEMBLER);
    /** Machines without Mastery that have chassis stages, and their highest stage. */
    private static final Map<String, Integer> STAGED = Map.of(COMPONENT_RECYCLER, 8);

    /**
     * The stats each family's stat goals may name: effective stats players raise through Gear, refinement, or Mastery and
     * the family's machine reads. A goal never grants a stat, it only asks for one.
     */
    private static final Map<String, Set<MachineStat>> GOAL_STATS = Map.of(
            "crusher", Set.of(MachineStat.PROCESSING_SPEED, MachineStat.SUPER_OUTPUT_CHANCE, MachineStat.OUTPUT_AMOUNT,
                    MachineStat.INSTANT_PROCESS_CHANCE, MachineStat.BATCH_SIZE),
            "furnace", Set.of(MachineStat.PROCESSING_SPEED, MachineStat.SUPER_OUTPUT_CHANCE, MachineStat.HEAT_TRANSFER,
                    MachineStat.TEMPERATURE_STABILITY, MachineStat.MAX_TEMPERATURE, MachineStat.BATCH_SIZE));

    /**
     * The groups each family's machines report, in a fixed order the menu sync uses: Metal Press molds and calibration
     * families. Group jobs may only name these.
     */
    private static final Map<String, List<String>> GROUPS = Map.of(
            "metal_press", List.of("plate", "gear", "casing", "connector"),
            "resonance_calibrator", List.of("structural", "kinetic", "thermal", "conductive", "storage", "logic"),
            "forestry", List.of("logs", "crops"));
    private static final List<String> ALL_GROUPS = GROUPS.values().stream().flatMap(List::stream).distinct().sorted().toList();

    /** The families whose machines report failures that break a streak. */
    private static final Set<String> STREAKS = Set.of("metal_press", "resonance_calibrator");

    private WorkOrderFamilies() { }

    /** Whether a group job for {@code id} may name {@code group}. */
    public static boolean group(String id, String group) {
        return GROUPS.getOrDefault(id, List.of()).contains(group);
    }

    /** Every family's groups, sorted, so the menu can sync a group by its index. */
    public static List<String> allGroups() {
        return ALL_GROUPS;
    }

    /** Whether streak jobs can roll for {@code id}: its machines report the failures that break a streak. */
    public static boolean streaks(String id) {
        return STREAKS.contains(id);
    }

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
        return STAGED.getOrDefault(id, 0);
    }

    public static boolean known(String id) {
        return topStage(id) > 0 || STAGELESS.contains(id);
    }

    /** Whether machines of {@code id} have chassis stages a chip's stage requirement checks. */
    public static boolean staged(String id) {
        return !STAGELESS.contains(id);
    }

    /**
     * The stage that caps what a job of {@code tier} can name, such as its item options: the tier's stage, capped at the
     * family's top stage when it has stages.
     */
    public static int stageCap(String id, int tier) {
        return staged(id) ? Math.min(WorkOrder.tierStage(tier), topStage(id)) : WorkOrder.tierStage(tier);
    }

    /** The chassis stage a job of {@code tier} needs: the stage cap, or none on a stageless machine. */
    public static int requiredStage(String id, int tier) {
        return staged(id) ? stageCap(id, tier) : 0;
    }
}
