package com.rngtech.rpg;

import com.rngtech.content.blockentity.HeatEngine;
import com.rngtech.content.blockentity.HeatLane;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.machine.AlloyCrucibleMaterial;
import com.rngtech.content.machine.AlloyFurnaceChassisMaterial;
import com.rngtech.content.machine.FurnaceChassisMaterial;
import com.rngtech.content.machine.ServoMaterial;
import com.rngtech.content.recipe.RecipeHeat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * The heat model's progression rules (docs/prds/heat-model.md, Progression safety): every quest step the pack expects at
 * unrolled gear still completes on the shared heat engine, within half to twice today's time and FE or fuel. Quest files
 * are local pack data, so the quest path cases live here. Each case runs tick by tick from a cold machine.
 */
public final class HeatProgressionChecks {
    private static final double LOWER = 0.5;
    private static final double UPPER = 2.0;
    private static final int MAX_TICKS = 200_000;
    /** A quest step is a run of crafts, so the first craft's warmup counts once. */
    private static final int QUEST_CRAFTS = 8;
    private static final Path RECIPES = Path.of("src/main/resources/data/rngtech/recipe");
    /** Stage 0 fuel: every burn tick of coal and the like supplies this much heat. */
    private static final int FORMING_BED_TEMPERATURE = 800;

    private static int checks;

    private HeatProgressionChecks() {
    }

    public static int run() {
        ProcessingBalanceSimulation.loadDefaultConfig();
        Path root = projectRoot();
        questOrderRun(root);
        everySetpointIsReachable();
        holdingHeatLeavesTransferHeadroom();
        baseGearBuildsNoNewStrain(root);
        stageZeroCoalSmeltsIronAndCopper(root);
        ungatedRecipesWorkAtAnyHeat();
        sliderStopsAtTheSafeMaximumWithoutBlastDraft();
        warmupFeelsLikeAFurnace();
        return checks;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Rule 5: the quest-order run, with roll-expected steps given the smallest bonus that starts them today
    // ---------------------------------------------------------------------------------------------------------------

    private static void questOrderRun(Path root) {
        List<String> failures = new ArrayList<>();
        for (QuestStep step : questSteps()) {
            Recipe recipe = Recipe.read(root, step.recipe());
            MachineStatAccumulator stats = step.gear().get().mutable();
            if (step.heatBonus() > 0) {
                stats.apply(new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.MAX_TEMPERATURE, ModifierOperation.ADD, step.heatBonus()));
            }
            require(stats.intValue(MachineStat.MAX_TEMPERATURE) >= recipe.target(), step.name() + " starts at its expected gear");
            require(stats.value(MachineStat.TEMPERATURE_STABILITY) + 1.0E-9 >= recipe.stability(),
                    step.name() + " meets its stability at its expected gear");
            Run now = simulate(stats, recipe, step.energyTransfer(), step.fuel());
            Run before = today(stats, recipe, step.energyTransfer(), step.fuel());
            double time = now.ticks() / (double) before.ticks();
            double cost = now.cost() / before.cost();
            if (Boolean.getBoolean("rngtech.heatReport")) {
                System.out.println(String.format(Locale.ROOT, "%-36s %6d ticks (%.2fx)  %8.0f %s (%.2fx)", step.name(), now.ticks(), time,
                        now.cost(), step.fuel() ? "burn" : "FE", cost));
            }
            if (!(time >= LOWER && time <= UPPER && cost >= LOWER && cost <= UPPER)) {
                failures.add(step.name());
            }
        }
        require(failures.isEmpty(), "quest steps outside half to twice today's time or cost: " + failures);
        System.out.println("Heat progression: " + questSteps().size() + " quest steps within half to twice today's time and cost");
    }

    private static List<QuestStep> questSteps() {
        return List.of(
                fuelStep("First Alloy: bronze blend", "furnace/alloy_blends/bronze_blend"),
                step("Steel blend bootstrap", "alloy_furnace/steel_blend_bootstrap_from_dust_and_fuels",
                        () -> alloyFurnace(AlloyFurnaceChassisMaterial.BRONZE, HeatCoreMaterial.BRONZE, AlloyCrucibleMaterial.BRONZE), 96, 0),
                // Roll-expected: a Copper furnace and core reach 1,000; the smelt needs 100 more, from a roll or Mastery.
                step("Steel blend smelt (roll-expected)", "furnace/alloy_blends/steel_blend",
                        () -> furnace(FurnaceChassisMaterial.COPPER, HeatCoreMaterial.COPPER), 72, 100),
                step("Pressed Circuits", "basic_electric_circuit", () -> press(HeatCoreMaterial.BRONZE, null), 128, 0),
                step("Connector Modules", "metal_press/basic_item_connector", () -> press(HeatCoreMaterial.BRONZE, ServoMaterial.STEEL), 128, 0),
                step("Advanced Electric Circuit", "advanced_electric_circuit", () -> press(HeatCoreMaterial.BRONZE, ServoMaterial.STEEL), 128, 0),
                step("Melter: Lubricant", "melter/lubricant", () -> melter(HeatCoreMaterial.TITANIUM), 160, 0),
                // Roll-expected: the Titanium core's 1,200 is the Melter's highest; Methane needs 200 more.
                step("Methane from Biomass (roll-expected)", "melter/methane_from_algae", () -> melter(HeatCoreMaterial.TITANIUM), 160, 200),
                step("Elite Electric Circuit", "elite_electric_circuit", () -> press(HeatCoreMaterial.STEEL, ServoMaterial.STEEL), 128, 0),
                step("Ultimate Electric Circuit", "ultimate_electric_circuit", () -> press(HeatCoreMaterial.TITANIUM, ServoMaterial.TITANIUM), 128, 0),
                step("Naquadah Gear", "metal_press/naquadah_gear", () -> press(HeatCoreMaterial.TITANIUM, ServoMaterial.TITANIUM), 128, 0),
                step("Nullite ingot", "alloy_furnace/nullite_from_ingots",
                        () -> alloyFurnace(AlloyFurnaceChassisMaterial.TITANIUM, HeatCoreMaterial.TITANIUM, AlloyCrucibleMaterial.TITANIUM), 192, 0),
                step("Tungstensteel ingot", "alloy_furnace/tungstensteel_from_ingots",
                        () -> alloyFurnace(AlloyFurnaceChassisMaterial.TITANIUM, HeatCoreMaterial.TITANIUM, AlloyCrucibleMaterial.TITANIUM), 192, 0),
                step("Aethergold ingot", "alloy_furnace/aethergold_from_ingots",
                        () -> alloyFurnace(AlloyFurnaceChassisMaterial.TITANIUM, HeatCoreMaterial.TITANIUM, AlloyCrucibleMaterial.TITANIUM), 192, 0),
                step("Iron dust, Iron Furnace", "furnace/metals/iron_from_dust", () -> furnace(FurnaceChassisMaterial.IRON, HeatCoreMaterial.IRON), 48, 0),
                step("Titanium dust, Titanium Furnace", "furnace/metals/titanium_from_dust",
                        () -> furnace(FurnaceChassisMaterial.TITANIUM, HeatCoreMaterial.TITANIUM), 144, 0),
                step("Platinum dust, Titanium Furnace", "furnace/metals/platinum_from_dust",
                        () -> furnace(FurnaceChassisMaterial.TITANIUM, HeatCoreMaterial.TITANIUM), 144, 0),
                step("Naquadah dust, Exotic Furnace", "furnace/metals/naquadah_from_dust",
                        () -> furnace(FurnaceChassisMaterial.EXOTIC, HeatCoreMaterial.EXOTIC), 192, 0)
        );
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Rules 2-4 and 6
    // ---------------------------------------------------------------------------------------------------------------

    /** Rule 2: with FE arriving, a lane reaches a setpoint at its Max Temperature; loss never caps it. */
    private static void everySetpointIsReachable() {
        for (QuestStep step : questSteps()) {
            if (step.fuel()) {
                continue;
            }
            MachineStatAccumulator stats = step.gear().get();
            int maximum = stats.intValue(MachineStat.MAX_TEMPERATURE);
            HeatEngine.Envelope envelope = HeatEngine.envelope(maximum / 2, maximum / 2, maximum / 2, 0.0, true, true, stats, 1.0, Integer.MAX_VALUE);
            HeatLane lane = new HeatLane();
            HeatEngine.Load idle = new HeatEngine.Load(0.0, 0.0, 1);
            for (int tick = 0; tick < 6_000 && lane.exactTemperature() < maximum - 1; tick++) {
                HeatEngine.work(lane, envelope, stats, idle, heat -> heat, tick, 0);
            }
            require(lane.exactTemperature() >= maximum - 1, step.name() + " gear reaches its Max Temperature " + maximum);
        }
    }

    /** Rule 3: at base gear, holding the hottest heat a stage reaches takes at most half the chassis's base energy transfer. */
    private static void holdingHeatLeavesTransferHeadroom() {
        for (FurnaceChassisMaterial chassis : FurnaceChassisMaterial.values()) {
            if (!chassis.electric()) {
                continue;
            }
            MachineStatAccumulator stats = furnace(chassis, bestCore(chassis.stage()));
            requireHeadroom(chassis.name() + " Furnace", stats, chassis.energyTransfer());
        }
        for (AlloyFurnaceChassisMaterial chassis : AlloyFurnaceChassisMaterial.values()) {
            requireHeadroom(chassis.name() + " Alloy Furnace",
                    alloyFurnace(chassis, bestCore(chassis.stage()), crucibleFor(chassis.stage())), chassis.energyTransfer());
        }
        requireHeadroom("Steel Press", press(HeatCoreMaterial.TITANIUM, ServoMaterial.TITANIUM), MachineBaseStatCatalog.METAL_PRESS_ENERGY_TRANSFER);
        requireHeadroom("Melter", melter(HeatCoreMaterial.TITANIUM), MachineBaseStatCatalog.MELTER_ENERGY_TRANSFER);
    }

    private static void requireHeadroom(String name, MachineStatAccumulator stats, int energyTransfer) {
        double holding = HeatEngine.holdingHeat(stats.value(MachineStat.MAX_TEMPERATURE), stats) * HeatEngine.energyRate(stats);
        require(holding <= 0.5 * energyTransfer, name + " holds its Max Temperature for " + fmt(holding) + " FE/t of " + energyTransfer);
    }

    /** Rule 4: at the default slider, base gear on its own stage's failure-bearing smelts stays inside the safe band. */
    private static void baseGearBuildsNoNewStrain(Path root) {
        record StrainCase(String name, String recipe, Supplier<MachineStatAccumulator> gear) {
        }
        List<StrainCase> cases = List.of(
                new StrainCase("Steel Furnace, lead dust", "furnace/metals/lead_from_dust", () -> furnace(FurnaceChassisMaterial.STEEL, HeatCoreMaterial.STEEL)),
                new StrainCase("Titanium Furnace, titanium dust", "furnace/metals/titanium_from_dust",
                        () -> furnace(FurnaceChassisMaterial.TITANIUM, HeatCoreMaterial.TITANIUM)),
                new StrainCase("Steel Press, advanced circuit", "advanced_electric_circuit", () -> press(HeatCoreMaterial.BRONZE, ServoMaterial.STEEL)),
                new StrainCase("Titanium Alloy Furnace, nullite", "alloy_furnace/nullite_from_ingots",
                        () -> alloyFurnace(AlloyFurnaceChassisMaterial.TITANIUM, HeatCoreMaterial.TITANIUM, AlloyCrucibleMaterial.TITANIUM)));
        for (StrainCase strainCase : cases) {
            Recipe recipe = Recipe.read(root, strainCase.recipe());
            MachineStatAccumulator stats = strainCase.gear().get();
            HeatEngine.Envelope envelope = recipe.envelope(stats, 0.0);
            HeatLane lane = new HeatLane();
            HeatEngine.Load load = load(recipe, stats);
            for (int tick = 0; tick < MAX_TICKS && !HeatEngine.finished(lane, recipe.ticks()); tick++) {
                HeatEngine.Tick result = HeatEngine.work(lane, envelope, stats, load, heat -> heat, tick, 0);
                if (lane.started()) {
                    require(result.workTemperature() >= envelope.minimum() - 1.0E-6 && result.workTemperature() <= envelope.overheat(),
                            strainCase.name() + " stays inside its safe band at the default slider");
                }
            }
            require(HeatEngine.finished(lane, recipe.ticks()), strainCase.name() + " finishes");
        }
    }

    /**
     * The slider reaches the safe maximum on its own. Blast Draft takes it to Max Temperature on recipes that can fail,
     * and to the overheat temperature on recipes that cannot.
     */
    private static void sliderStopsAtTheSafeMaximumWithoutBlastDraft() {
        MachineStatAccumulator stats = furnace(FurnaceChassisMaterial.TUNGSTENSTEEL, HeatCoreMaterial.TITANIUM);
        int maximum = stats.intValue(MachineStat.MAX_TEMPERATURE);
        HeatEngine.Envelope locked = HeatEngine.envelope(800, 800, 920, 0.0, true, false, stats, 1.0, Integer.MAX_VALUE);
        require(locked.setpoint() == 920, "without Blast Draft the slider stops at the safe maximum, not " + locked.setpoint());
        double low = 0.05;
        HeatEngine.Envelope lockedLow = HeatEngine.envelope(800, 800, 920, 0.0, true, false, stats, low, Integer.MAX_VALUE);
        HeatEngine.Envelope unlockedLow = HeatEngine.envelope(800, 800, 920, 0.0, true, true, stats, low, Integer.MAX_VALUE);
        require(lockedLow.setpoint() < 920 && lockedLow.setpoint() == unlockedLow.setpoint(),
                "Blast Draft never moves a setpoint the lane could already hold, not " + lockedLow.setpoint() + " to " + unlockedLow.setpoint());
        HeatEngine.Envelope cannotFail = HeatEngine.envelope(800, 800, 920, 0.0, false, true, stats, 1.0, Integer.MAX_VALUE);
        require(cannotFail.setpoint() == cannotFail.overheat(), "Blast Draft takes a recipe that cannot fail only to its overheat temperature");
        HeatEngine.Envelope canFail = HeatEngine.envelope(800, 800, 920, 0.0, true, true, stats, 1.0, Integer.MAX_VALUE);
        require(maximum > canFail.overheat() && canFail.setpoint() == maximum, "Blast Draft takes a recipe that can fail to Max Temperature");
    }

    /**
     * Heating should look like a furnace heating: unrolled gear climbs about 100 C a second and takes seconds to reach its
     * target, and even the fastest cores take a few seconds.
     */
    private static void warmupFeelsLikeAFurnace() {
        record Gear(String name, MachineStatAccumulator stats, int target) {
        }
        List<Gear> gears = List.of(
                new Gear("Stage0 furnace, 800", furnace(FurnaceChassisMaterial.values()[0], null), 800),
                new Gear("Iron furnace+iron core, 800", furnace(FurnaceChassisMaterial.IRON, HeatCoreMaterial.IRON), 800),
                new Gear("Steel furnace+steel core, 1100", furnace(FurnaceChassisMaterial.STEEL, HeatCoreMaterial.STEEL), 1100),
                new Gear("Aluminum furnace+aluminum core, 800", furnace(FurnaceChassisMaterial.ALUMINUM, HeatCoreMaterial.ALUMINUM), 800),
                new Gear("Titanium furnace+titanium core, 1450", furnace(FurnaceChassisMaterial.TITANIUM, HeatCoreMaterial.TITANIUM), 1450),
                new Gear("Tungstensteel furnace+exotic core, 800", furnace(FurnaceChassisMaterial.TUNGSTENSTEEL, HeatCoreMaterial.EXOTIC), 800),
                new Gear("Bronze alloy, 900", alloyFurnace(AlloyFurnaceChassisMaterial.BRONZE, HeatCoreMaterial.BRONZE, AlloyCrucibleMaterial.BRONZE), 900),
                new Gear("Crude press bronze core, 1000", press(HeatCoreMaterial.BRONZE, null), 1000),
                new Gear("Melter titanium core, 1200", melter(HeatCoreMaterial.TITANIUM), 1200));
        for (Gear gear : gears) {
            MachineStatAccumulator stats = gear.stats();
            HeatEngine.Envelope envelope = HeatEngine.envelope(gear.target(), gear.target(), gear.target(), 0.0, false, false, stats, 0.0, Integer.MAX_VALUE);
            HeatLane lane = new HeatLane();
            int tick = 0;
            for (; tick < 20_000 && lane.exactTemperature() < gear.target() - 1; tick++) {
                HeatEngine.work(lane, envelope, stats, new HeatEngine.Load(0.0, 0.0, 1), heat -> heat, tick, 0);
            }
            if (Boolean.getBoolean("rngtech.heatReport")) {
                System.out.printf(Locale.ROOT, "%-42s %d ticks (%.1f s)%n", gear.name(), tick, tick / 20.0);
            }
            require(tick >= 40, gear.name() + " takes at least two seconds to reach its target, not " + tick + " ticks");
            if (gear.name().startsWith("Iron furnace")) {
                require(tick >= 160 && tick <= 300, "unrolled Iron gear reaches 800 C in 8 to 15 seconds, not " + tick + " ticks");
            }
        }
    }

    /** A recipe without a temperature requirement works from cold or hot at its own pace, and never waits as Too Hot. */
    private static void ungatedRecipesWorkAtAnyHeat() {
        MachineStatAccumulator stats = furnace(FurnaceChassisMaterial.STEEL, HeatCoreMaterial.STEEL);
        HeatEngine.Envelope envelope = HeatEngine.envelope(0, 0, 0, 0.0, false, true, stats, 1.0, Integer.MAX_VALUE);
        require(envelope.ungated() && envelope.setpoint() == HeatEngine.AMBIENT, "an ungated recipe ignores the slider");
        int ticks = 1_200;
        double recipeHeat = 17_280.0;
        HeatEngine.Load load = new HeatEngine.Load(recipeHeat / ticks, 1.0, 1);
        for (int start : new int[] {HeatEngine.AMBIENT, 900}) {
            HeatLane lane = new HeatLane();
            lane.setTemperature(start);
            double[] paid = {0.0};
            int tick = 0;
            for (; tick < 2 * ticks && !HeatEngine.finished(lane, ticks); tick++) {
                HeatEngine.Tick result = HeatEngine.work(lane, envelope, stats, load, heat -> {
                    paid[0] += heat;
                    return heat;
                }, tick, 0);
                require(!result.tooHot() && result.heatFactor() == 1.0, "an ungated recipe works at " + fmt(lane.exactTemperature()) + " C");
            }
            require(tick == ticks, "an ungated recipe from " + start + " C takes its recipe time, not " + tick + " ticks");
            require(paid[0] <= recipeHeat + 1.0, "an ungated recipe from " + start + " C pays at most its recipe heat, not " + fmt(paid[0]));
        }
    }

    /** Rule 6: coal in the Stage 0 Furnace reaches 800 C and smelts Iron and Copper. */
    private static void stageZeroCoalSmeltsIronAndCopper(Path root) {
        for (String metal : List.of("iron", "copper")) {
            Recipe recipe = Recipe.read(root, "furnace/metals/" + metal + "_from_dust");
            MachineStatAccumulator stats = furnace(FurnaceChassisMaterial.PRIMITIVE, null);
            require(stats.intValue(MachineStat.MAX_TEMPERATURE) >= 800, "the Stage 0 Furnace reaches 800 C");
            Run run = simulate(stats, recipe, 0, true);
            require(run.ticks() < MAX_TICKS, "coal smelts " + metal + " dust in the Stage 0 Furnace");
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Simulation
    // ---------------------------------------------------------------------------------------------------------------

    /** One craft from a cold machine at the default slider, with FE intake capped at the chassis's base transfer. */
    private static Run simulate(MachineStatAccumulator stats, Recipe recipe, int energyTransfer, boolean fuel) {
        HeatEngine.Envelope envelope = recipe.envelope(stats, 0.0);
        HeatEngine.Load load = load(recipe, stats);
        double rate = HeatEngine.energyRate(stats);
        double[] paid = {0.0};
        HeatEngine.HeatSupply supply = heat -> {
            double delivered = fuel ? heat : Math.min(heat, energyTransfer / rate);
            paid[0] += delivered;
            return delivered;
        };
        HeatLane lane = new HeatLane();
        int crafts = 0;
        for (int tick = 1; tick <= MAX_TICKS; tick++) {
            HeatEngine.work(lane, envelope, stats, load, supply, tick, 0);
            if (HeatEngine.finished(lane, recipe.ticks())) {
                lane.resetCycle();
                if (++crafts == QUEST_CRAFTS) {
                    return new Run(tick, fuel ? paid[0] / HeatEngine.HEAT_PER_BURN_TICK : paid[0] * rate);
                }
            }
        }
        return new Run(MAX_TICKS, Double.MAX_VALUE);
    }

    /**
     * Today's model on the same gear: warm to the target at 10 x Heat Transfer degrees per tick, then craft in the recipe's
     * ticks divided by Processing Speed and Heat Transfer, paying the recipe's FE (or a burn tick) every tick, slowed when
     * that FE outruns the chassis's base energy transfer. Thermal
     * Conductivity carries what Heat Transfer did for crafting speed, so it stands in for today's Heat Transfer.
     */
    private static Run today(MachineStatAccumulator stats, Recipe recipe, int energyTransfer, boolean fuel) {
        double heatTransfer = Math.max(0.1, stats.value(MachineStat.THERMAL_CONDUCTIVITY));
        int warmup = (int) Math.ceil((recipe.target() - HeatEngine.AMBIENT) / (double) Math.max(1, Math.round(10.0 * heatTransfer)));
        int crafting = Math.max(1, (int) Math.ceil(stats.adjustedProcessingTicks(recipe.ticks()) / heatTransfer));
        int ticks = warmup + crafting * QUEST_CRAFTS;
        if (fuel) {
            return new Run(ticks, ticks);
        }
        int energy = stats.adjustedEnergyCost((int) Math.round(recipe.heat() / recipe.legacyScale() / RecipeHeat.HEAT_PER_LEGACY_FE));
        int perTick = Math.max(1, (int) Math.ceil(energy / (double) crafting));
        // FE intake past the transfer stretches today's craft the same way it throttles the heater.
        double throttle = Math.max(1.0, perTick / (double) energyTransfer);
        return new Run((int) Math.ceil(warmup + crafting * QUEST_CRAFTS * throttle), perTick * (double) ticks);
    }

    private static HeatEngine.Load load(Recipe recipe, MachineStatAccumulator stats) {
        double work = HeatEngine.speed(stats);
        return new HeatEngine.Load(recipe.heat() * work / recipe.ticks(), work, 1);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Gear
    // ---------------------------------------------------------------------------------------------------------------

    static MachineStatAccumulator furnace(FurnaceChassisMaterial chassis, HeatCoreMaterial core) {
        MachineStatAccumulator stats = MachineBaseStatCatalog.furnace(chassis);
        if (core != null && chassis.usesHeatCores()) {
            ComponentBaseStatCatalog.applyContribution(stats, ComponentBaseStatCatalog.heatCore(core), MachineTraits.EMPTY);
        }
        return stats;
    }

    static MachineStatAccumulator alloyFurnace(AlloyFurnaceChassisMaterial chassis, HeatCoreMaterial core, AlloyCrucibleMaterial crucible) {
        MachineStatAccumulator stats = MachineBaseStatCatalog.alloyFurnace(chassis);
        ComponentBaseStatCatalog.applyContribution(stats, ComponentBaseStatCatalog.heatCore(core), MachineTraits.EMPTY);
        ComponentBaseStatCatalog.applyContribution(stats, ComponentBaseStatCatalog.alloyCrucible(crucible), MachineTraits.EMPTY);
        return stats;
    }

    /** A Crude Press without a Servo, or a Steel Press with one; both heat their forming bed 800 C above the core. */
    static MachineStatAccumulator press(HeatCoreMaterial core, ServoMaterial servo) {
        MachineStatAccumulator stats = MachineBaseStatCatalog.metalPress();
        stats.apply(new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.MAX_TEMPERATURE, ModifierOperation.ADD, FORMING_BED_TEMPERATURE));
        ComponentBaseStatCatalog.applyContribution(stats, ComponentBaseStatCatalog.heatCore(core), MachineTraits.EMPTY);
        if (servo != null) {
            ComponentBaseStatCatalog.applyContribution(stats, ComponentBaseStatCatalog.servo(servo), MachineTraits.EMPTY);
        }
        return stats;
    }

    static MachineStatAccumulator melter(HeatCoreMaterial core) {
        MachineStatAccumulator stats = MachineBaseStatCatalog.melter();
        ComponentBaseStatCatalog.applyContribution(stats, ComponentBaseStatCatalog.heatCore(core), MachineTraits.EMPTY);
        return stats;
    }

    private static HeatCoreMaterial bestCore(int stage) {
        HeatCoreMaterial best = HeatCoreMaterial.IRON;
        for (HeatCoreMaterial core : HeatCoreMaterial.values()) {
            if (core.stage() <= stage && core.maxTemperatureBonus() > best.maxTemperatureBonus()) {
                best = core;
            }
        }
        return best;
    }

    private static AlloyCrucibleMaterial crucibleFor(int stage) {
        AlloyCrucibleMaterial best = AlloyCrucibleMaterial.BRONZE;
        for (AlloyCrucibleMaterial crucible : AlloyCrucibleMaterial.values()) {
            if (crucible.stage() <= stage && crucible.stage() >= best.stage()) {
                best = crucible;
            }
        }
        return best;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Data
    // ---------------------------------------------------------------------------------------------------------------

    private record QuestStep(String name, String recipe, Supplier<MachineStatAccumulator> gear, int energyTransfer, int heatBonus, boolean fuel) {
    }

    private static QuestStep step(String name, String recipe, Supplier<MachineStatAccumulator> gear, int energyTransfer, int heatBonus) {
        return new QuestStep(name, recipe, gear, energyTransfer, heatBonus, false);
    }

    private static QuestStep fuelStep(String name, String recipe) {
        return new QuestStep(name, recipe, () -> furnace(FurnaceChassisMaterial.PRIMITIVE, null), 0, 0, true);
    }

    private record Run(int ticks, double cost) {
    }

    /** The heat fields of a recipe file, with each recipe type's codec defaults. */
    /** {@code legacyScale} undoes the temperature scale (and the Press trim) that shipped recipes have built into their heat. */
    private record Recipe(int ticks, int heat, int minimum, int target, int safeMaximum, double stability, double legacyScale) {
        static Recipe read(Path root, String id) {
            Path path = root.resolve(RECIPES).resolve(id + ".json");
            JsonObject json;
            try {
                json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            } catch (IOException exception) {
                throw new IllegalStateException("Cannot read " + path, exception);
            }
            String type = json.get("type").getAsString();
            int defaultMinimum = switch (type) {
                case "rngtech:alloy_furnace" -> 700;
                case "rngtech:metal_press" -> 1100;
                case "rngtech:melter" -> 1200;
                default -> 0;
            };
            int minimum = intOr(json, "minimum_temperature", defaultMinimum);
            int target = intOr(json, "target_temperature", minimum);
            int defaultSafe = type.equals("rngtech:metal_press") ? 1400 : Math.max(target, (int) Math.round(target * 1.15));
            int safe = Math.max(target, intOr(json, "safe_maximum_temperature", defaultSafe));
            double defaultStability = type.equals("rngtech:metal_press") ? 1.0 : 0.0;
            double stability = json.has("required_temperature_stability") ? json.get("required_temperature_stability").getAsDouble() : defaultStability;
            double legacyScale = RecipeHeat.temperatureScale(target) * (type.equals("rngtech:metal_press") ? RecipeHeat.PRESS_HEAT_SCALE : 1.0);
            return new Recipe(intOr(json, "processing_ticks", 200), intOr(json, "heat", 1), minimum, target, safe,
                    type.equals("rngtech:melter") ? 0.0 : stability, legacyScale);
        }

        HeatEngine.Envelope envelope(MachineStatAccumulator stats, double slider) {
            return HeatEngine.envelope(minimum, target, safeMaximum, stability, false, false, stats, slider, Integer.MAX_VALUE);
        }

        private static int intOr(JsonObject json, String key, int fallback) {
            return json.has(key) ? json.get(key).getAsInt() : fallback;
        }
    }

    private static Path projectRoot() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            if (Files.exists(dir.resolve("settings.gradle"))) {
                return dir;
            }
        }
        throw new IllegalStateException("No project root above " + Path.of("").toAbsolutePath());
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
