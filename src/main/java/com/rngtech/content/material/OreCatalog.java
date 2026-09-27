package com.rngtech.content.material;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OreCatalog {
    private static final String CATALOG_PATH = "/data/rngtech/materials/ore_catalog.json";
    private static final CatalogData DATA = load();

    public static List<OreDefinition> ores() {
        return DATA.ores();
    }

    public static List<OreDefinition> generatedOres() {
        return DATA.generatedOres();
    }

    public static boolean hasBlockItem(String itemId) {
        return DATA.variantsByBlockId().containsKey(itemId);
    }

    public static OreDefinition definitionByMaterial(String materialId) {
        OreDefinition definition = DATA.oresByMaterial().get(materialId);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown ore material: " + materialId);
        }
        return definition;
    }

    public static OreVariant variantByBlockId(String blockId) {
        OreVariant variant = DATA.variantsByBlockId().get(blockId);
        if (variant == null) {
            throw new IllegalArgumentException("Unknown ore block: " + blockId);
        }
        return variant;
    }

    private static CatalogData load() {
        try (InputStream stream = OreCatalog.class.getResourceAsStream(CATALOG_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Missing ore catalog resource: " + CATALOG_PATH);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                List<OreDefinition> ores = parseOres(requiredArray(root, "ores"));
                return new CatalogData(ores);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load ore catalog", exception);
        }
    }

    private static List<OreDefinition> parseOres(JsonArray ores) {
        List<OreDefinition> parsed = new ArrayList<>();
        for (JsonElement element : ores) {
            JsonObject object = element.getAsJsonObject();
            String materialId = requiredString(object, "material");
            MaterialFamily material = MaterialCatalog.materialFamily(materialId);
            List<OreHost> hosts = optionalHosts(object, "hosts", List.of(OreHost.STONE, OreHost.DEEPSLATE));
            List<OreHost> generationHosts = optionalHosts(object, "generation_hosts", hosts);
            parsed.add(new OreDefinition(
                    materialId,
                    material.displayName(),
                    material.stage(),
                    requiredInt(object, "hardness_level"),
                    hosts,
                    generationHosts,
                    requiredBoolean(object, "generated_by_default"),
                    requiredInt(object, "vein_size"),
                    requiredInt(object, "veins_per_chunk"),
                    requiredInt(object, "min_y"),
                    requiredInt(object, "max_y"),
                    optionalString(object, "biomes", "#minecraft:is_overworld"),
                    requiredDouble(object, "discard_chance_on_air_exposure")
            ));
        }
        return List.copyOf(parsed);
    }

    private static List<OreHost> optionalHosts(JsonObject object, String member, List<OreHost> fallback) {
        JsonElement element = object.get(member);
        if (element == null) {
            return List.copyOf(fallback);
        }
        if (!element.isJsonArray()) {
            throw new IllegalStateException("Ore catalog member must be an array: " + member);
        }
        List<OreHost> hosts = new ArrayList<>();
        for (JsonElement host : element.getAsJsonArray()) {
            hosts.add(OreHost.byId(host.getAsString()));
        }
        return List.copyOf(hosts);
    }

    private static JsonArray requiredArray(JsonObject object, String member) {
        JsonElement element = object.get(member);
        if (element == null || !element.isJsonArray()) {
            throw new IllegalStateException("Ore catalog member must be an array: " + member);
        }
        return element.getAsJsonArray();
    }

    private static String requiredString(JsonObject object, String member) {
        JsonElement element = object.get(member);
        if (element == null || !element.isJsonPrimitive()) {
            throw new IllegalStateException("Ore catalog member must be a string: " + member);
        }
        return element.getAsString();
    }

    private static String optionalString(JsonObject object, String member, String fallback) {
        JsonElement element = object.get(member);
        if (element == null) {
            return fallback;
        }
        if (!element.isJsonPrimitive()) {
            throw new IllegalStateException("Ore catalog member must be a string: " + member);
        }
        return element.getAsString();
    }

    private static int requiredInt(JsonObject object, String member) {
        JsonElement element = object.get(member);
        if (element == null || !element.isJsonPrimitive()) {
            throw new IllegalStateException("Ore catalog member must be a number: " + member);
        }
        return element.getAsInt();
    }

    private static double requiredDouble(JsonObject object, String member) {
        JsonElement element = object.get(member);
        if (element == null || !element.isJsonPrimitive()) {
            throw new IllegalStateException("Ore catalog member must be a number: " + member);
        }
        return element.getAsDouble();
    }

    private static boolean requiredBoolean(JsonObject object, String member) {
        JsonElement element = object.get(member);
        if (element == null || !element.isJsonPrimitive()) {
            throw new IllegalStateException("Ore catalog member must be a boolean: " + member);
        }
        return element.getAsBoolean();
    }

    public record OreVariant(OreDefinition definition, OreHost host) {
        public String blockId() {
            return definition.blockId(host);
        }
    }

    private record CatalogData(
            List<OreDefinition> ores,
            List<OreDefinition> generatedOres,
            Map<String, OreDefinition> oresByMaterial,
            Map<String, OreVariant> variantsByBlockId
    ) {
        private CatalogData(List<OreDefinition> ores) {
            this(ores, generatedOresList(ores), indexMaterials(ores), indexVariants(ores));
        }
    }

    private static List<OreDefinition> generatedOresList(List<OreDefinition> ores) {
        List<OreDefinition> generated = new ArrayList<>();
        for (OreDefinition ore : ores) {
            if (ore.hasDefaultGeneration()) {
                generated.add(ore);
            }
        }
        return List.copyOf(generated);
    }

    private static Map<String, OreDefinition> indexMaterials(List<OreDefinition> ores) {
        Map<String, OreDefinition> indexed = new LinkedHashMap<>();
        for (OreDefinition ore : ores) {
            OreDefinition previous = indexed.put(ore.materialId(), ore);
            if (previous != null) {
                throw new IllegalStateException("Duplicate ore material in catalog: " + ore.materialId());
            }
        }
        return Collections.unmodifiableMap(indexed);
    }

    private static Map<String, OreVariant> indexVariants(List<OreDefinition> ores) {
        Map<String, OreVariant> indexed = new LinkedHashMap<>();
        for (OreDefinition ore : ores) {
            for (OreHost host : ore.hosts()) {
                OreVariant variant = new OreVariant(ore, host);
                OreVariant previous = indexed.put(variant.blockId(), variant);
                if (previous != null) {
                    throw new IllegalStateException("Duplicate ore block in catalog: " + variant.blockId());
                }
            }
        }
        return Collections.unmodifiableMap(indexed);
    }

    private OreCatalog() {
    }
}
