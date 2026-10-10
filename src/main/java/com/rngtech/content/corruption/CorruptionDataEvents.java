package com.rngtech.content.corruption;

import com.rngtech.RNGTech;
import com.rngtech.content.network.CorruptionCatalogPayload;
import com.rngtech.rpg.corruption.CorruptionCatalog;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads Volatile Catalyst outcome weights and pools from datapacks under {@code data/<namespace>/corruption/} and sends
 * them to clients, so packs can retune corruption. A pack that fails validation keeps the previous catalog.
 */
public final class CorruptionDataEvents {
    private static final ResourceLocation OUTCOMES = RNGTech.id("outcomes");
    private static final String POOL_PREFIX = "pools/";

    private CorruptionDataEvents() {
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new Listener());
    }

    public static void onDatapackSync(OnDatapackSyncEvent event) {
        CorruptionCatalogPayload payload = CorruptionCatalogPayload.of(CorruptionCatalog.active().source());
        if (event.getPlayer() != null) {
            PacketDistributor.sendToPlayer(event.getPlayer(), payload);
        } else {
            PacketDistributor.sendToAllPlayers(payload);
        }
    }

    private static final class Listener extends SimpleJsonResourceReloadListener {
        private Listener() {
            super(new Gson(), "corruption");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
            JsonElement outcomes = files.get(OUTCOMES);
            if (outcomes == null) {
                RNGTech.LOGGER.error("Corruption data has no {}; keeping the previous catalog", OUTCOMES);
                return;
            }
            Map<String, String> pools = new LinkedHashMap<>();
            files.forEach((id, json) -> {
                if (id.getPath().startsWith(POOL_PREFIX)) {
                    pools.put(id.getNamespace() + ":" + id.getPath().substring(POOL_PREFIX.length()), json.toString());
                }
            });
            try {
                CorruptionCatalog.setActive(CorruptionCatalog.parse(new CorruptionCatalog.Source(outcomes.toString(), pools)));
            } catch (IllegalArgumentException exception) {
                RNGTech.LOGGER.error("Invalid corruption data; keeping the previous catalog", exception);
            }
        }
    }
}
