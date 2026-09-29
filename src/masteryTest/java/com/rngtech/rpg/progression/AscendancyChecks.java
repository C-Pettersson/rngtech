package com.rngtech.rpg.progression;

import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

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
        mainCatalogAssets();
        return checks;
    }

    private static void catalog() {
        require(AscendancyCatalog.forFamily(CRUSHER).stream().map(Ascendancy::id).toList().equals(List.of(ALPHA, BETA, GAMMA)),
                "a family can have more than two ascendancies, listed in index order");
        require(AscendancyCatalog.forFamily(MachineMasteryFamily.FURNACE).stream().map(Ascendancy::id).toList().equals(List.of(FURNACE)),
                "ascendancies belong to their own family");
        require(AscendancyCatalog.forFamily(MachineMasteryFamily.MELTER).isEmpty(), "a family without entries has no ascendancies");
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
    }

    private static void declarations() {
        require(CRUSHER.supports(MachineStat.LUCK) && !MachineMasteryFamily.FURNACE.supports(MachineStat.LUCK), "a declared stat applies only to its families");
        require(CRUSHER.supportsBehavior("FIXTURE_ECHO") && !MachineMasteryFamily.FURNACE.supportsBehavior("FIXTURE_ECHO"), "a declared behavior needs no family switch");
        require(CRUSHER.supportsBehavior("DENSE_PARALLEL") && !MachineMasteryFamily.FURNACE.supportsBehavior("DENSE_PARALLEL"), "built-in behaviors are unchanged");
        require(MasteryDeclarations.yield(MachineStat.LUCK) == MasteryDeclarations.Yield.OUTPUT
                && MasteryDeclarations.yield(MachineStat.PROCESSING_SPEED) == MasteryDeclarations.Yield.NONE, "stats declare their yield for the loop audit");
        require(MasteryDeclarations.declared("FIXTURE_ECHO") && !MasteryDeclarations.declared("DENSE_PARALLEL"), "only registered behaviors are declared");
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

        private Host(MachineProgressionState state, Predicate<MachineProgressionState> gear) {
            this.state = state;
            this.gear = gear;
        }

        @Override public MachineMasteryFamily masteryFamily() { return CRUSHER; }
        @Override public MachineProgressionState machineProgression() { return state; }
        @Override public void setMachineProgression(MachineProgressionState state) { this.state = state; }
        @Override public int ascendancyEntryStage() { return AscendancyCatalog.ENTRY_STAGE; }
        @Override public boolean masteryGearAllows(MachineProgressionState state) { return gear.test(state); }
    }

    private static void near(double actual, double expected, String label) { require(Math.abs(actual - expected) < 0.00001, label + ": " + actual); }
    private static void require(boolean condition, String label) { checks++; if (!condition) { throw new AssertionError(label); } }
}
