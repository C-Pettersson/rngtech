package com.rngtech.benchmark;

import com.rngtech.content.blockentity.CableBlockEntity;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Builds {@link BenchmarkScene}, lets it settle, then times the server tick for a fixed number of ticks and writes the
 * result. It is registered only on the {@code tickBenchmark} run, never on {@code gameTestServer} or in the mod jar.
 */
@GameTestHolder("rngtech")
public final class TickBenchmark {
    private static final int SETTLE_TICKS = Integer.getInteger("rngtech.benchmark.settle", 100);
    private static final int WARMUP_TICKS = Integer.getInteger("rngtech.benchmark.warmup", 200);
    private static final int MEASURED_TICKS = Integer.getInteger("rngtech.benchmark.ticks", 400);
    /** Working machines are topped up this often, in ticks. */
    private static final int SERVICE_INTERVAL = 20;

    private TickBenchmark() {
    }

    @GameTest(template = "scene", timeoutTicks = 100_000)
    public static void scene(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO).offset(4, 0, 4);
        BenchmarkScene scene = new BenchmarkScene(helper.getLevel(), origin);
        long buildStart = System.nanoTime();
        scene.build();
        double buildSeconds = (System.nanoTime() - buildStart) / 1e9;

        Probe probe = new Probe(scene, SETTLE_TICKS + WARMUP_TICKS, MEASURED_TICKS);
        NeoForge.EVENT_BUS.register(probe);
        helper.succeedWhen(() -> {
            helper.assertTrue(probe.finished(), "measuring tick " + probe.tick() + " of " + (SETTLE_TICKS + WARMUP_TICKS + MEASURED_TICKS));
            NeoForge.EVENT_BUS.unregister(probe);
            report(scene, probe, buildSeconds);
        });
    }

    private static void report(BenchmarkScene scene, Probe probe, double buildSeconds) {
        double[] ms = probe.millis();
        double[] sorted = ms.clone();
        Arrays.sort(sorted);
        JsonObject result = new JsonObject();
        result.addProperty("ticks", ms.length);
        result.addProperty("mean_ms", mean(ms));
        result.addProperty("median_ms", percentile(sorted, 0.50));
        result.addProperty("p95_ms", percentile(sorted, 0.95));
        result.addProperty("p99_ms", percentile(sorted, 0.99));
        result.addProperty("max_ms", sorted[sorted.length - 1]);
        result.addProperty("gc_ms", probe.gcMillis());
        result.addProperty("build_seconds", buildSeconds);
        JsonObject sceneInfo = new JsonObject();
        BenchmarkScene.Counts counts = scene.counts();
        sceneInfo.addProperty("cables", counts.cables);
        sceneInfo.addProperty("connectors", counts.connectors);
        sceneInfo.addProperty("working_crushers", counts.workingCrushers);
        sceneInfo.addProperty("idle_machines", counts.idleMachines);
        sceneInfo.addProperty("debug_batteries", counts.batteries);
        sceneInfo.addProperty("solar_controllers", counts.solarControllers);
        sceneInfo.addProperty("solar_panels", counts.solarPanels);
        CableBlockEntity.NetworkDebugSnapshot network = scene.networkSnapshot();
        sceneInfo.addProperty("network_cable_nodes", network.cableNodes());
        sceneInfo.addProperty("network_energy_endpoints", network.energyEndpoints());
        sceneInfo.addProperty("network_universal_connectors", network.universalConnectors());
        sceneInfo.addProperty("network_energy_modules", network.energyModules());
        sceneInfo.addProperty("network_energy_transfer_cap", network.energyTransferCap());
        sceneInfo.addProperty("working_crusher_energy", scene.workingEnergy());
        sceneInfo.addProperty("items_produced", scene.itemsProduced());
        result.add("scene", sceneInfo);

        String json = new GsonBuilder().setPrettyPrinting().create().toJson(result);
        System.out.println("RNGTech tick benchmark result:\n" + json);
        System.out.printf(
                "RNGTech tick benchmark: mean %.3f ms/tick, median %.3f, p95 %.3f, max %.3f over %d ticks%n",
                mean(ms), percentile(sorted, 0.50), percentile(sorted, 0.95), sorted[sorted.length - 1], ms.length
        );
        String output = System.getProperty("rngtech.benchmark.output");
        if (output != null) {
            try {
                Path path = Path.of(output);
                Files.createDirectories(path.getParent());
                Files.writeString(path, json + System.lineSeparator());
            } catch (IOException e) {
                throw new IllegalStateException("Could not write the benchmark result to " + output, e);
            }
        }
    }

    private static double mean(double[] values) {
        return Arrays.stream(values).average().orElse(0);
    }

    private static double percentile(double[] sorted, double fraction) {
        return sorted[(int) Math.min(sorted.length - 1, Math.floor(sorted.length * fraction))];
    }

    /** Times each server tick between the tick events once the scene has settled, and keeps the working machines fed. */
    public static final class Probe {
        private final BenchmarkScene scene;
        private final int skipTicks;
        private final double[] millis;
        private int tick;
        private int recorded;
        private long started;
        private long gcStart = -1;
        private long gcEnd;

        Probe(BenchmarkScene scene, int skipTicks, int measuredTicks) {
            this.scene = scene;
            this.skipTicks = skipTicks;
            this.millis = new double[measuredTicks];
        }

        boolean finished() {
            return recorded >= millis.length;
        }

        int tick() {
            return tick;
        }

        double[] millis() {
            return millis;
        }

        long gcMillis() {
            return gcEnd - gcStart;
        }

        @SubscribeEvent
        public void onTickStart(ServerTickEvent.Pre event) {
            started = System.nanoTime();
        }

        @SubscribeEvent
        public void onTickEnd(ServerTickEvent.Post event) {
            long elapsed = System.nanoTime() - started;
            tick++;
            if (tick % SERVICE_INTERVAL == 0) {
                scene.service();
            }
            if (tick > skipTicks && recorded < millis.length) {
                if (gcStart < 0) {
                    gcStart = gcMillisNow();
                }
                millis[recorded++] = elapsed / 1e6;
                gcEnd = gcMillisNow();
            }
        }

        private static long gcMillisNow() {
            return ManagementFactory.getGarbageCollectorMXBeans().stream()
                    .mapToLong(GarbageCollectorMXBean::getCollectionTime)
                    .filter(time -> time >= 0)
                    .sum();
        }
    }
}
