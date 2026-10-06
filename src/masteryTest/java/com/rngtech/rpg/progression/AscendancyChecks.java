package com.rngtech.rpg.progression;

import com.rngtech.client.screen.AscendancyTreeLayout;
import com.rngtech.client.screen.MasteryCrestPlacement;
import com.rngtech.content.menu.MasteryMenuSupport;
import com.rngtech.rpg.BonusBanks;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.ModifierOperation;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.OutputLedger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.inventory.SimpleContainerData;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Ascendancy domain checks, run from {@link MasteryChecks} against the test-only fixture catalog. */
final class AscendancyChecks {
    private static final String ALPHA = "fixture_crusher_alpha";
    private static final String BETA = "fixture_crusher_beta";
    private static final String GAMMA = "fixture_crusher_gamma";
    private static final String FURNACE = "fixture_furnace_alpha";
    private static final String DIRECTORY = "/data/rngtech/mastery/ascendancies/";
    private static final MachineMasteryFamily CRUSHER = MachineMasteryFamily.CRUSHER;
    private static int checks;

    private AscendancyChecks() { }

    static int run() {
        catalog();
        shapeRules();
        declarations();
        sealTiers();
        allocationAndRefunds();
        switching();
        loadValidation();
        familyScope();
        effectPipeline();
        buildCodes();
        sealActions();
        ascendStatus();
        clientSync();
        bonusSummary();
        panelLayout();
        launchContent();
        bonusBanks();
        outputLedger();
        formulas();
        familyFormulas();
        familyLaunchContent();
        crestPlacement();
        mainCatalogAssets();
        return checks;
    }

    private static void catalog() {
        require(fixtures(CRUSHER).equals(List.of(ALPHA, BETA, GAMMA)), "a family can have more than two ascendancies, listed in index order");
        require(fixtures(MachineMasteryFamily.FURNACE).equals(List.of(FURNACE)), "ascendancies belong to their own family");
        for (MachineMasteryFamily family : MachineMasteryFamily.values()) {
            require(AscendancyCatalog.forFamily(family).stream().filter(ascendancy -> !ascendancy.id().startsWith("fixture_")).count() >= 2,
                    family + " ships at least two ascendancies");
        }
        for (Ascendancy ascendancy : AscendancyCatalog.all()) {
            require(AscendancyCatalog.violations(ascendancy).isEmpty(), ascendancy.id() + " follows the tree rules");
        }
        Ascendancy alpha = AscendancyCatalog.get(ALPHA);
        require(alpha.nodes().size() + 1 == 13 && alpha.root().kind() == AscendancyNode.Kind.ROOT, "the fixture uses the 13-node launch shape");
        require(alpha.isDeep(alpha.node("deep_e")) && !alpha.isDeep(alpha.node("notable_a")), "deep notables sit behind another notable");
    }

    private static void shapeRules() {
        rejects(raw -> { nodes(raw).remove(nodes(raw).size() - 1); nodes(raw).remove(nodes(raw).size() - 1); }, "nodes; expected");
        rejects(raw -> node(raw, "notable_a").addProperty("parent", "root"), "a notable sits behind a small node");
        rejects(raw -> node(raw, "small_e").addProperty("parent", "small_a"), "a small node sits behind the root or a notable");
        rejects(raw -> { node(raw, "small_e").addProperty("parent", "root"); node(raw, "small_f").addProperty("parent", "root"); }, "at least one deep notable");
        rejects(raw -> { node(raw, "small_c").addProperty("parent", "notable_a"); node(raw, "small_d").addProperty("parent", "notable_b"); }, "at least 3 notables");
        rejects(raw -> node(raw, "notable_b").add("effects", effect("CALIBRATION_PRECISION")), "does not apply to CRUSHER");
        rejects(raw -> behaviors(raw, "notable_b", "UNDECLARED_BEHAVIOR"), "is not declared for CRUSHER");
        rejects(raw -> { node(raw, "notable_b").remove("passive"); node(raw, "notable_b").add("effects", new JsonArray()); }, "grants nothing");
        rejects(raw -> node(raw, "small_a").addProperty("parent", "missing"), "unknown parent");
        rejects(raw -> node(raw, "small_e").addProperty("parent", "deep_e"), "does not reach the root");
        rejects(raw -> node(raw, "small_b").addProperty("id", "small_a"), "duplicate node");
        rejects(raw -> node(raw, "notable_d").addProperty("kind", "ROOT"), "second root");
        rejects(raw -> raw.addProperty("id", "Fixture-Alpha"), "lowercase letters");
        rejects(raw -> { node(raw, "small_b").add("x", node(raw, "small_a").get("x")); node(raw, "small_b").add("y", node(raw, "small_a").get("y")); },
                "shares a grid position");
    }

    private static void declarations() {
        require(CRUSHER.supports(MachineStat.LUCK) && !MachineMasteryFamily.FURNACE.supports(MachineStat.LUCK), "a declared stat applies only to its families");
        require(CRUSHER.supportsBehavior("FIXTURE_ECHO") && !MachineMasteryFamily.FURNACE.supportsBehavior("FIXTURE_ECHO"), "a declared behavior needs no family switch");
        require(CRUSHER.supportsBehavior("MATCHING_HEAD") && !MachineMasteryFamily.FURNACE.supportsBehavior("MATCHING_HEAD"), "built-in behaviors are unchanged");
        require(MasteryDeclarations.yield(MachineStat.LUCK) == MasteryDeclarations.Yield.OUTPUT
                && MasteryDeclarations.yield(MachineStat.PROCESSING_SPEED) == MasteryDeclarations.Yield.NONE, "stats declare their yield for the loop audit");
        require(MasteryDeclarations.declared("FIXTURE_ECHO") && !MasteryDeclarations.declared("MATCHING_HEAD"), "only registered behaviors are declared");
    }

    private static void sealTiers() {
        var fresh = MachineProgressionState.EMPTY.forFamily(CRUSHER);
        require(fresh.sealTiers() == 0 && fresh.ascendancy().isEmpty() && fresh.ascendancyPoints() == 0, "a new chassis has no ascendancy");
        require(fresh.withSealTier(1, CRUSHER, 3, ALPHA).equals(fresh), "Seal I needs entry stage 4");
        require(fresh.withSealTier(1, CRUSHER, 4, FURNACE).equals(fresh), "Seal I chooses one of the host family's ascendancies");
        require(fresh.withSealTier(1, CRUSHER, 4, "missing").equals(fresh), "Seal I rejects an unknown ascendancy");
        require(fresh.withSealTier(2, CRUSHER, 8, ALPHA).equals(fresh), "Seal tiers apply in order");
        var one = fresh.withSealTier(1, CRUSHER, 4, ALPHA);
        require(one.ascendancy().equals(ALPHA) && one.sealTiers() == 1 && one.ascendancyPoints() == 2 && one.ascendancyNodes().isEmpty(),
                "Seal I chooses the ascendancy and grants two points");
        require(one.withSealTier(1, CRUSHER, 4, BETA).equals(one), "each tier is earned once");
        var two = one.withSealTier(2, CRUSHER, 0, "");
        require(two.sealTiers() == 2 && two.ascendancy().equals(ALPHA), "later tiers need no entry stage and keep the choice");
        var three = two.withSealTier(3, CRUSHER, 0, "");
        require(three.ascendancyPoints() == AscendancyCatalog.MAX_POINTS, "three tiers grant six points");
        require(three.withSealTier(4, CRUSHER, 8, "").equals(three), "there is no fourth tier");
    }

    private static void allocationAndRefunds() {
        var full = ascended(ALPHA, List.of(), 3);
        require(full.withAscendancyNode("notable_a").equals(full), "a notable needs its small node");
        require(full.withAscendancyNode("root").equals(full), "the root is free and never allocated");
        var pair = full.withAscendancyNode("small_a").withAscendancyNode("notable_a");
        require(pair.ascendancyNodes().equals(List.of("small_a", "notable_a")) && pair.ascendancyUnspent() == 4, "a notable costs two points");
        var deep = pair.withAscendancyNode("small_e").withAscendancyNode("deep_e");
        require(deep.ascendancyNodes().size() == 4 && deep.ascendancyNodes().contains("deep_e"), "a deep notable costs four points with its parent pair");
        var capped = deep.withAscendancyNode("small_b").withAscendancyNode("notable_b");
        require(capped.ascendancyUnspent() == 0 && capped.withAscendancyNode("small_c").equals(capped), "six points is the budget");
        var threeNotables = full;
        for (String id : List.of("small_a", "notable_a", "small_b", "notable_b", "small_c", "notable_c")) { threeNotables = threeNotables.withAscendancyNode(id); }
        require(threeNotables.ascendancyNodes().size() == 6, "the full budget reaches three notables");
        require(capped.withoutAscendancyNode("notable_a").equals(capped), "a node that another depends on cannot be refunded");
        var refunded = capped.withoutAscendancyNode("deep_e");
        require(refunded.ascendancyNodes().size() == 5 && !refunded.ascendancyNodes().contains("deep_e"), "refunding a leaf returns its point");
        require(refunded.withoutAscendancyNode("root").equals(refunded), "the root cannot be refunded");
        var tierOne = ascended(ALPHA, List.of(), 1).withAscendancyNode("small_a").withAscendancyNode("notable_a");
        require(tierOne.withAscendancyNode("small_b").equals(tierOne), "Seal I alone allows one notable");
        require(capped.cleared().ascendancyNodes().equals(capped.ascendancyNodes()), "clearing the shared tree keeps the ascendancy");
    }

    private static void switching() {
        var full = ascended(ALPHA, List.of(), 3);
        var allocated = full.withAscendancyNode("small_a");
        require(allocated.withSwitchedAscendancy(CRUSHER, BETA).equals(allocated), "switching needs every non-root node refunded");
        var switched = full.withSwitchedAscendancy(CRUSHER, BETA);
        require(switched.ascendancy().equals(BETA) && switched.sealTiers() == 3, "switching keeps earned tiers");
        require(full.withSwitchedAscendancy(CRUSHER, ALPHA).equals(full), "switching to the same ascendancy does nothing");
        require(full.withSwitchedAscendancy(CRUSHER, FURNACE).equals(full), "switching stays within the family");
        var fresh = MachineProgressionState.EMPTY.forFamily(CRUSHER);
        require(fresh.withSwitchedAscendancy(CRUSHER, BETA).equals(fresh), "switching needs a chosen ascendancy");
    }

    private static void loadValidation() {
        require(ascended(ALPHA, List.of("small_a", "retired_node", "notable_a"), 1).ascendancyNodes().equals(List.of("small_a", "notable_a")),
                "retired nodes drop and return their points");
        require(ascended(ALPHA, List.of("small_a", "small_e", "deep_e"), 3).ascendancyNodes().equals(List.of("small_a")),
                "nodes that lost their parent are refunded along with their dependants");
        require(ascended(ALPHA, List.of("small_a", "notable_a", "small_b"), 1).ascendancyNodes().isEmpty(), "allocations over the earned budget are refunded");
        var retired = ascended("retired_ascendancy", List.of("small_a"), 2);
        require(retired.ascendancy().isEmpty() && retired.sealTiers() == 2 && retired.awaitingAscendancyChoice(),
                "a retired ascendancy clears the choice and keeps earned tiers");
        var rechosen = retired.withAscendancyChoice(CRUSHER, GAMMA);
        require(rechosen.ascendancy().equals(GAMMA) && rechosen.sealTiers() == 2, "a free choice follows a retired ascendancy");
        var full = ascended(ALPHA, List.of(), 3);
        require(full.withAscendancyChoice(CRUSHER, GAMMA).equals(full), "a free choice needs an earned tier without a valid choice");
        require(ascended(ALPHA, List.of(), 0).ascendancy().isEmpty(), "an ascendancy needs an earned tier");
        require(ascended(ALPHA, List.of(), 9).sealTiers() == AscendancyCatalog.MAX_TIERS, "tiers clamp to three");

        var state = ascended(ALPHA, List.of("small_a", "notable_a", "small_e"), 2);
        var json = MachineProgressionState.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow();
        require(state.equals(MachineProgressionState.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow()), "ascendancy state survives the save codec");
        var wire = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), net.minecraft.core.RegistryAccess.EMPTY);
        try {
            MachineProgressionState.STREAM_CODEC.encode(wire, state);
            require(state.equals(MachineProgressionState.STREAM_CODEC.decode(wire)), "ascendancy state survives the network codec");
        } finally { wire.release(); }
        var legacy = MachineProgressionState.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"xp\":100,\"level\":2,\"start\":\"start_drive\"}")).getOrThrow();
        require(legacy.sealTiers() == 0 && legacy.ascendancy().isEmpty(), "saves from before ascendancies load without one");
        var oldTree = JsonParser.parseString("{\"tree_version\":1,\"start\":\"start_drive\",\"ascendancy\":\"" + ALPHA
                + "\",\"ascendancy_nodes\":[\"small_a\"],\"seal_tiers\":1}");
        var migrated = MachineProgressionState.CODEC.parse(JsonOps.INSTANCE, oldTree).getOrThrow();
        require(migrated.ascendancy().equals(ALPHA) && migrated.ascendancyNodes().equals(List.of("small_a")), "a shared-tree version change keeps the ascendancy");
    }

    private static void familyScope() {
        var furnace = new MachineProgressionState(0, 0, 1, List.of(), MachineMasteryFamily.FURNACE.startNodeId(), List.of(), false, FURNACE, List.of("small_a"), 2);
        require(furnace.forFamily(MachineMasteryFamily.FURNACE) == furnace, "the owning family keeps its ascendancy");
        var alloy = furnace.forFamily(MachineMasteryFamily.ALLOY_FURNACE);
        require(alloy.ascendancy().isEmpty() && alloy.ascendancyNodes().isEmpty() && alloy.sealTiers() == 2
                && alloy.startNodeId().equals(furnace.startNodeId()), "a family sharing the start drops a foreign ascendancy and keeps tiers");
        var crusher = ascended(ALPHA, List.of("small_a"), 1).forFamily(MachineMasteryFamily.MELTER);
        require(crusher.ascendancy().isEmpty() && crusher.sealTiers() == 1, "a different start drops a foreign ascendancy and keeps tiers");
    }

    private static void effectPipeline() {
        var alpha = ascended(ALPHA, List.of("small_a", "notable_a", "small_b", "notable_b", "small_c", "notable_c"), 3);
        var plain = new MachineProgressionState(0, 0, 1, List.of(), CRUSHER.startNodeId(), List.of(), false);
        var with = MachineStatAccumulator.componentBase(Map.of(MachineStat.PROCESSING_SPEED, 1.0, MachineStat.LUCK, 0.0));
        MegaPassiveTree.applyStats(with, alpha, CRUSHER);
        near(with.value(MachineStat.PROCESSING_SPEED), 1.21, "root and small-node speed join the Drive conversion's increased bucket");
        near(with.value(MachineStat.LUCK), 5, "a declared stat applies through the ascendancy");
        var without = MachineStatAccumulator.componentBase(Map.of(MachineStat.PROCESSING_SPEED, 1.0, MachineStat.LUCK, 0.0));
        MegaPassiveTree.applyStats(without, plain, CRUSHER);
        near(without.value(MachineStat.PROCESSING_SPEED), 1.03, "no ascendancy leaves only the Drive conversion");
        var otherFamily = MachineStatAccumulator.componentBase(Map.of(MachineStat.PROCESSING_SPEED, 1.0));
        MegaPassiveTree.applyStats(otherFamily, alpha, MachineMasteryFamily.FURNACE);
        near(otherFamily.value(MachineStat.PROCESSING_SPEED), 1.015, "ascendancy effects apply only to their family");
        require(MegaPassiveTree.has(alpha, "FIXTURE_ECHO") && !MegaPassiveTree.has(plain, "FIXTURE_ECHO"), "the root's behavior is active");
        require(MegaPassiveTree.passive(alpha, PassiveStatType.COMPONENT_STAGE_SUPPORT) == 1, "ascendancy passive stats sum with the shared tree");
        require(MegaPassiveTree.sources(alpha).size() == 7, "effect sources are the root plus allocated nodes");
    }

    private static void buildCodes() {
        var state = ascended(ALPHA, List.of("small_a", "notable_a", "small_e", "deep_e"), 2);
        var code = MasteryBuildCode.decode(MasteryBuildCode.copy(state).encode());
        require(code.ascendancy().equals(ALPHA) && code.ascendancyNodes().equals(state.ascendancyNodes()), "build codes carry the ascendancy order");
        require(MasteryBuildCode.decode(new MasteryBuildCode(CRUSHER.startNodeId(), List.of()).encode()).ascendancy().isEmpty(),
                "codes without an ascendancy still decode");
        String encoded = code.encode();
        var retired = MasteryBuildCode.decode(encoded.replace(ALPHA, "retired_ascendancy"));
        require(retired.ascendancy().isEmpty() && retired.start().equals(code.start()), "a retired ascendancy is dropped and the shared build still pastes");
        var reversed = MasteryBuildCode.decode(new MasteryBuildCode(CRUSHER.startNodeId(), List.of(), ALPHA, List.of("notable_a", "small_a")).encode());
        require(reversed.ascendancy().isEmpty(), "an ascendancy order that allocates a child first is dropped");

        var partial = new Host(ascended(ALPHA, List.of(), 1), s -> true);
        MasteryOperations.pasteAscendancy(partial, code);
        require(partial.state.ascendancyNodes().equals(List.of("small_a", "notable_a")), "paste allocates as far as earned points reach");
        var other = new Host(ascended(BETA, List.of(), 3), s -> true);
        MasteryOperations.pasteAscendancy(other, code);
        require(other.state.ascendancyNodes().isEmpty(), "paste needs the same ascendancy");
        var busy = new Host(ascended(ALPHA, List.of("small_b"), 3), s -> true);
        MasteryOperations.pasteAscendancy(busy, code);
        require(busy.state.ascendancyNodes().equals(List.of("small_b")), "paste needs an empty ascendancy tree");
        var blocked = new Host(ascended(ALPHA, List.of(), 3), s -> !s.ascendancyNodes().contains("notable_a"));
        MasteryOperations.pasteAscendancy(blocked, code);
        require(blocked.state.ascendancyNodes().equals(List.of("small_a")), "paste stops before a Gear conflict");
    }

    private static void sealActions() {
        var pay = new Payment(Map.of(1, 1, 2, 1, 3, 1), 12);
        var host = new Host(MachineProgressionState.EMPTY.forFamily(CRUSHER), s -> true);
        host.entryStage = 3;
        require("entry_stage".equals(MasteryOperations.ascendancy(host, "ascend", ALPHA, pay)) && pay.seals.get(1) == 1,
                "Seal I needs entry stage 4 and costs nothing when rejected");
        host.entryStage = AscendancyCatalog.ENTRY_STAGE;
        require("ascend_failed".equals(MasteryOperations.ascendancy(host, "ascend", FURNACE, pay)) && pay.seals.get(1) == 1,
                "a foreign ascendancy is rejected before the Seal is paid");
        require("seal_missing".equals(MasteryOperations.ascendancy(host, "ascend", ALPHA, new Payment(Map.of(), 0))) && host.state.sealTiers() == 0,
                "a missing Seal leaves the machine unchanged");
        require(MasteryOperations.ascendancy(host, "ascend", ALPHA, pay) == null && host.state.ascendancy().equals(ALPHA)
                && host.state.sealTiers() == 1 && pay.seals.get(1) == 0, "Seal I is consumed and chooses the ascendancy");
        require(MasteryOperations.ascendancy(host, "ascend", "", pay) == null && host.state.sealTiers() == 2 && pay.seals.get(2) == 0,
                "Seal II adds the next tier");
        require(MasteryOperations.ascendancy(host, "allocate_ascendancy", "small_a", pay) == null && host.state.ascendancyNodes().equals(List.of("small_a")),
                "the allocate action spends an ascendancy point");
        require("ascendancy_allocation_failed".equals(MasteryOperations.ascendancy(host, "allocate_ascendancy", "deep_e", pay)),
                "the allocate action rejects a node without its parent");
        require("switch_failed".equals(MasteryOperations.ascendancy(host, "switch_ascendancy", BETA, pay)) && pay.seals.get(1) == 0,
                "switching needs an empty ascendancy tree");
        require("ascendancy_refund_cost".equals(MasteryOperations.ascendancy(host, "refund_ascendancy", "small_a", new Payment(Map.of(), 4)))
                && host.state.ascendancyNodes().size() == 1, "a refund needs five Mastery Refunds");
        require(MasteryOperations.ascendancy(host, "refund_ascendancy", "small_a", pay) == null && host.state.ascendancyNodes().isEmpty()
                && pay.refunds == 12 - AscendancyCatalog.REFUNDS_PER_NODE, "a refund costs five Mastery Refunds");
        require("seal_missing".equals(MasteryOperations.ascendancy(host, "switch_ascendancy", BETA, pay)), "switching costs a Seal I");
        pay.seals.put(1, 1);
        require(MasteryOperations.ascendancy(host, "switch_ascendancy", BETA, pay) == null && host.state.ascendancy().equals(BETA)
                && host.state.sealTiers() == 2 && pay.seals.get(1) == 0, "switching consumes a Seal I and keeps earned tiers");
        require(MasteryOperations.ascendancy(host, "ascend", "", pay) == null && "all_seals_used".equals(MasteryOperations.ascendancy(host, "ascend", "", pay)),
                "a machine earns each of the three tiers once");
        var retired = new Host(ascended("retired_ascendancy", List.of(), 2), s -> true);
        var free = new Payment(Map.of(), 0);
        require(MasteryOperations.ascendancy(retired, "choose_ascendancy", GAMMA, free) == null && retired.state.ascendancy().equals(GAMMA),
                "a free choice after retirement costs nothing");
        require("choice_failed".equals(MasteryOperations.ascendancy(retired, "choose_ascendancy", ALPHA, free)), "a free choice needs a missing ascendancy");
        require("unknown_action".equals(MasteryOperations.ascendancy(retired, "transmute", "", free)), "unknown actions are rejected");
    }

    /** The panel's Ascend status follows the server's order, so a ready button is an action the server accepts. */
    private static void ascendStatus() {
        var fresh = MachineProgressionState.EMPTY.forFamily(CRUSHER);
        require(AscendStatus.of(fresh, CRUSHER, 3, tier -> true) == AscendStatus.ENTRY_STAGE, "Seal I status names the entry stage first");
        require(AscendStatus.of(fresh, CRUSHER, 4, tier -> false) == AscendStatus.SEAL_MISSING, "Seal I status needs the Seal in inventory");
        require(AscendStatus.of(fresh, CRUSHER, 4, tier -> tier == 1) == AscendStatus.READY, "Seal I is ready with the Seal and the entry stage");
        var forestry = MachineProgressionState.EMPTY.forFamily(MachineMasteryFamily.FORESTRY);
        require(AscendStatus.of(forestry, MachineMasteryFamily.FORESTRY, 6, tier -> true) == AscendStatus.READY, "the Forestry Companion ascends from its tool head stage");
        var one = ascended(ALPHA, List.of(), 1);
        require(AscendStatus.of(one, CRUSHER, 0, tier -> tier == 2) == AscendStatus.READY, "later tiers need no entry stage");
        require(AscendStatus.of(one, CRUSHER, 8, tier -> tier == 1) == AscendStatus.SEAL_MISSING, "only the next tier's Seal counts");
        require(AscendStatus.of(ascended(ALPHA, List.of(), 3), CRUSHER, 8, tier -> true) == AscendStatus.ALL_TIERS, "three tiers end the Seals");
        require(AscendStatus.of(ascended("retired_ascendancy", List.of(), 2), CRUSHER, 8, tier -> false) == AscendStatus.FREE_CHOICE,
                "an earned tier without an ascendancy offers a free choice");
        var host = new Host(fresh, s -> true);
        host.entryStage = 3;
        require("entry_stage".equals(MasteryOperations.ascendancy(host, "ascend", ALPHA, new Payment(Map.of(1, 1), 0))), "the server agrees on the entry stage");
        host.entryStage = AscendancyCatalog.ENTRY_STAGE;
        require("seal_missing".equals(MasteryOperations.ascendancy(host, "ascend", ALPHA, new Payment(Map.of(), 0))), "the server agrees on a missing Seal");
        require(MasteryOperations.ascendancy(host, "ascend", ALPHA, new Payment(Map.of(1, 1), 0)) == null, "the server accepts a ready Seal");
    }

    /** Menus send ascendancy state to the client as numbers; the screen rebuilds it, so Copy includes the ascendancy. */
    private static void clientSync() {
        var host = new Host(ascended(ALPHA, List.of("small_e", "small_a", "deep_e"), 3), s -> true);
        host.entryStage = 5;
        var synced = MasteryMenuSupport.snapshot(synced(host), 0, CRUSHER);
        require(synced.ascendancy().equals(ALPHA) && synced.sealTiers() == 3, "menus sync the ascendancy and its Seal tiers");
        require(synced.ascendancyNodes().equals(host.state.ascendancyNodes()), "menus sync ascendancy nodes in allocation order");
        require(MasteryMenuSupport.ascendancyEntryStage(synced(host), 0) == 5, "menus sync the entry stage");
        require(MasteryBuildCode.copy(synced).ascendancyNodes().equals(host.state.ascendancyNodes()), "the screen's Copy includes the ascendancy");
        var none = MasteryMenuSupport.snapshot(synced(new Host(MachineProgressionState.EMPTY.forFamily(CRUSHER), s -> true)), 0, CRUSHER);
        require(none.ascendancy().isEmpty() && none.sealTiers() == 0 && none.ascendancyNodes().isEmpty(), "a machine without Seals syncs no ascendancy");

        var rockbreaker = new Host(ascended("rockbreaker", List.of("jam_recovery", "jam_breaker"), 1), s -> true);
        var stats = MachineStatAccumulator.componentBase(Map.of(MachineStat.JAM_CHANCE, 1.0));
        MegaPassiveTree.applyStats(stats, rockbreaker.state, CRUSHER);
        require(MasteryMenuSupport.grantedStats(synced(rockbreaker, stats), 0).equals(List.of(
                new MasteryMenuSupport.GrantedStat(MachineStat.HARDNESS_TOLERANCE, 1.0),
                new MasteryMenuSupport.GrantedStat(MachineStat.JAM_RECOVERY, 50.0),
                new MasteryMenuSupport.GrantedStat(MachineStat.JAM_CHANCE, 0.5))), "menus sync granted ascendancy stats with their values");
        require(MasteryMenuSupport.grantedStats(synced(new Host(MachineProgressionState.EMPTY.forFamily(CRUSHER), s -> true)), 0).isEmpty(),
                "a machine without an ascendancy syncs no granted stats");
    }

    private static SimpleContainerData synced(Host host) {
        return synced(host, MachineStatAccumulator.componentBase(Map.of()));
    }

    private static SimpleContainerData synced(Host host, MachineStatAccumulator stats) {
        var data = new SimpleContainerData(MasteryMenuSupport.FIELD_COUNT);
        for (int field = 0; field < MasteryMenuSupport.FIELD_COUNT; field++) {
            data.set(field, MasteryMenuSupport.get(host, field, () -> stats));
        }
        return data;
    }

    private static void bonusSummary() {
        var alpha = ascended(ALPHA, List.of("small_a", "notable_a"), 1);
        Ascendancy chosen = AscendancyCatalog.get(ALPHA);
        var lines = MasteryBonusSummary.of(alpha, MasteryApplicability.of(CRUSHER), 0, 0, 0);
        require(lines.stream().anyMatch(line -> line.kind() == MasteryBonusSummary.Kind.EFFECT && line.effect().stat() == MachineStat.PROCESSING_SPEED
                && line.sources().contains(chosen.root())), "the summary combines the root's effects and names it as a source");
        require(lines.stream().anyMatch(line -> line.kind() == MasteryBonusSummary.Kind.EFFECT && line.sources().contains(chosen.node("notable_a"))),
                "the summary includes allocated ascendancy nodes");
        require(lines.stream().anyMatch(line -> line.kind() == MasteryBonusSummary.Kind.BEHAVIOR && "FIXTURE_ECHO".equals(line.behavior()) && line.active()),
                "the summary lists ascendancy behaviors as active");
        var furnace = MasteryBonusSummary.of(alpha, MasteryApplicability.of(MachineMasteryFamily.FURNACE), 0, 0, 0);
        require(furnace.stream().noneMatch(line -> line.sources().stream().anyMatch(AscendancyNode.class::isInstance)),
                "another family's summary leaves the ascendancy out");
    }

    /** Every ascendancy fits the compact panel, a choose-dialog column, and a small box without overlapping nodes. */
    private static void panelLayout() {
        List<float[]> boxes = List.of(new float[] {12, 78, 228, 180}, new float[] {15, 92, 118, 147}, new float[] {0, 0, 40, 40});
        for (Ascendancy ascendancy : AscendancyCatalog.all()) {
            for (float[] box : boxes) {
                var placed = AscendancyTreeLayout.fit(ascendancy, box[0], box[1], box[2], box[3]);
                require(placed.size() == ascendancy.nodes().size() + 1 && placed.getFirst().node() == ascendancy.root(), ascendancy.id() + " places every node, root first");
                for (var node : placed) {
                    require(node.x() - node.radius() >= box[0] && node.x() + node.radius() <= box[2] && node.y() - node.radius() >= box[1]
                            && node.y() + node.radius() <= box[3], ascendancy.id() + " keeps " + node.node().id() + " inside the panel");
                }
                boolean apart = true;
                for (int first = 0; first < placed.size(); first++) {
                    for (int second = first + 1; second < placed.size(); second++) {
                        var a = placed.get(first);
                        var b = placed.get(second);
                        apart &= Math.hypot(a.x() - b.x(), a.y() - b.y()) >= a.radius() + b.radius();
                    }
                }
                require(apart, ascendancy.id() + " nodes do not overlap");
            }
            require(AscendancyTreeLayout.fit(ascendancy, 0, 0, 216, 102).stream().anyMatch(AscendancyTreeLayout.Placed::deep), ascendancy.id() + " marks its deep notables");
        }
    }

    /** Seals by tier and Mastery Refunds available to an action. */
    private static final class Payment implements MasteryOperations.AscendancyPayment {
        private final Map<Integer, Integer> seals;
        private int refunds;

        private Payment(Map<Integer, Integer> seals, int refunds) {
            this.seals = new java.util.HashMap<>(seals);
            this.refunds = refunds;
        }

        @Override public boolean seal(int tier) {
            if (seals.getOrDefault(tier, 0) <= 0) { return false; }
            seals.merge(tier, -1, Integer::sum);
            return true;
        }
        @Override public boolean refunds(int count) {
            if (refunds < count) { return false; }
            refunds -= count;
            return true;
        }
    }

    /** Shipped ascendancies need language keys and icons; fixtures stay out of the shipped index. */
    private static void mainCatalogAssets() {
        JsonObject index = MasteryNodeJson.resource(DIRECTORY + "index.json");
        require(index != null && index.get("version").getAsInt() == AscendancyCatalog.VERSION, "the shipped index matches the catalog version");
        JsonObject lang = MasteryNodeJson.resource("/assets/rngtech/lang/en_us.json");
        for (JsonElement file : MasteryNodeJson.array(index, "ascendancies")) {
            Ascendancy ascendancy = AscendancyCatalog.parse(MasteryNodeJson.resource(DIRECTORY + file.getAsString()));
            require(!ascendancy.id().startsWith("fixture_"), "fixtures stay out of the shipped index: " + ascendancy.id());
            require(lang.has(ascendancy.translationKey()), "language key: " + ascendancy.translationKey());
            for (AscendancyNode node : java.util.stream.Stream.concat(java.util.stream.Stream.of(ascendancy.root()), ascendancy.nodes().values().stream()).toList()) {
                require(lang.has(node.translationKey()), "language key: " + node.translationKey());
                require(AscendancyChecks.class.getResource("/assets/rngtech/textures/gui/mastery/" + node.masteryIconKey() + ".png") != null,
                        "icon texture: " + node.masteryIconKey());
            }
        }
    }

    private static List<String> fixtures(MachineMasteryFamily family) {
        return AscendancyCatalog.forFamily(family).stream().map(Ascendancy::id).filter(id -> id.startsWith("fixture_")).toList();
    }

    /** The shipped Crusher and Furnace ascendancies, their declarations, and the values their nodes produce. */
    private static void launchContent() {
        require(AscendancyCatalog.forFamily(CRUSHER).stream().map(Ascendancy::id).toList().subList(0, 2).equals(List.of("rockbreaker", "assayer")),
                "the Crusher ships Rockbreaker and Assayer first");
        require(AscendancyCatalog.forFamily(MachineMasteryFamily.FURNACE).stream().map(Ascendancy::id).toList().subList(0, 2)
                .equals(List.of("crucible_keeper", "bloomer")), "the Furnace ships Crucible Keeper and Bloomer first");
        JsonObject lang = MasteryNodeJson.resource("/assets/rngtech/lang/en_us.json");
        for (MachineStat stat : MachineStat.values()) {
            if (MasteryDeclarations.declared(stat)) {
                require(lang.has(stat.translationKey()) && lang.has(stat.translationKey() + ".description"), "declared stat names and describes " + stat);
            }
        }
        JsonObject declarations = MasteryNodeJson.resource("/data/rngtech/mastery/declarations.json");
        for (JsonElement behavior : MasteryNodeJson.array(declarations, "behaviors")) {
            String id = behavior.getAsJsonObject().get("id").getAsString();
            require(lang.has("rngtech.mastery.behavior." + id.toLowerCase(java.util.Locale.ROOT)), "declared behavior has a tooltip: " + id);
        }

        var rockbreaker = ascended("rockbreaker", List.of("jam_recovery", "jam_breaker", "under_level_efficiency", "pressure_stacking", "deep_efficiency", "bedrock_bite"), 3);
        var crusher = MachineStatAccumulator.componentBase(Map.of(MachineStat.JAM_CHANCE, 1.0));
        MegaPassiveTree.applyStats(crusher, rockbreaker, CRUSHER);
        require(crusher.intValue(MachineStat.HARDNESS_TOLERANCE) == 2, "Breaker's Stance and Bedrock Bite tolerate two missing levels");
        near(crusher.value(MachineStat.JAM_CHANCE), 0.5, "Jam Breaker halves the jam chance");
        near(crusher.value(MachineStat.UNDER_LEVEL_EFFICIENCY), 60, "the efficiency path stacks to 60%");
        var assayer = ascended("assayer", List.of("assay_yield", "compound_yield", "lode_chance", "mother_lode"), 2);
        var lode = MachineStatAccumulator.componentBase(Map.of());
        MegaPassiveTree.applyStats(lode, assayer, CRUSHER);
        require(lode.intValue(MachineStat.SUPER_OUTPUT_CADENCE) == 16 && lode.intValue(MachineStat.BANK_MEMORY) == 4,
                "Mother Lode fixes the cadence at 16 and the root remembers four inputs");
        require(MegaPassiveTree.has(assayer, "COMPOUND_YIELD") && !MegaPassiveTree.has(assayer, "REFINERS_OATH"), "allocated notables grant their behaviors");
        require(AscendancyCatalog.grantedStats(rockbreaker, CRUSHER).equals(List.of(MachineStat.HARDNESS_TOLERANCE, MachineStat.JAM_RECOVERY,
                MachineStat.JAM_CHANCE, MachineStat.UNDER_LEVEL_EFFICIENCY)), "the Stats tab lists declared stats in node order, without shared stats");
        require(AscendancyCatalog.grantedStats(rockbreaker, MachineMasteryFamily.FURNACE).isEmpty(), "another family shows no ascendancy stats");

        // The loop bound for Furnace recipes assumes the Bloom Ledger stays under one extra item per smelt.
        var bloomer = new MachineProgressionState(0, 0, 1, List.of(), MachineMasteryFamily.FURNACE.startNodeId(), List.of(), false, "bloomer",
                List.of("rich_rate", "rich_blooms", "patient_rate", "patient_bloom", "line_rate", "crusher_line"), 3);
        var furnace = MachineStatAccumulator.componentBase(Map.of(MachineStat.PROCESSING_SPEED, 1.0));
        MegaPassiveTree.applyStats(furnace, bloomer, MachineMasteryFamily.FURNACE);
        double strongest = AscendancyFormulas.ledgerShare(furnace, 1, true);
        require(strongest > 0.7 && strongest < 0.8, "the strongest Bloom Ledger banks about 0.77 of an item per crushed smelt: " + strongest);
    }

    private static void bonusBanks() {
        BonusBanks<String> banks = new BonusBanks<>(String::equals);
        banks.select("iron");
        banks.bank(0.5);
        banks.select("copper");
        require(banks.progressFor("iron") == 0.0 && banks.currentKey().equals("copper"), "one bank of memory resets when the input changes");
        require(!banks.wouldEvictProgress("tin"), "switching away from an empty bank loses nothing");
        banks.bank(0.25);
        require(banks.wouldEvictProgress("tin") && !banks.wouldEvictProgress("copper"), "a new input would push out banked progress");

        banks.setMemory(3);
        banks.select("iron");
        banks.bank(0.5);
        banks.select("tin");
        banks.select("copper");
        near(banks.currentProgress(), 0.25, "Bank Memory restores a remembered input's progress");
        near(banks.progressFor("iron"), 0.5, "other inputs keep their banks");
        banks.select("gold");
        require(banks.progressFor("iron") == 0.0 && banks.banks().size() == 3, "the oldest input is forgotten past the memory");

        banks.setMemory(1);
        require(banks.banks().size() == 1 && banks.currentKey().equals("gold"), "less memory keeps the most recent inputs");
        require(!banks.countCycle(3) && !banks.countCycle(3) && banks.countCycle(3) && !banks.countCycle(3), "a cadence of three fires every third cycle");
        require(!banks.countCycle(0), "a cadence below one never fires");
        banks.bank(100);
        near(banks.currentProgress(), BonusBanks.MAX_PROGRESS, "a bank holds at most its maximum");
        banks.resetCurrent();
        require(banks.currentProgress() == 0.0 && banks.currentKey().equals("gold"), "a reset empties the bank but keeps the input");
    }

    private static void outputLedger() {
        OutputLedger<String> ledger = new OutputLedger<>();
        ledger.add("iron", 0.4);
        ledger.add("iron", 0.4);
        require(ledger.payable("iron") == 0, "a ledger pays only whole items");
        ledger.add("iron", 0.4);
        require(ledger.payable("iron") == 1, "three smelts at 0.4 pay one item");
        ledger.pay("iron", 1);
        near(ledger.progress("iron"), 0.2, "a payout keeps the remainder");
        ledger.pay("iron", 1);
        require(ledger.progress("iron") == 0.0, "an overpaid entry clears");
        for (int item = 0; item <= OutputLedger.MAX_ENTRIES; item++) {
            ledger.add("item" + item, 0.5);
        }
        require(ledger.entries().size() == OutputLedger.MAX_ENTRIES && ledger.progress("item0") == 0.0, "the ledger forgets its least recent output");
    }

    private static void formulas() {
        var stats = MachineStatAccumulator.componentBase(Map.of(MachineStat.JAM_CHANCE, 1.0, MachineStat.OUTPUT_AMOUNT, 1.0));
        require(AscendancyFormulas.penalizedDeficit(3, stats) == 3, "without tolerance every missing level counts");
        near(AscendancyFormulas.underLevelPenaltyMultiplier(2, 1.0, stats), 3.0, "each penalized level adds the configured multiplier");
        require(AscendancyFormulas.jamChancePerThousand(2, 50, stats) == 100 && AscendancyFormulas.jamTicks(2, 40, stats) == 80,
                "base jam chance and duration are unchanged without ascendancy stats");
        apply(stats, MachineStat.HARDNESS_TOLERANCE, ModifierOperation.ADD, 2);
        apply(stats, MachineStat.UNDER_LEVEL_EFFICIENCY, ModifierOperation.ADD, 95);
        apply(stats, MachineStat.JAM_CHANCE, ModifierOperation.LESS, 0.5);
        apply(stats, MachineStat.JAM_RECOVERY, ModifierOperation.ADD, 50);
        require(AscendancyFormulas.penalizedDeficit(3, stats) == 1 && AscendancyFormulas.penalizedDeficit(1, stats) == 0, "tolerance removes missing levels");
        near(AscendancyFormulas.underLevelPenaltyMultiplier(2, 1.0, stats), 1.2, "Under-Level Efficiency stops at 90%");
        require(AscendancyFormulas.jamChancePerThousand(2, 50, stats) == 50 && AscendancyFormulas.jamTicks(2, 40, stats) == 54,
                "Jam Chance scales the chance and Jam Recovery shortens the jam");
        near(AscendancyFormulas.refinersOathMultiplier(1), 1.05, "a one-job chassis gets 5% more Output Amount");
        near(AscendancyFormulas.refinersOathMultiplier(4), 1.2, "Tungstensteel's four jobs give 20%");
        near(AscendancyFormulas.refinersOathMultiplier(9), 1.45, "Exotic's nine jobs give 45%");
        near(AscendancyFormulas.refinersOathMultiplier(20), 1.5, "Refiner's Oath stops at 50%");
        apply(stats, MachineStat.OUTPUT_AMOUNT, ModifierOperation.INCREASED_PERCENT, 10);
        near(stats.valueWithIncreased(MachineStat.OUTPUT_AMOUNT, 20), 1.3, "At-Level Output joins the increased bucket");

        var heat = MachineStatAccumulator.componentBase(Map.of());
        near(AscendancyFormulas.overdriveSpeedMultiplier(1500, 1000, heat), 1.0, "no Overdrive without the Crucible Keeper");
        apply(heat, MachineStat.OVERDRIVE_SPEED, ModifierOperation.ADD, 1);
        apply(heat, MachineStat.OVERDRIVE_CAP, ModifierOperation.ADD, 30);
        near(AscendancyFormulas.overdriveSpeedMultiplier(1200, 1000, heat), 1.2, "1% speed per 10 degrees over target");
        near(AscendancyFormulas.overdriveSpeedMultiplier(1600, 1000, heat), 1.3, "Overdrive stops at its cap");
        near(AscendancyFormulas.overdriveSpeedMultiplier(900, 1000, heat), 1.0, "a lane below target gains nothing");
        apply(heat, MachineStat.LEDGER_RATE, ModifierOperation.ADD, 100.0 / 9.0);
        near(AscendancyFormulas.ledgerShare(heat, 1, false), 1.0 / 9.0, "the root banks a ninth of an item per smelt");
        near(AscendancyFormulas.ledgerShare(heat, 1, true), 2.0 / 9.0, "Crusher Line feeds crushed smelts twice");
    }

    /** Phase 7 formulas: input savings, heat windows, streaks, and fluid yield. */
    private static void familyFormulas() {
        require(AscendancyFormulas.savableIngredient(new int[] {3, 1, 1}) == 0, "the largest ingredient is the one saved");
        require(AscendancyFormulas.savableIngredient(new int[] {1, 4, 4}) == 1, "a tie saves the first largest ingredient");
        require(AscendancyFormulas.savableIngredient(new int[] {1, 1}) == -1, "no saving removes an ingredient's only unit");
        var flux = MachineStatAccumulator.componentBase(Map.of());
        apply(flux, MachineStat.FLUX_RATE, ModifierOperation.ADD, 28);
        near(AscendancyFormulas.fluxShare(flux, true), 0.56, "Reactive Flux doubles the strongest Flux Rate to 0.56 of a unit");
        near(AscendancyFormulas.fluxShare(flux, false), 0.28, "Flux Rate is a share of one unit per craft");

        var window = MachineStatAccumulator.componentBase(Map.of());
        require(AscendancyFormulas.windowEdge(1000, 900, window) == 900, "the authored window is unchanged without Heat Window");
        apply(window, MachineStat.HEAT_WINDOW, ModifierOperation.ADD, -25);
        require(AscendancyFormulas.windowEdge(1000, 900, window) == 925 && AscendancyFormulas.windowEdge(1000, 1200, window) == 1150,
                "Drop Hammer narrows both edges toward the target");

        var streak = MachineStatAccumulator.componentBase(Map.of());
        apply(streak, MachineStat.STREAK_FLOOR, ModifierOperation.ADD, 1);
        apply(streak, MachineStat.STREAK_CAP, ModifierOperation.ADD, 10);
        require(AscendancyFormulas.streakFloor(4, streak) == 4 && AscendancyFormulas.streakFloor(40, streak) == 10,
                "the streak raises the floor by one per calibration up to its cap");

        require(AscendancyFormulas.yieldedFluid(1000, 0) == 1000 && AscendancyFormulas.yieldedFluid(1000, 59) == 1590,
                "Fluid Yield adds to the recipe amount");
        var vessel = new MachineProgressionState(0, 0, 1, List.of(), MachineMasteryFamily.MELTER.startNodeId(), List.of(), false, "pressure_vessel",
                List.of("loop_yield", "brine_loop", "autoclave_yield", "autoclave", "trap_yield"), 3);
        var melter = MachineStatAccumulator.componentBase(Map.of(MachineStat.PROCESSING_SPEED, 1.0, MachineStat.FLUID_CAPACITY, 4000.0));
        MegaPassiveTree.applyStats(melter, vessel, MachineMasteryFamily.MELTER);
        near(melter.value(MachineStat.FLUID_YIELD) + 25, 59, "the strongest Electrolyte Solution yield stays at the loop bound's 59%");
        near(melter.value(MachineStat.FLUID_CAPACITY), 12000, "Pressurized Tanks triple the tanks");
    }

    /** Phase 7 launch order and the worst-case values the loop bounds rely on. */
    private static void familyLaunchContent() {
        Map<MachineMasteryFamily, List<String>> launch = Map.of(
                MachineMasteryFamily.ALLOY_FURNACE, List.of("metallurgist", "blendwright"),
                MachineMasteryFamily.METAL_PRESS, List.of("die_keeper", "drop_forge"),
                MachineMasteryFamily.RESONANCE_CALIBRATOR, List.of("harmonist", "mass_tuner"),
                MachineMasteryFamily.MELTER, List.of("pressure_vessel", "twin_crucible"),
                MachineMasteryFamily.FORESTRY, List.of("timber_baron", "grove_warden"));
        launch.forEach((family, ids) -> require(AscendancyCatalog.forFamily(family).stream().map(Ascendancy::id).toList().subList(0, 2).equals(ids),
                family + " ships " + ids));

        var baron = ascendedForestry("timber_baron", List.of("heart_ledger", "fell_reach", "sawyers_eye", "charter_reach", "clearcut_charter"));
        var baronStats = forestryStats(baron);
        near(baronStats.value(MachineStat.LEDGER_RATE), 28, "the strongest Timber Baron Ledger Rate stays at the bound's 28%");
        near(AscendancyFormulas.ledgerShare(baronStats, 1, true), 0.56, "Heartwood doubles it to the bound's 56% of a batch log");
        require(MegaPassiveTree.has(baron, "CLEARCUT_CHARTER") && MegaPassiveTree.has(baron, "LOG_LEDGER"), "Clearcut Charter builds on the Log Ledger");
        var sprinter = forestryStats(ascendedForestry("timber_baron", List.of("rail_speed", "dock_sprint", "rolling_speed", "rolling_harvest")));
        near(sprinter.value(MachineStat.CART_SPEED), 1.3, "two Cart Speed nodes make the cart 30% faster");
        near(sprinter.valueWithIncreased(MachineStat.CART_SPEED, 50), 1.8, "Dock Sprint adds 50% while seeking a station");

        var warden = ascendedForestry("grove_warden", List.of("soil_pulse", "rich_soil", "surge_pulse", "verdant_surge"));
        var wardenStats = forestryStats(warden);
        near(wardenStats.value(MachineStat.GROWTH_PULSE), 1.7, "the strongest Growth Pulse applies bone meal 1.7 times");
        require(AscendancyFormulas.pulseAttempts(wardenStats, 0.5) == 2 && AscendancyFormulas.pulseAttempts(wardenStats, 0.8) == 1,
                "the fraction of Growth Pulse is a chance for one more use");
        require(MegaPassiveTree.has(warden, "FERTILIZER_PULSE"), "the Grove Warden root spends bone meal on each pulse");
        var nursery = ascendedForestry("grove_warden", List.of("nursery_cells", "nursery", "grove_cells", "ancient_grove"));
        require(MegaPassiveTree.passive(nursery, PassiveStatType.MANAGED_CELLS) == 32, "Spare Plots and Old Rows add 32 managed cells");
        near(forestryStats(ascendedForestry("grove_warden", List.of())).value(MachineStat.WORK_RANGE), 1, "a cart works one row per side");
        near(forestryStats(nursery).value(MachineStat.WORK_RANGE), 3, "Nursery widens the cart to three rows per side");
        near(forestryStats(sharedForestry(List.of("far_rows"))).value(MachineStat.WORK_RANGE), 2, "Far Rows adds a row");
        near(forestryStats(sharedForestry(List.of("far_rows", "outer_rows"))).value(MachineStat.WORK_RANGE), 3, "Outer Rows adds another");
        var farRows = sharedForestry(List.of("far_rows"));
        require(MegaPassiveTree.allocationPath(node -> farRows.allocatedNodes().contains(node.id()), MegaPassiveTree.node("outer_rows")).size() >= 8,
                "the two shared Work Range notables sit in separate, distant clusters");
        var idle = forestryStats(sharedForestry(List.of("open_track")));
        require(AscendancyFormulas.idleCartSpeedPercent(idle, AscendancyFormulas.IDLE_CART_TICKS - 1) == 0.0, "Idle Cart Speed waits 4 seconds");
        require(AscendancyFormulas.idleCartSpeedPercent(idle, AscendancyFormulas.IDLE_CART_TICKS) > 0.0, "Idle Cart Speed applies after 4 seconds");
        var driven = sharedForestry(List.of("driven_wheels"));
        var drivenStats = forestryStats(driven);
        require(drivenStats.value(MachineStat.CART_SPEED) > forestryStats(sharedForestry(List.of("greased_rails"))).value(MachineStat.CART_SPEED),
                "Driven Wheels scales Cart Speed from Drive");
        require(AscendancyFormulas.pulseAttempts(forestryStats(ascendedForestry("grove_warden", List.of())), 0.0) == 1, "the root pulses once");

        require(AscendancyCatalog.forFamily(MachineMasteryFamily.FORESTRY).stream().map(Ascendancy::id).toList()
                .equals(List.of("timber_baron", "grove_warden", "field_hand")), "Forestry ships Timber Baron, Grove Warden, and Field Hand");
        var fieldHand = ascendedForestry("field_hand", List.of());
        require(MegaPassiveTree.has(fieldHand, "CROP_TENDING") && MegaPassiveTree.has(fieldHand, "NO_TREE_WORK"),
                "the Field Hand root tends crops and gives up tree work");
        near(forestryStats(fieldHand).value(MachineStat.WORK_RANGE), 2, "the Field Hand root adds a row");
        var openFields = ascendedForestry("field_hand", List.of("furrow_cells", "wide_furrows", "long_furrows", "open_fields"));
        near(forestryStats(openFields).value(MachineStat.WORK_RANGE), 4, "Wide Furrows and Open Fields reach four rows per side");
        require(MegaPassiveTree.passive(openFields, PassiveStatType.MANAGED_CELLS) == 64, "the furrow branch adds 64 managed cells");
        Ascendancy field = AscendancyCatalog.get("field_hand");
        for (String deep : List.of("open_fields", "irrigation", "rolling_reap")) {
            require(field.isDeep(field.node(deep)), deep + " is a deep notable");
        }
        var sprinkler = ascendedForestry("field_hand", List.of("hose_fittings", "sprinkler", "wide_nozzles", "irrigation"));
        require(MegaPassiveTree.has(sprinkler, "SPRINKLER") && MegaPassiveTree.has(sprinkler, "IRRIGATION"), "Irrigation builds on the Sprinkler");
        near(forestryStats(sprinkler).value(MachineStat.FLUID_CAPACITY), 12000, "the sprinkler branch's two small nodes grow the 8,000 mB tank by half");
    }

    /** Every family's crest faces away from the tree's center and sits clear of links and nodes at each zoom. */
    private static void crestPlacement() {
        List<MegaPassiveNode> nodes = MegaPassiveTree.TREE.nodes();
        int count = nodes.size();
        float[] centerX = new float[count];
        float[] centerY = new float[count];
        float[] radius = new float[count];
        List<Float> segments = new java.util.ArrayList<>();
        for (int position = 0; position < count; position++) {
            MegaPassiveNode node = nodes.get(position);
            centerX[position] = node.x() + node.size() / 2.0F;
            centerY[position] = node.y() + node.size() / 2.0F;
            radius[position] = node.size() / 2.0F;
            for (PassiveNode parent : node.parents()) {
                if (parent.index() >= node.index()) {
                    continue;
                }
                var path = parent.linkPathTo(node);
                for (int index = 1; index < path.size(); index++) {
                    segments.addAll(List.of((float) path.get(index - 1).x(), (float) path.get(index - 1).y(), (float) path.get(index).x(), (float) path.get(index).y()));
                }
            }
        }
        float[] links = new float[segments.size()];
        for (int index = 0; index < links.length; index++) {
            links[index] = segments.get(index);
        }
        for (MachineMasteryFamily family : MachineMasteryFamily.values()) {
            int start = nodes.indexOf(MegaPassiveTree.node(family.startNodeId()));
            double angle = MasteryCrestPlacement.angle(centerX, centerY, radius, links, start);
            double outward = MasteryCrestPlacement.outward(centerX, centerY, start);
            require(Math.abs(Math.IEEEremainder(angle - outward, Math.PI * 2.0D)) <= Math.toRadians(20.0D),
                    family + " crest faces away from the tree's center, turned at most 20 degrees for room");
            for (double zoom : new double[] {0.5D, 0.7D, 1.0D, 1.5D, 2.0D}) {
                require(MasteryCrestPlacement.clearance(centerX, centerY, radius, links, start, angle, zoom) > 0.0D,
                        family + " crest clears links and nodes at zoom " + zoom);
            }
        }
    }

    private static MachineProgressionState ascendedForestry(String ascendancy, List<String> nodes) {
        return new MachineProgressionState(0, 0, 1, List.of(), MachineMasteryFamily.FORESTRY.startNodeId(), List.of(), false, ascendancy, nodes, 3);
    }

    /** A Forestry build that allocates the shortest routes to the given shared-tree nodes. */
    private static MachineProgressionState sharedForestry(List<String> targets) {
        String start = MachineMasteryFamily.FORESTRY.startNodeId();
        java.util.Set<String> allocated = new java.util.LinkedHashSet<>();
        for (String target : targets) {
            MegaPassiveTree.allocationPath(node -> node.id().equals(start) || allocated.contains(node.id()), MegaPassiveTree.node(target))
                    .forEach(node -> allocated.add(node.id()));
        }
        return new MachineProgressionState(0, 0, MachineProgressionState.MAX_LEVEL, List.copyOf(allocated), start, List.of(), false, "", List.of(), 0);
    }

    private static MachineStatAccumulator forestryStats(MachineProgressionState state) {
        var stats = MachineStatAccumulator.forestryCompanionBase();
        MegaPassiveTree.applyStats(stats, state, MachineMasteryFamily.FORESTRY);
        return stats;
    }

    private static void apply(MachineStatAccumulator stats, MachineStat stat, ModifierOperation operation, double value) {
        stats.apply(new MachineModifier(ModifierSlot.IMPLICIT, stat, operation, value));
    }

    private static MachineProgressionState ascended(String ascendancy, List<String> nodes, int tiers) {
        return new MachineProgressionState(0, 0, 1, List.of(), CRUSHER.startNodeId(), List.of(), false, ascendancy, nodes, tiers);
    }

    private static void rejects(Consumer<JsonObject> mutation, String fragment) {
        JsonObject raw = MasteryNodeJson.resource(DIRECTORY + "fixtures/crusher_alpha.json").deepCopy();
        mutation.accept(raw);
        try {
            AscendancyCatalog.parse(raw);
            require(false, "rejects an ascendancy where " + fragment);
        } catch (IllegalStateException exception) {
            require(exception.getMessage().contains(fragment), "rejects an ascendancy where " + fragment + ": " + exception.getMessage());
        }
    }

    private static JsonArray nodes(JsonObject raw) { return raw.getAsJsonArray("nodes"); }

    private static JsonObject node(JsonObject raw, String id) {
        for (JsonElement element : nodes(raw)) {
            if (element.getAsJsonObject().get("id").getAsString().equals(id)) { return element.getAsJsonObject(); }
        }
        throw new AssertionError("Missing fixture node " + id);
    }

    private static void behaviors(JsonObject raw, String id, String behavior) {
        JsonArray array = new JsonArray(); array.add(behavior); node(raw, id).add("behaviors", array);
    }

    private static JsonArray effect(String stat) {
        JsonObject effect = new JsonObject();
        effect.addProperty("stat", stat); effect.addProperty("operation", "INCREASED_PERCENT"); effect.addProperty("value", 5);
        JsonArray array = new JsonArray(); array.add(effect);
        return array;
    }

    private static final class Host implements MachineMasteryHost {
        private final Predicate<MachineProgressionState> gear;
        private MachineProgressionState state;
        private int entryStage = AscendancyCatalog.ENTRY_STAGE;

        private Host(MachineProgressionState state, Predicate<MachineProgressionState> gear) {
            this.state = state;
            this.gear = gear;
        }

        @Override public MachineMasteryFamily masteryFamily() { return CRUSHER; }
        @Override public MachineProgressionState machineProgression() { return state; }
        @Override public void setMachineProgression(MachineProgressionState state) { this.state = state; }
        @Override public int ascendancyEntryStage() { return entryStage; }
        @Override public boolean masteryGearAllows(MachineProgressionState state) { return gear.test(state); }
    }

    private static void near(double actual, double expected, String label) { require(Math.abs(actual - expected) < 0.00001, label + ": " + actual); }
    private static void require(boolean condition, String label) { checks++; if (!condition) { throw new AssertionError(label); } }
}
