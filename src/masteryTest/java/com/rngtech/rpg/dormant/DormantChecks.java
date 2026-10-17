package com.rngtech.rpg.dormant;

import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MegaPassiveNode;
import com.rngtech.rpg.progression.MegaPassiveTree;
import com.rngtech.rpg.progression.PassiveNodeKind;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.TreeMap;
import java.util.stream.Stream;

/** Dormant Machine souls: shipped site data, the wake curve, deterministic rolls, and builds the machine can use. */
public final class DormantChecks {
    private static int checks;

    private DormantChecks() {
    }

    public static int run() {
        checks = 0;
        Map<String, DormantSite> sites = shippedSites();
        shippedSitesMatchTheProposal(sites);
        wakeCurve(sites.get("end_relay"));
        rolls(sites);
        generatedBuilds();
        namedTargets();
        rejectsBadData();
        return checks;
    }

    private static void shippedSitesMatchTheProposal(Map<String, DormantSite> sites) {
        require(sites.keySet().equals(new HashSet<>(List.of("collapsed_workshop", "abandoned_cart_siding", "buried_calibration_vault",
                "flooded_foundry", "nether_smeltworks", "end_relay"))), "the six default sites ship, got " + sites.keySet());
        DormantSite workshop = sites.get("collapsed_workshop");
        require(workshop.minWakeFe() == 35_000 && workshop.maxWakeFe() == 100_000 && workshop.releaseStage() == 1,
                "the Collapsed Workshop is sized for carried cells");
        DormantSite end = sites.get("end_relay");
        require(end.minLevel() == 40 && end.maxLevel() == 70 && end.minWakeFe() == 10_000_000 && end.maxWakeFe() == 60_000_000
                && end.wakeInput() == 250_000 && end.releaseStage() == 7, "the End Relay carries the proposal's numbers");
        for (DormantSite site : sites.values()) {
            require(site.wakeFe(site.maxLevel()) / site.wakeInput() <= 20 * 20, site.id() + " wakes in under 20 seconds at its input rate");
        }
    }

    private static void wakeCurve(DormantSite end) {
        require(end.wakeFe(40) == 10_000_000 && end.wakeFe(70) == 60_000_000, "the band's ends cost the range's ends");
        require(end.wakeFe(55) == 22_500_000, "the middle of the band costs a quarter of the range on the squared curve");
        long previous = 0;
        for (int level = end.minLevel(); level <= end.maxLevel(); level++) {
            require(end.wakeFe(level) >= previous, "a higher soul never costs less to wake");
            previous = end.wakeFe(level);
        }
        require(end.wakeFe(70) - end.wakeFe(55) == 3 * (end.wakeFe(55) - end.wakeFe(40)), "the top half costs three times the bottom half");
    }

    private static void rolls(Map<String, DormantSite> sites) {
        for (DormantSite site : sites.values()) {
            HashSet<Integer> levels = new HashSet<>();
            for (long seed = 0; seed < 24; seed++) {
                DormantSoul soul = DormantSoul.roll(site, seed);
                require(soul.equals(DormantSoul.roll(site, seed)), site.id() + " rolls the same soul from the same seed");
                require(site.families().containsKey(soul.family()), site.id() + " rolls only its own families");
                require(soul.level() >= site.minLevel() && soul.level() <= site.maxLevel(), site.id() + " rolls a level in its band");
                require(soul.wakeFe() == site.wakeFe(soul.level()) && soul.wakeInput() == site.wakeInput()
                        && soul.releaseStage() == site.releaseStage(), site.id() + " prices the soul from the site");
                require(soul.state().freeReset() && soul.state().sealTiers() == 0 && soul.state().ascendancy().isEmpty(),
                        site.id() + " souls carry a free Full Reset and no ascendancy");
                require(soul.state().spentPoints() == soul.level() - 1, site.id() + " souls spend every point, got "
                        + soul.state().spentPoints() + " of " + (soul.level() - 1));
                require(MegaPassiveTree.validBuild(soul.state().startNodeId(), soul.state().allocatedNodes()), site.id() + " souls hold a connected build");
                require(soul.state().following() || soul.state().targetNodes().size() <= soul.state().allocatedNodes().size(),
                        site.id() + " souls follow the rest of their archetype");
                var json = DormantSoul.CODEC.encodeStart(JsonOps.INSTANCE, soul).getOrThrow();
                require(DormantSoul.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow().equals(soul), site.id() + " souls survive a save");
                levels.add(soul.level());
            }
            require(site.minLevel() == site.maxLevel() || levels.size() > 1, site.id() + " rolls different levels from different seeds");
        }
    }

    private static void generatedBuilds() {
        for (MachineMasteryFamily family : MachineMasteryFamily.values()) {
            List<String> build = DormantArchetype.generate(family, new SplittableRandom(family.ordinal()), 69);
            require(build.size() == 69, family + " gets a full generated build, got " + build.size());
            require(MegaPassiveTree.validBuild(family.startNodeId(), build), family + " generated builds connect to its start");
            for (String id : build) {
                MegaPassiveNode node = MegaPassiveTree.node(id);
                require(node.kind() != PassiveNodeKind.KEYSTONE, family + " generated builds never take a keystone");
                require(node.kind() != PassiveNodeKind.NOTABLE || DormantArchetype.usable(family, node),
                        family + " generated builds only take notables it can use, not " + id);
            }
        }
    }

    private static void namedTargets() {
        MegaPassiveNode notable = MegaPassiveTree.TREE.nodes().stream()
                .filter(node -> DormantArchetype.usable(MachineMasteryFamily.CRUSHER, node)).findFirst().orElseThrow();
        List<String> build = DormantArchetype.through(MachineMasteryFamily.CRUSHER, List.of(notable.id()), 99);
        require(!build.isEmpty() && build.getLast().equals(notable.id()), "a named target ends the build's path to it");
        require(MegaPassiveTree.validBuild(MachineMasteryFamily.CRUSHER.startNodeId(), build), "a named-target build connects");
    }

    private static void rejectsBadData() {
        String good = "{\"families\":{\"crusher\":1},\"level\":{\"min\":5,\"max\":10},\"wake_fe\":{\"min\":1,\"max\":2},\"wake_input\":1}";
        require(DormantSite.parse("good", JsonParser.parseString(good).getAsJsonObject()).wakeCurve() == 2.0D, "the wake curve defaults to 2");
        for (String bad : List.of(
                good.replace("\"min\":5,\"max\":10", "\"min\":10,\"max\":5"),
                good.replace("\"wake_input\":1", "\"wake_input\":1,\"wake_curve\":0.5"),
                good.replace("crusher", "toaster"),
                good.replace("\"wake_input\":1", "\"wake_input\":0"),
                good.replace("\"wake_input\":1", "\"wake_input\":1,\"builds\":{\"crusher\":[{\"targets\":[\"no_such_node\"]}]}"))) {
            boolean rejected;
            try {
                DormantSite.parse("bad", JsonParser.parseString(bad).getAsJsonObject());
                rejected = false;
            } catch (RuntimeException exception) {
                rejected = true;
            }
            require(rejected, "invalid site data is rejected: " + bad);
        }
    }

    private static Map<String, DormantSite> shippedSites() {
        try {
            Path any = Path.of(DormantChecks.class.getResource("/data/rngtech/dormant_machine/end_relay.json").toURI());
            Map<String, DormantSite> sites = new TreeMap<>();
            try (Stream<Path> files = Files.list(any.getParent())) {
                for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                    String name = file.getFileName().toString().replace(".json", "");
                    JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                    sites.put(name, DormantSite.parse("rngtech:" + name, json));
                }
            }
            return sites;
        } catch (URISyntaxException | IOException exception) {
            throw new AssertionError("Dormant Machine sites are readable from the check classpath", exception);
        }
    }

    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError(label);
        }
    }
}
