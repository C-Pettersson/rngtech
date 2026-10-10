package com.rngtech.rpg;

import com.rngtech.rpg.progression.AscendancyCatalog;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.MegaPassiveTree;
import com.rngtech.rpg.progression.PassiveNodeKind;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * Encode/decode checks for the codecs that persist machine data in item components and block entities, run from
 * {@code MasteryChecks}. They use plain DFU ops, so no Minecraft bootstrap is needed.
 */
public final class CodecRoundTripChecks {
    private static final String RETIRED = "retired_by_a_later_version";
    private static int checks;

    private CodecRoundTripChecks() {
    }

    public static int run() {
        checks = 0;
        traitsRoundTrip();
        traitsStayStableOnResave();
        legacyStatNamesStillDecode();
        unknownStatNamesInTraits();
        unknownSlotOperationBehaviorAndRarity();
        progressionRoundTrip();
        unknownMasteryNodes();
        return checks;
    }

    private static void traitsRoundTrip() {
        MachineTraits traits = sampleTraits();
        roundTrip(MachineTraits.CODEC, traits, "traits");
        roundTrip(MachineTraits.CODEC, MachineTraits.EMPTY, "empty traits");

        List<MachineModifier> everyStat = new ArrayList<>();
        for (MachineStat stat : MachineStat.values()) {
            everyStat.add(new MachineModifier(ModifierSlot.PREFIX, stat, ModifierOperation.ADD, 1, new ModifierValueRange(1, 5, true), 3));
        }
        roundTrip(MachineTraits.CODEC, new MachineTraits(Rarity.RARE, 1, everyStat), "a modifier for every stat");

        for (ModifierSlot slot : ModifierSlot.values()) {
            for (ModifierOperation operation : ModifierOperation.values()) {
                roundTrip(MachineTraits.CODEC, new MachineTraits(Rarity.MAGIC, 0,
                        List.of(new MachineModifier(slot, MachineStat.PROCESSING_SPEED, operation, 12.5))), "slot and operation " + slot + "/" + operation);
            }
        }
        for (Rarity rarity : Rarity.values()) {
            roundTrip(MachineTraits.CODEC, new MachineTraits(rarity, 0, List.of()), "rarity " + rarity);
        }
        roundTrip(MachineTraits.CODEC, new MachineTraits(Rarity.NORMAL, 0, List.of(), List.of(MachineBehavior.values())), "every behavior");

        RegistryFriendlyByteBuf wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            MachineTraits.STREAM_CODEC.encode(wire, traits);
            require(traits.equals(MachineTraits.STREAM_CODEC.decode(wire)), "traits survive the network codec");
        } finally {
            wire.release();
        }
    }

    /** Loading and saving again must not change what is stored, or every chunk save would drift. */
    private static void traitsStayStableOnResave() {
        MachineTraits traits = sampleTraits();
        JsonElement first = MachineTraits.CODEC.encodeStart(JsonOps.INSTANCE, traits).getOrThrow();
        MachineTraits loaded = MachineTraits.CODEC.parse(JsonOps.INSTANCE, first).getOrThrow();
        require(first.equals(MachineTraits.CODEC.encodeStart(JsonOps.INSTANCE, loaded).getOrThrow()), "re-encoding loaded traits is stable");
    }

    private static void legacyStatNamesStillDecode() {
        JsonObject modifier = modifierJson("prefix", "parallel_jobs", "add", 2);
        MachineTraits loaded = MachineTraits.CODEC.parse(JsonOps.INSTANCE, traitsJson("magic", modifier)).getOrThrow();
        require(loaded.modifiers().getFirst().stat() == MachineStat.BATCH_SIZE, "a stat stored under its old name loads as the renamed stat");
        MachineTraits noBehaviors = MachineTraits.CODEC.parse(JsonOps.INSTANCE, traitsJson("magic", modifier)).getOrThrow();
        require(noBehaviors.behaviors().isEmpty(), "traits stored before behaviors existed load with none");
    }

    /**
     * Known gap, issue #81: a stat name that no longer exists fails the whole traits decode, so an item loses every roll,
     * not only the one that names the missing stat.
     */
    private static void unknownStatNamesInTraits() {
        JsonObject kept = modifierJson("prefix", "processing_speed", "increased_percent", 25);
        JsonObject retired = modifierJson("suffix", RETIRED, "add", 4);
        JsonElement stored = traitsJson("rare", kept, retired);
        DataResult<MachineTraits> parsed = MachineTraits.CODEC.parse(JsonOps.INSTANCE, stored);
        require(parsed.result().isEmpty(), "known gap #81: an unknown stat name fails the whole traits decode");
    }

    /** Known gap, issue #81: the same failure for the other name-based enum codecs. */
    private static void unknownSlotOperationBehaviorAndRarity() {
        JsonObject kept = modifierJson("prefix", "processing_speed", "increased_percent", 25);
        JsonObject badSlot = modifierJson(RETIRED, "processing_speed", "add", 1);
        JsonObject badOperation = modifierJson("prefix", "processing_speed", RETIRED, 1);
        require(MachineTraits.CODEC.parse(JsonOps.INSTANCE, traitsJson("rare", kept, badSlot)).result().isEmpty(),
                "known gap #81: an unknown slot name fails the whole traits decode");
        require(MachineTraits.CODEC.parse(JsonOps.INSTANCE, traitsJson("rare", kept, badOperation)).result().isEmpty(),
                "known gap #81: an unknown operation name fails the whole traits decode");
        require(MachineTraits.CODEC.parse(JsonOps.INSTANCE, traitsJson(RETIRED, kept)).result().isEmpty(),
                "known gap #81: an unknown rarity name fails the whole traits decode");
        JsonObject withBehavior = traitsJson("rare", kept);
        JsonArray behaviors = new JsonArray();
        behaviors.add("quick_feed");
        behaviors.add(RETIRED);
        withBehavior.add("behaviors", behaviors);
        require(MachineTraits.CODEC.parse(JsonOps.INSTANCE, withBehavior).result().isEmpty(),
                "known gap #81: an unknown behavior name fails the whole traits decode");
    }

    private static void progressionRoundTrip() {
        MachineProgressionState plain = build(MachineMasteryFamily.CRUSHER);
        roundTrip(MachineProgressionState.CODEC, plain, "progression with allocations");
        roundTrip(MachineProgressionState.CODEC, MachineProgressionState.EMPTY, "empty progression");

        var ascendancy = AscendancyCatalog.forFamily(MachineMasteryFamily.CRUSHER).getFirst();
        MachineProgressionState sealed = plain.withSealTier(1, MachineMasteryFamily.CRUSHER, AscendancyCatalog.ENTRY_STAGE, ascendancy.id());
        MachineProgressionState chosen = sealed.withAscendancyNode(ascendancy.nodes().values().stream()
                .filter(node -> node.parent().equals(ascendancy.root().id())).findFirst().orElseThrow().id());
        require(!chosen.ascendancyNodes().isEmpty(), "the progression fixture allocates an ascendancy node");
        roundTrip(MachineProgressionState.CODEC, chosen, "progression with an ascendancy");

        RegistryFriendlyByteBuf wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            MachineProgressionState.STREAM_CODEC.encode(wire, chosen);
            require(chosen.equals(MachineProgressionState.STREAM_CODEC.decode(wire)), "progression survives the network codec");
        } finally {
            wire.release();
        }
    }

    /**
     * An allocated node id that the tree no longer knows. Today the whole shared-tree allocation is dropped on load,
     * which refunds every point, including those spent on nodes that still exist. Issue #81 keeps the valid nodes.
     */
    private static void unknownMasteryNodes() {
        MachineProgressionState plain = build(MachineMasteryFamily.CRUSHER);
        JsonObject stored = MachineProgressionState.CODEC.encodeStart(JsonOps.INSTANCE, plain).getOrThrow().getAsJsonObject();
        JsonArray nodes = stored.getAsJsonArray("allocated_nodes");
        require(nodes.size() > 2, "the progression fixture spends several points");
        nodes.add(RETIRED);
        MachineProgressionState loaded = MachineProgressionState.CODEC.parse(JsonOps.INSTANCE, stored).getOrThrow();
        require(loaded.allocatedNodes().isEmpty(), "known gap #81: an unknown node id drops the whole shared-tree allocation");
        require(loaded.unspentPoints() == loaded.totalPoints(), "known gap #81: every point is refunded, valid nodes included");
    }

    private static MachineTraits sampleTraits() {
        List<MachineModifier> modifiers = List.of(
                new MachineModifier("crusher_speed", "processing_speed", ModifierSlot.PREFIX, MachineStat.PROCESSING_SPEED,
                        ModifierOperation.INCREASED_PERCENT, 2, new ModifierValueRange(10, 30), 21.5, List.of()),
                new MachineModifier(ModifierSlot.SUFFIX, MachineStat.ENERGY_USAGE, ModifierOperation.DECREASED_PERCENT, 3,
                        new ModifierValueRange(2, 9, true), 7),
                MachineModifier.roll("twin", "twin", ModifierSlot.PREFIX, 1, List.of(
                        MachineModifierEffect.fixed(MachineStat.OUTPUT_AMOUNT, ModifierOperation.MORE, 1.1),
                        MachineModifierEffect.fixed(MachineStat.ENERGY_USAGE, ModifierOperation.MORE, 1.2))),
                new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.INPUT_SLOTS, ModifierOperation.ADD, 1)
        );
        return new MachineTraits(Rarity.RARE, 3, modifiers, List.of(MachineBehavior.QUICK_FEED, MachineBehavior.AUTO_PURGE));
    }

    private static MachineProgressionState build(MachineMasteryFamily family) {
        String start = family.startNodeId();
        List<String> order = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>(List.of(start));
        java.util.ArrayDeque<String> queue = new java.util.ArrayDeque<>(List.of(start));
        while (!queue.isEmpty() && order.size() < 30) {
            for (String next : MegaPassiveTree.node(queue.remove()).links()) {
                if (order.size() < 30 && seen.add(next) && MegaPassiveTree.node(next).kind() != PassiveNodeKind.STARTER) {
                    order.add(next);
                    queue.add(next);
                }
            }
        }
        int level = MachineProgressionState.MAX_LEVEL;
        return new MachineProgressionState(MachineProgressionState.xpForLevel(level), 2, level, order, start, List.of(), false);
    }

    private static JsonObject traitsJson(String rarity, JsonObject... modifiers) {
        JsonObject traits = new JsonObject();
        traits.addProperty("rarity", rarity);
        traits.addProperty("refinement_potential", 1);
        JsonArray array = new JsonArray();
        for (JsonObject modifier : modifiers) {
            array.add(modifier);
        }
        traits.add("modifiers", array);
        return traits;
    }

    private static JsonObject modifierJson(String slot, String stat, String operation, double value) {
        return JsonParser.parseString("{\"slot\":\"" + slot + "\",\"stat\":\"" + stat + "\",\"operation\":\"" + operation
                + "\",\"value\":" + value + "}").getAsJsonObject();
    }

    private static <T> void roundTrip(Codec<T> codec, T value, String label) {
        roundTrip(codec, JsonOps.INSTANCE, value, label + " through JSON");
        roundTrip(codec, NbtOps.INSTANCE, value, label + " through NBT");
    }

    private static <T, E> void roundTrip(Codec<T> codec, DynamicOps<E> ops, T value, String label) {
        E encoded = codec.encodeStart(ops, value).getOrThrow();
        T decoded = codec.parse(ops, encoded).getOrThrow();
        require(value.equals(decoded), label + " decodes to the same value");
        require(encoded.equals(codec.encodeStart(ops, decoded).getOrThrow()), label + " re-encodes unchanged");
    }

    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError(label);
        }
    }
}
