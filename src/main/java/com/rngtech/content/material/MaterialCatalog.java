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

public final class MaterialCatalog {
    private static final String CATALOG_PATH = "/data/rngtech/materials/catalog.json";
    private static final CatalogData DATA = load();

    public static List<MaterialForm> metalForms() {
        return DATA.metalForms();
    }

    public static List<MaterialFamily> materialFamilies() {
        return DATA.materialFamilies();
    }

    public static List<GenericMaterialForm> genericForms() {
        return DATA.genericForms();
    }

    public static List<NonStageMaterial> nonStageMaterials() {
        return DATA.nonStageMaterials();
    }

    public static List<String> vanillaFormItems(String materialId, String formId) {
        Map<String, List<String>> materialMappings = DATA.vanillaFormMappings().get(materialId);
        if (materialMappings == null) {
            return List.of();
        }
        return materialMappings.getOrDefault(formId, List.of());
    }

    public static boolean hasVanillaFormMapping(String materialId, String formId) {
        return !vanillaFormItems(materialId, formId).isEmpty();
    }

    public static List<MaterialItemDefinition> registeredItems() {
        return DATA.registeredItems();
    }

    public static MaterialFamily materialFamily(String id) {
        MaterialFamily family = DATA.materialsById().get(id);
        if (family == null) {
            throw new IllegalArgumentException("Unknown material family: " + id);
        }
        return family;
    }

    /** The material family's stage, or -1 for an unknown or blank id. */
    public static int materialStage(String id) {
        MaterialFamily family = id == null ? null : DATA.materialsById().get(id);
        return family == null ? -1 : family.stage();
    }

    public static MaterialItemDefinition itemDefinition(String itemId) {
        MaterialItemDefinition definition = DATA.itemsById().get(itemId);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown material item: " + itemId);
        }
        return definition;
    }

    public static boolean hasItemDefinition(String itemId) {
        return DATA.itemsById().containsKey(itemId);
    }

    private static CatalogData load() {
        try (InputStream stream = MaterialCatalog.class.getResourceAsStream(CATALOG_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Missing material catalog resource: " + CATALOG_PATH);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                List<MaterialForm> metalForms = parseMetalForms(requiredArray(root, "metal_forms"));
                Map<String, Map<String, List<String>>> vanillaFormMappings = parseVanillaFormMappings(root);
                List<MaterialFamily> materialFamilies = parseMaterialFamilies(requiredArray(root, "materials"));
                List<GenericMaterialForm> genericForms = parseGenericForms(requiredArray(root, "generic_forms"));
                List<NonStageMaterial> nonStageMaterials = parseNonStageMaterials(requiredArray(root, "non_stage_materials"));
                List<MaterialItemDefinition> registeredItems =
                        buildRegisteredItems(metalForms, vanillaFormMappings, materialFamilies, genericForms, nonStageMaterials);
                return new CatalogData(metalForms, materialFamilies, genericForms, nonStageMaterials, vanillaFormMappings, registeredItems);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load material catalog", exception);
        }
    }

    private static List<MaterialForm> parseMetalForms(JsonArray forms) {
        List<MaterialForm> parsed = new ArrayList<>();
        for (JsonElement element : forms) {
            JsonObject form = element.getAsJsonObject();
            parsed.add(new MaterialForm(
                    requiredString(form, "id"),
                    requiredString(form, "display_pattern"),
                    requiredString(form, "item_pattern"),
                    requiredBoolean(form, "stage_authority")
            ));
        }
        return List.copyOf(parsed);
    }

    private static Map<String, Map<String, List<String>>> parseVanillaFormMappings(JsonObject root) {
        JsonElement mappingsElement = root.get("vanilla_form_mappings");
        if (mappingsElement == null || mappingsElement.isJsonNull()) {
            return Map.of();
        }
        if (!mappingsElement.isJsonObject()) {
            throw new IllegalStateException("Material catalog member must be an object: vanilla_form_mappings");
        }

        Map<String, Map<String, List<String>>> mappings = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> materialEntry : mappingsElement.getAsJsonObject().entrySet()) {
            if (!materialEntry.getValue().isJsonObject()) {
                throw new IllegalStateException("Vanilla form mapping must be an object for material: " + materialEntry.getKey());
            }
            Map<String, List<String>> formMappings = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> formEntry : materialEntry.getValue().getAsJsonObject().entrySet()) {
                if (!formEntry.getValue().isJsonArray()) {
                    throw new IllegalStateException("Vanilla form mapping must be an array for form: " + formEntry.getKey());
                }
                List<String> values = new ArrayList<>();
                for (JsonElement value : formEntry.getValue().getAsJsonArray()) {
                    if (!value.isJsonPrimitive()) {
                        throw new IllegalStateException("Vanilla form mapping values must be strings for form: " + formEntry.getKey());
                    }
                    values.add(value.getAsString());
                }
                formMappings.put(formEntry.getKey(), List.copyOf(values));
            }
            mappings.put(materialEntry.getKey(), Collections.unmodifiableMap(formMappings));
        }
        return Collections.unmodifiableMap(mappings);
    }

    private static List<MaterialFamily> parseMaterialFamilies(JsonArray materials) {
        List<MaterialFamily> parsed = new ArrayList<>();
        for (JsonElement element : materials) {
            JsonObject material = element.getAsJsonObject();
            parsed.add(new MaterialFamily(
                    requiredString(material, "id"),
                    requiredString(material, "display"),
                    requiredInt(material, "stage"),
                    requiredBoolean(material, "default_enabled"),
                    requiredString(material, "default_reason"),
                    optionalStringList(material, "excluded_forms")
            ));
        }
        return List.copyOf(parsed);
    }

    private static List<GenericMaterialForm> parseGenericForms(JsonArray forms) {
        List<GenericMaterialForm> parsed = new ArrayList<>();
        for (JsonElement element : forms) {
            JsonObject form = element.getAsJsonObject();
            parsed.add(new GenericMaterialForm(requiredString(form, "id"), requiredString(form, "display")));
        }
        return List.copyOf(parsed);
    }

    private static List<NonStageMaterial> parseNonStageMaterials(JsonArray materials) {
        List<NonStageMaterial> parsed = new ArrayList<>();
        for (JsonElement element : materials) {
            JsonObject material = element.getAsJsonObject();
            parsed.add(new NonStageMaterial(
                    requiredString(material, "id"),
                    requiredString(material, "display"),
                    requiredString(material, "handling"),
                    optionalString(material, "maps_to"),
                    requiredString(material, "reason")
            ));
        }
        return List.copyOf(parsed);
    }

    private static List<MaterialItemDefinition> buildRegisteredItems(
            List<MaterialForm> metalForms,
            Map<String, Map<String, List<String>>> vanillaFormMappings,
            List<MaterialFamily> materialFamilies,
            List<GenericMaterialForm> genericForms,
            List<NonStageMaterial> nonStageMaterials
    ) {
        List<MaterialItemDefinition> items = new ArrayList<>();
        for (MaterialFamily family : materialFamilies) {
            for (MaterialForm form : metalForms) {
                if (family.excludesForm(form.id())) {
                    continue;
                }
                if (hasVanillaFormMapping(vanillaFormMappings, family.id(), form.id())) {
                    continue;
                }
                items.add(new MaterialItemDefinition(
                        form.itemId(family.id()),
                        form.displayName(family.displayName()),
                        form.texture(family.id()),
                        family.id(),
                        form.id(),
                        MaterialItemKind.METAL_FORM
                ));
            }
        }
        for (GenericMaterialForm form : genericForms) {
            items.add(new MaterialItemDefinition(
                    form.id(),
                    form.displayName(),
                    form.texture(),
                    null,
                    form.id(),
                    MaterialItemKind.GENERIC_FORM
            ));
        }
        for (NonStageMaterial material : nonStageMaterials) {
            if (material.registered()) {
                items.add(new MaterialItemDefinition(
                        material.id(),
                        material.displayName(),
                        material.texture(),
                        null,
                        "non_stage",
                        MaterialItemKind.NON_STAGE
                ));
            }
        }
        return List.copyOf(items);
    }

    private static JsonArray requiredArray(JsonObject object, String member) {
        JsonElement element = object.get(member);
        if (element == null || !element.isJsonArray()) {
            throw new IllegalStateException("Material catalog member must be an array: " + member);
        }
        return element.getAsJsonArray();
    }

    private static String requiredString(JsonObject object, String member) {
        JsonElement element = object.get(member);
        if (element == null || !element.isJsonPrimitive()) {
            throw new IllegalStateException("Material catalog member must be a string: " + member);
        }
        return element.getAsString();
    }

    private static String optionalString(JsonObject object, String member) {
        JsonElement element = object.get(member);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        return element.getAsString();
    }

    private static List<String> optionalStringList(JsonObject object, String member) {
        JsonElement element = object.get(member);
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        if (!element.isJsonArray()) {
            throw new IllegalStateException("Material catalog member must be an array: " + member);
        }
        List<String> values = new ArrayList<>();
        for (JsonElement value : element.getAsJsonArray()) {
            if (!value.isJsonPrimitive()) {
                throw new IllegalStateException("Material catalog member values must be strings: " + member);
            }
            values.add(value.getAsString());
        }
        return List.copyOf(values);
    }

    private static int requiredInt(JsonObject object, String member) {
        JsonElement element = object.get(member);
        if (element == null || !element.isJsonPrimitive()) {
            throw new IllegalStateException("Material catalog member must be a number: " + member);
        }
        return element.getAsInt();
    }

    private static boolean requiredBoolean(JsonObject object, String member) {
        JsonElement element = object.get(member);
        if (element == null || !element.isJsonPrimitive()) {
            throw new IllegalStateException("Material catalog member must be a boolean: " + member);
        }
        return element.getAsBoolean();
    }

    private static boolean hasVanillaFormMapping(
            Map<String, Map<String, List<String>>> vanillaFormMappings,
            String materialId,
            String formId
    ) {
        Map<String, List<String>> materialMappings = vanillaFormMappings.get(materialId);
        return materialMappings != null && !materialMappings.getOrDefault(formId, List.of()).isEmpty();
    }

    private record CatalogData(
            List<MaterialForm> metalForms,
            List<MaterialFamily> materialFamilies,
            List<GenericMaterialForm> genericForms,
            List<NonStageMaterial> nonStageMaterials,
            Map<String, Map<String, List<String>>> vanillaFormMappings,
            List<MaterialItemDefinition> registeredItems,
            Map<String, MaterialFamily> materialsById,
            Map<String, MaterialItemDefinition> itemsById
    ) {
        private CatalogData(
                List<MaterialForm> metalForms,
                List<MaterialFamily> materialFamilies,
                List<GenericMaterialForm> genericForms,
                List<NonStageMaterial> nonStageMaterials,
                Map<String, Map<String, List<String>>> vanillaFormMappings,
                List<MaterialItemDefinition> registeredItems
        ) {
            this(
                    metalForms,
                    materialFamilies,
                    genericForms,
                    nonStageMaterials,
                    vanillaFormMappings,
                    registeredItems,
                    indexMaterials(materialFamilies),
                    indexItems(registeredItems)
            );
        }
    }

    private static Map<String, MaterialFamily> indexMaterials(List<MaterialFamily> materialFamilies) {
        Map<String, MaterialFamily> indexed = new LinkedHashMap<>();
        for (MaterialFamily material : materialFamilies) {
            MaterialFamily previous = indexed.put(material.id(), material);
            if (previous != null) {
                throw new IllegalStateException("Duplicate material id in catalog: " + material.id());
            }
        }
        return Collections.unmodifiableMap(indexed);
    }

    private static Map<String, MaterialItemDefinition> indexItems(List<MaterialItemDefinition> registeredItems) {
        Map<String, MaterialItemDefinition> indexed = new LinkedHashMap<>();
        for (MaterialItemDefinition item : registeredItems) {
            MaterialItemDefinition previous = indexed.put(item.itemId(), item);
            if (previous != null) {
                throw new IllegalStateException("Duplicate material item id in catalog: " + item.itemId());
            }
        }
        return Collections.unmodifiableMap(indexed);
    }

    private MaterialCatalog() {
    }
}
