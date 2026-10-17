package com.rngtech.content.dormant;

import com.rngtech.RNGTech;
import com.rngtech.rpg.dormant.DormantSite;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dormant Machine site types from datapacks under {@code data/<namespace>/dormant_machine/}. A site file that fails
 * validation is skipped with an error; the others still load.
 */
public final class DormantSiteCatalog {
    public static final ResourceLocation DEFAULT_SITE = RNGTech.id("collapsed_workshop");
    private static volatile Map<ResourceLocation, DormantSite> sites = Map.of();

    private DormantSiteCatalog() {
    }

    public static DormantSite get(ResourceLocation id) {
        return id == null ? null : sites.get(id);
    }

    public static Map<ResourceLocation, DormantSite> all() {
        return sites;
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new Listener());
    }

    private static final class Listener extends SimpleJsonResourceReloadListener {
        private Listener() {
            super(new Gson(), "dormant_machine");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<ResourceLocation, DormantSite> loaded = new LinkedHashMap<>();
            files.forEach((id, json) -> {
                try {
                    loaded.put(id, DormantSite.parse(id.toString(), json.getAsJsonObject()));
                } catch (RuntimeException exception) {
                    RNGTech.LOGGER.error("Skipping invalid Dormant Machine site {}", id, exception);
                }
            });
            sites = Map.copyOf(loaded);
        }
    }
}
