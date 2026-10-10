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

    /** A stat name that no longer exists retires that one modifier; every other roll still loads and the entry is written back. */
    private static void unknownStatNamesInTraits() {
        JsonObject retired = modifierJson("suffix", RETIRED, "add", 4);
        JsonObject stored = storedWith(retired);
        MachineTraits loaded = MachineTraits.CODEC.parse(JsonOps.INSTANCE, stored).getOrThrow();
        require(loaded.rarity() == Rarity.RARE && loaded.modifiers().equals(knownTraits().modifiers()), "the other rolls survive an unknown stat name");
        require(loaded.retired().modifiers().size() == 1, "the unknown stat is kept as a retired entry");
        MachineStatAccumulator stats = new MachineStatAccumulator();
        stats.apply(new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.PROCESSING_SPEED, ModifierOperation.ADD, 100));
        loaded.modifiers().forEach(stats::apply);
        require(Math.abs(stats.value(MachineStat.PROCESSING_SPEED) - 125) < 1e-9, "stat building still applies the known roll");
        require(stored.equals(MachineTraits.CODEC.encodeStart(JsonOps.INSTANCE, loaded).getOrThrow()), "saving again keeps the unknown entry unchanged");
        require(!new MachineTraits(Rarity.NORMAL, 0, List.of(), List.of(), null, loaded.retired()).isEmpty(),
                "traits holding only retired entries are not treated as empty");

        JsonObject nested = modifierJson("suffix", "add", "add", 1);
        JsonObject effect = JsonParser.parseString("{\"stat\":\"" + RETIRED + "\",\"operation\":\"add\","
                + "\"range\":{\"min\":1,\"max\":2},\"value\":1}").getAsJsonObject();
        JsonArray effects = new JsonArray();
        effects.add(effect);
        nested.add("effects", effects);
        MachineTraits nestedLoaded = MachineTraits.CODEC.parse(JsonOps.INSTANCE, storedWith(nested)).getOrThrow();
        require(nestedLoaded.modifiers().size() == 1 && nestedLoaded.retired().modifiers().size() == 1,
                "an unknown stat inside a modifier's effects retires that modifier");

        var nbt = MachineTraits.CODEC.encodeStart(NbtOps.INSTANCE, loaded).getOrThrow();
        MachineTraits fromNbt = MachineTraits.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow();
        require(fromNbt.equals(MachineTraits.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow()) && nbt.equals(MachineTraits.CODEC.encodeStart(NbtOps.INSTANCE, fromNbt).getOrThrow()),
                "retired entries survive an NBT save and load");
        require(stored.equals(MachineTraits.CODEC.encodeStart(JsonOps.INSTANCE, fromNbt).getOrThrow()),
                "retired entries read from NBT are written back to JSON unchanged");
    }

    /** The same leniency for the other name-based enum codecs. */
    private static void unknownSlotOperationBehaviorAndRarity() {
        for (JsonObject bad : List.of(modifierJson(RETIRED, "processing_speed", "add", 1),
                modifierJson("prefix", "processing_speed", RETIRED, 1))) {
            JsonObject stored = storedWith(bad);
            MachineTraits loaded = MachineTraits.CODEC.parse(JsonOps.INSTANCE, stored).getOrThrow();
            require(loaded.modifiers().equals(knownTraits().modifiers()) && loaded.retired().modifiers().size() == 1,
                    "an unknown slot or operation retires one modifier");
            require(stored.equals(MachineTraits.CODEC.encodeStart(JsonOps.INSTANCE, loaded).getOrThrow()),
                    "an unknown slot or operation is written back unchanged");
        }

        JsonObject unknownRarity = storedWith();
        unknownRarity.addProperty("rarity", RETIRED);
        MachineTraits rarityLoaded = MachineTraits.CODEC.parse(JsonOps.INSTANCE, unknownRarity).getOrThrow();
        require(rarityLoaded.rarity() == Rarity.NORMAL && rarityLoaded.modifiers().equals(knownTraits().modifiers()),
                "an unknown rarity loads as Normal and keeps the rolls");
        require(unknownRarity.equals(MachineTraits.CODEC.encodeStart(JsonOps.INSTANCE, rarityLoaded).getOrThrow()),
                "an unknown rarity is written back unchanged");
        MachineTraits upgraded = new MachineTraits(Rarity.MAGIC, rarityLoaded.refinementPotential(), rarityLoaded.modifiers(),
                rarityLoaded.behaviors(), rarityLoaded.corruption(), rarityLoaded.retired());
        require(MachineTraits.CODEC.encodeStart(JsonOps.INSTANCE, upgraded).getOrThrow().getAsJsonObject().get("rarity").getAsString()
                .equals("magic"), "a rarity set after loading replaces the retired one");

        JsonObject withBehavior = storedWith();
        JsonArray behaviors = new JsonArray();
        behaviors.add("quick_feed");
        behaviors.add(RETIRED);
        withBehavior.add("behaviors", behaviors);
        MachineTraits behaviorLoaded = MachineTraits.CODEC.parse(JsonOps.INSTANCE, withBehavior).getOrThrow();
        require(behaviorLoaded.behaviors().equals(List.of(MachineBehavior.QUICK_FEED))
                        && behaviorLoaded.modifiers().equals(knownTraits().modifiers()),
                "an unknown behavior name leaves the other behaviors and the rolls");
        require(withBehavior.equals(MachineTraits.CODEC.encodeStart(JsonOps.INSTANCE, behaviorLoaded).getOrThrow()),
                "an unknown behavior is written back unchanged");

        JsonObject missing = storedWith();
        missing.remove("refinement_potential");
        require(MachineTraits.CODEC.parse(JsonOps.INSTANCE, missing).result().isEmpty(), "traits without a required field still fail to decode");
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

    /** A node id the tree no longer knows refunds only its own point. Valid nodes keep working. */
    private static void unknownMasteryNodes() {
        MachineProgressionState plain = build(MachineMasteryFamily.CRUSHER);
        JsonObject stored = encodedProgression(plain);
        require(stored.getAsJsonArray("allocated_nodes").size() > 3, "the progression fixture spends several points");
        stored.getAsJsonArray("allocated_nodes").add(RETIRED);
        MachineProgressionState loaded = MachineProgressionState.CODEC.parse(JsonOps.INSTANCE, stored).getOrThrow();
        require(loaded.allocatedNodes().equals(plain.allocatedNodes()), "valid nodes survive an unknown node id");
        require(loaded.spentPoints() == plain.spentPoints() && loaded.unspentPoints() == plain.unspentPoints(),
                "an unknown node id does not use a point");
        require(loaded.equals(plain), "the loaded progression equals the one without the retired node");
        require(!encodedProgression(loaded).toString().contains(RETIRED), "a retired node is refunded and not written back");

        JsonObject retiredFirst = encodedProgression(plain);
        JsonArray reordered = new JsonArray();
        reordered.add(RETIRED);
        retiredFirst.getAsJsonArray("allocated_nodes").forEach(reordered::add);
        retiredFirst.add("allocated_nodes", reordered);
        require(plain.equals(MachineProgressionState.CODEC.parse(JsonOps.INSTANCE, retiredFirst).getOrThrow()),
                "an unknown node id ahead of the build changes nothing");

        JsonObject gap = encodedProgression(plain);
        JsonArray original = gap.getAsJsonArray("allocated_nodes");
        JsonArray withoutThird = new JsonArray();
        for (int i = 0; i < original.size(); i++) {
            if (i != 2) {
                withoutThird.add(original.get(i));
            }
        }
        gap.add("allocated_nodes", withoutThird);
        MachineProgressionState partial = MachineProgressionState.CODEC.parse(JsonOps.INSTANCE, gap).getOrThrow();
        require(MegaPassiveTree.validBuild(plain.startNodeId(), partial.allocatedNodes()), "a build cut by a missing node loads as a legal build");
        require(partial.allocatedNodes().size() >= 2 && partial.allocatedNodes().size() < plain.allocatedNodes().size(),
                "nodes cut off by a missing node are refunded and the connected ones stay");
        require(partial.allocatedNodes().subList(0, 2).equals(plain.allocatedNodes().subList(0, 2)), "the connected part keeps its order");

        JsonObject target = encodedProgression(plain.withTarget(plain.allocatedNodes()));
        target.getAsJsonArray("target_nodes").add(RETIRED);
        MachineProgressionState withTarget = MachineProgressionState.CODEC.parse(JsonOps.INSTANCE, target).getOrThrow();
        require(withTarget.targetNodes().equals(plain.allocatedNodes()) && withTarget.following(),
                "an unknown node id in a target build is dropped");
    }

    private static JsonObject encodedProgression(MachineProgressionState state) {
        return MachineProgressionState.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow().getAsJsonObject();
    }

    private static MachineTraits knownTraits() {
        return new MachineTraits(Rarity.RARE, 1, List.of(
                new MachineModifier(ModifierSlot.PREFIX, MachineStat.PROCESSING_SPEED, ModifierOperation.INCREASED_PERCENT, 25)));
    }

    /** Stored form of {@link #knownTraits()} with raw entries appended to its modifiers, as a later version would leave them. */
    private static JsonObject storedWith(JsonObject... extraModifiers) {
        JsonObject stored = MachineTraits.CODEC.encodeStart(JsonOps.INSTANCE, knownTraits()).getOrThrow().getAsJsonObject();
        for (JsonObject extra : extraModifiers) {
            stored.getAsJsonArray("modifiers").add(extra);
        }
        return stored;
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
