package com.rngtech.content.workorder;

import com.rngtech.RNGTech;
import com.rngtech.rpg.workorder.WorkOrderTemplate;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Job templates from datapacks under {@code data/<namespace>/work_orders/templates/}. A file that fails validation, or
 * names a reward item that does not exist, is skipped with an error; the others still load. Objective ids are checked
 * when a template rolls, since objectives load beside templates.
 */
public final class WorkOrderTemplateCatalog {
    private static volatile Map<ResourceLocation, WorkOrderTemplate> templates = Map.of();

    private WorkOrderTemplateCatalog() {
    }

    public static Collection<WorkOrderTemplate> all() {
        return templates.values();
    }

    /** The template {@code id}, or null. */
    public static WorkOrderTemplate get(ResourceLocation id) {
        return id == null ? null : templates.get(id);
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new Listener());
    }

    private static final class Listener extends SimpleJsonResourceReloadListener {
        private Listener() {
            super(new Gson(), "work_orders/templates");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<ResourceLocation, WorkOrderTemplate> loaded = new LinkedHashMap<>();
            files.forEach((id, json) -> {
                try {
                    WorkOrderTemplate template = WorkOrderTemplate.parse(id, json.getAsJsonObject());
                    for (WorkOrderTemplate.Reward reward : template.rewards()) {
                        if (!BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(reward.item()))) {
                            throw new IllegalArgumentException(id + ": unknown reward item " + reward.item());
                        }
                    }
                    loaded.put(id, template);
                } catch (RuntimeException exception) {
                    RNGTech.LOGGER.error("Skipping invalid Work Order template {}", id, exception);
                }
            });
            templates = Map.copyOf(loaded);
        }
    }
}
