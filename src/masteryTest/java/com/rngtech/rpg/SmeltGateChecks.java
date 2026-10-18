package com.rngtech.rpg;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Every form of a metal (raw, ore, crushed, dust) smelts behind the same heat gate, so no form skips the Furnace stage
 * the metal needs. Raw metals once smelted at 1,000 C whatever the metal.
 */
public final class SmeltGateChecks {
    private static final Path FURNACE_RECIPES = Path.of("src/main/resources/data/rngtech/recipe/furnace");
    private static final List<String> FORMS = List.of("raw_", "ores/", "crushed", "_dust");
    private static int checks;

    private SmeltGateChecks() {
    }

    public static int run() {
        checks = 0;
        Map<String, List<Gate>> byIngot = new TreeMap<>();
        try (Stream<Path> files = Files.walk(projectRoot().resolve(FURNACE_RECIPES))) {
            for (Path path : files.filter(file -> file.toString().endsWith(".json")).toList()) {
                JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                String ingredient = ingredientKey(json.get("ingredient"));
                if (!"rngtech:furnace".equals(json.get("type").getAsString())
                        || FORMS.stream().noneMatch(ingredient::contains)
                        || !json.has("result")) {
                    continue;
                }
                String ingot = json.getAsJsonObject("result").get("id").getAsString();
                if (!ingot.endsWith("_ingot")) {
                    continue;
                }
                byIngot.computeIfAbsent(ingot, key -> new ArrayList<>()).add(new Gate(
                        path.getFileName().toString(),
                        json.has("minimum_temperature") ? json.get("minimum_temperature").getAsInt() : 0,
                        json.has("required_temperature_stability") ? json.get("required_temperature_stability").getAsDouble() : 0.0));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read Furnace recipes", exception);
        }
        require(byIngot.size() >= 10, "found the metal smelts, only " + byIngot.size());
        byIngot.forEach((ingot, gates) -> {
            Gate first = gates.getFirst();
            for (Gate gate : gates) {
                require(gate.minimum() == first.minimum() && gate.stability() == first.stability(),
                        ingot + ": " + gate.file() + " smelts at " + gate.minimum() + " C, stability " + gate.stability()
                                + ", but " + first.file() + " needs " + first.minimum() + " C, stability " + first.stability());
            }
        });
        return checks;
    }

    private static String ingredientKey(JsonElement ingredient) {
        if (ingredient == null) {
            return "";
        }
        JsonObject object = ingredient.isJsonArray() ? ingredient.getAsJsonArray().get(0).getAsJsonObject() : ingredient.getAsJsonObject();
        return object.has("item") ? object.get("item").getAsString() : object.has("tag") ? object.get("tag").getAsString() : "";
    }

    private record Gate(String file, int minimum, double stability) {
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
