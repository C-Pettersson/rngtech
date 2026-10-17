package com.rngtech.rpg.dormant;

import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.MegaPassiveNode;
import com.rngtech.rpg.progression.MegaPassiveTree;
import com.rngtech.rpg.progression.PassiveNodeKind;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One Dormant Machine site type, loaded from {@code data/<namespace>/dormant_machine/<site>.json}: which families its
 * souls roll, their level band, the wake FE range and curve, the wake input rate, the lowest machine stage a soul may be
 * released into, and optional archetype builds per family.
 */
public record DormantSite(
        String id,
        Map<MachineMasteryFamily, Integer> families,
        int minLevel,
        int maxLevel,
        long minWakeFe,
        long maxWakeFe,
        double wakeCurve,
        int wakeInput,
        int releaseStage,
        Map<MachineMasteryFamily, List<Archetype>> builds
) {
    public static final int MAX_STAGE = 8;

    /** A weighted build: the soul's followed build runs through these nodes in order. */
    public record Archetype(int weight, List<String> targets) {
        public Archetype {
            targets = List.copyOf(targets);
        }
    }

    public DormantSite {
        families = Map.copyOf(families);
        builds = Map.copyOf(builds);
    }

    /** The FE a soul of {@code level} needs to wake: the site's range, placed by the level's share of the band on the curve. */
    public long wakeFe(int level) {
        if (maxLevel <= minLevel) { return maxWakeFe; }
        double share = Math.clamp((level - minLevel) / (double) (maxLevel - minLevel), 0.0D, 1.0D);
        return minWakeFe + Math.round((maxWakeFe - minWakeFe) * Math.pow(share, wakeCurve));
    }

    public static DormantSite parse(String id, JsonObject json) {
        Map<MachineMasteryFamily, Integer> families = new LinkedHashMap<>();
        JsonObject familyWeights = object(json, "families");
        for (Map.Entry<String, JsonElement> entry : familyWeights.entrySet()) {
            int weight = entry.getValue().getAsInt();
            if (weight <= 0) { throw new IllegalArgumentException(id + ": family weights must be positive"); }
            families.put(family(id, entry.getKey()), weight);
        }
        if (families.isEmpty()) { throw new IllegalArgumentException(id + ": a site needs at least one family"); }

        JsonObject level = object(json, "level");
        int minLevel = level.get("min").getAsInt();
        int maxLevel = level.get("max").getAsInt();
        if (minLevel < MachineProgressionState.MIN_LEVEL || maxLevel > MachineProgressionState.MAX_LEVEL || minLevel > maxLevel) {
            throw new IllegalArgumentException(id + ": level must run low to high within 1-100");
        }
        JsonObject wake = object(json, "wake_fe");
        long minWakeFe = wake.get("min").getAsLong();
        long maxWakeFe = wake.get("max").getAsLong();
        if (minWakeFe <= 0 || minWakeFe > maxWakeFe) { throw new IllegalArgumentException(id + ": wake_fe must run low to high above 0"); }
        double curve = json.has("wake_curve") ? json.get("wake_curve").getAsDouble() : 2.0D;
        if (!(curve >= 1.0D)) { throw new IllegalArgumentException(id + ": wake_curve must be at least 1"); }
        int wakeInput = json.get("wake_input").getAsInt();
        if (wakeInput <= 0) { throw new IllegalArgumentException(id + ": wake_input must be positive"); }
        int releaseStage = json.has("release_stage") ? json.get("release_stage").getAsInt() : 0;
        if (releaseStage < 0 || releaseStage > MAX_STAGE) { throw new IllegalArgumentException(id + ": release_stage must be 0-" + MAX_STAGE); }

        Map<MachineMasteryFamily, List<Archetype>> builds = new EnumMap<>(MachineMasteryFamily.class);
        if (json.has("builds")) {
            for (Map.Entry<String, JsonElement> entry : object(json, "builds").entrySet()) {
                MachineMasteryFamily family = family(id, entry.getKey());
                List<Archetype> archetypes = new ArrayList<>();
                for (JsonElement element : entry.getValue().getAsJsonArray()) {
                    archetypes.add(archetype(id, family, element.getAsJsonObject()));
                }
                builds.put(family, List.copyOf(archetypes));
            }
        }
        return new DormantSite(id, families, minLevel, maxLevel, minWakeFe, maxWakeFe, curve, wakeInput, releaseStage, builds);
    }

    private static Archetype archetype(String id, MachineMasteryFamily family, JsonObject json) {
        int weight = json.has("weight") ? json.get("weight").getAsInt() : 1;
        if (weight <= 0) { throw new IllegalArgumentException(id + ": build weights must be positive"); }
        List<String> targets = new ArrayList<>();
        JsonArray array = json.getAsJsonArray("targets");
        for (JsonElement element : array) {
            String target = element.getAsString();
            MegaPassiveNode node = MegaPassiveTree.node(target);
            if (node == null || node.kind() == PassiveNodeKind.STARTER) {
                throw new IllegalArgumentException(id + ": unknown build target " + target + " for " + family.name().toLowerCase(Locale.ROOT));
            }
            targets.add(target);
        }
        if (targets.isEmpty()) { throw new IllegalArgumentException(id + ": a build needs at least one target"); }
        return new Archetype(weight, targets);
    }

    private static MachineMasteryFamily family(String id, String name) {
        try {
            return MachineMasteryFamily.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(id + ": unknown Mastery family " + name, exception);
        }
    }

    private static JsonObject object(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonObject()) { throw new IllegalArgumentException("missing object " + key); }
        return json.getAsJsonObject(key);
    }
}
