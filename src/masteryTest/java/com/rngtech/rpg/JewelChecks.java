package com.rngtech.rpg;

import com.rngtech.rpg.jewel.JewelAffixes;
import com.rngtech.rpg.jewel.JewelColour;
import com.rngtech.rpg.jewel.JewelEffects;
import com.rngtech.rpg.jewel.MachineJewels;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.MegaPassiveNode;
import com.rngtech.rpg.progression.MegaPassiveTree;
import com.rngtech.rpg.progression.PassiveNodeKind;
import com.rngtech.rpg.unique.UniqueStatReaders;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The jewel pool: one tier per line, every line read somewhere, the 2/2 slot limit, and the colour weighting. */
public final class JewelChecks {
    private static final int ROLLS = 4_000;
    private static int checks;

    private JewelChecks() {
    }

    public static int run() {
        checks = 0;
        onePoolOneTierEachReadSomewhere();
        colourWeightsTheSharedPool();
        jewelsHoldTwoOfEachSide();
        refinementPotentialFollowsTheCut();
        treeSocketsOpenSlots();
        socketedJewelsApplyWhereRead();
        bodySocketsComeFromBedazzlingAndGold();
        return checks;
    }

    private static void bodySocketsComeFromBedazzlingAndGold() {
        List<ModifierEligibilityProfile> bedazzled = ModifierEligibilityProfiles.allProfiles().stream()
                .filter(profile -> profile.definitions().stream().anyMatch(definition -> definition.id().equals("bedazzling")))
                .toList();
        Set<String> expected = Set.of("crusher", "furnace", "electric_furnace", "solid_fuel_burner", "bio_generator", "solar_array_controller",
                "potential_reactor", "corrosion_cell", "vacuum_collapse_generator", "component_recycler", "metal_press", "melter", "battery_assembler",
                "coal_gasifier", "syngas_combustor", "steam_methane_reformer", "cavitation_generator", "ammonia_synthesizer", "ammonia_fuel_cell",
                "compressor_tank", "fluid_tank", "alloy_furnace", "resonance_calibrator", "battery_chassis");
        Set<String> actual = new java.util.HashSet<>();
        bedazzled.forEach(profile -> actual.add(profile.id()));
        require(actual.equals(expected), "Bedazzling rolls on every machine body with a screen and nowhere else: " + actual);
        ModifierDefinition bedazzling = bedazzled.getFirst().definitions().stream().filter(definition -> definition.id().equals("bedazzling")).findFirst().orElseThrow();
        require(bedazzling.slot() == ModifierSlot.SUFFIX && bedazzling.modGroup().equals("jewel_sockets") && bedazzling.rollWeight() == 40,
                "Bedazzling is a weight-40 suffix in its own group");
        int[] sockets = {1, 1, 1, 2, 2, 2, 3};
        for (int tier = 1; tier <= 7; tier++) {
            require(bedazzling.rangeForTier(tier).min() == sockets[tier - 1] && bedazzling.rangeForTier(tier).max() == sockets[tier - 1],
                    "Bedazzling Tier " + tier + " grants " + sockets[tier - 1] + " sockets");
        }
        require(ModifierEligibilityProfiles.maxBedazzlingSockets() == 3, "three body sockets need Tier 7");

        MachineTraits gold = new MachineTraits(Rarity.RARE, 0, List.of(
                new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.JEWEL_SOCKETS, ModifierOperation.ADD, 1.0),
                bedazzling.roll(7, RandomSource.create(1L))));
        require(com.rngtech.content.jewel.JewelSockets.bodySockets(gold) == 4, "a Gold body's setting and Tier 7 Bedazzling make four sockets");
        require(com.rngtech.content.jewel.JewelSockets.bodySockets(MachineTraits.EMPTY) == 0, "a plain body has no sockets");
        MachineTraits many = new MachineTraits(Rarity.RARE, 0, List.of(
                new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.JEWEL_SOCKETS, ModifierOperation.ADD, 9.0)));
        require(com.rngtech.content.jewel.JewelSockets.bodySockets(many) == MachineJewels.BODY_SLOTS, "body sockets stop at the body row");
    }

    private static void treeSocketsOpenSlots() {
        List<MegaPassiveNode> sockets = MegaPassiveTree.socketNodes();
        require(sockets.size() == MachineJewels.TREE_SLOTS, "nine Jewel Sockets, found " + sockets.size());
        for (MegaPassiveNode socket : sockets) {
            require(socket.effects().isEmpty() && socket.tagged().isEmpty() && socket.behaviors().isEmpty(), socket.id() + " grants nothing itself");
            require(socket.links().size() == 1, socket.id() + " is a leaf");
            require(socket.masteryIconKey().equals("jewel_socket"), socket.id() + " draws the socket icon");
        }
        MachineProgressionState empty = MachineProgressionState.EMPTY.forFamily(MachineMasteryFamily.CRUSHER);
        require(MegaPassiveTree.allocatedSocketCount(empty) == 0 && !JewelEffects.treeSlotOpen(empty, 0), "no socket, no open slot");
        MachineProgressionState one = withNodes(empty, pathTo(empty, sockets.getFirst()));
        require(MegaPassiveTree.allocatedSocketCount(one) == 1 && JewelEffects.treeSlotOpen(one, 0) && !JewelEffects.treeSlotOpen(one, 1),
                "one allocated socket opens the first slot only");
        require(MegaPassiveTree.allocatedSocketCount(one.withoutNode(sockets.getFirst().id())) == 0, "refunding the socket closes the slot again");
    }

    private static void socketedJewelsApplyWhereRead() {
        List<JewelEffects.Jewel> jewels = List.of(new JewelEffects.Jewel(Component.literal("Test Jewel"), new MachineTraits(Rarity.RARE, 0, List.of(
                modifier("jewel_pace", MachineStat.PROCESSING_SPEED, ModifierOperation.INCREASED_PERCENT, 10),
                modifier("jewel_yielding", MachineStat.OUTPUT_AMOUNT, ModifierOperation.INCREASED_PERCENT, 8),
                modifier("jewel_drive", MachineStat.DRIVE, ModifierOperation.ADD, 10),
                modifier(JewelAffixes.MOMENTUM_ID, MachineStat.PROCESSING_SPEED, ModifierOperation.INCREASED_PERCENT, 12)))));

        MachineStatAccumulator furnace = MachineStatAccumulator.componentBase(Map.of(MachineStat.PROCESSING_SPEED, 1.0, MachineStat.OUTPUT_AMOUNT, 1.0, MachineStat.DRIVE, 0.0));
        JewelEffects.applyLines(furnace, jewels, MachineMasteryFamily.FURNACE);
        near(furnace.increasedPercent(MachineStat.PROCESSING_SPEED), 10, "a Furnace reads the jewel's speed line");
        near(furnace.increasedPercent(MachineStat.OUTPUT_AMOUNT), 0, "a Furnace ignores a Bonus Output line it never reads");
        near(furnace.value(MachineStat.DRIVE), 10, "the jewel's flat Drive lands before the conversions");
        JewelEffects.applyThresholds(furnace, jewels, MachineMasteryFamily.FURNACE);
        near(furnace.increasedPercent(MachineStat.PROCESSING_SPEED), 10, "Momentum waits for 100 Drive");

        MachineStatAccumulator driven = MachineStatAccumulator.componentBase(Map.of(MachineStat.PROCESSING_SPEED, 1.0, MachineStat.DRIVE, 95.0));
        JewelEffects.applyLines(driven, jewels, MachineMasteryFamily.FURNACE);
        JewelEffects.applyThresholds(driven, jewels, MachineMasteryFamily.FURNACE);
        near(driven.increasedPercent(MachineStat.PROCESSING_SPEED), 22, "Momentum pays once the jewel's own Drive takes the total to 100");

        MachineStatAccumulator crusher = MachineStatAccumulator.componentBase(Map.of(MachineStat.OUTPUT_AMOUNT, 1.0));
        JewelEffects.applyLines(crusher, jewels, MachineMasteryFamily.CRUSHER);
        near(crusher.increasedPercent(MachineStat.OUTPUT_AMOUNT), 8, "a Crusher reads the Bonus Output line");

        MachineStatAccumulator anyMachine = MachineStatAccumulator.componentBase(Map.of(MachineStat.OUTPUT_AMOUNT, 1.0));
        JewelEffects.applyLines(anyMachine, jewels, null);
        near(anyMachine.increasedPercent(MachineStat.OUTPUT_AMOUNT), 8, "a machine without Mastery reads every line");
    }

    private static MachineModifier modifier(String id, MachineStat stat, ModifierOperation operation, double value) {
        return MachineModifier.roll(id, id, ModifierSlot.SUFFIX, 1, List.of(new MachineModifierEffect(stat, operation, new ModifierValueRange(value, value, true), value)));
    }

    /** The shortest allocation path from the family's start to {@code target}, in allocation order. */
    private static List<String> pathTo(MachineProgressionState state, MegaPassiveNode target) {
        Map<String, String> parent = new HashMap<>();
        ArrayDeque<String> queue = new ArrayDeque<>(List.of(state.startNodeId()));
        parent.put(state.startNodeId(), null);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current.equals(target.id())) {
                break;
            }
            for (String next : MegaPassiveTree.node(current).links()) {
                MegaPassiveNode node = MegaPassiveTree.node(next);
                if (!parent.containsKey(next) && node.kind() != PassiveNodeKind.STARTER) {
                    parent.put(next, current);
                    queue.add(next);
                }
            }
        }
        List<String> path = new ArrayList<>();
        for (String id = target.id(); id != null && !id.equals(state.startNodeId()); id = parent.get(id)) {
            path.addFirst(id);
        }
        return path;
    }

    private static MachineProgressionState withNodes(MachineProgressionState state, List<String> ids) {
        MachineProgressionState next = new MachineProgressionState(0, 0, MachineProgressionState.MAX_LEVEL, List.of(), state.startNodeId(), List.of(), false,
                state.ascendancy(), state.ascendancyNodes(), state.sealTiers(), state.freeReset());
        for (String id : ids) {
            next = next.withUnlockedNode(MegaPassiveTree.node(id).index());
        }
        require(next.allocatedNodes().size() == ids.size(), "the socket path allocates in order: " + ids);
        return next;
    }

    private static void near(double actual, double expected, String message) {
        require(Math.abs(actual - expected) < 0.0001, message + ": expected " + expected + " but was " + actual);
    }

    private static void onePoolOneTierEachReadSomewhere() {
        List<ModifierDefinition> pool = ModifierEligibilityProfiles.forJewel(JewelColour.CONTROL).definitions();
        for (JewelColour colour : JewelColour.values()) {
            ModifierEligibilityProfile profile = ModifierEligibilityProfiles.forJewel(colour);
            require(profile.id().equals(colour.profileId()), colour + " profile id matches its colour");
            require(profile.definitions().equals(pool), colour + " shares the one jewel pool");
            require(profile.rollableBehaviors().isEmpty(), "jewels roll no behaviors");
        }
        require(pool.size() == 20, "the jewel pool has twenty families, found " + pool.size());
        Set<String> thresholds = Set.of(JewelAffixes.MOMENTUM_ID, JewelAffixes.POISE_ID, JewelAffixes.DEPTH_ID);
        for (ModifierDefinition definition : pool) {
            require(definition.id().startsWith("jewel_") && definition.modGroup().equals(definition.id()),
                    definition.id() + " uses its own jewel group");
            for (ModifierEffectDefinition effect : definition.effects()) {
                require(effect.rangeForTier(1).equals(effect.rangeForTier(7)), definition.id() + " has one tier");
                require(readSomewhere(effect.stat()), definition.id() + " is read by some machine: " + effect.stat());
            }
            require(definition.canRoll(), definition.id() + " rolls");
            require(ModifierEligibilityProfiles.hasCustomDescription(definition) == thresholds.contains(definition.id()),
                    definition.id() + " describes its condition only when it has one");
        }
        require(JewelAffixes.thresholdColour(JewelAffixes.MOMENTUM_ID) == JewelColour.DRIVE
                        && JewelAffixes.thresholdAttribute(JewelAffixes.POISE_ID) == MachineStat.CONTROL
                        && JewelAffixes.thresholdColour(JewelAffixes.DEPTH_ID) == JewelColour.RESERVE
                        && JewelAffixes.thresholdColour("jewel_pace") == null,
                "threshold lines name their colour and attribute");
    }

    private static boolean readSomewhere(MachineStat stat) {
        // Attributes and Mastery XP gain are machine-wide: every Mastery machine reads them outside the family table.
        if (MachineStat.isMachineWide(stat)) {
            return true;
        }
        for (MachineMasteryFamily family : MachineMasteryFamily.values()) {
            if (family.supports(stat)) {
                return true;
            }
        }
        for (UniqueStatReaders.Reader reader : UniqueStatReaders.Reader.values()) {
            if (UniqueStatReaders.reads(reader, stat)) {
                return true;
            }
        }
        return false;
    }

    private static void colourWeightsTheSharedPool() {
        ModifierEligibilityProfile drive = ModifierEligibilityProfiles.forJewel(JewelColour.DRIVE);
        Map<String, ModifierDefinition> byId = new HashMap<>();
        drive.definitions().forEach(definition -> byId.put(definition.id(), definition));
        require(drive.rollWeight(byId.get("jewel_pace")) == JewelColour.HOME_WEIGHT, "a home line weighs 150 on its colour");
        require(drive.rollWeight(byId.get("jewel_frugal")) == JewelColour.AWAY_WEIGHT, "an away line weighs 60");
        require(drive.rollWeight(byId.get("jewel_headroom")) == 25, "Headroom keeps its chase weight on every colour");
        require(drive.rollWeight(byId.get(JewelAffixes.MOMENTUM_ID)) == 20, "a colour's own threshold line keeps its weight");
        require(drive.rollWeight(byId.get(JewelAffixes.POISE_ID)) == 0 && drive.rollWeight(byId.get(JewelAffixes.DEPTH_ID)) == 0,
                "another colour's threshold line never rolls");
        require(ModifierEligibilityProfiles.forMachine(MachineType.CRUSHER).rollWeight(byId.get("jewel_pace")) == 100,
                "other profiles keep the family weight");

        for (JewelColour colour : JewelColour.values()) {
            Map<String, Integer> seen = new HashMap<>();
            RandomSource random = RandomSource.create(7L + colour.ordinal());
            for (int roll = 0; roll < ROLLS; roll++) {
                for (MachineModifier modifier : MachineTraitRoller.roll(ModifierEligibilityProfiles.forJewel(colour), JewelColour.COMPONENT_STAGE, random, false).modifiers()) {
                    seen.merge(modifier.affixId(), 1, Integer::sum);
                }
            }
            String ownThreshold = switch (colour) {
                case CONTROL -> JewelAffixes.POISE_ID;
                case DRIVE -> JewelAffixes.MOMENTUM_ID;
                case RESERVE -> JewelAffixes.DEPTH_ID;
            };
            for (String threshold : List.of(JewelAffixes.MOMENTUM_ID, JewelAffixes.POISE_ID, JewelAffixes.DEPTH_ID)) {
                require(threshold.equals(ownThreshold) == seen.containsKey(threshold), colour + " rolls only its own threshold line: " + seen);
            }
            String home = "jewel_" + colour.getSerializedName();
            String away = "jewel_" + (colour == JewelColour.CONTROL ? "drive" : "control");
            require(seen.getOrDefault(home, 0) > seen.getOrDefault(away, 0), colour + " rolls its own attribute more than another's: " + seen);
        }
    }

    private static void jewelsHoldTwoOfEachSide() {
        RandomSource random = RandomSource.create(99L);
        Map<Rarity, Integer> rarities = new EnumMap<>(Rarity.class);
        int three = 0;
        int four = 0;
        for (int roll = 0; roll < ROLLS; roll++) {
            MachineTraits traits = MachineTraitRoller.roll(ModifierEligibilityProfiles.forJewel(JewelColour.RESERVE), JewelColour.COMPONENT_STAGE, random, false);
            rarities.merge(traits.rarity(), 1, Integer::sum);
            long prefixes = traits.modifiers().stream().filter(modifier -> modifier.slot() == ModifierSlot.PREFIX).count();
            long suffixes = traits.modifiers().stream().filter(modifier -> modifier.slot() == ModifierSlot.SUFFIX).count();
            require(prefixes <= 2 && suffixes <= 2, "a jewel holds at most two prefixes and two suffixes");
            if (traits.rarity() == Rarity.RARE) {
                int count = traits.modifiers().size();
                require(count == 3 || count == 4, "a Rare jewel rolls three or four lines, rolled " + count);
                if (count == 3) { three++; } else { four++; }
            } else if (traits.rarity() == Rarity.MAGIC) {
                require(traits.modifiers().size() <= 2, "a Magic jewel rolls one or two lines");
            }
        }
        require(rarities.getOrDefault(Rarity.NORMAL, 0) == 0 && rarities.getOrDefault(Rarity.RARE, 0) > ROLLS / 2,
                "a Stage 3 jewel is Magic or Rare, mostly Rare: " + rarities);
        require(three > ROLLS / 8 && four > ROLLS / 8, "three and four lines both come up often: " + three + " / " + four);

        MachineTraits part = MachineTraitRoller.roll(ModifierEligibilityProfiles.forMachine(MachineType.CRUSHER), 7, RandomSource.create(1L), false);
        require(part.modifiers().size() <= 6, "other items keep the three-per-side limit");
    }

    private static void refinementPotentialFollowsTheCut() {
        require(JewelAffixes.refinementPotential(0) == 2 && JewelAffixes.refinementPotential(49) == 2, "a rough cut gives 2 RP");
        require(JewelAffixes.refinementPotential(50) == 3 && JewelAffixes.refinementPotential(79) == 3, "a fair cut gives 3 RP");
        require(JewelAffixes.refinementPotential(80) == 4 && JewelAffixes.refinementPotential(100) == 4, "a fine cut gives 4 RP");
        RandomSource random = RandomSource.create(3L);
        for (int roll = 0; roll < 200; roll++) {
            int potential = JewelAffixes.uncutRefinementPotential(random);
            require(potential >= 2 && potential <= 4, "an uncut jewel stays inside the cut range");
        }
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
