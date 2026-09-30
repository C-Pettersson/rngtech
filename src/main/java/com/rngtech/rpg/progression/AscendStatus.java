package com.rngtech.rpg.progression;

import java.util.function.IntPredicate;

/** Whether a machine can use its next Ascendancy Seal, checked in the same order as the server action. */
public enum AscendStatus {
    READY,
    /** An earned tier has no valid ascendancy, so the machine chooses again without a Seal. */
    FREE_CHOICE,
    ALL_TIERS,
    NO_ASCENDANCIES,
    ENTRY_STAGE,
    SEAL_MISSING;

    public static AscendStatus of(MachineProgressionState state, MachineMasteryFamily family, int entryStage, IntPredicate hasSeal) {
        if (state.awaitingAscendancyChoice()) { return FREE_CHOICE; }
        int tier = state.sealTiers() + 1;
        if (tier > AscendancyCatalog.MAX_TIERS) { return ALL_TIERS; }
        if (tier == 1 && AscendancyCatalog.forFamily(family).isEmpty()) { return NO_ASCENDANCIES; }
        if (tier == 1 && entryStage < AscendancyCatalog.ENTRY_STAGE) { return ENTRY_STAGE; }
        return hasSeal.test(tier) ? READY : SEAL_MISSING;
    }
}
