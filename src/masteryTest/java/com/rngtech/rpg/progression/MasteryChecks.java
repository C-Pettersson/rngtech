package com.rngtech.rpg.progression;

import com.rngtech.content.blockentity.EnergyTransferChecks;
import com.rngtech.content.cable.CableRoutingChecks;
import com.rngtech.content.entity.ForestryCartRulesChecks;
import com.rngtech.content.entity.ForestryTreeScanChecks;
import com.rngtech.rpg.BatchProcessing;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineModifierEffect;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.ModifierOperation;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.PartGenerationChecks;
import com.rngtech.rpg.StatBreakdown;
import com.rngtech.rpg.StatBreakdownChecks;
import com.rngtech.rpg.refinement.RefinementChecks;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Executable domain checks without launching a client or server. */
public final class MasteryChecks {
    private static int checks;

    public static void main(String[] args) {
        keywordMath();
        batchProcessing();
        savesAndMigration();
        graphAndBuilds();
        routeAllocation();
        attributesAndConstraints();
        familyAdapters();
        taggedPayoffs();
        keystonePayoffsNeedTheirCosts();
        bonusSummary();
        statBreakdowns();
        iconTextures();
        checks += AscendancyChecks.run();
        checks += PartGenerationChecks.run();
        checks += RefinementChecks.run();
        checks += StatBreakdownChecks.run();
        System.out.println("Machine mastery: " + checks + " checks passed");
        ForestryTreeScanChecks.run();
        ForestryCartRulesChecks.run();
        EnergyTransferChecks.run();
        CableRoutingChecks.run();
        modifierProfilesLoad();
    }

    /** Every modifier profile validates when its class loads; a broken profile crashes the game on world load instead. */
    private static void modifierProfilesLoad() {
        var profiles = com.rngtech.rpg.ModifierEligibilityProfiles.allProfiles();
        if (profiles.isEmpty()) {
            throw new AssertionError("modifier profiles load");
        }
        System.out.println("Modifier profiles: " + profiles.size() + " load and validate");
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

    private static void batchProcessing() {
        MachineStatAccumulator stats = MachineStatAccumulator.metalPressBase(1000, 100);
        require(BatchProcessing.statBatchSize(stats) == 1, "machines batch one item by default");
        near(BatchProcessing.timeMultiplier(stats, 1), 1.0, "a single item takes the base cycle time");
        near(BatchProcessing.timeMultiplier(stats, 4), 1.75, "every extra item adds 25% of the base cycle time");
        require(BatchProcessing.batchTicks(100, stats, 4) == 175, "batch ticks scale with Batch Overhead");
        effect(stats, MachineStat.BATCH_SIZE, ModifierOperation.ADD, 40);
        require(BatchProcessing.batchSize(stats, false) == BatchProcessing.MAX_BATCH_SIZE, "Batch Size is clamped");
        require(BatchProcessing.batchSize(stats, true) == 1, "a batching opt-out locks one item");
        effect(stats, MachineStat.BATCH_OVERHEAD, ModifierOperation.LESS, 0.01);
        near(BatchProcessing.overhead(stats), 0.05, "Batch Overhead never drops below 5%");
        MachineStatAccumulator calibrator = MachineStatAccumulator.resonanceCalibratorBase(com.rngtech.content.calibration.ResonanceCalibratorChassis.TUNGSTENSTEEL);
        require(BatchProcessing.statBatchSize(calibrator) == 3, "calibrator lanes are its base Batch Size");
        effect(calibrator, MachineStat.BATCH_SIZE, ModifierOperation.INCREASED_PERCENT, 100);
        require(BatchProcessing.statBatchSize(calibrator) == 6, "Resonance Array doubles the chassis batch");
        require(BatchProcessing.largestFitting(5, batch -> batch <= 3) == 3, "the largest batch that fits is locked");
        require(BatchProcessing.largestFitting(5, batch -> false) == 0, "nothing locks when one item does not fit");
        var legacy = new com.google.gson.JsonPrimitive("parallel_jobs");
        require(MachineStat.CODEC.parse(JsonOps.INSTANCE, legacy).getOrThrow() == MachineStat.BATCH_SIZE, "stored Parallel Jobs decode as Batch Size");
        require(MachineStat.fromName("PARALLEL_JOBS") == MachineStat.BATCH_SIZE, "authored Parallel Jobs parse as Batch Size");
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
        for (int level = 2; level <= MachineProgressionState.MAX_LEVEL; level++) { require(MachineProgressionState.xpForLevel(level) > MachineProgressionState.xpForLevel(level - 1), "increasing level thresholds"); }
        require(MachineProgressionState.MAX_LEVEL == 100 && MachineProgressionState.EMPTY.forFamily(MachineMasteryFamily.CRUSHER)
                .withAddedXp(MachineProgressionState.xpForLevel(100)).totalPoints() == 99, "level 100 grants 99 points");
        require(MachineProgressionState.progressionBand(25) == MachineProgressionState.MAX_LEVEL - 2, "top legacy band trains toward the level cap");
        var stale = JsonParser.parseString("{\"tree_version\":1,\"xp\":100,\"level\":2,\"allocated_nodes\":[\"r0_drive_0_0\"],\"start\":\"start_drive\"}");
        require(MachineProgressionState.CODEC.parse(JsonOps.INSTANCE, stale).getOrThrow().allocatedNodes().isEmpty(), "previous tree version refunds allocations");
    }

    private static void graphAndBuilds() {
        require(MegaPassiveTree.TREE.nodes().size() == 1346, "full shared catalog");
        for (MegaPassiveNode root : MegaPassiveTree.TREE.nodes().stream().filter(n -> n.kind() == PassiveNodeKind.STARTER).toList()) {
            require(root.links().size() == 3, "three exits per starter");
            for (MegaPassiveNode key : MegaPassiveTree.TREE.nodes().stream().filter(n -> n.kind() == PassiveNodeKind.KEYSTONE).toList()) {
                List<String> path = path(root.id(), key.id());
                require(path.size() >= 7 && path.size() <= MachineProgressionState.MAX_LEVEL - 1, "reachable keystone commitment from each start");
                require(MegaPassiveTree.validBuild(root.id(), path), "legal allocation order");
            }
        }
        var full = build(MachineMasteryFamily.CRUSHER, "soft_material_specialist");
        var code = MasteryBuildCode.decode(MasteryBuildCode.copy(full).encode());
        for (int i = 0; i < 14; i++) {
            int level = i % 3 == 0 ? 10 : i % 3 == 1 ? 40 : MachineProgressionState.MAX_LEVEL;
            var fresh = new MachineProgressionState(MachineProgressionState.xpForLevel(level), 0, level, List.of(), code.start(), List.of(), false);
            var partial = MegaPassiveTree.follow(fresh.withTarget(code.nodes()), state -> true);
            require(partial.spentPoints() == Math.min(level - 1, code.nodes().size()), "paste respects each machine's earned points");
            var finished = MegaPassiveTree.follow(partial.withAddedXp(MachineProgressionState.xpForLevel(MachineProgressionState.MAX_LEVEL)), state -> true);
            require(finished.allocatedNodes().equals(code.nodes()), "14 machines follow the same target");
        }
        var fresh = MachineProgressionState.EMPTY.forFamily(MachineMasteryFamily.CRUSHER).withAddedXp(MachineProgressionState.xpForLevel(MachineProgressionState.MAX_LEVEL));
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
        while (overBudget.size() <= MegaPassiveTree.MAX_ALLOCATIONS) { overBudget.add(code.nodes().getFirst()); }
        require(!MegaPassiveTree.validBuild(code.start(), overBudget), "target builds cannot exceed the point ceiling");
        List<String> ceiling = breadthFirst(code.start(), MegaPassiveTree.MAX_ALLOCATIONS);
        require(MegaPassiveTree.validBuild(code.start(), ceiling), "target builds can plan the 120-point ceiling");
        var planned = MachineProgressionState.EMPTY.forFamily(MachineMasteryFamily.CRUSHER)
                .withAddedXp(MachineProgressionState.xpForLevel(MachineProgressionState.MAX_LEVEL)).withTarget(ceiling);
        var followed = MegaPassiveTree.follow(planned, state -> true);
        require(followed.targetNodes().size() == 120 && followed.spentPoints() == 99, "level points stop short of the reserved ceiling");
        require(fresh.withUnlockedNode(MegaPassiveTree.node("soft_material_specialist").index()).equals(fresh), "reject forged distant unlock");
    }

    private static void routeAllocation() {
        var fresh = MachineProgressionState.EMPTY.forFamily(MachineMasteryFamily.CRUSHER).withAddedXp(MachineProgressionState.xpForLevel(MachineProgressionState.MAX_LEVEL));
        MegaPassiveNode target = MegaPassiveTree.node("soft_material_specialist");
        List<MegaPassiveNode> route = MegaPassiveTree.allocationPath(node -> node.isUnlocked(fresh), target);
        List<String> ids = route.stream().map(MegaPassiveNode::id).toList();
        require(route.size() > 1 && route.getLast() == target, "route ends at the requested node");
        require(route.size() == path(fresh.startNodeId(), target.id()).size(), "route is a shortest path");
        require(MegaPassiveTree.validBuild(fresh.startNodeId(), ids), "route is a legal allocation order");
        var partial = new MachineProgressionState(fresh.xp(), 0, fresh.level(), ids.subList(0, 2), fresh.startNodeId(), List.of(), false);
        List<String> continued = new ArrayList<>(partial.allocatedNodes());
        MegaPassiveTree.allocationPath(node -> node.isUnlocked(partial), target).forEach(node -> continued.add(node.id()));
        require(continued.size() == route.size() && continued.getLast().equals(target.id())
                && MegaPassiveTree.validBuild(fresh.startNodeId(), continued), "route continues from existing allocations");
        require(MegaPassiveTree.allocationPath(node -> node.isUnlocked(fresh), MegaPassiveTree.node("start_control")).isEmpty(), "foreign starts are not route targets");

        var host = new CheckHost(fresh, state -> true);
        require(host.allocateMasteryPath(route) && host.state.allocatedNodes().equals(ids), "one action allocates the whole route");
        require(MegaPassiveTree.allocationPath(node -> node.isUnlocked(host.state), target).isEmpty(), "allocated target has no route");
        int level = route.size();
        var poor = new CheckHost(new MachineProgressionState(MachineProgressionState.xpForLevel(level), 0, level, List.of(), fresh.startNodeId(), List.of(), false), state -> true);
        require(!poor.allocateMasteryPath(route) && poor.state.allocatedNodes().isEmpty(), "unaffordable route allocates nothing");
        String conflict = ids.get(ids.size() / 2);
        var blocked = new CheckHost(fresh, state -> !state.allocatedNodes().contains(conflict));
        require(!blocked.allocateMasteryPath(route) && blocked.state.allocatedNodes().isEmpty(), "gear conflict on a route allocates nothing");
        var skipped = new CheckHost(fresh, state -> true);
        require(!skipped.allocateMasteryPath(route.subList(1, route.size())) && skipped.state.allocatedNodes().isEmpty(), "disconnected route allocates nothing");
        require(!skipped.allocateMasteryPath(List.of()), "empty route is rejected");
    }

    private static final class CheckHost implements MachineMasteryHost {
        private final java.util.function.Predicate<MachineProgressionState> gear;
        private MachineProgressionState state;

        private CheckHost(MachineProgressionState state, java.util.function.Predicate<MachineProgressionState> gear) {
            this.state = state;
            this.gear = gear;
        }

        @Override public MachineMasteryFamily masteryFamily() { return MachineMasteryFamily.CRUSHER; }
        @Override public MachineProgressionState machineProgression() { return state; }
        @Override public void setMachineProgression(MachineProgressionState state) { this.state = state; }
        @Override public int ascendancyEntryStage() { return AscendancyCatalog.ENTRY_STAGE; }
        @Override public boolean masteryGearAllows(MachineProgressionState state) { return gear.test(state); }
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

    private static void familyAdapters() {
        for (MachineMasteryFamily family : MachineMasteryFamily.values()) {
            var start = MegaPassiveTree.node(family.startNodeId());
            require(start != null && start.kind() == PassiveNodeKind.STARTER, family + " enters the shared graph at a start");
        }
        var furnaceBuild = build(MachineMasteryFamily.FURNACE, "kiln_discipline");
        require(furnaceBuild.forFamily(MachineMasteryFamily.ALLOY_FURNACE).allocatedNodes().equals(furnaceBuild.allocatedNodes()), "families sharing a start keep allocations");
        require(furnaceBuild.forFamily(MachineMasteryFamily.MELTER).allocatedNodes().isEmpty(), "a different start clears allocations");
        require(MachineMasteryFamily.METAL_PRESS.startNodeId().equals(MachineMasteryFamily.RESONANCE_CALIBRATOR.startNodeId()), "Metal Press shares the Control start");
        var empty = MachineProgressionState.EMPTY;
        var press = new MachineStatAccumulator(); MegaPassiveTree.applyStats(press, empty.forFamily(MachineMasteryFamily.METAL_PRESS), MachineMasteryFamily.METAL_PRESS);
        near(press.value(MachineStat.CONTROL), 20, "Control start grants 20 Control");
        var melter = MachineStatAccumulator.componentBase(Map.of(MachineStat.FLUID_TRANSFER, 100.0));
        MegaPassiveTree.applyStats(melter, empty.forFamily(MachineMasteryFamily.MELTER), MachineMasteryFamily.MELTER);
        near(melter.value(MachineStat.RESERVE), 10, "Reserve/Control start grants 10 Reserve");
        near(melter.value(MachineStat.FLUID_TRANSFER), 101, "Melter converts Reserve into Fluid Transfer");
        var calibrator = MachineStatAccumulator.componentBase(Map.of(MachineStat.CALIBRATION_PRECISION, 100.0));
        MegaPassiveTree.applyStats(calibrator, empty.forFamily(MachineMasteryFamily.RESONANCE_CALIBRATOR), MachineMasteryFamily.RESONANCE_CALIBRATOR);
        near(calibrator.value(MachineStat.CALIBRATION_PRECISION), 101, "Resonance Calibrator converts Control into Calibration Precision");
        require(!MachineMasteryFamily.CRUSHER.supports(MachineStat.CALIBRATION_PRECISION) && !MachineMasteryFamily.MELTER.supports(MachineStat.STABILITY),
                "applicability follows the machine's real stat surface");
        var lowHeatMelter = build(MachineMasteryFamily.MELTER, "low_heat_specialist");
        var melterHeat = new MachineStatAccumulator(); MegaPassiveTree.applyStats(melterHeat, lowHeatMelter, MachineMasteryFamily.MELTER);
        effect(melterHeat, ModifierOperation.ADD, 4000);
        near(melterHeat.value(MachineStat.MAX_TEMPERATURE), 4000, "a temperature ceiling cannot strand a Melter below its recipes");
        require(MachineMasteryFamily.MELTER.supports(MachineStat.MAX_TEMPERATURE) && MachineMasteryFamily.FURNACE.supportsAbsolute(MachineStat.MAX_TEMPERATURE),
                "the Melter keeps ordinary heat bonuses while other heat machines keep absolute constraints");
        require(!MachineMasteryFamily.FURNACE.supports(MachineStat.OUTPUT_AMOUNT) && !MachineMasteryFamily.FURNACE.supports(MachineStat.BATCH_SIZE)
                && MachineMasteryFamily.FURNACE.supports(MachineStat.SUPER_OUTPUT_CHANCE), "only the Crusher reads Output Amount and Parallel Jobs");
    }

    private static void taggedPayoffs() {
        for (MachineMasteryFamily family : MachineMasteryFamily.values()) {
            double lowHeat = speedGain(family, "low_heat_specialist");
            near(lowHeat, family.has(MachineTag.HEATED) ? 2 : 1, family + " gains Low Heat Specialist speed only when it is a heated machine");
            near(speedGain(family, "soft_material_specialist"), family == MachineMasteryFamily.CRUSHER ? 3 : 1, family + " gains Soft Material Specialist speed only as a Crusher");
            near(speedGain(family, "single_pass"), family.has(MachineTag.BONUS_OUTPUT) ? 1.3 : 1, family + " gains Single Pass speed only with bonus output to give up");
        }
        require(!MachineMasteryFamily.FORESTRY.has(MachineTag.BONUS_OUTPUT) && !MachineMasteryFamily.MELTER.has(MachineTag.BONUS_OUTPUT)
                && MachineMasteryFamily.METAL_PRESS.has(MachineTag.BONUS_OUTPUT), "bonus output follows Output Amount and Super Output support");

        var furnace = build(MachineMasteryFamily.FURNACE, "low_heat_specialist");
        var weakHeat = new MachineStatAccumulator(); MegaPassiveTree.applyStats(weakHeat, furnace, MachineMasteryFamily.FURNACE);
        effect(weakHeat, ModifierOperation.ADD, 300);
        near(weakHeat.value(MachineStat.MAX_TEMPERATURE), 300, "a heat ceiling never raises a weak heat source to its limit");

        var crusher = build(MachineMasteryFamily.CRUSHER, "single_pass");
        var output = MachineStatAccumulator.componentBase(Map.of(MachineStat.OUTPUT_AMOUNT, 1.0, MachineStat.SUPER_OUTPUT_CHANCE, 0.0, MachineStat.CRUSHER_SALVAGE_CHANCE, 0.0));
        MegaPassiveTree.applyStats(output, crusher, MachineMasteryFamily.CRUSHER);
        output.apply(new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.OUTPUT_AMOUNT, ModifierOperation.INCREASED_PERCENT, 50));
        output.apply(new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.SUPER_OUTPUT_CHANCE, ModifierOperation.ADD, 25));
        output.apply(new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.CRUSHER_SALVAGE_CHANCE, ModifierOperation.ADD, 5));
        near(output.value(MachineStat.OUTPUT_AMOUNT), 1, "Single Pass stops Gear from raising Output Amount above the base");
        near(output.value(MachineStat.SUPER_OUTPUT_CHANCE), 0, "Single Pass removes Super Output chance");
        near(output.value(MachineStat.CRUSHER_SALVAGE_CHANCE), 0, "Single Pass removes salvage chance");
        var penalized = MachineStatAccumulator.componentBase(Map.of(MachineStat.OUTPUT_AMOUNT, 0.75));
        MegaPassiveTree.applyStats(penalized, crusher, MachineMasteryFamily.CRUSHER);
        near(penalized.value(MachineStat.OUTPUT_AMOUNT), 0.75, "Single Pass never lifts a penalized Output Amount");
        var press = build(MachineMasteryFamily.METAL_PRESS, "single_pass");
        var pressOutput = MachineStatAccumulator.componentBase(Map.of(MachineStat.SUPER_OUTPUT_CHANCE, 10.0));
        MegaPassiveTree.applyStats(pressOutput, press, MachineMasteryFamily.METAL_PRESS);
        near(pressOutput.value(MachineStat.SUPER_OUTPUT_CHANCE), 0, "Single Pass removes a Metal Press's Super Output");
    }

    /** Speed multiplier the destination keystone adds on top of the route that reaches it. */
    private static double speedGain(MachineMasteryFamily family, String keystone) {
        var with = build(family, keystone);
        var withStats = MachineStatAccumulator.componentBase(Map.of(MachineStat.PROCESSING_SPEED, 1.0));
        var withoutStats = MachineStatAccumulator.componentBase(Map.of(MachineStat.PROCESSING_SPEED, 1.0));
        MegaPassiveTree.applyStats(withStats, with, family);
        MegaPassiveTree.applyStats(withoutStats, with.withoutNode(keystone), family);
        return withStats.value(MachineStat.PROCESSING_SPEED) / withoutStats.value(MachineStat.PROCESSING_SPEED);
    }

    private static final Map<MachineStat, Double> BASE_100 = Arrays.stream(MachineStat.values()).collect(Collectors.toMap(stat -> stat, stat -> 100.0));
    private static final Set<MachineStat> LOWER_IS_BETTER = Set.of(
            MachineStat.ENERGY_USAGE, MachineStat.WARMUP_TIME, MachineStat.COOLING_RATE, MachineStat.IDLE_LOSS);
    private static final Set<String> COST_BEHAVIORS = Set.of("BLOCK_BATTERY", "MATCHING_HEAD", "NO_BONUS_OUTPUT", "NO_INHERENT_ATTRIBUTES");

    /**
     * A keystone's payoff must never reach a machine that escapes its cost. Limits and restricting behaviors are the
     * defining cost: a machine that gains the payoff must also receive one of them, not only a generic penalty.
     */
    private static void keystonePayoffsNeedTheirCosts() {
        for (MegaPassiveNode keystone : MegaPassiveTree.TREE.nodes().stream().filter(n -> n.kind() == PassiveNodeKind.KEYSTONE).toList()) {
            List<String> violations = payoffWithoutCost(keystone);
            require(violations.isEmpty(), "keystone payoffs follow their costs: " + violations);
        }
        // Before tags, Low Heat Specialist doubled every non-heat machine's speed for free.
        MegaPassiveNode lowHeat = MegaPassiveTree.node("low_heat_specialist");
        var untagged = new MegaPassiveNode(lowHeat.index(), lowHeat.id(), lowHeat.name(), lowHeat.kind(), lowHeat.x(), lowHeat.y(), lowHeat.links(),
                lowHeat.tagged().stream().map(MegaPassiveNode.TaggedEffect::effect).toList(), List.of(), lowHeat.behaviors(),
                Map.of(MachineStat.MAX_TEMPERATURE, 800.0), Map.of(), lowHeat.scaling(), lowHeat.passive(), 0);
        List<String> caught = payoffWithoutCost(untagged);
        require(caught.stream().anyMatch(v -> v.contains("CRUSHER")) && caught.stream().anyMatch(v -> v.contains("MELTER"))
                && caught.stream().noneMatch(v -> v.contains("FURNACE")), "audit catches an untagged heat payoff: " + caught);
    }

    private static List<String> payoffWithoutCost(MegaPassiveNode keystone) {
        List<String> violations = new ArrayList<>();
        for (MachineMasteryFamily family : MachineMasteryFamily.values()) {
            boolean payoff = false, cost = false, limit = false, hasCost = false, hasLimit = false;
            List<Map.Entry<MachineModifierEffect, Boolean>> effects = new ArrayList<>();
            keystone.effects().forEach(effect -> effects.add(Map.entry(effect, family.supports(effect.stat()))));
            keystone.tagged().forEach(tagged -> effects.add(Map.entry(tagged.effect(), tagged.appliesTo(family))));
            for (var entry : effects) {
                boolean raises = switch (entry.getKey().operation()) { case ADD, INCREASED_PERCENT, MORE -> true; case DECREASED_PERCENT, LESS -> false; };
                if (raises == LOWER_IS_BETTER.contains(entry.getKey().stat())) { hasCost = true; cost |= entry.getValue(); } else { payoff |= entry.getValue(); }
            }
            for (MachineStat stat : concat(keystone.fixed().keySet(), keystone.ceilings().keySet())) {
                hasCost = hasLimit = true;
                if (family.supportsAbsolute(stat)) { cost = limit = true; }
            }
            if (keystone.recipeHardnessCeiling() > 0) {
                hasCost = hasLimit = true;
                if (family.supports(MachineStat.PROCESSING_LEVEL)) { cost = limit = true; }
            }
            for (String behavior : keystone.behaviors()) {
                boolean applies = family.supportsBehavior(behavior);
                if (COST_BEHAVIORS.contains(behavior)) { hasCost = hasLimit = true; cost |= applies; limit |= applies; } else { payoff |= applies; }
            }
            payoff |= keystone.scaling().stream().anyMatch(scaling -> family.supports(scaling.stat()));
            if (payoff && hasCost && !cost) { violations.add(keystone.id() + " pays " + family + " without any cost"); }
            if (payoff && hasLimit && !limit) { violations.add(keystone.id() + " pays " + family + " without its limit"); }
        }
        return violations;
    }

    private static List<MachineStat> concat(Set<MachineStat> first, Set<MachineStat> second) {
        List<MachineStat> all = new ArrayList<>(first);
        all.addAll(second);
        return all;
    }

    private static void iconTextures() {
        for (MegaPassiveNode node : MegaPassiveTree.TREE.nodes().stream().filter(n -> n.kind() == PassiveNodeKind.KEYSTONE || n.kind() == PassiveNodeKind.STARTER).toList()) {
            require(node.masteryIconKey().equals(node.id()), node.kind() + " draws its own icon: " + node.id());
        }
        for (String key : MegaPassiveTree.TREE.nodes().stream().map(MegaPassiveNode::masteryIconKey).collect(Collectors.toSet())) {
            require(MasteryChecks.class.getResource("/assets/rngtech/textures/gui/mastery/" + key + ".png") != null, "icon texture exists: " + key);
        }
    }

    /** Shift-hover breakdowns of a passive build recombine to the pipeline's values and name the nodes behind them. */
    private static void statBreakdowns() {
        for (MachineMasteryFamily family : MachineMasteryFamily.values()) {
            var state = build(family, "single_pass");
            MachineStatAccumulator stats = MachineStatAccumulator.recording(() -> {
                MachineStatAccumulator recorded = MachineStatAccumulator.componentBase(BASE_100);
                MegaPassiveTree.applyStats(recorded, state, family);
                return recorded;
            });
            for (StatBreakdown breakdown : stats.breakdowns()) {
                near(breakdown.recompute(), stats.value(breakdown.stat()), family + " breakdown recombines " + breakdown.stat());
                require(breakdown.terms().stream().noneMatch(term -> term.source().getString().equals("rngtech.stat.breakdown.source.machine")),
                        family + " passive contributions all name their source for " + breakdown.stat());
            }
            String singlePass = MegaPassiveTree.node("single_pass").translationKey();
            require(stats.breakdowns().stream().flatMap(breakdown -> breakdown.terms().stream())
                    .anyMatch(term -> term.source().getString().equals(singlePass)) == family.has(MachineTag.BONUS_OUTPUT),
                    family + " breakdown names the Single Pass node where it pays out");
            require(Stream.of(MachineStat.CONTROL, MachineStat.DRIVE, MachineStat.RESERVE)
                    .flatMap(attribute -> stats.breakdown(attribute).orElseThrow().terms().stream())
                    .anyMatch(term -> term.source().getString().equals("rngtech.stat.breakdown.source.inherent")),
                    family + " inherent attribute points are labelled Inherent");
        }
    }

    private static void bonusSummary() {
        for (MachineMasteryFamily family : MachineMasteryFamily.values()) {
            var state = build(family, "single_pass");
            var totals = new MachineStatAccumulator(); MegaPassiveTree.applyStats(totals, state, family);
            var lines = MasteryBonusSummary.of(state, MasteryApplicability.of(family), totals.value(MachineStat.CONTROL), totals.value(MachineStat.DRIVE), totals.value(MachineStat.RESERVE));
            var direct = MachineStatAccumulator.componentBase(BASE_100);
            var summarized = MachineStatAccumulator.componentBase(BASE_100);
            for (String id : state.allocatedNodes()) {
                MegaPassiveNode node = MegaPassiveTree.node(id);
                node.effects().stream().filter(e -> family.supports(e.stat())).forEach(e -> direct.apply(new MachineModifier(ModifierSlot.IMPLICIT, e.stat(), e.operation(), e.value())));
                node.tagged().stream().filter(t -> t.appliesTo(family)).forEach(t -> direct.apply(new MachineModifier(ModifierSlot.IMPLICIT, t.effect().stat(), t.effect().operation(), t.effect().value())));
            }
            Set<MachineStat> touched = new HashSet<>();
            for (var line : lines) {
                if (line.kind() == MasteryBonusSummary.Kind.EFFECT && line.active()) {
                    summarized.apply(new MachineModifier(ModifierSlot.IMPLICIT, line.effect().stat(), line.effect().operation(), line.effect().value()));
                    touched.add(line.effect().stat());
                }
            }
            for (MachineStat stat : touched) {
                near(summarized.value(stat), direct.value(stat), family + " summary combines " + stat + " like the stat pipeline");
            }
            boolean paysOutput = family.has(MachineTag.BONUS_OUTPUT);
            require(lines.stream().anyMatch(line -> line.kind() == MasteryBonusSummary.Kind.BEHAVIOR && "NO_BONUS_OUTPUT".equals(line.behavior()) && line.active() == paysOutput),
                    family + " summary marks Single Pass's cost active only where it applies");
            require(lines.stream().anyMatch(line -> line.kind() == MasteryBonusSummary.Kind.EFFECT && line.effect().stat() == MachineStat.PROCESSING_SPEED
                    && line.effect().operation() == ModifierOperation.MORE && line.active() == paysOutput && line.sources().contains(MegaPassiveTree.node("single_pass"))),
                    family + " summary lists the tagged speed payoff with its source");
            require(lines.stream().anyMatch(line -> line.kind() == MasteryBonusSummary.Kind.CONVERSION),
                    family + " summary shows inherent attribute conversions");
        }
        var transfigured = build(MachineMasteryFamily.CRUSHER, "attribute_transfiguration");
        require(MasteryBonusSummary.of(transfigured, MasteryApplicability.of(MachineMasteryFamily.CRUSHER), 10, 10, 10).stream().noneMatch(line -> line.kind() == MasteryBonusSummary.Kind.CONVERSION),
                "no inherent bonuses removes conversions from the summary");
    }

    private static MachineProgressionState build(MachineMasteryFamily family, String destination) {
        return new MachineProgressionState(MachineProgressionState.xpForLevel(MachineProgressionState.MAX_LEVEL), 0, MachineProgressionState.MAX_LEVEL,
                path(family.startNodeId(), destination), family.startNodeId(), List.of(), false);
    }
    private static List<String> breadthFirst(String start, int count) {
        List<String> order = new ArrayList<>();
        ArrayDeque<String> queue = new ArrayDeque<>(List.of(start));
        java.util.Set<String> seen = new java.util.HashSet<>(List.of(start));
        while (!queue.isEmpty() && order.size() < count) {
            for (String next : MegaPassiveTree.node(queue.remove()).links()) {
                if (order.size() < count && seen.add(next) && MegaPassiveTree.node(next).kind() != PassiveNodeKind.STARTER) { order.add(next); queue.add(next); }
            }
        }
        return order;
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
    private static void effect(MachineStatAccumulator stats, MachineStat stat, ModifierOperation operation, double value) {
        stats.apply(new MachineModifier(ModifierSlot.IMPLICIT, stat, operation, value));
    }
    private static void near(double actual, double expected, String label) { require(Math.abs(actual - expected) < 0.00001, label + ": " + actual); }
    private static void require(boolean condition, String label) { checks++; if (!condition) { throw new AssertionError(label); } }
}
