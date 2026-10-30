package com.rngtech.rpg;

import com.rngtech.rpg.progression.MachineMasteryHost;
import com.rngtech.rpg.progression.MachineProgressionState;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Deep Tanks, Twinned, Studious, Glow, Discreet, and Telemetry: affixes several machine families share. */
public final class SharedAffixChecks {
    private static final List<ModifierEligibilityProfile> FLUID_MACHINES = List.of(
            ModifierEligibilityProfiles.CORROSION_CELL,
            ModifierEligibilityProfiles.MELTER,
            ModifierEligibilityProfiles.BATTERY_ASSEMBLER,
            ModifierEligibilityProfiles.COAL_GASIFIER,
            ModifierEligibilityProfiles.SYNGAS_COMBUSTOR,
            ModifierEligibilityProfiles.STEAM_METHANE_REFORMER,
            ModifierEligibilityProfiles.CAVITATION_GENERATOR,
            ModifierEligibilityProfiles.AMMONIA_SYNTHESIZER,
            ModifierEligibilityProfiles.AMMONIA_FUEL_CELL
    );
    private static final List<ModifierEligibilityProfile> BATCHING_MACHINES = List.of(
            ModifierEligibilityProfiles.FURNACE,
            ModifierEligibilityProfiles.ELECTRIC_FURNACE,
            ModifierEligibilityProfiles.ALLOY_FURNACE,
            ModifierEligibilityProfiles.MELTER,
            ModifierEligibilityProfiles.METAL_PRESS,
            ModifierEligibilityProfiles.RESONANCE_CALIBRATOR
    );

    /** Every machine body with a work state and a buffer or Heat Core: the Glow and Telemetry hosts. */
    private static final List<ModifierEligibilityProfile> LIT_MACHINES = List.of(
            ModifierEligibilityProfiles.CRUSHER,
            ModifierEligibilityProfiles.FURNACE,
            ModifierEligibilityProfiles.ELECTRIC_FURNACE,
            ModifierEligibilityProfiles.ALLOY_FURNACE,
            ModifierEligibilityProfiles.METAL_PRESS,
            ModifierEligibilityProfiles.MELTER,
            ModifierEligibilityProfiles.RESONANCE_CALIBRATOR,
            ModifierEligibilityProfiles.COMPONENT_RECYCLER,
            ModifierEligibilityProfiles.BATTERY_ASSEMBLER,
            ModifierEligibilityProfiles.COMPRESSOR_TANK,
            ModifierEligibilityProfiles.COAL_GASIFIER,
            ModifierEligibilityProfiles.STEAM_METHANE_REFORMER,
            ModifierEligibilityProfiles.SYNGAS_COMBUSTOR,
            ModifierEligibilityProfiles.AMMONIA_SYNTHESIZER,
            ModifierEligibilityProfiles.AMMONIA_FUEL_CELL,
            ModifierEligibilityProfiles.SOLID_FUEL_BURNER,
            ModifierEligibilityProfiles.BIO_GENERATOR,
            ModifierEligibilityProfiles.SOLAR_PANEL,
            ModifierEligibilityProfiles.SOLAR_ARRAY_CONTROLLER,
            ModifierEligibilityProfiles.POTENTIAL_REACTOR,
            ModifierEligibilityProfiles.CORROSION_CELL,
            ModifierEligibilityProfiles.VACUUM_COLLAPSE_GENERATOR,
            ModifierEligibilityProfiles.CAVITATION_GENERATOR,
            ModifierEligibilityProfiles.BATTERY_CHASSIS
    );
    private static final Set<String> HEATED = Set.of("furnace", "electric_furnace", "alloy_furnace", "metal_press", "melter");

    private static int checks;

    private SharedAffixChecks() {
    }

    public static int run() {
        checks = 0;
        deepTanksRollOnFluidMachines();
        twinnedRollsOnBatchingMachinesOnly();
        studiousRollsWhereverAttributesDo();
        studiousScalesMasteryXp();
        glowRollsOnMachineBodiesOnly();
        discreetStaysOffMasteryMachines();
        telemetrySplitsItsWeightAcrossGauges();
        genericSpeedYieldsToNamedSpeed();
        return checks;
    }

    private static void deepTanksRollOnFluidMachines() {
        for (ModifierEligibilityProfile profile : FLUID_MACHINES) {
            require(rolls(profile, ModifierSlot.PREFIX, "fluid_capacity_flat"), profile.id() + " rolls flat Fluid Capacity");
            require(rolls(profile, ModifierSlot.SUFFIX, "increased_percent:fluid_capacity"), profile.id() + " rolls increased Fluid Capacity");
        }
        require(!rolls(ModifierEligibilityProfiles.FLUID_TANK, ModifierSlot.PREFIX, "fluid_capacity_flat"),
                "storage tanks keep their fixed capacity");
    }

    private static void twinnedRollsOnBatchingMachinesOnly() {
        for (ModifierEligibilityProfile profile : BATCHING_MACHINES) {
            ModifierDefinition twinned = definition(profile, "twinned");
            require(twinned != null && twinned.slot() == ModifierSlot.PREFIX, profile.id() + " rolls Twinned as a prefix");
            require(twinned.modGroup().equals("dense_parallel"), "Twinned shares Crusher Throughput's group");
            require(twinned.rollWeight() == 15, "Twinned is a rare chase line");
        }
        require(definition(ModifierEligibilityProfiles.CRUSHER, "twinned") == null, "the Crusher keeps Crusher Throughput instead");
        for (ModifierEligibilityProfile profile : ModifierEligibilityProfiles.allProfiles()) {
            if (!BATCHING_MACHINES.contains(profile)) {
                require(definition(profile, "twinned") == null, profile.id() + " does not batch, so it never rolls Twinned");
            }
        }
    }

    private static void studiousRollsWhereverAttributesDo() {
        Set<String> withAttributes = ModifierEligibilityProfiles.allProfiles().stream()
                .filter(profile -> profile.hasCapability(ModifierCapability.HAS_MASTERY_ATTRIBUTES))
                .map(ModifierEligibilityProfile::id)
                .collect(Collectors.toSet());
        Set<String> withStudious = ModifierEligibilityProfiles.allProfiles().stream()
                .filter(profile -> rolls(profile, ModifierSlot.SUFFIX, "mastery_xp_gain"))
                .map(ModifierEligibilityProfile::id)
                .collect(Collectors.toSet());
        require(!withAttributes.isEmpty() && withAttributes.equals(withStudious), "Studious rolls exactly where attribute suffixes do");
        require(MachineStat.isMachineWide(MachineStat.MASTERY_XP_GAIN), "a part's Studious roll applies to the whole machine");
    }

    private static void studiousScalesMasteryXp() {
        MachineProgressionState start = MachineProgressionState.EMPTY;
        require(start.withAddedScaledXp(40, 4, 1.0).equals(start.withAddedScaledXp(40, 4)), "no Studious roll changes nothing");
        near(start.withAddedScaledXp(40, 4, 1.25).xp(), 50, "25% increased gain on a full grant");
        near(start.withAddedScaledXp(40, 2, 1.25).xp(), 25, "the gain applies after the band's quarters");
        MachineProgressionState small = start.withAddedScaledXp(1, 4, 1.25);
        near(small.xp() * 4 + small.xpRemainder(), 5, "a 1 XP grant keeps its bonus as a quarter");

        MachineStatAccumulator stats = MachineStatAccumulator.componentBase(java.util.Map.of());
        near(MachineMasteryHost.xpGain(stats), 1.0, "a machine without Studious gains XP at the normal rate");
        stats.apply(new MachineModifier(ModifierSlot.SUFFIX, MachineStat.MASTERY_XP_GAIN, ModifierOperation.INCREASED_PERCENT, 18));
        stats.apply(new MachineModifier(ModifierSlot.SUFFIX, MachineStat.MASTERY_XP_GAIN, ModifierOperation.INCREASED_PERCENT, 12));
        near(MachineMasteryHost.xpGain(stats), 1.30, "Studious rolls on the machine and its Gear add up");
    }

    private static void glowRollsOnMachineBodiesOnly() {
        for (ModifierEligibilityProfile profile : ModifierEligibilityProfiles.allProfiles()) {
            ModifierDefinition glow = definition(profile, "glow");
            require((glow != null) == LIT_MACHINES.contains(profile), profile.id() + " rolls Glow only if it is a lit machine body");
            if (glow != null) {
                require(glow.slot() == ModifierSlot.SUFFIX && glow.modGroup().equals("light_emission"), "Glow is the light_emission suffix");
                require(glow.rollWeight() == 70, "Glow is a filler line");
                require(glow.tierRanges().stream().map(ModifierValueRange::min).toList().equals(List.of(4.0, 7.0, 10.0, 13.0)),
                        "Glow lights at level 4, 7, 10, or 13");
            }
        }
        for (MachinePartType partType : MachinePartType.values()) {
            require(definition(ModifierEligibilityProfiles.forMachinePart(partType, null), "glow") == null, partType + " Gear never rolls Glow");
        }
    }

    private static void discreetStaysOffMasteryMachines() {
        List<String> quiet = new ArrayList<>();
        for (ModifierEligibilityProfile profile : ModifierEligibilityProfiles.allProfiles()) {
            ModifierDefinition discreet = definition(profile, "discreet");
            if (discreet == null) {
                continue;
            }
            quiet.add(profile.id());
            require(!profile.hasCapability(ModifierCapability.HAS_MASTERY_ATTRIBUTES),
                    profile.id() + " leaves silence to the Silent Operation keystone");
            require(LIT_MACHINES.contains(profile), profile.id() + " rolls Discreet on its machine body");
            require(discreet.slot() == ModifierSlot.PREFIX && discreet.rollWeight() == 70, "Discreet is a filler prefix");
            require(discreet.operation() == ModifierOperation.DECREASED_PERCENT && discreet.stat() == MachineStat.MACHINE_NOISE,
                    "Discreet reduces Machine Noise");
        }
        require(quiet.size() == 17, "Discreet rolls on the 17 sound-profile machines without Mastery, not " + quiet.size());
    }

    private static void telemetrySplitsItsWeightAcrossGauges() {
        for (ModifierEligibilityProfile profile : ModifierEligibilityProfiles.allProfiles()) {
            List<ModifierDefinition> telemetry = profile.definitions().stream()
                    .filter(definition -> definition.modGroup().equals("comparator_readout"))
                    .toList();
            require(telemetry.isEmpty() != LIT_MACHINES.contains(profile), profile.id() + " rolls Telemetry only if it is a lit machine body");
            if (telemetry.isEmpty()) {
                continue;
            }
            List<TelemetryReadout> readouts = ModifierEligibilityProfiles.telemetryReadouts(profile.id());
            require(telemetry.stream().map(definition -> TelemetryReadout.byAffixId(definition.id())).toList().equals(readouts),
                    profile.id() + " rolls one Telemetry line per gauge it reports");
            require(telemetry.stream().mapToInt(ModifierDefinition::rollWeight).sum() == 60, profile.id() + " splits a Telemetry weight of 60");
            require(telemetry.stream().allMatch(definition -> definition.slot() == ModifierSlot.SUFFIX && !definition.isTiered()),
                    "Telemetry is an untiered suffix");
            require(readouts.contains(TelemetryReadout.HEAT) == HEATED.contains(profile.id()),
                    profile.id() + " reports heat only with a Heat Core");
            require(readouts.contains(TelemetryReadout.ENERGY) == !profile.id().equals("furnace"),
                    profile.id() + " reports its buffer unless it burns fuel");
        }
    }

    private static void genericSpeedYieldsToNamedSpeed() {
        int diluted = 0;
        for (ModifierEligibilityProfile profile : ModifierEligibilityProfiles.allProfiles()) {
            ModifierDefinition generic = definition(profile, "processing_speed");
            if (generic == null) {
                continue;
            }
            boolean named = profile.definitions().stream().anyMatch(definition -> definition.slot() == ModifierSlot.SUFFIX
                    && definition.modGroup().equals("processing_speed")
                    && !definition.id().equals("processing_speed")
                    && !definition.id().equals("overclocked"));
            require(generic.rollWeight() == (named ? 75 : 100), profile.id() + " rolls generic speed at " + (named ? 75 : 100));
            if (named) {
                diluted++;
            }
        }
        require(diluted > 10, "generic speed yields on the processors with a named speed");
    }

    private static boolean rolls(ModifierEligibilityProfile profile, ModifierSlot slot, String group) {
        return profile.rollableDefinitions(slot).stream().anyMatch(definition -> definition.modGroup().equals(group));
    }

    private static ModifierDefinition definition(ModifierEligibilityProfile profile, String id) {
        return profile.definitions().stream().filter(definition -> definition.id().equals(id)).findFirst().orElse(null);
    }

    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError(label);
        }
    }

    private static void near(double actual, double expected, String label) {
        checks++;
        if (Math.abs(actual - expected) > 0.0001) {
            throw new AssertionError(label + ": expected " + expected + " but was " + actual);
        }
    }
}
