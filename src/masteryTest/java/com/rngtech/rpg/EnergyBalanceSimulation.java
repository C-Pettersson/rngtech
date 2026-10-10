package com.rngtech.rpg;

import com.rngtech.content.blockentity.SolidFuelBurnerBlockEntity;
import com.rngtech.content.cable.EnergyConnectorTier;
import com.rngtech.content.energy.CathodeMaterial;
import com.rngtech.content.energy.CavitationRotorMaterial;
import com.rngtech.content.energy.CollapseNozzleMaterial;
import com.rngtech.content.energy.ContainmentLiningMaterial;
import com.rngtech.content.energy.FuelBoxMaterial;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.energy.ReactorChamberMaterial;
import com.rngtech.content.energy.RecoveryFilterMaterial;
import com.rngtech.content.energy.SolarArrayExtenderMaterial;
import com.rngtech.content.energy.SolarPanelMaterial;
import com.rngtech.content.energy.SolidFuelBurnerChassis;
import com.rngtech.content.energy.VacuumCollapsePartMaterial;
import com.rngtech.content.machine.ServoMaterial;
import com.rngtech.content.recycling.RecyclingData;
import com.rngtech.rpg.refinement.RefinementEngine;
import com.rngtech.rpg.refinement.RefinementModifier;
import com.rngtech.rpg.refinement.RefinementOperation;
import com.rngtech.rpg.refinement.RefinementResult;
import com.rngtech.rpg.refinement.RefinementSelection;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.RandomSource;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Monte Carlo of generator output across crafted machines and parts.
 *
 * <p>Rolls use {@link MachineTraitRoller}, and part stats merge through {@link ComponentBaseStatCatalog}, so roll and
 * aggregation rules cannot drift. Each generator's final FE formula mirrors its block entity and names the source
 * method; update it when that method changes. Scenarios from {@code tools/balance/energy-scenarios.json} override those
 * mirrored formulas to preview balance changes without touching the game.
 */
public final class EnergyBalanceSimulation {
    private static final int DAY_TICKS = 24000;
    private static final int PEAK_START = 4000;
    private static final int PEAK_END = 8000;
    private static final int BULK_SPEED_STEADY_PERCENT = 100;
    private static final int BEST_OF = 5;
    private static final List<EnergyConnectorTier> REPORTED_CONNECTORS =
            List.of(EnergyConnectorTier.GOLD, EnergyConnectorTier.SPARKSTEEL, EnergyConnectorTier.ARCLITE);

    private final Path recipeRoot;
    private final ChainCosts chains;
    private final int dayTicks;
    private final int peakTicks;
    private Scenario scenario = Scenario.CURRENT;
    private List<Setup> setups = new ArrayList<>();

    private EnergyBalanceSimulation(Path projectDir) throws IOException {
        recipeRoot = projectDir.resolve("src/main/resources/data/rngtech/recipe");
        chains = ChainCosts.load(projectDir.resolve("tools/balance/energy-chain-costs.json"));
        dayTicks = countDaylightTicks();
        peakTicks = PEAK_END - PEAK_START + 1;
    }

    public static void main(String[] args) throws IOException {
        Path projectDir = Path.of(args.length > 0 ? args[0] : ".").toAbsolutePath().normalize();
        Path output = Path.of(args.length > 1 ? args[1] : "build/energy-balance/energy-balance.json");
        int trials = args.length > 2 ? Integer.parseInt(args[2]) : 1000;
        int behaviorTrials = args.length > 3 ? Integer.parseInt(args[3]) : Math.max(1, trials / 2);

        EnergyBalanceSimulation simulation = new EnergyBalanceSimulation(projectDir);
        Map<String, Object> scenarios = new LinkedHashMap<>();
        for (Scenario scenario : Scenario.load(projectDir.resolve("tools/balance/energy-scenarios.json"))) {
            simulation.scenario = scenario;
            simulation.setups = new ArrayList<>();
            simulation.defineSetups();
            Map<String, Object> behaviors = new LinkedHashMap<>();
            for (Behavior behavior : Behavior.values()) {
                int count = behavior == Behavior.FRESH ? trials : behaviorTrials;
                behaviors.put(behavior.id(), simulation.run(behavior, count));
                System.out.println("Energy balance: " + scenario.id() + "/" + behavior.id() + " " + simulation.setups.size() + " setups x " + count);
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("label", scenario.label());
            row.put("settings", scenario.settings());
            row.put("behaviors", behaviors);
            row.put("gearFuel", simulation.gearFuel(trials));
            scenarios.put(scenario.id(), row);
        }

        Map<String, Object> report = new LinkedHashMap<>();
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("trials", trials);
        meta.put("behaviorTrials", behaviorTrials);
        meta.put("bestOf", BEST_OF);
        meta.put("daylightTicks", simulation.dayTicks);
        meta.put("peakTicks", simulation.peakTicks);
        meta.put("fuels", simulation.chains.fuels());
        report.put("meta", meta);
        report.put("scenarios", scenarios);

        Files.createDirectories(output.toAbsolutePath().getParent());
        Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
        Files.writeString(output, gson.toJson(report), StandardCharsets.UTF_8);
        System.out.println("Energy balance report -> " + output);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Setups
    // ---------------------------------------------------------------------------------------------------------------

    private void defineSetups() throws IOException {
        solidFuelBurners();
        bioGenerators();
        solarPanels();
        solarArrays();
        potentialReactors();
        corrosionCells();
        syngasCombustors();
        cavitationGenerators();
        ammoniaFuelCells();
        vacuumCollapseGenerators();
    }

    /** SolidFuelBurnerBlockEntity#effectiveStats, #tryStartBurningFuel, #effectiveFuelDurationTicks, #applyHeatWaste. */
    private void solidFuelBurners() {
        ChainCosts.Fuel coal = chains.fuel("solid_fuel_burner");
        for (SolidFuelBurnerChassis chassis : SolidFuelBurnerChassis.values()) {
            for (HeatCoreMaterial core : HeatCoreMaterial.values()) {
                if (core.stage() > chassis.maxPartStage()) {
                    continue;
                }
                for (FuelBoxMaterial box : FuelBoxMaterial.values()) {
                    if (box.stage() > chassis.maxPartStage()) {
                        continue;
                    }
                    int stage = max(chassis.stage(), core.stage(), box.stage());
                    add("solid_fuel_burner", "Solid Fuel Burner", stage, List.of(
                            "chassis:" + name(chassis), "heat_core:" + name(core), "fuel_box:" + name(box)
                    ), coal, roller -> {
                        MachineStatAccumulator stats = MachineBaseStatCatalog.solidFuelBurner(chassis);
                        stats.apply(roller.machine(MachineType.SOLID_FUEL_BURNER, chassis.stage()));
                        part(stats, ComponentBaseStatCatalog.heatCore(core),
                                roller.part(MachinePartType.HEAT_CORE, MachineType.SOLID_FUEL_BURNER, core.stage(), core.refinementPotential()));
                        part(stats, ComponentBaseStatCatalog.fuelBox(box),
                                roller.part(MachinePartType.FUEL_BOX, MachineType.SOLID_FUEL_BURNER, box.stage(), box.refinementPotential()));

                        int generation = Math.max(0, (int) Math.round(stats.value(MachineStat.ENERGY_GENERATION)));
                        double fuelScale = Math.max(0.1, stats.value(MachineStat.FUEL_EFFICIENCY))
                                * Math.max(0.1, stats.value(MachineStat.EFFICIENCY))
                                * Math.max(0.1, stats.value(MachineStat.FUEL_DURATION));
                        double isolation = stats.value(MachineStat.HEAT_ISOLATION);
                        double wastePerTick = isolation >= 1.0 ? 0.0 : Math.min(1.0, 1.0 - Math.max(0.0, isolation));
                        double fePerBurnTick = scenario.solidFuelFePerBurnTick() > 0
                                ? scenario.solidFuelFePerBurnTick()
                                : SolidFuelBurnerBlockEntity.FUEL_ENERGY_PER_BURN_TICK;
                        double fuelEnergy = generation <= 0 ? 0.0 : Math.max(1, ceil(coal.baseAmount() * fuelScale)) * fePerBurnTick;
                        double perItem = fuelEnergy / (1.0 + wastePerTick);
                        return Outcome.running(generation, generation, perItem)
                                .withMetric("burnSeconds", generation <= 0 ? 0.0 : perItem / generation / 20.0);
                    });
                }
            }
        }
    }

    /** BioGeneratorBlockEntity#effectiveGenerationRate, #effectiveFuelDurationTicks. */
    private void bioGenerators() {
        ChainCosts.Fuel fuel = chains.fuel("bio_generator");
        MachineStat powerStat = MachineStat.valueOf(fuel.stat());
        for (boolean chamber : new boolean[] {false, true}) {
            add("bio_generator", "Bio Generator", 2, List.of(chamber ? "bio_chamber:standard" : "bio_chamber:none"), fuel, roller -> {
                MachineStatAccumulator stats = MachineBaseStatCatalog.bioGenerator();
                stats.apply(roller.machine(MachineType.BIO_GENERATOR, 2));
                if (chamber) {
                    part(stats, ComponentBaseStatCatalog.bioChamber(),
                            roller.part(MachinePartType.BIO_CHAMBER, MachineType.BIO_GENERATOR, 1, 0));
                }
                double multiplier = Math.max(0.1, stats.effectiveEnergyGenerationMultiplier());
                double flat = Math.max(0.0, stats.effectiveFlatEnergyGenerationBonus());
                int rate = Math.max(1, ceil(8 * multiplier + flat));
                double baseEnergy = fuel.baseAmount()
                        * Math.max(0.1, stats.value(powerStat))
                        * Math.max(0.1, stats.value(MachineStat.FUEL_EFFICIENCY))
                        * Math.max(0.1, stats.value(MachineStat.EFFICIENCY));
                int baseTicks = Math.max(1, ceil(baseEnergy / 8.0));
                int ticks = Math.max(1, ceil(baseTicks * Math.max(0.1, stats.value(MachineStat.FUEL_DURATION))));
                return Outcome.running(rate, rate, (double) ticks * rate);
            });
        }
    }

    /** SolarPanelBlockEntity#currentGeneration and #applyPeakSolarBonus, clear weather. */
    private void solarPanels() {
        for (SolarPanelMaterial material : SolarPanelMaterial.values()) {
            add("solar_panel", "Solar Panel", material.stage(), List.of("panel:" + name(material)), null, roller -> {
                MachineStatAccumulator stats = solarPanelStats(material, roller);
                double raw = clearGeneration(material) * panelScale(stats) * Math.max(0.01, stats.value(MachineStat.EFFICIENCY));
                double clear = Math.floor(raw);
                double peak = Math.floor(raw * Math.max(1.0, stats.value(MachineStat.PEAK_SOLAR_GENERATION)));
                double average = ((dayTicks - peakTicks) * clear + peakTicks * peak) / DAY_TICKS;
                return Outcome.running(clear, average, Double.NaN);
            });
        }
    }

    /** SolarArrayControllerBlockEntity#adjustedPanelGeneration and the array totals, clear weather, full single-stage ring. */
    private void solarArrays() {
        Scenario.Solar solar = scenario.solar();
        List<SolarArrayExtenderMaterial> extenders = withNone(SolarArrayExtenderMaterial.values());
        for (SolarArrayExtenderMaterial extender : extenders) {
            for (SolarPanelMaterial material : SolarPanelMaterial.values()) {
                int stage = max(4, material.stage(), extender == null ? 0 : extender.stage());
                add("solar_array", "Solar Array", stage, List.of(
                        "extender:" + (extender == null ? "none" : name(extender)), "panels:" + name(material)
                ), null, roller -> {
                    MachineStatAccumulator controller = MachineBaseStatCatalog.solarArrayController();
                    controller.apply(roller.machine(MachineType.SOLAR_ARRAY_CONTROLLER, 4));
                    if (extender != null) {
                        part(controller, ComponentBaseStatCatalog.solarArrayExtender(extender),
                                roller.part(MachinePartType.SOLAR_ARRAY_EXTENDER, MachineType.SOLAR_ARRAY_CONTROLLER, extender.stage(), 0));
                        if (solar.extenderRangeBonus() != null) {
                            controller.apply(new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.SOLAR_PANEL_LIMIT, ModifierOperation.ADD,
                                    solar.extenderRangeBonus() - extender.rangeBonus()));
                        }
                        if (solar.extenderGeneration() != null) {
                            controller.apply(new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.ENERGY_GENERATION, ModifierOperation.MORE,
                                    solar.extenderGeneration() / extender.generationMultiplier()));
                        }
                    }
                    int range = Math.max(1, Math.min(solar.maxRange(), controller.intValue(MachineStat.SOLAR_PANEL_LIMIT)));
                    int panels = (range * 2 + 1) * (range * 2 + 1) - 1;
                    double controllerEfficiency = Math.max(0.01, controller.value(MachineStat.EFFICIENCY));
                    double clearSky = controller.value(MachineStat.CLEAR_SKY_AMPLIFICATION);
                    double lunar = controller.value(MachineStat.LUNAR_INVERSION);
                    double nightPercent = Math.max(controller.value(MachineStat.MOONLIGHT_CONVERSION), lunar);

                    double clear = 0.0;
                    double peak = 0.0;
                    double night = 0.0;
                    for (int index = 0; index < panels; index++) {
                        MachineStatAccumulator panel = solarPanelStats(material, roller);
                        double base = clearGeneration(material) * panelScale(panel) * Math.max(0.01, panel.value(MachineStat.EFFICIENCY));
                        double day = clearSky > 0.0 ? base * (1.0 + clearSky / 100.0) : base;
                        if (lunar > 0.0) {
                            day *= 0.85;
                        }
                        double dayPeak = day * Math.max(1.0, panel.value(MachineStat.PEAK_SOLAR_GENERATION));
                        double moon = nightPercent <= 0.0 ? 0.0 : base * nightPercent / 100.0;
                        if (solar.controllerFlatOnce()) {
                            clear += day;
                            peak += dayPeak;
                            night += moon;
                        } else {
                            clear += arrayPanel(controller, day, controllerEfficiency);
                            peak += arrayPanel(controller, dayPeak, controllerEfficiency);
                            night += arrayPanel(controller, moon, controllerEfficiency);
                        }
                    }
                    if (solar.controllerFlatOnce()) {
                        clear = arrayPanel(controller, clear, controllerEfficiency);
                        peak = arrayPanel(controller, peak, controllerEfficiency);
                        night = arrayPanel(controller, night, controllerEfficiency);
                    }
                    double arrayScale = 1.20;
                    if (panels > 1 && controller.value(MachineStat.SOLAR_PANEL_SYNCHRONIZATION) > 0.0) {
                        arrayScale *= 1.0 + controller.value(MachineStat.SOLAR_PANEL_SYNCHRONIZATION) / 100.0;
                    }
                    clear *= arrayScale;
                    peak *= arrayScale;
                    night *= arrayScale;
                    double average = ((dayTicks - peakTicks) * clear + peakTicks * peak + (DAY_TICKS - dayTicks) * night) / DAY_TICKS;
                    return Outcome.running(clear, average, Double.NaN).withMetric("panels", panels).withMetric("perPanel", average / panels);
                });
            }
        }
    }

    /** PotentialReactorBlockEntity#effectiveStats and #effectiveRecipeEnergy; picks the best obtainable starter fuel. */
    private void potentialReactors() throws IOException {
        List<Recipe> recipes = recipes("potential_reactor");
        List<RecoveryFilterMaterial> filters = withNone(RecoveryFilterMaterial.values());
        List<ContainmentLiningMaterial> linings = withNone(ContainmentLiningMaterial.values());
        for (ReactorChamberMaterial chamber : ReactorChamberMaterial.values()) {
            for (RecoveryFilterMaterial filter : filters) {
                for (ContainmentLiningMaterial lining : linings) {
                    int partStage = max(3, chamber.stage(), filter == null ? 0 : filter.stage(), lining == null ? 0 : lining.stage());
                    boolean allSteel = chamber == ReactorChamberMaterial.STEEL && filter == RecoveryFilterMaterial.STEEL
                            && lining == ContainmentLiningMaterial.STEEL;
                    for (int stage : allSteel ? new int[] {partStage, 5} : new int[] {partStage}) {
                        add("potential_reactor", "Potential Reactor", stage, List.of(
                                "chamber:" + name(chamber),
                                "filter:" + (filter == null ? "none" : name(filter)),
                                "lining:" + (lining == null ? "none" : name(lining)),
                                "fuel_access:stage_" + stage
                        ), null, roller -> {
                            MachineStatAccumulator stats = reactorStats(roller, chamber, filter, lining);
                            int level = stats.intValue(MachineStat.PROCESSING_LEVEL);
                            List<FuelRecipe> legal = recipes.stream()
                                    .filter(recipe -> level >= recipe.minimumStage()
                                            && chains.available("potential_reactor/" + recipe.id(), stage))
                                    .map(recipe -> FuelRecipe.of(recipe, chains.fuel("potential_reactor/" + recipe.id())))
                                    .toList();
                            return bestRecipe(legal, recipe -> {
                                int ticks = stats.adjustedProcessingTicks(recipe.ticks());
                                return new double[] {reactorEnergy(stats, recipe.energy(), ticks), ticks};
                            });
                        });
                    }
                }
            }
        }
    }

    private MachineStatAccumulator reactorStats(Roller roller, ReactorChamberMaterial chamber, RecoveryFilterMaterial filter,
            ContainmentLiningMaterial lining) {
        MachineStatAccumulator stats = MachineBaseStatCatalog.potentialReactor();
        MachineTraits traits = roller.machine(MachineType.POTENTIAL_REACTOR, 3);
        stats.apply(traits);
        part(stats, ComponentBaseStatCatalog.reactorChamber(chamber),
                roller.part(MachinePartType.REACTOR_CHAMBER, MachineType.POTENTIAL_REACTOR, chamber.stage(), chamber.refinementPotential()));
        if (filter != null) {
            part(stats, ComponentBaseStatCatalog.recoveryFilter(filter),
                    roller.part(MachinePartType.RECOVERY_FILTER, MachineType.POTENTIAL_REACTOR, filter.stage(), filter.refinementPotential()));
        }
        if (lining != null) {
            part(stats, ComponentBaseStatCatalog.containmentLining(lining),
                    roller.part(MachinePartType.CONTAINMENT_LINING, MachineType.POTENTIAL_REACTOR, lining.stage(), lining.refinementPotential()));
        }
        bulkSpeed(stats, traits);
        return stats;
    }

    private static double reactorEnergy(MachineStatAccumulator stats, double energy, int ticks) {
        return Math.max(1.0, stats.generatedEnergyTotal(energy, ticks)
                * Math.max(0.1, stats.value(MachineStat.EFFICIENCY))
                * clamp(stats.value(MachineStat.STABILITY), 0.25, 1.50));
    }

    /**
     * CorrosionCellBlockEntity#effectiveStats, #canProcessStage and #effectiveRecipeEnergy; the Fluid Pump has no
     * generation stats. One setup per player stage with that stage's Cathode installed, plus a plates-only setup
     * without a Cathode, since later plates and anodes unlock better fuel for the same Stage 4 machine.
     */
    private void corrosionCells() throws IOException {
        List<FuelRecipe> recipes;
        Map<String, Integer> cathodeStages = new LinkedHashMap<>();
        if (scenario.corrosionRecipes() != null) {
            recipes = scenario.corrosionRecipes();
        } else {
            recipes = recipes("corrosion_cell").stream().map(recipe -> FuelRecipe.of(recipe, chains.fuel("corrosion_cell/" + recipe.id()))).toList();
            for (FuelRecipe recipe : recipes) {
                JsonObject json = recipeJson("corrosion_cell", recipe.id());
                cathodeStages.put(recipe.id(), json.has("minimum_cathode_stage") ? json.get("minimum_cathode_stage").getAsInt() : 0);
            }
        }
        for (int playerStage = 4; playerStage <= 8; playerStage++) {
            int stage = playerStage;
            CathodeMaterial cathode = Arrays.stream(CathodeMaterial.values()).filter(material -> material.stage() == stage).findFirst().orElse(null);
            corrosionCell(recipes, cathodeStages, stage, cathode);
            if (cathode != null) {
                corrosionCell(recipes, cathodeStages, stage, null);
            }
        }
    }

    private void corrosionCell(List<FuelRecipe> recipes, Map<String, Integer> cathodeStages, int stage, CathodeMaterial cathode) {
        List<String> components = cathode != null
                ? List.of("fuel_access:stage_" + stage, "cathode:" + name(cathode))
                : stage >= 5 ? List.of("fuel_access:stage_" + stage, "cathode:none") : List.of("fuel_access:stage_" + stage);
        int cathodeStage = cathode == null ? 0 : cathode.stage();
        add("corrosion_cell", "Corrosion Cell", stage, components, null, roller -> {
            MachineStatAccumulator stats = MachineBaseStatCatalog.corrosionCell();
            MachineTraits traits = roller.machine(MachineType.CORROSION_CELL, 4);
            stats.apply(traits);
            if (cathode != null) {
                part(stats, ComponentBaseStatCatalog.cathode(cathode),
                        roller.part(MachinePartType.CATHODE, MachineType.CORROSION_CELL, cathode.stage(), cathode.refinementPotential()));
            }
            bulkSpeed(stats, traits);
            List<FuelRecipe> legal = recipes.stream()
                    .filter(recipe -> 4 >= recipe.minimumStage() && recipe.obtainable() && stage >= recipe.availableFromStage())
                    .filter(recipe -> cathodeStages.getOrDefault(recipe.id(), 0) <= cathodeStage)
                    .filter(recipe -> cathode != null || !recipe.id().endsWith("_anode"))
                    .toList();
            return bestRecipe(legal, recipe -> {
                int ticks = stats.adjustedProcessingTicks(recipe.ticks());
                return new double[] {reactorEnergy(stats, recipe.energy(), ticks), ticks};
            });
        });
    }

    /** GasChemistryBlockEntity#adjustedTicks and #adjustedGeneration for the Syngas Combustor. */
    private void syngasCombustors() throws IOException {
        FuelRecipe syngas = FuelRecipe.of(recipe("gas_combustion", "syngas_power"), chains.fuel("syngas_combustor/syngas_power"))
                .withEnergy(scenario.recipeEnergy("gas_combustion/syngas_power"));
        List<ServoMaterial> servos = withNone(Arrays.stream(ServoMaterial.values()).filter(servo -> servo.stage() >= 4).toArray(ServoMaterial[]::new));
        for (ServoMaterial servo : servos) {
            int stage = max(5, servo == null ? 0 : servo.stage());
            add("syngas_combustor", "Syngas Combustor", stage, List.of("servo:" + (servo == null ? "none" : name(servo))), null, roller -> {
                MachineStatAccumulator stats = MachineBaseStatCatalog.syngasCombustor();
                roller.machine(MachineType.SYNGAS_COMBUSTOR, 5).modifiers().forEach(stats::apply);
                if (servo != null) {
                    part(stats, ComponentBaseStatCatalog.servo(servo), roller.part(MachinePartType.SERVO, MachineType.METAL_PRESS, servo.stage(), servo.refinementPotential()));
                }
                int ticks = Math.max(1, ceil(syngas.ticks() / stats.value(MachineStat.PROCESSING_SPEED)));
                double energy = Math.max(1L, (long) Math.floor(stats.generatedEnergyTotal(syngas.energy(), ticks) * stats.value(MachineStat.EFFICIENCY)));
                return Outcome.recipe(syngas, energy, ticks);
            });
        }
    }

    /**
     * CavitationGeneratorBlockEntity: water recipe with steady-state heat strain (#coolDown, #currentGenerationRate).
     * FE per input is FE per rotor, since water is free and the rotor wears out. Delivered metrics cap output at the
     * attached connector's tier.
     */
    private void cavitationGenerators() throws IOException {
        Recipe water = recipe("cavitation", "water");
        JsonObject waterJson = recipeJson("cavitation", "water");
        int heatStrain = waterJson.get("heat_strain").getAsInt();
        int rotorWear = waterJson.get("wear").getAsInt();
        List<CavitationRotorMaterial> rotors = Arrays.stream(CavitationRotorMaterial.values())
                .filter(rotor -> rotor != CavitationRotorMaterial.NITROGEN_EXTRACTION && rotor.stage() >= water.minimumStage())
                .toList();
        List<CollapseNozzleMaterial> nozzles = Arrays.stream(CollapseNozzleMaterial.values())
                .filter(nozzle -> nozzle.cavitationCompatible() && nozzle != CollapseNozzleMaterial.NITROGEN_SEPARATION)
                .toList();
        List<HeatCoreMaterial> cores = withNone(HeatCoreMaterial.values());
        List<ServoMaterial> servos = withNone(Arrays.stream(ServoMaterial.values()).filter(servo -> servo.stage() >= 6).toArray(ServoMaterial[]::new));
        for (CavitationRotorMaterial rotor : rotors) {
            for (CollapseNozzleMaterial nozzle : nozzles) {
                for (HeatCoreMaterial core : cores) {
                    for (ServoMaterial servo : servos) {
                        int stage = max(5, rotor.stage(), nozzle.stage(), core == null ? 0 : core.stage(), servo == null ? 0 : servo.stage());
                        add("cavitation_generator", "Cavitation Generator", stage, List.of(
                                "rotor:" + name(rotor),
                                "nozzle:" + name(nozzle),
                                "heat_core:" + (core == null ? "none" : name(core)),
                                "servo:" + (servo == null ? "none" : name(servo))
                        ), null, roller -> {
                            MachineStatAccumulator stats = MachineBaseStatCatalog.cavitationGenerator();
                            stats.apply(roller.machine(MachineType.CAVITATION_GENERATOR, 5));
                            MachineTraits rotorTraits = roller.part(MachinePartType.CAVITATION_ROTOR, MachineType.CAVITATION_GENERATOR, rotor.stage(), rotor.refinementPotential());
                            part(stats, ComponentBaseStatCatalog.cavitationRotor(rotor), rotorTraits);
                            int durability = Math.max(1, (int) Math.round(ComponentBaseStatCatalog.effectiveStats(
                                    ComponentBaseStatCatalog.cavitationRotor(rotor), rotorTraits).value(MachineStat.DURABILITY)));
                            part(stats, ComponentBaseStatCatalog.collapseNozzle(nozzle),
                                    roller.part(MachinePartType.COLLAPSE_NOZZLE, MachineType.CAVITATION_GENERATOR, nozzle.stage(), nozzle.refinementPotential()));
                            if (core != null) {
                                cavitationHeatCore(stats, ComponentBaseStatCatalog.effectiveStats(ComponentBaseStatCatalog.heatCore(core),
                                        roller.part(MachinePartType.HEAT_CORE, MachineType.SOLID_FUEL_BURNER, core.stage(), core.refinementPotential())));
                            }
                            if (servo != null) {
                                part(stats, ComponentBaseStatCatalog.servo(servo),
                                        roller.part(MachinePartType.SERVO, MachineType.METAL_PRESS, servo.stage(), servo.refinementPotential()));
                            }
                            if (stats.intValue(MachineStat.PROCESSING_LEVEL) < water.minimumStage()) {
                                return Outcome.blocked();
                            }
                            int ticks = stats.adjustedProcessingTicks(water.ticks());
                            double energy = Math.max(1.0, stats.generatedEnergyTotal(water.energy(), ticks)
                                    * Math.max(0.1, stats.value(MachineStat.EFFICIENCY)));
                            int strain = Math.max(0, (int) Math.round(heatStrain / Math.max(0.25, stats.value(MachineStat.TEMPERATURE_STABILITY))));
                            int cooling = 1 + (core == null ? 0 : Math.max(0, core.stage() / 2));
                            double[] steady = cavitationSteadyState(energy, ticks, strain, cooling);
                            int wear = Math.max(1, (int) Math.round(rotorWear / Math.max(0.25, stats.value(MachineStat.STABILITY))));
                            int recipesPerRotor = ceil(durability / (double) wear);
                            double fePerRotor = steady[1] * recipesPerRotor;
                            Outcome outcome = Outcome.running(steady[0], steady[0], fePerRotor)
                                    .withMetric("strainPenalty", 1.0 - steady[0] / (energy / ticks))
                                    .withMetric("rotorSeconds", fePerRotor / steady[0] / 20.0);
                            connectorMetrics(outcome, steady[0], fePerRotor, scenario.cavitationVent());
                            return outcome;
                        });
                    }
                }
            }
        }
    }

    /**
     * Output kept per attached connector tier. Stalling keeps every FE for later; venting loses what the connector cannot
     * take while the input is still consumed.
     */
    private static void connectorMetrics(Outcome outcome, double generation, double perInput, boolean vent) {
        for (EnergyConnectorTier tier : REPORTED_CONNECTORS) {
            connectorMetric(outcome, name(tier), generation, perInput, tier.transferRate(), vent);
        }
    }

    private static void connectorMetric(Outcome outcome, String key, double generation, double perInput, double cap, boolean vent) {
        outcome.withMetric("delivered_" + key, Math.min(generation, cap));
        outcome.withMetric("perInput_" + key, vent && generation > cap ? perInput * cap / generation : perInput);
    }

    /** AmmoniaFuelCellBlockEntity#adjustedTicks and #adjustedEnergy. */
    private void ammoniaFuelCells() throws IOException {
        FuelRecipe ammonia = FuelRecipe.of(recipe("ammonia_power_cycle", "ammonia_power"), chains.fuel("ammonia_fuel_cell/ammonia_power"))
                .withEnergy(scenario.recipeEnergy("ammonia_power_cycle/ammonia_power"));
        add("ammonia_fuel_cell", "Ammonia Fuel Cell", 6, List.of("membrane:standard"), null, roller -> {
            MachineStatAccumulator stats = MachineBaseStatCatalog.ammoniaFuelCell();
            roller.machine(MachineType.AMMONIA_FUEL_CELL, 6).modifiers().forEach(stats::apply);
            part(stats, ComponentBaseStatCatalog.fuelCellMembrane(),
                    roller.part(MachinePartType.FUEL_CELL_MEMBRANE, MachineType.AMMONIA_FUEL_CELL, 6, 0));
            int ticks = Math.max(1, ceil(ammonia.ticks() / stats.value(MachineStat.PROCESSING_SPEED)));
            double energy = Math.max(1L, (long) Math.floor(stats.generatedEnergyTotal(ammonia.energy(), ticks) * stats.value(MachineStat.EFFICIENCY)));
            return Outcome.recipe(ammonia, energy, ticks);
        });
    }

    /** VacuumCollapseGeneratorBlockEntity#effectiveRecipeEnergy, #adjustedProcessingTicks, #instabilityPressure. */
    private void vacuumCollapseGenerators() throws IOException {
        FuelRecipe catalyst = FuelRecipe.of(recipe("vacuum_collapse", "void_catalyst"), chains.fuel("vacuum_collapse_generator/void_catalyst"));
        JsonObject catalystJson = recipeJson("vacuum_collapse", "void_catalyst");
        double minimumStability = catalystJson.get("minimum_stability").getAsDouble();
        double instability = catalystJson.get("instability").getAsDouble();
        List<CollapseNozzleMaterial> nozzles = Arrays.stream(CollapseNozzleMaterial.values())
                .filter(CollapseNozzleMaterial::vacuumCollapseCompatible)
                .toList();
        for (VacuumCollapsePartMaterial chamber : VacuumCollapsePartMaterial.values()) {
            for (CollapseNozzleMaterial nozzle : nozzles) {
                for (VacuumCollapsePartMaterial stabilizer : VacuumCollapsePartMaterial.values()) {
                    int stage = max(7, chamber.stage(), nozzle.stage(), stabilizer.stage());
                    add("vacuum_collapse_generator", "Vacuum Collapse Generator", stage, List.of(
                            "void_chamber:" + name(chamber), "nozzle:" + name(nozzle), "stabilizer:" + name(stabilizer)
                    ), null, roller -> {
                        MachineStatAccumulator stats = MachineBaseStatCatalog.vacuumCollapseGenerator();
                        stats.apply(roller.machine(MachineType.VACUUM_COLLAPSE_GENERATOR, 7));
                        part(stats, ComponentBaseStatCatalog.vacuumCollapsePart(MachinePartType.VOID_CHAMBER, chamber),
                                roller.part(MachinePartType.VOID_CHAMBER, MachineType.VACUUM_COLLAPSE_GENERATOR, chamber.stage(), chamber.refinementPotential()));
                        part(stats, ComponentBaseStatCatalog.vacuumCollapseNozzle(nozzle),
                                roller.part(MachinePartType.COLLAPSE_NOZZLE, MachineType.CAVITATION_GENERATOR, nozzle.stage(), nozzle.refinementPotential()));
                        part(stats, ComponentBaseStatCatalog.vacuumCollapsePart(MachinePartType.DIMENSIONAL_STABILIZER, stabilizer),
                                roller.part(MachinePartType.DIMENSIONAL_STABILIZER, MachineType.VACUUM_COLLAPSE_GENERATOR, stabilizer.stage(), stabilizer.refinementPotential()));
                        if (stats.intValue(MachineStat.PROCESSING_LEVEL) < catalyst.minimumStage()
                                || stats.value(MachineStat.STABILITY) < minimumStability) {
                            return Outcome.blocked();
                        }
                        double pressure = Math.max(0.0, instability / Math.max(0.1, stats.value(MachineStat.STABILITY)))
                                + Math.max(0, catalyst.minimumStage() - nozzle.stage());
                        int ticks = Math.max(1, (int) Math.round(stats.adjustedProcessingTicks(catalyst.ticks()) * (1.0 + Math.min(0.75, pressure * 0.10))));
                        double energy = Math.max(1L, Math.round(stats.generatedEnergyTotal(catalyst.energy(), ticks)
                                * Math.max(0.1, stats.value(MachineStat.EFFICIENCY))
                                * (1.0 - Math.min(0.35, pressure * 0.08))));
                        Outcome outcome = Outcome.recipe(catalyst, energy, ticks);
                        connectorMetrics(outcome, energy / ticks, energy, scenario.vacuumVent());
                        return outcome;
                    });
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Gear as Potential Reactor fuel
    // ---------------------------------------------------------------------------------------------------------------

    /** A craftable RPG item a player might feed the Potential Reactor, rolled the way crafting rolls it. */
    private record GearItem(String id, String label, int stage, String recipe, ModifierEligibilityProfile profile, int materialRp,
            int masteryLevel, int sealTiers) {
        GearItem(String id, String label, int stage, String recipe, ModifierEligibilityProfile profile, int materialRp) {
            this(id, label, stage, recipe, profile, materialRp, 0, 0);
        }

        MachineTraits craft(RandomSource random) {
            MachineTraits rolled = MachineTraitRoller.roll(profile, stage, random);
            return new MachineTraits(rolled.rarity(), materialRp + rolled.refinementPotential(), rolled.modifiers());
        }
    }

    private static final List<GearItem> GEAR_ITEMS = List.of(
            new GearItem("flint_shovel_head", "Flint Shovel Head", 0, "1 flint + 1 wooden button", ModifierEligibilityProfiles.forToolHead(false), 0),
            new GearItem("wooden_tool_rod", "Wooden Tool Rod", 0, "3 sticks + 2 buttons", ModifierEligibilityProfiles.forMachine(MachineType.TOOL_ROD), 0),
            new GearItem("iron_tool_rod", "Iron Tool Rod", 1, "1 iron plate + 2 iron rods", ModifierEligibilityProfiles.forMachine(MachineType.TOOL_ROD), 0),
            new GearItem("iron_heat_core", "Iron Heat Core", 1, "2 iron coils, heat core, 2 coal, 4 redstone",
                    ModifierEligibilityProfiles.forMachinePart(MachinePartType.HEAT_CORE, MachineType.SOLID_FUEL_BURNER), HeatCoreMaterial.IRON.refinementPotential()),
            new GearItem("steel_heat_core", "Steel Heat Core (retired)", 4, "Stage 4 gear being replaced",
                    ModifierEligibilityProfiles.forMachinePart(MachinePartType.HEAT_CORE, MachineType.SOLID_FUEL_BURNER), HeatCoreMaterial.STEEL.refinementPotential()),
            new GearItem("titanium_heat_core", "Titanium Heat Core (retired)", 6, "Stage 6 gear being replaced",
                    ModifierEligibilityProfiles.forMachinePart(MachinePartType.HEAT_CORE, MachineType.SOLID_FUEL_BURNER), HeatCoreMaterial.TITANIUM.refinementPotential()),
            new GearItem("exotic_heat_core", "Exotic Heat Core (retired)", 8, "Stage 8 gear being replaced",
                    ModifierEligibilityProfiles.forMachinePart(MachinePartType.HEAT_CORE, MachineType.SOLID_FUEL_BURNER), HeatCoreMaterial.EXOTIC.refinementPotential()),
            masteryCrusher(0), masteryCrusher(1), masteryCrusher(2), masteryCrusher(3)
    );

    /** A retired Steel Crusher at Mastery level 50 with the given Ascendancy Seal tiers. */
    private static GearItem masteryCrusher(int sealTiers) {
        String seals = sealTiers == 0 ? "no Seals" : sealTiers + " Seal tier" + (sealTiers > 1 ? "s" : "");
        return new GearItem("steel_crusher_seal_" + sealTiers, "Steel Crusher, Mastery 50, " + seals, 4, "Retired Mastery machine",
                ModifierEligibilityProfiles.forMachine(MachineType.CRUSHER), 0, 50, sealTiers);
    }

    /**
     * Gear burned in an unrolled all-Steel Potential Reactor (PotentialReactorBlockEntity#nextWork, RecyclingData#rpgEnergyValue).
     * Fatigue rows show the FE multiplier when a player cycles through k different item ids.
     */
    private Map<String, Object> gearFuel(int trials) {
        MachineStatAccumulator reactor = reactorStats(Roller.UNROLLED, ReactorChamberMaterial.STEEL, RecoveryFilterMaterial.STEEL,
                ContainmentLiningMaterial.STEEL);
        List<Map<String, Object>> items = new ArrayList<>();
        for (GearItem item : GEAR_ITEMS) {
            RandomSource random = RandomSource.create(item.id().hashCode() * 131L + trials);
            double[] perItem = new double[trials];
            double[] fet = new double[trials];
            for (int trial = 0; trial < trials; trial++) {
                int traitValue = rpgEnergyValue(item.craft(random));
                int value;
                if (scenario.gearScale() != null || scenario.mastery() != null) {
                    double stageScale = scenario.gearScale() == null ? 1.0 : scenario.gearScale().at(item.stage());
                    double masteryScale = scenario.mastery() == null ? 1.0 : scenario.mastery().at(item.masteryLevel(), item.sealTiers());
                    value = (int) Math.round(traitValue * stageScale * masteryScale);
                } else {
                    value = RecyclingData.reactorFuelValue(traitValue, item.stage(), Math.max(0, item.masteryLevel() - 1), item.sealTiers());
                }
                int ticks = Math.max(100, Math.min(280, 80 + Math.max(1, item.stage()) * 15 + value / 240));
                int adjusted = reactor.adjustedProcessingTicks(ticks);
                perItem[trial] = value <= 0 ? 0.0 : reactorEnergy(reactor, value, adjusted);
                fet[trial] = perItem[trial] / adjusted;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", item.id());
            row.put("label", item.label());
            row.put("stage", item.stage());
            row.put("recipe", item.recipe());
            row.put("perItem", Distribution.of(perItem));
            row.put("fet", Distribution.of(fet));
            items.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reactor", "Potential Reactor, Steel chamber, filter and lining, unrolled");
        result.put("items", items);
        if (scenario.gearScale() != null) {
            result.put("stageScale", Map.of("stage0", scenario.gearScale().stage0(), "perStage", scenario.gearScale().perStage()));
        }
        if (scenario.mastery() != null) {
            result.put("mastery", Map.of("perLevel", scenario.mastery().perLevel(), "perSealTier", scenario.mastery().perSealTier()));
        }
        Scenario.Fatigue fatigue = scenario.fatigue();
        if (fatigue != null) {
            Map<String, Object> rotation = new LinkedHashMap<>();
            for (int distinct : new int[] {1, 2, 4, 8, 16}) {
                int repeats = Math.max(0, (int) Math.ceil(fatigue.window() / (double) distinct) - 1);
                rotation.put(String.valueOf(distinct), Math.max(fatigue.floor(), Math.pow(fatigue.factorPerRepeat(), repeats)));
            }
            result.put("fatigue", Map.of("window", fatigue.window(), "factorPerRepeat", fatigue.factorPerRepeat(), "floor", fatigue.floor(),
                    "multiplierByDistinctItems", rotation));
        }
        return result;
    }

    /** RecyclingData#rpgEnergyValue: rarity value, 120 FE per unspent RP, and a value per affix tier. */
    static int rpgEnergyValue(MachineTraits traits) {
        if (traits.rarity() == Rarity.UNIQUE) {
            return 0;
        }
        int value = switch (traits.rarity()) {
            case MAGIC -> 1200;
            case RARE -> 3600;
            default -> 0;
        };
        value += Math.max(0, traits.refinementPotential()) * 120;
        for (MachineModifier modifier : traits.modifiers()) {
            if (modifier.slot().isAffix()) {
                value += switch (Math.max(1, modifier.tier())) {
                    case 1 -> 500;
                    case 2 -> 1400;
                    case 3 -> 4000;
                    default -> 9000;
                };
            }
        }
        return value;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Shared generator math
    // ---------------------------------------------------------------------------------------------------------------

    private int clearGeneration(SolarPanelMaterial material) {
        return scenario.solar().panelClearGeneration().getOrDefault(material.name(), material.clearGeneration());
    }

    private static MachineStatAccumulator solarPanelStats(SolarPanelMaterial material, Roller roller) {
        MachineStatAccumulator stats = MachineBaseStatCatalog.solarPanel(material);
        stats.apply(roller.machine(MachineType.SOLAR_PANEL, material.stage()));
        return stats;
    }

    private static double panelScale(MachineStatAccumulator panel) {
        return panel.value(MachineStat.ENERGY_GENERATION) / Math.max(0.0001, panel.baseValue(MachineStat.ENERGY_GENERATION));
    }

    /** A solar panel's own 24-hour clear-weather output, used to pick the best of several crafted panels. */
    private double panelScore(MachineTraits traits, int stage) {
        SolarPanelMaterial material = Arrays.stream(SolarPanelMaterial.values()).filter(value -> value.stage() == stage).findFirst()
                .orElse(SolarPanelMaterial.CRUDE);
        MachineStatAccumulator stats = MachineBaseStatCatalog.solarPanel(material);
        stats.apply(traits);
        double raw = clearGeneration(material) * panelScale(stats) * Math.max(0.01, stats.value(MachineStat.EFFICIENCY));
        return ((dayTicks - peakTicks) * raw + peakTicks * raw * Math.max(1.0, stats.value(MachineStat.PEAK_SOLAR_GENERATION))) / DAY_TICKS;
    }

    private static double arrayPanel(MachineStatAccumulator controller, double generation, double controllerEfficiency) {
        return generation <= 0.0 ? 0.0 : Math.max(0.0, controller.generatedEnergyTotal(generation, 1)) * controllerEfficiency;
    }

    /** BulkSpeedState at its 100-process cap, which a continuously running generator reaches. */
    private static void bulkSpeed(MachineStatAccumulator stats, MachineTraits traits) {
        if (traits.hasBehavior(MachineBehavior.BULK_SPEED)) {
            stats.apply(new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.PROCESSING_SPEED, ModifierOperation.INCREASED_PERCENT,
                    BULK_SPEED_STEADY_PERCENT));
        }
    }

    /** CavitationGeneratorBlockEntity#applyHeatCoreStats: only heat stats transfer from the core. */
    private static void cavitationHeatCore(MachineStatAccumulator stats, MachineStatAccumulator core) {
        for (MachineStat stat : List.of(MachineStat.HEAT_TRANSFER, MachineStat.HEAT_ISOLATION, MachineStat.COOLING_RATE,
                MachineStat.TEMPERATURE_STABILITY, MachineStat.OVERHEAT_TOLERANCE)) {
            double value = core.value(stat);
            if (Math.abs(value - 1.0) > 0.0001) {
                stats.apply(new MachineModifier(ModifierSlot.IMPLICIT, stat, ModifierOperation.MORE, value));
            }
        }
        double maxTemperature = core.value(MachineStat.MAX_TEMPERATURE);
        if (Math.abs(maxTemperature) > 0.0001) {
            stats.apply(new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.MAX_TEMPERATURE, ModifierOperation.ADD, maxTemperature));
        }
    }

    /**
     * Runs recipes back to back until heat strain settles. Cooling happens before generation each tick, strain is
     * added when a recipe finishes, and a recipe cannot start at the strain cap.
     *
     * @return average FE/t including cooling waits, and FE per recipe
     */
    private static double[] cavitationSteadyState(double recipeEnergy, int ticks, int strainPerRecipe, int cooling) {
        double strain = 0.0;
        double energy = 0.0;
        long elapsed = 0;
        int recipes = 0;
        for (int cycle = 0; cycle < 400; cycle++) {
            long wait = 0;
            if (strain >= 1000) {
                wait = (long) Math.ceil((strain - 999) / cooling);
                strain = Math.max(0.0, strain - wait * cooling);
            }
            int coolingTicks = (int) Math.min(ticks, Math.floor(strain / cooling));
            double strainSum = coolingTicks * strain - cooling * coolingTicks * (coolingTicks + 1) / 2.0;
            double delivered = recipeEnergy / ticks * (ticks - strainSum / 2000.0);
            strain = Math.min(1000.0, Math.max(0.0, strain - (double) cooling * ticks) + strainPerRecipe);
            if (cycle >= 200) {
                energy += delivered;
                elapsed += ticks + wait;
                recipes++;
            }
        }
        return new double[] {energy / elapsed, energy / recipes};
    }

    private interface RecipeEnergy {
        double[] energyAndTicks(FuelRecipe recipe);
    }

    private static Outcome bestRecipe(List<FuelRecipe> recipes, RecipeEnergy energy) {
        Outcome best = Outcome.blocked();
        for (FuelRecipe recipe : recipes) {
            double[] result = energy.energyAndTicks(recipe);
            Outcome outcome = Outcome.recipe(recipe, result[0], (int) result[1]);
            if (!best.runs() || outcome.fet() > best.fet()) {
                best = outcome;
            }
        }
        return best;
    }

    private static void part(MachineStatAccumulator target, ComponentBaseStatCatalog.Profile profile, MachineTraits traits) {
        ComponentBaseStatCatalog.applyContribution(target, profile, traits);
    }

    /** Minecraft's isDay() for a clear Overworld: skyDarken < 4 (Level#updateSkyBrightness, DimensionType#timeOfDay). */
    private static int countDaylightTicks() {
        int day = 0;
        for (int tick = 0; tick < DAY_TICKS; tick++) {
            double fraction = frac(tick / 24000.0 - 0.25);
            double smoothing = 0.5 - Math.cos(fraction * Math.PI) / 2.0;
            float timeOfDay = (float) (fraction * 2.0 + smoothing) / 3.0F;
            double brightness = 0.5 + 2.0 * clamp(Math.cos(timeOfDay * (float) (Math.PI * 2)), -0.25, 0.25);
            if ((int) ((1.0 - brightness) * 11.0) < 4) {
                day++;
            }
        }
        return day;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Player behaviors
    // ---------------------------------------------------------------------------------------------------------------

    enum Behavior {
        /** Craft each machine and part once and install what rolled. */
        FRESH("fresh"),
        /** Craft five of each machine and part and keep the one that adds the most output on its own. */
        BEST_OF("best_of_5"),
        /** Craft once, then spend the item's Refinement Potential on output-raising refinement. */
        REFINED("refined");

        private final String id;

        Behavior(String id) {
            this.id = id;
        }

        String id() {
            return id;
        }
    }

    private record Call(boolean machine, MachineType machineType, MachinePartType partType, int stage, int materialRp) {
        ModifierEligibilityProfile profile() {
            return machine ? ModifierEligibilityProfiles.forMachine(machineType) : ModifierEligibilityProfiles.forMachinePart(partType, machineType);
        }

        /** CraftedTraitOutputs#applyRolledTraits: parts add material RP and keep only rarity, RP and modifiers. */
        MachineTraits craft(RandomSource random) {
            MachineTraits rolled = MachineTraitRoller.roll(profile(), stage, random);
            return machine ? rolled : new MachineTraits(rolled.rarity(), materialRp + rolled.refinementPotential(), rolled.modifiers());
        }

        String owner() {
            return machine ? name(machineType) : name(partType);
        }
    }

    /** Rolls traits for one trial under a behavior, logging every affix it hands out. */
    private final class TrialRoller implements Roller {
        private final Behavior behavior;
        private final RandomSource random;
        private final List<String> log = new ArrayList<>();
        private List<MachineTraits> script = List.of();
        private int index;

        TrialRoller(Behavior behavior, RandomSource random) {
            this.behavior = behavior;
            this.random = random;
        }

        Outcome evaluate(Model model) {
            log.clear();
            if (behavior == Behavior.FRESH) {
                script = List.of();
                index = 0;
                return model.evaluate(this);
            }
            List<Call> calls = record(model);
            List<MachineTraits> chosen = new ArrayList<>();
            for (int position = 0; position < calls.size(); position++) {
                chosen.add(choose(model, calls, position));
            }
            script = chosen;
            index = 0;
            return model.evaluate(this);
        }

        private MachineTraits choose(Model model, List<Call> calls, int position) {
            Call call = calls.get(position);
            if (behavior == Behavior.REFINED) {
                return refine(call, call.craft(random));
            }
            boolean arrayPanel = call.machine() && call.machineType() == MachineType.SOLAR_PANEL && calls.size() > 1;
            MachineTraits best = null;
            double bestScore = Double.NEGATIVE_INFINITY;
            for (int candidate = 0; candidate < BEST_OF; candidate++) {
                MachineTraits traits = call.craft(random);
                double score = arrayPanel ? panelScore(traits, call.stage()) : isolated(model, calls.size(), position, traits);
                if (score > bestScore) {
                    best = traits;
                    bestScore = score;
                }
            }
            return best;
        }

        /**
         * Spends all Refinement Potential through RefinementEngine the way a player chasing output would: fill affix
         * slots, catalyse Magic to Rare, then upgrade, each step with the first lens (Power, Efficiency, Kinetic) the
         * item can use. Consumable items are assumed available; only RP limits the item.
         */
        private MachineTraits refine(Call call, MachineTraits crafted) {
            MachineTraits traits = crafted;
            ModifierEligibilityProfile profile = call.profile();
            for (int step = 0; step < 64 && traits.refinementPotential() > 0; step++) {
                MachineTraits next = null;
                for (RefinementOperation operation : refinementPlan(traits)) {
                    next = refineStep(profile, traits, operation, call.stage());
                    if (next != null) {
                        break;
                    }
                }
                if (next == null) {
                    break;
                }
                traits = next;
            }
            return traits;
        }

        private MachineTraits refineStep(ModifierEligibilityProfile profile, MachineTraits traits, RefinementOperation operation, int stage) {
            boolean lensable = operation == RefinementOperation.ADD_MODIFIER || operation == RefinementOperation.UPGRADE_RANDOM_MODIFIER;
            List<Set<ModifierLensTag>> lenses = lensable ? REFINEMENT_LENSES : List.of(Set.of());
            for (Set<ModifierLensTag> lens : lenses) {
                RefinementResult result = RefinementEngine.apply(profile, traits, operation, stage, RefinementSelection.none(),
                        RefinementModifier.NONE, lens, random);
                if (result.success()) {
                    return result.traits();
                }
            }
            return null;
        }


        /** Output with only this component rolled, so a candidate is judged the way its tooltip would be. */
        private double isolated(Model model, int size, int position, MachineTraits traits) {
            List<MachineTraits> probe = new ArrayList<>();
            for (int slot = 0; slot < size; slot++) {
                probe.add(slot == position ? traits : MachineTraits.EMPTY);
            }
            ScriptRoller roller = new ScriptRoller(probe);
            Outcome outcome = model.evaluate(roller);
            return outcome.runs() ? outcome.averageFet() : 0.0;
        }

        private List<Call> record(Model model) {
            List<Call> calls = new ArrayList<>();
            model.evaluate(new Roller() {
                @Override
                public MachineTraits machine(MachineType type, int stage) {
                    calls.add(new Call(true, type, null, stage, 0));
                    return MachineTraits.EMPTY;
                }

                @Override
                public MachineTraits part(MachinePartType partType, MachineType machineType, int stage, int materialRp) {
                    calls.add(new Call(false, machineType, partType, stage, materialRp));
                    return MachineTraits.EMPTY;
                }
            });
            return calls;
        }

        @Override
        public MachineTraits machine(MachineType type, int stage) {
            return next(new Call(true, type, null, stage, 0));
        }

        @Override
        public MachineTraits part(MachinePartType partType, MachineType machineType, int stage, int materialRp) {
            return next(new Call(false, machineType, partType, stage, materialRp));
        }

        /** Scripted choices first; calls past the script (a rolled range adding panels) use the same behavior per call. */
        private MachineTraits next(Call call) {
            MachineTraits traits;
            if (index < script.size()) {
                traits = script.get(index);
            } else if (behavior == Behavior.FRESH) {
                traits = call.craft(random);
            } else {
                traits = choose(model -> Outcome.blocked(), List.of(call, call), 0);
            }
            index++;
            if (log.size() < 64) {
                for (MachineModifier modifier : traits.modifiers()) {
                    log.add(call.owner() + ":" + modifier.affixId() + ":T" + modifier.tier() + ":" + Math.round(modifier.value()));
                }
                for (MachineBehavior granted : traits.behaviors()) {
                    log.add(call.owner() + ":behavior_" + name(granted));
                }
            }
            return traits;
        }
    }

    private static final List<Set<ModifierLensTag>> REFINEMENT_LENSES =
            List.of(Set.of(ModifierLensTag.POWER), Set.of(ModifierLensTag.EFFICIENCY), Set.of(ModifierLensTag.KINETIC), Set.of());

    /** Operations to try, in order, for the item's current rarity. */
    private static List<RefinementOperation> refinementPlan(MachineTraits traits) {
        return switch (traits.rarity()) {
            case NORMAL -> List.of(RefinementOperation.ADD_MODIFIER);
            case MAGIC -> traits.refinementPotential() >= 5
                    ? List.of(RefinementOperation.ADD_MODIFIER, RefinementOperation.ASCENSION_CATALYST, RefinementOperation.UPGRADE_RANDOM_MODIFIER)
                    : List.of(RefinementOperation.ADD_MODIFIER, RefinementOperation.UPGRADE_RANDOM_MODIFIER);
            default -> List.of(RefinementOperation.ADD_MODIFIER, RefinementOperation.UPGRADE_RANDOM_MODIFIER);
        };
    }

    private record ScriptRoller(List<MachineTraits> script, int[] index) implements Roller {
        ScriptRoller(List<MachineTraits> script) {
            this(script, new int[1]);
        }

        @Override
        public MachineTraits machine(MachineType type, int stage) {
            return next();
        }

        @Override
        public MachineTraits part(MachinePartType partType, MachineType machineType, int stage, int materialRp) {
            return next();
        }

        private MachineTraits next() {
            int position = index[0]++;
            return position < script.size() ? script.get(position) : MachineTraits.EMPTY;
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Running and reporting
    // ---------------------------------------------------------------------------------------------------------------

    private interface Model {
        Outcome evaluate(Roller roller);
    }

    private record Setup(String generator, String generatorName, int stage, List<String> components, ChainCosts.Fuel fixedFuel, Model model) {
        String label() {
            return generatorName + " [" + String.join(", ", components) + "]";
        }
    }

    private void add(String generator, String generatorName, int stage, List<String> components, ChainCosts.Fuel fixedFuel, Model model) {
        setups.add(new Setup(generator, generatorName, stage, components, fixedFuel, model));
    }

    private Map<String, Object> run(Behavior behavior, int trials) {
        List<Map<String, Object>> setupRows = new ArrayList<>();
        Map<Setup, double[]> averages = new LinkedHashMap<>();
        Map<Setup, Double> baselines = new LinkedHashMap<>();
        Map<String, Pool> pools = new TreeMap<>();
        for (Setup setup : setups) {
            Outcome baseline = setup.model().evaluate(Roller.UNROLLED);
            RandomSource random = RandomSource.create(setup.label().hashCode() * 31L + trials + behavior.ordinal() * 7919L);
            TrialRoller roller = new TrialRoller(behavior, random);
            List<List<String>> logs = new ArrayList<>();
            double[] fet = new double[trials];
            double[] average = new double[trials];
            double[] perInput = new double[trials];
            double[] net = new double[trials];
            Map<String, double[]> metrics = new TreeMap<>();
            int blocked = 0;
            for (int trial = 0; trial < trials; trial++) {
                Outcome outcome = roller.evaluate(setup.model());
                logs.add(List.copyOf(roller.log));
                if (!outcome.runs()) {
                    blocked++;
                }
                fet[trial] = outcome.fet();
                average[trial] = outcome.averageFet();
                perInput[trial] = outcome.perInput();
                net[trial] = netFet(outcome, fuelFor(setup, outcome));
                for (Map.Entry<String, Double> metric : outcome.metrics().entrySet()) {
                    metrics.computeIfAbsent(metric.getKey(), key -> filled(trials))[trial] = metric.getValue();
                }
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("generator", setup.generator());
            row.put("name", setup.generatorName());
            row.put("stage", setup.stage());
            row.put("components", setup.components());
            row.put("blockedFraction", blocked / (double) trials);
            ChainCosts.Fuel baselineFuel = fuelFor(setup, baseline);
            Map<String, Object> base = new LinkedHashMap<>();
            base.put("fet", baseline.fet());
            base.put("averageFet", baseline.averageFet());
            base.put("perInput", baseline.perInput());
            base.put("net", netFet(baseline, baselineFuel));
            base.put("input", baselineFuel == null ? null : baselineFuel.id());
            base.put("inputLabel", baselineFuel == null ? null : baselineFuel.input());
            base.put("chainCost", baselineFuel == null ? null : baselineFuel.chainCost());
            base.put("metrics", baseline.metrics());
            row.put("baseline", base);
            row.put("fet", Distribution.of(fet));
            row.put("averageFet", Distribution.of(average));
            row.put("perInput", Distribution.of(perInput));
            row.put("net", Distribution.of(net));
            Map<String, Object> metricRows = new TreeMap<>();
            metrics.forEach((key, values) -> metricRows.put(key, Distribution.of(values)));
            row.put("metrics", metricRows);
            averages.put(setup, average);
            baselines.put(setup, baseline.runs() ? baseline.averageFet() : 0.0);
            int best = argMax(average);
            row.put("bestTrial", Map.of("averageFet", average[best], "affixes", logs.get(best)));
            setupRows.add(row);

            pools.computeIfAbsent(setup.generator() + "@" + setup.stage(), key -> new Pool(setup.generator(), setup.generatorName(), setup.stage()))
                    .add(fet, average, net, baseline, logs);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("trials", trials);
        result.put("pools", pools.values().stream().map(Pool::summary).toList());
        result.put("ladder", ladder(averages, baselines));
        result.put("setups", setupRows);
        return result;
    }

    private static double[] filled(int length) {
        double[] values = new double[length];
        Arrays.fill(values, Double.NaN);
        return values;
    }

    /**
     * For each stage, the sustained (non-solar, non-burst) setup with the best unrolled output, and how often a craft
     * of it beats the next stage's best unrolled setup. Solar aggregates up to 168 blocks, and the Aethergold rotor is
     * a burst consumable, so neither anchors a stage.
     */
    private static List<Map<String, Object>> ladder(Map<Setup, double[]> averages, Map<Setup, Double> baselines) {
        Map<Integer, Setup> best = new TreeMap<>();
        for (Setup setup : averages.keySet()) {
            if (setup.generator().startsWith("solar") || setup.components().contains("rotor:aethergold")) {
                continue;
            }
            Setup current = best.get(setup.stage());
            if (current == null || baselines.get(setup) > baselines.get(current)) {
                best.put(setup.stage(), setup);
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        List<Integer> stages = new ArrayList<>(best.keySet());
        for (int index = 0; index < stages.size(); index++) {
            Setup setup = best.get(stages.get(index));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("stage", setup.stage());
            row.put("setup", setup.label());
            row.put("baseline", baselines.get(setup));
            row.put("distribution", Distribution.of(averages.get(setup)));
            if (index + 1 < stages.size()) {
                Setup next = best.get(stages.get(index + 1));
                double nextBaseline = baselines.get(next);
                row.put("nextStage", next.stage());
                row.put("nextSetup", next.label());
                row.put("nextBaseline", nextBaseline);
                row.put("beatsNextBaseline", Arrays.stream(averages.get(setup)).filter(value -> value >= nextBaseline).count()
                        / (double) averages.get(setup).length);
            }
            rows.add(row);
        }
        return rows;
    }

    private ChainCosts.Fuel fuelFor(Setup setup, Outcome outcome) {
        if (setup.fixedFuel() != null) {
            return setup.fixedFuel();
        }
        return outcome.fuel();
    }

    /** FE/t left after paying, at base machine stats, for the chain that produced the consumed input. */
    private static double netFet(Outcome outcome, ChainCosts.Fuel fuel) {
        if (!outcome.runs()) {
            return 0.0;
        }
        if (fuel == null || Double.isNaN(outcome.perInput()) || outcome.perInput() <= 0.0) {
            return outcome.averageFet();
        }
        return outcome.averageFet() * (outcome.perInput() - fuel.chainCost()) / outcome.perInput();
    }

    private static final class Pool {
        private final String generator;
        private final String name;
        private final int stage;
        private final List<double[]> fet = new ArrayList<>();
        private final List<double[]> average = new ArrayList<>();
        private final List<double[]> net = new ArrayList<>();
        private double baselineMin = Double.POSITIVE_INFINITY;
        private double baselineMax = Double.NEGATIVE_INFINITY;
        private int setups;
        private int trialsSeen;
        private int topTrials;
        private final Map<String, Integer> affixTrials = new TreeMap<>();
        private final Map<String, Integer> affixTopTrials = new TreeMap<>();

        Pool(String generator, String name, int stage) {
            this.generator = generator;
            this.name = name;
            this.stage = stage;
        }

        void add(double[] fetValues, double[] averageValues, double[] netValues, Outcome baseline, List<List<String>> logs) {
            double cutoff = percentile(averageValues, 95);
            for (int trial = 0; trial < logs.size(); trial++) {
                boolean top = averageValues[trial] >= cutoff && averageValues[trial] > 0.0;
                trialsSeen++;
                if (top) {
                    topTrials++;
                }
                for (String affix : new HashSet<>(logs.get(trial).stream().map(Pool::affixKey).toList())) {
                    affixTrials.merge(affix, 1, Integer::sum);
                    if (top) {
                        affixTopTrials.merge(affix, 1, Integer::sum);
                    }
                }
            }
            fet.add(fetValues);
            average.add(averageValues);
            net.add(netValues);
            if (baseline.runs()) {
                baselineMin = Math.min(baselineMin, baseline.averageFet());
                baselineMax = Math.max(baselineMax, baseline.averageFet());
            }
            setups++;
        }

        Map<String, Object> summary() {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("generator", generator);
            row.put("name", name);
            row.put("stage", stage);
            row.put("setups", setups);
            row.put("baselineMin", Double.isInfinite(baselineMin) ? 0.0 : baselineMin);
            row.put("baselineMax", Double.isInfinite(baselineMax) ? 0.0 : baselineMax);
            row.put("fet", Distribution.of(concat(fet)));
            row.put("averageFet", Distribution.of(concat(average)));
            row.put("net", Distribution.of(concat(net)));
            row.put("topDrivers", topDrivers());
            return row;
        }

        /** Affixes most over-represented in each setup's top 5% of trials, ranked by top share times log lift. */
        private List<Map<String, Object>> topDrivers() {
            List<Map<String, Object>> drivers = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : affixTopTrials.entrySet()) {
                double topShare = entry.getValue() / (double) topTrials;
                double allShare = affixTrials.get(entry.getKey()) / (double) trialsSeen;
                Map<String, Object> driver = new LinkedHashMap<>();
                driver.put("affix", entry.getKey());
                driver.put("topShare", topShare);
                driver.put("allShare", allShare);
                driver.put("lift", topShare / allShare);
                drivers.add(driver);
            }
            drivers.sort(Comparator.comparingDouble(
                    (Map<String, Object> driver) -> -(double) driver.get("topShare") * Math.log((double) driver.get("lift"))));
            return drivers.subList(0, Math.min(8, drivers.size()));
        }

        private static String affixKey(String logEntry) {
            String[] parts = logEntry.split(":");
            return parts.length >= 2 ? parts[0] + ":" + parts[1] : logEntry;
        }

        private static double[] concat(List<double[]> parts) {
            return parts.stream().flatMapToDouble(Arrays::stream).toArray();
        }
    }

    record Outcome(boolean runs, double fet, double averageFet, double perInput, ChainCosts.Fuel fuel, Map<String, Double> metrics) {
        static Outcome running(double fet, double averageFet, double perInput) {
            return new Outcome(true, fet, averageFet, perInput, null, new LinkedHashMap<>());
        }

        static Outcome recipe(FuelRecipe recipe, double energy, int ticks) {
            double fet = energy / Math.max(1, ticks);
            return new Outcome(true, fet, fet, energy, recipe.fuel(), new LinkedHashMap<>());
        }

        static Outcome blocked() {
            return new Outcome(false, 0.0, 0.0, Double.NaN, null, new LinkedHashMap<>());
        }

        Outcome withMetric(String key, double value) {
            metrics.put(key, value);
            return this;
        }
    }

    /** Supplies traits to a generator model: rolled, chosen by a behavior, or empty for the unmodified baseline. */
    private interface Roller {
        Roller UNROLLED = new Roller() {
            @Override
            public MachineTraits machine(MachineType type, int stage) {
                return MachineTraits.EMPTY;
            }

            @Override
            public MachineTraits part(MachinePartType partType, MachineType machineType, int stage, int materialRp) {
                return MachineTraits.EMPTY;
            }
        };

        MachineTraits machine(MachineType type, int stage);

        /** {@code materialRp} is the part material's own Refinement Potential, added to the rolled amount on craft. */
        MachineTraits part(MachinePartType partType, MachineType machineType, int stage, int materialRp);
    }

    static final class Distribution {
        private static final double[] PERCENTILES = {1, 5, 10, 25, 50, 75, 90, 95, 99};

        static Map<String, Object> of(double[] values) {
            double[] finite = Arrays.stream(values).filter(Double::isFinite).sorted().toArray();
            Map<String, Object> row = new LinkedHashMap<>();
            if (finite.length == 0) {
                return row;
            }
            row.put("min", finite[0]);
            for (double percentile : PERCENTILES) {
                row.put("p" + (int) percentile, finite[Math.min(finite.length - 1, (int) Math.ceil(percentile / 100.0 * finite.length) - 1)]);
            }
            row.put("max", finite[finite.length - 1]);
            row.put("mean", Arrays.stream(finite).average().orElse(0.0));
            return row;
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Data
    // ---------------------------------------------------------------------------------------------------------------

    record Recipe(String id, double energy, int ticks, int minimumStage) {
    }

    /** A generator recipe with the chain cost and availability of what it consumes. */
    record FuelRecipe(String id, double energy, int ticks, int minimumStage, ChainCosts.Fuel fuel) {
        static FuelRecipe of(Recipe recipe, ChainCosts.Fuel fuel) {
            return new FuelRecipe(recipe.id(), recipe.energy(), recipe.ticks(), recipe.minimumStage(), fuel);
        }

        FuelRecipe withEnergy(Double override) {
            return override == null ? this : new FuelRecipe(id, override, ticks, minimumStage, fuel);
        }

        boolean obtainable() {
            return fuel == null || fuel.obtainable();
        }

        int availableFromStage() {
            return fuel == null ? 0 : fuel.availableFromStage();
        }
    }

    private List<Recipe> recipes(String directory) throws IOException {
        List<Recipe> recipes = new ArrayList<>();
        try (var files = Files.list(recipeRoot.resolve(directory))) {
            for (Path file : files.sorted().toList()) {
                String id = file.getFileName().toString().replace(".json", "");
                recipes.add(recipe(directory, id));
            }
        }
        return recipes;
    }

    private Recipe recipe(String directory, String id) throws IOException {
        JsonObject json = recipeJson(directory, id);
        int minimum = 0;
        for (String key : List.of("minimum_material_stage", "minimum_rotor_stage", "minimum_chamber_stage", "minimum_membrane_stage")) {
            if (json.has(key)) {
                minimum = json.get(key).getAsInt();
            }
        }
        return new Recipe(id, json.get("energy").getAsDouble(), json.get("processing_ticks").getAsInt(), minimum);
    }

    private JsonObject recipeJson(String directory, String id) throws IOException {
        try (Reader reader = Files.newBufferedReader(recipeRoot.resolve(directory).resolve(id + ".json"))) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static JsonObject readJson(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    /** Base-stat FE spent producing one generator input, from tools/balance/energy-chain-costs.json. */
    record ChainCosts(Map<String, Fuel> fuels) {
        record Fuel(String id, String input, double baseAmount, String stat, double chainCost, boolean obtainable, int availableFromStage, String chain) {
            static Fuel parse(String id, JsonObject json) {
                return new Fuel(
                        id,
                        json.get("input").getAsString(),
                        json.has("base_amount") ? json.get("base_amount").getAsDouble() : 0.0,
                        json.has("stat") ? json.get("stat").getAsString() : null,
                        json.get("chain_fe").getAsDouble(),
                        !json.has("obtainable") || json.get("obtainable").getAsBoolean(),
                        json.has("available_from_stage") ? json.get("available_from_stage").getAsInt() : 0,
                        json.has("chain") ? json.get("chain").getAsString() : ""
                );
            }
        }

        static ChainCosts load(Path path) throws IOException {
            Map<String, Fuel> fuels = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : readJson(path).getAsJsonObject("inputs").entrySet()) {
                fuels.put(entry.getKey(), Fuel.parse(entry.getKey(), entry.getValue().getAsJsonObject()));
            }
            return new ChainCosts(fuels);
        }

        /** Whether a player at {@code stage} can source this input at all. */
        boolean available(String id, int stage) {
            Fuel fuel = fuel(id);
            return fuel.obtainable() && stage >= fuel.availableFromStage();
        }

        Fuel fuel(String id) {
            Fuel fuel = fuels.get(id);
            if (fuel == null) {
                throw new IllegalStateException("Missing chain cost for " + id + " in tools/balance/energy-chain-costs.json");
            }
            return fuel;
        }
    }

    /** A what-if from tools/balance/energy-scenarios.json; the empty scenario is the game as coded. */
    record Scenario(
            String id,
            String label,
            JsonObject settings,
            double solidFuelFePerBurnTick,
            Map<String, Double> recipeEnergies,
            Solar solar,
            List<FuelRecipe> corrosionRecipes,
            Fatigue fatigue,
            GearScale gearScale,
            boolean cavitationVent,
            boolean vacuumVent,
            Mastery mastery
    ) {
        /** PotentialReactorBlockEntity: 15 remembered burns, x0.75 per repeat, floor 10%. */
        static final Fatigue CURRENT_FATIGUE = new Fatigue(16, 0.75, 0.10);
        /** Cavitation and Vacuum Collapse vent FE their buffer cannot hold instead of pausing. */
        static final Scenario CURRENT = new Scenario("current", "Current", new JsonObject(), 0, Map.of(), Solar.CURRENT, null, CURRENT_FATIGUE,
                null, true, true, null);

        /** Reactor gear-fuel bonus for a machine's Mastery: {@code (1 + level * perLevel) * (1 + sealTiers * perSealTier)}. */
        record Mastery(double perLevel, double perSealTier) {
            double at(int level, int sealTiers) {
                return (1.0 + level * perLevel) * (1.0 + sealTiers * perSealTier);
            }
        }

        private static Map<String, Integer> intMap(JsonObject json, String key) {
            Map<String, Integer> values = new LinkedHashMap<>();
            if (json.has(key)) {
                json.getAsJsonObject(key).entrySet().forEach(entry -> values.put(entry.getKey(), entry.getValue().getAsInt()));
            }
            return values;
        }

        record Solar(Integer extenderRangeBonus, Double extenderGeneration, boolean controllerFlatOnce, int maxRange,
                Map<String, Integer> panelClearGeneration) {
            /** SolarArrayControllerBlockEntity: flat bonus once per array, MAX_RANGE 2. */
            static final Solar CURRENT = new Solar(null, null, true, 2, Map.of());
        }

        /** Reactor gear-fuel value multiplier: {@code stage0 + perStage * stage}, so obsolete late gear is worth more than junk. */
        record GearScale(double stage0, double perStage) {
            double at(int stage) {
                return stage0 + perStage * Math.max(0, stage);
            }
        }

        /** Reactor gear-fuel penalty: each repeat of an item id within the window multiplies its FE, down to a floor. */
        record Fatigue(int window, double factorPerRepeat, double floor) {
        }

        Double recipeEnergy(String key) {
            return recipeEnergies.get(key);
        }

        static List<Scenario> load(Path path) throws IOException {
            List<Scenario> scenarios = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : readJson(path).getAsJsonObject("scenarios").entrySet()) {
                scenarios.add(parse(entry.getKey(), entry.getValue().getAsJsonObject()));
            }
            return scenarios;
        }

        private static Scenario parse(String id, JsonObject json) {
            Map<String, Double> energies = new LinkedHashMap<>();
            if (json.has("recipe_energy")) {
                json.getAsJsonObject("recipe_energy").entrySet().forEach(entry -> energies.put(entry.getKey(), entry.getValue().getAsDouble()));
            }
            Solar solar = Solar.CURRENT;
            if (json.has("solar")) {
                JsonObject value = json.getAsJsonObject("solar");
                solar = new Solar(
                        value.has("extender_range_bonus") ? value.get("extender_range_bonus").getAsInt() : null,
                        value.has("extender_generation") ? value.get("extender_generation").getAsDouble() : null,
                        !value.has("controller_flat_once") || value.get("controller_flat_once").getAsBoolean(),
                        value.has("max_range") ? value.get("max_range").getAsInt() : Solar.CURRENT.maxRange(),
                        intMap(value, "panel_clear_generation")
                );
            }
            List<FuelRecipe> corrosion = null;
            if (json.has("corrosion_recipes")) {
                corrosion = new ArrayList<>();
                for (JsonElement element : json.getAsJsonArray("corrosion_recipes")) {
                    JsonObject recipe = element.getAsJsonObject();
                    String recipeId = recipe.get("id").getAsString();
                    corrosion.add(new FuelRecipe(recipeId, recipe.get("energy").getAsDouble(), recipe.get("ticks").getAsInt(), 0,
                            ChainCosts.Fuel.parse("corrosion_cell/" + recipeId, recipe)));
                }
            }
            Fatigue fatigue = Scenario.CURRENT_FATIGUE;
            if (json.has("reactor_fatigue")) {
                JsonObject value = json.getAsJsonObject("reactor_fatigue");
                fatigue = new Fatigue(value.get("window").getAsInt(), value.get("factor_per_repeat").getAsDouble(), value.get("floor").getAsDouble());
            }
            GearScale gearScale = null;
            if (json.has("reactor_gear_stage_scale")) {
                JsonObject value = json.getAsJsonObject("reactor_gear_stage_scale");
                gearScale = new GearScale(value.get("stage_0").getAsDouble(), value.get("per_stage").getAsDouble());
            }
            return new Scenario(
                    id,
                    json.has("label") ? json.get("label").getAsString() : id.substring(0, 1).toUpperCase(Locale.ROOT) + id.substring(1),
                    json,
                    json.has("solid_fuel_fe_per_burn_tick") ? json.get("solid_fuel_fe_per_burn_tick").getAsDouble() : 0,
                    energies,
                    solar,
                    corrosion,
                    fatigue,
                    gearScale,
                    !json.has("cavitation_overflow") || "vent".equals(json.get("cavitation_overflow").getAsString()),
                    !json.has("vacuum_collapse_overflow") || "vent".equals(json.get("vacuum_collapse_overflow").getAsString()),
                    json.has("reactor_mastery")
                            ? new Mastery(json.getAsJsonObject("reactor_mastery").get("per_level").getAsDouble(),
                                    json.getAsJsonObject("reactor_mastery").get("per_seal_tier").getAsDouble())
                            : null
            );
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------------------------

    private static int argMax(double[] values) {
        int best = 0;
        for (int index = 1; index < values.length; index++) {
            if (values[index] > values[best]) {
                best = index;
            }
        }
        return best;
    }

    private static double percentile(double[] values, double percentile) {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[Math.min(sorted.length - 1, Math.max(0, (int) Math.ceil(percentile / 100.0 * sorted.length) - 1))];
    }

    @SafeVarargs
    private static <T> List<T> withNone(T... values) {
        List<T> list = new ArrayList<>();
        list.add(null);
        list.addAll(List.of(values));
        return list;
    }

    private static int max(int... values) {
        return Arrays.stream(values).max().orElse(0);
    }

    private static int ceil(double value) {
        return (int) Math.ceil(value);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double frac(double value) {
        return value - Math.floor(value);
    }

    private static String name(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }
}
