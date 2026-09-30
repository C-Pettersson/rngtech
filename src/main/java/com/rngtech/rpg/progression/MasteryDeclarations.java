package com.rngtech.rpg.progression;

import com.rngtech.rpg.MachineStat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Declared Mastery stats and behaviors: the families that support them beyond the built-in rules in
 * {@link MachineMasteryFamily}, and whether they are yield for the recipe loop audit. New ascendancy stats and
 * behaviors are declared in {@code data/rngtech/mastery/declarations.json} instead of in family code.
 */
public final class MasteryDeclarations {
    /** What a stat or behavior can multiply: produced items or fluid, saved inputs, or energy. */
    public enum Yield { NONE, OUTPUT, INPUT, ENERGY }

    public record Declaration(Set<MachineMasteryFamily> families, Yield yield) {
        private static final Declaration NONE = new Declaration(Set.of(), Yield.NONE);
    }

    private static final String PATH = "/data/rngtech/mastery/declarations.json";
    /** Present only on the domain-check classpath. */
    private static final String FIXTURE_PATH = "/data/rngtech/mastery/ascendancies/fixtures/declarations.json";
    private static final Map<MachineStat, Declaration> STATS = new EnumMap<>(MachineStat.class);
    private static final Map<String, Declaration> BEHAVIORS = new HashMap<>();

    static {
        JsonObject main = MasteryNodeJson.resource(PATH);
        if (main == null) { throw new IllegalStateException("Missing Mastery declarations"); }
        load(main);
        JsonObject fixture = MasteryNodeJson.resource(FIXTURE_PATH);
        if (fixture != null) { load(fixture); }
    }

    private MasteryDeclarations() { }

    public static Set<MachineMasteryFamily> families(MachineStat stat) { return STATS.getOrDefault(stat, Declaration.NONE).families(); }
    public static Set<MachineMasteryFamily> families(String behavior) { return BEHAVIORS.getOrDefault(behavior, Declaration.NONE).families(); }
    public static Yield yield(MachineStat stat) { return STATS.getOrDefault(stat, Declaration.NONE).yield(); }
    public static Yield yield(String behavior) { return BEHAVIORS.getOrDefault(behavior, Declaration.NONE).yield(); }
    public static boolean declared(String behavior) { return BEHAVIORS.containsKey(behavior); }
    public static boolean declared(MachineStat stat) { return STATS.containsKey(stat); }

    private static void load(JsonObject data) {
        for (JsonElement element : MasteryNodeJson.array(data, "stats")) {
            JsonObject raw = element.getAsJsonObject();
            MachineStat stat = MachineStat.valueOf(raw.get("stat").getAsString());
            if (STATS.put(stat, declaration(raw)) != null) { throw new IllegalStateException("Duplicate stat declaration " + stat); }
        }
        for (JsonElement element : MasteryNodeJson.array(data, "behaviors")) {
            JsonObject raw = element.getAsJsonObject();
            String id = raw.get("id").getAsString();
            if (BEHAVIORS.put(id, declaration(raw)) != null) { throw new IllegalStateException("Duplicate behavior declaration " + id); }
        }
    }

    private static Declaration declaration(JsonObject raw) {
        Set<MachineMasteryFamily> families = EnumSet.noneOf(MachineMasteryFamily.class);
        for (JsonElement family : MasteryNodeJson.array(raw, "families")) {
            String name = family.getAsString();
            if (name.equals("*")) { families.addAll(EnumSet.allOf(MachineMasteryFamily.class)); }
            else { families.add(MachineMasteryFamily.valueOf(name.toUpperCase(Locale.ROOT))); }
        }
        Yield yield = raw.has("yield") ? Yield.valueOf(raw.get("yield").getAsString().toUpperCase(Locale.ROOT)) : Yield.NONE;
        return new Declaration(Set.copyOf(families), yield);
    }
}
