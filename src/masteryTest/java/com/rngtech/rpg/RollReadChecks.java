package com.rngtech.rpg;

import com.rngtech.content.calibration.CalibrationGearMaterial;
import com.rngtech.content.energy.CathodeMaterial;
import com.rngtech.content.energy.CavitationRotorMaterial;
import com.rngtech.content.energy.CollapseNozzleMaterial;
import com.rngtech.content.energy.ContainmentLiningMaterial;
import com.rngtech.content.energy.FuelBoxMaterial;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.energy.ReactorChamberMaterial;
import com.rngtech.content.energy.RecoveryFilterMaterial;
import com.rngtech.content.energy.SolarArrayExtenderMaterial;
import com.rngtech.content.energy.VacuumCollapsePartMaterial;
import com.rngtech.content.machine.AlloyCrucibleMaterial;
import com.rngtech.content.machine.CrushHeadMaterial;
import com.rngtech.content.machine.FluidPumpMaterial;
import com.rngtech.content.machine.ServoMaterial;
import com.rngtech.content.recycling.DisassemblyHeadMaterial;
import com.rngtech.content.tool.ToolHeadFamily;
import com.rngtech.content.tool.ToolHeadMaterial;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * No machine or part may roll a stat or behavior that nothing reads. {@code READS} declares which stats each machine's
 * logic reads, taken from its block entity; a part stat counts only when it survives the part's merge rules (checked
 * from {@link ComponentBaseStatCatalog}) into at least one host that reads it. Update the tables when a machine starts
 * or stops reading a stat, never to make a new dead roll pass.
 */
public final class RollReadChecks {
    private static final Set<MachineStat> HEAT = EnumSet.of(MachineStat.HEAT_TRANSFER, MachineStat.THERMAL_CONDUCTIVITY,
            MachineStat.MAX_TEMPERATURE, MachineStat.HEAT_ISOLATION, MachineStat.THERMAL_MASS, MachineStat.COOLING_RATE,
            MachineStat.TEMPERATURE_STABILITY, MachineStat.OVERHEAT_TOLERANCE);
    private static final Set<MachineStat> ATTRIBUTES = EnumSet.of(MachineStat.CONTROL, MachineStat.DRIVE, MachineStat.RESERVE);
    private static final Set<MachineStat> BATCHING = EnumSet.of(MachineStat.BATCH_SIZE, MachineStat.BATCH_OVERHEAD);
    private static final Set<MachineStat> TOOL = EnumSet.of(MachineStat.DURABILITY, MachineStat.SELF_REPAIR, MachineStat.MINING_SPEED,
            MachineStat.ATTACK_SPEED, MachineStat.FE_USAGE, MachineStat.STABILITY, MachineStat.LUCK);
    /** Hosts that add the Control, Drive, and Reserve rolled on their Gear before their Mastery conversions. */
    private static final Set<String> ATTRIBUTE_HOSTS = Set.of("crusher", "furnace", "electric_furnace", "alloy_furnace",
            "metal_press", "melter", "resonance_calibrator", "forestry_companion");
    private static final Map<String, Set<MachineStat>> READS = new HashMap<>();
    private static final Map<String, Set<MachineBehavior>> BEHAVIOR_READS = new HashMap<>();
    private static int checks;

    static {
        reads("crusher", Set.of(ATTRIBUTES, BATCHING), MachineStat.PROCESSING_LEVEL, MachineStat.PROCESSING_SPEED,
                MachineStat.ENERGY_USAGE, MachineStat.ENERGY_CAPACITY, MachineStat.OUTPUT_AMOUNT, MachineStat.SUPER_OUTPUT_CHANCE,
                MachineStat.INSTANT_PROCESS_CHANCE, MachineStat.CRUSHER_SALVAGE_CHANCE, MachineStat.NO_BATTERY_OUTPUT_RETENTION,
                MachineStat.OUTPUT_GUARD_GRACE, MachineStat.CRUSHER_INPUT_FILTER, MachineStat.HIGH_HARDNESS_ENERGY_MITIGATION);
        reads("furnace", Set.of(HEAT, ATTRIBUTES, BATCHING), MachineStat.EFFICIENCY, MachineStat.FUEL_EFFICIENCY,
                MachineStat.FUEL_DURATION, MachineStat.PROCESSING_SPEED, MachineStat.INPUT_SLOTS, MachineStat.INSTANT_PROCESS_CHANCE,
                MachineStat.SUPER_OUTPUT_CHANCE);
        reads("electric_furnace", Set.of(HEAT, ATTRIBUTES, BATCHING), MachineStat.ENERGY_USAGE, MachineStat.ENERGY_CAPACITY,
                MachineStat.PROCESSING_SPEED, MachineStat.INPUT_SLOTS, MachineStat.INSTANT_PROCESS_CHANCE, MachineStat.SUPER_OUTPUT_CHANCE);
        reads("alloy_furnace", Set.of(HEAT, ATTRIBUTES, BATCHING), MachineStat.ENERGY_USAGE, MachineStat.ENERGY_CAPACITY,
                MachineStat.PROCESSING_SPEED, MachineStat.INPUT_SLOTS, MachineStat.INSTANT_PROCESS_CHANCE, MachineStat.SUPER_OUTPUT_CHANCE);
        // Stability only acts on the Steel Press, where servo instability adds strain.
        reads("metal_press", Set.of(HEAT, ATTRIBUTES, BATCHING), MachineStat.ENERGY_USAGE, MachineStat.ENERGY_CAPACITY,
                MachineStat.PROCESSING_SPEED, MachineStat.STABILITY, MachineStat.INSTANT_PROCESS_CHANCE, MachineStat.SUPER_OUTPUT_CHANCE);
        reads("melter", Set.of(HEAT, ATTRIBUTES, BATCHING), MachineStat.ENERGY_USAGE, MachineStat.ENERGY_CAPACITY,
                MachineStat.PROCESSING_SPEED, MachineStat.PROCESSING_LEVEL, MachineStat.FLUID_TRANSFER, MachineStat.FLUID_CAPACITY,
                MachineStat.INSTANT_PROCESS_CHANCE);
        reads("resonance_calibrator", Set.of(ATTRIBUTES, BATCHING), MachineStat.PROCESSING_SPEED, MachineStat.ENERGY_USAGE,
                MachineStat.ENERGY_CAPACITY, MachineStat.CALIBRATION_QUALITY, MachineStat.CALIBRATION_PRECISION, MachineStat.STABILITY,
                MachineStat.CATALYST_EFFICIENCY, MachineStat.REFINEMENT_POTENTIAL_BONUS, MachineStat.INSTANT_PROCESS_CHANCE,
                MachineStat.SUPER_OUTPUT_CHANCE);
        reads("battery_assembler", Set.of(), MachineStat.PROCESSING_SPEED, MachineStat.ENERGY_USAGE, MachineStat.ENERGY_CAPACITY,
                MachineStat.FLUID_TRANSFER, MachineStat.INSTANT_PROCESS_CHANCE);
        reads("component_recycler", Set.of(), MachineStat.PROCESSING_SPEED, MachineStat.ENERGY_USAGE, MachineStat.ENERGY_CAPACITY,
                MachineStat.PROCESSING_LEVEL, MachineStat.INSTANT_PROCESS_CHANCE);
        reads("compressor_tank", Set.of(), MachineStat.PROCESSING_SPEED, MachineStat.ENERGY_USAGE, MachineStat.ENERGY_CAPACITY,
                MachineStat.FLUID_TRANSFER);
        reads("fluid_tank", Set.of());
        reads("coal_gasifier", Set.of(), MachineStat.PROCESSING_SPEED, MachineStat.ENERGY_USAGE, MachineStat.ENERGY_CAPACITY,
                MachineStat.INSTANT_PROCESS_CHANCE);
        reads("steam_methane_reformer", Set.of(), MachineStat.PROCESSING_SPEED, MachineStat.ENERGY_USAGE, MachineStat.ENERGY_CAPACITY,
                MachineStat.INSTANT_PROCESS_CHANCE);
        reads("syngas_combustor", Set.of(), MachineStat.PROCESSING_SPEED, MachineStat.ENERGY_CAPACITY, MachineStat.ENERGY_GENERATION,
                MachineStat.EFFICIENCY);
        reads("ammonia_synthesizer", Set.of(), MachineStat.PROCESSING_SPEED, MachineStat.ENERGY_USAGE, MachineStat.ENERGY_CAPACITY,
                MachineStat.INSTANT_PROCESS_CHANCE);
        reads("ammonia_fuel_cell", Set.of(), MachineStat.PROCESSING_SPEED, MachineStat.ENERGY_CAPACITY, MachineStat.ENERGY_GENERATION,
                MachineStat.EFFICIENCY);
        reads("solid_fuel_burner", Set.of(), MachineStat.ENERGY_GENERATION, MachineStat.EFFICIENCY, MachineStat.FUEL_EFFICIENCY,
                MachineStat.FUEL_DURATION, MachineStat.HEAT_ISOLATION, MachineStat.INPUT_SLOTS);
        reads("bio_generator", Set.of(), MachineStat.ENERGY_GENERATION, MachineStat.EFFICIENCY, MachineStat.FUEL_EFFICIENCY,
                MachineStat.FUEL_DURATION, MachineStat.ENERGY_CAPACITY, MachineStat.POTATO_POWER, MachineStat.CARROT_POWER,
                MachineStat.BREAD_POWER, MachineStat.SAPLING_POWER, MachineStat.SEED_POWER, MachineStat.PLANT_POWER,
                MachineStat.ORGANIC_REAGENT_POWER, MachineStat.COMPOSTED_BIOMASS_POWER, MachineStat.ALGAE_POWER,
                MachineStat.RICH_BIOMASS_POWER);
        reads("solar_panel", Set.of(), MachineStat.ENERGY_GENERATION, MachineStat.EFFICIENCY, MachineStat.PEAK_SOLAR_GENERATION,
                MachineStat.ENERGY_CAPACITY);
        reads("solar_array_controller", Set.of(), MachineStat.ENERGY_GENERATION, MachineStat.EFFICIENCY, MachineStat.ENERGY_CAPACITY,
                MachineStat.STABILITY, MachineStat.SOLAR_PANEL_LIMIT, MachineStat.SOLAR_PANEL_SYNCHRONIZATION,
                MachineStat.SOLAR_PANEL_ARBITRATION, MachineStat.WEATHER_RECOVERY, MachineStat.MOONLIGHT_CONVERSION,
                MachineStat.LUNAR_INVERSION, MachineStat.CLEAR_SKY_AMPLIFICATION);
        reads("potential_reactor", Set.of(), MachineStat.PROCESSING_LEVEL, MachineStat.PROCESSING_SPEED, MachineStat.EFFICIENCY,
                MachineStat.STABILITY, MachineStat.ENERGY_GENERATION, MachineStat.ENERGY_CAPACITY);
        reads("corrosion_cell", Set.of(), MachineStat.PROCESSING_SPEED, MachineStat.EFFICIENCY, MachineStat.STABILITY,
                MachineStat.ENERGY_GENERATION, MachineStat.ENERGY_CAPACITY, MachineStat.FLUID_CAPACITY, MachineStat.FLUID_TRANSFER);
        reads("vacuum_collapse_generator", Set.of(), MachineStat.PROCESSING_LEVEL, MachineStat.STABILITY, MachineStat.EFFICIENCY,
                MachineStat.PROCESSING_SPEED, MachineStat.ENERGY_GENERATION, MachineStat.ENERGY_CAPACITY);
        // Durability is the installed rotor's own wear limit.
        reads("cavitation_generator", Set.of(), MachineStat.PROCESSING_LEVEL, MachineStat.PROCESSING_SPEED, MachineStat.ENERGY_GENERATION,
                MachineStat.EFFICIENCY, MachineStat.TEMPERATURE_STABILITY, MachineStat.STABILITY, MachineStat.OUTPUT_AMOUNT,
                MachineStat.MAX_TEMPERATURE, MachineStat.OVERHEAT_TOLERANCE, MachineStat.STRAIN_RECOVERY, MachineStat.HEAT_TRANSFER,
                MachineStat.FLUID_TRANSFER, MachineStat.ENERGY_CAPACITY, MachineStat.DURABILITY);
        reads("battery_cell", Set.of(), MachineStat.ENERGY_CAPACITY, MachineStat.ENERGY_TRANSFER, MachineStat.IDLE_LOSS);
        reads("battery_chassis", Set.of(), MachineStat.ENERGY_CAPACITY, MachineStat.EFFICIENCY, MachineStat.ENERGY_TRANSFER,
                MachineStat.BURST_TRANSFER, MachineStat.BURST_DURATION, MachineStat.STABILITY, MachineStat.IDLE_LOSS,
                MachineStat.GLOBAL_MODIFIER_STRENGTH, MachineStat.BATTERY_SLOTS);
        reads("tool_head", Set.of(TOOL));
        reads("treefeller_head", Set.of(TOOL), MachineStat.TREE_FELL_LIMIT);
        reads("ore_burst_head", Set.of(TOOL), MachineStat.ORE_BURST_SPEED);
        reads("pick_head", Set.of(TOOL), MachineStat.VEIN_MINE_LIMIT, MachineStat.VEIN_MINE_FE_USAGE);
        reads("ore_burst_pick_head", Set.of(TOOL), MachineStat.VEIN_MINE_LIMIT, MachineStat.VEIN_MINE_FE_USAGE, MachineStat.ORE_BURST_SPEED);
        reads("tool_rod", Set.of(TOOL), MachineStat.FE_TRANSFER, MachineStat.CONTROL, MachineStat.BATTERY_SUPPORT);
        reads("modular_tool", Set.of(TOOL), MachineStat.BATTERY_SUPPORT);
        reads("miners_companion", Set.of(), MachineStat.ENERGY_USAGE, MachineStat.BLOCK_FILTER_SLOTS);
        reads("forestry_companion", Set.of(ATTRIBUTES), MachineStat.PROCESSING_SPEED, MachineStat.ENERGY_USAGE,
                MachineStat.FLUID_CAPACITY, MachineStat.FLUID_TRANSFER);
        reads("phase_emitter", Set.of(PhaseArrayRules.LINE_STATS));
        reads("phase_coil", Set.of(PhaseArrayRules.LINE_STATS));

        for (String processor : List.of("crusher", "electric_furnace", "alloy_furnace", "metal_press", "melter", "resonance_calibrator",
                "battery_assembler", "component_recycler", "potential_reactor", "corrosion_cell", "coal_gasifier",
                "steam_methane_reformer", "ammonia_synthesizer")) {
            BEHAVIOR_READS.computeIfAbsent(processor, id -> EnumSet.noneOf(MachineBehavior.class)).add(MachineBehavior.BULK_SPEED);
        }
        for (String graced : List.of("electric_furnace", "alloy_furnace", "metal_press")) {
            BEHAVIOR_READS.computeIfAbsent(graced, id -> EnumSet.noneOf(MachineBehavior.class)).add(MachineBehavior.POWER_GRACE);
        }
    }

    private RollReadChecks() {
    }

    public static int run() {
        checks = 0;
        machineRollsAreRead();
        partRollsReachAReadingHost();
        toolHeadsRollOnlyTheirFamilyStats();
        retiredRollsStillDisplay();
        return checks;
    }

    private static void machineRollsAreRead() {
        Set<ModifierEligibilityProfile> profiles = new LinkedHashSet<>();
        for (MachineType type : MachineType.values()) {
            profiles.add(ModifierEligibilityProfiles.forMachine(type));
        }
        profiles.add(ModifierEligibilityProfiles.TREEFELLER_HEAD);
        profiles.add(ModifierEligibilityProfiles.ORE_BURST_HEAD);
        profiles.add(ModifierEligibilityProfiles.PICK_HEAD);
        profiles.add(ModifierEligibilityProfiles.ORE_BURST_PICK_HEAD);
        for (ModifierEligibilityProfile profile : profiles) {
            Set<MachineStat> read = READS.get(profile.id());
            require(read != null, profile.id() + " declares the stats it reads");
            if (read == null) {
                continue;
            }
            for (MachineStat stat : rolledStats(profile)) {
                require(read.contains(stat), profile.id() + " rolls " + stat + " and reads it");
            }
            for (MachineBehavior behavior : profile.rollableBehaviors()) {
                require(BEHAVIOR_READS.getOrDefault(profile.id(), Set.of()).contains(behavior),
                        profile.id() + " rolls " + behavior + " and reads it");
            }
        }
    }

    private static void partRollsReachAReadingHost() {
        for (MachinePartType partType : MachinePartType.values()) {
            ModifierEligibilityProfile profile = ModifierEligibilityProfiles.forMachinePart(partType, null);
            List<List<Host>> variants = hosts(partType);
            require(!variants.isEmpty(), partType + " declares its hosts");
            for (List<Host> hosts : variants) {
                for (MachineStat stat : rolledStats(profile)) {
                    require(hosts.stream().anyMatch(host -> host.reads(stat)), partType + " rolls " + stat + " and a host reads it");
                }
                for (MachineBehavior behavior : profile.rollableBehaviors()) {
                    require(hosts.stream().anyMatch(host -> BEHAVIOR_READS.getOrDefault(host.machine(), Set.of()).contains(behavior)),
                            partType + " rolls " + behavior + " and a host reads it");
                }
            }
        }
    }

    private static void toolHeadsRollOnlyTheirFamilyStats() {
        for (ToolHeadFamily family : ToolHeadFamily.values()) {
            for (ToolHeadMaterial material : ToolHeadMaterial.values()) {
                Set<MachineStat> rolled = rolledStats(ModifierEligibilityProfiles.forToolHead(family, material));
                require(rolled.contains(MachineStat.TREE_FELL_LIMIT) == (family == ToolHeadFamily.TREEFELLER),
                        family + " " + material + " head rolls Tree Fell Limit only on a Treefeller");
                require(rolled.contains(MachineStat.ORE_BURST_SPEED) == (family.supportsOreBurst() && material.oreBurst()),
                        family + " " + material + " head rolls Ore Burst Speed only with an ore burst base");
            }
        }
    }

    /** A part or machine that rolled a stat before it stopped rolling keeps the line, and it still reads and can be removed. */
    private static void retiredRollsStillDisplay() {
        retired(ModifierEligibilityProfiles.COAL_GASIFIER, MachineStat.STABILITY);
        retired(ModifierEligibilityProfiles.BATTERY_CELL, MachineStat.EFFICIENCY);
        retired(ModifierEligibilityProfiles.FUEL_CELL_MEMBRANE, MachineStat.FLUID_TRANSFER);
        MachineModifier cleanBreak = new MachineModifier("clean_break", "output_amount", ModifierSlot.SUFFIX, MachineStat.OUTPUT_AMOUNT,
                ModifierOperation.INCREASED_PERCENT, 2, ModifierValueRange.fixed(6), 6, List.of());
        require(ModifierLensTargets.definitionFor(ModifierEligibilityProfiles.COMPONENT_RECYCLER, cleanBreak) == null,
                "retired Clean Break no longer matches a Component Recycler roll");
        require(MachineModifierText.tooltipLine(cleanBreak) != null, "retired Clean Break still has a tooltip line");
    }

    private static void retired(ModifierEligibilityProfile profile, MachineStat stat) {
        MachineModifier old = new MachineModifier(ModifierSlot.SUFFIX, stat, ModifierOperation.INCREASED_PERCENT, 10);
        require(!rolledStats(profile).contains(stat), profile.id() + " no longer rolls " + stat);
        require(ModifierLensTargets.definitionFor(profile, old) == null, profile.id() + " keeps no roll for an old " + stat + " line");
        require(MachineModifierText.tooltipLine(old) != null, profile.id() + " still shows an old " + stat + " line");
    }

    private static Set<MachineStat> rolledStats(ModifierEligibilityProfile profile) {
        Set<MachineStat> stats = EnumSet.noneOf(MachineStat.class);
        for (ModifierDefinition definition : profile.definitions()) {
            if (!definition.canRoll() || definition.isBehaviorOnly()) {
                continue;
            }
            definition.effects().forEach(effect -> stats.add(MachineStatAccumulator.accumulationStat(effect.stat())));
        }
        return stats;
    }

    /** One host list per part material, so a stat must reach a reader whatever the part is made of. */
    private static List<List<Host>> hosts(MachinePartType partType) {
        return switch (partType) {
            case CRUSH_HEAD -> each(CrushHeadMaterial.values(), material -> merged(ComponentBaseStatCatalog.crushHead(material),
                    "crusher", "melter"));
            case HEAT_CORE -> each(HeatCoreMaterial.values(), material -> {
                List<Host> hosts = merged(ComponentBaseStatCatalog.heatCore(material), "electric_furnace", "alloy_furnace",
                        "metal_press", "melter", "solid_fuel_burner", "coal_gasifier", "steam_methane_reformer");
                hosts.add(new Host("cavitation_generator", EnumSet.of(MachineStat.HEAT_TRANSFER, MachineStat.HEAT_ISOLATION,
                        MachineStat.COOLING_RATE, MachineStat.TEMPERATURE_STABILITY, MachineStat.OVERHEAT_TOLERANCE,
                        MachineStat.MAX_TEMPERATURE, MachineStat.STRAIN_RECOVERY)));
                return hosts;
            });
            case BIO_CHAMBER -> List.of(merged(ComponentBaseStatCatalog.bioChamber(), "bio_generator"));
            case ALLOY_CRUCIBLE -> each(AlloyCrucibleMaterial.values(), material -> merged(ComponentBaseStatCatalog.alloyCrucible(material),
                    "alloy_furnace"));
            case SERVO -> each(ServoMaterial.values(), material -> {
                List<Host> hosts = merged(ComponentBaseStatCatalog.servo(material), "alloy_furnace", "metal_press", "melter",
                        "cavitation_generator", "coal_gasifier", "syngas_combustor", "steam_methane_reformer");
                hosts.add(new Host("compressor_tank", EnumSet.of(MachineStat.PROCESSING_SPEED, MachineStat.ENERGY_USAGE,
                        MachineStat.STABILITY)));
                return hosts;
            });
            case FUEL_BOX -> each(FuelBoxMaterial.values(), material -> merged(ComponentBaseStatCatalog.fuelBox(material),
                    "solid_fuel_burner"));
            case REACTOR_CHAMBER -> each(ReactorChamberMaterial.values(), material -> merged(
                    ComponentBaseStatCatalog.reactorChamber(material), "potential_reactor"));
            case RECOVERY_FILTER -> each(RecoveryFilterMaterial.values(), material -> merged(
                    ComponentBaseStatCatalog.recoveryFilter(material), "potential_reactor", "component_recycler"));
            case CONTAINMENT_LINING -> each(ContainmentLiningMaterial.values(), material -> merged(
                    ComponentBaseStatCatalog.containmentLining(material), "potential_reactor"));
            case DISASSEMBLY_HEAD -> each(DisassemblyHeadMaterial.values(), material -> merged(
                    ComponentBaseStatCatalog.disassemblyHead(material), "component_recycler"));
            case FLUID_PUMP -> each(FluidPumpMaterial.values(), material -> {
                List<Host> hosts = merged(ComponentBaseStatCatalog.fluidPump(material), "melter", "corrosion_cell");
                hosts.add(new Host("bio_generator", EnumSet.of(MachineStat.FLUID_CAPACITY, MachineStat.FLUID_TRANSFER)));
                hosts.add(new Host("forestry_companion", EnumSet.of(MachineStat.FLUID_CAPACITY, MachineStat.FLUID_TRANSFER,
                        MachineStat.CONTROL, MachineStat.DRIVE, MachineStat.RESERVE)));
                return hosts;
            });
            case CATHODE -> each(CathodeMaterial.values(), material -> merged(ComponentBaseStatCatalog.cathode(material), "corrosion_cell"));
            case REFORMING_CATALYST_BED -> IntStream.rangeClosed(1, 8)
                    .mapToObj(stage -> merged(ComponentBaseStatCatalog.reformingCatalystBed(stage), "steam_methane_reformer"))
                    .toList();
            case AMMONIA_CATALYST_BED -> List.of(merged(ComponentBaseStatCatalog.ammoniaCatalystBed(), "ammonia_synthesizer"));
            case FUEL_CELL_MEMBRANE -> List.of(merged(ComponentBaseStatCatalog.fuelCellMembrane(), "ammonia_fuel_cell"));
            case CAVITATION_ROTOR -> each(CavitationRotorMaterial.values(), material -> merged(
                    ComponentBaseStatCatalog.cavitationRotor(material), "cavitation_generator"));
            case VOID_CHAMBER, DIMENSIONAL_STABILIZER -> each(VacuumCollapsePartMaterial.values(), material -> merged(
                    ComponentBaseStatCatalog.vacuumCollapsePart(partType, material), "vacuum_collapse_generator"));
            case COLLAPSE_NOZZLE -> each(CollapseNozzleMaterial.values(), material -> {
                List<Host> hosts = merged(ComponentBaseStatCatalog.collapseNozzle(material), "cavitation_generator");
                if (material.vacuumCollapseCompatible()) {
                    hosts.addAll(merged(ComponentBaseStatCatalog.vacuumCollapseNozzle(material), "vacuum_collapse_generator"));
                }
                return hosts;
            });
            case SOLAR_ARRAY_EXTENDER -> each(SolarArrayExtenderMaterial.values(), material -> merged(
                    ComponentBaseStatCatalog.solarArrayExtender(material), "solar_array_controller"));
            case RESONANCE_COIL -> each(CalibrationGearMaterial.values(), material -> merged(
                    ComponentBaseStatCatalog.resonanceCoil(material), "resonance_calibrator"));
            case CONTROL_BOARD -> each(CalibrationGearMaterial.values(), material -> merged(
                    ComponentBaseStatCatalog.controlBoard(material), "resonance_calibrator"));
            case STABILIZER_MATRIX -> each(CalibrationGearMaterial.values(), material -> merged(
                    ComponentBaseStatCatalog.stabilizerMatrix(material), "resonance_calibrator"));
        };
    }

    private static <M> List<List<Host>> each(M[] materials, Function<M, List<Host>> hosts) {
        List<List<Host>> variants = new ArrayList<>();
        for (M material : materials) {
            variants.add(hosts.apply(material));
        }
        return variants;
    }

    /**
     * Hosts that merge the part through {@code profile}: a stat reaches them when its merge rules carry it, when it is a
     * host stat such as Fluid Capacity, or when it is an attribute and the host adds Gear attributes.
     */
    private static List<Host> merged(ComponentBaseStatCatalog.Profile profile, String... machines) {
        List<Host> hosts = new ArrayList<>();
        for (String machine : machines) {
            Set<MachineStat> reaches = EnumSet.copyOf(profile.mergeRules().keySet());
            reaches.add(MachineStat.FLUID_CAPACITY);
            if (ATTRIBUTE_HOSTS.contains(machine)) {
                reaches.addAll(ATTRIBUTES);
            }
            hosts.add(new Host(machine, reaches));
        }
        return hosts;
    }

    private static void reads(String machine, Set<Set<MachineStat>> shared, MachineStat... stats) {
        Set<MachineStat> read = EnumSet.noneOf(MachineStat.class);
        shared.forEach(read::addAll);
        read.addAll(List.of(stats));
        READS.put(machine, read);
    }

    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError(label);
        }
    }

    private record Host(String machine, Set<MachineStat> reaches) {
        private boolean reads(MachineStat stat) {
            return reaches.contains(stat) && READS.getOrDefault(machine, Set.of()).contains(stat);
        }
    }
}
