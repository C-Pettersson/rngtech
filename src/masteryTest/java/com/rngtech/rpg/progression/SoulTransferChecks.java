package com.rngtech.rpg.progression;

import com.mojang.serialization.JsonOps;

import java.util.List;
import java.util.OptionalInt;
import java.util.function.Predicate;

final class SoulTransferChecks {
    private static final Predicate<MachineProgressionState> ANY_GEAR = state -> true;
    private static int checks;

    private SoulTransferChecks() { }

    static int run() {
        checks = 0;
        successChance();
        lostXp();
        lostLevels();
        gearKeepsNodes();
        release();
        chargeCodec();
        return checks;
    }

    private static void successChance() {
        require(SoulTransfer.successChance(OptionalInt.of(55), false) == 55, "the recorded stability is the success chance");
        require(SoulTransfer.successChance(OptionalInt.of(55), true) == 100, "the pack config can make every transfer succeed");
        require(SoulTransfer.successChance(OptionalInt.empty(), false) == 100, "an uncalibrated Soul Transfer from loot or quests always succeeds");
        require(SoulTransfer.successChance(OptionalInt.of(140), false) == 100, "the chance never passes 100");
    }

    private static void lostXp() {
        var built = built(40);
        require(SoulTransfer.afterFailure(built, 0, 0, ANY_GEAR).equals(built), "the default failure costs no XP");
        var halved = SoulTransfer.afterFailure(built, 50, 0, ANY_GEAR);
        require(halved.xp() == built.xp() - built.xp() / 2, "the configured share of XP is lost");
        require(halved.level() == MachineProgressionState.levelForXp(halved.xp()) && halved.level() < built.level(), "the level follows the remaining XP");
        require(halved.spentPoints() == halved.level() - 1, "points the lower level no longer covers come off the build");
        require(built.allocatedNodes().subList(0, halved.spentPoints()).equals(halved.allocatedNodes()), "the build loses its last allocations first");
        require(halved.following() && halved.targetNodes().equals(built.allocatedNodes()), "the old build refills as XP returns");
        require(halved.sealTiers() == built.sealTiers(), "Seal tiers are never lost to a failure");
        var refilled = MegaPassiveTree.follow(halved.withAddedXp(built.xp()), state -> true);
        require(refilled.allocatedNodes().equals(built.allocatedNodes()), "regained XP restores the same build");
        var wiped = SoulTransfer.afterFailure(built, 100, 0, ANY_GEAR);
        require(wiped.xp() == 0 && wiped.level() == 1 && wiped.allocatedNodes().isEmpty(), "a full loss leaves a level 1 machine");
    }

    private static void lostLevels() {
        var built = built(40);
        var lower = SoulTransfer.afterFailure(built, 0, 5, ANY_GEAR);
        require(lower.level() == 35 && lower.spentPoints() == 34, "a flat loss takes whole levels and their points");
        require(lower.xpInCurrentLevel() == built.xpInCurrentLevel(), "progress into the current level is kept");
        require(validTree(lower), "a level loss leaves a connected tree");
        require(built.allocatedNodes().subList(0, 34).equals(lower.allocatedNodes()), "the latest allocations go first");
        var both = SoulTransfer.afterFailure(built, 50, 2, ANY_GEAR);
        require(both.level() == MachineProgressionState.levelForXp(built.xp() - built.xp() / 2) - 2, "level loss applies after XP loss");
        var floor = SoulTransfer.afterFailure(built, 0, 99, ANY_GEAR);
        require(floor.level() == 1 && floor.allocatedNodes().isEmpty() && floor.xp() < MachineProgressionState.xpForLevel(2), "a large loss stops at level 1");
        var refilled = MegaPassiveTree.follow(lower.withAddedXp(built.xp()), ANY_GEAR);
        require(refilled.allocatedNodes().equals(built.allocatedNodes()), "regained levels restore the same build");
    }

    private static void gearKeepsNodes() {
        var built = built(40);
        String needed = built.allocatedNodes().getLast();
        var kept = SoulTransfer.afterFailure(built, 0, 5, state -> state.allocatedNodes().contains(needed));
        require(kept.allocatedNodes().contains(needed), "a node the installed Gear needs is never refunded");
        require(validTree(kept) && built.allocatedNodes().containsAll(kept.allocatedNodes()), "skipping a node still leaves a connected tree");
        require(kept.spentPoints() <= kept.level() - 1, "the build never outgrows the level");
        Predicate<MachineProgressionState> locked = state -> state.allocatedNodes().size() == built.spentPoints();
        var capped = SoulTransfer.afterFailure(built, 100, 10, locked);
        require(capped.level() == built.level() && capped.allocatedNodes().equals(built.allocatedNodes()),
                "when no node can go, the loss stops at the level the build needs");
    }

    private static boolean validTree(MachineProgressionState state) {
        return state.allocatedNodes().isEmpty() || MegaPassiveTree.validBuild(state.startNodeId(), state.allocatedNodes());
    }

    private static void release() {
        var charge = new SoulTransfer.Charge(MachineMasteryFamily.CRUSHER, built(20));
        var empty = MachineProgressionState.EMPTY.forFamily(MachineMasteryFamily.CRUSHER);
        require(SoulTransfer.release(charge, MachineMasteryFamily.CRUSHER, 1, empty, false) == SoulTransfer.Release.RELEASE,
                "a soul releases into an empty machine of its family");
        require(SoulTransfer.release(charge, MachineMasteryFamily.FURNACE, 1, empty, false) == SoulTransfer.Release.WRONG_FAMILY,
                "a soul only fits its own family");
        require(SoulTransfer.release(new SoulTransfer.Charge(MachineMasteryFamily.FURNACE, built(20)), MachineMasteryFamily.ALLOY_FURNACE, 1, empty, false)
                == SoulTransfer.Release.WRONG_FAMILY, "families that share a start are still different families");
        var occupied = empty.withAddedXp(10);
        require(SoulTransfer.release(charge, MachineMasteryFamily.CRUSHER, 1, occupied, false) == SoulTransfer.Release.CONFIRM_OVERWRITE,
                "replacing a machine's own Mastery needs confirmation");
        require(SoulTransfer.release(charge, MachineMasteryFamily.CRUSHER, 1, occupied, true) == SoulTransfer.Release.RELEASE,
                "a confirmed release replaces the target's Mastery");
        var ascended = new SoulTransfer.Charge(MachineMasteryFamily.CRUSHER, new MachineProgressionState(100, 0, 1, List.of(),
                MachineMasteryFamily.CRUSHER.startNodeId(), List.of(), false, "", List.of(), 1));
        require(SoulTransfer.release(ascended, MachineMasteryFamily.CRUSHER, AscendancyCatalog.ENTRY_STAGE - 1, empty, false)
                == SoulTransfer.Release.ENTRY_STAGE, "an ascended soul needs a chassis that could take the first Seal");
        require(SoulTransfer.release(ascended, MachineMasteryFamily.CRUSHER, AscendancyCatalog.ENTRY_STAGE, empty, false)
                == SoulTransfer.Release.RELEASE, "an ascended soul releases at the entry stage");
        require(!SoulTransfer.hasSoul(empty) && SoulTransfer.hasSoul(occupied) && SoulTransfer.hasSoul(ascended.state()),
                "XP or a Seal tier is something to absorb");
        var dormant = new SoulTransfer.Charge(MachineMasteryFamily.CRUSHER, built(20), 5);
        require(SoulTransfer.release(dormant, MachineMasteryFamily.CRUSHER, 4, empty, false) == SoulTransfer.Release.RELEASE_STAGE,
                "a Dormant Machine soul refuses a machine below its release stage");
        require(SoulTransfer.release(dormant, MachineMasteryFamily.CRUSHER, 5, empty, false) == SoulTransfer.Release.RELEASE,
                "a Dormant Machine soul releases at its release stage");
        require(charge.releaseStage() == 0, "a soul taken from a machine fits any stage");
        var melter = new SoulTransfer.Charge(MachineMasteryFamily.MELTER, built(20).forFamily(MachineMasteryFamily.MELTER), 7);
        require(melter.effectiveReleaseStage() == 6 && SoulTransfer.release(melter, MachineMasteryFamily.MELTER, 6,
                MachineProgressionState.EMPTY.forFamily(MachineMasteryFamily.MELTER), false) == SoulTransfer.Release.RELEASE,
                "a soul whose release stage is above its family's top stage still fits that family's top machine");
    }

    private static void chargeCodec() {
        var charge = new SoulTransfer.Charge(MachineMasteryFamily.RESONANCE_CALIBRATOR, built(30).forFamily(MachineMasteryFamily.RESONANCE_CALIBRATOR));
        var json = SoulTransfer.Charge.CODEC.encodeStart(JsonOps.INSTANCE, charge).getOrThrow();
        require(SoulTransfer.Charge.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow().equals(charge), "a held soul survives a save");
        var dormant = new SoulTransfer.Charge(MachineMasteryFamily.CRUSHER, built(20), 7);
        var dormantJson = SoulTransfer.Charge.CODEC.encodeStart(JsonOps.INSTANCE, dormant).getOrThrow();
        require(SoulTransfer.Charge.CODEC.parse(JsonOps.INSTANCE, dormantJson).getOrThrow().equals(dormant), "a release stage survives a save");
        require(!json.getAsJsonObject().has("release_stage") || json.getAsJsonObject().get("release_stage").getAsInt() == 0,
                "a soul without a release stage saves none");
    }

    /** A crusher at the given level with every point spent in allocation order. */
    private static MachineProgressionState built(int level) {
        var state = MachineProgressionState.EMPTY.forFamily(MachineMasteryFamily.CRUSHER).withAddedXp(MachineProgressionState.xpForLevel(level) + 7);
        while (state.unspentPoints() > 0) {
            MachineProgressionState current = state;
            MegaPassiveNode next = MegaPassiveTree.TREE.nodes().stream().filter(node -> MegaPassiveTree.TREE.canUnlock(node, current)).findFirst().orElseThrow();
            state = state.withUnlockedNode(next.index());
        }
        return state;
    }

    private static void require(boolean condition, String label) { checks++; if (!condition) { throw new AssertionError(label); } }
}
