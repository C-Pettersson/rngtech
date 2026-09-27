package com.rngtech.rpg.refinement;

import com.rngtech.rpg.MachineStat;

import java.util.List;
import java.util.Locale;

public enum RefinementOperation {
    ADD_MODIFIER(RefinementAction.RANDOM_ADD),
    UPGRADE_RANDOM_MODIFIER(RefinementAction.RANDOM_UPGRADE),
    UPGRADE_SELECTED_MODIFIER(RefinementAction.SELECTED_UPGRADE),
    ASCEND_RARITY(RefinementAction.ASCEND_RARITY),
    ASCENSION_CATALYST(RefinementAction.CATALYZE_ASCENSION),
    REMOVE_MODIFIER(RefinementAction.RANDOM_REMOVE),
    KINETIC_LENS(
            RefinementAction.TARGETED_ADD_OR_UPGRADE,
            List.of(
                    MachineStat.PROCESSING_SPEED,
                    MachineStat.INSTANT_PROCESS_CHANCE,
                    MachineStat.OUTPUT_AMOUNT,
                    MachineStat.SUPER_OUTPUT_CHANCE,
                    MachineStat.ENERGY_GENERATION,
                    MachineStat.ENERGY_CAPACITY,
                    MachineStat.ENERGY_CAPACITY_FLAT,
                    MachineStat.PARALLEL_JOBS,
                    MachineStat.CRUSHER_SALVAGE_CHANCE,
                    MachineStat.MINING_SPEED,
                    MachineStat.ATTACK_SPEED,
                    MachineStat.FE_TRANSFER,
                    MachineStat.BATTERY_SUPPORT,
                    MachineStat.LUCK,
                    MachineStat.VEIN_MINE_LIMIT,
                    MachineStat.TREE_FELL_LIMIT,
                    MachineStat.ORE_BURST_SPEED
            )
    ),
    EFFICIENCY_LENS(
            RefinementAction.TARGETED_ADD_OR_UPGRADE,
            List.of(
                    MachineStat.EFFICIENCY,
                    MachineStat.ENERGY_USAGE,
                    MachineStat.FUEL_EFFICIENCY,
                    MachineStat.IDLE_LOSS,
                    MachineStat.HIGH_HARDNESS_ENERGY_MITIGATION,
                    MachineStat.NO_BATTERY_OUTPUT_RETENTION,
                    MachineStat.DURABILITY,
                    MachineStat.SELF_REPAIR,
                    MachineStat.FE_USAGE,
                    MachineStat.VEIN_MINE_FE_USAGE,
                    MachineStat.CONTROL
            )
    ),
    CHAOS_CRYSTAL(RefinementAction.FULL_REROLL),
    EXPANSION_CRYSTAL(RefinementAction.FILL_OPEN_SLOTS),
    NULL_CRYSTAL(RefinementAction.RANDOM_REMOVE);

    private final RefinementAction action;
    private final List<MachineStat> targetStats;
    private final String serializedName;

    RefinementOperation(RefinementAction action) {
        this(action, List.of());
    }

    RefinementOperation(RefinementAction action, List<MachineStat> targetStats) {
        this.action = action;
        this.targetStats = List.copyOf(targetStats);
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public RefinementAction action() {
        return action;
    }

    public List<MachineStat> targetStats() {
        return targetStats;
    }

    public boolean hasTargetStats() {
        return !targetStats.isEmpty();
    }

    public boolean requiresForgeSelection() {
        return false;
    }

    public static RefinementOperation targetedOperationFor(MachineStat stat) {
        for (RefinementOperation operation : values()) {
            if (operation.action == RefinementAction.TARGETED_ADD_OR_UPGRADE
                    && operation.targetStats.contains(stat)) {
                return operation;
            }
        }
        return null;
    }

    public static String wrongLensMessageKey(MachineStat stat) {
        RefinementOperation operation = targetedOperationFor(stat);
        if (operation == null) {
            return "rngtech.refinement.failure.wrong_lens.no_current_lens";
        }
        return "rngtech.refinement.failure.wrong_lens." + operation.serializedName;
    }

    public String tooltipKey() {
        return "rngtech.tooltip.refinement_operation." + serializedName;
    }
}
