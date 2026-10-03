package com.rngtech.rpg.progression;

import com.rngtech.rpg.MachineModifierEffect;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.ModifierOperation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared JSON reader for node effects in the shared tree and ascendancy catalogs. Missing effect fields mean none. */
final class MasteryNodeJson {
    record Effects(
            List<MachineModifierEffect> effects, List<MegaPassiveNode.TaggedEffect> tagged, Set<String> behaviors,
            Map<MachineStat, Double> fixed, Map<MachineStat, Double> ceilings,
            List<MegaPassiveNode.AttributeScaling> scaling, Map<PassiveStatType, Integer> passive, int recipeHardnessCeiling
    ) {
    }

    private MasteryNodeJson() { }

    static Effects effects(JsonObject raw) {
        List<MachineModifierEffect> effects = new ArrayList<>();
        List<MegaPassiveNode.TaggedEffect> tagged = new ArrayList<>();
        for (JsonElement item : array(raw, "effects")) {
            JsonObject e = item.getAsJsonObject();
            MachineModifierEffect effect = MachineModifierEffect.fixed(MachineStat.valueOf(e.get("stat").getAsString()), ModifierOperation.valueOf(e.get("operation").getAsString()), e.get("value").getAsDouble());
            if (e.has("tag")) { tagged.add(new MegaPassiveNode.TaggedEffect(MachineTag.valueOf(e.get("tag").getAsString()), effect)); }
            else { effects.add(effect); }
        }
        Set<String> behaviors = new HashSet<>(); array(raw, "behaviors").forEach(e -> behaviors.add(e.getAsString()));
        List<MegaPassiveNode.AttributeScaling> scaling = new ArrayList<>();
        array(raw, "scaling").forEach(e -> {
            JsonObject s = e.getAsJsonObject();
            scaling.add(new MegaPassiveNode.AttributeScaling(MachineStat.valueOf(s.get("attribute").getAsString()), MachineStat.valueOf(s.get("stat").getAsString()), ModifierOperation.valueOf(s.get("operation").getAsString()), s.get("perPoint").getAsDouble()));
        });
        Map<PassiveStatType, Integer> passive = new HashMap<>();
        object(raw, "passive").entrySet().forEach(e -> passive.put(PassiveStatType.valueOf(e.getKey()), e.getValue().getAsInt()));
        return new Effects(List.copyOf(effects), List.copyOf(tagged), Set.copyOf(behaviors), statsMap(object(raw, "fixed")), statsMap(object(raw, "ceilings")),
                List.copyOf(scaling), Map.copyOf(passive), raw.has("recipeHardnessCeiling") ? raw.get("recipeHardnessCeiling").getAsInt() : 0);
    }

    static JsonObject resource(String path) {
        try (InputStream stream = MasteryNodeJson.class.getResourceAsStream(path)) {
            if (stream == null) { return null; }
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException exception) { throw new IllegalStateException("Cannot read " + path, exception); }
    }

    static JsonArray array(JsonObject raw, String key) { return raw.has(key) ? raw.getAsJsonArray(key) : new JsonArray(); }

    private static JsonObject object(JsonObject raw, String key) { return raw.has(key) ? raw.getAsJsonObject(key) : new JsonObject(); }

    private static Map<MachineStat, Double> statsMap(JsonObject raw) {
        Map<MachineStat, Double> result = new HashMap<>();
        raw.entrySet().forEach(e -> result.put(MachineStat.valueOf(e.getKey()), e.getValue().getAsDouble()));
        return Map.copyOf(result);
    }
}
