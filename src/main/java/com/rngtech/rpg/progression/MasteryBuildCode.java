package com.rngtech.rpg.progression;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

/** An ordered shared-tree build, plus an optional ascendancy and its allocation order. */
public record MasteryBuildCode(String start, List<String> nodes, String ascendancy, List<String> ascendancyNodes) {
    public static final int MAX_LENGTH = 10000;
    public MasteryBuildCode {
        nodes = List.copyOf(nodes);
        ascendancy = ascendancy == null ? "" : ascendancy;
        ascendancyNodes = List.copyOf(ascendancyNodes);
    }
    public MasteryBuildCode(String start, List<String> nodes) { this(start, nodes, "", List.of()); }

    public static MasteryBuildCode copy(MachineProgressionState state) {
        return new MasteryBuildCode(state.startNodeId(), state.targetNodes().isEmpty() ? state.allocatedNodes() : state.targetNodes(),
                state.ascendancy(), state.ascendancyNodes());
    }
    public String encode() {
        JsonObject object = new JsonObject(); object.addProperty("tree", "rngtech:machine_mastery");
        object.addProperty("version", MegaPassiveTree.VERSION); object.addProperty("start", start);
        JsonArray array = new JsonArray(); nodes.forEach(array::add); object.add("nodes", array);
        if (!ascendancy.isEmpty()) {
            object.addProperty("ascendancy", ascendancy);
            JsonArray ascendant = new JsonArray(); ascendancyNodes.forEach(ascendant::add); object.add("ascendancy_nodes", ascendant);
        }
        return object.toString();
    }
    /** Rejects an invalid shared-tree build. An unknown ascendancy or invalid ascendancy order is dropped, so older codes still paste. */
    public static MasteryBuildCode decode(String text) {
        if (text.length() > MAX_LENGTH) { throw new IllegalArgumentException("Build code too large"); }
        try {
            JsonObject object = JsonParser.parseString(text).getAsJsonObject();
            int version = object.get("version").getAsInt();
            if (!object.get("tree").getAsString().equals("rngtech:machine_mastery")
                    || !MegaPassiveTree.migrates(version)) { throw new IllegalArgumentException("Different tree version"); }
            String start = object.get("start").getAsString();
            List<String> saved = new ArrayList<>(); object.getAsJsonArray("nodes").forEach(e -> saved.add(e.getAsString()));
            // An older catalog's code keeps what still connects after its nodes are renamed or dropped.
            List<String> nodes = version == MegaPassiveTree.VERSION ? saved : MegaPassiveTree.salvage(start, MegaPassiveTree.migrate(version, saved));
            if (!MegaPassiveTree.validBuild(start, nodes)) { throw new IllegalArgumentException("Invalid or disconnected build"); }
            Ascendancy ascendancy = object.has("ascendancy") ? AscendancyCatalog.get(object.get("ascendancy").getAsString()) : null;
            List<String> order = new ArrayList<>();
            if (ascendancy != null && object.has("ascendancy_nodes")) { object.getAsJsonArray("ascendancy_nodes").forEach(e -> order.add(e.getAsString())); }
            if (ascendancy == null || order.size() > AscendancyCatalog.MAX_POINTS || !ascendancy.validOrder(order)) { return new MasteryBuildCode(start, nodes); }
            return new MasteryBuildCode(start, nodes, ascendancy.id(), order);
        } catch (RuntimeException exception) { throw new IllegalArgumentException("Invalid mastery build code", exception); }
    }
}
