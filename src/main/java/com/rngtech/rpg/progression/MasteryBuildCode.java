package com.rngtech.rpg.progression;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

public record MasteryBuildCode(String start, List<String> nodes) {
    public static final int MAX_LENGTH = 10000;
    public MasteryBuildCode { nodes = List.copyOf(nodes); }

    public static MasteryBuildCode copy(MachineProgressionState state) {
        return new MasteryBuildCode(state.startNodeId(), state.targetNodes().isEmpty() ? state.allocatedNodes() : state.targetNodes());
    }
    public String encode() {
        JsonObject object = new JsonObject(); object.addProperty("tree", "rngtech:machine_mastery");
        object.addProperty("version", MegaPassiveTree.VERSION); object.addProperty("start", start);
        JsonArray array = new JsonArray(); nodes.forEach(array::add); object.add("nodes", array);
        return object.toString();
    }
    public static MasteryBuildCode decode(String text) {
        if (text.length() > MAX_LENGTH) { throw new IllegalArgumentException("Build code too large"); }
        try {
            JsonObject object = JsonParser.parseString(text).getAsJsonObject();
            if (!object.get("tree").getAsString().equals("rngtech:machine_mastery")
                    || object.get("version").getAsInt() != MegaPassiveTree.VERSION) { throw new IllegalArgumentException("Different tree version"); }
            String start = object.get("start").getAsString();
            List<String> nodes = new ArrayList<>(); object.getAsJsonArray("nodes").forEach(e -> nodes.add(e.getAsString()));
            if (!MegaPassiveTree.validBuild(start, nodes)) { throw new IllegalArgumentException("Invalid or disconnected build"); }
            return new MasteryBuildCode(start, nodes);
        } catch (RuntimeException exception) { throw new IllegalArgumentException("Invalid mastery build code", exception); }
    }
}
