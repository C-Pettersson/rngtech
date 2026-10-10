package com.rngtech.rpg.corruption;

import com.rngtech.rpg.CorruptionOutcome;
import com.rngtech.rpg.MachineBehavior;
import com.rngtech.rpg.MachineCorruption;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineModifierEffect;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.ModifierOperation;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.ModifierValueRange;
import com.rngtech.rpg.progression.MasteryDeclarations;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.RandomSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Volatile Catalyst data: outcome weights from {@code data/rngtech/corruption/outcomes.json} and per-host implicit pools
 * from {@code data/rngtech/corruption/pools/*.json}. A pool lists the eligibility profile ids it applies to; a host may
 * appear in several pools and draws from all of their entries. Every entry is validated when it loads.
 *
 * <p>The mod's own files load from the classpath at startup, so headless checks and clients see the defaults. A server
 * replaces the active catalog from datapacks on reload and syncs it to clients.
 */
public final class CorruptionCatalog {
    public static final String DIRECTORY = "data/rngtech/corruption/";
    /** Lists the default pool files, since a classpath directory cannot be listed from a jar. */
    public static final String POOL_INDEX = DIRECTORY + "pool_index.json";
    public static final String OUTCOMES = DIRECTORY + "outcomes.json";

    /** Stats that multiply items or fluid, or save inputs. A 2.0 pool must not grant them. */
    private static final Set<MachineStat> YIELD_STATS = EnumSet.of(
            MachineStat.OUTPUT_AMOUNT,
            MachineStat.SUPER_OUTPUT_CHANCE,
            MachineStat.MAGIC_FIND,
            MachineStat.CRUSHER_SALVAGE_CHANCE,
            MachineStat.LUCK
    );
    private static final Set<MachineStat> REFINEMENT_POTENTIAL_STATS =
            EnumSet.of(MachineStat.REFINEMENT_POTENTIAL, MachineStat.REFINEMENT_POTENTIAL_BONUS);
    /** Stats that change which Gear or blocks a host accepts. */
    private static final Set<MachineStat> STAGE_ACCEPTANCE_STATS =
            EnumSet.of(MachineStat.UPGRADE_LIMIT, MachineStat.MINING_LEVEL);

    private static final String UNIQUE_PREFIX = "unique_";

    private static volatile CorruptionCatalog active;

    private final Map<CorruptionOutcome, Integer> weights;
    private final WarpRange warp;
    private final Map<String, Pools> hosts;
    private final Source source;

    /** How far Warped moves each affix, in percent of its value; each affix rolls its own percent in this range. */
    public record WarpRange(double minPercent, double maxPercent) {
        public static final WarpRange DEFAULT = new WarpRange(-15.0, 15.0);

        public WarpRange {
            if (!(minPercent <= maxPercent) || minPercent <= -100.0) {
                throw new IllegalArgumentException("Warp range needs min_percent above -100 and no greater than max_percent");
            }
        }

        public double roll(RandomSource random) {
            return minPercent + random.nextDouble() * (maxPercent - minPercent);
        }
    }

    public record Entry(String id, int weight, List<MachineModifierEffect> effects, List<MachineBehavior> behaviors) {
        public Entry {
            effects = List.copyOf(effects);
            behaviors = List.copyOf(behaviors);
        }

        public MachineCorruption toCorruption(CorruptionOutcome outcome) {
            List<MachineModifier> modifiers = effects.stream()
                    .map(effect -> new MachineModifier(
                            "",
                            "corruption:" + id,
                            ModifierSlot.CORRUPTION,
                            effect.stat(),
                            effect.operation(),
                            0,
                            ModifierValueRange.fixed(effect.value()),
                            effect.value(),
                            List.of()
                    ))
                    .toList();
            return new MachineCorruption(outcome, modifiers, behaviors);
        }
    }

    public record Pools(List<Entry> blessed, List<Entry> blighted) {
        public Pools {
            blessed = List.copyOf(blessed);
            blighted = List.copyOf(blighted);
        }

        public List<Entry> entries(CorruptionOutcome outcome) {
            return switch (outcome) {
                case BLESSED -> blessed;
                case BLIGHTED -> blighted;
                default -> List.of();
            };
        }
    }

    /** The raw files a catalog came from, kept so a server can sync exactly what it loaded. */
    public record Source(String outcomes, Map<String, String> pools) {
        public Source {
            pools = Map.copyOf(new TreeMap<>(pools));
        }
    }

    private CorruptionCatalog(Map<CorruptionOutcome, Integer> weights, WarpRange warp, Map<String, Pools> hosts, Source source) {
        this.weights = weights;
        this.warp = warp;
        this.hosts = hosts;
        this.source = source;
    }

    public static CorruptionCatalog active() {
        CorruptionCatalog catalog = active;
        if (catalog == null) {
            catalog = defaults();
            active = catalog;
        }
        return catalog;
    }

    public static void setActive(CorruptionCatalog catalog) {
        active = catalog;
    }

    /** The catalog shipped in the mod's own data, loaded from the classpath. */
    public static CorruptionCatalog defaults() {
        Map<String, String> pools = new LinkedHashMap<>();
        for (JsonElement name : JsonParser.parseString(resource(POOL_INDEX)).getAsJsonObject().getAsJsonArray("pools")) {
            String poolName = name.getAsString();
            pools.put(poolName, resource(DIRECTORY + "pools/" + poolName + ".json"));
        }
        return parse(new Source(resource(OUTCOMES), pools));
    }

    /** Parses and validates a catalog, throwing {@link IllegalArgumentException} that names the bad file and entry. */
    public static CorruptionCatalog parse(Source source) {
        JsonObject outcomes = JsonParser.parseString(source.outcomes()).getAsJsonObject();
        Map<CorruptionOutcome, Integer> weights = parseWeights(outcomes);
        WarpRange warp = parseWarp(outcomes);
        Map<String, List<Entry>> blessed = new TreeMap<>();
        Map<String, List<Entry>> blighted = new TreeMap<>();
        for (Map.Entry<String, String> file : source.pools().entrySet()) {
            String pool = file.getKey();
            try {
                JsonObject json = JsonParser.parseString(file.getValue()).getAsJsonObject();
                List<String> poolHosts = strings(json, "hosts");
                if (poolHosts.isEmpty()) {
                    throw new IllegalArgumentException("lists no hosts");
                }
                List<Entry> poolBlessed = entries(json, "blessed");
                List<Entry> poolBlighted = entries(json, "blighted");
                for (String host : poolHosts) {
                    if (!CorruptionHostReaders.isKnownHost(host)) {
                        throw new IllegalArgumentException("names unknown host " + host);
                    }
                    validate(host, poolBlessed);
                    validate(host, poolBlighted);
                    blessed.computeIfAbsent(host, ignored -> new ArrayList<>()).addAll(poolBlessed);
                    blighted.computeIfAbsent(host, ignored -> new ArrayList<>()).addAll(poolBlighted);
                }
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("Corruption pool " + pool + ": " + exception.getMessage(), exception);
            }
        }
        Map<String, Pools> hosts = new TreeMap<>();
        Set<String> hostIds = new HashSet<>(blessed.keySet());
        hostIds.addAll(blighted.keySet());
        for (String host : hostIds) {
            List<Entry> hostBlessed = blessed.getOrDefault(host, List.of());
            List<Entry> hostBlighted = blighted.getOrDefault(host, List.of());
            requireUniqueIds(host, hostBlessed);
            requireUniqueIds(host, hostBlighted);
            hosts.put(host, new Pools(hostBlessed, hostBlighted));
        }
        return new CorruptionCatalog(Map.copyOf(weights), warp, Map.copyOf(hosts), source);
    }

    public Source source() {
        return source;
    }

    /** Whether the host has any implicit to grant. Hosts without a pool cannot be corrupted. */
    public boolean canCorrupt(String hostId) {
        Pools pools = hosts.get(poolHost(hostId));
        return pools != null && (!pools.blessed().isEmpty() || !pools.blighted().isEmpty());
    }

    public Set<String> hosts() {
        return hosts.keySet();
    }

    public List<Entry> entries(String hostId, CorruptionOutcome outcome) {
        Pools pools = hosts.get(poolHost(hostId));
        return pools == null ? List.of() : pools.entries(outcome);
    }

    public WarpRange warp() {
        return warp;
    }

    /** Outcome weights; a Stabilization Crystal moves the Blighted weight to Untouched. */
    public Map<CorruptionOutcome, Integer> weights(boolean warded) {
        Map<CorruptionOutcome, Integer> result = new EnumMap<>(weights);
        if (warded) {
            result.merge(CorruptionOutcome.UNTOUCHED, result.getOrDefault(CorruptionOutcome.BLIGHTED, 0), Integer::sum);
            result.put(CorruptionOutcome.BLIGHTED, 0);
        }
        return result;
    }

    public CorruptionOutcome rollOutcome(boolean warded, RandomSource random) {
        Map<CorruptionOutcome, Integer> table = weights(warded);
        int total = table.values().stream().mapToInt(Integer::intValue).sum();
        int roll = random.nextInt(total);
        for (CorruptionOutcome outcome : CorruptionOutcome.values()) {
            roll -= table.getOrDefault(outcome, 0);
            if (roll < 0) {
                return outcome;
            }
        }
        return CorruptionOutcome.UNTOUCHED;
    }

    /** One weighted implicit for the host, or null when its pool for that outcome is empty. */
    public Entry rollEntry(String hostId, CorruptionOutcome outcome, RandomSource random) {
        List<Entry> entries = entries(hostId, outcome);
        int total = entries.stream().mapToInt(Entry::weight).sum();
        if (total <= 0) {
            return null;
        }
        int roll = random.nextInt(total);
        for (Entry entry : entries) {
            roll -= entry.weight();
            if (roll < 0) {
                return entry;
            }
        }
        return entries.getLast();
    }

    /** A Unique part's {@code unique_<part>} profile draws from its part's pools unless a pool names it directly. */
    private String poolHost(String hostId) {
        if (!hosts.containsKey(hostId) && hostId.startsWith(UNIQUE_PREFIX)) {
            return hostId.substring(UNIQUE_PREFIX.length());
        }
        return hostId;
    }

    /** Why {@code stat} cannot appear in a 2.0 pool, or null when it can. */
    public static String forbiddenReason(MachineStat stat) {
        if (REFINEMENT_POTENTIAL_STATS.contains(stat)) {
            return "grants Refinement Potential";
        }
        if (STAGE_ACCEPTANCE_STATS.contains(stat)) {
            return "changes stage acceptance";
        }
        MasteryDeclarations.Yield yield = MasteryDeclarations.yield(stat);
        if (YIELD_STATS.contains(stat) || yield == MasteryDeclarations.Yield.OUTPUT || yield == MasteryDeclarations.Yield.INPUT) {
            return "is a yield stat";
        }
        return null;
    }

    private static WarpRange parseWarp(JsonObject json) {
        JsonObject raw = json.getAsJsonObject("warp");
        if (raw == null) {
            return WarpRange.DEFAULT;
        }
        return new WarpRange(raw.get("min_percent").getAsDouble(), raw.get("max_percent").getAsDouble());
    }

    private static Map<CorruptionOutcome, Integer> parseWeights(JsonObject json) {
        JsonObject raw = json.getAsJsonObject("weights");
        if (raw == null) {
            throw new IllegalArgumentException("Corruption outcomes need a weights object");
        }
        Map<CorruptionOutcome, Integer> weights = new EnumMap<>(CorruptionOutcome.class);
        for (Map.Entry<String, JsonElement> entry : raw.entrySet()) {
            CorruptionOutcome outcome = outcome(entry.getKey());
            int weight = entry.getValue().getAsInt();
            if (weight < 0) {
                throw new IllegalArgumentException("Corruption outcome " + entry.getKey() + " has a negative weight");
            }
            weights.put(outcome, weight);
        }
        for (CorruptionOutcome outcome : CorruptionOutcome.values()) {
            weights.putIfAbsent(outcome, 0);
        }
        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) {
            throw new IllegalArgumentException("Corruption outcome weights sum to zero");
        }
        return weights;
    }

    private static CorruptionOutcome outcome(String name) {
        for (CorruptionOutcome outcome : CorruptionOutcome.values()) {
            if (outcome.getSerializedName().equals(name)) {
                return outcome;
            }
        }
        throw new IllegalArgumentException("Unknown corruption outcome " + name);
    }

    private static List<Entry> entries(JsonObject json, String key) {
        List<Entry> entries = new ArrayList<>();
        JsonArray array = json.getAsJsonArray(key);
        if (array == null) {
            return entries;
        }
        for (JsonElement element : array) {
            JsonObject raw = element.getAsJsonObject();
            String id = raw.get("id").getAsString();
            int weight = raw.has("weight") ? raw.get("weight").getAsInt() : 1;
            if (weight <= 0) {
                throw new IllegalArgumentException(key + " entry " + id + " needs a positive weight");
            }
            List<MachineModifierEffect> effects = new ArrayList<>();
            JsonArray modifiers = raw.getAsJsonArray("modifiers");
            if (modifiers != null) {
                for (JsonElement modifier : modifiers) {
                    effects.add(effect(id, modifier.getAsJsonObject()));
                }
            }
            List<MachineBehavior> behaviors = new ArrayList<>();
            for (String behavior : strings(raw, "behaviors")) {
                behaviors.add(behavior(id, behavior));
            }
            if (effects.isEmpty() && behaviors.isEmpty()) {
                throw new IllegalArgumentException(key + " entry " + id + " grants nothing");
            }
            entries.add(new Entry(id, weight, effects, behaviors));
        }
        return entries;
    }

    private static MachineModifierEffect effect(String entryId, JsonObject raw) {
        if (raw.has("min") || raw.has("max") || raw.has("tiers")) {
            throw new IllegalArgumentException("entry " + entryId + " has a range; corruption implicits have one fixed value");
        }
        MachineStat stat = MachineStat.fromName(raw.get("stat").getAsString());
        ModifierOperation operation = operation(raw.get("operation").getAsString());
        double value = raw.get("value").getAsDouble();
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("entry " + entryId + " has a non-finite value");
        }
        if ((operation == ModifierOperation.MORE || operation == ModifierOperation.LESS) && value <= 0.0) {
            throw new IllegalArgumentException("entry " + entryId + " needs a positive multiplier for " + operation.getSerializedName());
        }
        if (operation == ModifierOperation.MORE && value < 1.0 || operation == ModifierOperation.LESS && value > 1.0) {
            throw new IllegalArgumentException("entry " + entryId + " has a " + operation.getSerializedName()
                    + " multiplier pointing the wrong way");
        }
        return MachineModifierEffect.fixed(stat, operation, value);
    }

    private static ModifierOperation operation(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        for (ModifierOperation operation : ModifierOperation.values()) {
            if (operation.getSerializedName().equals(key)) {
                return operation;
            }
        }
        throw new IllegalArgumentException("Unknown modifier operation " + name);
    }

    private static MachineBehavior behavior(String entryId, String name) {
        String key = name.toLowerCase(Locale.ROOT);
        for (MachineBehavior behavior : MachineBehavior.values()) {
            if (behavior.getSerializedName().equals(key)) {
                return behavior;
            }
        }
        throw new IllegalArgumentException("entry " + entryId + " names unknown behavior " + name);
    }

    private static void validate(String host, List<Entry> entries) {
        for (Entry entry : entries) {
            for (MachineModifierEffect effect : entry.effects()) {
                String forbidden = forbiddenReason(effect.stat());
                if (forbidden != null) {
                    throw new IllegalArgumentException("entry " + entry.id() + ": " + effect.stat().getSerializedName() + " " + forbidden);
                }
                if (!CorruptionHostReaders.readsStat(host, effect.stat())) {
                    throw new IllegalArgumentException("entry " + entry.id() + ": " + host + " does not read "
                            + effect.stat().getSerializedName());
                }
            }
            for (MachineBehavior behavior : entry.behaviors()) {
                MasteryDeclarations.Yield yield = MasteryDeclarations.yield(behavior.name());
                if (yield == MasteryDeclarations.Yield.OUTPUT || yield == MasteryDeclarations.Yield.INPUT) {
                    throw new IllegalArgumentException("entry " + entry.id() + ": behavior " + behavior.getSerializedName() + " is yield");
                }
                if (!CorruptionHostReaders.readsBehavior(host, behavior)) {
                    throw new IllegalArgumentException("entry " + entry.id() + ": " + host + " does not read behavior "
                            + behavior.getSerializedName());
                }
            }
        }
    }

    private static void requireUniqueIds(String host, List<Entry> entries) {
        Set<String> ids = new HashSet<>();
        for (Entry entry : entries) {
            if (!ids.add(entry.id())) {
                throw new IllegalArgumentException("Corruption host " + host + " has two entries named " + entry.id());
            }
        }
    }

    private static List<String> strings(JsonObject json, String key) {
        List<String> values = new ArrayList<>();
        JsonArray array = json.getAsJsonArray(key);
        if (array != null) {
            array.forEach(element -> values.add(element.getAsString()));
        }
        return values;
    }

    private static String resource(String path) {
        try (InputStream stream = CorruptionCatalog.class.getResourceAsStream("/" + path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing corruption data " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read corruption data " + path, exception);
        }
    }
}
