package com.rngtech.rpg;

import com.rngtech.content.energy.HeatCoreMaterial;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * The Collapse Residue route to Ender Slurry rewards a well-rolled Melter: no unrolled Heat Core the Melter accepts
 * reaches its heat, while the best one still runs the plain Ender Pearl slurry.
 */
public final class VoidCatalystChainChecks {
    /** MelterBlockEntity#isHeatCore accepts Heat Cores up to this stage. */
    private static final int MELTER_MAX_HEAT_CORE_STAGE = 6;
    private static int checks;

    private VoidCatalystChainChecks() {
    }

    public static int run() {
        checks = 0;
        int residueHeat = minimumTemperature("ender_slurry_from_residue");
        int plainHeat = minimumTemperature("ender_slurry");
        int best = 0;
        for (HeatCoreMaterial core : HeatCoreMaterial.values()) {
            if (core.stage() > MELTER_MAX_HEAT_CORE_STAGE) {
                continue;
            }
            MachineStatAccumulator stats = MachineBaseStatCatalog.melter();
            ComponentBaseStatCatalog.applyContribution(stats, ComponentBaseStatCatalog.heatCore(core), MachineTraits.EMPTY);
            int heat = stats.intValue(MachineStat.MAX_TEMPERATURE);
            require(heat < residueHeat, "an unrolled Melter with a " + core + " Heat Core reaches " + heat
                    + " heat, enough for Ender Slurry from Collapse Residue (" + residueHeat + ")");
            best = Math.max(best, heat);
        }
        require(best >= plainHeat, "the best unrolled Melter reaches " + best + " heat, short of plain Ender Slurry (" + plainHeat + ")");
        return checks;
    }

    private static int minimumTemperature(String recipe) {
        String path = "/data/rngtech/recipe/melter/" + recipe + ".json";
        try (InputStream stream = VoidCatalystChainChecks.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new AssertionError("Missing Melter recipe " + path);
            }
            JsonObject json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            return json.get("minimum_temperature").getAsInt();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
