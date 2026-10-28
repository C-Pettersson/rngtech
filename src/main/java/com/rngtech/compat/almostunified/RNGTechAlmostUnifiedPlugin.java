package com.rngtech.compat.almostunified;

import com.rngtech.RNGTech;
import com.rngtech.content.material.MaterialCatalog;

import com.almostreliable.unified.api.constant.RecipeConstants;
import com.almostreliable.unified.api.plugin.AlmostUnifiedNeoPlugin;
import com.almostreliable.unified.api.plugin.AlmostUnifiedPlugin;
import com.almostreliable.unified.api.unification.bundled.GenericRecipeUnifier;
import com.almostreliable.unified.api.unification.bundled.ShapedRecipeUnifier;
import com.almostreliable.unified.api.unification.recipe.RecipeJson;
import com.almostreliable.unified.api.unification.recipe.RecipeUnifier;
import com.almostreliable.unified.api.unification.recipe.RecipeUnifierRegistry;
import com.almostreliable.unified.api.unification.recipe.UnificationHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Keeps RNGTech's own plates, gears, rods, and alloys in RNGTech recipe outputs. RNGTech recipes take those items through
 * RNGTech-only tags, so swapping a Metal Press or Alloy Furnace output for another mod's item would leave nothing that
 * RNGTech recipes accept.
 */
@AlmostUnifiedNeoPlugin
public final class RNGTechAlmostUnifiedPlugin implements AlmostUnifiedPlugin {
    private static final String RNGTECH_PREFIX = RNGTech.MOD_ID + ":";

    @Override
    public ResourceLocation getPluginId() {
        return RNGTech.id("almostunified");
    }

    @Override
    public void registerRecipeUnifiers(RecipeUnifierRegistry registry) {
        registry.registerForModId(RNGTech.MOD_ID, new KeepTaggedOutputsUnifier());
    }

    private static final class KeepTaggedOutputsUnifier implements RecipeUnifier {
        @Override
        public void unify(UnificationHelper helper, RecipeJson recipe) {
            Map<String, JsonElement> outputs = new LinkedHashMap<>();
            for (String key : RecipeConstants.DEFAULT_OUTPUT_KEYS) {
                JsonElement output = recipe.getProperty(key);
                if (output != null) {
                    outputs.put(key, output.deepCopy());
                }
            }
            RecipeUnifier defaultUnifier = ShapedRecipeUnifier.isApplicable(recipe)
                    ? ShapedRecipeUnifier.INSTANCE
                    : GenericRecipeUnifier.INSTANCE;
            defaultUnifier.unify(helper, recipe);
            outputs.forEach((key, original) -> restoreTaggedItems(original, recipe.getProperty(key)));
        }

        private static void restoreTaggedItems(JsonElement original, JsonElement unified) {
            if (original instanceof JsonObject originalObject && unified instanceof JsonObject unifiedObject) {
                for (Map.Entry<String, JsonElement> entry : originalObject.entrySet()) {
                    if (isTaggedItemId(entry.getKey(), entry.getValue())) {
                        unifiedObject.add(entry.getKey(), entry.getValue());
                    } else if (unifiedObject.has(entry.getKey())) {
                        restoreTaggedItems(entry.getValue(), unifiedObject.get(entry.getKey()));
                    }
                }
            } else if (original instanceof JsonArray originalArray && unified instanceof JsonArray unifiedArray
                    && originalArray.size() == unifiedArray.size()) {
                for (int index = 0; index < originalArray.size(); index++) {
                    restoreTaggedItems(originalArray.get(index), unifiedArray.get(index));
                }
            }
        }

        private static boolean isTaggedItemId(String key, JsonElement value) {
            if (!RecipeConstants.ID.equals(key) && !RecipeConstants.ITEM.equals(key)) {
                return false;
            }
            if (!(value instanceof JsonPrimitive primitive) || !primitive.isString()) {
                return false;
            }
            String id = primitive.getAsString();
            return id.startsWith(RNGTECH_PREFIX) && MaterialCatalog.isRngtechTagged(id.substring(RNGTECH_PREFIX.length()));
        }
    }
}
