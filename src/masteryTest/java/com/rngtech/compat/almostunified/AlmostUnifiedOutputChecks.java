package com.rngtech.compat.almostunified;

import com.rngtech.content.material.MaterialCatalog;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The AlmostUnified plugin keeps RNGTech-tagged outputs only in recipes of RNGTech recipe types. A vanilla-type recipe
 * that makes an RNGTech alloy, plate, gear, or rod would have its output swapped for another mod's item, which no
 * RNGTech recipe accepts. FTB Evolution turned Bronze Ingots into FTB Materials Bronze Nuggets this way.
 */
public final class AlmostUnifiedOutputChecks {
    private static final Path RECIPES = Path.of("src/main/resources/data/rngtech/recipe");
    private static final String PREFIX = "rngtech:";
    private static int checks;

    private AlmostUnifiedOutputChecks() {
    }

    public static int run() {
        checks = 0;
        List<String> unprotected = new ArrayList<>();
        int scanned = 0;
        try (Stream<Path> files = Files.walk(projectRoot().resolve(RECIPES))) {
            for (Path path : files.filter(file -> file.toString().endsWith(".json")).sorted().toList()) {
                JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                if (!json.has("type") || json.get("type").getAsString().startsWith(PREFIX)) {
                    continue;
                }
                scanned++;
                for (String key : List.of("result", "results", "output", "outputs")) {
                    collectTagged(json.get(key), path.getFileName().toString(), unprotected);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read RNGTech recipes", exception);
        }
        require(scanned > 0, "found vanilla-type RNGTech recipes");
        require(unprotected.isEmpty(), "vanilla-type recipes output RNGTech-tagged items that AlmostUnified would replace; "
                + "use an rngtech: recipe type: " + unprotected);
        return checks;
    }

    private static void collectTagged(JsonElement element, String file, List<String> found) {
        if (element == null) {
            return;
        }
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collectTagged(child, file, found));
        } else if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                JsonElement value = entry.getValue();
                if ((entry.getKey().equals("id") || entry.getKey().equals("item")) && value.isJsonPrimitive()) {
                    String id = value.getAsString();
                    if (id.startsWith(PREFIX) && MaterialCatalog.isRngtechTagged(id.substring(PREFIX.length()))) {
                        found.add(file + " -> " + id);
                    }
                } else {
                    collectTagged(value, file, found);
                }
            }
        }
    }

    private static Path projectRoot() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            if (Files.exists(dir.resolve("settings.gradle"))) {
                return dir;
            }
        }
        throw new IllegalStateException("No project root above " + Path.of("").toAbsolutePath());
    }

    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError(label);
        }
    }
}
