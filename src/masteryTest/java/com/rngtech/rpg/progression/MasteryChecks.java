package com.rngtech.rpg.progression;

import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.ModifierOperation;
import com.rngtech.rpg.ModifierSlot;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Executable domain checks without launching a client or server. */
public final class MasteryChecks {
    private static int checks;

    public static void main(String[] args) {
        keywordMath();
        savesAndMigration();
        graphAndBuilds();
        attributesAndConstraints();
        System.out.println("Machine mastery: " + checks + " checks passed");
    }

    private static void keywordMath() {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        effect(stats, ModifierOperation.ADD, 120);
        effect(stats, ModifierOperation.INCREASED_PERCENT, 30);
        effect(stats, ModifierOperation.DECREASED_PERCENT, 10);
        effect(stats, ModifierOperation.MORE, 1.5);
        effect(stats, ModifierOperation.LESS, 0.8);
        near(stats.value(MachineStat.MAX_TEMPERATURE), 172.8, "increased/reduced bucket followed by independent more/less");
        MachineStatAccumulator less = new MachineStatAccumulator(); effect(less, ModifierOperation.ADD, 100);
        effect(less, ModifierOperation.LESS, 0.5); effect(less, ModifierOperation.LESS, 0.5);
        near(less.value(MachineStat.MAX_TEMPERATURE), 25, "two 50% less modifiers multiply");
        MachineStatAccumulator reduced = new MachineStatAccumulator(); effect(reduced, ModifierOperation.ADD, 100);
        effect(reduced, ModifierOperation.DECREASED_PERCENT, 50); effect(reduced, ModifierOperation.DECREASED_PERCENT, 50);
        near(reduced.value(MachineStat.MAX_TEMPERATURE), 0, "two 50% reduced modifiers add");
        effect(less, ModifierOperation.LESS, 0); effect(less, ModifierOperation.INCREASED_PERCENT, 1000);
        near(less.value(MachineStat.MAX_TEMPERATURE), 0, "100% less cannot be offset by increased");
        stats.setAbsolute(MachineStat.MAX_TEMPERATURE, 800);
        effect(stats, ModifierOperation.MORE, 100);
        near(stats.value(MachineStat.MAX_TEMPERATURE), 800, "fixed temperature ignores subsequent ordinary modifiers");
        stats.setAbsolute(MachineStat.MAX_TEMPERATURE, 600);
        stats.setAbsolute(MachineStat.MAX_TEMPERATURE, 1000);
        near(stats.value(MachineStat.MAX_TEMPERATURE), 600, "lower fixed value wins");
        stats.capAbsolute(MachineStat.MAX_TEMPERATURE, 500);
        near(stats.value(MachineStat.MAX_TEMPERATURE), 500, "lower absolute ceiling wins");
    }

    private static void savesAndMigration() {
        var legacy = JsonParser.parseString("{\"xp\":5334600,\"xp_remainder\":3,\"level\":30,\"spent_points\":20,\"unlocked_node_mask\":-1,\"unlocked_node_mask_high\":-1}");
        MachineProgressionState migrated = MachineProgressionState.CODEC.parse(JsonOps.INSTANCE, legacy).getOrThrow();
        require(migrated.xp() == 5334600 && migrated.xpRemainder() == 3 && migrated.level() == 30, "preserve legacy experience and level");
        require(migrated.allocatedNodes().isEmpty() && migrated.unspentPoints() == 29, "refund legacy masks");
        require(MachineProgressionState.EMPTY.forFamily(MachineMasteryFamily.CRUSHER).unspentPoints() == 0, "new chassis starts fresh");
        MachineProgressionState original = build(MachineMasteryFamily.CRUSHER, "soft_material_specialist");
        var json = MachineProgressionState.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        require(original.equals(MachineProgressionState.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow()), "stable IDs survive codec roundtrip");
        var wire = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), net.minecraft.core.RegistryAccess.EMPTY);
        try {
            var following = original.withTarget(original.allocatedNodes());
            MachineProgressionState.STREAM_CODEC.encode(wire, following);
            require(following.equals(MachineProgressionState.STREAM_CODEC.decode(wire)), "network codec preserves stable allocations and target order");
        } finally { wire.release(); }
        require(original.allocatedNodes().stream().anyMatch(id -> MegaPassiveTree.node(id).index() > 127), "save exercises nodes beyond old mask limit");
        require(MachineProgressionState.xpQuarters(40, 37) == 0, "outleveled work grants no XP");
        require(MachineProgressionState.workXp(10, 78) > MachineProgressionState.workXp(10, 1) * 100, "advanced work accelerates catch-up");
        for (int level = 2; level <= 80; level++) { require(MachineProgressionState.xpForLevel(level) > MachineProgressionState.xpForLevel(level - 1), "increasing level thresholds"); }
    }

    private static void graphAndBuilds() {
        require(MegaPassiveTree.TREE.nodes().size() == 750, "full shared catalog");
        for (MegaPassiveNode root : MegaPassiveTree.TREE.nodes().stream().filter(n -> n.kind() == PassiveNodeKind.STARTER).toList()) {
            require(root.links().size() == 3, "three exits per starter");
            for (MegaPassiveNode key : MegaPassiveTree.TREE.nodes().stream().filter(n -> n.kind() == PassiveNodeKind.KEYSTONE).toList()) {
                List<String> path = path(root.id(), key.id());
                require(path.size() >= 7 && path.size() <= 79, "reachable keystone commitment from each start");
                require(MegaPassiveTree.validBuild(root.id(), path), "legal allocation order");
            }
        }
        var full = build(MachineMasteryFamily.CRUSHER, "soft_material_specialist");
        var code = MasteryBuildCode.decode(MasteryBuildCode.copy(full).encode());
        for (int i = 0; i < 14; i++) {
            int level = i % 3 == 0 ? 10 : i % 3 == 1 ? 25 : 80;
            var fresh = new MachineProgressionState(MachineProgressionState.xpForLevel(level), 0, level, List.of(), code.start(), List.of(), false);
            var partial = MegaPassiveTree.follow(fresh.withTarget(code.nodes()), state -> true);
            require(partial.spentPoints() == Math.min(level - 1, code.nodes().size()), "paste respects each machine's earned points");
            var finished = MegaPassiveTree.follow(partial.withAddedXp(MachineProgressionState.xpForLevel(80)), state -> true);
            require(finished.allocatedNodes().equals(code.nodes()), "14 machines follow the same target");
        }
        var fresh = MachineProgressionState.EMPTY.forFamily(MachineMasteryFamily.CRUSHER).withAddedXp(MachineProgressionState.xpForLevel(80));
        var paused = MegaPassiveTree.follow(fresh.withTarget(code.nodes()), state -> !state.allocatedNodes().contains("soft_material_specialist"));
        require(!paused.following() && !paused.allocatedNodes().contains("soft_material_specialist"), "stop at gear conflict");
        var resumed = MegaPassiveTree.follow(paused.withFollowing(true), state -> true);
        require(resumed.allocatedNodes().equals(code.nodes()), "resume after conflict resolves");
        require(full.withoutNode(code.nodes().getFirst()).equals(full), "refund cannot disconnect allocations");
        require(full.withoutNode("soft_material_specialist").spentPoints() == full.spentPoints() - 1, "leaf refund returns one point");
        require(!full.withTarget(code.nodes()).withoutNode("soft_material_specialist").following(), "manual refund pauses automatic allocation");
        require(!MegaPassiveTree.validBuild(code.start(), List.of("soft_material_specialist")), "reject disconnected template");
        require(!MegaPassiveTree.validBuild(code.start(), List.of("start_control")), "foreign starts do not become free roots");
        require(!MegaPassiveTree.validBuild(code.start(), List.of(code.nodes().getFirst(), code.nodes().getFirst())), "duplicate allocations are rejected");
        var overBudget = new ArrayList<>(code.nodes());
        while (overBudget.size() < 80) { overBudget.add(code.nodes().getFirst()); }
        require(!MegaPassiveTree.validBuild(code.start(), overBudget), "target builds cannot exceed the point ceiling");
        require(fresh.withUnlockedNode(MegaPassiveTree.node("soft_material_specialist").index()).equals(fresh), "reject forged distant unlock");
    }

    private static void attributesAndConstraints() {
        var transfigured = build(MachineMasteryFamily.CRUSHER, "attribute_transfiguration");
        var stats = new MachineStatAccumulator(); MegaPassiveTree.applyStats(stats, transfigured, MachineMasteryFamily.CRUSHER);
        require(stats.value(MachineStat.DRIVE) > 0, "no inherent bonuses preserves attribute totals");
        require(MegaPassiveTree.has(transfigured, "NO_INHERENT_ATTRIBUTES"), "attribute suppression is explicit");
        var direct = MachineStatAccumulator.componentBase(Map.of(MachineStat.PROCESSING_SPEED, 1.0, MachineStat.STABILITY, 1.0));
        for (String id : transfigured.allocatedNodes()) {
            for (var effect : MegaPassiveTree.node(id).effects()) {
                if (MachineMasteryFamily.CRUSHER.supports(effect.stat())) {
                    direct.apply(new MachineModifier(ModifierSlot.IMPLICIT, effect.stat(), effect.operation(), effect.value()));
                }
            }
        }
        var suppressed = MachineStatAccumulator.componentBase(Map.of(MachineStat.PROCESSING_SPEED, 1.0, MachineStat.STABILITY, 1.0));
        MegaPassiveTree.applyStats(suppressed, transfigured, MachineMasteryFamily.CRUSHER);
        near(suppressed.value(MachineStat.STABILITY), direct.value(MachineStat.STABILITY), "suppression removes Control's inherent stability conversion");
        var explicit = build(MachineMasteryFamily.CRUSHER, "reserve_actuation");
        var explicitStats = MachineStatAccumulator.componentBase(Map.of(MachineStat.PROCESSING_SPEED, 1.0, MachineStat.RESERVE, 100.0));
        var zeroReserve = MachineStatAccumulator.componentBase(Map.of(MachineStat.PROCESSING_SPEED, 1.0));
        MegaPassiveTree.applyStats(explicitStats, explicit, MachineMasteryFamily.CRUSHER);
        MegaPassiveTree.applyStats(zeroReserve, explicit, MachineMasteryFamily.CRUSHER);
        require(explicitStats.value(MachineStat.PROCESSING_SPEED) > zeroReserve.value(MachineStat.PROCESSING_SPEED), "explicit Reserve scaling survives no inherent bonuses");
        var inactiveBenefit = build(MachineMasteryFamily.CRUSHER, "closed_loop_recuperator");
        var activePenalty = MachineStatAccumulator.componentBase(Map.of(MachineStat.PROCESSING_SPEED, 1.0));
        var noPenalty = MachineStatAccumulator.componentBase(Map.of(MachineStat.PROCESSING_SPEED, 1.0));
        MegaPassiveTree.applyStats(activePenalty, inactiveBenefit, MachineMasteryFamily.CRUSHER);
        MegaPassiveTree.applyStats(noPenalty, inactiveBenefit.withoutNode("closed_loop_recuperator"), MachineMasteryFamily.CRUSHER);
        near(activePenalty.value(MachineStat.PROCESSING_SPEED), noPenalty.value(MachineStat.PROCESSING_SPEED) * 0.85, "irrelevant Furnace behavior still applies the speed penalty to a Crusher");
        var crusher = build(MachineMasteryFamily.CRUSHER, "low_heat_specialist");
        var crusherStats = new MachineStatAccumulator(); MegaPassiveTree.applyStats(crusherStats, crusher, MachineMasteryFamily.CRUSHER);
        near(crusherStats.value(MachineStat.MAX_TEMPERATURE), 0, "irrelevant fixed heat does not create a Crusher heat capability");
        var furnace = build(MachineMasteryFamily.FURNACE, "low_heat_specialist");
        var furnaceStats = new MachineStatAccumulator(); MegaPassiveTree.applyStats(furnaceStats, furnace, MachineMasteryFamily.FURNACE);
        effect(furnaceStats, ModifierOperation.ADD, 4000); effect(furnaceStats, ModifierOperation.MORE, 3);
        near(furnaceStats.value(MachineStat.MAX_TEMPERATURE), 800, "gear cannot exceed absolute temperature");
        var lowHardness = build(MachineMasteryFamily.CRUSHER, "soft_material_specialist");
        require(MegaPassiveTree.acceptsHardness(lowHardness, 2) && !MegaPassiveTree.acceptsHardness(lowHardness, 3), "hard recipe ceiling");
    }

    private static MachineProgressionState build(MachineMasteryFamily family, String destination) {
        return new MachineProgressionState(MachineProgressionState.xpForLevel(80), 0, 80,
                path(family.startNodeId(), destination), family.startNodeId(), List.of(), false);
    }
    private static List<String> path(String start, String destination) {
        Map<String, String> previous = new HashMap<>(); previous.put(start, "");
        ArrayDeque<String> queue = new ArrayDeque<>(); queue.add(start);
        while (!queue.isEmpty() && !previous.containsKey(destination)) {
            String id = queue.remove();
            for (String next : MegaPassiveTree.node(id).links()) {
                if (!previous.containsKey(next) && MegaPassiveTree.node(next).kind() != PassiveNodeKind.STARTER) {
                    previous.put(next, id); queue.add(next);
                }
            }
        }
        if (!previous.containsKey(destination)) { throw new AssertionError("Unreachable " + destination); }
        List<String> path = new ArrayList<>();
        for (String id = destination; !id.equals(start); id = previous.get(id)) { path.addFirst(id); }
        return path;
    }
    private static void effect(MachineStatAccumulator stats, ModifierOperation operation, double value) {
        stats.apply(new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.MAX_TEMPERATURE, operation, value));
    }
    private static void near(double actual, double expected, String label) { require(Math.abs(actual - expected) < 0.00001, label + ": " + actual); }
    private static void require(boolean condition, String label) { checks++; if (!condition) { throw new AssertionError(label); } }
}
