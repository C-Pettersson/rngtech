package com.rngtech.rpg.unique;

import com.rngtech.rpg.ComponentBaseStatCatalog;
import com.rngtech.rpg.MachineBehavior;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.progression.Ascendancy;
import com.rngtech.rpg.progression.AscendancyCatalog;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MasteryDeclarations;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The Unique catalog: {@code data/rngtech/uniques/index.json} lists one file per built-in Unique, and packs may add more as
 * {@code config/rngtech/uniques/*.json}. Every file is validated when the catalog loads. A violation in a built-in file
 * stops the game, so a shipped Unique always has readable stats, assets, and loot; a violation in a pack file skips that
 * file with a logged error.
 */
public final class UniqueCatalog {
    /** The newest catalog file format this version reads; files without a {@code version} are version 1. */
    public static final int FORMAT_VERSION = 1;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> EXTERNAL = new LinkedHashSet<>();
    private static final String NAMESPACE = "rngtech";
    private static final String DIRECTORY = "/data/rngtech/uniques/";
    private static final String LANGUAGE = "/assets/rngtech/lang/en_us.json";
    private static final Pattern ID = Pattern.compile("[a-z0-9_]+");
    private static final Map<String, UniqueDefinition> CATALOG = load();

    /** The result of reading a pack's Unique folder: the files that loaded, and one error per skipped file. */
    public record ExternalUniques(List<UniqueDefinition> loaded, List<String> errors) {
    }

    private UniqueCatalog() {
    }

    public static UniqueDefinition get(String id) {
        return id == null ? null : CATALOG.get(path(id));
    }

    public static Collection<UniqueDefinition> all() {
        return Collections.unmodifiableCollection(CATALOG.values());
    }

    /** Whether {@code id} came from a pack's config folder rather than the mod. */
    public static boolean isExternal(String id) {
        return EXTERNAL.contains(path(id));
    }

    /** Drops a pack Unique that cannot register, such as one whose id an RNGTech item already uses. */
    public static void rejectExternal(String id, String reason) {
        if (EXTERNAL.remove(path(id)) && CATALOG.remove(path(id)) != null) {
            LOGGER.error("Skipped pack Unique {}: {}", id, reason);
        }
    }

    /**
     * Reads every {@code *.json} in {@code directory} in file-name order. A file that fails to parse, breaks a catalog rule,
     * uses a Battery Cell host, or reuses an id in {@code taken} is skipped; its error says why.
     */
    public static ExternalUniques loadExternal(Path directory, Set<String> taken) {
        List<UniqueDefinition> loaded = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        if (directory == null || !Files.isDirectory(directory)) {
            return new ExternalUniques(loaded, errors);
        }
        List<Path> files;
        try (Stream<Path> listing = Files.list(directory)) {
            files = listing.filter(file -> file.getFileName().toString().endsWith(".json")).sorted().toList();
        } catch (IOException exception) {
            errors.add(directory + ": cannot list files: " + exception.getMessage());
            return new ExternalUniques(loaded, errors);
        }
        Set<String> used = new HashSet<>(taken);
        for (Path file : files) {
            String name = file.getFileName().toString();
            try {
                JsonObject raw = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                List<String> violations = new ArrayList<>();
                UniqueDefinition definition = read(raw, violations);
                if (definition != null) {
                    violations.addAll(violations(definition));
                    if (definition.host() == UniqueHost.BATTERY_CELL) {
                        violations.add("pack Uniques cannot be Battery Cells, which need a code-side material");
                    }
                    if (used.contains(definition.id())) {
                        violations.add("the id " + definition.id() + " is already a Unique");
                    }
                }
                if (violations.isEmpty()) {
                    loaded.add(definition);
                    used.add(definition.id());
                } else {
                    errors.add(name + ": " + violations);
                }
            } catch (IOException | RuntimeException exception) {
                errors.add(name + ": " + exception.getMessage());
            }
        }
        return new ExternalUniques(loaded, errors);
    }

    /** Parses one catalog file, rejecting it with every violation found. */
    public static UniqueDefinition parse(JsonObject raw) {
        List<String> violations = new ArrayList<>();
        UniqueDefinition definition = read(raw, violations);
        if (definition != null) {
            violations.addAll(violations(definition));
        }
        if (!violations.isEmpty()) {
            throw new IllegalStateException("Invalid Unique " + string(raw, "id") + ": " + violations);
        }
        return definition;
    }

    /** Every violation in one catalog file, including its assets; empty when the file is valid. */
    public static List<String> violations(JsonObject raw) {
        List<String> violations = new ArrayList<>();
        UniqueDefinition definition = read(raw, violations);
        if (definition != null) {
            violations.addAll(violations(definition));
            violations.addAll(assetViolations(definition, language()));
        }
        return violations;
    }

    /** Stat, stage, and yield rules every Unique must follow. */
    public static List<String> violations(UniqueDefinition definition) {
        List<String> violations = new ArrayList<>();
        UniqueHost host = definition.host();
        if (!ID.matcher(definition.id()).matches()) {
            violations.add("the id must use lowercase letters, digits, and underscores");
        }
        if (definition.slotStage() < host.minStage() || definition.slotStage() > host.maxStage()) {
            violations.add("slot stage " + definition.slotStage() + " is outside " + host.serializedName() + " stages "
                    + host.minStage() + "-" + host.maxStage());
        }
        if (!ComponentBaseStatCatalog.hasUniqueBase(definition) && !definition.baseProfile().isEmpty()) {
            violations.add("unknown base profile " + definition.baseProfile());
        }
        if (host == UniqueHost.BATTERY_CELL && UniqueItems.batteryCellMaterial(definition.id()) == null) {
            violations.add("a Unique Battery Cell needs a Unique BatteryCellMaterial with item id " + definition.id());
        }
        for (UniqueStatLine line : definition.lines()) {
            violations.addAll(lineViolations(definition, line));
        }
        for (MachineBehavior behavior : definition.behaviors()) {
            if (!UniqueStatReaders.readByHost(host, behavior)) {
                violations.add("behavior " + behavior.getSerializedName() + " is read by no " + host.serializedName() + " host");
            }
        }
        if (violations.isEmpty()) {
            violations.addAll(gateViolations(definition));
        }
        return violations;
    }

    private static List<String> lineViolations(UniqueDefinition definition, UniqueStatLine line) {
        List<String> violations = new ArrayList<>();
        String name = line.stat().getSerializedName();
        if (!UniqueStatReaders.readByHost(definition.host(), line.stat())) {
            violations.add(name + " is read by no " + definition.host().serializedName() + " host");
        }
        if (line.wholeNumber() && (line.worst() != Math.rint(line.worst()) || line.best() != Math.rint(line.best()))) {
            violations.add(name + " is an integer stat and needs whole-number bounds");
        }
        switch (line.role()) {
            case SIGNATURE, HOOK -> {
                if (!line.isBenefit(line.worst())) {
                    violations.add(name + " must still be a benefit at its worst roll");
                }
            }
            case DRAWBACK -> {
                if (!line.isPenalty(line.best())) {
                    violations.add(name + " must still be a penalty at its best roll");
                }
            }
            case IDENTITY -> {
            }
        }
        if (line.role() == UniqueStatLine.Role.HOOK) {
            violations.addAll(hookViolations(definition, line));
        } else if (!line.ascendancy().isEmpty()) {
            violations.add(name + " names an ascendancy but is not a hook");
        }
        if (isYield(line.stat()) && !(line.role() == UniqueStatLine.Role.DRAWBACK && line.isPenalty(line.best()))) {
            violations.add(name + " is a yield stat; Uniques may only lower yield");
        }
        return violations;
    }

    private static List<String> hookViolations(UniqueDefinition definition, UniqueStatLine line) {
        Ascendancy ascendancy = AscendancyCatalog.get(line.ascendancy());
        if (ascendancy == null) {
            return List.of(line.stat().getSerializedName() + " hooks unknown ascendancy " + line.ascendancy());
        }
        UniqueStatReaders.Reader reader = reader(ascendancy.family());
        if (!UniqueStatReaders.readers(definition.host()).contains(reader) || !UniqueStatReaders.reads(reader, line.stat())) {
            return List.of(line.stat().getSerializedName() + " is not read by " + ascendancy.id() + "'s machine");
        }
        return List.of();
    }

    /** A recipe-gating stat may reach at most one stage past the slot stage at its best roll. */
    private static List<String> gateViolations(UniqueDefinition definition) {
        List<String> violations = new ArrayList<>();
        if (!ComponentBaseStatCatalog.hasUniqueBase(definition) && definition.host() != UniqueHost.BATTERY_CELL
                && definition.lines().isEmpty()) {
            violations.add("a Unique without a base profile must list its stats");
        }
        for (MachineStat stat : UniqueStatReaders.recipeGates(definition.host())) {
            double best = ComponentBaseStatCatalog.uniqueStats(definition, definition.rollModifiers(bestValues(definition))).baseValue(stat);
            double limit = ComponentBaseStatCatalog.normalMaximum(definition.host(), stat, definition.slotStage() + 1);
            if (best > limit + 1.0E-9) {
                violations.add(stat.getSerializedName() + " reaches " + best + " at its best roll, past the stage "
                        + (definition.slotStage() + 1) + " limit of " + limit);
            }
        }
        return violations;
    }

    /** Missing language keys, item texture, or default loot table. */
    public static List<String> assetViolations(UniqueDefinition definition, Set<String> languageKeys) {
        List<String> violations = new ArrayList<>();
        for (String key : List.of(definition.translationKey(), definition.descriptionKey(), definition.sourceKey())) {
            if (!languageKeys.contains(key)) {
                violations.add("missing language key " + key);
            }
        }
        violations.addAll(textureViolations(definition));
        if (!exists(DIRECTORY.replace("uniques/", "loot_table/uniques/") + definition.id() + ".json")) {
            violations.add("missing default loot table loot_table/uniques/" + definition.id() + ".json");
        }
        return violations;
    }

    /** The item model must exist, and every RNGTech texture it names must too; vanilla textures are trusted. */
    private static List<String> textureViolations(UniqueDefinition definition) {
        JsonObject model = resource("/assets/rngtech/models/item/" + definition.id() + ".json");
        if (model == null || !model.has("textures")) {
            return List.of("missing item model models/item/" + definition.id() + ".json");
        }
        List<String> violations = new ArrayList<>();
        for (Map.Entry<String, JsonElement> texture : model.getAsJsonObject("textures").entrySet()) {
            String name = texture.getValue().getAsString();
            if (!name.startsWith("minecraft:") && !exists("/assets/rngtech/textures/" + path(name) + ".png")) {
                violations.add("missing texture " + name);
            }
        }
        return violations;
    }

    private static List<Double> bestValues(UniqueDefinition definition) {
        return definition.lines().stream().map(UniqueStatLine::best).toList();
    }

    private static boolean isYield(MachineStat stat) {
        return UniqueStatReaders.yieldStat(stat) || MasteryDeclarations.yield(stat) != MasteryDeclarations.Yield.NONE;
    }

    private static UniqueStatReaders.Reader reader(MachineMasteryFamily family) {
        return switch (family) {
            case CRUSHER -> UniqueStatReaders.Reader.CRUSHER;
            case FURNACE -> UniqueStatReaders.Reader.FURNACE;
            case ALLOY_FURNACE -> UniqueStatReaders.Reader.ALLOY_FURNACE;
            case METAL_PRESS -> UniqueStatReaders.Reader.METAL_PRESS;
            case MELTER -> UniqueStatReaders.Reader.MELTER;
            case RESONANCE_CALIBRATOR -> UniqueStatReaders.Reader.RESONANCE_CALIBRATOR;
            case FORESTRY -> UniqueStatReaders.Reader.FORESTRY_CART;
        };
    }

    /** Reads a catalog file; unknown hosts, stats, behaviors, or malformed lines become violations. */
    private static UniqueDefinition read(JsonObject raw, List<String> violations) {
        String id = path(string(raw, "id"));
        if (id.contains(":")) {
            violations.add("Uniques register under rngtech; use a bare id or rngtech:<id>, such as rngtech:mypack_ember_core");
            return null;
        }
        if (raw.has("version") && raw.get("version").getAsInt() > FORMAT_VERSION) {
            violations.add("format version " + raw.get("version").getAsInt() + " is newer than this RNGTech reads (" + FORMAT_VERSION + ")");
            return null;
        }
        UniqueHost host = UniqueHost.byName(string(raw, "host"));
        if (host == null) {
            violations.add("unknown host " + string(raw, "host"));
            return null;
        }
        String baseProfile = "";
        if (raw.has("base_profile")) {
            String profile = path(raw.get("base_profile").getAsString());
            String suffix = "_" + host.serializedName();
            if (profile.endsWith(suffix)) {
                baseProfile = profile.substring(0, profile.length() - suffix.length());
            } else {
                violations.add("base profile " + profile + " is not a " + host.serializedName());
            }
        }
        List<UniqueStatLine> lines = new ArrayList<>();
        JsonObject stats = raw.has("stats") ? raw.getAsJsonObject("stats") : new JsonObject();
        for (Map.Entry<String, JsonElement> entry : stats.entrySet()) {
            UniqueStatLine line = readLine(entry.getKey(), entry.getValue().getAsJsonObject(), violations);
            if (line != null) {
                lines.add(line);
            }
        }
        List<MachineBehavior> behaviors = new ArrayList<>();
        if (raw.has("behaviors")) {
            for (JsonElement element : raw.getAsJsonArray("behaviors")) {
                MachineBehavior behavior = behavior(path(element.getAsString()));
                if (behavior == null) {
                    violations.add("unknown behavior " + element.getAsString());
                } else {
                    behaviors.add(behavior);
                }
            }
        }
        String source = string(raw, "source").replace(':', '.');
        return new UniqueDefinition(id, host, raw.has("slot_stage") ? raw.get("slot_stage").getAsInt() : -1, baseProfile, lines,
                behaviors, source);
    }

    private static UniqueStatLine readLine(String key, JsonObject raw, List<String> violations) {
        MachineStat stat = MachineStat.bySerializedName(path(key));
        if (stat == null) {
            violations.add("unknown stat " + key);
            return null;
        }
        UniqueStatLine.Operation operation;
        try {
            operation = UniqueStatLine.Operation.valueOf(string(raw, "operation").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            violations.add(key + ": unknown operation " + string(raw, "operation"));
            return null;
        }
        UniqueStatLine.Role role;
        try {
            role = UniqueStatLine.Role.valueOf(string(raw, "role").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            violations.add(key + ": unknown role " + string(raw, "role"));
            return null;
        }
        double worst;
        double best;
        if (raw.has("value") && !raw.has("min") && !raw.has("max")) {
            worst = raw.get("value").getAsDouble();
            best = worst;
        } else if (raw.has("min") && raw.has("max") && !raw.has("value")) {
            worst = raw.get("min").getAsDouble();
            best = raw.get("max").getAsDouble();
        } else {
            violations.add(key + ": a stat line has either value or min and max");
            return null;
        }
        String ascendancy = raw.has("ascendancy") ? raw.get("ascendancy").getAsString() : "";
        if (role == UniqueStatLine.Role.HOOK && ascendancy.isEmpty()) {
            violations.add(key + ": a hook names its ascendancy");
        }
        return new UniqueStatLine(stat, operation, worst, best, role, ascendancy);
    }

    private static MachineBehavior behavior(String name) {
        for (MachineBehavior behavior : MachineBehavior.values()) {
            if (behavior.getSerializedName().equals(name)) {
                return behavior;
            }
        }
        return null;
    }

    private static Map<String, UniqueDefinition> load() {
        JsonObject index = resource(DIRECTORY + "index.json");
        if (index == null) {
            throw new IllegalStateException("Missing Unique index");
        }
        Set<String> languageKeys = language();
        Map<String, UniqueDefinition> catalog = new LinkedHashMap<>();
        for (JsonElement element : index.getAsJsonArray("uniques")) {
            String file = element.getAsString();
            JsonObject raw = resource(DIRECTORY + file);
            if (raw == null) {
                throw new IllegalStateException("Missing Unique file " + file);
            }
            UniqueDefinition definition = parse(raw);
            List<String> assets = assetViolations(definition, languageKeys);
            if (!assets.isEmpty()) {
                throw new IllegalStateException("Invalid Unique " + definition.id() + ": " + assets);
            }
            if (catalog.put(definition.id(), definition) != null) {
                throw new IllegalStateException("Duplicate Unique " + definition.id());
            }
        }
        ExternalUniques external = loadExternal(packDirectory(), catalog.keySet());
        external.errors().forEach(error -> LOGGER.error("Skipped pack Unique {}", error));
        for (UniqueDefinition definition : external.loaded()) {
            catalog.put(definition.id(), definition);
            EXTERNAL.add(definition.id());
        }
        if (!external.loaded().isEmpty()) {
            LOGGER.info("Loaded {} pack Uniques from {}", external.loaded().size(), packDirectory());
        }
        return catalog;
    }

    /** {@code config/rngtech/uniques}, or null outside a running game, such as in domain checks. */
    private static Path packDirectory() {
        try {
            Path config = FMLPaths.CONFIGDIR.get();
            return config == null ? null : config.resolve("rngtech").resolve("uniques");
        } catch (RuntimeException | LinkageError exception) {
            return null;
        }
    }

    private static Set<String> language() {
        JsonObject language = resource(LANGUAGE);
        return language == null ? Set.of() : Set.copyOf(language.keySet());
    }

    private static String path(String id) {
        if (id == null) {
            return "";
        }
        int colon = id.indexOf(':');
        if (colon < 0) {
            return id;
        }
        return id.substring(0, colon).equals(NAMESPACE) ? id.substring(colon + 1) : id;
    }

    private static String string(JsonObject raw, String key) {
        return raw.has(key) ? raw.get(key).getAsString() : "";
    }

    private static boolean exists(String path) {
        try (InputStream stream = UniqueCatalog.class.getResourceAsStream(path)) {
            return stream != null;
        } catch (IOException exception) {
            return false;
        }
    }

    private static JsonObject resource(String path) {
        try (InputStream stream = UniqueCatalog.class.getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read " + path, exception);
        }
    }
}
