package com.rngtech.content.workorder;

import com.rngtech.RNGTech;
import com.rngtech.rpg.workorder.WorkObjectiveEntry;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Work Order objectives from datapacks under {@code data/<namespace>/work_orders/objectives/}. A file that fails
 * validation is skipped with an error; the others still load.
 */
public final class WorkObjectiveCatalog {
    private static volatile List<WorkObjectiveEntry> entries = List.of();

    private WorkObjectiveCatalog() {
    }

    public static List<WorkObjectiveEntry> all() {
        return entries;
    }

    /** The loaded objectives by id, which templates name. */
    public static Map<ResourceLocation, WorkObjectiveEntry> byId() {
        Map<ResourceLocation, WorkObjectiveEntry> byId = new HashMap<>();
        entries.forEach(entry -> byId.put(entry.id(), entry));
        return byId;
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new Listener());
    }

    private static final class Listener extends SimpleJsonResourceReloadListener {
        private Listener() {
            super(new Gson(), "work_orders/objectives");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
            List<WorkObjectiveEntry> loaded = new ArrayList<>();
            files.forEach((id, json) -> {
                try {
                    loaded.add(WorkObjectiveEntry.parse(id, json.getAsJsonObject()));
                } catch (RuntimeException exception) {
                    RNGTech.LOGGER.error("Skipping invalid Work Order objective {}", id, exception);
                }
            });
            entries = List.copyOf(loaded);
        }
    }
}
