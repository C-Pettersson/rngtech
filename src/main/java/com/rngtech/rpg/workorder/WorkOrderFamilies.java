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
     * Stages a family's stage jobs can name that no shipped recipe reaches, which Deep Ore skips: the Crusher has no
     * Processing Level 7 recipe and the Alloy Furnace no Stage 5 alloy.
     */
    private static final Map<String, Set<Integer>> EMPTY_STAGES = Map.of("crusher", Set.of(7), "alloy_furnace", Set.of(5));

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

    /**
     * Each recipe family's reference time in ticks, about its median recipe time per item it reports. Any-work, stage,
     * and setting jobs count an item as its recipe's time over this, so a quick recipe counts less than a slow one. The
     * Melter counts mB and the Forestry Companion has no recipes, so neither is listed.
     */
    private static final Map<String, Integer> WORK_TICKS = Map.of(
            "crusher", 120, "furnace", 200, "alloy_furnace", 600, "metal_press", 240, "resonance_calibrator", 300,
            COMPONENT_ASSEMBLER, 240, COMPONENT_RECYCLER, 144);

    /**
     * A good build's throughput at each tier's stage, in hundredths of base recipe speed: the Rush clock is set from it,
     * and so are job sizes for families whose jobs are sized by time. The Crusher and Furnace come from the processing
     * balance simulation (the 75th percentile build at Mastery level 1), raised where a stage's sample metal ran slower
     * than the stage below; the Crusher's Tier V stays at its Tier IV speed, so a Stage 8 job is not a huge mining trip.
     * The others are the bare chassis's best throughput at or below the stage, times the good-to-bare ratio of a similar
     * machine: about 1.5 for heated machines, from the Furnace, and 4 for the rest, from the Crusher.
     */
    private static final Map<String, int[]> JOB_SPEED = Map.of(
            "crusher", new int[] {450, 690, 690, 990, 990},
            "furnace", new int[] {160, 350, 430, 430, 490},
            "alloy_furnace", new int[] {165, 165, 183, 183, 183},
            "metal_press", new int[] {150, 150, 150, 150, 150},
            "resonance_calibrator", new int[] {460, 460, 510, 660, 660},
            COMPONENT_ASSEMBLER, new int[] {400, 400, 400, 400, 400},
            COMPONENT_RECYCLER, new int[] {420, 480, 528, 528, 580});

    /**
     * Families whose jobs are sized by the materials the work eats, such as catalysts, cell materials, or parts to
     * recycle, rather than by machine time. Their counts are set by hand; the speed table sets only their Rush clock.
     */
    private static final Set<String> MATERIAL_BOUND = Set.of("resonance_calibrator", COMPONENT_ASSEMBLER, COMPONENT_RECYCLER);

    private WorkOrderFamilies() { }

    /** Whether a group job for {@code id} may name {@code group}. */
    public static boolean group(String id, String group) {
        return GROUPS.getOrDefault(id, List.of()).contains(group);
    }

    /** Every family's groups, sorted, so the menu can sync a group by its index. */
    public static List<String> allGroups() {
        return ALL_GROUPS;
    }

    /** {@code id}'s reference recipe time in ticks, or 0 when its jobs count items rather than work. */
    public static int workTicks(String id) {
        return WORK_TICKS.getOrDefault(id, 0);
    }

    /** Whether a {@code filter} part of {@code id}'s jobs counts work rather than items. */
    public static boolean countsWork(String id, String filter) {
        return workTicks(id) > 0 && (WorkFilter.ANY.id().equals(filter) || WorkFilter.STAGE.id().equals(filter)
                || WorkFilter.SETTING.id().equals(filter));
    }

    /** Whether {@code id}'s work jobs are sized by machine time; the others are sized by the materials they use. */
    public static boolean sizedByTime(String id) {
        return workTicks(id) > 0 && !MATERIAL_BOUND.contains(id);
    }

    /** A good build's throughput at {@code tier}'s stage in hundredths of base recipe speed, or 0 when none is set. */
    public static int jobSpeed(String id, int tier) {
        int[] speeds = JOB_SPEED.get(id);
        return speeds == null ? 0 : speeds[Math.clamp(tier, WorkOrder.MIN_TIER, WorkOrder.MAX_TIER) - 1];
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

    /**
     * The minimum a stage part of a {@code tier} job needs under Deep Ore: the next stage above {@code minimum} that has
     * work and that the tier's chassis runs, or {@code minimum} when there is none, so Deep Ore then costs nothing.
     */
    public static int deepMinimum(String id, int tier, int minimum) {
        Set<Integer> empty = EMPTY_STAGES.getOrDefault(id, Set.of());
        for (int stage = minimum + 1; stage <= stageCap(id, tier); stage++) {
            if (!empty.contains(stage)) { return stage; }
        }
        return minimum;
    }

    /** The chassis stage a job of {@code tier} needs: the stage cap, or none on a stageless machine. */
    public static int requiredStage(String id, int tier) {
        return staged(id) ? stageCap(id, tier) : 0;
    }
}
