package com.rngtech.rpg;

import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MegaPassiveNode;
import com.rngtech.rpg.progression.MegaPassiveTree;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Every tree allocation in {@code tools/balance/processing-setups.json} is still a legal build on the current passive
 * tree, so a tree change fails here instead of breaking {@code processingBalanceSim} unnoticed.
 */
public final class ProcessingSetupChecks {
    private static final Path SETUPS = Path.of("tools/balance/processing-setups.json");
    private static int checks;

    private ProcessingSetupChecks() {
    }

    public static int run() {
        checks = 0;
        JsonObject setups;
        try {
            setups = JsonParser.parseString(Files.readString(projectRoot().resolve(SETUPS))).getAsJsonObject();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read " + SETUPS, exception);
        }
        JsonObject trees = setups.getAsJsonObject("trees");
        for (Map.Entry<String, JsonElement> archetype : setups.getAsJsonObject("archetypes").entrySet()) {
            for (MachineMasteryFamily family : List.of(MachineMasteryFamily.CRUSHER, MachineMasteryFamily.FURNACE)) {
                String tree = archetype.getValue().getAsJsonObject().getAsJsonObject(family == MachineMasteryFamily.CRUSHER ? "crusher" : "furnace")
                        .get("tree").getAsString();
                require(trees.has(tree), archetype.getKey() + " names a stored tree, not " + tree);
                for (JsonElement levelElement : setups.getAsJsonArray("levels")) {
                    int level = levelElement.getAsInt();
                    String label = tree + " at level " + level;
                    require(trees.getAsJsonObject(tree).has(levelElement.getAsString()), label + " is stored");
                    List<String> nodes = trees.getAsJsonObject(tree).getAsJsonArray(levelElement.getAsString()).asList().stream()
                            .map(JsonElement::getAsString).toList();
                    require(nodes.size() <= level - 1, label + " spends at most " + (level - 1) + " points");
                    require(MegaPassiveTree.validBuild(family.startNodeId(), nodes),
                            label + " is a valid " + family + " build; rerun ./gradlew processingTreeSearch");
                    for (String id : nodes) {
                        MegaPassiveNode node = MegaPassiveTree.node(id);
                        require(!ProcessingTreeSearch.blocksRecipes(node), label + " leaves out the recipe-blocking " + id);
                    }
                }
            }
        }
        return checks;
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
